"use client";

import brandLogo from "@admin/assets/brand-logo.png";
import { useAuthUser, usePersonalizedConfigs } from "@admin/features/auth/model/auth-store";
import {
  useUnreadCount,
  useUnreadList,
} from "@admin/features/notifications/model/notification-store";
import { markAllRead, markRead } from "@admin/features/notifications/service/notification-service";
import { resolveResourceIconUrl } from "@admin/features/resources/model/resource-icon";
import { logoutSession } from "@admin/runtime/session/session-service";
import { message } from "@admin/shared/lib/feedback/message";
import { formatDateTime } from "@admin/shared/lib/formatter";
import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Activity,
  type MouseEvent as ReactMouseEvent,
  type PointerEvent as ReactPointerEvent,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { createPortal } from "react-dom";

import {
  type AdminMenuNode,
  getAdminSelfServiceRoute,
  useAdminBreadcrumb,
  useAdminMenuTree,
} from "./admin-routes";
import styles from "./AdminShell.module.css";

const MAX_OPEN_TABS = 20;

export function AdminShell({ children }: { children: React.ReactNode }) {
  const pathnameValue = usePathname();
  const router = useRouter();
  const authUser = useAuthUser();
  const personalizedConfigs = usePersonalizedConfigs();
  const unreadCount = useUnreadCount();
  const unreadList = useUnreadList();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [userOpen, setUserOpen] = useState(false);
  const [expandedIds, setExpandedIds] = useState<string[]>([]);
  const [openTabs, setOpenTabs] = useState<string[]>([]);
  const [tabReloadVersions, setTabReloadVersions] = useState<Record<string, number>>({});
  const [tabScrollState, setTabScrollState] = useState({
    overflow: false,
    canScrollLeft: false,
    canScrollRight: false,
  });
  const [tabMenu, setTabMenu] = useState<TabMenuState | null>(null);
  const [draggingPath, setDraggingPath] = useState<string | null>(null);
  const [dragTarget, setDragTarget] = useState<TabDragTarget | null>(null);
  const [blankMode, setBlankMode] = useState(false);
  const tabsScrollRef = useRef<HTMLDivElement>(null);
  const userPanelHostRef = useRef<HTMLDivElement>(null);
  const tabElementsRef = useRef(new Map<string, HTMLDivElement>());
  const dragSessionRef = useRef<TabDragSession | null>(null);
  const dragTargetRef = useRef<TabDragTarget | null>(null);
  const suppressTabClickRef = useRef(false);
  const lastAcceptedPathRef = useRef(pathnameValue ?? "/admin");
  const emptyWorkspacePendingRef = useRef(false);
  const pageCacheRef = useRef(new Map<string, React.ReactNode>());

  const pathname = pathnameValue ?? "/admin";
  pageCacheRef.current.set(pathname, children);

  useEffect(() => {
    setNotificationOpen(false);
    setUserOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!userOpen) return;
    function closeUserPanelOnOutsidePointer(event: PointerEvent) {
      const target = event.target;
      if (target instanceof Node && !userPanelHostRef.current?.contains(target)) {
        setUserOpen(false);
      }
    }
    document.addEventListener("pointerdown", closeUserPanelOnOutsidePointer, true);
    return () => document.removeEventListener("pointerdown", closeUserPanelOnOutsidePointer, true);
  }, [userOpen]);

  const menuTree = useAdminMenuTree();
  const breadcrumbItems = useAdminBreadcrumb(pathname);
  const previewList = useMemo(() => unreadList.slice(0, 4), [unreadList]);
  const menuIndex = useMemo(() => buildMenuIndex(menuTree), [menuTree]);
  const currentMenuEntry = menuIndex.byPath.get(pathname);
  const currentTabEntry = useMemo(
    () => resolveTabEntry(pathname, menuIndex.byPath),
    [menuIndex.byPath, pathname],
  );
  const activeTabs = useMemo(
    () =>
      openTabs
        .map((href) => resolveTabEntry(href, menuIndex.byPath))
        .filter((item): item is TabEntry => Boolean(item)),
    [menuIndex.byPath, openTabs],
  );
  const workspaceTabs = useMemo(
    () =>
      currentTabEntry && !activeTabs.some((tab) => tab.path === currentTabEntry.path)
        ? [...activeTabs, currentTabEntry]
        : activeTabs,
    [activeTabs, currentTabEntry],
  );
  const displayBlankWorkspace = pathname === "/admin" && blankMode;
  const currentTabReloadVersion = tabReloadVersions[pathname] ?? 0;
  const tabKeepAlive =
    personalizedConfigs.find((item) => item.code === "USER_ADMIN_TAB_KEEP_ALIVE")?.value !==
    "false";
  const displayUnreadCount = unreadCount > 0;
  const userName = authUser?.account.trim() || "Admin";
  const userAccount = authUser?.account?.trim() || "账号后台";
  const avatarText = userName.slice(0, 1).toUpperCase() || "A";

  useEffect(() => {
    if (blankMode || !currentTabEntry) {
      return;
    }
    setOpenTabs((current) => {
      if (current.includes(currentTabEntry.path)) {
        lastAcceptedPathRef.current = currentTabEntry.path;
        return current;
      }
      if (current.length >= MAX_OPEN_TABS) {
        queueMicrotask(() => {
          message.warning(`最多同时打开 ${MAX_OPEN_TABS} 个标签页，请先关闭不需要的标签`);
          router.replace(lastAcceptedPathRef.current);
        });
        return current;
      }
      lastAcceptedPathRef.current = currentTabEntry.path;
      return [...current, currentTabEntry.path];
    });
  }, [currentTabEntry, blankMode, router]);

  useEffect(() => {
    if (pathname === "/admin") {
      emptyWorkspacePendingRef.current = false;
    } else if (blankMode && !emptyWorkspacePendingRef.current) {
      setBlankMode(false);
    }
  }, [pathname, blankMode]);

  useEffect(() => {
    if (!currentMenuEntry) {
      return;
    }
    setExpandedIds((current) => Array.from(new Set([...current, ...currentMenuEntry.ancestorIds])));
  }, [currentMenuEntry]);

  useEffect(() => {
    const scrollElement = tabsScrollRef.current;
    if (!scrollElement) return;
    const observer = new ResizeObserver(updateTabScrollState);
    observer.observe(scrollElement);
    const listElement = scrollElement.firstElementChild;
    if (listElement) observer.observe(listElement);
    const frame = window.requestAnimationFrame(updateTabScrollState);
    return () => {
      observer.disconnect();
      window.cancelAnimationFrame(frame);
    };
  }, [activeTabs]);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      tabElementsRef.current.get(pathname)?.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
        inline: "nearest",
      });
      updateTabScrollState();
    });
    return () => window.cancelAnimationFrame(frame);
  }, [activeTabs, pathname]);

  useEffect(() => {
    if (!tabMenu) return;
    function closeMenu(event: globalThis.PointerEvent) {
      const target = event.target;
      if (target instanceof Element && target.closest(`.${styles["tabs-context-menu"]}`)) return;
      setTabMenu(null);
    }
    function closeMenuOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setTabMenu(null);
    }
    document.addEventListener("pointerdown", closeMenu);
    document.addEventListener("keydown", closeMenuOnEscape);
    return () => {
      document.removeEventListener("pointerdown", closeMenu);
      document.removeEventListener("keydown", closeMenuOnEscape);
    };
  }, [tabMenu]);

  useEffect(() => {
    function moveTab(event: globalThis.PointerEvent) {
      const session = dragSessionRef.current;
      if (!session || event.pointerId !== session.pointerId) return;
      if (!session.dragging && Math.abs(event.clientX - session.startX) < 5) return;
      if (!session.dragging) {
        session.dragging = true;
        setDraggingPath(session.path);
        document.body.classList.add(styles["is-dragging-admin-tab"]);
      }
      event.preventDefault();

      const scrollElement = tabsScrollRef.current;
      if (scrollElement) {
        const bounds = scrollElement.getBoundingClientRect();
        const edgeSize = 52;
        if (event.clientX < bounds.left + edgeSize) {
          const ratio = 1 - Math.max(0, event.clientX - bounds.left) / edgeSize;
          scrollElement.scrollBy({ left: -Math.ceil(24 * ratio) });
        } else if (event.clientX > bounds.right - edgeSize) {
          const ratio = 1 - Math.max(0, bounds.right - event.clientX) / edgeSize;
          scrollElement.scrollBy({ left: Math.ceil(24 * ratio) });
        }
      }

      const hit = document.elementFromPoint(event.clientX, event.clientY);
      const tabElement =
        hit instanceof Element ? hit.closest<HTMLElement>("[data-tab-path]") : null;
      const targetPath = tabElement?.dataset.tabPath;
      if (!tabElement || !targetPath || targetPath === session.path) {
        dragTargetRef.current = null;
        setDragTarget(null);
        return;
      }
      const bounds = tabElement.getBoundingClientRect();
      const nextTarget: TabDragTarget = {
        path: targetPath,
        side: event.clientX < bounds.left + bounds.width / 2 ? "before" : "after",
      };
      dragTargetRef.current = nextTarget;
      setDragTarget(nextTarget);
    }

    function finishTabDrag(event: globalThis.PointerEvent) {
      const session = dragSessionRef.current;
      if (!session || event.pointerId !== session.pointerId) return;
      if (session.dragging) {
        suppressTabClickRef.current = true;
        setOpenTabs((current) => reorderTabs(current, session.path, dragTargetRef.current));
        window.setTimeout(() => {
          suppressTabClickRef.current = false;
        }, 0);
      }
      dragSessionRef.current = null;
      dragTargetRef.current = null;
      setDraggingPath(null);
      setDragTarget(null);
      document.body.classList.remove(styles["is-dragging-admin-tab"]);
    }

    document.addEventListener("pointermove", moveTab, { passive: false });
    document.addEventListener("pointerup", finishTabDrag);
    document.addEventListener("pointercancel", finishTabDrag);
    return () => {
      document.removeEventListener("pointermove", moveTab);
      document.removeEventListener("pointerup", finishTabDrag);
      document.removeEventListener("pointercancel", finishTabDrag);
      document.body.classList.remove(styles["is-dragging-admin-tab"]);
    };
  }, []);

  function toggleExpanded(nodeId: string) {
    setExpandedIds((current) =>
      current.includes(nodeId) ? current.filter((item) => item !== nodeId) : [...current, nodeId],
    );
  }

  function updateTabScrollState() {
    const element = tabsScrollRef.current;
    if (!element) return;
    const maxScrollLeft = Math.max(0, element.scrollWidth - element.clientWidth);
    const next = {
      overflow: maxScrollLeft > 1,
      canScrollLeft: element.scrollLeft > 1,
      canScrollRight: element.scrollLeft < maxScrollLeft - 1,
    };
    setTabScrollState((current) =>
      current.overflow === next.overflow &&
      current.canScrollLeft === next.canScrollLeft &&
      current.canScrollRight === next.canScrollRight
        ? current
        : next,
    );
  }

  function scrollTabs(direction: "left" | "right") {
    const scrollElement = tabsScrollRef.current;
    if (!scrollElement) return;
    const pageDistance = Math.min(
      scrollElement.clientWidth,
      Math.max(160, Math.floor(scrollElement.clientWidth * 0.7)),
    );
    const desired = Math.max(
      0,
      Math.min(
        scrollElement.scrollWidth - scrollElement.clientWidth,
        scrollElement.scrollLeft + (direction === "right" ? pageDistance : -pageDistance),
      ),
    );
    const offsets = activeTabs.map((tab) => tabElementsRef.current.get(tab.path)?.offsetLeft ?? 0);
    const aligned =
      direction === "right"
        ? (offsets.find((offset) => offset >= desired) ?? desired)
        : ([...offsets].reverse().find((offset) => offset <= desired) ?? desired);
    scrollElement.scrollTo({ left: aligned, behavior: "smooth" });
  }

  function showTabMenu(path: string, x: number, y: number) {
    const width = 196;
    const height = 224;
    setTabMenu({
      path,
      x: Math.max(8, Math.min(x, window.innerWidth - width - 8)),
      y: Math.max(8, Math.min(y, window.innerHeight - height - 8)),
    });
  }

  function openTabContextMenu(event: ReactMouseEvent, path: string) {
    event.preventDefault();
    event.stopPropagation();
    showTabMenu(path, event.clientX, event.clientY);
  }

  function openTabToolsMenu(event: ReactMouseEvent<HTMLButtonElement>) {
    const targetPath = openTabs.includes(pathname) ? pathname : openTabs.at(-1);
    if (!targetPath) return;
    const bounds = event.currentTarget.getBoundingClientRect();
    showTabMenu(targetPath, bounds.right - 196, bounds.bottom + 6);
  }

  function startTabDrag(event: ReactPointerEvent<HTMLDivElement>, path: string) {
    if (event.button !== 0 || (event.target as Element).closest("button")) return;
    setTabMenu(null);
    dragSessionRef.current = {
      path,
      pointerId: event.pointerId,
      startX: event.clientX,
      dragging: false,
    };
  }

  function closeTab(href: string) {
    closeTabPaths(new Set([href]));
  }

  function closeTabPaths(paths: Set<string>, preferredPath?: string) {
    setTabMenu(null);
    const currentIndex = openTabs.indexOf(pathname);
    const nextTabs = openTabs.filter((item) => !paths.has(item));
    const closingActiveTab = paths.has(pathname);
    const nextHref = closingActiveTab
      ? ((preferredPath && nextTabs.includes(preferredPath) ? preferredPath : undefined) ??
        nextTabs[currentIndex] ??
        nextTabs[currentIndex - 1])
      : undefined;
    if (closingActiveTab) {
      emptyWorkspacePendingRef.current = !nextHref;
      setBlankMode(!nextHref);
    }
    setOpenTabs(nextTabs);
    setTabReloadVersions((current) =>
      Object.fromEntries(Object.entries(current).filter(([path]) => !paths.has(path))),
    );
    paths.forEach((path) => pageCacheRef.current.delete(path));

    if (!closingActiveTab) return;
    queueMicrotask(() => {
      if (nextHref) {
        router.push(nextHref);
      } else {
        router.push("/admin");
      }
    });
  }

  function runTabMenuAction(action: TabMenuAction) {
    if (!tabMenu) return;
    const targetIndex = openTabs.indexOf(tabMenu.path);
    if (targetIndex < 0) return;
    if (action === "current") {
      closeTab(tabMenu.path);
      return;
    }
    if (action === "others") {
      closeTabPaths(new Set(openTabs.filter((path) => path !== tabMenu.path)), tabMenu.path);
      return;
    }
    if (action === "left") {
      closeTabPaths(new Set(openTabs.slice(0, targetIndex)), tabMenu.path);
      return;
    }
    if (action === "right") {
      closeTabPaths(new Set(openTabs.slice(targetIndex + 1)), tabMenu.path);
      return;
    }
    closeTabPaths(new Set(openTabs));
  }

  function reloadCurrentTab() {
    if (displayBlankWorkspace) return;
    setNotificationOpen(false);
    setUserOpen(false);
    setTabReloadVersions((current) => ({
      ...current,
      [pathname]: (current[pathname] ?? 0) + 1,
    }));
  }

  function openUtilityPage(href: string) {
    setNotificationOpen(false);
    setUserOpen(false);
    emptyWorkspacePendingRef.current = false;
    setBlankMode(false);
    router.push(href);
  }

  return (
    <div className={shellClasses("admin-shell", sidebarCollapsed && "collapsed")}>
      <aside className={shellClasses("admin-sidebar")}>
        <div className={shellClasses("brand")}>
          <button
            className={shellClasses("brand-link")}
            type="button"
            aria-label="Breezy Admin"
            onClick={() => openUtilityPage("/admin")}
          >
            <span className={shellClasses("brand-logo")}>
              <Image
                src={brandLogo}
                alt="Breezy Admin"
                priority
              />
            </span>
            {!sidebarCollapsed ? (
              <span className={shellClasses("brand-title")}>Breezy Admin</span>
            ) : null}
          </button>
        </div>

        <nav
          className={shellClasses("admin-nav")}
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
              onNavigate={() => setBlankMode(false)}
            />
          ))}
        </nav>
      </aside>

      <section className={shellClasses("admin-main")}>
        <div className={shellClasses("admin-fixed-header")}>
          <header className={shellClasses("topbar")}>
            <div className={shellClasses("topbar-left")}>
              <button
                className={shellClasses("topbar-icon")}
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
                className={shellClasses("breadcrumb")}
                aria-label="面包屑导航"
              >
                <ol>
                  {breadcrumbItems.map((item, index) => (
                    <li
                      className={shellClasses("breadcrumb-entry")}
                      key={`${item.label}-${index}`}
                    >
                      {index > 0 ? (
                        <span
                          className={shellClasses("breadcrumb-separator")}
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
                          className={shellClasses("breadcrumb-link")}
                          href={item.href}
                        >
                          {item.label}
                        </Link>
                      ) : (
                        <span
                          className={
                            index === breadcrumbItems.length - 1
                              ? shellClasses("breadcrumb-current")
                              : shellClasses("breadcrumb-link")
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

            <div className={shellClasses("topbar-right")}>
              <button
                className={shellClasses("search-trigger")}
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

              <button
                className={shellClasses("topbar-icon", "round")}
                type="button"
                title="偏好设置"
                onClick={() => openUtilityPage("/admin/profile/preferences")}
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
              </button>

              <div className={shellClasses("topbar-popover-host")}>
                <button
                  className={shellClasses("topbar-icon", "round")}
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
                  {displayUnreadCount ? <span className={shellClasses("notify-dot")} /> : null}
                </button>
                {notificationOpen ? (
                  <section className={shellClasses("notification-panel")}>
                    <div className={shellClasses("panel-header")}>
                      <strong>通知</strong>
                    </div>
                    <button
                      className={shellClasses("mark-read")}
                      type="button"
                      onClick={() => void markAllRead()}
                    >
                      全部标记为已读
                    </button>
                    <div className={shellClasses("notification-list")}>
                      {previewList.length === 0 ? (
                        <div className={shellClasses("notification-empty")}>暂无未读消息</div>
                      ) : null}
                      {previewList.map((item) => (
                        <button
                          key={item.id}
                          className={shellClasses("notification-item")}
                          type="button"
                          onClick={() => void markRead(item.id)}
                        >
                          <span className={shellClasses("notification-avatar")}>
                            {item.title.slice(0, 1).toUpperCase()}
                          </span>
                          <span className={shellClasses("notification-body")}>
                            <strong>{item.title}</strong>
                            <span>{item.content || "暂无摘要内容"}</span>
                            <small>{formatDateTime(item.createdAt || "")}</small>
                          </span>
                          {!item.read ? (
                            <span className={shellClasses("notification-unread")} />
                          ) : null}
                        </button>
                      ))}
                    </div>
                    <footer className={shellClasses("panel-footer")}>
                      <button
                        type="button"
                        onClick={() => void markAllRead()}
                      >
                        清空
                      </button>
                      <button
                        className={shellClasses("primary")}
                        type="button"
                        onClick={() => setNotificationOpen(false)}
                      >
                        查看全部
                      </button>
                    </footer>
                  </section>
                ) : null}
              </div>

              <div
                className={shellClasses("topbar-popover-host")}
                ref={userPanelHostRef}
              >
                <button
                  className={shellClasses("avatar-button")}
                  type="button"
                  title="用户菜单"
                  onClick={() => setUserOpen((value) => !value)}
                >
                  <span className={shellClasses("avatar-face")}>{avatarText}</span>
                  <span className={shellClasses("avatar-status")} />
                </button>
                {userOpen ? (
                  <section className={shellClasses("user-panel")}>
                    <div className={shellClasses("user-card")}>
                      <span className={shellClasses("avatar-large")}>{avatarText}</span>
                      <div className={shellClasses("user-meta")}>
                        <strong>{userName}</strong>
                        <span>{userAccount}</span>
                      </div>
                    </div>
                    <div className={shellClasses("user-menu")}>
                      <button
                        type="button"
                        onClick={() => openUtilityPage("/admin/profile")}
                      >
                        个人中心
                      </button>
                      <button
                        type="button"
                        onClick={() => openUtilityPage("/admin/profile/password")}
                      >
                        修改密码
                      </button>
                      <button
                        type="button"
                        onClick={() => openUtilityPage("/admin/profile/preferences")}
                      >
                        偏好设置
                      </button>
                      <button
                        type="button"
                        onClick={() => openUtilityPage("/admin/help")}
                      >
                        问题与帮助
                      </button>
                      <button
                        type="button"
                        onClick={() => void logoutSession()}
                      >
                        退出登录
                      </button>
                    </div>
                  </section>
                ) : null}
              </div>
            </div>
          </header>

          <div className={shellClasses("tabs-row")}>
            {tabScrollState.overflow ? (
              <button
                className={shellClasses("tab-scroll-control")}
                type="button"
                title="向左滚动标签"
                aria-label="向左滚动标签"
                disabled={!tabScrollState.canScrollLeft}
                onClick={() => scrollTabs("left")}
              >
                <svg
                  viewBox="0 0 20 20"
                  aria-hidden="true"
                >
                  <path d="m12.5 4.5-5 5.5 5 5.5" />
                </svg>
              </button>
            ) : null}
            <div
              className={shellClasses("tabs-scroll")}
              ref={tabsScrollRef}
              onScroll={updateTabScrollState}
            >
              <div
                className={shellClasses("tabs-list")}
                onContextMenu={(event) => {
                  if (event.target !== event.currentTarget || activeTabs.length === 0) return;
                  const targetPath = openTabs.includes(pathname) ? pathname : openTabs.at(-1);
                  if (targetPath) openTabContextMenu(event, targetPath);
                }}
              >
                {activeTabs.map((tab) => {
                  const active = pathname === tab.path;
                  const insertSide = dragTarget?.path === tab.path ? dragTarget.side : null;
                  return (
                    <div
                      key={tab.path}
                      ref={(element) => {
                        if (element) tabElementsRef.current.set(tab.path, element);
                        else tabElementsRef.current.delete(tab.path);
                      }}
                      data-tab-path={tab.path}
                      className={shellClasses(
                        "tab-item",
                        active && "active",
                        draggingPath === tab.path && "dragging",
                        insertSide && `insert-${insertSide}`,
                      )}
                      onPointerDown={(event) => startTabDrag(event, tab.path)}
                      onContextMenu={(event) => openTabContextMenu(event, tab.path)}
                      onClickCapture={(event) => {
                        if (!suppressTabClickRef.current) return;
                        event.preventDefault();
                        event.stopPropagation();
                      }}
                    >
                      <Link
                        href={tab.path}
                        className={shellClasses("tab-link")}
                        draggable={false}
                        onClick={() => setBlankMode(false)}
                      >
                        <span
                          className={shellClasses("tab-icon")}
                          aria-hidden="true"
                        >
                          <ResourceIconImage src={tab.iconUrl} />
                        </span>
                        <span className={shellClasses("tab-title-text")}>{tab.title}</span>
                      </Link>
                      <button
                        className={shellClasses("tab-close")}
                        type="button"
                        title={`关闭 ${tab.title}`}
                        onClick={(event) => {
                          event.preventDefault();
                          event.stopPropagation();
                          closeTab(tab.path);
                        }}
                      >
                        <svg
                          viewBox="0 0 24 24"
                          aria-hidden="true"
                        >
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
            {tabScrollState.overflow ? (
              <button
                className={shellClasses("tab-scroll-control")}
                type="button"
                title="向右滚动标签"
                aria-label="向右滚动标签"
                disabled={!tabScrollState.canScrollRight}
                onClick={() => scrollTabs("right")}
              >
                <svg
                  viewBox="0 0 20 20"
                  aria-hidden="true"
                >
                  <path d="m7.5 4.5 5 5.5-5 5.5" />
                </svg>
              </button>
            ) : null}
            <div className={shellClasses("tabs-tools")}>
              <button
                className={shellClasses("tabbar-tool")}
                type="button"
                title="刷新当前页面"
                aria-label="刷新当前标签页"
                onClick={reloadCurrentTab}
              >
                <svg
                  viewBox="0 0 20 20"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.7"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M17.5 10a7.5 7.5 0 1 1-7.5-7.5c2.1 0 4.1.83 5.61 2.28L17.5 6.67" />
                  <path d="M17.5 2.5v4.17h-4.17" />
                </svg>
              </button>
              <button
                className={shellClasses("tabbar-tool")}
                type="button"
                title="标签页操作"
                aria-label="打开标签页操作菜单"
                disabled={openTabs.length === 0}
                onClick={openTabToolsMenu}
              >
                <svg
                  viewBox="0 0 20 20"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.7"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="m5 7.5 5 5 5-5" />
                </svg>
              </button>
            </div>
            {tabMenu
              ? createPortal(
                  <div
                    className={shellClasses("tabs-context-menu")}
                    role="menu"
                    style={{ left: tabMenu.x, top: tabMenu.y }}
                    onContextMenu={(event) => event.preventDefault()}
                  >
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => runTabMenuAction("current")}
                    >
                      关闭当前标签
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      disabled={openTabs.length <= 1}
                      onClick={() => runTabMenuAction("others")}
                    >
                      关闭其它标签
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      disabled={openTabs.indexOf(tabMenu.path) <= 0}
                      onClick={() => runTabMenuAction("left")}
                    >
                      关闭左侧标签
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      disabled={openTabs.indexOf(tabMenu.path) >= openTabs.length - 1}
                      onClick={() => runTabMenuAction("right")}
                    >
                      关闭右侧标签
                    </button>
                    <div className={shellClasses("tabs-context-menu__separator")} />
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => runTabMenuAction("all")}
                    >
                      关闭全部标签
                    </button>
                  </div>,
                  document.body,
                )
              : null}
          </div>
        </div>

        <main className={shellClasses("admin-content")}>
          <div className={shellClasses("admin-workspace")}>
            {displayBlankWorkspace ? (
              <div className={shellClasses("admin-workspace-blank")} />
            ) : tabKeepAlive ? (
              <>
                {workspaceTabs.map((tab) => {
                  const active = pathname === tab.path;
                  const page = pageCacheRef.current.get(tab.path);
                  if (!page) return null;
                  return (
                    <Activity
                      key={tab.path}
                      mode={active ? "visible" : "hidden"}
                    >
                      <div className={shellClasses("admin-tab-panel")}>
                        <div key={`${tab.path}:${tabReloadVersions[tab.path] ?? 0}`}>{page}</div>
                      </div>
                    </Activity>
                  );
                })}
                {!workspaceTabs.some((tab) => tab.path === pathname) ? (
                  <div
                    className={shellClasses("admin-tab-panel")}
                    key={`${pathname}:${currentTabReloadVersion}`}
                  >
                    {children}
                  </div>
                ) : null}
              </>
            ) : (
              <div
                className={shellClasses("admin-tab-panel")}
                key={`${pathname}:${currentTabReloadVersion}`}
              >
                {children}
              </div>
            )}
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

interface TabEntry {
  path: string;
  title: string;
  iconUrl: string;
}

type TabMenuAction = "current" | "others" | "left" | "right" | "all";

interface TabMenuState {
  path: string;
  x: number;
  y: number;
}

interface TabDragSession {
  path: string;
  pointerId: number;
  startX: number;
  dragging: boolean;
}

interface TabDragTarget {
  path: string;
  side: "before" | "after";
}

function shellClasses(...classNames: Array<string | false | null | undefined>): string {
  return classNames
    .filter((className): className is string => Boolean(className))
    .map((className) => styles[className])
    .join(" ");
}

function reorderTabs(tabs: string[], sourcePath: string, target: TabDragTarget | null): string[] {
  if (!target || sourcePath === target.path) return tabs;
  const sourceIndex = tabs.indexOf(sourcePath);
  if (sourceIndex < 0) return tabs;
  const next = tabs.filter((path) => path !== sourcePath);
  const targetIndex = next.indexOf(target.path);
  if (targetIndex < 0) return tabs;
  next.splice(targetIndex + (target.side === "after" ? 1 : 0), 0, sourcePath);
  return next.every((path, index) => path === tabs[index]) ? tabs : next;
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

function resolveTabEntry(pathname: string, byPath: Map<string, MenuPathEntry>): TabEntry | null {
  const menuEntry = byPath.get(pathname);
  if (menuEntry?.node.path) {
    return {
      path: menuEntry.node.path,
      title: menuEntry.node.title,
      iconUrl: menuEntry.node.iconUrl,
    };
  }

  const route = getAdminSelfServiceRoute(pathname);
  if (!route) {
    return null;
  }

  return {
    path: route.path,
    title: route.title,
    iconUrl: resolveHiddenRouteIconUrl(route.path),
  };
}

function resolveHiddenRouteIconUrl(path: string): string {
  if (path === "/admin/profile/preferences") {
    return resolveResourceIconUrl("settings", "MENU") || "/admin-icons/default-menu.svg";
  }
  if (path === "/admin/profile") {
    return resolveResourceIconUrl("user", "MENU") || "/admin-icons/default-menu.svg";
  }
  if (path === "/admin/profile/password") {
    return resolveResourceIconUrl("shield", "MENU") || "/admin-icons/default-menu.svg";
  }
  if (path === "/admin/help") {
    return resolveResourceIconUrl("book", "MENU") || "/admin-icons/default-menu.svg";
  }
  return resolveResourceIconUrl("menu", "MENU") || "/admin-icons/default-menu.svg";
}

function ResourceIconImage({
  src,
  nodeType = "MENU",
}: {
  src: string;
  nodeType?: "DIRECTORY" | "MENU";
}) {
  return (
    <img
      src={src}
      alt=""
      onError={(event) => {
        const fallbackUrl = resolveResourceIconUrl(null, nodeType);
        if (fallbackUrl && event.currentTarget.getAttribute("src") !== fallbackUrl) {
          event.currentTarget.src = fallbackUrl;
        }
      }}
    />
  );
}

function AdminNavItem({
  node,
  pathname,
  collapsed,
  expandedIds,
  onToggle,
  onNavigate,
  depth = 0,
}: {
  node: AdminMenuNode;
  pathname: string;
  collapsed: boolean;
  expandedIds: string[];
  onToggle: (nodeId: string) => void;
  onNavigate: () => void;
  depth?: number;
}) {
  const hasChildren = node.children.length > 0;
  const expanded = hasChildren && expandedIds.includes(node.id) && !collapsed;
  const active = Boolean(node.path) && pathname === node.path;
  const hasActiveDescendant = hasChildren && containsPath(node.children, pathname);

  if (node.nodeType === "DIRECTORY") {
    return (
      <div className={shellClasses("nav-node")}>
        <button
          className={shellClasses(
            "nav-item",
            "nav-directory",
            expanded && "expanded",
            hasActiveDescendant && !expanded && "active-ancestor",
          )}
          type="button"
          title={node.title}
          style={!collapsed ? { paddingLeft: `${12 + depth * 14}px` } : undefined}
          onClick={() => onToggle(node.id)}
        >
          <span className={shellClasses("nav-item-main")}>
            <span
              className={shellClasses("nav-icon")}
              aria-hidden="true"
            >
              <ResourceIconImage
                src={node.iconUrl}
                nodeType="DIRECTORY"
              />
            </span>
            {!collapsed ? <span className={shellClasses("nav-label")}>{node.title}</span> : null}
          </span>
          {!collapsed ? (
            <span
              className={shellClasses("nav-caret")}
              aria-hidden="true"
            >
              <svg viewBox="0 0 24 24">
                <path
                  d="m9 6 6 6-6 6"
                  fill="none"
                  stroke="currentColor"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="1.8"
                />
              </svg>
            </span>
          ) : null}
        </button>
        {expanded ? (
          <div className={shellClasses("nav-children")}>
            {node.children.map((child) => (
              <AdminNavItem
                key={child.id}
                node={child}
                pathname={pathname}
                collapsed={collapsed}
                expandedIds={expandedIds}
                onToggle={onToggle}
                onNavigate={onNavigate}
                depth={depth + 1}
              />
            ))}
          </div>
        ) : null}
      </div>
    );
  }

  const activeDescendantExpanded =
    hasChildren && hasActiveDescendant && expandedIds.includes(node.id);

  return (
    <div className={shellClasses("nav-node")}>
      <div
        className={shellClasses("nav-item", active && "active")}
        style={!collapsed ? { paddingLeft: `${12 + depth * 14}px` } : undefined}
      >
        {node.path ? (
          <Link
            href={node.path}
            className={shellClasses("nav-item-main")}
            title={node.title}
            onClick={onNavigate}
          >
            <span
              className={shellClasses("nav-icon")}
              aria-hidden="true"
            >
              <ResourceIconImage src={node.iconUrl} />
            </span>
            {!collapsed ? <span className={shellClasses("nav-label")}>{node.title}</span> : null}
          </Link>
        ) : (
          <span className={shellClasses("nav-item-main", "nav-linkless")}>
            <span
              className={shellClasses("nav-icon")}
              aria-hidden="true"
            >
              <ResourceIconImage src={node.iconUrl} />
            </span>
            {!collapsed ? <span className={shellClasses("nav-label")}>{node.title}</span> : null}
          </span>
        )}
      </div>
      {activeDescendantExpanded ? (
        <div className={shellClasses("nav-children")}>
          {node.children.map((child) => (
            <AdminNavItem
              key={child.id}
              node={child}
              pathname={pathname}
              collapsed={collapsed}
              expandedIds={expandedIds}
              onToggle={onToggle}
              onNavigate={onNavigate}
              depth={depth + 1}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function containsPath(nodes: AdminMenuNode[], pathname: string): boolean {
  return nodes.some((node) => node.path === pathname || containsPath(node.children, pathname));
}
