import {
  ensureAdminMenuResourcesLoaded,
  normalizeAdminMenuPath,
  useAdminMenuResources,
} from "@admin/registry/admin-menu-resources";
import { ensureAdminPermissionsLoaded } from "@admin/registry/admin-permissions";
import { ensureAuthLoaded, isAuthenticated } from "@admin/registry/auth";
import type { RouteRecordRaw } from "vue-router";
import { createRouter, createWebHistory } from "vue-router";

function resolveAdminPageComponent(path: string) {
  switch (path) {
    case "/profile":
      return () => import("@admin/pages/AdminProfilePage.vue");
    case "/profile/password":
      return () => import("@admin/pages/AdminProfilePasswordPage.vue");
    case "/profile/preferences":
      return () => import("@admin/pages/AdminProfilePreferencesPage.vue");
    case "/help":
      return () => import("@admin/pages/AdminHelpPage.vue");
    case "/method-stat":
      return () => import("@admin/pages/MethodStatPage.vue");
    case "/dicts":
      return () => import("@admin/pages/DictAdminPage.vue");
    case "/login-logs":
      return () => import("@admin/pages/LoginLogsPage.vue");
    case "/audit-logs":
      return () => import("@admin/pages/AuditLogsPage.vue");
    case "/web-users":
      return () => import("@admin/pages/WebUsersAdminPage.vue");
    case "/web-user-stats":
      return () => import("@admin/pages/WebUserGroupsPage.vue");
    case "/normal-features":
      return () => import("@admin/pages/NormalFeatureAdminPage.vue");
    case "/system-files":
      return () => import("@admin/pages/SystemFilesAdminPage.vue");
    case "/diagnostic":
      return () => import("@admin/pages/DiagnosticAdminPage.vue");
    default:
      return () => import("@admin/pages/AdminPagePlaceholder.vue");
  }
}

const staticRoutes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "admin-login",
    component: () => import("@admin/pages/AdminLoginPage.vue"),
    meta: { layout: "blank", title: "后台登录" },
  },
  {
    path: "/",
    name: "admin-workbench",
    component: () => import("@admin/pages/AdminWorkbenchPage.vue"),
    meta: { title: "工作台" },
  },
  {
    path: "/profile",
    name: "admin-profile",
    component: () => import("@admin/pages/AdminProfilePage.vue"),
    meta: { title: "个人中心", originalPath: "/admin/profile" },
  },
  {
    path: "/profile/password",
    name: "admin-profile-password",
    component: () => import("@admin/pages/AdminProfilePasswordPage.vue"),
    meta: { title: "修改密码", originalPath: "/admin/profile/password" },
  },
  {
    path: "/profile/preferences",
    name: "admin-profile-preferences",
    component: () => import("@admin/pages/AdminProfilePreferencesPage.vue"),
    meta: { title: "偏好设置", originalPath: "/admin/profile/preferences" },
  },
  {
    path: "/users",
    name: "admin-users",
    component: () => import("@admin/pages/UsersAdminPage.vue"),
    meta: { title: "账号管理", originalPath: "/admin/users", resourceCode: "usr.manage" },
  },
  {
    path: "/roles",
    name: "admin-roles",
    component: () => import("@admin/pages/RolesAdminPage.vue"),
    meta: { title: "角色管理", originalPath: "/admin/roles", resourceCode: "rol.manage" },
  },
  {
    path: "/configs",
    name: "admin-configs",
    component: () => import("@admin/pages/ConfigsAdminPage.vue"),
    meta: { title: "系统配置", originalPath: "/admin/configs", resourceCode: "cfg.admin.view" },
  },
  {
    path: "/apis",
    name: "admin-apis",
    component: () => import("@admin/pages/ApisAdminPage.vue"),
    meta: { title: "接口管理", originalPath: "/admin/apis", resourceCode: "api.view" },
  },
  {
    path: "/method-stat",
    name: "admin-method-stat",
    component: () => import("@admin/pages/MethodStatPage.vue"),
    meta: {
      title: "方法统计",
      originalPath: "/admin/method-stat",
      resourceCode: "method-stat-center",
    },
  },
  {
    path: "/dicts",
    name: "admin-dicts",
    component: () => import("@admin/pages/DictAdminPage.vue"),
    meta: { title: "数据字典", originalPath: "/admin/dicts", resourceCode: "dict-manage-view" },
  },
  {
    path: "/login-logs",
    name: "admin-login-logs",
    component: () => import("@admin/pages/LoginLogsPage.vue"),
    meta: { title: "登录日志", originalPath: "/admin/login-logs", resourceCode: "login-log-view" },
  },
  {
    path: "/audit-logs",
    name: "admin-audit-logs",
    component: () => import("@admin/pages/AuditLogsPage.vue"),
    meta: { title: "审计日志", originalPath: "/admin/audit-logs", resourceCode: "audit-log-view" },
  },
  {
    path: "/web-users",
    name: "admin-web-users",
    component: () => import("@admin/pages/WebUsersAdminPage.vue"),
    meta: { title: "用户管理", originalPath: "/admin/web-users", resourceCode: "web-user-center" },
  },
  {
    path: "/web-user-stats",
    name: "admin-web-user-stats",
    component: () => import("@admin/pages/WebUserGroupsPage.vue"),
    meta: {
      title: "用户分组",
      originalPath: "/admin/web-user-stats",
      resourceCode: "web-user-stats-center",
    },
  },
  {
    path: "/normal-features",
    name: "admin-normal-features",
    component: () => import("@admin/pages/NormalFeatureAdminPage.vue"),
    meta: {
      title: "功能配置",
      originalPath: "/admin/normal-features",
      resourceCode: "normal-feature-center",
    },
  },
  {
    path: "/system-files",
    name: "admin-system-files",
    component: () => import("@admin/pages/SystemFilesAdminPage.vue"),
    meta: {
      title: "系统文件",
      originalPath: "/admin/system-files",
      resourceCode: "system-file-center",
    },
  },
  {
    path: "/diagnostic",
    name: "admin-diagnostic",
    component: () => import("@admin/pages/DiagnosticAdminPage.vue"),
    meta: {
      title: "诊断工具",
      originalPath: "/admin/diagnostic",
      resourceCode: "diagnostic-center",
    },
  },
  {
    path: "/help",
    name: "admin-help",
    component: () => import("@admin/pages/AdminHelpPage.vue"),
    meta: { title: "帮助中心", section: "帮助中心", originalPath: "/admin/help" },
  },
  {
    path: "/:pathMatch(.*)*",
    name: "admin-not-found",
    component: () => import("@admin/pages/AdminPagePlaceholder.vue"),
    meta: { title: "页面不存在", section: "提示" },
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: staticRoutes,
});

let dynamicRoutesLoaded = false;

export async function ensureAdminDynamicRoutes(): Promise<boolean> {
  if (dynamicRoutesLoaded) {
    return false;
  }
  await ensureAdminMenuResourcesLoaded();
  const routeNames = new Set(
    router
      .getRoutes()
      .map((route) => route.name)
      .filter((name): name is string => typeof name === "string"),
  );
  const routePaths = new Set(router.getRoutes().map((route) => route.path));

  useAdminMenuResources().value.forEach((resource) => {
    if (resource.openMode !== "PAGE" || !resource.url) {
      return;
    }
    const path = normalizeAdminMenuPath(resource.url);
    if (path === "/") {
      return;
    }
    if (routeNames.has(resource.id)) {
      return;
    }
    if (routePaths.has(path)) {
      return;
    }
    router.addRoute({
      path,
      name: resource.id,
      component: resolveAdminPageComponent(path),
      meta: {
        title: resource.name,
        section: "后台菜单",
        resourceCode: resource.code,
        originalPath: resource.url,
      },
    });
  });

  dynamicRoutesLoaded = true;
  return true;
}

router.beforeEach(async (to) => {
  await ensureAuthLoaded();

  if (to.name === "admin-login") {
    return isAuthenticated.value ? { path: "/", replace: true } : true;
  }

  if (!isAuthenticated.value) {
    return { name: "admin-login", replace: true, query: { redirect: to.fullPath } };
  }

  await ensureAdminPermissionsLoaded();

  const loaded = await ensureAdminDynamicRoutes();
  if (loaded) {
    return { ...to, replace: true };
  }
  return true;
});

export default router;
