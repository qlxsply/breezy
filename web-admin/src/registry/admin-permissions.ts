import {
  ensureAdminMenuResourcesLoaded,
  findAdminMenuResourceByCode,
  getAdminMenuResourceMap,
} from "@admin/registry/admin-menu-resources";
import type { AdminMenuResourceEntry } from "@admin/types/admin-menu-resource";

export async function ensureAdminPermissionsLoaded(): Promise<void> {
  return ensureAdminMenuResourcesLoaded();
}

function isEnabledWithAncestors(
  resource: AdminMenuResourceEntry,
  map: Map<string, AdminMenuResourceEntry>,
  visited = new Set<string>(),
): boolean {
  if (!resource.enabled) {
    return false;
  }
  if (visited.has(resource.id)) {
    return true;
  }
  visited.add(resource.id);

  const parentId = resource.parentId ?? null;
  if (!parentId) {
    return true;
  }
  const parent = map.get(parentId);
  if (!parent) {
    return true;
  }
  return isEnabledWithAncestors(parent, map, visited);
}

function hasAdminMenuAccess(
  resource: AdminMenuResourceEntry,
  map: Map<string, AdminMenuResourceEntry>,
): boolean {
  if (resource.type !== "MENU") {
    return false;
  }
  if (!isEnabledWithAncestors(resource, map)) {
    return false;
  }
  return map.has(resource.id);
}

function hasAdminActionAccess(
  resource: AdminMenuResourceEntry,
  map: Map<string, AdminMenuResourceEntry>,
): boolean {
  if (!isEnabledWithAncestors(resource, map)) {
    return false;
  }
  return map.has(resource.id);
}

export function hasAdminResourceCodeAccess(code: string): boolean {
  if (!code) {
    return true;
  }
  const resource = findAdminMenuResourceByCode(code);
  if (!resource) {
    return false;
  }
  const map = getAdminMenuResourceMap();
  if (resource.type === "MENU") {
    return hasAdminMenuAccess(resource, map);
  }
  return hasAdminActionAccess(resource, map);
}
