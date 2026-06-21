"use client";

import { createStore, useStoreValue } from "@admin/core/client-store";
import type { ResourceEntry, ResourceScope } from "@admin/types/resource-admin";

const resourcesStore = createStore<ResourceEntry[]>([]);

export function setResources(next: ResourceEntry[]): void {
  resourcesStore.setState(next);
}

export function getResources(): ResourceEntry[] {
  return resourcesStore.getState();
}

export function useResources(): ResourceEntry[] {
  return useStoreValue(resourcesStore, (state) => state);
}

export function getResourceMap(): Map<string, ResourceEntry> {
  const map = new Map<string, ResourceEntry>();
  resourcesStore.getState().forEach((resource) => map.set(resource.id, resource));
  return map;
}

export function getResourceById(id: string): ResourceEntry | undefined {
  return getResourceMap().get(id);
}

export function findMenuResourceByCode(code: string, scope?: ResourceScope): ResourceEntry | undefined {
  const trimmed = code.trim().toLowerCase();
  return resourcesStore.getState().find((resource) => {
    if (resource.type !== "MENU") return false;
    if (scope && resource.scope !== scope) return false;
    return (resource.code || "").toLowerCase() === trimmed;
  });
}

export function findResourceByCode(code: string): ResourceEntry | undefined {
  const trimmed = code.trim().toLowerCase();
  return resourcesStore.getState().find((resource) => (resource.code || "").toLowerCase() === trimmed);
}

export function findMenuResourceByUrl(url: string): ResourceEntry | undefined {
  const normalized = normalizeAdminAppPath(url);
  return resourcesStore
    .getState()
    .find((resource) => resource.type === "MENU" && resource.openMode === "PAGE" && normalizeAdminAppPath(resource.url) === normalized);
}

function normalizeAdminAppPath(url: string): string {
  const normalized = url.trim();
  if (normalized === "/admin") {
    return "/";
  }
  if (normalized.startsWith("/admin/")) {
    return normalized.substring("/admin".length);
  }
  return normalized || "/";
}
