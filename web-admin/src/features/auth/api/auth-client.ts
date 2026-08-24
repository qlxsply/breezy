import { get, post, put, type RequestOptions } from "@admin/shared/transport";

import type { AuthUser } from "../model/types";
import type { AdminLoginPayload, AuthUserPayload } from "./payload";

export async function loginAdmin(
  account: string,
  password: string,
  options?: RequestOptions,
): Promise<AuthUser> {
  const payload = await post<AdminLoginPayload>(
    "/admin/auth/login",
    { account, password },
    options,
  );
  if (!payload.user) throw new Error("登录响应缺少用户信息");
  return toAuthUser(payload.user);
}

export async function getCurrentAdmin(options?: RequestOptions): Promise<AuthUser | null> {
  const payload = await get<AuthUserPayload | null>("/admin/auth/me", options);
  return payload ? toAuthUser(payload) : null;
}

export function logoutAdmin(options?: RequestOptions): Promise<boolean> {
  return post<boolean>("/admin/auth/logout", {}, options);
}

export function changeAdminPassword(
  oldPassword: string,
  newPassword: string,
  options?: RequestOptions,
): Promise<boolean> {
  return put<boolean>("/admin/auth/password", { oldPassword, newPassword }, options);
}

function toAuthUser(payload: AuthUserPayload): AuthUser {
  const id = String(payload.id ?? "");
  const account = payload.account || "";
  if (!id || !account || payload.userType !== "ADMIN") throw new Error("认证用户信息无效");
  return {
    id,
    account,
    userType: payload.userType,
    mustChangePassword: Boolean(payload.mustChangePassword),
    configs: Array.isArray(payload.configs) ? payload.configs : [],
  };
}
