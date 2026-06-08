import { getAuthToken } from "@admin/utils/authStorage";

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  msg: string;
  data: T;
}

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8910/api";

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers ?? {});
  if (!headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const token = getAuthToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...init,
    headers,
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} ${response.statusText}`);
  }
  const payload = (await response.json()) as ApiResponse<T>;
  if (!payload.success) {
    throw new Error(payload.msg || `API Error: ${payload.code}`);
  }
  return payload.data;
}

export function get<T>(url: string): Promise<T> {
  return request<T>(url);
}

export function post<T>(url: string, body: unknown): Promise<T> {
  return request<T>(url, { method: "POST", body: JSON.stringify(body) });
}

export function put<T>(url: string, body: unknown): Promise<T> {
  return request<T>(url, { method: "PUT", body: JSON.stringify(body) });
}

export function del<T>(url: string): Promise<T> {
  return request<T>(url, { method: "DELETE" });
}
