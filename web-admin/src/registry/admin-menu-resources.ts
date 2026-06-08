import { fetchAdminMenuResources } from "@admin/api/admin-menu-resources";
import type { AdminMenuResourceEntry, AdminMenuTreeNode } from "@admin/types/admin-menu-resource";
import { computed, ref } from "vue";

const resources = ref<AdminMenuResourceEntry[]>([]);

const state = {
  loaded: false,
  loading: false,
  promise: null as Promise<void> | null,
};

export const adminMenuResources = computed(() =>
  resources.value.filter((resource) => resource.type === "MENU" && resource.enabled),
);
export const adminMenuTree = computed(() => buildAdminMenuTree(adminMenuResources.value));
export const adminMenuResourceMap = computed(() => {
  const map = new Map<string, AdminMenuResourceEntry>();
  resources.value.forEach((resource) => {
    map.set(resource.id, resource);
  });
  return map;
});

export function useAdminMenuResources() {
  return adminMenuResources;
}

export function useAdminMenuTree() {
  return adminMenuTree;
}

export function getAdminMenuResourceMap(): Map<string, AdminMenuResourceEntry> {
  return adminMenuResourceMap.value;
}

export function findAdminMenuResourceByCode(code: string): AdminMenuResourceEntry | undefined {
  const trimmed = code.trim().toLowerCase();
  return resources.value.find((resource) => resource.code.toLowerCase() === trimmed);
}

export async function ensureAdminMenuResourcesLoaded(force = false): Promise<void> {
  if (!force && state.loaded) {
    return state.promise ?? Promise.resolve();
  }
  if (!force && state.loading) {
    return state.promise ?? Promise.resolve();
  }
  state.loading = true;
  state.promise = (async () => {
    try {
      resources.value = await fetchAdminMenuResources();
    } finally {
      state.loaded = true;
      state.loading = false;
    }
  })();
  return state.promise;
}

export function normalizeAdminMenuPath(url: string): string {
  const cleaned = url.trim();
  if (!cleaned || cleaned === "/admin") {
    return "/";
  }
  return cleaned.startsWith("/admin/") ? cleaned.slice(6) : cleaned;
}

function buildAdminMenuTree(items: AdminMenuResourceEntry[]): AdminMenuTreeNode[] {
  const map = new Map<string, AdminMenuTreeNode>();
  const roots: AdminMenuTreeNode[] = [];

  items.forEach((item) => {
    map.set(item.id, {
      id: item.id,
      name: item.name,
      code: item.code,
      to: item.openMode === "PAGE" && item.url ? normalizeAdminMenuPath(item.url) : undefined,
      children: [],
      orderNo: item.orderNo,
    });
  });

  items.forEach((item) => {
    const node = map.get(item.id);
    if (!node) {
      return;
    }
    if (!item.parentId) {
      roots.push(node);
      return;
    }
    const parent = map.get(item.parentId);
    if (!parent) {
      roots.push(node);
      return;
    }
    parent.children.push(node);
  });

  sortNodes(roots);
  return roots;
}

function sortNodes(nodes: AdminMenuTreeNode[]): void {
  nodes.sort((left, right) => left.orderNo - right.orderNo);
  nodes.forEach((node) => sortNodes(node.children));
}
