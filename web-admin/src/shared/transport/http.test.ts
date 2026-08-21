import { afterEach, describe, expect, it, vi } from "vitest";

import { get, post, registerUnauthorizedHandler } from "./http";

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
