import type { PageQuery, PageResult } from "@admin/shared/types/pagination";

export type TodoStatus = "TODO" | "PAUSED" | "DONE";

export interface TodoItem {
  id: number;
  content: string;
  dueTime?: string | null;
  status: TodoStatus;
  note?: string | null;
  contentAttachmentFileIds: string[];
  completionAttachmentFileIds: string[];
  sortNo: number;
  completionNote?: string | null;
  completedAt?: string | null;
  createdAt?: string | null;
}

export interface TodoSaveReq {
  content: string;
  dueTime?: string | null;
  note?: string | null;
  contentAttachmentFileIds: string[];
  completionAttachmentFileIds: string[];
}

export interface TodoCompleteReq {
  completionNote?: string | null;
  completionAttachmentFileIds: string[];
}

export interface TodoPageReq extends PageQuery {
  status?: TodoStatus | null;
  contentLike?: string;
}

export interface TodoReorderReq {
  groups: Array<{
    status: TodoStatus;
    orderedIds: number[];
  }>;
}

export interface TodoDailyStats {
  date: string;
  createdCount: number;
  completedCount: number;
}

export interface TodoDailyDetail {
  date: string;
  createdItems: TodoItem[];
  completedItems: TodoItem[];
}

export interface TodoAttachmentMeta {
  id: string;
  name: string;
  size?: number | null;
  contentType?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface TodoReminderItem {
  id: number;
  sourceType: "TODO" | "SCHEDULE";
  sourceId: number;
  title: string;
  body?: string | null;
  dueTime?: string | null;
  eventTimeZoneId?: string | null;
  route?: string | null;
}

export interface TodoReminderPollResult {
  cutoffTime: string;
  items: TodoReminderItem[];
}

export interface TodoAttachmentDraftItem {
  localId: string;
  file: File;
  name: string;
  previewUrl: string;
}

export interface TodoAttachmentPreviewItem {
  key: string;
  name: string;
  previewUrl?: string;
  removable?: boolean;
  source: "saved" | "draft";
  fileId?: string;
  localId?: string;
}

export type TodoPageResult = PageResult<TodoItem>;
