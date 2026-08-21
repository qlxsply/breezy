import {
  getPushPublicKey,
  removePushSubscription,
  savePushSubscription,
} from "@admin/features/notifications/api/push-client";
import type { RealtimeNotificationMessage } from "@admin/features/notifications/model/types";

import { getPushDeviceId } from "./device-id";
import { type PushPermission, setPushState } from "./push-store";

interface ActivePushRuntime {
  generation: number;
  userId: string;
  onMessage: (message: RealtimeNotificationMessage) => void;
}

let pushGeneration = 0;
let activeRuntime: ActivePushRuntime | null = null;
let registrationPromise: Promise<ServiceWorkerRegistration | null> | null = null;
let messageHandler: ((event: MessageEvent<unknown>) => void) | null = null;
const pendingSubscriptionSyncs = new Set<Promise<void>>();
let lifecycleQueue: Promise<void> = Promise.resolve();

export function startWebPushRuntime(
  userId: string,
  onMessage: (message: RealtimeNotificationMessage) => void,
): Promise<void> {
  return enqueueLifecycle(() => startRuntime(userId, onMessage));
}

async function startRuntime(
  userId: string,
  onMessage: (message: RealtimeNotificationMessage) => void,
): Promise<void> {
  const normalizedUserId = userId.trim();
  if (!normalizedUserId || typeof window === "undefined") return;
  if (activeRuntime?.userId === normalizedUserId) {
    activeRuntime.onMessage = onMessage;
    return;
  }

  const generation = ++pushGeneration;
  detachActiveRuntime();
  await waitForSubscriptionSyncs();
  if (generation !== pushGeneration) return;
  activeRuntime = { generation, userId: normalizedUserId, onMessage };
  const permission = currentPermission();
  if (!window.isSecureContext || !("serviceWorker" in navigator) || permission === "unsupported") {
    setPushState({
      status: "unavailable",
      permission,
      serviceWorkerScope: "",
      error: "当前浏览器环境不支持 Web Push",
    });
    return;
  }

  setPushState({ status: "starting", permission, serviceWorkerScope: "", error: null });
  bindMessageListener();
  try {
    const registration = await ensureServiceWorker();
    if (!registration || !isCurrent(generation)) return;
    if (permission === "granted") await runSubscriptionSync(registration, generation);
    if (!isCurrent(generation)) return;
    setPushState({
      status: permission === "granted" ? "ready" : "unavailable",
      permission,
      serviceWorkerScope: registration.scope,
      error: permission === "denied" ? "浏览器通知权限已被拒绝" : null,
    });
  } catch (error) {
    if (!isCurrent(generation)) return;
    setPushState({
      status: "error",
      permission: currentPermission(),
      serviceWorkerScope: "",
      error: error instanceof Error ? error.message : "Web Push 初始化失败",
    });
  }
}

export async function requestWebPushPermission(): Promise<PushPermission> {
  if (typeof window === "undefined" || !("Notification" in window)) return "unsupported";
  const permission = await Notification.requestPermission();
  const runtime = activeRuntime;
  if (permission === "granted" && runtime) {
    const registration = await ensureServiceWorker();
    if (registration && isCurrent(runtime.generation)) {
      await runSubscriptionSync(registration, runtime.generation);
      if (!isCurrent(runtime.generation)) return permission;
      setPushState({
        status: "ready",
        permission,
        serviceWorkerScope: registration.scope,
        error: null,
      });
    }
  }
  return permission;
}

export function stopWebPushRuntime(
  options: { removeServerSubscription?: boolean; unsubscribeLocal?: boolean } = {},
): Promise<void> {
  return enqueueLifecycle(() => stopRuntime(options));
}

async function stopRuntime(options: {
  removeServerSubscription?: boolean;
  unsubscribeLocal?: boolean;
}): Promise<void> {
  const generation = ++pushGeneration;
  detachActiveRuntime();
  await waitForSubscriptionSyncs();
  if (generation !== pushGeneration) return;
  try {
    if (options.removeServerSubscription) {
      await removePushSubscription(getPushDeviceId());
    }
  } catch (error) {
    console.warn("[web-push] remove server subscription failed", error);
  }
  if (options.unsubscribeLocal) {
    try {
      const registration = await registrationPromise;
      if (registration && "pushManager" in registration) {
        await (await registration.pushManager.getSubscription())?.unsubscribe();
      }
    } catch (error) {
      console.warn("[web-push] unsubscribe local subscription failed", error);
    }
  }
  setPushState({
    status: "stopped",
    permission: currentPermission(),
    serviceWorkerScope: "",
    error: null,
  });
}

function enqueueLifecycle(operation: () => Promise<void>): Promise<void> {
  const result = lifecycleQueue.then(operation, operation);
  lifecycleQueue = result.catch(() => undefined);
  return result;
}

async function ensureServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (registrationPromise) return registrationPromise;
  registrationPromise = navigator.serviceWorker.register("/sw.js").catch((error) => {
    registrationPromise = null;
    throw error;
  });
  return registrationPromise;
}

async function syncSubscription(
  registration: ServiceWorkerRegistration,
  generation: number,
): Promise<void> {
  if (!("pushManager" in registration)) throw new Error("当前浏览器不支持 PushManager");
  const publicKey = (await getPushPublicKey()).publicKey;
  if (!isCurrent(generation)) return;
  let subscription = await registration.pushManager.getSubscription();
  let created = false;
  if (!subscription) {
    subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToArrayBuffer(publicKey),
    });
    created = true;
  }
  if (!isCurrent(generation)) {
    if (created) await subscription.unsubscribe();
    return;
  }
  const value = subscription.toJSON();
  if (!value.endpoint || !value.keys?.p256dh || !value.keys.auth) {
    throw new Error("浏览器 PushSubscription 数据不完整");
  }
  await savePushSubscription({
    deviceId: getPushDeviceId(),
    endpoint: value.endpoint,
    p256dh: value.keys.p256dh,
    auth: value.keys.auth,
  });
}

function runSubscriptionSync(
  registration: ServiceWorkerRegistration,
  generation: number,
): Promise<void> {
  const promise = syncSubscription(registration, generation);
  pendingSubscriptionSyncs.add(promise);
  void promise.then(
    () => pendingSubscriptionSyncs.delete(promise),
    () => pendingSubscriptionSyncs.delete(promise),
  );
  return promise;
}

async function waitForSubscriptionSyncs(): Promise<void> {
  if (pendingSubscriptionSyncs.size === 0) return;
  await Promise.allSettled([...pendingSubscriptionSyncs]);
}

function detachActiveRuntime(): void {
  activeRuntime = null;
  unbindMessageListener();
}

function bindMessageListener(): void {
  if (messageHandler || !("serviceWorker" in navigator)) return;
  messageHandler = (event) => {
    const message = parsePushMessage(event.data);
    if (message) activeRuntime?.onMessage(message);
  };
  navigator.serviceWorker.addEventListener("message", messageHandler);
}

function unbindMessageListener(): void {
  if (messageHandler && "serviceWorker" in navigator) {
    navigator.serviceWorker.removeEventListener("message", messageHandler);
  }
  messageHandler = null;
}

function parsePushMessage(raw: unknown): RealtimeNotificationMessage | null {
  if (!raw || typeof raw !== "object") return null;
  const record = raw as Record<string, unknown>;
  if (record.type !== "BREEZY_WEB_PUSH_MESSAGE") return null;
  const notificationId = stringValue(record.notificationId);
  if (!notificationId) return null;
  return {
    eventId: stringValue(record.eventId),
    notificationId,
    msgType: stringValue(record.msgType),
    title: stringValue(record.title),
    content: stringValue(record.content),
    route: stringValue(record.route),
    priority: stringValue(record.priority) || "LOW",
    panelAutoOpen: record.panelAutoOpen === true,
    osNotificationEnabled: record.osNotificationEnabled === true,
  };
}

function currentPermission(): PushPermission {
  return typeof window !== "undefined" && "Notification" in window
    ? Notification.permission
    : "unsupported";
}

function isCurrent(generation: number): boolean {
  return activeRuntime?.generation === generation && pushGeneration === generation;
}

function stringValue(value: unknown): string {
  return typeof value === "string" || typeof value === "number" ? String(value).trim() : "";
}

function urlBase64ToArrayBuffer(value: string): ArrayBuffer {
  const padding = "=".repeat((4 - (value.length % 4)) % 4);
  const base64 = (value + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = window.atob(base64);
  return Uint8Array.from(raw, (character) => character.charCodeAt(0)).buffer;
}
