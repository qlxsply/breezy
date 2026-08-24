import { afterEach, describe, expect, it, vi } from "vitest";

import { parseRuntimeConfig } from "./runtime-config";

afterEach(() => {
  vi.resetModules();
  vi.unstubAllGlobals();
});

describe("parseRuntimeConfig", () => {
  it("accepts same-origin paths and removes trailing slashes", () => {
    expect(
      parseRuntimeConfig({
        apiBaseUrl: "/api/",
        bootstrapPath: "/admin/menu-resources/",
      }),
    ).toEqual({
      apiBaseUrl: "/api",
      bootstrapPath: "/admin/menu-resources",
    });
  });

  it.each([
    "https://example.com/api",
    "//example.com/api",
    "api",
    "/api?target=external",
    "/api#external",
    "/api\\external",
  ])("rejects unsafe API path %s", (apiBaseUrl) => {
    expect(() => parseRuntimeConfig({ apiBaseUrl })).toThrow();
  });

  it("rejects unknown configuration fields", () => {
    expect(() =>
      parseRuntimeConfig({ apiBaseUrl: "/api", bootstrapUrl: "https://example.com" }),
    ).toThrow("运行时配置包含未知字段：bootstrapUrl");
  });

  it("coalesces concurrent initialization into one request", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ apiBaseUrl: "/backend", bootstrapPath: "/resources" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const { ensureRuntimeConfigLoaded } = await import("./runtime-config");

    const first = ensureRuntimeConfigLoaded();
    const second = ensureRuntimeConfigLoaded();

    expect(second).toBe(first);
    await expect(Promise.all([first, second])).resolves.toEqual([
      { apiBaseUrl: "/backend", bootstrapPath: "/resources" },
      { apiBaseUrl: "/backend", bootstrapPath: "/resources" },
    ]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("clears failed initialization so a later call can retry", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 500 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({}), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );
    vi.stubGlobal("fetch", fetchMock);
    const { ensureRuntimeConfigLoaded } = await import("./runtime-config");

    await expect(ensureRuntimeConfigLoaded()).rejects.toThrow("运行时配置加载失败：HTTP 500");
    await expect(ensureRuntimeConfigLoaded()).resolves.toEqual({
      apiBaseUrl: "/api",
      bootstrapPath: "/admin/menu-resources",
    });
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
