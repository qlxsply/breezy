"use client";

import { useStoreValue } from "@admin/shared/hooks/useStoreValue";
import { createStore } from "@admin/shared/lib/store";

import type { NotificationItem } from "./types";

export type NotificationStatus = "idle" | "loading" | "ready" | "error";

interface NotificationState {
  status: NotificationStatus;
  unreadList: readonly NotificationItem[];
  error: string | null;
}

const notificationStore = createStore<NotificationState>({
  status: "idle",
  unreadList: [],
  error: null,
});

export function getNotificationSnapshot(): NotificationState {
  return notificationStore.getState();
}

export function setNotificationsLoading(): void {
  notificationStore.setState((state) => ({ ...state, status: "loading", error: null }));
}

export function setNotificationsReady(items: readonly NotificationItem[]): void {
  notificationStore.setState({ status: "ready", unreadList: sortUnread(items), error: null });
}

export function setNotificationsError(error: string): void {
  notificationStore.setState((state) => ({ ...state, status: "error", error }));
}

export function upsertUnreadNotification(item: NotificationItem): void {
  const current = notificationStore.getState().unreadList;
  const byId = new Map(current.map((notification) => [notification.id, notification]));
  byId.set(item.id, item);
  setNotificationsReady([...byId.values()]);
}

export function removeUnreadNotification(id: string): NotificationItem | undefined {
  const current = notificationStore.getState().unreadList;
  const removed = current.find((item) => item.id === id);
  if (removed) setNotificationsReady(current.filter((item) => item.id !== id));
  return removed;
}

export function resetNotificationStore(): void {
  notificationStore.setState({ status: "idle", unreadList: [], error: null });
}

export function useUnreadCount(): number {
  return useStoreValue(notificationStore, (state) => state.unreadList.length);
}

export function useUnreadList(): readonly NotificationItem[] {
  return useStoreValue(notificationStore, (state) => state.unreadList);
}

function sortUnread(items: readonly NotificationItem[]): NotificationItem[] {
  return [...items].sort((a, b) => createdAtMillis(b) - createdAtMillis(a));
}

function createdAtMillis(item: NotificationItem): number {
  const timestamp = item.createdAt ? Date.parse(item.createdAt) : 0;
  return Number.isFinite(timestamp) ? timestamp : 0;
}
