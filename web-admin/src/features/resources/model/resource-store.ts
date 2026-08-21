"use client";

import { useStoreValue } from "@admin/shared/hooks/useStoreValue";
import { createStore } from "@admin/shared/lib/store";
import { useMemo } from "react";

import type { ResourceEntry } from "./types";

export type ResourceStatus = "idle" | "loading" | "ready" | "error";

export interface ResourceState {
  status: ResourceStatus;
  items: readonly ResourceEntry[];
  byId: ReadonlyMap<string, ResourceEntry>;
  byCode: ReadonlyMap<string, ResourceEntry>;
  byPath: ReadonlyMap<string, ResourceEntry>;
  error: string | null;
}

const EMPTY_STATE: ResourceState = {
  status: "idle",
  items: [],
  byId: new Map(),
  byCode: new Map(),
  byPath: new Map(),
  error: null,
};

const resourceStore = createStore<ResourceState>(EMPTY_STATE);

export function getResourceSnapshot(): ResourceState {
  return resourceStore.getState();
}

export function setResourcesLoading(): void {
  resourceStore.setState({ ...EMPTY_STATE, status: "loading" });
}

export function setResourcesReady(items: readonly ResourceEntry[]): void {
  const byId = new Map<string, ResourceEntry>();
  const byCode = new Map<string, ResourceEntry>();
  const byPath = new Map<string, ResourceEntry>();
  items.forEach((resource) => {
    byId.set(resource.id, resource);
    if (resource.code.trim()) byCode.set(resource.code.trim().toLowerCase(), resource);
    if (resource.type === "MENU" && resource.openMode === "PAGE" && resource.url.trim()) {
      byPath.set(normalizePath(resource.url), resource);
    }
  });
  resourceStore.setState({ status: "ready", items: [...items], byId, byCode, byPath, error: null });
}

export function setResourcesError(error: string): void {
  resourceStore.setState({ ...EMPTY_STATE, status: "error", error });
}

export function resetResourceStore(): void {
  resourceStore.setState(EMPTY_STATE);
}

export function useResourceState(): ResourceState {
  return useStoreValue(resourceStore, (state) => state);
}

export function useResourceStatus(): ResourceStatus {
  return useStoreValue(resourceStore, (state) => state.status);
}

export function useResources(): readonly ResourceEntry[] {
  return useStoreValue(resourceStore, (state) => state.items);
}

export function usePermission(code: string): boolean {
  const state = useResourceState();
  return hasPermission(state, code);
}

export function usePermissions(codes: readonly string[]): ReadonlySet<string> {
  const state = useResourceState();
  const key = codes.join("\u0000");
  return useMemo(
    () => new Set(codes.filter((code) => hasPermission(state, code))),
    [codes, key, state],
  );
}

export function hasPermission(state: ResourceState, code: string): boolean {
  const normalizedCode = code.trim().toLowerCase();
  if (state.status !== "ready" || !normalizedCode) return false;
  const resource = state.byCode.get(normalizedCode);
  return resource ? hasResourceAccess(state, resource.id) : false;
}

export function hasMenuAccess(state: ResourceState, resourceId: string): boolean {
  if (state.status !== "ready") return false;
  const resource = state.byId.get(resourceId);
  return Boolean(resource?.type === "MENU" && hasResourceAccess(state, resourceId));
}

export function findMenuResourceByPath(
  state: ResourceState,
  path: string,
): ResourceEntry | undefined {
  if (state.status !== "ready") return undefined;
  return state.byPath.get(normalizePath(path));
}

function hasResourceAccess(
  state: ResourceState,
  resourceId: string,
  visited = new Set<string>(),
): boolean {
  const resource = state.byId.get(resourceId);
  if (!resource?.enabled || visited.has(resourceId)) return false;
  visited.add(resourceId);
  if (!resource.parentId) return true;
  if (!state.byId.has(resource.parentId)) return false;
  return hasResourceAccess(state, resource.parentId, visited);
}

function normalizePath(path: string): string {
  const trimmed = path.trim();
  if (!trimmed) return "/";
  return trimmed.length > 1 && trimmed.endsWith("/") ? trimmed.slice(0, -1) : trimmed;
}
