export interface ApiPayload {
  id: string;
  module: string;
  protocol: string;
  httpMethod: string;
  pathPattern: string;
  handlerClass?: string;
  handlerMethod?: string;
  permissionDeclared: boolean;
  accessType: string;
  userType?: string;
  auditDeclared: boolean;
  auditResource?: string;
  auditAction?: string;
  auditDescription?: string;
  sortOptionsJson?: string;
  enabled: boolean;
}

export interface ApiPageQueryParams {
  module?: string;
  pathPattern?: string;
  handlerClass?: string;
  handlerMethod?: string;
  permissionDeclared?: string;
  accessType?: string;
  userType?: string;
  auditDeclared?: string;
  status?: string;
  pageNo: number;
  pageSize: number;
}
