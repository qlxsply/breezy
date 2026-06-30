import type { ApiEntry } from "../types/api-admin";
import type { PageResult } from "../types/page";
import { get, put } from "./http";

const BASE = "/apis";

interface ApiPayload {
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

function toApiEntry(payload: ApiPayload): ApiEntry {
  return {
    id: String(payload.id),
    module: payload.module,
    protocol: payload.protocol,
    httpMethod: payload.httpMethod as ApiEntry["httpMethod"],
    pathPattern: payload.pathPattern,
    handlerClass: payload.handlerClass,
    handlerMethod: payload.handlerMethod,
    permissionDeclared: Boolean(payload.permissionDeclared),
    accessType: payload.accessType,
    userType: payload.userType,
    auditDeclared: Boolean(payload.auditDeclared),
    auditResource: payload.auditResource,
    auditAction: payload.auditAction,
    auditDescription: payload.auditDescription,
    enabled: Boolean(payload.enabled),
  };
}

export async function listApis(): Promise<ApiEntry[]> {
  const rows = await get<ApiPayload[]>(BASE);
  return rows.map(toApiEntry);
}

export async function pageApis(query: ApiPageQueryParams): Promise<PageResult<ApiEntry>> {
  const params = new URLSearchParams();
  appendQueryParam(params, "module", query.module);
  appendQueryParam(params, "pathPattern", query.pathPattern);
  appendQueryParam(params, "handlerClass", query.handlerClass);
  appendQueryParam(params, "handlerMethod", query.handlerMethod);
  appendQueryParam(params, "permissionDeclared", query.permissionDeclared);
  appendQueryParam(params, "accessType", query.accessType);
  appendQueryParam(params, "userType", query.userType);
  appendQueryParam(params, "auditDeclared", query.auditDeclared);
  appendQueryParam(params, "status", query.status);
  params.set("pageNo", String(query.pageNo));
  params.set("pageSize", String(query.pageSize));

  const page = await get<PageResult<ApiPayload>>(`${BASE}/page?${params.toString()}`);
  return {
    ...page,
    elements: page.elements.map(toApiEntry),
  };
}

export async function publishApi(id: string): Promise<ApiEntry> {
  const row = await put<ApiPayload>(`${BASE}/${id}/publish`, {});
  return toApiEntry(row);
}

export async function disableApi(id: string): Promise<ApiEntry> {
  const row = await put<ApiPayload>(`${BASE}/${id}/disable`, {});
  return toApiEntry(row);
}

function appendQueryParam(params: URLSearchParams, key: string, value?: string) {
  const normalized = value?.trim();
  if (normalized) {
    params.set(key, normalized);
  }
}
