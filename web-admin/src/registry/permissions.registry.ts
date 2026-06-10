// /src/registry/permissions.registry.ts
import type { ResourceEntry } from "../types/resource-admin";
import { ensureRegistryLoaded, isRegistryLoaded, refreshRegistryLoaded } from "./bootstrap";
import { findResourceByCode, getResourceMap } from "./resources.registry";

/**
 * 权限状态由 Registry 统一驱动
 */
export const isPermissionsLoaded = isRegistryLoaded;

export async function ensurePermissionsLoaded(): Promise<void> {
  return ensureRegistryLoaded();
}

export async function refreshPermissions(): Promise<void> {
  return refreshRegistryLoaded();
}

export function getPermissionIds(): string[] {
  return Array.from(getResourceMap().keys());
}

/**
 * 判断是否拥有指定 ID 的资源权限
 */
export function hasPermission(resourceId: string): boolean {
  return getResourceMap().has(resourceId);
}

/**
 * 判断是否拥有指定 Code 的资源权限 (替代原有的 API 权限码判断)
 */
export function hasApiPermission(code: string): boolean {
  return hasResourceCodeAccess(code);
}

/**
 * 递归检查资源及其父级是否启用
 */
function isEnabledWithAncestors(
  resource: ResourceEntry,
  map: Map<string, ResourceEntry>,
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

/**
 * 检查菜单访问权限
 */
export function hasMenuAccess(resource: ResourceEntry, map: Map<string, ResourceEntry>): boolean {
  if (resource.type !== "MENU") return false;
  if (!isEnabledWithAncestors(resource, map)) return false;
  return map.has(resource.id);
}

/**
 * 检查动作权限
 */
export function hasActionAccess(resource: ResourceEntry, map: Map<string, ResourceEntry>): boolean {
  if (!isEnabledWithAncestors(resource, map)) return false;
  return map.has(resource.id);
}

export function hasResourceCodeAccess(code: string): boolean {
  if (!code) return true;
  const resource = findResourceByCode(code);
  if (!resource) return false;
  const map = getResourceMap();
  if (resource.type === "MENU") {
    return hasMenuAccess(resource, map);
  }
  return hasActionAccess(resource, map);
}

export function hasAnyResourceCodeAccess(codes: string[]): boolean {
  return codes.some((code) => hasResourceCodeAccess(code));
}
