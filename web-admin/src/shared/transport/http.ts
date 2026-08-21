import { resolveApiUrl } from "@admin/shared/transport/runtime-config";

export interface ApiEnvelope<T> {
  success: boolean;
  code: string;
  msg: string;
  timestamp: string;
  data: T;
}

export type ResponseMode = "envelope" | "json" | "blob" | "response";

export interface RequestOptions {
  signal?: AbortSignal;
  timeoutMs?: number;
  headers?: HeadersInit;
  cache?: RequestCache;
  waitForSessionTermination?: boolean;
}

export class TransportError extends Error {
  constructor(
    message: string,
    readonly kind: "http" | "api" | "network" | "timeout" | "invalid-response",
    readonly status?: number,
    readonly code?: string,
    options?: ErrorOptions,
  ) {
    super(message, options);
    this.name = "TransportError";
  }
}

export class HttpError extends TransportError {
  constructor(message: string, status: number, options?: ErrorOptions) {
    super(message, "http", status, undefined, options);
    this.name = "HttpError";
  }
}

export class ApiError extends TransportError {
  constructor(message: string, status: number, code: string) {
    super(message, "api", status, code);
    this.name = "ApiError";
  }
}

type UnauthorizedHandler = () => void | Promise<void>;

const DEFAULT_TIMEOUT_MS = 30_000;
const CSRF_COOKIE = "__Host-breezy-admin-csrf";
const CSRF_HEADER = "X-CSRF-Token";
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

let unauthorizedHandler: UnauthorizedHandler | null = null;
let unauthorizedPromise: Promise<void> | null = null;
let csrfPromise: Promise<string> | null = null;

export function registerUnauthorizedHandler(handler: UnauthorizedHandler): () => void {
  unauthorizedHandler = handler;
  return () => {
    if (unauthorizedHandler === handler) unauthorizedHandler = null;
  };
}

function isObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function parseEnvelope<T>(payload: unknown): ApiEnvelope<T> {
  if (
    !isObject(payload) ||
    typeof payload.success !== "boolean" ||
    typeof payload.code !== "string" ||
    typeof payload.msg !== "string" ||
    typeof payload.timestamp !== "string" ||
    !("data" in payload)
  ) {
    throw new TransportError("服务端返回了无效的响应结构", "invalid-response");
  }
  return payload as unknown as ApiEnvelope<T>;
}

function readCookie(name: string): string {
  if (typeof document === "undefined") return "";
  const prefix = `${encodeURIComponent(name)}=`;
  const item = document.cookie
    .split(";")
    .map((value) => value.trim())
    .find((value) => value.startsWith(prefix));
  return item ? decodeURIComponent(item.slice(prefix.length)) : "";
}

async function ensureCsrfToken(): Promise<string> {
  const existing = readCookie(CSRF_COOKIE);
  if (existing) return existing;

  if (!csrfPromise) {
    csrfPromise = request<boolean>("/admin/auth/csrf", { method: "GET" }, "envelope", {
      skipCsrf: true,
      waitForSessionTermination: false,
    })
      .then(() => {
        const token = readCookie(CSRF_COOKIE);
        if (!token) {
          throw new TransportError("CSRF 令牌未写入浏览器", "invalid-response");
        }
        return token;
      })
      .finally(() => {
        csrfPromise = null;
      });
  }
  return csrfPromise;
}

async function handleUnauthorized(): Promise<void> {
  if (!unauthorizedHandler) return;
  if (!unauthorizedPromise) {
    unauthorizedPromise = Promise.resolve(unauthorizedHandler()).finally(() => {
      unauthorizedPromise = null;
    });
  }
  await unauthorizedPromise;
}

function createAbortContext(signal: AbortSignal | undefined, timeoutMs: number) {
  const controller = new AbortController();
  let timedOut = false;
  const onAbort = () => controller.abort(signal?.reason);
  signal?.addEventListener("abort", onAbort, { once: true });
  if (signal?.aborted) controller.abort(signal.reason);
  const timer = window.setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, timeoutMs);
  return {
    signal: controller.signal,
    didTimeOut: () => timedOut,
    dispose: () => {
      window.clearTimeout(timer);
      signal?.removeEventListener("abort", onAbort);
    },
  };
}

function prepareBody(body: unknown): BodyInit | undefined {
  if (body === undefined) return undefined;
  if (
    typeof body === "string" ||
    body instanceof FormData ||
    body instanceof Blob ||
    body instanceof URLSearchParams ||
    body instanceof ArrayBuffer ||
    ArrayBuffer.isView(body)
  ) {
    return body as BodyInit;
  }
  return JSON.stringify(body);
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const payload: unknown = await response.clone().json();
    if (isObject(payload) && typeof payload.msg === "string" && payload.msg.trim()) {
      return payload.msg;
    }
  } catch {
    // The HTTP status remains the authoritative fallback for non-JSON errors.
  }
  return `请求失败（HTTP ${response.status}）`;
}

interface InternalOptions extends RequestOptions {
  skipCsrf?: boolean;
}

async function request<T>(
  endpoint: string,
  init: RequestInit,
  mode: ResponseMode,
  options: InternalOptions = {},
): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(options.headers);
  headers.set("Accept", mode === "blob" ? "application/octet-stream, */*" : "application/json");

  const body = prepareBody(init.body);
  if (body && !(body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (!options.skipCsrf && !SAFE_METHODS.has(method)) {
    headers.set(CSRF_HEADER, await ensureCsrfToken());
  }

  options.signal?.throwIfAborted();
  const abort = createAbortContext(options.signal, options.timeoutMs ?? DEFAULT_TIMEOUT_MS);
  try {
    const response = await fetch(resolveApiUrl(endpoint), {
      ...init,
      body,
      headers,
      cache: options.cache ?? "no-store",
      credentials: "same-origin",
      signal: abort.signal,
    });

    if (response.status === 401) {
      const termination = handleUnauthorized();
      if (options.waitForSessionTermination === false) {
        void termination.catch((error: unknown) => {
          console.error("[transport] session termination failed", error);
        });
      } else {
        await termination;
      }
    }
    if (!response.ok) {
      throw new HttpError(await errorMessage(response), response.status);
    }
    if (mode === "response") return response as T;
    if (mode === "blob") return (await response.blob()) as T;
    if (response.status === 204) return undefined as T;

    let payload: unknown;
    try {
      payload = await response.json();
    } catch (cause) {
      throw new TransportError(
        "服务端未返回有效 JSON",
        "invalid-response",
        response.status,
        undefined,
        {
          cause,
        },
      );
    }
    if (mode === "json") return payload as T;

    const envelope = parseEnvelope<T>(payload);
    if (!envelope.success) {
      throw new ApiError(envelope.msg || "请求失败", response.status, envelope.code);
    }
    return envelope.data;
  } catch (error) {
    if (error instanceof TransportError) throw error;
    if (abort.didTimeOut()) {
      throw new TransportError("请求超时", "timeout", undefined, undefined, { cause: error });
    }
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new TransportError("网络请求失败", "network", undefined, undefined, { cause: error });
  } finally {
    abort.dispose();
  }
}

export const get = <T>(endpoint: string, options?: RequestOptions) =>
  request<T>(endpoint, { method: "GET" }, "envelope", options);

export const post = <T>(endpoint: string, body?: unknown, options?: RequestOptions) =>
  request<T>(endpoint, { method: "POST", body: body as BodyInit }, "envelope", options);

export const put = <T>(endpoint: string, body?: unknown, options?: RequestOptions) =>
  request<T>(endpoint, { method: "PUT", body: body as BodyInit }, "envelope", options);

export const patch = <T>(endpoint: string, body?: unknown, options?: RequestOptions) =>
  request<T>(endpoint, { method: "PATCH", body: body as BodyInit }, "envelope", options);

export const del = <T>(endpoint: string, body?: unknown, options?: RequestOptions) =>
  request<T>(endpoint, { method: "DELETE", body: body as BodyInit }, "envelope", options);

export const getJson = <T>(endpoint: string, options?: RequestOptions) =>
  request<T>(endpoint, { method: "GET" }, "json", options);

export const getBlob = (endpoint: string, options?: RequestOptions) =>
  request<Blob>(endpoint, { method: "GET" }, "blob", options);

export const getResponse = (endpoint: string, options?: RequestOptions) =>
  request<Response>(endpoint, { method: "GET" }, "response", options);
