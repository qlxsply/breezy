// /src/registry/bootstrap.ts
import { computed, ref } from "vue";

import { API_BASE_URL } from "../api/http";
import type {
  ResourceEntry,
  ResourceLevel,
  ResourceOpenMode,
  ResourceScope,
  ResourceType,
} from "../types/resource-admin";
import { getAuthToken } from "../utils/authStorage";
import { resolveBootstrapUrl } from "../utils/bootstrapConfig";
import { setResources } from "./resources.registry";

interface RegistryPayload {
  resources?: unknown;
}

const loaded = ref(false);
const registryState = {
  loading: false,
  promise: null as Promise<void> | null,
};

export const isRegistryLoaded = computed(() => loaded.value);

export async function ensureRegistryLoaded(): Promise<void> {
  if (loaded.value) return registryState.promise ?? Promise.resolve();
  if (registryState.loading) return registryState.promise ?? Promise.resolve();

  registryState.loading = true;
  registryState.promise = (async () => {
    try {
      const payload = await fetchRegistryPayload();
      if (!payload) return;

      setResources(payload.resources);
      loaded.value = true;
    } catch (err) {
      console.warn("[registry] bootstrap failed", err);
      setResources([]);
    } finally {
      registryState.loading = false;
    }
  })();

  return registryState.promise;
}

export async function refreshRegistryLoaded(): Promise<void> {
  loaded.value = false;
  registryState.loading = false;
  registryState.promise = null;
  await ensureRegistryLoaded();
}

async function fetchRegistryPayload(): Promise<{ resources: ResourceEntry[] } | null> {
  const url = resolveBootstrapUrl(`${API_BASE_URL}/admin/menu-resources`);
  if (!url) return null;

  const headers = new Headers({ Accept: "application/json" });
  const token = getAuthToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const resp = await fetch(url, { headers });
  if (!resp.ok) {
    throw new Error(`Registry request failed: ${resp.status} ${resp.statusText}`);
  }

  const json = await resp.json();
  const root = unwrapPayload(json);
  if (!root) return null;

  return {
    resources: normalizeResources(root.resources),
  };
}

function unwrapPayload(raw: unknown): RegistryPayload | null {
  if (!raw || typeof raw !== "object") return null;
  const record = raw as Record<string, unknown>;
  if (record.data && typeof record.data === "object") return record.data as RegistryPayload;
  return record as RegistryPayload;
}

function normalizeResources(raw: unknown): ResourceEntry[] {
  if (!Array.isArray(raw)) return [];

  return raw
    .map((item, index) => {
      if (!item || typeof item !== "object") return null;
      const record = item as Record<string, unknown>;
      const code = stringOr(record.code ?? record.shortcut ?? record.cmd, "").trim();
      if (!code) return null;

      const rawType = String(record.type ?? "").toUpperCase();
      const type = normalizeType(rawType);
      const scope = normalizeScope(record.scope, type, record.url ?? record.path, rawType);
      const openMode = normalizeOpenMode(record.openMode, type, rawType);
      const level = normalizeLevel(record.level, record.system);

      const rawParent = record.parentId ?? record.parent_id ?? null;
      const parentId =
        rawParent === null || rawParent === undefined || rawParent === ""
          ? null
          : String(rawParent);

      return {
        id: String(record.id ?? code),
        parentId,
        name: stringOr(record.name, code),
        icon: stringOr(record.icon, ""),
        description: stringOr(record.description, ""),
        code,
        type,
        scope,
        openMode,
        url: stringOr(record.url ?? record.path, ""),
        loadTarget: stringOr(record.loadTarget ?? record.component, ""),
        orderNo: toNumber(record.orderNo ?? record.weight, index + 1),
        level,
        enabled: typeof record.enabled === "boolean" ? record.enabled : true,
        guestAccess: record.guestAccess === true,
      } as ResourceEntry;
    })
    .filter((item): item is ResourceEntry => Boolean(item));
}

function stringOr(value: unknown, fallback: string): string {
  if (typeof value === "string" && value.trim()) return value;
  return fallback;
}

function toNumber(value: unknown, fallback: number): number {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

function normalizeType(raw: string): ResourceType {
  if (raw === "BUTTON" || raw === "FEATURE" || raw === "DATA") return raw as ResourceType;
  return "MENU";
}

function normalizeScope(
  value: unknown,
  type: ResourceType,
  urlValue: unknown,
  rawType: string,
): ResourceScope {
  if (type !== "MENU") return "NONE";
  if (rawType === "DIRECTORY") {
    return "SETTING";
  }
  const raw = String(value ?? "").toUpperCase();
  if (raw === "TOOL" || raw === "SETTING" || raw === "INFO") return raw as ResourceScope;
  const url = String(urlValue ?? "").trim();
  if (url.startsWith("/admin")) return "SETTING";
  return "NONE";
}

function normalizeOpenMode(
  value: unknown,
  type: ResourceType,
  rawType: string,
): ResourceOpenMode {
  if (rawType === "DIRECTORY") return "NONE";
  const raw = String(value ?? "").toUpperCase();
  if (raw === "MODAL" || raw === "PAGE") return raw as ResourceOpenMode;
  if (raw === "NONE") return "NONE";
  return type === "MENU" ? "PAGE" : "NONE";
}

function normalizeLevel(value: unknown, systemFlag: unknown): ResourceLevel {
  const raw = String(value ?? "").toUpperCase();
  if (raw === "SYSTEM" || raw === "CUSTOM") return raw as ResourceLevel;
  if (systemFlag === true) return "SYSTEM";
  return "CUSTOM";
}
