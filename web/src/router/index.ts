import type { RouteMeta, RouteRecordRaw } from "vue-router";
import { createRouter, createWebHistory } from "vue-router";

import { ensureAuthLoaded, getCurrentUserType } from "../registry/auth.registry";
import {
  ensureUserToolPermissionsLoaded,
  hasToolPageAccess,
  hasUserPermissionCode,
} from "../registry/user-tool-permissions.registry";
import {
  ensureUserToolsLoaded,
  findUserToolByUrl,
  getUserToolMap,
  getUserTools,
} from "../registry/user-tools.registry";
import type { UserToolEntry } from "../types/user-tools";
import { resolveRouteComponent } from "../utils/resourceLoader";

const staticRoutes: RouteRecordRaw[] = [
  {
    path: "/",
    name: "home",
    component: () => import("../pages/HomePage.vue"),
    meta: { header: { showHome: false } },
  },
  {
    path: "/login",
    name: "external-login",
    component: () => import("../pages/ExternalLoginPage.vue"),
    meta: { layout: "blank" },
  },
  {
    path: "/register",
    name: "external-register",
    component: () => import("../pages/ExternalRegisterPage.vue"),
    meta: { layout: "blank" },
  },
  {
    path: "/datasource",
    name: "tools-datasource",
    component: () => import("../pages/DataSourceAdminPage.vue"),
    meta: { header: { prefix: "工具", title: "数据源管理" }, appArea: "tool" },
  },
  {
    path: "/database-schemas",
    name: "tools-database-schema",
    component: () => import("../pages/DatabaseSchemaPage.vue"),
    meta: { header: { prefix: "工具", title: "数据库管理" }, appArea: "tool" },
  },
  {
    path: "/storage",
    name: "tools-storage",
    component: () => import("../pages/StoragePage.vue"),
    meta: { header: { prefix: "工具", title: "文件存储" }, appArea: "tool" },
  },
  {
    path: "/schemaforge",
    name: "tools-schemaforge",
    component: () => import("../pages/SchemaForgePage.vue"),
    meta: { header: { prefix: "工具", title: "结构工厂" }, appArea: "tool" },
  },
  {
    path: "/jsonfmt",
    name: "tools-jsonfmt",
    component: () => import("../pages/JsonFormatterPage.vue"),
    meta: { header: { prefix: "工具", title: "JSON 格式化" }, appArea: "tool" },
  },
  {
    path: "/todo",
    name: "tools-todo",
    component: () => import("../pages/TodoBoardPage.vue"),
    meta: { header: { prefix: "工具", title: "待处理事项" }, appArea: "tool" },
  },
  {
    path: "/todo/all",
    name: "tools-todo-all",
    component: () => import("../pages/TodoAllPage.vue"),
    meta: {
      header: { prefix: "工具", title: "待办全部查看" },
      permissionCode: "tdo.use",
      appArea: "tool",
    },
  },
  {
    path: "/database-schemas/:id/metadata",
    name: "tools-metadata-browser",
    component: () => import("../pages/MetadataBrowserPage.vue"),
    meta: {
      header: { prefix: "工具", title: "元数据浏览" },
      permissionCode: "mdb.meta.view",
      appArea: "tool",
    },
  },
  {
    path: "/clinic/management",
    name: "clinic-management",
    component: () => import("../pages/clinic/ClinicManagementPage.vue"),
    meta: { header: { prefix: "工具", title: "诊所管理" }, appArea: "tool" },
  },
  {
    path: "/profile",
    name: "profile",
    component: () => import("../pages/ProfilePage.vue"),
    meta: { header: { prefix: "个人", title: "Profile" }, appArea: "info" },
  },
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: () => import("../pages/NotFoundPage.vue"),
    meta: { header: { prefix: "提示", title: "页面不存在" } },
  },
];

const staticRouteNames = new Set(
  staticRoutes
    .map((route) => route.name)
    .filter((name): name is string => typeof name === "string"),
);
const staticRoutePaths = new Set(staticRoutes.map((route) => route.path));

function resolveHeaderPrefix(resource: UserToolEntry): string {
  if (resource.scope === "TOOL") return "工具";
  return "";
}

function buildRouteMeta(resource: UserToolEntry): RouteMeta {
  return {
    appArea: "tool",
    header: {
      prefix: resolveHeaderPrefix(resource),
      title: resource.name,
    },
  };
}

function resolveRouteArea(
  to: { name?: unknown; path: string; meta?: Record<string, unknown> },
  target?: UserToolEntry,
): "public" | "tool" | "info" | "unknown" {
  if (
    to.name === "home" ||
    to.name === "not-found" ||
    to.name === "profile" ||
    to.name === "external-login"
  ) {
    return "public";
  }
  if (target) {
    if (target.scope === "TOOL") return "tool";
  }
  const appArea = typeof to.meta?.appArea === "string" ? to.meta.appArea : "";
  if (appArea === "tool" || appArea === "info") {
    return appArea;
  }
  const name = typeof to.name === "string" ? to.name : "";
  if (name.startsWith("tools-") || name === "clinic-management") return "tool";
  return "unknown";
}

function buildDynamicRoutesFromResources(): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = [];
  const resources = getUserTools();

  resources.forEach((resource) => {
    if (resource.type !== "MENU") return;
    if (resource.scope !== "TOOL") return;
    if (resource.openMode !== "PAGE") return;
    if (!resource.url) return;
    if (!resource.loadTarget) return;
    if (staticRoutePaths.has(resource.url)) return;
    if (staticRouteNames.has(resource.id)) return;

    const component = resolveRouteComponent(resource.loadTarget);
    if (!component) return;

    routes.push({
      path: resource.url,
      name: resource.id,
      component,
      meta: buildRouteMeta(resource),
    });
  });

  return routes;
}

let hasLoadedRoutes = false;
let loadingPromise: Promise<void> | null = null;

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: staticRoutes,
});

export async function initDynamicRoutes(): Promise<void> {
  if (loadingPromise) {
    await loadingPromise;
    return;
  }

  loadingPromise = (async () => {
    try {
      await ensureUserToolsLoaded();
      const dynamicRoutes = buildDynamicRoutesFromResources();

      const existingRouteNames = router
        .getRoutes()
        .map((route) => route.name)
        .filter((name): name is string => typeof name === "string")
        .filter((name) => !staticRouteNames.has(name));

      existingRouteNames.forEach((name) => {
        router.removeRoute(name);
      });

      dynamicRoutes.forEach((routeConfig) => {
        router.addRoute(routeConfig);
      });
    } catch (error) {
      console.error("load dynamic routes failed", error);
    }
  })();

  try {
    await loadingPromise;
  } finally {
    loadingPromise = null;
    hasLoadedRoutes = true;
  }
}

async function ensureDynamicRoutes(): Promise<boolean> {
  if (hasLoadedRoutes) return false;
  await initDynamicRoutes();
  return true;
}

router.beforeEach(async (to) => {
  const loaded = await ensureDynamicRoutes();
  if (loaded) {
    return { ...to, replace: true };
  }

  await ensureUserToolsLoaded();
  await ensureAuthLoaded();
  await ensureUserToolPermissionsLoaded();

  const resourceMap = getUserToolMap();
  const target = findUserToolByUrl(to.path);
  const currentUserType = getCurrentUserType();
  const routeArea = resolveRouteArea(to, target);
  const permissionCode = typeof to.meta.permissionCode === "string" ? to.meta.permissionCode : "";

  if (to.name === "external-login") {
    return currentUserType === "GUEST" ? true : { path: "/", replace: true };
  }

  if (to.name === "home" || to.name === "not-found") return true;

  if (target) {
    if (hasToolPageAccess(target, resourceMap)) return true;
  } else {
    if (!permissionCode || hasUserPermissionCode(permissionCode)) return true;
  }

  const fromHome = Boolean((to as { state?: { fromHome?: boolean } }).state?.fromHome);
  return {
    name: "not-found",
    replace: true,
    state: { fromHome: routeArea === "public" ? fromHome : false },
  };
});

export default router;
