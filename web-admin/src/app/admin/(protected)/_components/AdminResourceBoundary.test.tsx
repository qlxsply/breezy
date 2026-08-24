import {
  resetResourceStore,
  setResourcesLoading,
  setResourcesReady,
} from "@admin/features/resources/model/resource-store";
import type { ResourceEntry } from "@admin/features/resources/model/types";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminResourceBoundary } from "./AdminResourceBoundary";

const navigation = vi.hoisted(() => ({
  pathname: "/admin/users",
  replace: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  usePathname: () => navigation.pathname,
  useRouter: () => ({ replace: navigation.replace }),
}));

afterEach(() => {
  cleanup();
  navigation.pathname = "/admin/users";
  navigation.replace.mockReset();
  resetResourceStore();
});

describe("AdminResourceBoundary", () => {
  it("allows self-service pages before resources are initialized", () => {
    navigation.pathname = "/admin/profile";

    render(<AdminResourceBoundary>profile content</AdminResourceBoundary>);

    expect(screen.getByText("profile content")).toBeInTheDocument();
  });

  it("shows resource synchronization feedback while loading", () => {
    setResourcesLoading();

    render(<AdminResourceBoundary>protected content</AdminResourceBoundary>);

    expect(screen.getByRole("heading", { name: "正在同步后台资源" })).toBeInTheDocument();
    expect(screen.queryByText("protected content")).not.toBeInTheDocument();
  });

  it("allows a loaded page only when its inherited menu chain is accessible", () => {
    setResourcesReady([
      resource({ id: "system", code: "system", openMode: "NONE", url: "" }),
      resource({ id: "users", parentId: "system", code: "users", url: "/admin/users" }),
    ]);

    render(<AdminResourceBoundary>users content</AdminResourceBoundary>);

    expect(screen.getByText("users content")).toBeInTheDocument();
  });

  it("renders 403 feedback and returns to the dashboard for an inaccessible page", () => {
    setResourcesReady([]);
    render(<AdminResourceBoundary>protected content</AdminResourceBoundary>);

    expect(screen.getByRole("heading", { name: "无权限访问当前页面" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "返回工作台" }));
    expect(navigation.replace).toHaveBeenCalledWith("/admin");
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
