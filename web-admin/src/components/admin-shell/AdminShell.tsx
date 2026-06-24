"use client";

import brandLogo from "@admin/assets/brand-logo.png";
import { formatDateTime } from "@admin/core/formatter";
import { logout, useAuthUser } from "@admin/core/registry/auth-registry";
import {
  markAllRead,
  markRead,
  useUnreadCount,
  useUnreadList,
} from "@admin/core/registry/notifications-registry";
import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import { type AdminMenuNode, useAdminBreadcrumb, useAdminMenuTree } from "./admin-routes";

export function AdminShell({ children }: { children: React.ReactNode }) {
  const pathnameValue = usePathname();
  const router = useRouter();
  const authUser = useAuthUser();
  const unreadCount = useUnreadCount();
  const unreadList = useUnreadList();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [userOpen, setUserOpen] = useState(false);
  const [expandedIds, setExpandedIds] = useState<string[]>([]);
  const [openTabs, setOpenTabs] = useState<string[]>([]);
  const [blankMode, setBlankMode] = useState(false);

  const pathname = pathnameValue ?? "/admin";

  useEffect(() => {
    setNotificationOpen(false);
    setUserOpen(false);
  }, [pathname]);

  const menuTree = useAdminMenuTree();
  const breadcrumbItems = useAdminBreadcrumb(pathname);
  const previewList = useMemo(() => unreadList.slice(0, 4), [unreadList]);
  const menuIndex = useMemo(() => buildMenuIndex(menuTree), [menuTree]);
  const currentMenuEntry = menuIndex.byPath.get(pathname);
  const activeTabs = useMemo(
    () => openTabs.map((href) => menuIndex.byPath.get(href)).filter((item): item is MenuPathEntry => Boolean(item)),
    [menuIndex.byPath, openTabs],
  );
  const displayBlankWorkspace = pathname === "/admin" && blankMode;
  const displayUnreadCount = unreadCount > 0;
  const userName = (authUser?.account || authUser?.username || "Admin").trim() || "Admin";
  const userAccount = authUser?.account?.trim() || "账号后台";
  const avatarText = userName.slice(0, 1).toUpperCase() || "A";

  useEffect(() => {
    if (blankMode || !currentMenuEntry?.node.path) {
      return;
    }
    setOpenTabs((current) =>
      current.includes(currentMenuEntry.node.path as string)
        ? current
        : [...current, currentMenuEntry.node.path as string],
    );
  }, [currentMenuEntry, blankMode]);

  useEffect(() => {
    if (pathname !== "/admin" && blankMode) {
      setBlankMode(false);
    }
  }, [pathname, blankMode]);

  useEffect(() => {
    if (!currentMenuEntry) {
      return;
    }
    setExpandedIds((current) => Array.from(new Set([...current, ...currentMenuEntry.ancestorIds])));
  }, [currentMenuEntry]);

  function toggleExpanded(nodeId: string) {
    setExpandedIds((current) =>
      current.includes(nodeId) ? current.filter((item) => item !== nodeId) : [...current, nodeId],
    );
  }

  function closeTab(href: string) {
    setOpenTabs((current) => {
      const index = current.indexOf(href);
      if (index < 0) return current;
      const nextTabs = current.filter((item) => item !== href);
      if (pathname === href) {
        const nextHref = nextTabs[index] ?? nextTabs[index - 1];
        queueMicrotask(() => {
          if (nextHref) {
            router.push(nextHref);
          } else {
            setBlankMode(true);
            router.push("/admin");
          }
        });
      }
      return nextTabs;
    });
  }

  return (
    <div className={`admin-shell${sidebarCollapsed ? " collapsed" : ""}`}>
      <aside className="admin-sidebar">
        <div className="brand">
          <button
            className="brand-link"
            type="button"
            aria-label="Breezy Admin"
            onClick={() => router.push("/admin")}
          >
            <span className="brand-logo">
              <Image
                src={brandLogo}
                alt="Breezy Admin"
                priority
              />
            </span>
            {!sidebarCollapsed ? <span className="brand-title">Breezy Admin</span> : null}
          </button>
        </div>

        <nav
          className="admin-nav"
          aria-label="后台导航"
        >
          {menuTree.map((node) => (
            <AdminNavItem
              key={node.id}
              node={node}
              pathname={pathname}
              collapsed={sidebarCollapsed}
              expandedIds={expandedIds}
              onToggle={toggleExpanded}
            />
          ))}
        </nav>
      </aside>

      <section className="admin-main">
        <div className="admin-fixed-header">
          <header className="topbar">
            <div className="topbar-left">
              <button
                className="topbar-icon"
                type="button"
                title={sidebarCollapsed ? "展开导航" : "收起导航"}
                onClick={() => setSidebarCollapsed((value) => !value)}
              >
                <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    d="M4 7h16M4 12h16M4 17h16"
                    fill="none"
                    stroke="currentColor"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="1.8"
                  />
                </svg>
              </button>

              <nav
                className="breadcrumb"
                aria-label="面包屑导航"
              >
                <ol>
                  {breadcrumbItems.map((item, index) => (
                    <li
                      className="breadcrumb-entry"
                      key={`${item.label}-${index}`}
                    >
                      {index > 0 ? (
                        <span
                          className="breadcrumb-separator"
                          aria-hidden="true"
                        >
                          <svg viewBox="0 0 24 24">
                            <path
                              d="m9 18 6-6-6-6"
                              fill="none"
                              stroke="currentColor"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth="1.8"
                            />
                          </svg>
                        </span>
                      ) : null}
                      {item.href ? (
                        <Link
                          className="breadcrumb-link"
                          href={item.href}
                        >
                          {item.label}
                        </Link>
                      ) : (
                        <span
                          className={
                            index === breadcrumbItems.length - 1
                              ? "breadcrumb-current"
                              : "breadcrumb-link"
                          }
                        >
                          {item.label}
                        </span>
                      )}
                    </li>
                  ))}
                </ol>
              </nav>
            </div>

            <div className="topbar-right">
              <button
                className="search-trigger"
                type="button"
                title="搜索"
              >
                <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    d="m21 21-4.34-4.34M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16"
                    fill="none"
                    stroke="currentColor"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="1.8"
                  />
                </svg>
                <span>搜索</span>
                <kbd>Ctrl K</kbd>
              </button>

              <Link
                className="topbar-icon round"
                href="/admin/profile/preferences"
                title="设置"
              >
                <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    d="M9.67 4.14a2.34 2.34 0 0 1 4.66 0 2.34 2.34 0 0 0 3.32 1.91 2.34 2.34 0 0 1 2.33 4.03 2.34 2.34 0 0 0 0 3.84 2.34 2.34 0 0 1-2.33 4.03 2.34 2.34 0 0 0-3.32 1.91 2.34 2.34 0 0 1-4.66 0 2.34 2.34 0 0 0-3.32-1.91 2.34 2.34 0 0 1-2.33-4.03 2.34 2.34 0 0 0 0-3.84 2.34 2.34 0 0 1 2.33-4.03 2.34 2.34 0 0 0 3.32-1.91"
                    fill="none"
                    stroke="currentColor"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="1.6"
                  />
                  <circle
                    cx="12"
                    cy="12"
                    r="3"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.6"
                  />
                </svg>
              </Link>

              <div className="topbar-popover-host">
                <button
                  className="topbar-icon round notify-button"
                  type="button"
                  title="通知"
                  onClick={() => setNotificationOpen((value) => !value)}
                >
                  <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                  >
                    <path
                      d="M10.27 21a2 2 0 0 0 3.46 0M4 17h16a1 1 0 0 0 .74-1.67C19.41 13.96 18 12.5 18 8A6 6 0 0 0 6 8c0 4.5-1.41 5.96-2.74 7.33A1 1 0 0 0 4 17"
                      fill="none"
                      stroke="currentColor"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="1.8"
                    />
                  </svg>
                  {displayUnreadCount ? <span className="notify-dot" /> : null}
                </button>
                {notificationOpen ? (
                  <section className="notification-panel">
                    <div className="panel-header">
                      <strong>通知</strong>
                    </div>
                    <button
                      className="mark-read"
                      type="button"
                      onClick={() => void markAllRead()}
                    >
                      全部标记为已读
                    </button>
                    <div className="notification-list">
                      {previewList.length === 0 ? <div className="notification-empty">暂无未读消息</div> : null}
                      {previewList.map((item) => (
                        <button
                          key={item.id}
                          className="notification-item"
                          type="button"
                          onClick={() => void markRead(item.id)}
                        >
                          <span className="notification-avatar">{item.title.slice(0, 1).toUpperCase()}</span>
                          <span className="notification-body">
                            <strong>{item.title}</strong>
                            <span>{item.content || "暂无摘要内容"}</span>
                            <small>{formatDateTime(item.createdAt || "")}</small>
                          </span>
                          {!item.read ? <span className="notification-unread" /> : null}
                        </button>
                      ))}
                    </div>
                    <footer className="panel-footer">
                      <button
                        type="button"
                        onClick={() => void markAllRead()}
                      >
                        清空
                      </button>
                      <button
                        className="primary"
                        type="button"
                        onClick={() => setNotificationOpen(false)}
                      >
                        查看全部
                      </button>
                    </footer>
                  </section>
                ) : null}
              </div>

              <div className="topbar-popover-host">
                <button
                  className="avatar-button"
                  type="button"
                  title="用户菜单"
                  onClick={() => setUserOpen((value) => !value)}
                >
                  <span className="avatar-face">{avatarText}</span>
                  <span className="avatar-status" />
                </button>
                {userOpen ? (
                  <section className="user-panel">
                    <div className="user-card">
                      <span className="avatar-large">{avatarText}</span>
                      <div className="user-meta">
                        <strong>{userName}</strong>
                        <span>{userAccount}</span>
                      </div>
                    </div>
                    <div className="user-menu">
                      <Link href="/admin/profile">个人中心</Link>
                      <Link href="/admin/profile/password">修改密码</Link>
                      <Link href="/admin/profile/preferences">偏好设置</Link>
                      <Link href="/admin/help">问题与帮助</Link>
                      <button
                        type="button"
                        onClick={() => void logout()}
                      >
                        退出登录
                      </button>
                    </div>
                  </section>
                ) : null}
              </div>
            </div>
          </header>

          <div className="tabs-row">
            <div className="tabs-scroll">
              <div className="tabs-list">
                {activeTabs.map((tab) => {
                  const active = pathname === tab.node.path;
                  return (
                    <div
                      key={tab.node.path}
                      className={`tab-item${active ? " active" : ""}`}
                    >
                      <Link
                        href={tab.node.path || "/admin"}
                        className="tab-link"
                      >
                        <span className="tab-icon" aria-hidden="true">
                          <img
                            src={tab.node.iconUrl}
                            alt=""
                          />
                        </span>
                        <span className="tab-title-text">{tab.node.title}</span>
                      </Link>
                      <button
                        className="tab-close"
                        type="button"
                        title={`关闭 ${tab.node.title}`}
                        onClick={(event) => {
                          event.preventDefault();
                          event.stopPropagation();
                          closeTab(tab.node.path || "/admin");
                        }}
                      >
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                          <path
                            d="M7 7l10 10M17 7 7 17"
                            fill="none"
                            stroke="currentColor"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="1.8"
                          />
                        </svg>
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>
            <div className="tabs-tools">
              <button
                className="tabbar-tool"
                type="button"
                title="刷新当前页面"
                onClick={() => router.refresh()}
              >
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.92 1 6.73 2.74L21 8" />
                  <path d="M21 3v5h-5" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <main className="admin-content">
          <div className="admin-workspace">
            {displayBlankWorkspace ? <div className="admin-workspace-blank" /> : children}
          </div>
        </main>
      </section>
    </div>
  );
}

interface MenuPathEntry {
  node: AdminMenuNode;
  ancestorIds: string[];
}

function buildMenuIndex(nodes: AdminMenuNode[]): { byPath: Map<string, MenuPathEntry> } {
  const byPath = new Map<string, MenuPathEntry>();

  const visit = (node: AdminMenuNode, ancestorIds: string[]) => {
    if (node.nodeType === "MENU" && node.path) {
      byPath.set(node.path, { node, ancestorIds });
    }
    node.children.forEach((child) => visit(child, [...ancestorIds, node.id]));
  };

  nodes.forEach((node) => visit(node, []));
  return { byPath };
}

function AdminNavItem({
  node,
  pathname,
  collapsed,
  expandedIds,
  onToggle,
  depth = 0,
}: {
  node: AdminMenuNode;
  pathname: string;
  collapsed: boolean;
  expandedIds: string[];
  onToggle: (nodeId: string) => void;
  depth?: number;
}) {
  const hasChildren = node.children.length > 0;
  const expanded = hasChildren && expandedIds.includes(node.id) && !collapsed;
  const active = Boolean(node.path) && pathname === node.path;
  const hasActiveDescendant = hasChildren && containsPath(node.children, pathname);

  if (node.nodeType === "DIRECTORY") {
    return (
      <div className={`nav-node nav-level-directory`}>
        <button
          className={`nav-item nav-directory${expanded ? " expanded" : ""}${hasActiveDescendant && !expanded ? " active-ancestor" : ""}`}
          type="button"
          title={node.title}
          style={!collapsed ? { paddingLeft: `${12 + depth * 14}px` } : undefined}
          onClick={() => onToggle(node.id)}
        >
          <span className="nav-item-main">
            <span className="nav-icon" aria-hidden="true">
              <img src={node.iconUrl} alt="" />
            </span>
            {!collapsed ? <span className="nav-label">{node.title}</span> : null}
          </span>
          {!collapsed ? (
            <span className="nav-caret" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path d="m9 6 6 6-6 6" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
              </svg>
            </span>
          ) : null}
        </button>
        {expanded ? (
          <div className="nav-children">
            {node.children.map((child) => (
              <AdminNavItem key={child.id} node={child} pathname={pathname} collapsed={collapsed} expandedIds={expandedIds} onToggle={onToggle} depth={depth + 1} />
            ))}
          </div>
        ) : null}
      </div>
    );
  }

  const activeDescendantExpanded = hasChildren && hasActiveDescendant && expandedIds.includes(node.id);

  return (
    <div className={`nav-node nav-level-menu`}>
      <div className={`nav-item nav-menu${active ? " active" : ""}`} style={!collapsed ? { paddingLeft: `${12 + depth * 14}px` } : undefined}>
        {node.path ? (
          <Link href={node.path} className="nav-item-main" title={node.title}>
            <span className="nav-icon" aria-hidden="true">
              <img src={node.iconUrl} alt="" />
            </span>
            {!collapsed ? <span className="nav-label">{node.title}</span> : null}
          </Link>
        ) : (
          <span className="nav-item-main nav-linkless">
            <span className="nav-icon" aria-hidden="true">
              <img src={node.iconUrl} alt="" />
            </span>
            {!collapsed ? <span className="nav-label">{node.title}</span> : null}
          </span>
        )}
      </div>
      {activeDescendantExpanded ? (
        <div className="nav-children">
          {node.children.map((child) => (
            <AdminNavItem key={child.id} node={child} pathname={pathname} collapsed={collapsed} expandedIds={expandedIds} onToggle={onToggle} depth={depth + 1} />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function containsPath(nodes: AdminMenuNode[], pathname: string): boolean {
  return nodes.some((node) => node.path === pathname || containsPath(node.children, pathname));
}
