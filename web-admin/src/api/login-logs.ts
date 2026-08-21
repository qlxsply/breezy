// /src/api/login-logs.ts
import { post } from "@admin/shared/transport";
import type { PageResult, PageRule, SortRule } from "@admin/shared/types/pagination";

import type { LoginLogEntry } from "../types/login-log";

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
