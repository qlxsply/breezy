import { post, put, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type { NotificationItem, NotificationPage } from "../model/types";

const BASE = "/notifications";

export function listNotifications(
  params: { status: "all" | "unread"; page: number; size: number },
  options?: RequestOptions,
): Promise<PageResult<NotificationItem> | NotificationPage | NotificationItem[]> {
  return post<PageResult<NotificationItem> | NotificationPage | NotificationItem[]>(
    `${BASE}/page`,
    {
      status: params.status,
      page: { pageNo: params.page, pageSize: params.size },
    },
    options,
  );
}

export function markNotificationRead(id: string): Promise<boolean> {
  return put<boolean>(`${BASE}/${encodeURIComponent(id)}/read`, {});
}

export function markAllNotificationsRead(): Promise<boolean> {
  return put<boolean>(`${BASE}/read-all`, {});
}
