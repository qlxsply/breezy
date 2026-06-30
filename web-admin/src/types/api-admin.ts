export type ApiMethod = "GET" | "POST" | "PUT" | "DELETE" | "PATCH" | "OPTIONS" | "HEAD" | "ANY";
export type ApiProtocol = "HTTP";
export type ApiAccessType = "PUBLIC" | "AUTHENTICATED" | "AUTHORIZED";

export interface ApiEntry {
  id: string;
  module: string;
  protocol: ApiProtocol | string;
  httpMethod: ApiMethod;
  pathPattern: string;
  handlerClass?: string;
  handlerMethod?: string;
  permissionDeclared: boolean;
  accessType: ApiAccessType | string;
  userType?: string;
  auditDeclared: boolean;
  auditResource?: string;
  auditAction?: string;
  auditDescription?: string;
  enabled: boolean;
  protocolLabel?: string;
  httpMethodLabel?: string;
  accessTypeLabel?: string;
  userTypeLabel?: string;
  auditTooltip?: string;
}
