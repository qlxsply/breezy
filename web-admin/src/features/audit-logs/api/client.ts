import { get, post, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type { AuditLogEntry } from "../model/types";
import type { AuditLogPageRequest } from "./payload";

const BASE = "/sys/audit-logs";

export function pageAuditLogs(
  req: AuditLogPageRequest,
  options?: Pick<RequestOptions, "signal">,
): Promise<PageResult<AuditLogEntry>> {
  return post<PageResult<AuditLogEntry>>(`${BASE}/page`, req, options);
}

export function getAuditLog(
  id: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<AuditLogEntry> {
  return get<AuditLogEntry>(`${BASE}/${encodeURIComponent(id)}`, options);
}
