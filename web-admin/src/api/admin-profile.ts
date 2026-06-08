import { get, put } from "@admin/api/http";
import type { LoginEvent } from "@admin/types/login-log";

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
  userType: "INTERNAL" | "SYSTEM" | "EXTERNAL" | "GUEST";
  status: string;
  mustChangePassword: boolean;
  lastPasswordChangedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  recentActivities: AdminProfileLoginActivityEntry[];
}

interface AdminProfilePayload extends AdminProfileEntry {}

export interface AdminProfileLoginActivityPageResult {
  pageNo: number;
  pageSize: number;
  numberOfElements: number;
  totalPages: number;
  totalElements: number;
  elements: AdminProfileLoginActivityEntry[];
}

export function getAdminProfile(): Promise<AdminProfileEntry> {
  return get<AdminProfilePayload>("/admin/profile");
}

export function updateAdminProfile(nickname: string): Promise<AdminProfileEntry> {
  return put<AdminProfilePayload>("/admin/profile", { nickname });
}

export function pageAdminProfileLoginActivities(params: {
  pageNo?: number;
  pageSize?: number;
}): Promise<AdminProfileLoginActivityPageResult> {
  const searchParams = new URLSearchParams();
  if (params.pageNo) searchParams.set("pageNo", String(params.pageNo));
  if (params.pageSize) searchParams.set("pageSize", String(params.pageSize));
  const query = searchParams.toString();
  return get<AdminProfileLoginActivityPageResult>(
    query ? `/admin/profile/login-activities?${query}` : "/admin/profile/login-activities",
  );
}
