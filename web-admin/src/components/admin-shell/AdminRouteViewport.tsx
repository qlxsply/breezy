"use client";

import { renderMappedAdminPage } from "@admin/components/admin-pages/page-map";
import {
  getCurrentRouteTitle,
  isPublicAdminSelfServicePath,
  useAdminBreadcrumb,
  useAdminRouteResolved,
} from "@admin/components/admin-shell/admin-routes";
import {
  useAuthSession,
  useCurrentUserType,
  useIsAuthenticated,
} from "@admin/features/auth/model/auth-store";
import { useResourceStatus } from "@admin/features/resources/model/resource-store";
import { BzButton, BzCard } from "@admin/shared/ui/bz";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";

import { AdminCapabilityDemo } from "./AdminCapabilityDemo";

function AdminPlaceholderCard({
  badge,
  title,
  description,
  extra,
}: {
  badge: string;
  title: string;
  description: string;
  extra?: React.ReactNode;
}) {
  return (
    <div className="admin-page-placeholder">
      <section className="admin-placeholder-card">
        <span className="admin-placeholder-meta">{badge}</span>
        <h1>{title}</h1>
        <p>{description}</p>
        {extra ? <div style={{ marginTop: 16 }}>{extra}</div> : null}
      </section>
    </div>
  );
}

export function AdminRouteViewport({
  path,
  active = true,
}: {
  path?: string;
  active?: boolean;
} = {}) {
  const routePathname = usePathname() ?? "/admin";
  const pathname = path ?? routePathname;
  const router = useRouter();
  const authSession = useAuthSession();
  const authLoaded = authSession.status !== "idle" && authSession.status !== "loading";
  const authenticated = useIsAuthenticated();
  const currentUserType = useCurrentUserType();
  const resourceStatus = useResourceStatus();
  const selfServiceRoute = isPublicAdminSelfServicePath(pathname);
  const breadcrumb = useAdminBreadcrumb(pathname);
  const resolved = useAdminRouteResolved(pathname);

  useEffect(() => {
    if (!active) {
      return;
    }
    if (!authLoaded) {
      return;
    }
    if (authSession.status === "error") {
      return;
    }
    if (!authenticated) {
      const redirect = encodeURIComponent(pathname);
      router.replace(`/admin/login?redirect=${redirect}`);
      return;
    }
    if (currentUserType !== "ADMIN") {
      router.replace("/");
    }
  }, [active, authLoaded, authSession.status, authenticated, currentUserType, pathname, router]);

  if (!authLoaded) {
    return (
      <AdminPlaceholderCard
        badge="系统 / 加载中"
        title="正在初始化后台"
        description="后台登录态、资源树、权限和通知能力正在加载，请稍候。"
      />
    );
  }

  if (authSession.status === "error") {
    return (
      <AdminPlaceholderCard
        badge="系统 / 初始化失败"
        title="后台会话恢复失败"
        description={authSession.error || "无法连接后台服务，请刷新页面后重试。"}
      />
    );
  }

  if (!authenticated) {
    return (
      <AdminPlaceholderCard
        badge="系统 / 跳转中"
        title="正在跳转到登录页"
        description="当前未登录，系统将自动跳转到后台登录页。"
      />
    );
  }

  if (currentUserType !== "ADMIN") {
    return (
      <AdminPlaceholderCard
        badge="系统 / 无权访问"
        title="当前账号不能访问后台"
        description="该账号不是内部账号，系统将返回首页。"
      />
    );
  }

  if (resourceStatus === "error" && !selfServiceRoute) {
    return (
      <AdminPlaceholderCard
        badge="系统 / 初始化失败"
        title="后台资源加载失败"
        description="无法验证当前页面权限，请刷新页面后重试。"
      />
    );
  }

  if (resourceStatus !== "ready" && !selfServiceRoute) {
    return (
      <AdminPlaceholderCard
        badge="系统 / 同步中"
        title="正在同步后台资源"
        description="后台资源树和权限数据正在准备，完成后会自动进入目标页面。"
      />
    );
  }

  if (!resolved.exists) {
    return (
      <AdminPlaceholderCard
        badge="提示 / 页面不存在"
        title="页面不存在"
        description="当前路径没有对应的后台资源或静态自助页定义。"
      />
    );
  }

  if (!resolved.accessible) {
    return (
      <AdminPlaceholderCard
        badge="提示 / 无权限访问"
        title="无权限访问当前页面"
        description="当前账号没有访问该后台页面的资源权限。"
        extra={
          <BzButton
            buttonType="primary"
            onClick={() => router.replace("/admin")}
          >
            返回工作台
          </BzButton>
        }
      />
    );
  }

  const title = getCurrentRouteTitle(pathname);
  const badge = breadcrumb.map((item) => item.label).join(" / ");
  const mappedPage = renderMappedAdminPage(pathname);

  if (mappedPage) {
    return <>{mappedPage}</>;
  }

  if (pathname === "/admin") {
    return (
      <div className="admin-page-placeholder">
        <section className="admin-placeholder-card">
          <span className="admin-placeholder-meta">{badge || "概览 / 工作台"}</span>
          <h1>{title}</h1>
          <p>
            后台壳体、Bz UI、基础能力和 registry 已经切换到 React
            方案。后续页面主体会继续按批次逐页接管。
          </p>
        </section>
        <AdminCapabilityDemo />
      </div>
    );
  }

  return (
    <AdminPlaceholderCard
      badge={badge}
      title={title}
      description="当前页面的路由、菜单、权限、面包屑已经由新 registry 驱动；页面主体内容仍待后续逐页迁移。"
      extra={
        <BzCard shadow="never">
          <div style={{ display: "grid", gap: 8, color: "#475569", fontSize: 13 }}>
            <div>当前路径：{pathname}</div>
            <div>
              返回工作台：
              <Link
                href="/admin"
                style={{ color: "#2563eb" }}
              >
                /admin
              </Link>
            </div>
          </div>
        </BzCard>
      }
    />
  );
}
