// /src/api/notifications.ts
import type {
  NotificationItem,
  NotificationPage,
  NotificationPullResult,
} from "../types/notification";
import type { PageResult } from "../types/page";
import { get, post, put } from "./http";

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
  return post<PageResult<NotificationItem> | NotificationPage | NotificationItem[]>(`${BASE}/page`, {
    status: params.status,
    page: {
      pageNo: params.page,
      pageSize: params.size,
    },
  });
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
