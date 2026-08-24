import { afterEach, describe, expect, it } from "vitest";

import {
  getResourceSnapshot,
  hasPermission,
  resetResourceStore,
  setResourcesReady,
} from "./resource-store";
import type { ResourceEntry } from "./types";

afterEach(resetResourceStore);

describe("resource permission inheritance", () => {
  it("requires every ancestor to exist and remain enabled", () => {
    const parent = resource({ id: "settings", code: "settings", type: "MENU" });
    const permission = resource({
      id: "user-create",
      parentId: parent.id,
      code: "user.create",
      type: "BUTTON",
      openMode: "NONE",
    });

    setResourcesReady([parent, permission]);
    expect(hasPermission(getResourceSnapshot(), " USER.CREATE ")).toBe(true);

    setResourcesReady([{ ...parent, enabled: false }, permission]);
    expect(hasPermission(getResourceSnapshot(), "user.create")).toBe(false);

    setResourcesReady([permission]);
    expect(hasPermission(getResourceSnapshot(), "user.create")).toBe(false);
  });
});

function resource(overrides: Partial<ResourceEntry>): ResourceEntry {
  return {
    id: "resource",
    parentId: null,
    name: "资源",
    code: "resource",
    type: "MENU",
    scope: "SETTING",
    openMode: "PAGE",
    url: "/admin/resource",
    orderNo: 1,
    level: "SYSTEM",
    enabled: true,
    guestAccess: false,
    ...overrides,
  };
}
