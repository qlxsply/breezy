import type { PageRule, SortRule } from "@admin/shared/types/pagination";

import type { AuditLevel } from "../model/types";

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
