"use client";

import { getPushPublicKey, removePushSubscription, savePushSubscription } from "@admin/api/push";
import { getAuthToken } from "@admin/core/auth-storage";
import { createStore, useStoreValue } from "@admin/core/client-store";
import { getPushDeviceId } from "@admin/core/device-id";

type ReminderPermissionState = NotificationPermission | "unsupported";

export interface WebPushMessagePayload {
  eventId: string;
  notificationId: string;
  msgType: string;
  title: string;
  content: string;
  route: string;
  priority: string;
  panelAutoOpen: boolean;
  osNotificationEnabled: boolean;
}

interface TodoReminderState {
  permission: ReminderPermissionState;
  pollingEnabled: boolean;
  pushReady: boolean;
  pushSyncing: boolean;
  pushLastError: string;
  secureContext: boolean;
  serviceWorkerScope: string;
}

const todoReminderStore = createStore<TodoReminderState>({
  permission: "unsupported",
  pollingEnabled: true,
  pushReady: false,
  pushSyncing: false,
  pushLastError: "",
  secureContext: false,
  serviceWorkerScope: "",
});

const LOG_PREFIX = "[web-push]";
let serviceWorkerRegistrationPromise: Promise<ServiceWorkerRegistration | null> | null = null;
let pushSubscriptionSyncPromise: Promise<void> | null = null;
let serviceWorkerMessageBound = false;
const webPushMessageSubscribers = new Set<(payload: WebPushMessagePayload) => void>();

export function useTodoReminderPermission(): ReminderPermissionState {
  return useStoreValue(todoReminderStore, (state) => state.permission);
}

export function useTodoReminderPushReady(): boolean {
  return useStoreValue(todoReminderStore, (state) => state.pushReady);
}

export function useTodoReminderPushSyncing(): boolean {
  return useStoreValue(todoReminderStore, (state) => state.pushSyncing);
}

export function useTodoReminderPushLastError(): string {
  return useStoreValue(todoReminderStore, (state) => state.pushLastError);
}

export function useTodoReminderSecureContext(): boolean {
  return useStoreValue(todoReminderStore, (state) => state.secureContext);
}

export function useTodoReminderServiceWorkerScope(): string {
  return useStoreValue(todoReminderStore, (state) => state.serviceWorkerScope);
}

export function useTodoReminderPollingEnabled(): boolean {
  return useStoreValue(todoReminderStore, (state) => state.pollingEnabled);
}

export function onWebPushMessage(handler: (payload: WebPushMessagePayload) => void): () => void {
  webPushMessageSubscribers.add(handler);
  return () => webPushMessageSubscribers.delete(handler);
}

export function initTodoReminderPermission(): void {
  bindServiceWorkerMessageListener();
  todoReminderStore.setState((state) => ({ ...state, secureContext: typeof window !== "undefined" && window.isSecureContext }));

  if (typeof window === "undefined" || !("Notification" in window)) {
    todoReminderStore.setState((state) => ({
      ...state,
      permission: "unsupported",
      pushReady: false,
      pushLastError: "当前浏览器不支持 Notification API",
    }));
    console.warn(`${LOG_PREFIX} Notification API unsupported`);
    return;
  }

  if (!window.isSecureContext) {
    todoReminderStore.setState((state) => ({ ...state, pushLastError: "当前页面不是安全上下文，无法使用 Web Push" }));
  }

  todoReminderStore.setState((state) => ({ ...state, permission: Notification.permission }));
  if (Notification.permission === "granted") {
    void ensureWebPushSubscription();
  } else {
    todoReminderStore.setState((state) => ({ ...state, pushReady: false }));
  }
}

export async function requestTodoReminderPermission(): Promise<ReminderPermissionState> {
  todoReminderStore.setState((state) => ({ ...state, secureContext: typeof window !== "undefined" && window.isSecureContext }));
  if (typeof window === "undefined" || !("Notification" in window)) {
    todoReminderStore.setState((state) => ({
      ...state,
      permission: "unsupported",
      pushReady: false,
      pushLastError: "当前浏览器不支持 Notification API",
    }));
    return todoReminderStore.getState().permission;
  }

  const permission = await Notification.requestPermission();
  todoReminderStore.setState((state) => ({ ...state, permission }));
  if (permission === "granted") {
    await ensureWebPushSubscription();
  } else {
    todoReminderStore.setState((state) => ({
      ...state,
      pushReady: false,
      pushLastError: permission === "denied" ? "通知权限已被拒绝，请在浏览器设置中手动开启" : state.pushLastError,
    }));
  }

  return todoReminderStore.getState().permission;
}

export async function enableTodoReminderPushFromUserGesture(): Promise<boolean> {
  const permission = await requestTodoReminderPermission();
  if (permission !== "granted") {
    return false;
  }
  await ensureWebPushSubscription(true);
  return todoReminderStore.getState().pushReady;
}

export function startTodoReminderPolling(): void {
  // Polling disabled in favor of SSE
}

export function stopTodoReminderPolling(): void {
  // Polling disabled
}

export function resetTodoReminderPolling(): void {
  // Polling disabled
}

export async function ensureWebPushSubscription(force = false): Promise<void> {
  if (typeof window === "undefined") return;
  if (!window.isSecureContext) {
    todoReminderStore.setState((state) => ({ ...state, pushReady: false, pushLastError: "当前页面不是安全上下文，无法建立 Web Push 订阅" }));
    return;
  }
  if (Notification.permission !== "granted") {
    todoReminderStore.setState((state) => ({ ...state, pushReady: false, pushLastError: "通知权限未授权" }));
    return;
  }
  if (!getAuthToken()) {
    todoReminderStore.setState((state) => ({ ...state, pushReady: false, pushLastError: "用户未登录，无法保存推送订阅" }));
    return;
  }
  if (pushSubscriptionSyncPromise && !force) {
    return pushSubscriptionSyncPromise;
  }

  todoReminderStore.setState((state) => ({ ...state, pushSyncing: true, pushLastError: "" }));
  pushSubscriptionSyncPromise = (async () => {
    const registration = await ensureReminderServiceWorker();
    if (!registration || !("pushManager" in registration)) {
      todoReminderStore.setState((state) => ({ ...state, pushReady: false, pushLastError: "Service Worker 或 PushManager 不可用" }));
      return;
    }

    const publicKey = (await getPushPublicKey()).publicKey;
    const applicationServerKey = urlBase64ToArrayBuffer(publicKey);
    let subscription = await registration.pushManager.getSubscription();
    if (subscription && force) {
      await subscription.unsubscribe();
      subscription = null;
    }
    if (!subscription) {
      subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey,
      });
    }

    const subscriptionJson = subscription.toJSON();
    const endpoint = subscriptionJson.endpoint;
    const p256dh = subscriptionJson.keys?.p256dh;
    const auth = subscriptionJson.keys?.auth;
    if (!endpoint || !p256dh || !auth) {
      todoReminderStore.setState((state) => ({ ...state, pushReady: false, pushLastError: "浏览器 PushSubscription 数据不完整" }));
      return;
    }

    await savePushSubscription({
      deviceId: getPushDeviceId(),
      endpoint,
      p256dh,
      auth,
    });

    todoReminderStore.setState((state) => ({ ...state, pushReady: true, pushLastError: "" }));
  })()
    .catch((error) => {
      todoReminderStore.setState((state) => ({ ...state, pushReady: false, pushLastError: resolveErrorMessage(error, "同步 Web Push 订阅失败") }));
      console.warn("[todo-reminder] sync web push subscription failed", error);
    })
    .finally(() => {
      todoReminderStore.setState((state) => ({ ...state, pushSyncing: false }));
      pushSubscriptionSyncPromise = null;
    });

  return pushSubscriptionSyncPromise;
}

export async function removeWebPushSubscription(): Promise<void> {
  if (typeof window === "undefined" || !getAuthToken()) return;
  try {
    await removePushSubscription(getPushDeviceId());
  } catch (error) {
    console.warn("[todo-reminder] remove web push subscription failed", error);
  } finally {
    todoReminderStore.setState((state) => ({ ...state, pushReady: false }));
  }
}

async function ensureReminderServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (serviceWorkerRegistrationPromise) {
    return serviceWorkerRegistrationPromise;
  }

  if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
    return null;
  }

  serviceWorkerRegistrationPromise = navigator.serviceWorker
    .register("/sw.js")
    .then((registration) => {
      todoReminderStore.setState((state) => ({ ...state, serviceWorkerScope: registration.scope }));
      return registration;
    })
    .catch((error) => {
      console.warn("[todo-reminder] register service worker failed", error);
      return null;
    });

  return serviceWorkerRegistrationPromise;
}

function bindServiceWorkerMessageListener(): void {
  if (serviceWorkerMessageBound || typeof window === "undefined" || !("serviceWorker" in navigator)) {
    return;
  }
  serviceWorkerMessageBound = true;
  navigator.serviceWorker.addEventListener("message", (event: MessageEvent<unknown>) => {
    const payload = normalizeWebPushMessagePayload(event.data);
    if (!payload) return;
    webPushMessageSubscribers.forEach((handler) => handler(payload));
  });
}

function normalizeWebPushMessagePayload(raw: unknown): WebPushMessagePayload | null {
  if (!raw || typeof raw !== "object") return null;
  const record = raw as Record<string, unknown>;
  if (record.type !== "BREEZY_WEB_PUSH_MESSAGE") return null;
  return {
    eventId: String(record.eventId ?? ""),
    notificationId: String(record.notificationId ?? ""),
    msgType: String(record.msgType ?? ""),
    title: String(record.title ?? ""),
    content: String(record.content ?? ""),
    route: String(record.route ?? ""),
    priority: String(record.priority ?? "LOW"),
    panelAutoOpen: record.panelAutoOpen === true,
    osNotificationEnabled: record.osNotificationEnabled === true,
  };
}

function urlBase64ToArrayBuffer(base64String: string): ArrayBuffer {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let index = 0; index < rawData.length; index += 1) {
    outputArray[index] = rawData.charCodeAt(index);
  }
  return outputArray.buffer;
}

function resolveErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return fallback;
}
