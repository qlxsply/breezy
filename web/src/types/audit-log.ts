export type AuditLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface AuditLogEntry {
  id: string;
  traceId?: string | null;
  requestId?: string | null;
  operatorUserId?: string | null;
  operatorUsername?: string | null;
  operatorUserType?: string | null;
  applicationCode?: string | null;
  protocol?: string | null;
  httpMethod?: string | null;
  pathPattern?: string | null;
  requestUri?: string | null;
  permissionCodes: string[];
  auditResource?: string | null;
  auditAction?: string | null;
  auditDescription?: string | null;
  auditLevel?: AuditLevel | null;
  requestIp?: string | null;
  userAgent?: string | null;
  requestParamSummary?: string | null;
  requestBodySummary?: string | null;
  responseSummary?: string | null;
  success: boolean;
  errorCode?: string | null;
  errorMessage?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
  durationMs?: number | null;
  createdAt?: string | null;
}
