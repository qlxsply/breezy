"use client";

import { hasMenuAccess } from "@admin/core/registry/permissions-registry";
import { getResources, useResources } from "@admin/core/registry/resources-registry";
import type { ResourceEntry } from "@admin/types/resource-admin";
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

export interface AdminMenuSection {
  id: string;
  title: string;
  items: AdminRouteMeta[];
}

const staticAdminRoutes: AdminRouteMeta[] = [
  { path: "/admin", title: "工作台", section: "概览", sectionOrder: 10, order: 10 },
  { path: "/admin/configs", title: "系统配置", section: "平台管理", sectionOrder: 20, order: 10 },
  { path: "/admin/apis", title: "接口管理", section: "平台管理", sectionOrder: 20, order: 20 },
  { path: "/admin/dicts", title: "数据字典", section: "平台管理", sectionOrder: 20, order: 30 },
  {
    path: "/admin/system-files",
    title: "系统文件",
    section: "平台管理",
    sectionOrder: 20,
    order: 40,
  },
  {
    path: "/admin/diagnostic",
    title: "诊断工具",
    section: "平台管理",
    sectionOrder: 20,
    order: 50,
  },
  {
    path: "/admin/method-stat",
    title: "方法统计",
    section: "平台管理",
    sectionOrder: 20,
    order: 90,
    hidden: true,
  },
  { path: "/admin/users", title: "账号管理", section: "权限中心", sectionOrder: 30, order: 10 },
  { path: "/admin/roles", title: "角色管理", section: "权限中心", sectionOrder: 30, order: 20 },
  {
    path: "/admin/permission-policies",
    title: "权限策略",
    section: "权限中心",
    sectionOrder: 30,
    order: 30,
  },
  {
    path: "/admin/login-logs",
    title: "登录日志",
    section: "权限中心",
    sectionOrder: 30,
    order: 40,
  },
  {
    path: "/admin/audit-logs",
    title: "审计日志",
    section: "权限中心",
    sectionOrder: 30,
    order: 50,
  },
  { path: "/admin/web-users", title: "用户管理", section: "用户中心", sectionOrder: 40, order: 10 },
  {
    path: "/admin/user-feature-packages",
    title: "应用包管理",
    section: "用户中心",
    sectionOrder: 40,
    order: 20,
  },
  {
    path: "/admin/user-feature-applications",
    title: "应用配置",
    section: "用户中心",
    sectionOrder: 40,
    order: 30,
  },
  {
    path: "/admin/profile",
    title: "个人中心",
    section: "个人中心",
    sectionOrder: 90,
    order: 10,
    hidden: true,
  },
  {
    path: "/admin/profile/password",
    title: "修改密码",
    section: "个人中心",
    sectionOrder: 90,
    order: 20,
    hidden: true,
  },
  {
    path: "/admin/profile/preferences",
    title: "偏好设置",
    section: "个人中心",
    sectionOrder: 90,
    order: 30,
    hidden: true,
  },
  {
    path: "/admin/help",
    title: "问题与帮助",
    section: "个人中心",
    sectionOrder: 90,
    order: 40,
    hidden: true,
  },
];

export interface AdminResolvedRoute {
  route?: AdminRouteMeta;
  exists: boolean;
  accessible: boolean;
}

interface AdminMenuNode {
  id: string;
  title: string;
  path?: string;
  order: number;
  hidden?: boolean;
  resourceId?: string;
  children: AdminMenuNode[];
}

function normalizePath(pathname: string): string {
  if (pathname === "/admin/") {
    return "/admin";
  }
  return pathname.endsWith("/") && pathname.length > 1 ? pathname.slice(0, -1) : pathname;
}

export function getAdminRoute(pathname: string): AdminRouteMeta | undefined {
  const normalized = normalizePath(pathname);
  const resourceRoute = getAdminDynamicRoutes().find((route) => route.path === normalized);
  return resourceRoute ?? staticAdminRoutes.find((route) => route.path === normalized);
}

export function getAdminMenuSections(): AdminMenuSection[] {
  const resources = getResources();
  if (resources.length > 0) {
    return buildMenuSectionsFromResources(resources);
  }

  const sectionMap = new Map<string, AdminMenuSection>();

  staticAdminRoutes
    .filter((route) => !route.hidden)
    .sort((a, b) => a.sectionOrder - b.sectionOrder || a.order - b.order)
    .forEach((route) => {
      const key = `${route.sectionOrder}:${route.section}`;
      const section = sectionMap.get(key) ?? {
        id: key,
        title: route.section,
        items: [],
      };
      section.items.push(route);
      sectionMap.set(key, section);
    });

  return Array.from(sectionMap.values());
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
  if (route.path === "/admin") {
    return [{ label: route.section }, { label: route.title }];
  }
  return [{ label: route.section }, { label: route.title }];
}

export function getVisitedTabs(
  pathname: string,
): Array<{ title: string; href: string; pinned: boolean }> {
  const currentRoute = getAdminRoute(pathname);
  const tabs = [{ title: "工作台", href: "/admin", pinned: true }];
  if (currentRoute && currentRoute.path !== "/admin") {
    tabs.push({ title: currentRoute.title, href: currentRoute.path, pinned: false });
  }
  return tabs;
}

export function getCurrentRouteTitle(pathname: string): string {
  return getAdminRoute(pathname)?.title ?? "页面不存在";
}

export function getAdminResolvedRoute(pathname: string): AdminResolvedRoute {
  const normalized = normalizePath(pathname);
  const route = getAdminRoute(normalized);
  if (!route) {
    return { exists: false, accessible: false };
  }

  if (!route.resourceId) {
    return { route, exists: true, accessible: true };
  }

  const resource = getResources().find((item) => item.id === route.resourceId);
  if (!resource) {
    return { route, exists: true, accessible: false };
  }

  const accessible = hasMenuAccess(
    resource,
    new Map(getResources().map((item) => [item.id, item])),
  );
  return { route, exists: true, accessible };
}

export function useAdminMenuSections(): AdminMenuSection[] {
  const resources = useResources();
  return useMemo(
    () =>
      resources.length > 0 ? buildMenuSectionsFromResources(resources) : getAdminMenuSections(),
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
    return {
      route,
      exists: true,
      accessible: hasMenuAccess(resource, resourceMap),
    };
  }, [pathname, resources]);
}

function resolveAdminRoute(
  pathname: string,
  resources: ResourceEntry[],
): AdminRouteMeta | undefined {
  const dynamicRoute = buildMenuSectionsFromResources(resources)
    .flatMap((section) => section.items)
    .find((route) => route.path === pathname);
  return dynamicRoute ?? staticAdminRoutes.find((route) => route.path === pathname);
}

function getAdminDynamicRoutes(): AdminRouteMeta[] {
  return buildMenuSectionsFromResources(getResources()).flatMap((section) => section.items);
}

function buildMenuSectionsFromResources(resources: ResourceEntry[]): AdminMenuSection[] {
  const map = new Map<string, AdminMenuNode>();
  const roots: AdminMenuNode[] = [];

  resources.filter(isAdminMenuResource).forEach((resource) => {
    map.set(resource.id, {
      id: resource.id,
      title: resource.name,
      path: resource.openMode === "PAGE" ? normalizeResourcePath(resource.url) : undefined,
      order: resource.orderNo ?? 0,
      hidden: false,
      resourceId: resource.id,
      children: [],
    });
  });

  resources.filter(isAdminMenuResource).forEach((resource) => {
    const node = map.get(resource.id);
    if (!node) return;
    if (!resource.parentId || !map.has(resource.parentId)) {
      roots.push(node);
      return;
    }
    map.get(resource.parentId)?.children.push(node);
  });

  const sections = roots
    .sort(sortByOrder)
    .map((root, index) => ({
      id: root.id,
      title: root.title,
      items: flattenMenuLeafRoutes(root, root.title, (index + 1) * 10),
    }))
    .filter((section) => section.items.length > 0);

  return sections;
}

function flattenMenuLeafRoutes(
  node: AdminMenuNode,
  section: string,
  sectionOrder: number,
): AdminRouteMeta[] {
  if (node.children.length === 0) {
    if (!node.path) {
      return [];
    }
    return [
      {
        path: node.path,
        title: node.title,
        section,
        sectionOrder,
        order: node.order,
        hidden: node.hidden,
        resourceId: node.resourceId,
        dynamic: true,
      },
    ];
  }

  return node.children
    .sort(sortByOrder)
    .flatMap((child) => flattenMenuLeafRoutes(child, section, sectionOrder));
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

function isAdminMenuResource(resource: ResourceEntry): boolean {
  if (resource.type !== "MENU" || !resource.enabled) return false;
  if (resource.openMode === "NONE") return true;
  return Boolean(resource.url?.startsWith("/admin"));
}

function normalizeResourcePath(url: string): string {
  const normalized = normalizePath(url || "/admin");
  if (normalized === "/admin") return "/admin";
  if (!normalized.startsWith("/admin"))
    return `/admin${normalized.startsWith("/") ? normalized : `/${normalized}`}`;
  return normalized;
}

function sortByOrder(a: AdminMenuNode, b: AdminMenuNode): number {
  return a.order - b.order || a.title.localeCompare(b.title, "zh-CN");
}
