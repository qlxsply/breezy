import { clearAuthToken, getAuthToken } from "./auth-storage";
import { API_BASE_URL } from "./env";
import { message } from "./message";
import type { ApiResponse } from "./types";

function isDirectBody(body: unknown): body is BodyInit {
  return (
    body instanceof Blob ||
    body instanceof FormData ||
    body instanceof URLSearchParams ||
    typeof body === "string"
  );
}

function resolveBody(body: unknown): BodyInit | undefined {
  if (body === undefined || body === null) {
    return undefined;
  }
  if (isDirectBody(body)) {
    return body;
  }
  return JSON.stringify(body);
}

function buildRequestHeaders(init?: RequestInit, hasJsonBody = true): Headers {
  const headers = new Headers(init?.headers ?? {});
  if (hasJsonBody && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const token = getAuthToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return headers;
}

async function parseErrorResponse(resp: Response): Promise<never> {
  if (resp.status === 401) {
    clearAuthToken();
  } else {
    message.error(`网络请求异常 (HTTP ${resp.status})`);
  }
  throw new Error(`HTTP ${resp.status} ${resp.statusText}`);
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const body = resolveBody(init?.body);
  const headers = buildRequestHeaders(init, !(body instanceof FormData));

  try {
    const resp = await fetch(`${API_BASE_URL}${url}`, {
      ...init,
      body,
      headers,
    });

    if (!resp.ok) {
      return parseErrorResponse(resp);
    }

    const json = (await resp.json()) as ApiResponse<T>;
    if (!json.success) {
      const errorMsg = json.msg || `API Error code=${json.code}`;
      message.error(errorMsg);
      throw new Error(errorMsg);
    }
    return json.data;
  } catch (error) {
    if (error instanceof TypeError) {
      message.error("网络连接异常，请检查您的网络连接");
    }
    throw error;
  }
}

async function requestRaw<T>(url: string, init?: RequestInit): Promise<T> {
  const body = resolveBody(init?.body);
  const headers = buildRequestHeaders(init, !(body instanceof FormData));

  try {
    const resp = await fetch(`${API_BASE_URL}${url}`, {
      ...init,
      body,
      headers,
    });

    if (!resp.ok) {
      return parseErrorResponse(resp);
    }

    if (resp.status === 204) {
      return undefined as T;
    }

    return (await resp.json()) as T;
  } catch (error) {
    if (error instanceof TypeError) {
      message.error("网络连接异常，请检查您的网络连接");
    }
    throw error;
  }
}

export function get<T>(url: string): Promise<T> {
  return request<T>(url);
}

export function post<T>(url: string, body: unknown): Promise<T> {
  return request<T>(url, { method: "POST", body: resolveBody(body) });
}

export function put<T>(url: string, body: unknown): Promise<T> {
  return request<T>(url, { method: "PUT", body: resolveBody(body) });
}

export function del<T>(url: string): Promise<T> {
  return request<T>(url, { method: "DELETE" });
}

export function getRaw<T>(url: string): Promise<T> {
  return requestRaw<T>(url);
}

export function postRaw<T>(url: string, body: unknown): Promise<T> {
  return requestRaw<T>(url, { method: "POST", body: resolveBody(body) });
}

export function putRaw<T>(url: string, body: unknown): Promise<T> {
  return requestRaw<T>(url, { method: "PUT", body: resolveBody(body) });
}

export function delRaw<T>(url: string): Promise<T> {
  return requestRaw<T>(url, { method: "DELETE" });
}
