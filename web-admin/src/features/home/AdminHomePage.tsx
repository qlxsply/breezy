"use client";

import { useAuthUser } from "@admin/features/auth/public/session";
import { useResources } from "@admin/features/resources/permissions";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { BzCard } from "@admin/shared/ui/bz";

import styles from "./AdminHomePage.module.css";

export function AdminHomePage() {
  const user = useAuthUser();
  const resources = useResources();
  const menuCount = resources.filter((resource) => resource.nodeType === "MENU").length;
  const directoryCount = resources.filter((resource) => resource.nodeType === "DIRECTORY").length;

  return (
    <div className={layoutStyles.page}>
      <div className={layoutStyles.content}>
        <div className={`${layoutStyles.pageStack} ${styles.page}`}>
          <section className={styles.hero}>
            <div className={styles.eyebrow}>BREEZY ADMIN</div>
            <h1>欢迎回来，{user?.account || "管理员"}</h1>
            <p>工作台已连接当前账号的资源与权限视图，可从左侧菜单进入具体管理功能。</p>
          </section>
          <div className={styles.metrics}>
            <BzCard
              className={styles.metric}
              shadow="never"
            >
              <span>可见菜单</span>
              <strong>{menuCount}</strong>
            </BzCard>
            <BzCard
              className={styles.metric}
              shadow="never"
            >
              <span>功能分区</span>
              <strong>{directoryCount}</strong>
            </BzCard>
            <BzCard
              className={styles.metric}
              shadow="never"
            >
              <span>会话类型</span>
              <strong>{user?.userType || "-"}</strong>
            </BzCard>
          </div>
        </div>
      </div>
    </div>
  );
}
