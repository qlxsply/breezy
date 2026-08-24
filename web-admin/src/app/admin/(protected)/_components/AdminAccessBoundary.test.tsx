import { setAuthAnonymous, setAuthenticated } from "@admin/features/auth/model/auth-store";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminAccessBoundary } from "./AdminAccessBoundary";

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
  navigation.replace.mockReset();
  setAuthAnonymous();
});

describe("AdminAccessBoundary", () => {
  it("redirects an anonymous visitor back to login with the requested path", async () => {
    setAuthAnonymous();
    render(<AdminAccessBoundary>protected content</AdminAccessBoundary>);

    expect(screen.getByRole("heading", { name: "正在跳转到后台登录页" })).toBeInTheDocument();
    await waitFor(() =>
      expect(navigation.replace).toHaveBeenCalledWith("/admin/login?redirect=%2Fadmin%2Fusers"),
    );
  });

  it("redirects an authenticated non-admin session without exposing children", async () => {
    setAuthenticated({
      id: "user-1",
      account: "member",
      userType: "USER",
      mustChangePassword: false,
    });
    render(<AdminAccessBoundary>protected content</AdminAccessBoundary>);

    expect(screen.queryByText("protected content")).not.toBeInTheDocument();
    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith("/admin/login"));
  });
});
