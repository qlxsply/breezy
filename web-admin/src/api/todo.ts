import type {
  TodoAttachmentMeta,
  TodoCompleteReq,
  TodoDailyDetail,
  TodoDailyStats,
  TodoPageReq,
  TodoPageResult,
  TodoReminderPollResult,
  TodoReorderReq,
  TodoSaveReq,
} from "../types/todo";
import { getAuthToken } from "../utils/authStorage";
import { API_BASE_URL, del, get, post, put } from "./http";

const TODO_BASE = "/todo";
const TODO_ATTACHMENT_BASE = "/todo/attachments";
const REMINDER_BASE = "/reminder";

interface ApiResponse<T> {
  success: boolean;
  code: string;
  msg: string;
  data: T;
}

export function listTodos(req: TodoPageReq): Promise<TodoPageResult> {
  return post<TodoPageResult>(`${TODO_BASE}/page`, req);
}

export function createTodo(req: TodoSaveReq): Promise<number> {
  return post<number>(TODO_BASE, req);
}

export function updateTodo(id: number, req: TodoSaveReq): Promise<void> {
  return put<void>(`${TODO_BASE}/${id}`, req);
}

export function deleteTodo(id: number): Promise<void> {
  return del<void>(`${TODO_BASE}/${id}`);
}

export function pauseTodo(id: number): Promise<void> {
  return post<void>(`${TODO_BASE}/${id}/pause`, {});
}

export function resumeTodo(id: number): Promise<void> {
  return post<void>(`${TODO_BASE}/${id}/resume`, {});
}

export function quickCompleteTodo(id: number): Promise<void> {
  return post<void>(`${TODO_BASE}/${id}/complete/quick`, {});
}

export function completeTodo(id: number, req: TodoCompleteReq): Promise<void> {
  return post<void>(`${TODO_BASE}/${id}/complete`, req);
}

export function reorderTodos(req: TodoReorderReq): Promise<void> {
  return post<void>(`${TODO_BASE}/reorder`, req);
}

export function getTodoDailyStats(startDate: string, endDate: string): Promise<TodoDailyStats[]> {
  const query = new URLSearchParams({ startDate, endDate });
  return get<TodoDailyStats[]>(`${TODO_BASE}/stats/daily?${query.toString()}`);
}

export function getTodoDailyDetail(date: string): Promise<TodoDailyDetail> {
  return get<TodoDailyDetail>(`${TODO_BASE}/stats/daily/${encodeURIComponent(date)}`);
}

export async function uploadTodoAttachment(file: File): Promise<string> {
  const token = getAuthToken();
  const form = new FormData();
  form.append("file", file);

  const response = await fetch(`${API_BASE_URL}${TODO_ATTACHMENT_BASE}/upload`, {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    body: form,
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} ${response.statusText}`);
  }
  const json = (await response.json()) as ApiResponse<string>;
  if (!json.success) {
    throw new Error(json.msg || `API Error: ${json.code}`);
  }
  return json.data;
}

export function getTodoAttachmentMetadata(ids: string[]): Promise<TodoAttachmentMeta[]> {
  return post<TodoAttachmentMeta[]>(`${TODO_ATTACHMENT_BASE}/metadata`, ids);
}

export async function fetchTodoAttachmentView(fileId: string): Promise<Blob> {
  const token = getAuthToken();
  const response = await fetch(
    `${API_BASE_URL}${TODO_ATTACHMENT_BASE}/${encodeURIComponent(fileId)}/view`,
    {
      method: "GET",
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    },
  );
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} ${response.statusText}`);
  }
  return response.blob();
}

export function pollTodoReminders(startTime: string): Promise<TodoReminderPollResult> {
  return post<TodoReminderPollResult>(`${REMINDER_BASE}/outbox/poll`, { startTime });
}

export function ackTodoReminder(id: number): Promise<void> {
  return post<void>(`${REMINDER_BASE}/outbox/${id}/ack`, {});
}

export function ackReminderDelivery(deliveryId: number): Promise<void> {
  return post<void>(`${REMINDER_BASE}/deliveries/${deliveryId}/ack`, {});
}
