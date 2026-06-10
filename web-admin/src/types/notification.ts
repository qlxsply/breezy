// /src/types/notification.ts

export type NotificationType = "TODO_REMINDER" | "SYSTEM_EVENT" | "BUSINESS_EVENT" | string;

export type NotificationPriority = "LOW" | "MEDIUM" | "HIGH" | string;

export interface NotificationItem {
  id: string;
  title: string;
  content?: string;
  type: NotificationType;
  priority: NotificationPriority;
  route?: string;
  createdAt?: string;
  read: boolean;
}

export interface NotificationPage {
  items: NotificationItem[];
  hasMore?: boolean;
}

export interface NotificationPullResult {
  items: NotificationItem[];
  lastPullAt: number;
}
