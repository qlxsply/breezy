"use client";

import { getAuthToken } from "@admin/core/auth-storage";
import { resolveBootstrapUrl } from "@admin/core/bootstrap-config";
import { createStore, useStoreValue } from "@admin/core/client-store";
import { API_BASE_URL } from "@admin/core/env";
import { setResources } from "@admin/core/registry/resources-registry";
import type {
  ResourceEntry,
  ResourceLevel,
  ResourceOpenMode,
  ResourceScope,
  ResourceType,
} from "@admin/types/resource-admin";

interface RegistryPayload {
  resources?: unknown;
}

interface BootstrapRegistryState {
  loaded: boolean;
  loading: boolean;
}

const bootstrapRegistryStore = createStore<BootstrapRegistryState>({
  loaded: false,
  loading: false,
});
let loadingPromise: Promise<void> | null = null;

export function useIsRegistryLoaded(): boolean {
  return useStoreValue(bootstrapRegistryStore, (state) => state.loaded);
}

export function isRegistryLoaded(): boolean {
  return bootstrapRegistryStore.getState().loaded;
}

export async function ensureRegistryLoaded(force = false): Promise<void> {
  const state = bootstrapRegistryStore.getState();
  if (!force && state.loaded) {
    return loadingPromise ?? Promise.resolve();
  }
  if (!force && state.loading && loadingPromise) {
    return loadingPromise;
  }

  bootstrapRegistryStore.setState((current) => ({ ...current, loading: true }));

  loadingPromise = (async () => {
    try {
      const payload = await fetchRegistryPayload();
      if (payload) {
        setResources(payload.resources);
        bootstrapRegistryStore.setState({ loaded: true, loading: false });
      } else {
        bootstrapRegistryStore.setState({ loaded: false, loading: false });
      }
    } catch (error) {
      console.warn("[registry] bootstrap failed", error);
      setResources([]);
      bootstrapRegistryStore.setState({ loaded: false, loading: false });
    } finally {
      loadingPromise = null;
    }
  })();

  return loadingPromise;
}

export async function refreshRegistryLoaded(): Promise<void> {
  bootstrapRegistryStore.setState({ loaded: false, loading: false });
  loadingPromise = null;
  await ensureRegistryLoaded(true);
}

async function fetchRegistryPayload(): Promise<{ resources: ResourceEntry[] } | null> {
  const url = resolveBootstrapUrl(`${API_BASE_URL}/admin/menu-resources`);
  if (!url) {
    return null;
  }

  const headers = new Headers({ Accept: "application/json" });
  const token = getAuthToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const resp = await fetch(url, { headers });
  if (!resp.ok) {
    throw new Error(`Registry request failed: ${resp.status} ${resp.statusText}`);
  }

  const json = await resp.json();
  const root = unwrapPayload(json);
  if (!root) {
    return null;
  }

  return {
    resources: normalizeResources(root.resources),
  };
}

function unwrapPayload(raw: unknown): RegistryPayload | null {
  if (!raw || typeof raw !== "object") return null;
  const record = raw as Record<string, unknown>;
  if (record.data && typeof record.data === "object") {
    return record.data as RegistryPayload;
  }
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
  if (rawType === "DIRECTORY") return "SETTING";
  const raw = String(value ?? "").toUpperCase();
  if (raw === "TOOL" || raw === "SETTING" || raw === "INFO") return raw as ResourceScope;
  const url = String(urlValue ?? "").trim();
  if (url.startsWith("/admin")) return "SETTING";
  return "NONE";
}

function normalizeOpenMode(value: unknown, type: ResourceType, rawType: string): ResourceOpenMode {
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
