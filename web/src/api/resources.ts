// /src/api/resources.ts
import type {
  ResourceEntry,
  ResourceEntryCreate,
  ResourceEntryUpdate,
} from "../types/resource-admin";
import { del, get, post, put } from "./http";

/**
 * 资源管理 API（按实际后端路径调整）
 * - GET    /api/resources              -> 列表
 * - GET    /api/resources/{id}         -> 详情
 * - POST   /api/resources              -> 新增
 * - PUT    /api/resources/{id}         -> 更新
 * - DELETE /api/resources/{id}         -> 删除
 */
const BASE = "/resources";

export function listResources(): Promise<ResourceEntry[]> {
  return get<ResourceEntry[]>(BASE);
}

export function getResource(id: string): Promise<ResourceEntry> {
  return get<ResourceEntry>(`${BASE}/${encodeURIComponent(id)}`);
}

export function createResource(req: ResourceEntryCreate): Promise<ResourceEntry> {
  return post<ResourceEntry>(BASE, req);
}

export function updateResource(id: string, req: ResourceEntryUpdate): Promise<ResourceEntry> {
  return put<ResourceEntry>(`${BASE}/${encodeURIComponent(id)}`, req);
}

export function deleteResource(id: string): Promise<boolean> {
  return del<boolean>(`${BASE}/${encodeURIComponent(id)}`);
}

export function getResourceApis(id: string): Promise<string[]> {
  return get<string[]>(`${BASE}/${encodeURIComponent(id)}/apis`);
}

export function updateResourceApis(id: string, apiIds: string[]): Promise<boolean> {
  const payload = apiIds.map((apiId) => Number(apiId)).filter((apiId) => Number.isFinite(apiId));
  return put<boolean>(`${BASE}/${encodeURIComponent(id)}/apis`, { apiIds: payload });
}
