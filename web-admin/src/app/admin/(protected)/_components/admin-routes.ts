"use client";

import { resolveResourceIconUrl } from "@admin/features/resources/model/resource-icon";
import { useResources } from "@admin/features/resources/model/resource-store";
import type { ResourceEntry, ResourceNodeType } from "@admin/features/resources/model/types";
import { useMemo } from "react";

export interface AdminMenuNode {
  id: string;
  title: string;
  path?: string;
  order: number;
  icon?: string;
  iconUrl: string;
  nodeType: ResourceNodeType;
  children: AdminMenuNode[];
}

interface AdminSelfServiceRoute {
  path: string;
  title: string;
}

const SELF_SERVICE_ROUTES: readonly AdminSelfServiceRoute[] = [
  { path: "/admin/profile", title: "个人中心" },
  { path: "/admin/profile/password", title: "修改密码" },
  { path: "/admin/profile/preferences", title: "偏好设置" },
  { path: "/admin/help", title: "问题与帮助" },
];

export function getAdminSelfServiceRoute(pathname: string): AdminSelfServiceRoute | undefined {
  const normalized = normalizePath(pathname);
  return SELF_SERVICE_ROUTES.find((route) => route.path === normalized);
}

export function useAdminMenuTree(): AdminMenuNode[] {
  const resources = useResources();
  return useMemo(() => buildMenuTreeFromResources([...resources]), [resources]);
}

export function useAdminBreadcrumb(pathname: string): Array<{ label: string; href?: string }> {
  const resources = useResources();
  return useMemo(() => {
    const normalized = normalizePath(pathname);
    const resourceBreadcrumb = buildBreadcrumbFromResources(normalized, [...resources]);
    if (resourceBreadcrumb.length > 0) return resourceBreadcrumb;

    const selfServiceRoute = getAdminSelfServiceRoute(normalized);
    return selfServiceRoute ? [{ label: selfServiceRoute.title }] : [{ label: "后台" }];
  }, [pathname, resources]);
}

function buildMenuTreeFromResources(resources: ResourceEntry[]): AdminMenuNode[] {
  const nodes = new Map<string, AdminMenuNode>();
  const roots: AdminMenuNode[] = [];

  resources.filter(isAdminTreeResource).forEach((resource) => {
    nodes.set(resource.id, {
      id: resource.id,
      title: resource.name,
      path: isPageMenuResource(resource) ? normalizeResourcePath(resource.url) : undefined,
      order: resource.orderNo ?? 0,
      icon: resource.icon,
      iconUrl:
        resolveResourceIconUrl(resource.icon, resource.nodeType ?? resource.type) ??
        resolveResourceIconUrl(null, resource.nodeType === "DIRECTORY" ? "DIRECTORY" : "MENU") ??
        "",
      nodeType: resource.nodeType ?? resource.type,
      children: [],
    });
  });

  resources.filter(isAdminTreeResource).forEach((resource) => {
    const node = nodes.get(resource.id);
    if (!node) return;
    if (!resource.parentId || !nodes.has(resource.parentId)) {
      roots.push(node);
      return;
    }
    nodes.get(resource.parentId)?.children.push(node);
  });

  return sortMenuTree(roots).filter(isVisibleMenuNode);
}

function buildBreadcrumbFromResources(
  pathname: string,
  resources: ResourceEntry[],
): Array<{ label: string; href?: string }> {
  const target = resources.find(
    (resource) =>
      resource.type === "MENU" &&
      resource.openMode === "PAGE" &&
      normalizeResourcePath(resource.url) === pathname,
  );
  if (!target) return [];

  const map = new Map(resources.map((resource) => [resource.id, resource]));
  const chain: ResourceEntry[] = [];
  let current: ResourceEntry | undefined = target;
  const visited = new Set<string>();
  while (current && !visited.has(current.id)) {
    visited.add(current.id);
    chain.unshift(current);
    current = current.parentId ? map.get(current.parentId) : undefined;
  }

  return chain.map((item, index) => ({
    label: item.name,
    href:
      item.openMode === "PAGE" && index < chain.length - 1
        ? normalizeResourcePath(item.url)
        : undefined,
  }));
}

function sortMenuTree(nodes: AdminMenuNode[]): AdminMenuNode[] {
  return nodes.sort(sortByOrder).map((node) => ({
    ...node,
    children: sortMenuTree(node.children).filter(isVisibleMenuNode),
  }));
}

function isAdminTreeResource(resource: ResourceEntry): boolean {
  if (!resource.enabled) return false;
  if (
    resource.nodeType === "FUNCTION" ||
    resource.nodeType === "FEATURE" ||
    resource.nodeType === "BUTTON" ||
    resource.type === "FUNCTION" ||
    resource.type === "FEATURE" ||
    resource.type === "BUTTON"
  ) {
    return false;
  }
  if (resource.nodeType === "DIRECTORY") return true;
  if (resource.type !== "MENU") return false;
  if (resource.openMode === "NONE") return true;
  return Boolean(resource.url?.startsWith("/admin"));
}

function isPageMenuResource(resource: ResourceEntry): boolean {
  return resource.type === "MENU" && resource.openMode === "PAGE" && Boolean(resource.url);
}

function isVisibleMenuNode(node: AdminMenuNode): boolean {
  if (node.nodeType === "DIRECTORY") return node.children.length > 0;
  if (node.nodeType === "MENU") return Boolean(node.path) || node.children.length > 0;
  return false;
}

function normalizeResourcePath(url: string): string {
  const normalized = normalizePath(url || "/admin");
  if (normalized === "/admin") return "/admin";
  if (!normalized.startsWith("/admin")) {
    return `/admin${normalized.startsWith("/") ? normalized : `/${normalized}`}`;
  }
  return normalized;
}

function normalizePath(pathname: string): string {
  if (pathname === "/admin/") return "/admin";
  return pathname.endsWith("/") && pathname.length > 1 ? pathname.slice(0, -1) : pathname;
}

function sortByOrder(a: AdminMenuNode, b: AdminMenuNode): number {
  return a.order - b.order || a.title.localeCompare(b.title, "zh-CN");
}
