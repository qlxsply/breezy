// /src/api/auth.ts
import type { UserConfigItem } from "./configs";
import { get, post, put } from "./http";

export type AuthUserType = "INTERNAL" | "EXTERNAL" | "GUEST";
export type AuthSpace = "internal" | "external";

export interface AuthUser {
  id: string | null;
  username: string | null;
  account?: string | null;
  userType: AuthUserType;
  configs?: UserConfigItem[];
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

interface AuthUserPayload {
  id: string | null;
  account: string | null;
  userType?: AuthUserType | null;
  configs?: UserConfigItem[];
}

interface LoginResponsePayload {
  token: string;
  user: AuthUserPayload;
}

function authBase(space: AuthSpace): string {
  return space === "internal" ? "/admin/auth" : "/auth";
}

export async function login(
  space: AuthSpace,
  username: string,
  password: string,
): Promise<LoginResponse> {
  const payload = await post<LoginResponsePayload>(`${authBase(space)}/login`, {
    account: username,
    password,
  });
  return {
    token: payload.token,
    user: toAuthUser(payload.user),
  };
}

export async function getMe(space: AuthSpace): Promise<AuthUser | null> {
  const payload = await get<AuthUserPayload | null>(`${authBase(space)}/me`);
  if (!payload) return null;
  return toAuthUser(payload);
}

export function logout(space: AuthSpace): Promise<boolean> {
  return post<boolean>(`${authBase(space)}/logout`, {});
}

export function changePassword(
  space: AuthSpace,
  oldPassword: string,
  newPassword: string,
): Promise<boolean> {
  return put<boolean>(`${authBase(space)}/password`, { oldPassword, newPassword });
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
