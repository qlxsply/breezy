import type { LoginEvent } from "@admin/shared/types/login-event";

export interface AdminProfileLoginActivityEntry {
  id: string;
  eventType: LoginEvent;
  success: boolean;
  loginIp?: string | null;
  remark?: string | null;
  occurredAt?: string | null;
}

export interface AdminProfileEntry {
  id: string;
  username: string;
  nickname: string;
  userType: "ADMIN" | "SYSTEM" | "USER" | "GUEST";
  status: string;
  mustChangePassword: boolean;
  lastPasswordChangedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  recentActivities: AdminProfileLoginActivityEntry[];
}
