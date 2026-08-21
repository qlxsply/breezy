import { del, get, post, put, type RequestOptions } from "@admin/shared/transport";

import type {
  ResourceManageEntry,
  ResourceManagePermissionSelection,
  ResourceManageSaveRequest,
  ResourcePermissionOption,
} from "../model/types";
import type {
  PermissionPayload,
  PermissionSelectionPayload,
  ResourceManagePayload,
} from "./payload";

const BASE = "/system/resources";
const PERMISSION_BASE = "/system/permissions";

function toEntry(payload: ResourceManagePayload): ResourceManageEntry {
  return {
    id: String(payload.id),
    parentId: payload.parentId == null ? null : String(payload.parentId),
    code: payload.code,
    name: payload.name,
    resourceType: (payload.resourceType || "MENU") as ResourceManageEntry["resourceType"],
    path: payload.path ?? null,
    component: payload.component ?? null,
    icon: payload.icon ?? null,
    sortNo: Number(payload.sortNo || 0),
    visible: payload.visible !== false,
    enabled: payload.enabled !== false,
    defaultEntry: payload.defaultEntry === true,
    systemBuiltin: payload.systemBuiltin === true,
    remark: payload.remark ?? null,
    permissionIds: Array.isArray(payload.permissionIds)
      ? payload.permissionIds.map((item) => String(item))
      : [],
    children: Array.isArray(payload.children) ? payload.children.map(toEntry) : [],
  };
}

function toSavePayload(req: ResourceManageSaveRequest) {
  return {
    parentId: req.parentId ? Number(req.parentId) : null,
    code: req.code,
    name: req.name,
    resourceType: req.resourceType,
    path: req.path ?? null,
    component: req.component ?? null,
    icon: req.icon ?? null,
    sortNo: req.sortNo,
    visible: req.visible,
    enabled: req.enabled,
    defaultEntry: req.defaultEntry,
    systemBuiltin: req.systemBuiltin,
    remark: req.remark ?? null,
  };
}

export async function listResources(
  options?: Pick<RequestOptions, "signal">,
): Promise<ResourceManageEntry[]> {
  const rows = await get<ResourceManagePayload[]>(`${BASE}/tree`, options);
  return rows.map(toEntry);
}

export async function getResource(
  id: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<ResourceManageEntry> {
  return toEntry(await get<ResourceManagePayload>(`${BASE}/${encodeURIComponent(id)}`, options));
}

export async function createResource(req: ResourceManageSaveRequest): Promise<ResourceManageEntry> {
  return toEntry(await post<ResourceManagePayload>(BASE, toSavePayload(req)));
}

export async function updateResource(
  id: string,
  req: ResourceManageSaveRequest,
): Promise<ResourceManageEntry> {
  return toEntry(
    await put<ResourceManagePayload>(`${BASE}/${encodeURIComponent(id)}`, toSavePayload(req)),
  );
}

export function deleteResource(id: string): Promise<boolean> {
  return del<boolean>(`${BASE}/${encodeURIComponent(id)}`);
}

export async function listPermissions(
  options?: Pick<RequestOptions, "signal">,
): Promise<ResourcePermissionOption[]> {
  const rows = await get<PermissionPayload[]>(PERMISSION_BASE, options);
  return rows.map((payload) => ({
    id: String(payload.id),
    code: payload.code,
    name: payload.name,
    userScope: payload.userScope,
  }));
}

export async function getResourcePermissions(
  id: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<ResourceManagePermissionSelection> {
  const payload = await get<PermissionSelectionPayload>(
    `${BASE}/${encodeURIComponent(id)}/permissions`,
    options,
  );
  return {
    permissionIds: Array.isArray(payload.permissionIds)
      ? payload.permissionIds.map((item) => String(item))
      : [],
  };
}

export function updateResourcePermissions(
  id: string,
  selection: ResourceManagePermissionSelection,
): Promise<boolean> {
  return put<boolean>(`${BASE}/${encodeURIComponent(id)}/permissions`, {
    permissionIds: selection.permissionIds
      .map((item) => Number(item))
      .filter((item) => Number.isFinite(item)),
  });
}
