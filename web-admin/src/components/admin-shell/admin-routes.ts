"use client";

import {
  getResources,
  hasMenuAccess,
  useResources,
} from "@admin/core/registry/resources-registry";
import { resolveResourceIconUrl } from "@admin/core/resource-icon";
import type { ResourceEntry, ResourceNodeType } from "@admin/types/resource-admin";
import { useMemo } from "react";

export interface AdminRouteMeta {
  path: string;
  title: string;
  section: string;
  sectionOrder: number;
  order: number;
  hidden?: boolean;
  resourceId?: string;
  dynamic?: boolean;
}

export interface AdminResolvedRoute {
  route?: AdminRouteMeta;
  exists: boolean;
  accessible: boolean;
}

export interface AdminMenuNode {
  id: string;
  title: string;
  path?: string;
  order: number;
  icon?: string;
  iconUrl: string;
  nodeType: ResourceNodeType;
  hidden?: boolean;
  resourceId?: string;
  children: AdminMenuNode[];
}

const staticAdminRoutes: AdminRouteMeta[] = [
  { path: "/admin", title: "工作台", section: "概览", sectionOrder: 10, order: 10 },
  { path: "/admin/configs", title: "系统配置", section: "平台管理", sectionOrder: 20, order: 10 },
  { path: "/admin/apis", title: "接口管理", section: "平台管理", sectionOrder: 20, order: 20 },
  { path: "/admin/dicts", title: "数据字典", section: "平台管理", sectionOrder: 20, order: 30 },
  { path: "/admin/system-files", title: "系统文件", section: "平台管理", sectionOrder: 20, order: 40 },
  { path: "/admin/diagnostic", title: "诊断工具", section: "平台管理", sectionOrder: 20, order: 50 },
  { path: "/admin/method-stat", title: "方法统计", section: "平台管理", sectionOrder: 20, order: 90, hidden: true },
  { path: "/admin/users", title: "账号管理", section: "权限中心", sectionOrder: 30, order: 10 },
  { path: "/admin/roles", title: "角色管理", section: "权限中心", sectionOrder: 30, order: 20 },
  { path: "/admin/permission-policies", title: "权限策略", section: "权限中心", sectionOrder: 30, order: 30 },
  { path: "/admin/login-logs", title: "登录日志", section: "权限中心", sectionOrder: 30, order: 40 },
  { path: "/admin/audit-logs", title: "审计日志", section: "权限中心", sectionOrder: 30, order: 50 },
  { path: "/admin/web-users", title: "用户管理", section: "用户中心", sectionOrder: 40, order: 10 },
  { path: "/admin/user-feature-packages", title: "应用包管理", section: "用户中心", sectionOrder: 40, order: 20 },
  { path: "/admin/user-feature-applications", title: "应用配置", section: "用户中心", sectionOrder: 40, order: 30 },
  { path: "/admin/profile", title: "个人中心", section: "个人中心", sectionOrder: 90, order: 10, hidden: true },
  { path: "/admin/profile/password", title: "修改密码", section: "个人中心", sectionOrder: 90, order: 20, hidden: true },
  { path: "/admin/profile/preferences", title: "偏好设置", section: "个人中心", sectionOrder: 90, order: 30, hidden: true },
  { path: "/admin/help", title: "问题与帮助", section: "个人中心", sectionOrder: 90, order: 40, hidden: true },
];

function normalizePath(pathname: string): string {
  if (pathname === "/admin/") {
    return "/admin";
  }
  return pathname.endsWith("/") && pathname.length > 1 ? pathname.slice(0, -1) : pathname;
}

export function getAdminRoute(pathname: string): AdminRouteMeta | undefined {
  const normalized = normalizePath(pathname);
  const resourceRoute = buildDynamicRouteList(getResources()).find((route) => route.path === normalized);
  return resourceRoute ?? staticAdminRoutes.find((route) => route.path === normalized);
}

export function getAdminBreadcrumb(pathname: string): Array<{ label: string; href?: string }> {
  const normalized = normalizePath(pathname);
  const resources = getResources();
  if (resources.length > 0) {
    const matched = buildBreadcrumbFromResources(normalized, resources);
    if (matched.length > 0) {
      return matched;
    }
  }

  const route = getAdminRoute(normalized);
  if (!route) {
    return [{ label: "提示" }, { label: "页面不存在" }];
  }
  if (route.hidden) {
    return [{ label: route.title }];
  }
  return [{ label: route.section }, { label: route.title }];
}

export function getCurrentRouteTitle(pathname: string): string {
  return getAdminRoute(pathname)?.title ?? "页面不存在";
}

export function useAdminMenuTree(): AdminMenuNode[] {
  const resources = useResources();
  return useMemo(
    () => (resources.length > 0 ? buildMenuTreeFromResources(resources) : buildFallbackMenuTree()),
    [resources],
  );
}

export function useAdminBreadcrumb(pathname: string): Array<{ label: string; href?: string }> {
  const resources = useResources();
  return useMemo(() => {
    const normalized = normalizePath(pathname);
    if (resources.length > 0) {
      const breadcrumb = buildBreadcrumbFromResources(normalized, resources);
      if (breadcrumb.length > 0) {
        return breadcrumb;
      }
    }
    return getAdminBreadcrumb(normalized);
  }, [pathname, resources]);
}

export function useAdminRouteResolved(pathname: string): AdminResolvedRoute {
  const resources = useResources();
  return useMemo(() => {
    const normalized = normalizePath(pathname);
    const route = resolveAdminRoute(normalized, resources);
    if (!route) {
      return { exists: false, accessible: false };
    }
    if (!route.resourceId) {
      return { route, exists: true, accessible: true };
    }
    const resourceMap = new Map(resources.map((item) => [item.id, item]));
    const resource = resourceMap.get(route.resourceId);
    if (!resource) {
      return { route, exists: true, accessible: false };
    }
    return { route, exists: true, accessible: hasMenuAccess(resource, resourceMap) };
  }, [pathname, resources]);
}

function resolveAdminRoute(pathname: string, resources: ResourceEntry[]): AdminRouteMeta | undefined {
  const dynamicRoute = buildDynamicRouteList(resources).find((route) => route.path === pathname);
  return dynamicRoute ?? staticAdminRoutes.find((route) => route.path === pathname);
}

function buildDynamicRouteList(resources: ResourceEntry[]): AdminRouteMeta[] {
  return flattenMenuRoutes(buildMenuTreeFromResources(resources));
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
      hidden: false,
      resourceId: resource.id,
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

function buildFallbackMenuTree(): AdminMenuNode[] {
  const sectionMap = new Map<string, AdminMenuNode>();

  staticAdminRoutes
    .filter((route) => !route.hidden)
    .sort((a, b) => a.sectionOrder - b.sectionOrder || a.order - b.order)
    .forEach((route) => {
      const key = `${route.sectionOrder}:${route.section}`;
      const section = sectionMap.get(key) ?? {
        id: key,
        title: route.section,
        order: route.sectionOrder,
        iconUrl: resolveResourceIconUrl(null, "DIRECTORY") ?? "",
        nodeType: "DIRECTORY" as ResourceNodeType,
        children: [],
      };
      section.children.push({
        id: route.path,
        title: route.title,
        path: route.path,
        order: route.order,
        iconUrl: resolveResourceIconUrl(null, "MENU") ?? "",
        nodeType: "MENU",
        resourceId: route.resourceId,
        children: [],
      });
      sectionMap.set(key, section);
    });

  return sortMenuTree(Array.from(sectionMap.values()));
}

function flattenMenuRoutes(nodes: AdminMenuNode[]): AdminRouteMeta[] {
  const routes: AdminRouteMeta[] = [];

  const visit = (node: AdminMenuNode, chain: AdminMenuNode[]) => {
    const nextChain = node.nodeType === "DIRECTORY" || node.nodeType === "MENU" ? [...chain, node] : chain;
    if (node.nodeType === "MENU" && node.path) {
      routes.push({
        path: node.path,
        title: node.title,
        section: chain[0]?.title ?? node.title,
        sectionOrder: chain[0]?.order ?? node.order,
        order: node.order,
        hidden: node.hidden,
        resourceId: node.resourceId,
        dynamic: true,
      });
    }
    node.children.forEach((child) => visit(child, nextChain));
  };

  nodes.forEach((node) => visit(node, []));
  return routes.sort((a, b) => a.sectionOrder - b.sectionOrder || a.order - b.order);
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
  if (!target) {
    return [];
  }

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
  return nodes
    .sort(sortByOrder)
    .map((node) => ({
      ...node,
      children: sortMenuTree(node.children).filter(isVisibleMenuNode),
    }));
}

function isAdminTreeResource(resource: ResourceEntry): boolean {
  if (!resource.enabled) return false;
  if (resource.nodeType === "FEATURE" || resource.nodeType === "BUTTON" || resource.type === "FEATURE" || resource.type === "BUTTON") {
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
  if (node.nodeType === "DIRECTORY") {
    return node.children.length > 0;
  }
  if (node.nodeType === "MENU") {
    return Boolean(node.path) || node.children.length > 0;
  }
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

function sortByOrder(a: AdminMenuNode, b: AdminMenuNode): number {
  return a.order - b.order || a.title.localeCompare(b.title, "zh-CN");
}
