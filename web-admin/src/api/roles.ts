import type { PageResult, PageRule, SortRule } from "../types/page";
import type {
  RoleCreateRequest,
  RoleEntry,
  RoleGrantResourceEntry,
  RoleGrantSelection,
  RoleUpdateRequest,
} from "../types/role-admin";
import { del, get, post, put } from "./http";

const BASE = "/roles";

interface RolePayload {
  id: string;
  code: string;
  name: string;
  enabled: boolean;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

interface RoleGrantResourcePayload {
  id: string;
  parentId?: string | null;
  resourceId: string;
  name: string;
  code: string;
  type: string;
  description?: string;
  enabled: boolean;
  selectable: boolean;
  orderNo: number;
}

interface RoleGrantSelectionPayload {
  resourceIds?: Array<string | number>;
}

function toRoleEntry(payload: RolePayload): RoleEntry {
  return {
    id: payload.id,
    code: payload.code,
    name: payload.name,
    enabled: Boolean(payload.enabled),
    createdBy: payload.createdBy,
    createdAt: payload.createdAt,
    updatedBy: payload.updatedBy,
    updatedAt: payload.updatedAt,
  };
}

function toGrantResourceEntry(payload: RoleGrantResourcePayload): RoleGrantResourceEntry {
  return {
    id: String(payload.id),
    parentId: payload.parentId ?? null,
    resourceId: String(payload.resourceId),
    name: payload.name,
    code: payload.code,
    type: payload.type,
    description: payload.description,
    enabled: Boolean(payload.enabled),
    selectable: Boolean(payload.selectable),
    orderNo: Number(payload.orderNo || 0),
  };
}

export async function listRoles(): Promise<RoleEntry[]> {
  const rows = await get<RolePayload[]>(BASE);
  return rows.map(toRoleEntry);
}

export async function pageRoles(params: {
  keyword?: string;
  enabled?: boolean;
  page?: PageRule;
  sort?: SortRule;
}): Promise<PageResult<RoleEntry>> {
  const page = await post<PageResult<RolePayload>>(`${BASE}/page`, {
    keyword: params.keyword || undefined,
    enabled: params.enabled,
    page: params.page,
    sort: params.sort,
  });
  return {
    ...page,
    elements: page.elements.map(toRoleEntry),
  };
}

export async function createRole(req: RoleCreateRequest): Promise<RoleEntry> {
  const row = await post<RolePayload>(BASE, req);
  return toRoleEntry(row);
}

export async function updateRole(id: string, req: RoleUpdateRequest): Promise<RoleEntry> {
  const row = await put<RolePayload>(`${BASE}/${encodeURIComponent(id)}`, req);
  return toRoleEntry(row);
}

export async function deleteRole(id: string): Promise<boolean> {
  return del<boolean>(`${BASE}/${encodeURIComponent(id)}`);
}

export async function listRoleGrantResources(): Promise<RoleGrantResourceEntry[]> {
  const rows = await get<RoleGrantResourcePayload[]>(`${BASE}/grant-resources`);
  return rows.map(toGrantResourceEntry);
}

export async function getRoleGrantSelection(id: string): Promise<RoleGrantSelection> {
  const payload = await get<RoleGrantSelectionPayload>(`${BASE}/${encodeURIComponent(id)}/grant`);
  return {
    resourceIds: Array.isArray(payload.resourceIds)
      ? payload.resourceIds.map((value) => String(value))
      : [],
  };
}

export function updateRoleGrantSelection(
  id: string,
  selection: RoleGrantSelection,
): Promise<boolean> {
  const resourceIds = selection.resourceIds
    .map((resourceId) => Number(resourceId))
    .filter((resourceId) => Number.isFinite(resourceId));
  return put<boolean>(`${BASE}/${encodeURIComponent(id)}/grant`, { resourceIds });
}
