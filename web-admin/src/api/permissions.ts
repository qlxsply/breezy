// /src/api/permissions.ts
import type { PermissionEntry, PermissionUserScope } from "../types/permission-admin";
import { get } from "./http";

const BASE = "/permissions";

export interface MyPermissionsRes {
  permissionCodes: string[];
}

export interface MyPermissionsDetailRes {
  username: string;
  roles: string[];
  permissionCodes: string[];
}

interface PermissionPayload {
  id: string;
  code: string;
  name: string;
  userScope: PermissionUserScope;
}

function toPermissionEntry(payload: PermissionPayload): PermissionEntry {
  return {
    id: String(payload.id),
    code: payload.code,
    name: payload.name,
    userScope: payload.userScope,
  };
}

export function getMyPermissions(): Promise<MyPermissionsRes> {
  return get<MyPermissionsRes>(`${BASE}/me`);
}

export function getMyPermissionsDetails(): Promise<MyPermissionsDetailRes> {
  return get<MyPermissionsDetailRes>(`${BASE}/me/details`);
}

export async function listPermissions(): Promise<PermissionEntry[]> {
  const rows = await get<PermissionPayload[]>(BASE);
  return rows.map(toPermissionEntry);
}
