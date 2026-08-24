"use client";

import {
  findMenuResourceByPath,
  hasMenuAccess,
  useResourceState,
} from "@admin/features/resources/model/resource-store";
import { AdminRouteFeedback } from "@admin/shared/ui/admin/AdminRouteFeedback";
import { BzButton } from "@admin/shared/ui/bz";
import { usePathname, useRouter } from "next/navigation";

import { getAdminSelfServiceRoute } from "./admin-routes";

export function AdminResourceBoundary({ children }: { children: React.ReactNode }) {
  const pathname = usePathname() ?? "/admin";
  const router = useRouter();
  const resourceState = useResourceState();

  if (getAdminSelfServiceRoute(pathname)) return children;

  if (resourceState.status === "error") {
    return (
      <AdminRouteFeedback
        badge="系统 / 初始化失败"
        title="后台资源加载失败"
        description={resourceState.error || "无法验证当前页面权限，请刷新页面后重试。"}
      />
    );
  }

  if (resourceState.status !== "ready") {
    return (
      <AdminRouteFeedback
        badge="系统 / 同步中"
        title="正在同步后台资源"
        description="后台菜单和权限数据正在准备，完成后会自动进入目标页面。"
      />
    );
  }

  const resource = findMenuResourceByPath(resourceState, pathname);
  if (!resource || !hasMenuAccess(resourceState, resource.id)) {
    return (
      <AdminRouteFeedback
        badge="提示 / 无权限访问"
        title="无权限访问当前页面"
        description="当前账号没有访问该后台页面的菜单权限。"
        action={
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

  return children;
}
