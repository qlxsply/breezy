import { ref } from "vue";

import { getPushPublicKey, removePushSubscription, savePushSubscription } from "../api/push";
import { getAuthToken } from "../utils/authStorage";
import { getPushDeviceId } from "../utils/deviceId";

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

const reminderPermission = ref<ReminderPermissionState>("unsupported");
const reminderPollingEnabled = ref(true);
const reminderPushReady = ref(false);
const reminderPushSyncing = ref(false);
const reminderPushLastError = ref("");
const reminderSecureContext = ref(false);
const reminderServiceWorkerScope = ref("");

const LOG_PREFIX = "[web-push]";

let serviceWorkerRegistrationPromise: Promise<ServiceWorkerRegistration | null> | null = null;
let pushSubscriptionSyncPromise: Promise<void> | null = null;
let serviceWorkerMessageBound = false;

const webPushMessageSubscribers = new Set<(payload: WebPushMessagePayload) => void>();

export function useTodoReminderPermission() {
  return reminderPermission;
}

export function useTodoReminderPushReady() {
  return reminderPushReady;
}

export function useTodoReminderPushSyncing() {
  return reminderPushSyncing;
}

export function useTodoReminderPushLastError() {
  return reminderPushLastError;
}

export function useTodoReminderSecureContext() {
  return reminderSecureContext;
}

export function useTodoReminderServiceWorkerScope() {
  return reminderServiceWorkerScope;
}

export function useTodoReminderPollingEnabled() {
  return reminderPollingEnabled;
}

export function onWebPushMessage(handler: (payload: WebPushMessagePayload) => void): () => void {
  webPushMessageSubscribers.add(handler);
  return () => {
    webPushMessageSubscribers.delete(handler);
  };
}

export function initTodoReminderPermission(): void {
  bindServiceWorkerMessageListener();

  reminderSecureContext.value = typeof window !== "undefined" && window.isSecureContext;

  if (typeof window === "undefined" || !("Notification" in window)) {
    reminderPermission.value = "unsupported";
    reminderPushReady.value = false;
    reminderPushLastError.value = "当前浏览器不支持 Notification API";
    console.warn(`${LOG_PREFIX} Notification API unsupported`);
    return;
  }

  if (!window.isSecureContext) {
    reminderPushLastError.value = "当前页面不是安全上下文，无法使用 Web Push";
    console.warn(`${LOG_PREFIX} insecure context, Web Push unavailable`);
  }

  reminderPermission.value = Notification.permission;
  console.info(`${LOG_PREFIX} permission status initialized:`, reminderPermission.value);

  if (reminderPermission.value === "granted") {
    void ensureWebPushSubscription();
  } else {
    reminderPushReady.value = false;
  }
}

export async function requestTodoReminderPermission(): Promise<ReminderPermissionState> {
  reminderSecureContext.value = typeof window !== "undefined" && window.isSecureContext;

  if (typeof window === "undefined" || !("Notification" in window)) {
    reminderPermission.value = "unsupported";
    reminderPushReady.value = false;
    reminderPushLastError.value = "当前浏览器不支持 Notification API";
    return reminderPermission.value;
  }

  console.info(`${LOG_PREFIX} requesting notification permission...`);
  const permission = await Notification.requestPermission();
  reminderPermission.value = permission;
  console.info(`${LOG_PREFIX} permission result:`, permission);

  if (permission === "granted") {
    await ensureWebPushSubscription();
  } else {
    reminderPushReady.value = false;
    if (permission === "denied") {
      reminderPushLastError.value = "通知权限已被拒绝，请在浏览器设置中手动开启";
    }
  }

  return reminderPermission.value;
}

export async function enableTodoReminderPushFromUserGesture(): Promise<boolean> {
  const permission = await requestTodoReminderPermission();
  if (permission !== "granted") {
    console.warn(`${LOG_PREFIX} cannot enable push, permission is`, permission);
    return false;
  }

  await ensureWebPushSubscription(true);
  return reminderPushReady.value;
}

export function startTodoReminderPolling(): void {
  // Polling disabled in favor of SSE
  console.log("[todo-reminder] startTodoReminderPolling called but ignored (using SSE)");
}

export function stopTodoReminderPolling(): void {
  // Polling disabled
}

export function resetTodoReminderPolling(): void {
  // Polling disabled
}

export async function ensureWebPushSubscription(force = false): Promise<void> {
  if (typeof window === "undefined") {
    return;
  }

  if (!window.isSecureContext) {
    reminderPushReady.value = false;
    reminderPushLastError.value = "当前页面不是安全上下文，无法建立 Web Push 订阅";
    console.warn(`${LOG_PREFIX} skip subscription because context is not secure`);
    return;
  }

  if (Notification.permission !== "granted") {
    reminderPushReady.value = false;
    reminderPushLastError.value = "通知权限未授权";
    console.info(`${LOG_PREFIX} skip subscription because permission is`, Notification.permission);
    return;
  }

  if (!getAuthToken()) {
    reminderPushReady.value = false;
    reminderPushLastError.value = "用户未登录，无法保存推送订阅";
    console.info(`${LOG_PREFIX} skip subscription because auth token is missing`);
    return;
  }

  if (pushSubscriptionSyncPromise && !force) {
    return pushSubscriptionSyncPromise;
  }

  reminderPushSyncing.value = true;
  reminderPushLastError.value = "";

  pushSubscriptionSyncPromise = (async () => {
    const registration = await ensureReminderServiceWorker();
    if (!registration || !("pushManager" in registration)) {
      reminderPushReady.value = false;
      reminderPushLastError.value = "Service Worker 或 PushManager 不可用";
      console.warn(`${LOG_PREFIX} pushManager unavailable`);
      return;
    }

    console.info(`${LOG_PREFIX} syncing push subscription to server...`);

    const publicKey = (await getPushPublicKey()).publicKey;
    const applicationServerKey = urlBase64ToArrayBuffer(publicKey);
    let subscription = await registration.pushManager.getSubscription();
    if (subscription && force) {
      console.info(
        `${LOG_PREFIX} force resubscribe requested, unsubscribe existing subscription first`,
      );
      await subscription.unsubscribe();
      subscription = null;
    }
    if (!subscription) {
      subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey,
      });
      console.info(`${LOG_PREFIX} created new browser PushSubscription`);
    } else {
      console.info(`${LOG_PREFIX} reusing existing browser PushSubscription`);
    }

    const subscriptionJson = subscription.toJSON();
    const endpoint = subscriptionJson.endpoint;
    const p256dh = subscriptionJson.keys?.p256dh;
    const auth = subscriptionJson.keys?.auth;
    if (!endpoint || !p256dh || !auth) {
      reminderPushReady.value = false;
      reminderPushLastError.value = "浏览器 PushSubscription 数据不完整";
      console.warn(`${LOG_PREFIX} invalid subscription payload`);
      return;
    }

    await savePushSubscription({
      deviceId: getPushDeviceId(),
      endpoint,
      p256dh,
      auth,
    });

    reminderPushReady.value = true;
    reminderPushLastError.value = "";
    console.info(`${LOG_PREFIX} subscription saved to backend`, {
      endpointHost: resolveEndpointHost(endpoint),
      deviceId: getPushDeviceId(),
    });
  })()
    .catch((error) => {
      reminderPushReady.value = false;
      reminderPushLastError.value = resolveErrorMessage(error, "同步 Web Push 订阅失败");
      console.warn("[todo-reminder] sync web push subscription failed", error);
    })
    .finally(() => {
      reminderPushSyncing.value = false;
      pushSubscriptionSyncPromise = null;
    });

  return pushSubscriptionSyncPromise;
}

export async function removeWebPushSubscription(): Promise<void> {
  if (typeof window === "undefined") {
    return;
  }
  if (!getAuthToken()) {
    return;
  }

  console.info(`${LOG_PREFIX} removing push subscription...`);

  const deviceId = getPushDeviceId();
  try {
    await removePushSubscription(deviceId);
    console.info(`${LOG_PREFIX} removed subscription from backend`, { deviceId });
  } catch (error) {
    console.warn("[todo-reminder] remove push subscription failed", error);
  }

  const registration = await ensureReminderServiceWorker();
  if (!registration || !("pushManager" in registration)) {
    return;
  }

  const subscription = await registration.pushManager.getSubscription();
  if (subscription) {
    try {
      await subscription.unsubscribe();
      console.info(`${LOG_PREFIX} unsubscribed from browser push manager`);
    } catch (error) {
      console.warn("[todo-reminder] unsubscribe push manager failed", error);
    }
  }

  reminderPushReady.value = false;
}

async function ensureReminderServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
    reminderPushLastError.value = "当前浏览器不支持 Service Worker";
    return null;
  }

  if (!window.isSecureContext) {
    reminderPushLastError.value = "当前页面不是安全上下文，无法注册 Service Worker";
    return null;
  }

  bindServiceWorkerMessageListener();

  if (!serviceWorkerRegistrationPromise) {
    serviceWorkerRegistrationPromise = navigator.serviceWorker
      .register("/reminder-sw.js")
      .then((registration) => {
        reminderServiceWorkerScope.value = registration.scope || "";
        console.info(`${LOG_PREFIX} service worker registered`, {
          scope: registration.scope,
        });
        return registration;
      })
      .catch((error) => {
        reminderServiceWorkerScope.value = "";
        reminderPushLastError.value = resolveErrorMessage(error, "Service Worker 注册失败");
        console.warn("[todo-reminder] service worker register failed", error);
        return null;
      });
  }

  return serviceWorkerRegistrationPromise;
}

function bindServiceWorkerMessageListener(): void {
  if (serviceWorkerMessageBound) {
    return;
  }
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
    return;
  }

  navigator.serviceWorker.addEventListener("message", (event: MessageEvent<unknown>) => {
    const rawData = event.data;
    if (!isRecord(rawData)) {
      return;
    }

    const type = typeof rawData.type === "string" ? rawData.type : "";
    if (type !== "WEB_PUSH_MESSAGE") {
      return;
    }

    const payload = normalizeWebPushPayload(rawData.payload);
    console.info(`${LOG_PREFIX} received message from service worker`, {
      eventId: payload.eventId,
      notificationId: payload.notificationId,
      msgType: payload.msgType,
      priority: payload.priority,
      route: payload.route,
    });
    webPushMessageSubscribers.forEach((subscriber) => {
      try {
        subscriber(payload);
      } catch (error) {
        console.warn(`${LOG_PREFIX} web push subscriber execution failed`, error);
      }
    });
  });

  serviceWorkerMessageBound = true;
  console.info(`${LOG_PREFIX} service worker message listener bound`);
}

function urlBase64ToArrayBuffer(base64String: string): ArrayBuffer {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; i += 1) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray.buffer;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === "object";
}

function normalizeWebPushPayload(raw: unknown): WebPushMessagePayload {
  const data = isRecord(raw) ? raw : {};
  const priority = normalizePriority(data.priority);
  return {
    eventId: normalizeString(data.eventId),
    notificationId: normalizeString(data.notificationId),
    msgType: normalizeString(data.msgType) || "SYSTEM_EVENT",
    title: normalizeString(data.title) || "消息提醒",
    content: normalizeString(data.content) || "您有一条新消息",
    route: normalizeRoute(data.route),
    priority,
    panelAutoOpen: resolveBoolean(data.panelAutoOpen, priority !== "LOW"),
    osNotificationEnabled: resolveBoolean(data.osNotificationEnabled, priority === "HIGH"),
  };
}

function normalizePriority(raw: unknown): string {
  const value = normalizeString(raw).toUpperCase();
  if (value === "HIGH" || value === "MEDIUM" || value === "LOW") {
    return value;
  }
  return "LOW";
}

function normalizeRoute(raw: unknown): string {
  const value = normalizeString(raw).trim();
  if (!value) {
    return "";
  }
  if (value === "/todo-all") {
    return "/todo/all";
  }
  if (!value.startsWith("/")) {
    return "";
  }
  return value;
}

function normalizeString(raw: unknown): string {
  if (typeof raw === "string") {
    return raw.trim();
  }
  if (typeof raw === "number") {
    return String(raw);
  }
  return "";
}

function resolveBoolean(raw: unknown, fallback: boolean): boolean {
  if (typeof raw === "boolean") {
    return raw;
  }
  if (typeof raw === "string") {
    const normalized = raw.trim().toLowerCase();
    if (normalized === "true") {
      return true;
    }
    if (normalized === "false") {
      return false;
    }
  }
  return fallback;
}

function resolveEndpointHost(endpoint: string): string {
  try {
    return new URL(endpoint).host;
  } catch (_error) {
    return "unknown";
  }
}

function resolveErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}
