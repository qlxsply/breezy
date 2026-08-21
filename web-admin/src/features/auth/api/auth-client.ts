import { get, post, put, type RequestOptions } from "@admin/shared/transport";
import type { UserConfigItem } from "@admin/shared/types/user-config";

import type { AuthUser, AuthUserType } from "../model/types";

interface AuthUserPayload {
  id: string | null;
  account: string | null;
  userType?: AuthUserType | null;
  configs?: UserConfigItem[];
}

interface AdminLoginPayload {
  sessionExpiresAt: string;
  user: AuthUserPayload;
}

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
  const account = payload.account || "";
  return {
    id: payload.id,
    username: account,
    account,
    userType: payload.userType || "GUEST",
    configs: payload.configs,
  };
}
