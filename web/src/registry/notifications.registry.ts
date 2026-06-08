// /src/registry/notifications.registry.ts
import { ref } from "vue";

import {
  listNotifications,
  markAllRead as markAllReadApi,
  markRead as markReadApi,
} from "../api/notifications";
import type {
  NotificationItem,
  NotificationPriority,
  NotificationType,
} from "../types/notification";
import { getAuthToken } from "../utils/authStorage";
import type { SseMessage } from "../utils/sse-coordinator";
import { onSseMessage } from "./sse.registry";
import { onWebPushMessage, type WebPushMessagePayload } from "./todo-reminder.registry";

const unreadCount = ref(0);
const unreadList = ref<NotificationItem[]>([]);
const pollingEnabled = ref(true);

const attentionSignal = ref(0);
const attentionNotificationId = ref("");

const UNREAD_PAGE_SIZE = 100;
const MAX_UNREAD_FETCH_PAGES = 50;

let initialized = false;
let loadingPromise: Promise<void> | null = null;

onSseMessage((msg) => {
  applyIncomingSseMessage(msg, "SSE");
});

onWebPushMessage((payload) => {
  applyIncomingWebPushMessage(payload);
});

export function useUnreadCount() {
  return unreadCount;
}

export function useUnreadList() {
  return unreadList;
}

export function useNotificationPollingEnabled() {
  return pollingEnabled;
}

export function useNotificationAttentionSignal() {
  return attentionSignal;
}

export function useNotificationAttentionNotificationId() {
  return attentionNotificationId;
}

export async function refreshUnread(_limit = 6): Promise<void> {
  await ensureUnreadLoaded(true);
}

export async function ensureUnreadLoaded(force = false): Promise<void> {
  const token = getAuthToken();
  if (!token) {
    resetUnreadState();
    initialized = false;
    return;
  }

  if (!force && initialized) {
    return;
  }
  if (loadingPromise) {
    await loadingPromise;
    return;
  }

  loadingPromise = (async () => {
    try {
      const allUnread = await fetchAllUnreadPages();
      unreadList.value = allUnread;
      unreadCount.value = allUnread.length;
      initialized = true;
    } catch (err) {
      console.warn("[notifications] load unread failed", err);
      resetUnreadState();
    } finally {
      loadingPromise = null;
    }
  })();

  await loadingPromise;
}

export function startNotificationPolling() {
  // Polling disabled in favor of SSE
}

export function stopNotificationPolling() {
  // Polling disabled in favor of SSE
}

export function enableNotificationPolling() {
  if (pollingEnabled.value) return;
  pollingEnabled.value = true;
}

export function disableNotificationPolling() {
  if (!pollingEnabled.value) return;
  pollingEnabled.value = false;
}

export function toggleNotificationPolling() {
  if (pollingEnabled.value) {
    disableNotificationPolling();
  } else {
    enableNotificationPolling();
  }
}

export async function markAllRead(): Promise<void> {
  if (!getAuthToken()) {
    resetUnreadState();
    return;
  }

  const snapshot = unreadList.value;
  unreadList.value = [];
  unreadCount.value = 0;
  try {
    await markAllReadApi();
  } catch (err) {
    console.warn("[notifications] mark all read failed", err);
    unreadList.value = snapshot;
    unreadCount.value = snapshot.length;
  }
}

export async function markRead(id: string): Promise<void> {
  if (!getAuthToken()) {
    resetUnreadState();
    return;
  }

  const targetId = id.trim();
  if (!targetId) {
    return;
  }

  const snapshot = unreadList.value;
  unreadList.value = snapshot.filter((item) => item.id !== targetId);
  unreadCount.value = unreadList.value.length;

  try {
    await markReadApi(targetId);
  } catch (err) {
    console.warn("[notifications] mark read failed", err);
    unreadList.value = snapshot;
    unreadCount.value = snapshot.length;
  }
}

export async function pullUnreadBatch(): Promise<void> {
  await ensureUnreadLoaded(false);
}

function applyIncomingSseMessage(msg: SseMessage, source: "SSE" | "WEB_PUSH"): void {
  if (!getAuthToken()) {
    return;
  }

  console.info("[notifications] realtime message received", {
    source,
    eventId: msg.eventId,
    notificationId: msg.notificationId,
    msgType: msg.msgType,
    priority: msg.priority,
    route: msg.route,
  });

  const item = convertSseToNotification(msg);
  if (!item) {
    return;
  }

  upsertUnread(item);
  unreadCount.value = unreadList.value.length;

  if (msg.panelAutoOpen || shouldAutoOpenByPriority(item.priority)) {
    triggerAttention(item.id);
  }
}

function applyIncomingWebPushMessage(payload: WebPushMessagePayload): void {
  const msg: SseMessage = {
    eventId: payload.eventId,
    notificationId: payload.notificationId,
    msgType: payload.msgType,
    title: payload.title,
    content: payload.content,
    route: payload.route,
    priority: payload.priority,
    panelAutoOpen: payload.panelAutoOpen,
    osNotificationEnabled: payload.osNotificationEnabled,
  };
  applyIncomingSseMessage(msg, "WEB_PUSH");
}

function convertSseToNotification(msg: SseMessage): NotificationItem | null {
  const notificationId = (msg.notificationId || "").trim();
  if (!notificationId) {
    return null;
  }

  const title = (msg.title || "").trim();
  const content = msg.content || "";
  const type = ((msg.msgType || "").trim().toUpperCase() || "SYSTEM_EVENT") as NotificationType;
  const priority = normalizePriority(msg.priority);
  const route = normalizeRoute(msg.route);

  return {
    id: notificationId,
    title: title || "消息提醒",
    content,
    type,
    priority,
    route,
    createdAt: new Date().toISOString(),
    read: false,
  };
}

function upsertUnread(next: NotificationItem): void {
  const map = new Map<string, NotificationItem>();
  unreadList.value.forEach((item) => {
    map.set(item.id, item);
  });
  map.set(next.id, next);
  unreadList.value = Array.from(map.values()).sort(
    (a, b) => resolveCreatedAtMs(b) - resolveCreatedAtMs(a),
  );
}

function normalizePriority(raw: string | null | undefined): NotificationPriority {
  const value = (raw || "LOW").trim().toUpperCase();
  if (value === "HIGH" || value === "MEDIUM" || value === "LOW") {
    return value;
  }
  return "LOW";
}

function shouldAutoOpenByPriority(priority: NotificationPriority): boolean {
  return priority === "MEDIUM" || priority === "HIGH";
}

function normalizeRoute(raw: string | null | undefined): string {
  const value = (raw || "").trim();
  if (!value) return "";
  if (value === "/todo-all") return "/todo/all";
  if (!value.startsWith("/")) return "";
  return value;
}

async function fetchAllUnreadPages(): Promise<NotificationItem[]> {
  const merged: NotificationItem[] = [];

  for (let pageNo = 1; pageNo <= MAX_UNREAD_FETCH_PAGES; pageNo += 1) {
    const payload = await listNotifications({
      status: "unread",
      page: pageNo,
      size: UNREAD_PAGE_SIZE,
    });
    const { items, hasMore } = normalizePagePayload(payload);
    if (items.length > 0) {
      merged.push(...items);
    }
    if (!hasMore) {
      break;
    }
  }

  const dedup = new Map<string, NotificationItem>();
  merged.forEach((item) => {
    dedup.set(item.id, item);
  });
  return Array.from(dedup.values()).sort((a, b) => resolveCreatedAtMs(b) - resolveCreatedAtMs(a));
}

function normalizePagePayload(payload: unknown): { items: NotificationItem[]; hasMore: boolean } {
  if (Array.isArray(payload)) {
    return {
      items: payload as NotificationItem[],
      hasMore: false,
    };
  }

  if (payload && typeof payload === "object" && "elements" in payload) {
    const page = payload as {
      elements?: NotificationItem[];
      pageNo?: number;
      totalPages?: number;
    };
    const items = Array.isArray(page.elements) ? page.elements : [];
    const pageNo = Number(page.pageNo || 1);
    const totalPages = Number(page.totalPages || pageNo);
    return {
      items,
      hasMore: pageNo < totalPages,
    };
  }

  if (payload && typeof payload === "object" && "items" in payload) {
    const page = payload as {
      items?: NotificationItem[];
      hasMore?: boolean;
    };
    const items = Array.isArray(page.items) ? page.items : [];
    return {
      items,
      hasMore: Boolean(page.hasMore),
    };
  }

  return { items: [], hasMore: false };
}

function resolveCreatedAtMs(item: NotificationItem): number {
  if (!item.createdAt) return 0;
  const time = Date.parse(item.createdAt);
  return Number.isFinite(time) ? time : 0;
}

function triggerAttention(notificationId: string): void {
  if (typeof document !== "undefined" && document.visibilityState !== "visible") {
    return;
  }
  attentionNotificationId.value = notificationId;
  attentionSignal.value += 1;
}

function resetUnreadState(): void {
  unreadCount.value = 0;
  unreadList.value = [];
}
