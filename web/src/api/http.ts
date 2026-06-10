// /src/api/http.ts

import {
  ensureValidAccessToken,
  handleUnauthorizedResponse,
} from "../registry/auth-token.registry";
import { getAuthToken } from "../utils/authStorage";
import { message as toast } from "../utils/message";

/**
 * 约定：后端成功统一返回 ApiResponse.ok(data)
 * 典型结构（示例）：
 * { "success": true, "code": "0", "msg": "OK", "data": ... }
 */
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  msg: string;
  data: T;
}

/**
 * 这里的 baseURL 你按 breezy 后端实际情况改：
 * - 如果前后端同域部署，通常为空即可
 * - 如果后端是 /api 前缀，把 baseURL 改成 "/api"
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8910/api";
const baseURL = API_BASE_URL;

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  await ensureValidAccessToken();
  const token = getAuthToken();
  const headers = new Headers(init?.headers ?? {});
  if (!headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  try {
    const resp = await fetch(baseURL + url, {
      ...init,
      headers,
    });

    if (!resp.ok) {
      if (resp.status === 401) {
        handleUnauthorizedResponse();
      }
      if (resp.status !== 401) {
        toast.error(`网络请求异常 (HTTP ${resp.status})`);
      }
      throw new Error(`HTTP ${resp.status} ${resp.statusText}`);
    }

    const json = (await resp.json()) as ApiResponse<T>;
    if (!json.success) {
      const errorMsg = json.msg || `API Error code=${json.code}`;
      toast.error(errorMsg);
      throw new Error(errorMsg);
    }
    return json.data;
  } catch (err) {
    // 如果是 fetch 本身失败（比如网络断了），err 可能是 TypeError: Failed to fetch
    if (err instanceof TypeError) {
      toast.error("网络连接异常，请检查您的网络连接");
    }

    throw err;
  }
}

async function requestRaw<T>(url: string, init?: RequestInit): Promise<T> {
  await ensureValidAccessToken();
  const token = getAuthToken();
  const headers = new Headers(init?.headers ?? {});
  if (!headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  try {
    const resp = await fetch(baseURL + url, {
      ...init,
      headers,
    });

    if (!resp.ok) {
      if (resp.status === 401) {
        handleUnauthorizedResponse();
      }
      if (resp.status !== 401) {
        toast.error(`网络请求异常 (HTTP ${resp.status})`);
      }
      throw new Error(`HTTP ${resp.status} ${resp.statusText}`);
    }

    if (resp.status === 204) {
      return undefined as T;
    }

    return (await resp.json()) as T;
  } catch (err) {
    if (err instanceof TypeError) {
      toast.error("网络连接异常，请检查您的网络连接");
    }
    throw err;
  }
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

export function getRaw<T>(url: string): Promise<T> {
  return requestRaw<T>(url);
}

export function postRaw<T>(url: string, body: unknown): Promise<T> {
  return requestRaw<T>(url, { method: "POST", body: JSON.stringify(body) });
}

export function putRaw<T>(url: string, body: unknown): Promise<T> {
  return requestRaw<T>(url, { method: "PUT", body: JSON.stringify(body) });
}

export function delRaw<T>(url: string): Promise<T> {
  return requestRaw<T>(url, { method: "DELETE" });
}
