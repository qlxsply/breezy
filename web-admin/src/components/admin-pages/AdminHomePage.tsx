"use client";

import { useCurrentUserType } from "@admin/core/registry/auth-registry";
import { hasMenuAccess } from "@admin/core/registry/permissions-registry";
import {
  getResourceMap,
  getResources,
  useResources,
} from "@admin/core/registry/resources-registry";
import { useRouter } from "next/navigation";
import { useMemo } from "react";

interface QuickMenuItem {
  id: string;
  name: string;
  url: string;
  orderNo: number;
  parentName: string;
}

const USER_TYPE_LABELS: Record<string, string> = {
  INTERNAL: "账号",
  SYSTEM: "系统账号",
  EXTERNAL: "用户",
  GUEST: "游客",
};

const metrics = [
  { label: "用户量", value: "2,000", caption: "总用户量", total: "120,000", icon: "▤" },
  { label: "访问量", value: "20,000", caption: "总访问量", total: "500,000", icon: "◔" },
  { label: "下载量", value: "8,000", caption: "总下载量", total: "120,000", icon: "↓" },
  { label: "使用量", value: "5,000", caption: "总使用量", total: "50,000", icon: "◴" },
];

export function AdminHomePage() {
  const router = useRouter();
  const currentUserType = useCurrentUserType();
  const resources = useResources();

  const quickMenus = useMemo<QuickMenuItem[]>(() => {
    const resourceMap = getResourceMap();
    return getResources()
      .filter((resource) => resource.type === "MENU")
      .filter((resource) => resource.scope === "SETTING")
      .filter((resource) => resource.openMode === "PAGE")
      .filter((resource) => Boolean(resource.url))
      .filter((resource) => hasMenuAccess(resource, resourceMap))
      .map((resource) => ({
        id: resource.id,
        name: resource.name,
        url: resource.url,
        orderNo: resource.orderNo || 0,
        parentName: resource.parentId ? resourceMap.get(resource.parentId)?.name || "" : "",
      }))
      .sort((left, right) => {
        if (left.orderNo !== right.orderNo) return left.orderNo - right.orderNo;
        return left.name.localeCompare(right.name);
      });
  }, [resources]);

  const userTypeLabel = USER_TYPE_LABELS[currentUserType] || "账号";

  return (
    <div className="admin-home">
      <section className="metric-grid">
        {metrics.map((item) => (
          <article
            key={item.label}
            className="metric-card"
          >
            <div>
              <h2>{item.label}</h2>
              <strong>{item.value}</strong>
            </div>
            <span className="metric-icon">{item.icon}</span>
            <footer>
              <span>{item.caption}</span>
              <b>{item.total}</b>
            </footer>
          </article>
        ))}
      </section>

      <section className="chart-panel">
        <header className="panel-header">
          <div className="segmented">
            <button
              className="active"
              type="button"
            >
              流量趋势
            </button>
            <button type="button">月访问量</button>
          </div>
        </header>
        <div className="chart">
          <div className="chart-grid" />
          <svg
            viewBox="0 0 1200 260"
            aria-hidden="true"
          >
            <path
              className="area area-blue"
              d="M0 250 C80 245 120 210 170 175 C230 128 260 65 330 58 C395 50 430 120 480 165 C540 220 610 230 670 190 C735 145 750 78 820 58 C900 36 930 144 1000 175 C1070 205 1130 225 1200 242 L1200 260 L0 260 Z"
            />
            <path
              className="line line-blue"
              d="M0 250 C80 245 120 210 170 175 C230 128 260 65 330 58 C395 50 430 120 480 165 C540 220 610 230 670 190 C735 145 750 78 820 58 C900 36 930 144 1000 175 C1070 205 1130 225 1200 242"
            />
            <path
              className="area area-green"
              d="M0 252 C160 250 250 250 330 228 C390 210 430 142 480 218 C560 250 665 244 720 205 C790 155 865 158 930 210 C1000 248 1120 252 1200 252 L1200 260 L0 260 Z"
            />
            <path
              className="line line-green"
              d="M0 252 C160 250 250 250 330 228 C390 210 430 142 480 218 C560 250 665 244 720 205 C790 155 865 158 930 210 C1000 248 1120 252 1200 252"
            />
          </svg>
        </div>
      </section>

      <section className="panel-grid">
        <article className="admin-panel">
          <header className="panel-header">
            <h2>常用入口</h2>
          </header>
          <div className="quick-grid">
            {quickMenus.map((menu) => (
              <button
                key={menu.id}
                className="quick-card"
                type="button"
                onClick={() => router.push(menu.url)}
              >
                <span>{menu.parentName || "系统管理"}</span>
                <strong>{menu.name}</strong>
              </button>
            ))}
          </div>
        </article>

        <article className="admin-panel">
          <header className="panel-header">
            <h2>后台信息</h2>
          </header>
          <div className="info-list">
            <div>
              <span>当前角色</span>
              <strong>{userTypeLabel}</strong>
            </div>
            <div>
              <span>可访问菜单</span>
              <strong>{quickMenus.length}</strong>
            </div>
            <div>
              <span>后台路径</span>
              <strong>/admin</strong>
            </div>
          </div>
        </article>
      </section>

      {quickMenus.length > 6 ? (
        <section className="admin-panel">
          <header className="panel-header">
            <h2>更多入口</h2>
          </header>
          <div className="quick-grid compact">
            {quickMenus.slice(6).map((menu) => (
              <button
                key={menu.id}
                className="quick-card"
                type="button"
                onClick={() => router.push(menu.url)}
              >
                <span>{menu.parentName || "系统管理"}</span>
                <strong>{menu.name}</strong>
              </button>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}
