"use client";

import { createStore, useStoreValue } from "@admin/core/client-store";
import { ensureRegistryLoaded, refreshRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import { findResourceByCode, getResourceMap } from "@admin/core/registry/resources-registry";
import type { ResourceEntry } from "@admin/types/resource-admin";

const permissionsLoadedStore = createStore(false);

export function useIsPermissionsLoaded(): boolean {
  return useStoreValue(permissionsLoadedStore, (state) => state);
}

export function isPermissionsLoaded(): boolean {
  return permissionsLoadedStore.getState();
}

export async function ensurePermissionsLoaded(): Promise<void> {
  await ensureRegistryLoaded();
  permissionsLoadedStore.setState(true);
}

export async function refreshPermissions(): Promise<void> {
  permissionsLoadedStore.setState(false);
  await refreshRegistryLoaded();
  permissionsLoadedStore.setState(true);
}

export function getPermissionIds(): string[] {
  return Array.from(getResourceMap().keys());
}

export function hasPermission(resourceId: string): boolean {
  return getResourceMap().has(resourceId);
}

export function hasApiPermission(code: string): boolean {
  return hasResourceCodeAccess(code);
}

function isEnabledWithAncestors(resource: ResourceEntry, map: Map<string, ResourceEntry>, visited = new Set<string>()): boolean {
  if (!resource.enabled) return false;
  if (visited.has(resource.id)) return true;
  visited.add(resource.id);
  const parentId = resource.parentId ?? null;
  if (!parentId) return true;
  const parent = map.get(parentId);
  if (!parent) return true;
  return isEnabledWithAncestors(parent, map, visited);
}

export function hasMenuAccess(resource: ResourceEntry, map: Map<string, ResourceEntry>): boolean {
  if (resource.type !== "MENU") return false;
  if (!isEnabledWithAncestors(resource, map)) return false;
  return map.has(resource.id);
}

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
