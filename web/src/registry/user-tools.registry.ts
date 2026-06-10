import { computed, ref } from "vue";

import { API_BASE_URL } from "../api/http";
import type { UserToolEntry, UserToolEntryLevel } from "../types/user-tools";
import { resolveBootstrapUrl } from "../utils/bootstrapConfig";
import { getValidAuthToken, handleUnauthorizedResponse } from "./auth-token.registry";

interface UserToolsPayload {
  tools?: unknown;
  permissionCodes?: unknown;
}

const userTools = ref<UserToolEntry[]>([]);

const state = {
  loaded: false,
  loading: false,
  promise: null as Promise<void> | null,
};

export const isUserToolsLoaded = computed(() => state.loaded);
export const userToolMap = computed(() => {
  const map = new Map<string, UserToolEntry>();
  userTools.value.forEach((tool) => map.set(tool.id, tool));
  return map;
});

export function getUserToolMap(): Map<string, UserToolEntry> {
  return userToolMap.value;
}

export function getUserTools(): UserToolEntry[] {
  return userTools.value;
}

export function setUserTools(next: UserToolEntry[]): void {
  userTools.value = next;
}

export function findUserToolByCode(code: string): UserToolEntry | undefined {
  const trimmed = code.trim().toLowerCase();
  return userTools.value.find(
    (tool) => tool.type === "MENU" && tool.code.toLowerCase() === trimmed,
  );
}

export function findUserPermissionByCode(code: string): UserToolEntry | undefined {
  const trimmed = code.trim().toLowerCase();
  return userTools.value.find((tool) => tool.code.toLowerCase() === trimmed);
}

export function findUserToolByUrl(url: string): UserToolEntry | undefined {
  const normalized = url.trim();
  return userTools.value.find(
    (tool) => tool.type === "MENU" && tool.openMode === "PAGE" && tool.url === normalized,
  );
}

export async function ensureUserToolsLoaded(): Promise<void> {
  if (state.loaded) return state.promise ?? Promise.resolve();
  if (state.loading) return state.promise ?? Promise.resolve();

  state.loading = true;
  state.promise = (async () => {
    try {
      const payload = await fetchUserToolsPayload();
      if (!payload) return;
      setUserTools(payload.tools);
      state.loaded = true;
    } catch (err) {
      console.warn("[user-tools] bootstrap failed", err);
      setUserTools([]);
    } finally {
      state.loading = false;
    }
  })();

  return state.promise;
}

export async function refreshUserToolsLoaded(): Promise<void> {
  state.loaded = false;
  state.loading = false;
  state.promise = null;
  await ensureUserToolsLoaded();
}

async function fetchUserToolsPayload(): Promise<{ tools: UserToolEntry[] } | null> {
  const url = resolveBootstrapUrl(`${API_BASE_URL}/user/tools`);
  if (!url) return null;

  const headers = new Headers({ Accept: "application/json" });
  const token = await getValidAuthToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const resp = await fetch(url, { headers });
  if (!resp.ok) {
    if (resp.status === 401) {
      handleUnauthorizedResponse();
    }
    throw new Error(`User tools request failed: ${resp.status} ${resp.statusText}`);
  }

  const json = await resp.json();
  const root = unwrapPayload(json);
  if (!root) return null;

  return {
    tools: normalizeUserToolEntries(root.tools, root.permissionCodes),
  };
}

function unwrapPayload(raw: unknown): UserToolsPayload | null {
  if (!raw || typeof raw !== "object") return null;
  const record = raw as Record<string, unknown>;
  if (record.data && typeof record.data === "object") return record.data as UserToolsPayload;
  return record as UserToolsPayload;
}

function normalizeUserToolEntries(toolsRaw: unknown, permissionsRaw: unknown): UserToolEntry[] {
  const toolEntries: UserToolEntry[] = [];
  if (Array.isArray(toolsRaw)) {
    toolsRaw.forEach((item, index) => {
      if (!item || typeof item !== "object") return;
      const record = item as Record<string, unknown>;
      const code = stringOr(record.code, "").trim();
      if (!code) return;
      toolEntries.push({
        id: String(record.id ?? code),
        parentId: null,
        name: stringOr(record.name, code),
        icon: stringOr(record.icon, ""),
        description: stringOr(record.description, ""),
        code,
        type: "MENU",
        scope: "TOOL",
        openMode: "PAGE",
        url: stringOr(record.path, ""),
        loadTarget: stringOr(record.component, ""),
        orderNo: toNumber(record.sortNo, index + 1),
        level: normalizeLevel(record.level, record.system),
        enabled: typeof record.enabled === "boolean" ? record.enabled : true,
        guestAccess: record.guestAccess === true,
      });
    });
  }

  const permissionEntries: UserToolEntry[] = [];
  if (Array.isArray(permissionsRaw)) {
    permissionsRaw.forEach((item, index) => {
      const code = typeof item === "string" ? item.trim() : "";
      if (!code) return;
      permissionEntries.push({
        id: `perm:${code}`,
        parentId: null,
        name: code,
        icon: "",
        description: "",
        code,
        type: "DATA",
        scope: "NONE",
        openMode: "NONE",
        url: "",
        loadTarget: "",
        orderNo: toolEntries.length + index + 1,
        level: "CUSTOM",
        enabled: true,
        guestAccess: false,
      });
    });
  }

  return [...toolEntries, ...permissionEntries];
}

function stringOr(value: unknown, fallback: string): string {
  if (typeof value === "string" && value.trim()) return value;
  return fallback;
}

function toNumber(value: unknown, fallback: number): number {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

function normalizeLevel(value: unknown, systemFlag: unknown): UserToolEntryLevel {
  const raw = String(value ?? "").toUpperCase();
  if (raw === "SYSTEM" || raw === "CUSTOM") return raw as UserToolEntryLevel;
  if (systemFlag === true) return "SYSTEM";
  return "CUSTOM";
}
