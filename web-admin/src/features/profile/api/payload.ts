import type { LoginEvent } from "@admin/shared/types/login-event";

export interface AdminProfileLoginActivityPayload {
  id?: string | number | null;
  eventType?: LoginEvent | null;
  success?: boolean | null;
  loginIp?: string | null;
  remark?: string | null;
  occurredAt?: string | null;
}

export interface AdminProfilePayload {
  id?: string | number | null;
  username?: string | null;
  nickname?: string | null;
  userType?: "ADMIN" | "SYSTEM" | "USER" | "GUEST" | null;
  status?: string | null;
  mustChangePassword?: boolean | null;
  lastPasswordChangedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  recentActivities?: AdminProfileLoginActivityPayload[] | null;
}
