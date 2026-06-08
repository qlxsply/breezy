// /src/api/login-logs.ts
import type { LoginLogEntry } from "../types/login-log";
import type { PageResult, PageRule, SortRule } from "../types/page";
import { post } from "./http";

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
