import type { AuditLevel, AuditLogEntry } from "../types/audit-log";
import type { PageResult, PageRule, SortRule } from "../types/page";
import { get, post } from "./http";

const BASE = "/sys/audit-logs";

export interface AuditLogPageRequest {
  traceId?: string;
  operatorUsername?: string;
  requestUri?: string;
  auditResource?: string;
  auditAction?: string;
  auditLevel?: AuditLevel | "";
  success?: boolean;
  startAt?: string;
  endAt?: string;
  page?: PageRule;
  sort?: SortRule;
}

export function pageAuditLogs(req: AuditLogPageRequest): Promise<PageResult<AuditLogEntry>> {
  return post<PageResult<AuditLogEntry>>(`${BASE}/page`, req);
}

export function getAuditLog(id: string): Promise<AuditLogEntry> {
  return get<AuditLogEntry>(`${BASE}/${encodeURIComponent(id)}`);
}
