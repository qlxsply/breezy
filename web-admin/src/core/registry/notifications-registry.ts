"use client";

import { listNotifications,markAllRead as markAllReadApi, markRead as markReadApi } from "@admin/api/notifications";
import { getAuthToken } from "@admin/core/auth-storage";
import { createStore, useStoreValue } from "@admin/core/client-store";
import { onSseMessage, type SseMessage } from "@admin/core/registry/sse-registry";
import { onWebPushMessage, type WebPushMessagePayload } from "@admin/core/registry/todo-reminder-registry";
import type { NotificationItem, NotificationPriority, NotificationType } from "@admin/types/notification";

interface NotificationsState {
  unreadCount: number;
  unreadList: NotificationItem[];
  pollingEnabled: boolean;
  attentionSignal: number;
  attentionNotificationId: string;
}

const UNREAD_PAGE_SIZE = 100;
const MAX_UNREAD_FETCH_PAGES = 50;

const notificationsStore = createStore<NotificationsState>({
  unreadCount: 0,
  unreadList: [],
  pollingEnabled: true,
  attentionSignal: 0,
  attentionNotificationId: "",
});

let initialized = false;
let loadingPromise: Promise<void> | null = null;
let subscribersBound = false;

export function bindNotificationRealtime(): void {
  if (subscribersBound) return;
  subscribersBound = true;
  onSseMessage((msg) => applyIncomingSseMessage(msg, "SSE"));
  onWebPushMessage((payload) => applyIncomingWebPushMessage(payload));
}

export function useUnreadCount(): number {
  return useStoreValue(notificationsStore, (state) => state.unreadCount);
}

export function useUnreadList(): NotificationItem[] {
  return useStoreValue(notificationsStore, (state) => state.unreadList);
}

export function useNotificationPollingEnabled(): boolean {
  return useStoreValue(notificationsStore, (state) => state.pollingEnabled);
}

export function useNotificationAttentionSignal(): number {
  return useStoreValue(notificationsStore, (state) => state.attentionSignal);
}

export function useNotificationAttentionNotificationId(): string {
  return useStoreValue(notificationsStore, (state) => state.attentionNotificationId);
}

export function getUnreadPreviewList(limit = 4): NotificationItem[] {
  return notificationsStore.getState().unreadList.slice(0, limit);
}

export async function refreshUnread(): Promise<void> {
  await ensureUnreadLoaded(true);
}

export async function ensureUnreadLoaded(force = false): Promise<void> {
  const token = getAuthToken();
  if (!token) {
    resetUnreadState();
    initialized = false;
    return;
  }
  if (!force && initialized) return;
  if (loadingPromise) {
    await loadingPromise;
    return;
  }

  loadingPromise = (async () => {
    try {
      const allUnread = await fetchAllUnreadPages();
      notificationsStore.setState((state) => ({ ...state, unreadList: allUnread, unreadCount: allUnread.length }));
      initialized = true;
    } catch (error) {
      console.warn("[notifications] load unread failed", error);
      resetUnreadState();
    } finally {
      loadingPromise = null;
    }
  })();

  await loadingPromise;
}

export function enableNotificationPolling() {
  notificationsStore.setState((state) => ({ ...state, pollingEnabled: true }));
}

export function disableNotificationPolling() {
  notificationsStore.setState((state) => ({ ...state, pollingEnabled: false }));
}

export async function markAllRead(): Promise<void> {
  if (!getAuthToken()) {
    resetUnreadState();
    return;
  }

  const snapshot = notificationsStore.getState().unreadList;
  notificationsStore.setState((state) => ({ ...state, unreadList: [], unreadCount: 0 }));
  try {
    await markAllReadApi();
  } catch (error) {
    console.warn("[notifications] mark all read failed", error);
    notificationsStore.setState((state) => ({ ...state, unreadList: snapshot, unreadCount: snapshot.length }));
  }
}

export async function markRead(id: string): Promise<void> {
  if (!getAuthToken()) {
    resetUnreadState();
    return;
  }
  const targetId = id.trim();
  if (!targetId) return;

  const snapshot = notificationsStore.getState().unreadList;
  const next = snapshot.filter((item) => item.id !== targetId);
  notificationsStore.setState((state) => ({ ...state, unreadList: next, unreadCount: next.length }));
  try {
    await markReadApi(targetId);
  } catch (error) {
    console.warn("[notifications] mark read failed", error);
    notificationsStore.setState((state) => ({ ...state, unreadList: snapshot, unreadCount: snapshot.length }));
  }
}

function applyIncomingSseMessage(msg: SseMessage, source: "SSE" | "WEB_PUSH"): void {
  if (!getAuthToken()) return;
  console.warn("[notifications] realtime message received", { source, eventId: msg.eventId, notificationId: msg.notificationId, msgType: msg.msgType });
  const item = convertSseToNotification(msg);
  if (!item) return;
  const nextList = upsertUnread(notificationsStore.getState().unreadList, item);
  notificationsStore.setState((state) => ({ ...state, unreadList: nextList, unreadCount: nextList.length }));
  if (msg.panelAutoOpen || shouldAutoOpenByPriority(item.priority)) {
    triggerAttention(item.id);
  }
}

function applyIncomingWebPushMessage(payload: WebPushMessagePayload): void {
  applyIncomingSseMessage(
    {
      eventId: payload.eventId,
      notificationId: payload.notificationId,
      msgType: payload.msgType,
      title: payload.title,
      content: payload.content,
      route: payload.route,
      priority: payload.priority,
      panelAutoOpen: payload.panelAutoOpen,
      osNotificationEnabled: payload.osNotificationEnabled,
    },
    "WEB_PUSH",
  );
}

function convertSseToNotification(msg: SseMessage): NotificationItem | null {
  const notificationId = (msg.notificationId || "").trim();
  if (!notificationId) return null;
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

function upsertUnread(current: NotificationItem[], next: NotificationItem): NotificationItem[] {
  const map = new Map<string, NotificationItem>();
  current.forEach((item) => map.set(item.id, item));
  map.set(next.id, next);
  return Array.from(map.values()).sort((a, b) => resolveCreatedAtMs(b) - resolveCreatedAtMs(a));
}

function normalizePriority(raw: string | null | undefined): NotificationPriority {
  const value = (raw || "LOW").trim().toUpperCase();
  if (value === "HIGH" || value === "MEDIUM" || value === "LOW") return value;
  return "LOW";
}

function shouldAutoOpenByPriority(priority: NotificationPriority): boolean {
  return priority === "MEDIUM" || priority === "HIGH";
}

function normalizeRoute(raw: string | null | undefined): string {
  const value = (raw || "").trim();
  if (!value) return "";
  return value.startsWith("/") ? value : `/${value}`;
}

function resolveCreatedAtMs(item: NotificationItem): number {
  const raw = item.createdAt?.trim();
  if (!raw) return 0;
  const ts = Date.parse(raw);
  return Number.isFinite(ts) ? ts : 0;
}

function resetUnreadState() {
  notificationsStore.setState((state) => ({ ...state, unreadList: [], unreadCount: 0 }));
}

function triggerAttention(notificationId: string) {
  notificationsStore.setState((state) => ({
    ...state,
    attentionSignal: state.attentionSignal + 1,
    attentionNotificationId: notificationId,
  }));
}

async function fetchAllUnreadPages(): Promise<NotificationItem[]> {
  const results: NotificationItem[] = [];
  for (let page = 0; page < MAX_UNREAD_FETCH_PAGES; page += 1) {
    const batch = await listNotifications({ status: "unread", page, size: UNREAD_PAGE_SIZE });
    const items = normalizeNotificationPageItems(batch);
    if (items.length === 0) break;
    results.push(...items.filter((item) => !item.read));
    if (items.length < UNREAD_PAGE_SIZE) break;
  }
  return results.sort((a, b) => resolveCreatedAtMs(b) - resolveCreatedAtMs(a));
}

function normalizeNotificationPageItems(payload: unknown): NotificationItem[] {
  if (Array.isArray(payload)) {
    return payload as NotificationItem[];
  }
  if (payload && typeof payload === "object") {
    const record = payload as Record<string, unknown>;
    if (Array.isArray(record.items)) return record.items as NotificationItem[];
    if (Array.isArray(record.elements)) return record.elements as NotificationItem[];
  }
  return [];
}
