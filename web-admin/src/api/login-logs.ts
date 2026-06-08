import { post } from "@admin/api/http";
import type { LoginLogEntry } from "@admin/types/login-log";
import type { PageResult, PageRule, SortRule } from "@admin/types/page";

const BASE = "/sys/login-logs";

export interface LoginLogPageRequest {
  userAccount?: string;
  startAt?: string;
  endAt?: string;
  page?: PageRule;
  sort?: SortRule;
}

export function pageLoginLogs(req: LoginLogPageRequest): Promise<PageResult<LoginLogEntry>> {
  return post<PageResult<LoginLogEntry>>(`${BASE}/page`, req);
}
