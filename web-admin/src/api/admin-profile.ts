import type { LoginEvent } from "../types/login-log";
import { get, put } from "./http";

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

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
interface AdminProfilePayload extends AdminProfileEntry {}

export function getAdminProfile(): Promise<AdminProfileEntry> {
  return get<AdminProfilePayload>("/admin/profile");
}

export function updateAdminProfile(nickname: string): Promise<AdminProfileEntry> {
  return put<AdminProfilePayload>("/admin/profile", { nickname });
}

export interface AdminProfileLoginActivityPageResult {
  pageNo: number;
  pageSize: number;
  numberOfElements: number;
  totalPages: number;
  totalElements: number;
  elements: AdminProfileLoginActivityEntry[];
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
