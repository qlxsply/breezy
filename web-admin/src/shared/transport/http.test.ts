import { afterEach, describe, expect, it, vi } from "vitest";

import {
  type ApiError,
  get,
  getBlob,
  getJson,
  getResponse,
  type HttpError,
  post,
  registerUnauthorizedHandler,
} from "./http";

function envelope<T>(data: T) {
  return {
    success: true,
    code: "SUCCESS",
    msg: "成功",
    timestamp: "2026-08-20T00:00:00Z",
    data,
  };
}

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("HTTP transport", () => {
  it("sends same-origin cookies and the CSRF header for unsafe requests", async () => {
    vi.spyOn(document, "cookie", "get").mockReturnValue("__Host-breezy-admin-csrf=csrf-token");
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(envelope(true)), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(post<boolean>("/admin/auth/logout", {})).resolves.toBe(true);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("/api/admin/auth/logout");
    expect(init.credentials).toBe("same-origin");
    expect(new Headers(init.headers).get("X-CSRF-Token")).toBe("csrf-token");
    expect(init.headers).not.toHaveProperty("Authorization");
  });

  it("coalesces concurrent 401 session termination", async () => {
    let releaseHandler: () => void = () => {};
    const handlerGate = new Promise<void>((resolve) => {
      releaseHandler = resolve;
    });
    const handler = vi.fn(() => handlerGate);
    const unregister = registerUnauthorizedHandler(handler);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ msg: "未登录" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    const first = get("/first");
    const second = get("/second");
    await vi.waitFor(() => expect(handler).toHaveBeenCalledTimes(1));
    releaseHandler();

    const results = await Promise.allSettled([first, second]);
    expect(results.every((result) => result.status === "rejected")).toBe(true);
    unregister();
  });

  it("rejects malformed API envelopes", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ success: true, data: true }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(get("/invalid")).rejects.toMatchObject({
      kind: "invalid-response",
    });
  });

  it("classifies timeout and network failures", async () => {
    vi.useFakeTimers();
    vi.stubGlobal(
      "fetch",
      vi.fn(
        (_url: string, init: RequestInit) =>
          new Promise<Response>((_resolve, reject) => {
            init.signal?.addEventListener("abort", () =>
              reject(new DOMException("", "AbortError")),
            );
          }),
      ),
    );

    const timeoutAssertion = expect(get("/slow", { timeoutMs: 20 })).rejects.toMatchObject({
      kind: "timeout",
      message: "请求超时",
    });
    await vi.advanceTimersByTimeAsync(20);
    await timeoutAssertion;

    vi.useRealTimers();
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("connection refused")));
    await expect(get("/offline")).rejects.toMatchObject({
      kind: "network",
      message: "网络请求失败",
    });
  });

  it("distinguishes business API errors from ordinary HTTP errors", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            ...envelope(null),
            success: false,
            code: "RESOURCE_FORBIDDEN",
            msg: "无权操作",
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ msg: "服务暂不可用" }), {
          status: 503,
          headers: { "Content-Type": "application/json" },
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(get("/business-error")).rejects.toEqual(
      expect.objectContaining<ApiError>({
        name: "ApiError",
        kind: "api",
        code: "RESOURCE_FORBIDDEN",
        message: "无权操作",
      }),
    );
    await expect(get("/http-error")).rejects.toEqual(
      expect.objectContaining<HttpError>({
        name: "HttpError",
        kind: "http",
        status: 503,
        message: "服务暂不可用",
      }),
    );
  });

  it("returns envelope data, plain JSON, blobs, and raw responses in their requested modes", async () => {
    const rawResponse = new Response("raw", { status: 200 });
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(
          new Response(JSON.stringify(envelope({ id: "admin" })), {
            headers: { "Content-Type": "application/json" },
          }),
        )
        .mockResolvedValueOnce(
          new Response(JSON.stringify({ id: "plain" }), {
            headers: { "Content-Type": "application/json" },
          }),
        )
        .mockResolvedValueOnce(new Response("export-content"))
        .mockResolvedValueOnce(rawResponse),
    );

    await expect(get<{ id: string }>("/envelope")).resolves.toEqual({ id: "admin" });
    await expect(getJson<{ id: string }>("/json")).resolves.toEqual({ id: "plain" });
    await expect(getBlob("/blob").then((blob) => blob.text())).resolves.toBe("export-content");
    await expect(getResponse("/response")).resolves.toBe(rawResponse);
  });

  it("preserves caller cancellation", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn((_url: string, init: RequestInit) => {
        return new Promise<Response>((_resolve, reject) => {
          init.signal?.addEventListener("abort", () =>
            reject(new DOMException("Aborted", "AbortError")),
          );
        });
      }),
    );
    const controller = new AbortController();
    const pending = get("/slow", { signal: controller.signal });
    controller.abort();

    await expect(pending).rejects.toMatchObject({ name: "AbortError" });
  });

  it("does not bind shared CSRF initialization to the first caller signal", async () => {
    let csrfCookie = "";
    vi.spyOn(document, "cookie", "get").mockImplementation(() => csrfCookie);
    let resolveCsrf: (response: Response) => void = () => {};
    const csrfResponse = new Promise<Response>((resolve) => {
      resolveCsrf = resolve;
    });
    const fetchMock = vi.fn((url: string) => {
      if (url.endsWith("/admin/auth/csrf")) return csrfResponse;
      return Promise.resolve(
        new Response(JSON.stringify(envelope(true)), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);
    const firstController = new AbortController();

    const first = post<boolean>("/first", {}, { signal: firstController.signal });
    const second = post<boolean>("/second", {});
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    firstController.abort();
    csrfCookie = "__Host-breezy-admin-csrf=shared-token";
    resolveCsrf(
      new Response(JSON.stringify(envelope(true)), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    await expect(first).rejects.toMatchObject({ name: "AbortError" });
    await expect(second).resolves.toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
