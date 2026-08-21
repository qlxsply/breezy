import { get, post, put, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type { ApiEntry } from "../model/types";
import type { ApiPageQueryParams, ApiPayload } from "./payload";

const BASE = "/apis";

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
    sortOptionsJson: payload.sortOptionsJson,
    enabled: Boolean(payload.enabled),
  };
}

export async function pageApis(
  query: ApiPageQueryParams,
  options?: Pick<RequestOptions, "signal">,
): Promise<PageResult<ApiEntry>> {
  const page = await post<PageResult<ApiPayload>>(
    `${BASE}/page`,
    {
      module: normalizeQueryValue(query.module),
      pathPattern: normalizeQueryValue(query.pathPattern),
      handlerClass: normalizeQueryValue(query.handlerClass),
      handlerMethod: normalizeQueryValue(query.handlerMethod),
      permissionDeclared: normalizeQueryValue(query.permissionDeclared),
      accessType: normalizeQueryValue(query.accessType),
      userType: normalizeQueryValue(query.userType),
      auditDeclared: normalizeQueryValue(query.auditDeclared),
      status: normalizeQueryValue(query.status),
      page: {
        pageNo: query.pageNo,
        pageSize: query.pageSize,
      },
    },
    options,
  );
  return {
    ...page,
    elements: page.elements.map(toApiEntry),
  };
}

export async function getApi(
  id: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<ApiEntry> {
  const row = await get<ApiPayload>(`${BASE}/${encodeURIComponent(id)}`, options);
  return toApiEntry(row);
}

export async function updateApiSortOptions(id: string, sortOptionsJson: string): Promise<ApiEntry> {
  const row = await put<ApiPayload>(`${BASE}/${encodeURIComponent(id)}/sort-options`, {
    sortOptionsJson,
  });
  return toApiEntry(row);
}

export async function publishApi(id: string): Promise<ApiEntry> {
  const row = await put<ApiPayload>(`${BASE}/${id}/publish`, {});
  return toApiEntry(row);
}

export async function disableApi(id: string): Promise<ApiEntry> {
  const row = await put<ApiPayload>(`${BASE}/${id}/disable`, {});
  return toApiEntry(row);
}

function normalizeQueryValue(value?: string): string | undefined {
  const normalized = value?.trim();
  return normalized || undefined;
}
