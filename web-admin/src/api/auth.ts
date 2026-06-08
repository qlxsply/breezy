import { get, post, put } from "@admin/api/http";

export interface AdminAuthUser {
  id: string | null;
  account: string;
  userType: "INTERNAL" | "EXTERNAL" | "GUEST";
}

interface AdminAuthUserPayload {
  id: string | null;
  account: string | null;
  userType?: "INTERNAL" | "EXTERNAL" | "GUEST" | null;
}

interface LoginPayload {
  token: string;
  user: AdminAuthUserPayload;
}

export interface AdminLoginResponse {
  token: string;
  user: AdminAuthUser;
}

export async function login(account: string, password: string): Promise<AdminLoginResponse> {
  const payload = await post<LoginPayload>("/admin/auth/login", { account, password });
  return {
    token: payload.token,
    user: toAdminAuthUser(payload.user),
  };
}

export async function getMe(): Promise<AdminAuthUser | null> {
  const payload = await get<AdminAuthUserPayload | null>("/admin/auth/me");
  return payload ? toAdminAuthUser(payload) : null;
}

export function logout(): Promise<boolean> {
  return post<boolean>("/admin/auth/logout", {});
}

export function changePassword(oldPassword: string, newPassword: string): Promise<boolean> {
  return put<boolean>("/admin/auth/password", { oldPassword, newPassword });
}

function toAdminAuthUser(payload: AdminAuthUserPayload): AdminAuthUser {
  return {
    id: payload.id,
    account: payload.account || "",
    userType: payload.userType || "GUEST",
  };
}
