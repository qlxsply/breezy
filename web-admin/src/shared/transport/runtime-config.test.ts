import { describe, expect, it } from "vitest";

import { parseRuntimeConfig } from "./runtime-config";

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
});
