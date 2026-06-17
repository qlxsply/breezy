// /src/registry/resources.registry.ts
import { computed, ref } from "vue";

import type { ResourceEntry, ResourceScope } from "../types/resource-admin";

export const RESOURCES = ref<ResourceEntry[]>([]);

/**
 * 缓存 Resource Map，避免频繁重新计算
 */
export const RESOURCE_MAP = computed(() => {
  const map = new Map<string, ResourceEntry>();
  RESOURCES.value.forEach((r) => map.set(r.id, r));
  return map;
});

export function setResources(next: ResourceEntry[]): void {
  RESOURCES.value = next;
}

export function getResourceMap(): Map<string, ResourceEntry> {
  return RESOURCE_MAP.value;
}

export function getResourceById(id: string): ResourceEntry | undefined {
  return RESOURCE_MAP.value.get(id);
}

export function getResources(): ResourceEntry[] {
  return RESOURCES.value;
}

export function findMenuResourceByCode(
  code: string,
  scope?: ResourceScope,
): ResourceEntry | undefined {
  const trimmed = code.trim().toLowerCase();

  return RESOURCES.value.find((r) => {
    if (r.type !== "MENU") return false;
    if (scope && r.scope !== scope) return false;
    return (r.code || "").toLowerCase() === trimmed;
  });
}

export function findResourceByCode(code: string): ResourceEntry | undefined {
  const trimmed = code.trim().toLowerCase();
  return RESOURCES.value.find((r) => (r.code || "").toLowerCase() === trimmed);
}

export function findMenuResourceByUrl(url: string): ResourceEntry | undefined {
  const normalized = normalizeAdminAppPath(url);
  return RESOURCES.value.find(
    (r) => r.type === "MENU" && r.openMode === "PAGE" && normalizeAdminAppPath(r.url) === normalized,
  );
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
