"use client";

import {
  useAuthSession,
  useCurrentUserType,
  useIsAuthenticated,
} from "@admin/features/auth/model/auth-store";
import { PASSWORD_CHANGE_PATH } from "@admin/features/auth/public/session";
import { AdminRouteFeedback } from "@admin/shared/ui/admin/AdminRouteFeedback";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";

export function AdminAccessBoundary({ children }: { children: React.ReactNode }) {
  const pathname = usePathname() ?? "/admin";
  const router = useRouter();
  const authSession = useAuthSession();
  const authenticated = useIsAuthenticated();
  const currentUserType = useCurrentUserType();
  const mustChangePassword = authSession.user?.mustChangePassword === true;
  const onPasswordPage =
    pathname === PASSWORD_CHANGE_PATH || pathname === `${PASSWORD_CHANGE_PATH}/`;
  const authLoaded = authSession.status !== "idle" && authSession.status !== "loading";

  useEffect(() => {
    if (!authLoaded || authSession.status === "error") return;
    if (!authenticated) {
      router.replace(`/admin/login?redirect=${encodeURIComponent(pathname)}`);
      return;
    }
    if (currentUserType !== "ADMIN") {
      router.replace("/admin/login");
      return;
    }
    if (mustChangePassword && !onPasswordPage) router.replace(PASSWORD_CHANGE_PATH);
  }, [
    authLoaded,
    authSession.status,
    authenticated,
    currentUserType,
    mustChangePassword,
    onPasswordPage,
    pathname,
    router,
  ]);

  if (!authLoaded) {
    return (
      <AdminRouteFeedback
        badge="系统 / 加载中"
        title="正在初始化后台"
        description="后台登录态正在恢复，请稍候。"
      />
    );
  }

  if (authSession.status === "error") {
    return (
      <AdminRouteFeedback
        badge="系统 / 初始化失败"
        title="后台会话恢复失败"
        description={authSession.error || "无法连接后台服务，请刷新页面后重试。"}
      />
    );
  }

  if (!authenticated || currentUserType !== "ADMIN") {
    return (
      <AdminRouteFeedback
        badge="系统 / 跳转中"
        title="正在跳转到后台登录页"
        description="当前会话无权访问管理后台。"
      />
    );
  }

  if (mustChangePassword && !onPasswordPage) {
    return (
      <AdminRouteFeedback
        badge="安全 / 必须修改密码"
        title="正在跳转到修改密码页"
        description="当前密码需要更新，完成修改后才能继续使用管理后台。"
      />
    );
  }

  return children;
}
