import { getAuthSessionSnapshot, setAuthenticated } from "@admin/features/auth/model/auth-store";
import {
  getResourceSnapshot,
  setResourcesReady,
} from "@admin/features/resources/model/resource-store";
import { stopAdminRuntime } from "@admin/runtime/admin-runtime";
import { get, registerUnauthorizedHandler } from "@admin/shared/transport";
import { afterEach, describe, expect, it, vi } from "vitest";

import { endSession } from "./session-service";

vi.mock("@admin/runtime/admin-runtime", () => ({
  stopAdminRuntime: vi.fn().mockResolvedValue(undefined),
}));
vi.mock("@admin/features/notifications/service/notification-service", () => ({
  resetNotifications: vi.fn(),
}));

afterEach(() => {
  vi.mocked(stopAdminRuntime).mockClear();
  vi.unstubAllGlobals();
});

describe("endSession", () => {
  it("stops runtime and clears authentication and resource stores after a 401", async () => {
    setAuthenticated({
      id: "admin-1",
      account: "admin",
      userType: "ADMIN",
      mustChangePassword: false,
    });
    setResourcesReady([
      {
        id: "users",
        parentId: null,
        name: "用户",
        code: "users",
        type: "MENU",
        scope: "SETTING",
        openMode: "PAGE",
        url: "/admin/users",
        orderNo: 1,
        level: "SYSTEM",
        enabled: true,
        guestAccess: false,
      },
    ]);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ msg: "登录状态已失效" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );
    const unregister = registerUnauthorizedHandler(() =>
      endSession({ redirectToLogin: false, broadcast: false }),
    );

    await expect(get("/admin/users")).rejects.toMatchObject({ kind: "http", status: 401 });

    expect(stopAdminRuntime).toHaveBeenCalledWith({ unsubscribeLocalPush: true });
    expect(getAuthSessionSnapshot()).toMatchObject({ status: "anonymous", user: null });
    expect(getResourceSnapshot()).toMatchObject({ status: "idle", items: [] });
    unregister();
  });
});
