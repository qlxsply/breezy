<template>
  <div
    class="admin-shell"
    :class="{ collapsed: sidebarCollapsed }"
  >
    <aside class="admin-sidebar">
      <div class="brand">
        <button
          class="brand-link"
          type="button"
          aria-label="Breezy Admin"
          @click="navigate('/')"
        >
          <span class="brand-logo">
            <img
              :src="brandLogo"
              alt="Breezy Admin"
            />
          </span>
          <span
            v-if="!sidebarCollapsed"
            class="brand-title"
          >
            Breezy Admin
          </span>
        </button>
      </div>

      <nav class="admin-nav">
        <AdminMenuNode
          v-for="node in menuTree"
          :key="node.id"
          :node="node"
          :level="0"
        />
      </nav>
    </aside>

    <section class="admin-main">
      <div class="admin-fixed-header">
        <header class="topbar">
          <div class="topbar-left">
            <nav
              class="breadcrumb"
              aria-label="面包屑导航"
            >
              <ol>
                <template
                  v-for="(item, index) in breadcrumbItems"
                  :key="`${item.label}-${index}`"
                >
                  <li class="breadcrumb-entry">
                    <button
                      v-if="item.to && !item.current"
                      class="breadcrumb-link"
                      type="button"
                      @click="router.push(item.to)"
                    >
                      <span class="breadcrumb-icon">
                        <svg
                          viewBox="0 0 24 24"
                          aria-hidden="true"
                        >
                          <path
                            d="M3 3h7v7H3zm11 0h7v5h-7zm0 9h7v9h-7zM3 16h7v5H3z"
                            fill="none"
                            stroke="currentColor"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="1.8"
                          />
                        </svg>
                      </span>
                      <span>{{ item.label }}</span>
                    </button>
                    <span
                      v-else
                      class="breadcrumb-current"
                    >
                      <span>{{ item.label }}</span>
                    </span>
                  </li>
                  <li
                    v-if="index < breadcrumbItems.length - 1"
                    class="breadcrumb-separator"
                    aria-hidden="true"
                  >
                    <svg
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                    >
                      <path
                        d="m9 18 6-6-6-6"
                        fill="none"
                        stroke="currentColor"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="1.8"
                      />
                    </svg>
                  </li>
                </template>
              </ol>
            </nav>
          </div>

          <div class="topbar-right">
            <button
              class="search-trigger"
              type="button"
              title="搜索"
              @click="showFeatureTip('全局搜索')"
            >
              <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  d="m21 21-4.34-4.34M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.8"
                />
              </svg>
              <span>搜索</span>
              <kbd>Ctrl K</kbd>
            </button>

            <button
              class="topbar-icon round"
              type="button"
              title="设置"
              @click="goPreferences"
            >
              <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  d="M9.67 4.14a2.34 2.34 0 0 1 4.66 0 2.34 2.34 0 0 0 3.32 1.91 2.34 2.34 0 0 1 2.33 4.03 2.34 2.34 0 0 0 0 3.84 2.34 2.34 0 0 1-2.33 4.03 2.34 2.34 0 0 0-3.32 1.91 2.34 2.34 0 0 1-4.66 0 2.34 2.34 0 0 0-3.32-1.91 2.34 2.34 0 0 1-2.33-4.03 2.34 2.34 0 0 0 0-3.84 2.34 2.34 0 0 1 2.33-4.03 2.34 2.34 0 0 0 3.32-1.91"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.6"
                />
                <circle
                  cx="12"
                  cy="12"
                  r="3"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.6"
                />
              </svg>
            </button>

            <div
              ref="notifyRootRef"
              class="topbar-popover-host"
            >
              <button
                class="topbar-icon round notify-button"
                type="button"
                title="通知"
                @click="toggleNotificationPopover"
              >
                <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    d="M10.27 21a2 2 0 0 0 3.46 0M4 17h16a1 1 0 0 0 .74-1.67C19.41 13.96 18 12.5 18 8A6 6 0 0 0 6 8c0 4.5-1.41 5.96-2.74 7.33A1 1 0 0 0 4 17"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.8"
                  />
                </svg>
                <span
                  v-if="displayUnreadCount"
                  class="notify-dot"
                ></span>
              </button>

              <section
                v-if="notificationOpen"
                class="notification-panel"
              >
                <div class="panel-header">
                  <strong>通知</strong>
                  <button
                    class="panel-icon"
                    type="button"
                    title="消息中心"
                    @click="openMessageCenter"
                  >
                    <svg
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                    >
                      <path
                        d="M4 4h16v16H4zM4 8l8 5 8-5"
                        fill="none"
                        stroke="currentColor"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="1.8"
                      />
                    </svg>
                  </button>
                </div>

                <button
                  class="mark-read"
                  type="button"
                  @click="onMarkAllRead"
                >
                  全部标记为已读
                </button>

                <div class="notification-list">
                  <button
                    v-for="item in previewList"
                    :key="item.id"
                    class="notification-item"
                    type="button"
                    @click="openNotificationDetail(item)"
                  >
                    <span class="notification-avatar">{{ notificationAvatar(item.title) }}</span>
                    <span class="notification-body">
                      <strong>{{ item.title }}</strong>
                      <span>{{ item.content || "暂无摘要内容" }}</span>
                      <small>{{ formatDateTime(item.createdAt) }}</small>
                    </span>
                    <span
                      v-if="!item.read"
                      class="notification-unread"
                    ></span>
                  </button>
                  <div
                    v-if="previewList.length === 0"
                    class="notification-empty"
                  >
                    暂无未读消息
                  </div>
                </div>

                <footer class="panel-footer">
                  <button
                    type="button"
                    @click="onMarkAllRead"
                  >
                    清空
                  </button>
                  <button
                    class="primary"
                    type="button"
                    @click="openMessageCenter"
                  >
                    查看所有消息
                  </button>
                </footer>
              </section>
            </div>

            <div
              ref="userRootRef"
              class="topbar-popover-host"
            >
              <button
                class="avatar-button"
                type="button"
                title="当前用户"
                @click="toggleUserPopover"
              >
                <span class="avatar-face">{{ avatarText }}</span>
                <span class="avatar-status"></span>
              </button>

              <section
                v-if="userOpen"
                class="user-panel"
              >
                <div class="user-card">
                  <span class="avatar-large">{{ avatarText }}</span>
                  <div class="user-meta">
                    <strong>{{ userName }}</strong>
                    <span>{{ userAccount }}</span>
                  </div>
                </div>

                <div class="user-menu">
                  <button
                    type="button"
                    @click="goProfile"
                  >
                    <span class="user-menu-icon">
                      <svg
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                      >
                        <path
                          d="M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4zm-7 8a7 7 0 1 1 14 0"
                          fill="none"
                          stroke="currentColor"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="1.8"
                        />
                      </svg>
                    </span>
                    <span>个人中心</span>
                  </button>
                  <button
                    type="button"
                    @click="goChangePassword"
                  >
                    <span class="user-menu-icon">
                      <svg
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                      >
                        <path
                          d="M6 10V8a6 6 0 1 1 12 0v2M7 10h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2Z"
                          fill="none"
                          stroke="currentColor"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="1.8"
                        />
                      </svg>
                    </span>
                    <span>修改密码</span>
                  </button>
                  <button
                    type="button"
                    @click="goPreferences"
                  >
                    <span class="user-menu-icon">
                      <svg
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                      >
                        <path
                          d="M12 3v3m0 12v3M4.93 4.93l2.12 2.12m9.9 9.9 2.12 2.12M3 12h3m12 0h3M4.93 19.07l2.12-2.12m9.9-9.9 2.12-2.12M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8Z"
                          fill="none"
                          stroke="currentColor"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="1.8"
                        />
                      </svg>
                    </span>
                    <span>偏好设置</span>
                  </button>
                  <button
                    type="button"
                    @click="goHelp"
                  >
                    <span class="user-menu-icon">
                      <svg
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                      >
                        <path
                          d="M9.09 9a3 3 0 1 1 5.82 1c0 2-3 2-3 4m.09 4h.01"
                          fill="none"
                          stroke="currentColor"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="1.8"
                        />
                        <circle
                          cx="12"
                          cy="12"
                          r="9"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="1.8"
                        />
                      </svg>
                    </span>
                    <span>问题与帮助</span>
                  </button>
                </div>

                <div class="user-menu user-menu-secondary">
                  <button
                    type="button"
                    @click="onLogout"
                  >
                    <span class="user-menu-icon">
                      <svg
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                      >
                        <path
                          d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"
                          fill="none"
                          stroke="currentColor"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="1.8"
                        />
                        <path
                          d="m16 17 5-5-5-5M21 12H9"
                          fill="none"
                          stroke="currentColor"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="1.8"
                        />
                      </svg>
                    </span>
                    <span>退出登录</span>
                    <kbd>Alt Q</kbd>
                  </button>
                </div>
              </section>
            </div>
          </div>
        </header>

        <section class="tabs-row">
          <button
            v-if="showTabScrollControls"
            class="tabs-nav"
            :class="{ disabled: !canScrollTabsLeft }"
            type="button"
            title="向左滚动标签"
            :disabled="!canScrollTabsLeft"
            @click="scrollTabs('left')"
          >
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                d="m15 18-6-6 6-6"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.8"
              />
            </svg>
          </button>

          <div
            ref="tabsScrollRef"
            class="tabs-scroll"
            @scroll="updateTabScrollState"
          >
            <div class="tabs-list">
              <div
                v-for="tab in visitedTabs"
                :key="tab.fullPath"
                class="tab-item"
                :class="{ active: tab.fullPath === route.fullPath, pinned: tab.pinned }"
                role="button"
                tabindex="0"
                @click="router.push(tab.fullPath)"
                @keydown.enter="router.push(tab.fullPath)"
              >
                <span class="tab-icon">
                  <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                  >
                    <path
                      d="M3 3v18h18M7 12v5h12V8l-5 5-4-4z"
                      fill="none"
                      stroke="currentColor"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1.8"
                    />
                  </svg>
                </span>
                <span class="tab-title-text">{{ tab.title }}</span>
                <button
                  v-if="!tab.pinned"
                  class="tab-close"
                  type="button"
                  title="关闭标签"
                  @click.stop="closeTab(tab)"
                >
                  <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                  >
                    <path
                      d="M18 6 6 18M6 6l12 12"
                      fill="none"
                      stroke="currentColor"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1.8"
                    />
                  </svg>
                </button>
                <span
                  v-else
                  class="tab-pin"
                  title="固定标签"
                >
                  <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                  >
                    <path
                      d="M12 17v4M8 3h8a2 2 0 0 1 0 4 1 1 0 0 0-1 1v3.76a2 2 0 0 0 1.11 1.79l1.78.9A2 2 0 0 1 19 16.24V17H5v-.76a2 2 0 0 1 1.11-1.79l1.78-.9A2 2 0 0 0 9 11.76V8a1 1 0 0 0-1-1 2 2 0 0 1 0-4z"
                      fill="none"
                      stroke="currentColor"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1.7"
                    />
                  </svg>
                </span>
              </div>
            </div>
          </div>

          <button
            v-if="showTabScrollControls"
            class="tabs-nav"
            :class="{ disabled: !canScrollTabsRight }"
            type="button"
            title="向右滚动标签"
            :disabled="!canScrollTabsRight"
            @click="scrollTabs('right')"
          >
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                d="m9 18 6-6-6-6"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.8"
              />
            </svg>
          </button>

          <div class="tabs-tools">
            <button
              class="tabbar-tool"
              type="button"
              title="标签布局"
              @click="showFeatureTip('标签布局')"
            >
              <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  d="M3 3h7v7H3zm11 0h7v7h-7zm0 11h7v7h-7zM3 14h7v7H3z"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.8"
                />
              </svg>
            </button>
            <button
              class="tabbar-tool"
              type="button"
              title="刷新"
              @click="refreshCurrentPage"
            >
              <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  d="M20 11a8.1 8.1 0 0 0-15.5-2M4 5v4h4"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.8"
                />
                <path
                  d="M4 13a8.1 8.1 0 0 0 15.5 2M20 19v-4h-4"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.8"
                />
              </svg>
            </button>
            <button
              class="tabbar-tool"
              type="button"
              title="全屏"
              @click="toggleFullscreen"
            >
              <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  d="M3 7V5a2 2 0 0 1 2-2h2m10 0h2a2 2 0 0 1 2 2v2m0 10v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.8"
                />
              </svg>
            </button>
          </div>
        </section>
      </div>

      <main class="admin-content">
        <div class="admin-workspace">
          <slot />
        </div>
      </main>
    </section>

    <MessagesModal
      :open="messageCenterOpen"
      :initial-message="selectedMessage"
      @close="messageCenterOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import {
  computed,
  defineComponent,
  h,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  Transition,
  watch,
} from "vue";
import type { RouteLocationRaw } from "vue-router";
import { useRoute, useRouter } from "vue-router";

import brandLogo from "../assets/brand-logo.png";
import MessagesModal from "../components/notifications/MessagesModal.vue";
import { logout as logoutAction, useAuthUser } from "../registry/auth.registry";
import {
  ensureUnreadLoaded,
  markAllRead,
  markRead,
  useUnreadCount,
  useUnreadList,
} from "../registry/notifications.registry";
import { getResources } from "../registry/resources.registry";
import type { NotificationItem } from "../types/notification";
import type { ResourceEntry } from "../types/resource-admin";
import { formatDateTime } from "../utils/formatter";
import { message } from "../utils/message";
import { resolveResourceIconUrl } from "../utils/resource-icon";

type MenuNodeType = "directory" | "menu";

interface MenuNode {
  id: string;
  name: string;
  type: MenuNodeType;
  to?: string;
  fallbackTo?: string;
  order: number;
  icon?: string;
  children: MenuNode[];
}

interface BreadcrumbItem {
  label: string;
  to?: RouteLocationRaw;
  current?: boolean;
}

interface VisitedTab {
  title: string;
  fullPath: string;
  pinned: boolean;
}

const router = useRouter();
const route = useRoute();

const authUser = useAuthUser();
const unreadCount = useUnreadCount();
const unreadList = useUnreadList();

const sidebarCollapsed = ref(false);
const notificationOpen = ref(false);
const userOpen = ref(false);
const messageCenterOpen = ref(false);
const selectedMessage = ref<NotificationItem | null>(null);

const notifyRootRef = ref<HTMLElement | null>(null);
const userRootRef = ref<HTMLElement | null>(null);
const tabsScrollRef = ref<HTMLElement | null>(null);

const showTabScrollControls = ref(false);
const canScrollTabsLeft = ref(false);
const canScrollTabsRight = ref(false);

const visitedTabs = ref<VisitedTab[]>([
  {
    title: "工作台",
    fullPath: "/",
    pinned: true,
  },
]);

function normalizeAdminRoutePath(path?: string): string | undefined {
  if (!path) {
    return undefined;
  }
  const normalized = path.trim();
  if (normalized === "/admin") {
    return "/";
  }
  if (normalized.startsWith("/admin/")) {
    return normalized.substring("/admin".length);
  }
  return normalized || undefined;
}

const previewList = computed(() => unreadList.value.slice(0, 4));
const displayUnreadCount = computed(() => unreadCount.value > 0);

const userName = computed(() => {
  const account = authUser.value?.account?.trim();
  const username = authUser.value?.username?.trim();
  return account || username || "Admin";
});

const userAccount = computed(() => authUser.value?.account?.trim() || "账号");

const avatarText = computed(() => {
  const source = userName.value.trim();
  return source ? source.slice(0, 1).toUpperCase() : "A";
});

const DEFAULT_DIRECTORY_ICON_PATH =
  "M160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h240l80 80h320q33 0 56.5 23.5T880-640v400q0 33-23.5 56.5T800-160H160Zm0-80h640v-400H447l-80-80H160v480Zm0 0v-480 480Z";

const DEFAULT_MENU_ICON_PATH =
  "M120-240v-80h720v80H120Zm0-200v-80h720v80H120Zm0-200v-80h720v80H120Z";

const expandedMenuKeys = ref<Set<string>>(new Set());
const menuTree = computed<MenuNode[]>(() => buildMenuTree(getResources()));

const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
  if (route.meta?.standaloneBreadcrumb === true) {
    return [{ label: resolveRouteTitle(route), current: true }];
  }
  const matchedNodes = findMenuNodePath(menuTree.value, route.path);
  if (matchedNodes.length === 0) {
    return [{ label: "管理后台" }, { label: resolveRouteTitle(route), current: true }];
  }
  return matchedNodes.map((node, index) => ({
    label: node.name,
    to: node.to,
    current: index === matchedNodes.length - 1,
  }));
});

watch(
  () => route.fullPath,
  async () => {
    ensureVisitedTab();
    notificationOpen.value = false;
    userOpen.value = false;
    await nextTick();
    scrollActiveTabIntoView();
    updateTabScrollState();
  },
  { immediate: true },
);

watch(
  () => visitedTabs.value.map((tab) => tab.fullPath).join("|"),
  async () => {
    await nextTick();
    updateTabScrollState();
  },
);

watch(
  () => route.path,
  () => {
    const activeParentKeys = collectActiveParentKeys(menuTree.value, route.path);
    if (activeParentKeys === null) {
      return;
    }
    const next = new Set(expandedMenuKeys.value);
    activeParentKeys.forEach((key) => next.add(key));
    expandedMenuKeys.value = next;
  },
  { immediate: true },
);

onMounted(async () => {
  document.addEventListener("pointerdown", handleDocumentPointerDown);
  document.addEventListener("keydown", handleDocumentKeyDown);
  window.addEventListener("resize", updateTabScrollState);
  await nextTick();
  updateTabScrollState();
});

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", handleDocumentPointerDown);
  document.removeEventListener("keydown", handleDocumentKeyDown);
  window.removeEventListener("resize", updateTabScrollState);
});

function buildMenuTree(resources: ResourceEntry[]): MenuNode[] {
  const nodeMap = new Map<string, MenuNode>();
  const roots: MenuNode[] = [];

  resources.filter(isAdminMenuResource).forEach((item) => {
    nodeMap.set(item.id, {
      id: item.id,
      name: item.name,
      type: resolveMenuNodeType(item),
      to: item.openMode === "PAGE" && item.url ? normalizeAdminRoutePath(item.url) : undefined,
      fallbackTo: undefined,
      order: item.orderNo ?? 0,
      icon: item.icon || undefined,
      children: [],
    });
  });

  resources.filter(isAdminMenuResource).forEach((item) => {
    const node = nodeMap.get(item.id);
    if (!node) {
      return;
    }
    if (!item.parentId) {
      roots.push(node);
      return;
    }
    const parent = nodeMap.get(item.parentId);
    if (!parent) {
      roots.push(node);
      return;
    }
    if (parent.type === "menu" && node.type === "directory") {
      console.warn(`菜单节点 "${parent.name}" 下不能挂目录节点 "${node.name}"，已跳过`);
      return;
    }
    parent.children.push(node);
  });

  sortMenuNodes(roots);
  resolveDirectoryFallbackTarget(roots);
  return roots;
}

function resolveRouteTitle(target: { meta?: Record<string, unknown> }): string {
  const header = (target.meta?.header as { title?: string } | undefined) ?? {};
  if (typeof header.title === "string" && header.title.trim()) {
    return header.title.trim();
  }
  return "未命名页面";
}

// function isAdminMenuResource(resource: ResourceEntry): boolean {
//   if (resource.type !== "MENU" || !resource.enabled) {
//     return false;
//   }
//   if (resource.openMode === "NONE") {
//     return true;
//   }
//   return resource.scope === "SETTING" && resource.url.startsWith("/admin");
// }

function isAdminMenuResource(resource: ResourceEntry): boolean {
  if (resource.type !== "MENU" || !resource.enabled) {
    return false;
  }

  if (resource.openMode === "NONE") {
    return true;
  }

  return Boolean(resource.url?.startsWith("/admin"));
}

function resolveMenuNodeType(resource: ResourceEntry): MenuNodeType {
  return resource.openMode === "NONE" ? "directory" : "menu";
}

function sortMenuNodes(nodes: MenuNode[]): void {
  nodes.sort((left, right) => left.order - right.order);
  nodes.forEach((node) => sortMenuNodes(node.children));
}

function resolveDirectoryFallbackTarget(nodes: MenuNode[]): void {
  nodes.forEach((node) => {
    resolveDirectoryFallbackTarget(node.children);
    if (node.type === "directory" && !node.to) {
      node.fallbackTo = findFirstNavigableChild(node.children);
    }
  });
}

function findFirstNavigableChild(children: MenuNode[]): string | undefined {
  for (const child of children) {
    if (child.to) {
      return child.to;
    }
    const nested = findFirstNavigableChild(child.children);
    if (nested) {
      return nested;
    }
  }
  return undefined;
}

function toggleMenuNode(nodeId: string): void {
  const next = new Set(expandedMenuKeys.value);
  if (next.has(nodeId)) {
    next.delete(nodeId);
  } else {
    next.add(nodeId);
  }
  expandedMenuKeys.value = next;
}

function isMenuNodeExpanded(nodeId: string): boolean {
  return expandedMenuKeys.value.has(nodeId);
}

function handleMenuNodeClick(node: MenuNode): void {
  const hasChildren = node.children.length > 0;
  if (node.type === "directory") {
    if (hasChildren) {
      toggleMenuNode(node.id);
    }
    return;
  }

  if (hasChildren) {
    toggleMenuNode(node.id);
  }

  if (node.to) {
    navigate(node.to);
  }
}

function isMenuNodeActive(node: MenuNode): boolean {
  if (node.to && normalizeMenuPath(node.to) === normalizeMenuPath(route.path)) {
    return true;
  }

  return node.children.some((child) => isMenuNodeActive(child));
}

function normalizeMenuPath(path: string): string {
  const cleanPath = path.split("?")[0]?.split("#")[0] || "";

  if (cleanPath.length > 1 && cleanPath.endsWith("/")) {
    return cleanPath.slice(0, -1);
  }

  return cleanPath;
}

function collectActiveParentKeys(
  nodes: MenuNode[],
  currentPath: string,
  parents: string[] = [],
): string[] | null {
  const normalizedCurrentPath = normalizeMenuPath(currentPath);

  for (const node of nodes) {
    if (node.to && normalizeMenuPath(node.to) === normalizedCurrentPath) {
      return parents;
    }

    const matched = collectActiveParentKeys(node.children, currentPath, [...parents, node.id]);
    if (matched !== null) {
      return matched;
    }
  }

  return null;
}

function findMenuNodePath(
  nodes: MenuNode[],
  currentPath: string,
  parents: MenuNode[] = [],
): MenuNode[] {
  const normalizedCurrentPath = normalizeMenuPath(currentPath);

  for (const node of nodes) {
    const nextParents = [...parents, node];

    if (node.to && normalizeMenuPath(node.to) === normalizedCurrentPath) {
      return nextParents;
    }

    const matched = findMenuNodePath(node.children, currentPath, nextParents);
    if (matched.length > 0) {
      return matched;
    }
  }

  return [];
}

function renderMenuNodeIcon(node: MenuNode) {
  const iconUrl = resolveResourceIconUrl(node.icon);
  if (iconUrl) {
    return [
      h("img", {
        src: iconUrl,
        alt: node.name,
      }),
    ];
  }
  if (node.type === "menu") {
    return [
      h(
        "svg",
        {
          class: "default-menu-icon",
          viewBox: "0 -960 960 960",
          "aria-hidden": "true",
        },
        [h("path", { d: DEFAULT_MENU_ICON_PATH })],
      ),
    ];
  }
  return [
    h(
      "svg",
      {
        viewBox: "0 -960 960 960",
        class: "default-directory-icon",
        "aria-hidden": "true",
      },
      [h("path", { d: DEFAULT_DIRECTORY_ICON_PATH })],
    ),
  ];
}

const AdminMenuNode: ReturnType<typeof defineComponent> = defineComponent({
  name: "AdminMenuNode",
  props: {
    node: {
      type: Object as () => MenuNode,
      required: true,
    },
    level: {
      type: Number,
      required: true,
    },
  },
  setup(props) {
    return (): ReturnType<typeof h> => {
      const node = props.node;
      const hasChildren = node.children.length > 0;
      const expanded = isMenuNodeExpanded(node.id);
      const active = isMenuNodeActive(node);
      return h("div", { class: "nav-node" }, [
        h(
          "button",
          {
            class: [
              "nav-item",
              node.type === "directory" ? "nav-directory" : "nav-menu",
              active ? "active" : "",
            ],
            type: "button",
            title: node.name,
            style: {
              paddingLeft: sidebarCollapsed.value ? undefined : `${12 + props.level * 16}px`,
            },
            onClick: () => handleMenuNodeClick(node),
          },
          [
            h(
              "span",
              { class: ["nav-icon", node.type === "menu" ? "nav-icon-child" : ""] },
              renderMenuNodeIcon(node),
            ),
            !sidebarCollapsed.value ? h("span", { class: "nav-label" }, node.name) : null,
            !sidebarCollapsed.value && hasChildren
              ? h("span", { class: ["nav-caret", expanded ? "expanded" : ""] }, [
                  h("svg", { viewBox: "0 0 24 24", "aria-hidden": "true" }, [
                    h("path", {
                      d: "m9 6 6 6-6 6",
                      fill: "none",
                      stroke: "currentColor",
                      "stroke-linecap": "round",
                      "stroke-linejoin": "round",
                      "stroke-width": "1.8",
                    }),
                  ]),
                ])
              : null,
          ],
        ),
        !sidebarCollapsed.value && hasChildren
          ? h(Transition, { name: "nav-collapse" }, (): ReturnType<typeof h> | null =>
              expanded
                ? h(
                    "div",
                    { class: "nav-children" },
                    node.children.map((child) =>
                      h(AdminMenuNode, {
                        key: child.id,
                        node: child,
                        level: props.level + 1,
                      }),
                    ),
                  )
                : null,
            )
          : null,
      ]);
    };
  },
});

function navigate(target: string): void {
  if (!target || target === route.path) {
    return;
  }
  void router.push(target);
}

function ensureVisitedTab(): void {
  if (route.name === "admin-login" || route.name === "not-found") {
    return;
  }

  const nextTab: VisitedTab = {
    title: resolveRouteTitle(route),
    fullPath: route.fullPath,
    pinned: route.path === "/",
  };

  const existingIndex = visitedTabs.value.findIndex((item) => item.fullPath === nextTab.fullPath);
  if (existingIndex >= 0) {
    visitedTabs.value[existingIndex] = nextTab;
    return;
  }

  visitedTabs.value = [...visitedTabs.value, nextTab];
}

function closeTab(tab: VisitedTab): void {
  if (tab.pinned) {
    return;
  }

  const currentIndex = visitedTabs.value.findIndex((item) => item.fullPath === tab.fullPath);
  visitedTabs.value = visitedTabs.value.filter((item) => item.fullPath !== tab.fullPath);

  if (route.fullPath !== tab.fullPath) {
    return;
  }

  const fallback =
    visitedTabs.value[currentIndex - 1] || visitedTabs.value[currentIndex] || visitedTabs.value[0];
  void router.push(fallback?.fullPath || "/");
}

function refreshCurrentPage(): void {
  if (typeof window !== "undefined") {
    window.location.reload();
  }
}

function updateTabScrollState(): void {
  const container = tabsScrollRef.value;
  if (!container) {
    showTabScrollControls.value = false;
    canScrollTabsLeft.value = false;
    canScrollTabsRight.value = false;
    return;
  }

  const maxScroll = Math.max(0, container.scrollWidth - container.clientWidth);
  showTabScrollControls.value = maxScroll > 4;
  canScrollTabsLeft.value = container.scrollLeft > 4;
  canScrollTabsRight.value = container.scrollLeft < maxScroll - 4;
}

function scrollTabs(direction: "left" | "right"): void {
  const container = tabsScrollRef.value;
  if (!container) {
    return;
  }
  const delta = direction === "left" ? -240 : 240;
  container.scrollBy({ left: delta, behavior: "smooth" });
  window.setTimeout(updateTabScrollState, 260);
}

function scrollActiveTabIntoView(): void {
  const container = tabsScrollRef.value;
  if (!container) {
    return;
  }
  const activeTab = container.querySelector<HTMLElement>(".tab-item.active");
  activeTab?.scrollIntoView({ behavior: "smooth", inline: "nearest", block: "nearest" });
}

async function toggleNotificationPopover(): Promise<void> {
  if (!notificationOpen.value) {
    await ensureUnreadLoaded();
  }
  notificationOpen.value = !notificationOpen.value;
  if (notificationOpen.value) {
    userOpen.value = false;
  }
}

function toggleUserPopover(): void {
  userOpen.value = !userOpen.value;
  if (userOpen.value) {
    notificationOpen.value = false;
  }
}

async function onMarkAllRead(): Promise<void> {
  await markAllRead();
}

async function openNotificationDetail(item: NotificationItem): Promise<void> {
  selectedMessage.value = item;
  if (!item.read) {
    await markRead(item.id);
  }
  notificationOpen.value = false;
  messageCenterOpen.value = true;
}

function openMessageCenter(): void {
  selectedMessage.value = previewList.value[0] ?? null;
  notificationOpen.value = false;
  messageCenterOpen.value = true;
}

function goProfile(): void {
  userOpen.value = false;
  void router.push("/profile");
}

function goChangePassword(): void {
  userOpen.value = false;
  void router.push("/profile/password");
}

function goPreferences(): void {
  userOpen.value = false;
  void router.push("/profile/preferences");
}

function goHelp(): void {
  userOpen.value = false;
  void router.push("/help");
}

async function onLogout(): Promise<void> {
  userOpen.value = false;
  notificationOpen.value = false;
  await logoutAction();
  message.success("已退出登录");
  await router.push("/login");
}

function notificationAvatar(title: string): string {
  const text = title.trim();
  return text ? text.slice(0, 2) : "消息";
}

function showFeatureTip(name: string): void {
  message.info(`${name}暂未接入`);
}

async function toggleFullscreen(): Promise<void> {
  if (typeof document === "undefined") {
    return;
  }
  if (document.fullscreenElement) {
    await document.exitFullscreen();
    return;
  }
  await document.documentElement.requestFullscreen();
}

function handleDocumentPointerDown(event: PointerEvent): void {
  const target = event.target;
  if (!(target instanceof Node)) {
    return;
  }

  if (notificationOpen.value && notifyRootRef.value && !notifyRootRef.value.contains(target)) {
    notificationOpen.value = false;
  }

  if (userOpen.value && userRootRef.value && !userRootRef.value.contains(target)) {
    userOpen.value = false;
  }
}

function handleDocumentKeyDown(event: KeyboardEvent): void {
  if (event.key === "Escape") {
    notificationOpen.value = false;
    userOpen.value = false;
    return;
  }
  if (event.altKey && event.key.toLowerCase() === "q") {
    event.preventDefault();
    void onLogout();
    return;
  }
  if (event.altKey && event.key.toLowerCase() === "l") {
    event.preventDefault();
    showFeatureTip("锁定屏幕");
  }
}
</script>

<style scoped>
.admin-shell {
  --sidebar-width: 224px;
  --sidebar-collapsed-width: 72px;
  --header-height: 50px;
  --tab-height: 34px;
  --border-color: #e2e8f0;
  --surface-color: rgba(255, 255, 255, 0.98);
  --surface-soft: #f8fafc;
  --surface-hover: #eff6ff;
  --text-color: #0f172a;
  --muted-color: #64748b;
  --primary-color: #3b82f6;
  --shadow-color: rgba(15, 23, 42, 0.08);
  min-height: 100vh;
  width: 100%;
  max-width: 100vw;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.06), transparent 24%),
    linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  color: var(--text-color);
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  overflow: hidden;
}

.admin-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 220;
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--surface-color);
  border-right: 1px solid var(--border-color);
  backdrop-filter: blur(18px);
  transition: width 0.2s ease;
}

.admin-shell.collapsed .admin-sidebar {
  width: var(--sidebar-collapsed-width);
}

.brand {
  flex: 0 0 49px;
  height: 49px;
  border-bottom: 1px solid var(--border-color);
}

.brand-link {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 100%;
  padding: 0 14px;
  color: var(--text-color);
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
}

.brand-logo {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.brand-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.brand-title {
  overflow: hidden;
  font-size: 1.125rem;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.admin-nav {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
}

.admin-nav :deep(.nav-node) {
  display: grid;
  gap: 6px;
}

.admin-nav :deep(.nav-directory),
.admin-nav :deep(.nav-menu) {
  position: relative;
}

.admin-nav :deep(.nav-item) {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  color: #334155;
  background: transparent;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.admin-shell.collapsed .admin-nav :deep(.nav-item) {
  justify-content: center;
  padding: 0;
}

.admin-nav :deep(.nav-item:hover) {
  color: var(--text-color);
  background: var(--surface-soft);
}

.admin-nav :deep(.nav-item.active) {
  color: #2563eb;
  background: #dbeafe;
  box-shadow: inset 0 0 0 1px #bfdbfe;
}

.admin-nav :deep(.nav-icon) {
  display: inline-flex;
  flex: 0 0 18px;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: #64748b;
}

.admin-nav :deep(.nav-item.active .nav-icon) {
  color: #2563eb;
}

.admin-nav :deep(.nav-icon svg),
.admin-nav :deep(.nav-icon img),
.admin-nav :deep(.nav-caret svg) {
  width: 16px;
  height: 16px;
}

.admin-nav :deep(.nav-icon img) {
  display: block;
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.admin-nav :deep(.nav-icon-child) {
  opacity: 0.72;
}

.admin-nav :deep(.nav-label) {
  flex: 1;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.2;
  white-space: nowrap;
  text-align: left;
  text-overflow: ellipsis;
}

.admin-nav :deep(.nav-caret) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  transition: transform 0.18s ease;
}

.admin-nav :deep(.nav-caret.expanded) {
  transform: rotate(90deg);
}

.admin-nav :deep(.nav-children) {
  display: grid;
  gap: 6px;
  padding-left: 0;
}

.admin-nav :deep(.default-menu-icon) {
  fill: currentColor;
}

.admin-nav :deep(.default-directory-icon) {
  fill: currentColor;
}

.admin-nav :deep(.nav-collapse-enter-active),
.admin-nav :deep(.nav-collapse-leave-active) {
  overflow: hidden;
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.admin-nav :deep(.nav-collapse-enter-from),
.admin-nav :deep(.nav-collapse-leave-to) {
  opacity: 0;
  transform: translateY(-4px);
}

.admin-main {
  margin-left: var(--sidebar-width);
  width: calc(100vw - var(--sidebar-width));
  max-width: calc(100vw - var(--sidebar-width));
  min-width: 0;
  transition:
    margin-left 0.2s ease,
    width 0.2s ease;
}

.admin-shell.collapsed .admin-main {
  margin-left: var(--sidebar-collapsed-width);
  width: calc(100vw - var(--sidebar-collapsed-width));
  max-width: calc(100vw - var(--sidebar-collapsed-width));
}

.admin-fixed-header {
  position: fixed;
  top: 0;
  left: var(--sidebar-width);
  z-index: 200;
  width: calc(100vw - var(--sidebar-width));
  max-width: calc(100vw - var(--sidebar-width));
  height: calc(var(--header-height) + var(--tab-height));
  transition:
    left 0.2s ease,
    width 0.2s ease;
}

.admin-shell.collapsed .admin-fixed-header {
  left: var(--sidebar-collapsed-width);
  width: calc(100vw - var(--sidebar-collapsed-width));
  max-width: calc(100vw - var(--sidebar-collapsed-width));
}

.topbar,
.tabs-row {
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  background: var(--surface-color);
  border-bottom: 1px solid var(--border-color);
  backdrop-filter: blur(18px);
}

.topbar {
  position: relative;
  z-index: 2;
  height: var(--header-height);
  padding: 0 10px 0 8px;
}

.tabs-row {
  position: relative;
  z-index: 1;
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  min-width: 0;
}

.topbar-left {
  gap: 6px;
}

.topbar-right {
  flex: 1;
  justify-content: flex-end;
  gap: 6px;
}

.topbar-icon,
.tabbar-tool,
.panel-icon,
.tabs-nav {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: #475569;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    opacity 0.18s ease;
}

.topbar-icon.round {
  border-radius: 999px;
}

.topbar-icon:hover,
.tabbar-tool:hover,
.panel-icon:hover,
.tabs-nav:hover {
  color: #2563eb;
  background: var(--surface-hover);
}

.topbar-icon svg,
.tabbar-tool svg,
.panel-icon svg,
.tabs-nav svg,
.breadcrumb-icon svg,
.tab-icon svg,
.search-trigger svg,
.tab-close svg,
.tab-pin svg,
.user-menu-icon svg {
  width: 16px;
  height: 16px;
}

.breadcrumb {
  min-width: 0;
  margin-left: 6px;
}

.breadcrumb ol {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.breadcrumb-entry {
  display: flex;
  align-items: center;
}

.breadcrumb-link,
.breadcrumb-current {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 14px;
}

.breadcrumb-link {
  color: var(--muted-color);
  background: transparent;
  border: none;
  cursor: pointer;
}

.breadcrumb-link:hover {
  color: var(--text-color);
}

.breadcrumb-current {
  color: var(--text-color);
  font-weight: 600;
}

.breadcrumb-separator {
  display: inline-flex;
  align-items: center;
  color: #94a3b8;
}

.breadcrumb-separator svg {
  width: 14px;
  height: 14px;
}

.search-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  color: var(--muted-color);
  background: var(--surface-soft);
  border: none;
  border-radius: 999px;
  cursor: pointer;
}

.search-trigger:hover {
  color: var(--text-color);
  background: var(--surface-hover);
}

.search-trigger kbd,
.user-menu kbd {
  padding: 2px 6px;
  color: var(--muted-color);
  background: #ffffff;
  border: 1px solid #dbe1ea;
  border-radius: 999px;
  font: inherit;
  font-size: 12px;
}

.topbar-popover-host {
  position: relative;
  z-index: 4;
}

.notify-dot,
.notification-unread,
.avatar-status {
  position: absolute;
  border-radius: 999px;
}

.notify-dot {
  top: 5px;
  right: 5px;
  width: 7px;
  height: 7px;
  background: var(--primary-color);
}

.avatar-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
}

.avatar-face,
.avatar-large {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: linear-gradient(135deg, #f59e0b 0%, #fb7185 55%, #60a5fa 100%);
  border-radius: 999px;
  font-weight: 700;
}

.avatar-face {
  width: 32px;
  height: 32px;
  font-size: 13px;
}

.avatar-large {
  width: 54px;
  height: 54px;
  font-size: 18px;
}

.avatar-status {
  right: 5px;
  bottom: 7px;
  width: 10px;
  height: 10px;
  background: #22c55e;
  border: 2px solid #ffffff;
}

.notification-panel,
.user-panel {
  position: absolute;
  right: 0;
  margin-top: 10px;
  z-index: 8;
  background: var(--surface-color);
  border: 1px solid var(--border-color);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18px);
}

.notification-panel {
  width: 360px;
  border-radius: 16px;
}

.user-panel {
  width: 280px;
  border-radius: 16px;
}

.panel-header,
.panel-footer,
.user-card,
.user-menu button {
  display: flex;
  align-items: center;
}

.panel-header,
.panel-footer {
  justify-content: space-between;
  padding: 14px 16px;
}

.panel-header {
  border-bottom: 1px solid var(--border-color);
}

.mark-read {
  width: calc(100% - 32px);
  height: 36px;
  margin: 12px 16px 0;
  color: #334155;
  background: var(--surface-soft);
  border: none;
  border-radius: 10px;
  cursor: pointer;
}

.notification-list {
  max-height: 360px;
  padding: 12px 16px 8px;
  overflow-y: auto;
}

.notification-item {
  position: relative;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 12px;
  width: 100%;
  padding: 10px 4px;
  color: inherit;
  text-align: left;
  background: transparent;
  border: none;
  border-radius: 12px;
  cursor: pointer;
}

.notification-item:hover {
  background: var(--surface-soft);
}

.notification-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: #ffffff;
  background: linear-gradient(135deg, #84cc16 0%, #22d3ee 100%);
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
}

.notification-body {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.notification-body strong,
.notification-body span,
.notification-body small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-body span,
.notification-body small,
.notification-empty,
.user-meta span {
  color: var(--muted-color);
}

.notification-unread {
  top: 18px;
  right: 10px;
  width: 8px;
  height: 8px;
  background: var(--primary-color);
}

.notification-empty {
  padding: 24px 0;
  text-align: center;
}

.panel-footer {
  border-top: 1px solid var(--border-color);
}

.panel-footer button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 14px;
  color: #475569;
  background: transparent;
  border: none;
  border-radius: 10px;
  cursor: pointer;
}

.panel-footer button:hover {
  background: var(--surface-soft);
}

.panel-footer .primary {
  color: #ffffff;
  background: var(--primary-color);
}

.user-card {
  gap: 12px;
  padding: 18px 18px 16px;
  border-bottom: 1px solid var(--border-color);
}

.user-meta {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.user-meta strong,
.user-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-menu {
  padding: 10px 8px;
}

.user-menu-secondary {
  border-top: 1px solid var(--border-color);
}

.user-menu button {
  gap: 10px;
  width: 100%;
  height: 42px;
  padding: 0 10px;
  color: #334155;
  background: transparent;
  border: none;
  border-radius: 10px;
  cursor: pointer;
}

.user-menu button:hover {
  background: var(--surface-soft);
}

.user-menu-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  color: #475569;
}

.user-menu button span:last-of-type {
  flex: 1;
  text-align: left;
}

.tabs-row {
  height: var(--tab-height);
  padding: 0;
}

.tabs-nav {
  flex: 0 0 28px;
  width: 28px;
  height: 34px;
  border-radius: 0;
  border-right: 1px solid var(--border-color);
}

.tabs-nav + .tabs-scroll {
  width: calc(100% - 28px);
}

.tabs-nav.disabled {
  color: #cbd5e1;
  cursor: default;
}

.tabs-nav.disabled:hover {
  background: transparent;
}

.tabs-scroll {
  flex: 1;
  min-width: 0;
  height: 34px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.tabs-scroll::-webkit-scrollbar {
  display: none;
}

.tabs-list {
  display: inline-flex;
  align-items: stretch;
  min-width: 100%;
  height: 34px;
  padding: 0 8px;
  box-sizing: border-box;
}

.tab-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  min-width: 0;
  max-width: 220px;
  height: 34px;
  padding: 0 12px;
  color: #475569;
  border-radius: 8px 8px 0 0;
  cursor: pointer;
  user-select: none;
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
}

.tab-item:hover {
  color: var(--text-color);
  background: #f8fafc;
}

.tab-item.active {
  color: #2563eb;
  background: #dbeafe;
}

.tab-item.pinned {
  padding-right: 10px;
}

.tab-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: currentColor;
}

.tab-title-text {
  overflow: hidden;
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.tab-close,
.tab-pin {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  color: inherit;
  background: transparent;
  border: none;
  border-radius: 999px;
}

.tab-close {
  cursor: pointer;
}

.tab-close:hover {
  background: rgba(148, 163, 184, 0.18);
}

.tabs-tools {
  display: flex;
  align-items: center;
  align-self: stretch;
  flex: 0 0 auto;
  border-left: 1px solid var(--border-color);
}

.tabbar-tool {
  width: 32px;
  height: 34px;
  border-radius: 0;
}

.admin-content {
  height: 100vh;
  min-width: 0;
  max-width: 100%;
  padding: calc(var(--header-height) + var(--tab-height) + 14px) 14px 14px;
  overflow-y: auto;
  overflow-x: hidden;
  box-sizing: border-box;
}

.admin-workspace {
  min-width: 0;
  max-width: 100%;
}

@media (max-width: 1200px) {
  .breadcrumb {
    max-width: 300px;
    overflow: hidden;
  }

  .search-trigger span {
    display: none;
  }
}

@media (max-width: 960px) {
  .admin-shell {
    --sidebar-width: 208px;
  }

  .topbar-right {
    gap: 4px;
  }

  .search-trigger kbd,
  .breadcrumb {
    display: none;
  }

  .notification-panel,
  .user-panel {
    right: -8px;
  }
}

@media (max-width: 768px) {
  .admin-shell {
    --sidebar-width: 72px;
  }

  .admin-sidebar {
    width: 72px;
  }

  .admin-main,
  .admin-fixed-header {
    margin-left: 72px;
    left: 72px;
    width: calc(100vw - 72px);
    max-width: calc(100vw - 72px);
  }

  .nav-label,
  .brand-title,
  .nav-caret,
  .nav-children {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 0;
  }

  .admin-content {
    padding-right: 10px;
    padding-left: 10px;
  }
}
</style>
