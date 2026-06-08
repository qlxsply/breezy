import { get } from "@admin/api/http";
import type {
  AdminMenuResourceEntry,
  AdminMenuResourceOpenMode,
  AdminMenuResourceScope,
  AdminMenuResourceType,
} from "@admin/types/admin-menu-resource";

interface AdminMenuRegistryPayload {
  resources?: unknown;
}

export async function fetchAdminMenuResources(): Promise<AdminMenuResourceEntry[]> {
  const payload = await get<AdminMenuRegistryPayload>("/admin/menu-resources");
  return normalizeAdminMenuResources(payload.resources);
}

function normalizeAdminMenuResources(raw: unknown): AdminMenuResourceEntry[] {
  if (!Array.isArray(raw)) {
    return [];
  }

  const resources = raw
    .map((item, index) => {
      if (!item || typeof item !== "object") {
        return null;
      }
      const record = item as Record<string, unknown>;
      const code = stringValue(record.code).trim();
      if (!code) {
        return null;
      }

      return {
        id: String(record.id ?? code),
        parentId: record.parentId ? String(record.parentId) : null,
        name: stringValue(record.name) || code,
        code,
        type: normalizeType(record.type),
        scope: normalizeScope(record.scope, record.type),
        openMode: normalizeOpenMode(record.openMode, record.type),
        url: stringValue(record.url ?? record.path),
        orderNo: numberValue(record.orderNo ?? record.weight, index + 1),
        enabled: typeof record.enabled === "boolean" ? record.enabled : true,
      } satisfies AdminMenuResourceEntry;
    })
    .filter((item): item is AdminMenuResourceEntry => Boolean(item));

  return filterAdminMenuResources(resources);
}

function filterAdminMenuResources(resources: AdminMenuResourceEntry[]): AdminMenuResourceEntry[] {
  const seedIds = new Set(resources.filter(isAdminMenuSeed).map((resource) => resource.id));
  const allowedIds = new Set<string>();
  const queue = [...seedIds];

  while (queue.length > 0) {
    const currentId = queue.shift();
    if (!currentId || allowedIds.has(currentId)) {
      continue;
    }
    allowedIds.add(currentId);
    resources.forEach((resource) => {
      if (resource.parentId === currentId) {
        queue.push(resource.id);
      }
    });
  }

  return resources.filter((resource) => allowedIds.has(resource.id));
}

function isAdminMenuSeed(resource: AdminMenuResourceEntry): boolean {
  if (resource.type !== "MENU" || !resource.enabled) {
    return false;
  }
  if (resource.scope === "SETTING") {
    return true;
  }
  return resource.url === "/admin" || resource.url.startsWith("/admin/");
}

function normalizeType(value: unknown): AdminMenuResourceType {
  const raw = String(value ?? "").toUpperCase();
  if (raw === "BUTTON" || raw === "FEATURE" || raw === "DATA") {
    return raw;
  }
  return "MENU";
}

function normalizeScope(value: unknown, typeValue: unknown): AdminMenuResourceScope {
  const type = normalizeType(typeValue);
  if (type !== "MENU") {
    return "NONE";
  }
  const raw = String(value ?? "").toUpperCase();
  if (raw === "TOOL" || raw === "SETTING" || raw === "INFO") {
    return raw;
  }
  return "NONE";
}

function normalizeOpenMode(value: unknown, typeValue: unknown): AdminMenuResourceOpenMode {
  const raw = String(value ?? "").toUpperCase();
  if (raw === "PAGE" || raw === "MODAL") {
    return raw;
  }
  if (raw === "NONE") {
    return "NONE";
  }
  return normalizeType(typeValue) === "MENU" ? "PAGE" : "NONE";
}

function stringValue(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function numberValue(value: unknown, fallback: number): number {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}
