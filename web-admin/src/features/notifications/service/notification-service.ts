import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from "@admin/features/notifications/api/notification-client";
import {
  getNotificationSnapshot,
  removeUnreadNotification,
  resetNotificationStore,
  setNotificationsError,
  setNotificationsLoading,
  setNotificationsReady,
  upsertUnreadNotification,
} from "@admin/features/notifications/model/notification-store";
import type {
  NotificationItem,
  NotificationPriority,
  NotificationType,
  RealtimeNotificationMessage,
} from "@admin/features/notifications/model/types";

const PAGE_SIZE = 100;
const MAX_PAGES = 50;
const MAX_EVENT_IDS = 500;

let loadPromise: Promise<void> | null = null;
let notificationGeneration = 0;
const readIds = new Set<string>();
const eventIds = new Set<string>();
const eventIdQueue: string[] = [];

export function loadUnreadNotifications(
  options: { force?: boolean; signal?: AbortSignal } = {},
): Promise<void> {
  const state = getNotificationSnapshot();
  if (!options.force && state.status === "ready") return Promise.resolve();
  if (!options.force && loadPromise) return loadPromise;

  const generation = ++notificationGeneration;
  setNotificationsLoading();
  loadPromise = fetchAllUnread(options.signal)
    .then((loaded) => {
      if (generation !== notificationGeneration) return;
      const current = getNotificationSnapshot().unreadList;
      const merged = new Map<string, NotificationItem>();
      loaded.forEach((item) => {
        if (!item.read && !readIds.has(item.id)) merged.set(item.id, item);
      });
      current.forEach((item) => {
        if (!readIds.has(item.id)) merged.set(item.id, item);
      });
      setNotificationsReady([...merged.values()]);
    })
    .catch((error: unknown) => {
      if (generation !== notificationGeneration) return;
      if (error instanceof DOMException && error.name === "AbortError") throw error;
      setNotificationsError(error instanceof Error ? error.message : "未读通知加载失败");
      throw error;
    })
    .finally(() => {
      if (generation === notificationGeneration) loadPromise = null;
    });
  return loadPromise;
}

export function refreshUnreadNotifications(signal?: AbortSignal): Promise<void> {
  return loadUnreadNotifications({ force: true, signal });
}

export async function markRead(id: string): Promise<void> {
  const targetId = id.trim();
  if (!targetId) return;
  const removed = removeUnreadNotification(targetId);
  readIds.add(targetId);
  try {
    await markNotificationRead(targetId);
  } catch (error) {
    readIds.delete(targetId);
    if (removed && !getNotificationSnapshot().unreadList.some((item) => item.id === targetId)) {
      upsertUnreadNotification(removed);
    }
    console.warn("[notifications] mark read failed", error);
  }
}

export async function markAllRead(): Promise<void> {
  const removed = [...getNotificationSnapshot().unreadList];
  removed.forEach((item) => readIds.add(item.id));
  setNotificationsReady([]);
  try {
    await markAllNotificationsRead();
  } catch (error) {
    removed.forEach((item) => readIds.delete(item.id));
    const current = getNotificationSnapshot().unreadList;
    const restored = new Map([...removed, ...current].map((item) => [item.id, item]));
    setNotificationsReady([...restored.values()]);
    console.warn("[notifications] mark all read failed", error);
  }
}

export function acceptRealtimeNotification(message: RealtimeNotificationMessage): void {
  const eventId = message.eventId.trim();
  if (eventId && eventIds.has(eventId)) return;
  if (eventId) rememberEventId(eventId);

  const id = message.notificationId.trim();
  if (!id || readIds.has(id)) return;
  upsertUnreadNotification({
    id,
    title: message.title.trim() || "消息提醒",
    content: message.content,
    type: (message.msgType.trim().toUpperCase() || "SYSTEM_EVENT") as NotificationType,
    priority: normalizePriority(message.priority),
    route: normalizeRoute(message.route),
    createdAt: new Date().toISOString(),
    read: false,
  });
}

export function resetNotifications(): void {
  notificationGeneration += 1;
  loadPromise = null;
  readIds.clear();
  eventIds.clear();
  eventIdQueue.length = 0;
  resetNotificationStore();
}

async function fetchAllUnread(signal?: AbortSignal): Promise<NotificationItem[]> {
  const results: NotificationItem[] = [];
  for (let page = 0; page < MAX_PAGES; page += 1) {
    const payload = await listNotifications(
      { status: "unread", page, size: PAGE_SIZE },
      { signal },
    );
    const items = normalizePageItems(payload);
    if (items.length === 0) break;
    results.push(...items.filter((item) => !item.read));
    if (items.length < PAGE_SIZE) break;
  }
  return results;
}

function normalizePageItems(payload: unknown): NotificationItem[] {
  if (Array.isArray(payload)) return payload.filter(isNotificationItem);
  if (!payload || typeof payload !== "object") return [];
  const record = payload as Record<string, unknown>;
  const items = Array.isArray(record.items)
    ? record.items
    : Array.isArray(record.elements)
      ? record.elements
      : [];
  return items.filter(isNotificationItem);
}

function isNotificationItem(value: unknown): value is NotificationItem {
  if (!value || typeof value !== "object") return false;
  const record = value as Record<string, unknown>;
  return (
    typeof record.id === "string" &&
    typeof record.title === "string" &&
    typeof record.type === "string" &&
    typeof record.priority === "string" &&
    typeof record.read === "boolean"
  );
}

function normalizePriority(value: string): NotificationPriority {
  const normalized = value.trim().toUpperCase();
  return normalized === "HIGH" || normalized === "MEDIUM" ? normalized : "LOW";
}

function normalizeRoute(value: string): string {
  const route = value.trim();
  if (!route) return "";
  return route.startsWith("/") ? route : `/${route}`;
}

function rememberEventId(eventId: string): void {
  eventIds.add(eventId);
  eventIdQueue.push(eventId);
  while (eventIdQueue.length > MAX_EVENT_IDS) {
    const oldest = eventIdQueue.shift();
    if (oldest) eventIds.delete(oldest);
  }
}
