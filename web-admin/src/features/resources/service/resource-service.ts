import { fetchResourceBootstrap } from "@admin/features/resources/api/resource-bootstrap-client";
import {
  getResourceSnapshot,
  resetResourceStore,
  setResourcesError,
  setResourcesLoading,
  setResourcesReady,
} from "@admin/features/resources/model/resource-store";
import type {
  ResourceEntry,
  ResourceLevel,
  ResourceNodeType,
  ResourceOpenMode,
  ResourceScope,
  ResourceType,
} from "@admin/features/resources/model/types";

let loadingPromise: Promise<void> | null = null;
let resourceGeneration = 0;

export function loadResources(
  options: { force?: boolean; signal?: AbortSignal } = {},
): Promise<void> {
  const state = getResourceSnapshot();
  if (!options.force && state.status === "ready") return Promise.resolve();
  if (!options.force && loadingPromise) return loadingPromise;

  const generation = ++resourceGeneration;
  setResourcesLoading();
  loadingPromise = fetchResourceBootstrap({ signal: options.signal })
    .then((raw) => {
      if (generation !== resourceGeneration) return;
      setResourcesReady(normalizeResources(raw));
    })
    .catch((error: unknown) => {
      if (generation !== resourceGeneration) return;
      if (error instanceof DOMException && error.name === "AbortError") throw error;
      setResourcesError(error instanceof Error ? error.message : "后台资源加载失败");
      throw error;
    })
    .finally(() => {
      if (generation === resourceGeneration) loadingPromise = null;
    });
  return loadingPromise;
}

export function refreshResources(signal?: AbortSignal): Promise<void> {
  return loadResources({ force: true, signal });
}

export function resetResources(): void {
  resourceGeneration += 1;
  loadingPromise = null;
  resetResourceStore();
}

function normalizeResources(raw: unknown): ResourceEntry[] {
  if (!Array.isArray(raw)) return [];

  const resources: ResourceEntry[] = [];
  const visit = (items: unknown[], parentId: string | null) => {
    items.forEach((item, index) => {
      if (!item || typeof item !== "object") return;
      const record = item as Record<string, unknown>;
      const code = stringOr(record.code ?? record.shortcut ?? record.cmd, "").trim();
      if (!code) return;

      const id = String(record.id ?? code);
      const nodeType = normalizeNodeType(String(record.type ?? "").toUpperCase());
      const type = normalizeType(nodeType);
      const rawParent = record.parentId ?? record.parent_id ?? null;
      resources.push({
        id,
        parentId:
          rawParent === null || rawParent === undefined || rawParent === ""
            ? parentId
            : String(rawParent),
        nodeType,
        name: stringOr(record.name, code),
        icon: stringOr(record.icon, ""),
        description: stringOr(record.description, ""),
        code,
        type,
        scope: normalizeScope(record.scope, type, record.url ?? record.path, nodeType),
        openMode: normalizeOpenMode(record.openMode, type, nodeType),
        url: stringOr(record.url ?? record.path, ""),
        orderNo: toNumber(record.orderNo ?? record.weight, index + 1),
        level: normalizeLevel(record.level, record.system),
        enabled: typeof record.enabled === "boolean" ? record.enabled : true,
        guestAccess: record.guestAccess === true,
      });

      const children = Array.isArray(record.children) ? record.children : [];
      if (children.length > 0) visit(children, id);
    });
  };
  visit(raw, null);
  return resources;
}

function stringOr(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim() ? value : fallback;
}

function toNumber(value: unknown, fallback: number): number {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function normalizeNodeType(raw: string): ResourceNodeType {
  if (["DIRECTORY", "MENU", "BUTTON", "FUNCTION", "FEATURE", "DATA"].includes(raw)) {
    return raw as ResourceNodeType;
  }
  return "MENU";
}

function normalizeType(nodeType: ResourceNodeType): ResourceType {
  return nodeType === "DIRECTORY" || nodeType === "MENU" ? "MENU" : nodeType;
}

function normalizeScope(
  value: unknown,
  type: ResourceType,
  urlValue: unknown,
  nodeType: ResourceNodeType,
): ResourceScope {
  if (type !== "MENU") return "NONE";
  if (nodeType === "DIRECTORY") return "SETTING";
  const raw = String(value ?? "").toUpperCase();
  if (raw === "TOOL" || raw === "SETTING" || raw === "INFO") return raw;
  return String(urlValue ?? "")
    .trim()
    .startsWith("/admin")
    ? "SETTING"
    : "NONE";
}

function normalizeOpenMode(
  value: unknown,
  type: ResourceType,
  nodeType: ResourceNodeType,
): ResourceOpenMode {
  if (nodeType === "DIRECTORY") return "NONE";
  const raw = String(value ?? "").toUpperCase();
  if (raw === "MODAL" || raw === "PAGE" || raw === "NONE") return raw;
  return type === "MENU" ? "PAGE" : "NONE";
}

function normalizeLevel(value: unknown, systemFlag: unknown): ResourceLevel {
  const raw = String(value ?? "").toUpperCase();
  if (raw === "SYSTEM" || raw === "CUSTOM") return raw;
  return systemFlag === true ? "SYSTEM" : "CUSTOM";
}
