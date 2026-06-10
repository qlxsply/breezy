import type { UserToolEntry } from "../types/user-tools";
import {
  ensureUserToolsLoaded,
  findUserPermissionByCode,
  getUserToolMap,
  refreshUserToolsLoaded,
} from "./user-tools.registry";

export async function ensureUserToolPermissionsLoaded(): Promise<void> {
  return ensureUserToolsLoaded();
}

export async function refreshUserToolPermissions(): Promise<void> {
  return refreshUserToolsLoaded();
}

export function getUserPermissionIds(): string[] {
  return Array.from(getUserToolMap().keys());
}

export function hasUserToolPermission(resourceId: string): boolean {
  return getUserToolMap().has(resourceId);
}

export function hasUserPermissionCode(code: string): boolean {
  return hasUserToolCodeAccess(code);
}

function isEnabledWithAncestors(
  resource: UserToolEntry,
  map: Map<string, UserToolEntry>,
  visited = new Set<string>(),
): boolean {
  if (!resource.enabled) return false;
  if (visited.has(resource.id)) return true;
  visited.add(resource.id);

  const parentId = resource.parentId ?? null;
  if (!parentId) return true;
  const parent = map.get(parentId);
  if (!parent) return true;
  return isEnabledWithAncestors(parent, map, visited);
}

export function hasToolPageAccess(
  resource: UserToolEntry,
  map: Map<string, UserToolEntry>,
): boolean {
  if (resource.type !== "MENU") return false;
  if (!isEnabledWithAncestors(resource, map)) return false;
  return map.has(resource.id);
}

function hasToolActionAccess(resource: UserToolEntry, map: Map<string, UserToolEntry>): boolean {
  if (!isEnabledWithAncestors(resource, map)) return false;
  return map.has(resource.id);
}

export function hasUserToolCodeAccess(code: string): boolean {
  if (!code) return true;
  const resource = findUserPermissionByCode(code);
  if (!resource) return false;
  const map = getUserToolMap();
  if (resource.type === "MENU") {
    return hasToolPageAccess(resource, map);
  }
  return hasToolActionAccess(resource, map);
}
