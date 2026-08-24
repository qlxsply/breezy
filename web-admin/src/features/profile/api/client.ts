import { get, put, type RequestOptions } from "@admin/shared/transport";

import type { AdminProfileEntry, AdminProfileLoginActivityEntry } from "../model/types";
import type { AdminProfileLoginActivityPayload, AdminProfilePayload } from "./payload";

type Options = Pick<RequestOptions, "signal">;

function toActivity(payload: AdminProfileLoginActivityPayload): AdminProfileLoginActivityEntry {
  return {
    id: String(payload.id ?? ""),
    eventType: payload.eventType || "LOGIN_FAILURE",
    success: Boolean(payload.success),
    loginIp: payload.loginIp,
    remark: payload.remark,
    occurredAt: payload.occurredAt,
  };
}

function toProfile(payload: AdminProfilePayload): AdminProfileEntry {
  return {
    id: String(payload.id ?? ""),
    username: payload.username || "",
    nickname: payload.nickname || "",
    userType: payload.userType || "GUEST",
    status: payload.status || "",
    mustChangePassword: Boolean(payload.mustChangePassword),
    lastPasswordChangedAt: payload.lastPasswordChangedAt,
    createdAt: payload.createdAt,
    updatedAt: payload.updatedAt,
    recentActivities: Array.isArray(payload.recentActivities)
      ? payload.recentActivities.map(toActivity)
      : [],
  };
}

export async function getAdminProfile(options?: Options): Promise<AdminProfileEntry> {
  return toProfile(await get<AdminProfilePayload>("/admin/profile", options));
}

export async function updateAdminProfile(nickname: string): Promise<AdminProfileEntry> {
  return toProfile(await put<AdminProfilePayload>("/admin/profile", { nickname }));
}
