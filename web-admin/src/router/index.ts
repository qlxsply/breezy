import type { RouteLocationNormalizedLoadedGeneric, RouteMeta, RouteRecordRaw } from "vue-router";
import { createRouter, createWebHistory } from "vue-router";

import {
  ensureAuthLoaded,
  getCurrentUserType,
  INTERNAL_USER_LANDING_PATH,
} from "../registry/auth.registry";
import { ensureRegistryLoaded } from "../registry/bootstrap";
import {
  ensurePermissionsLoaded,
  hasApiPermission,
  hasMenuAccess,
} from "../registry/permissions.registry";
import {
  findMenuResourceByUrl,
  getResourceMap,
  getResources,
} from "../registry/resources.registry";
import type { ResourceEntry } from "../types/resource-admin";
import { resolveRouteComponent } from "../utils/resourceLoader";

interface AdminNavMeta {
  sectionId: string;
  sectionName: string;
  sectionOrder: number;
  order: number;
  hidden?: boolean;
}

function adminMeta(
  sectionName: string,
  title: string,
  sectionOrder: number,
  order: number,
  extra: Partial<RouteMeta> = {},
): RouteMeta {
  return {
    layout: "admin",
    appArea: "setting",
    header: {
      prefix: sectionName,
      title,
      ...(typeof extra.header === "object" ? extra.header : {}),
    },
    adminNav: {
      sectionId: `admin-section-${sectionOrder}`,
      sectionName,
      sectionOrder,
      order,
      hidden: extra.hidden === true,
    } satisfies AdminNavMeta,
    ...extra,
  };
}

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
    path: "/admin/login",
    name: "admin-login",
    component: () => import("../pages/AdminLoginPage.vue"),
    meta: { layout: "blank" },
  },
  {
    path: "/admin",
    name: "admin-workbench",
    component: () => import("../pages/AdminPlaceholderPage.vue"),
    meta: adminMeta("概览", "工作台", 10, 10, {
      description: "当前工作台页面预留为空白工作区域。",
      showHome: false,
    }),
  },
  {
    path: "/admin/configs",
    name: "settings-system",
    component: () => import("../pages/ConfigsAdminPage.vue"),
    meta: adminMeta("平台管理", "系统配置", 20, 10),
  },
  {
    path: "/admin/apis",
    name: "settings-apis",
    component: () => import("../pages/ApisAdminPage.vue"),
    meta: adminMeta("平台管理", "接口管理", 20, 20),
  },
  {
    path: "/admin/dicts",
    name: "settings-dicts",
    component: () => import("../pages/DictAdminPage.vue"),
    meta: adminMeta("平台管理", "数据字典", 20, 30),
  },
  {
    path: "/admin/system-files",
    name: "settings-system-files",
    component: () => import("../pages/SystemFilesPage.vue"),
    meta: adminMeta("平台管理", "系统文件", 20, 40),
  },
  {
    path: "/admin/diagnostic",
    name: "settings-diagnostic",
    component: () => import("../pages/DiagnosticPage.vue"),
    meta: adminMeta("平台管理", "诊断工具", 20, 50),
  },
  {
    path: "/admin/method-stat",
    name: "settings-method-stat",
    component: () => import("../pages/MethodStatPage.vue"),
    meta: adminMeta("平台管理", "方法统计", 20, 90, {
      hidden: true,
      permissionCode: "mst.stat.view",
    }),
  },
  {
    path: "/admin/users",
    name: "settings-users",
    component: () => import("../pages/UsersAdminPage.vue"),
    meta: adminMeta("权限中心", "账号管理", 30, 10),
  },
  {
    path: "/admin/roles",
    name: "settings-roles",
    component: () => import("../pages/RolesAdminPage.vue"),
    meta: adminMeta("权限中心", "角色管理", 30, 20),
  },
  {
    path: "/admin/permission-policies",
    name: "settings-permission-policies",
    component: () => import("../pages/AdminPlaceholderPage.vue"),
    meta: adminMeta("权限中心", "权限策略", 30, 30, {
      description: "当前权限策略页面预留为空白工作区域。",
    }),
  },
  {
    path: "/admin/login-logs",
    name: "settings-login-logs",
    component: () => import("../pages/LoginLogsPage.vue"),
    meta: adminMeta("权限中心", "登录日志", 30, 40),
  },
  {
    path: "/admin/audit-logs",
    name: "settings-audit-logs",
    component: () => import("../pages/AuditLogsPage.vue"),
    meta: adminMeta("权限中心", "审计日志", 30, 50),
  },
  {
    path: "/admin/web-users",
    name: "settings-web-users",
    component: () => import("../pages/WebUsersAdminPage.vue"),
    meta: adminMeta("用户中心", "用户管理", 40, 10),
  },
  {
    path: "/admin/web-user-stats",
    name: "settings-web-user-groups",
    component: () => import("../pages/WebUserGroupsPage.vue"),
    meta: adminMeta("用户中心", "用户分组", 40, 20),
  },
  {
    path: "/admin/normal-features",
    name: "settings-normal-features",
    component: () => import("../pages/NormalFeatureAdminPage.vue"),
    meta: adminMeta("用户中心", "功能配置", 40, 30),
  },
  {
    path: "/admin/profile",
    name: "admin-profile",
    component: () => import("../pages/AdminProfilePage.vue"),
    meta: adminMeta("个人中心", "个人中心", 90, 10, {
      hidden: true,
      permissionCode: "usr.view",
      standaloneBreadcrumb: true,
      header: { prefix: "", title: "个人中心" },
      selfServiceAdmin: true,
    }),
  },
  {
    path: "/admin/profile/password",
    name: "admin-profile-password",
    component: () => import("../pages/AdminProfilePasswordPage.vue"),
    meta: adminMeta("个人中心", "修改密码", 90, 20, {
      hidden: true,
      standaloneBreadcrumb: true,
      header: { prefix: "", title: "修改密码" },
      selfServiceAdmin: true,
    }),
  },
  {
    path: "/admin/profile/preferences",
    name: "admin-profile-preferences",
    component: () => import("../pages/AdminProfilePreferencesPage.vue"),
    meta: adminMeta("个人中心", "偏好设置", 90, 30, {
      hidden: true,
      standaloneBreadcrumb: true,
      header: { prefix: "", title: "偏好设置" },
      selfServiceAdmin: true,
    }),
  },
  {
    path: "/admin/help",
    name: "admin-help",
    component: () => import("../pages/AdminHelpPage.vue"),
    meta: adminMeta("个人中心", "问题与帮助", 90, 40, {
      hidden: true,
      standaloneBreadcrumb: true,
      header: { prefix: "", title: "问题与帮助" },
      selfServiceAdmin: true,
    }),
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

function resolveHeaderPrefix(resource: ResourceEntry): string {
  if (resource.scope === "SETTING") return "系统";
  if (resource.scope === "TOOL") return "工具";
  if (resource.scope === "INFO") return "信息";
  return "";
}

function buildRouteMeta(resource: ResourceEntry): RouteMeta {
  return {
    appArea: resource.scope === "TOOL" ? "tool" : resource.scope === "SETTING" ? "setting" : "info",
    layout: resource.scope === "SETTING" ? "admin" : "default",
    header: {
      prefix: resolveHeaderPrefix(resource),
      title: resource.name,
    },
  };
}

function resolveRouteArea(
  to: { name?: unknown; path: string; meta?: Record<string, unknown> },
  target?: ResourceEntry,
): "public" | "tool" | "setting" | "info" | "unknown" {
  if (
    to.name === "home" ||
    to.name === "not-found" ||
    to.name === "profile" ||
    to.name === "external-login" ||
    to.name === "admin-login"
  ) {
    return "public";
  }
  if (target) {
    if (target.scope === "TOOL") return "tool";
    if (target.scope === "SETTING") return "setting";
    if (target.scope === "INFO") return "info";
  }
  if (to.path === "/admin" || to.path.startsWith("/admin/")) {
    return "setting";
  }
  const appArea = typeof to.meta?.appArea === "string" ? to.meta.appArea : "";
  if (appArea === "tool" || appArea === "setting" || appArea === "info") {
    return appArea;
  }
  const name = typeof to.name === "string" ? to.name : "";
  if (name.startsWith("tools-") || name === "clinic-management") return "tool";
  if (name.startsWith("settings-") || name.startsWith("admin-")) return "setting";
  return "unknown";
}

function buildDynamicRoutesFromResources(): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = [];
  const resources = getResources();

  resources.forEach((resource) => {
    if (resource.type !== "MENU") return;
    if (resource.scope === "TOOL") return;
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
      await ensureRegistryLoaded();
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

  await ensureRegistryLoaded();
  await ensureAuthLoaded();
  await ensurePermissionsLoaded();

  const resourceMap = getResourceMap();
  const target = findMenuResourceByUrl(to.path);
  const currentUserType = getCurrentUserType();
  const routeArea = resolveRouteArea(to, target);
  const permissionCode = typeof to.meta.permissionCode === "string" ? to.meta.permissionCode : "";

  if (to.name === "external-login" || to.name === "admin-login") {
    return currentUserType === "GUEST"
      ? true
      : { path: resolveLandingPathForAuthenticatedUser(currentUserType), replace: true };
  }

  if (currentUserType === "GUEST" && routeArea === "setting") {
    return { name: "admin-login", replace: true, query: { redirect: to.fullPath } };
  }

  if (currentUserType === "INTERNAL" && (to.name === "home" || routeArea === "tool")) {
    return { path: INTERNAL_USER_LANDING_PATH, replace: true };
  }

  if (currentUserType === "EXTERNAL" && routeArea === "setting") {
    return { name: "home", replace: true };
  }

  if (to.meta?.selfServiceAdmin === true) {
    return currentUserType === "INTERNAL" ? true : { name: "not-found", replace: true };
  }

  if (to.name === "admin-profile") {
    return currentUserType === "INTERNAL" ? true : { name: "not-found", replace: true };
  }

  if (to.name === "admin-workbench") {
    return currentUserType === "INTERNAL" ? true : { name: "not-found", replace: true };
  }

  if (to.name === "home" || to.name === "not-found") return true;

  if (target) {
    if (hasMenuAccess(target, resourceMap)) return true;
  } else {
    if (routeArea === "setting") {
      if (permissionCode && hasApiPermission(permissionCode)) {
        return true;
      }
      const fromHome = Boolean((to as { state?: { fromHome?: boolean } }).state?.fromHome);
      return { name: "not-found", replace: true, state: { fromHome } };
    }
    if (!permissionCode || hasApiPermission(permissionCode)) return true;
  }

  const fromHome = Boolean((to as { state?: { fromHome?: boolean } }).state?.fromHome);
  return { name: "not-found", replace: true, state: { fromHome } };
});

export default router;

function resolveLandingPathForAuthenticatedUser(
  userType: ReturnType<typeof getCurrentUserType>,
): string {
  return userType === "INTERNAL" ? INTERNAL_USER_LANDING_PATH : "/";
}

export function resolveAdminSectionMeta(
  target: Pick<RouteLocationNormalizedLoadedGeneric, "meta">,
): AdminNavMeta | null {
  const nav = target.meta?.adminNav;
  if (!nav || typeof nav !== "object") {
    return null;
  }
  const record = nav as Record<string, unknown>;
  const sectionId = typeof record.sectionId === "string" ? record.sectionId : "";
  const sectionName = typeof record.sectionName === "string" ? record.sectionName : "";
  const sectionOrder = typeof record.sectionOrder === "number" ? record.sectionOrder : 0;
  const order = typeof record.order === "number" ? record.order : 0;
  const hidden = record.hidden === true;
  if (!sectionId || !sectionName) {
    return null;
  }
  return { sectionId, sectionName, sectionOrder, order, hidden };
}
