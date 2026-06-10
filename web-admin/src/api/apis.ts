import type { ApiEntry } from "../types/api-admin";
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
  userTypes?: string;
  auditDeclared: boolean;
  auditResource?: string;
  auditAction?: string;
  auditDescription?: string;
  enabled: boolean;
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
    userTypes: payload.userTypes,
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

export async function publishApi(id: string): Promise<ApiEntry> {
  const row = await put<ApiPayload>(`${BASE}/${id}/publish`, {});
  return toApiEntry(row);
}

export async function disableApi(id: string): Promise<ApiEntry> {
  const row = await put<ApiPayload>(`${BASE}/${id}/disable`, {});
  return toApiEntry(row);
}
