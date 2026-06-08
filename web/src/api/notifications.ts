// /src/api/notifications.ts
import type {
  NotificationItem,
  NotificationPage,
  NotificationPullResult,
} from "../types/notification";
import type { PageResult } from "../types/page";
import { get, put } from "./http";

const BASE = "/notifications";

export function getUnreadCount(): Promise<number> {
  return get<number>(`${BASE}/unread/count`);
}

export function listUnread(limit = 6): Promise<NotificationItem[]> {
  return get<NotificationItem[]>(`${BASE}/unread?limit=${encodeURIComponent(limit)}`);
}

export function listNotifications(params: {
  status: "all" | "unread";
  page: number;
  size: number;
}): Promise<PageResult<NotificationItem> | NotificationPage | NotificationItem[]> {
  const query = new URLSearchParams({
    status: params.status,
    page: String(params.page),
    size: String(params.size),
  });
  return get<PageResult<NotificationItem> | NotificationPage | NotificationItem[]>(
    `${BASE}?${query.toString()}`,
  );
}

export function markRead(id: string): Promise<boolean> {
  return put<boolean>(`${BASE}/${encodeURIComponent(id)}/read`, {});
}

export function markAllRead(): Promise<boolean> {
  return put<boolean>(`${BASE}/read-all`, {});
}

export function pullUnread(after: number, limit = 50): Promise<NotificationPullResult> {
  const query = new URLSearchParams({
    after: String(after),
    limit: String(limit),
  });
  return get<NotificationPullResult>(`${BASE}/unread/pull?${query.toString()}`);
}
