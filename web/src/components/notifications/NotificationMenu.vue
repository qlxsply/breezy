<!-- /src/components/notifications/NotificationMenu.vue -->
<template>
  <div
    ref="rootRef"
    class="notify"
  >
    <button
      class="icon-btn"
      type="button"
      aria-label="通知"
      title="通知"
      @click="togglePopover"
    >
      <svg
        v-if="!pollingEnabled"
        class="bell-icon"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path
          fill="none"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="1.6"
          d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 0 0-4-5.7V5a2 2 0 1 0-4 0v.3C8 6 6 8.2 6 11v3.2c0 .5-.2 1-.6 1.4L4 17h5m6 0a3 3 0 1 1-6 0h6Z"
        />
        <line
          x1="4"
          y1="4"
          x2="20"
          y2="20"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-width="1.6"
        />
      </svg>
      <svg
        v-else-if="hasUnread"
        class="bell-icon"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path
          fill="currentColor"
          d="M21 19v1H3v-1l2-2v-6c0-3.1 2.03-5.83 5-6.71V4a2 2 0 0 1 2-2a2 2 0 0 1 2 2v.29c2.97.88 5 3.61 5 6.71v6zm-7 2a2 2 0 0 1-2 2a2 2 0 0 1-2-2"
        />
      </svg>
      <svg
        v-else
        class="bell-icon"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path
          fill="none"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="1.6"
          d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 0 0-4-5.7V5a2 2 0 1 0-4 0v.3C8 6 6 8.2 6 11v3.2c0 .5-.2 1-.6 1.4L4 17h5m6 0a3 3 0 1 1-6 0h6Z"
        />
      </svg>
      <span
        v-if="hasUnread"
        class="notify-dot"
      ></span>
    </button>

    <div
      v-if="open"
      class="popover"
    >
      <div class="popover-header">
        <div class="header-left">
          <span>通知</span>
          <button
            class="toggle-btn"
            type="button"
            :title="toggleLabel"
            :aria-pressed="!pollingEnabled"
            aria-label="切换通知拉取"
            @click="togglePolling"
          >
            <svg
              v-if="pollingEnabled"
              viewBox="0 0 16 16"
              aria-hidden="true"
            >
              <path
                fill="currentColor"
                d="M5 3a5 5 0 0 0 0 10h6a5 5 0 0 0 0-10zm6 9a4 4 0 1 1 0-8 4 4 0 0 1 0 8"
              />
            </svg>
            <svg
              v-else
              viewBox="0 0 16 16"
              aria-hidden="true"
            >
              <path
                fill="currentColor"
                d="M11 4a4 4 0 0 1 0 8H8a5 5 0 0 0 2-4 5 5 0 0 0-2-4zm-6 8a4 4 0 1 1 0-8 4 4 0 0 1 0 8M0 8a5 5 0 0 0 5 5h6a5 5 0 0 0 0-10H5a5 5 0 0 0-5 5"
              />
            </svg>
          </button>
        </div>
        <button
          class="link"
          @click="onMarkAllRead"
        >
          全部已读
        </button>
      </div>

      <div class="push-status-row">
        <div class="push-status-text">
          <span>{{ pushPermissionText }}</span>
          <span
            v-if="pushLastError"
            class="push-error"
            >{{ pushLastError }}</span
          >
        </div>
        <button
          class="push-enable-btn"
          :disabled="pushActionDisabled"
          @click="onEnablePush"
        >
          {{ pushActionLabel }}
        </button>
      </div>

      <div class="push-health-panel">
        <div class="push-health-header">
          <span>推送健康检查</span>
          <div class="push-health-actions">
            <button
              class="push-health-btn"
              :disabled="healthLoading"
              @click="refreshPushHealth"
            >
              {{ healthLoading ? "刷新中..." : "刷新" }}
            </button>
            <button
              class="push-health-btn"
              :disabled="healthTesting"
              @click="sendPushTest"
            >
              {{ healthTesting ? "发送中..." : "发送高优先级测试" }}
            </button>
          </div>
        </div>
        <div class="push-health-grid">
          <span>浏览器安全上下文</span><span>{{ secureContextText }}</span>
          <span>Service Worker Scope</span><span>{{ swScopeText }}</span> <span>VAPID 状态</span
          ><span>{{ vapidStatusText }}</span> <span>活跃/失效订阅</span
          ><span>{{ subscriptionCountText }}</span> <span>最近订阅更新时间</span
          ><span>{{ formatNullableTime(pushHealth?.latestSubscriptionUpdatedAt) }}</span>
          <span>最近推送成功时间</span
          ><span>{{ formatNullableTime(pushHealth?.latestSubscriptionPushAt) }}</span>
          <span>最近订阅错误</span><span>{{ pushHealth?.latestSubscriptionError || "无" }}</span>
          <span>最近投递状态</span><span>{{ latestDeliveryText }}</span>
        </div>
      </div>

      <div
        v-loading="loading"
        class="list"
      >
        <bz-empty
          v-if="!loading && previewList.length === 0"
          description="暂无未读消息"
        />

        <div
          v-for="item in previewList"
          :key="item.id"
          class="item"
          :class="{ highlight: highlightedId === item.id }"
          @click="openDetail(item)"
        >
          <div class="item-title">
            <span class="dot" />
            <span :class="['priority-dot', priorityClass(item.priority)]" />
            <span class="text">{{ item.title }}</span>
          </div>
          <div class="meta">{{ formatDateTime(item.createdAt) }}</div>
        </div>
      </div>

      <div class="popover-footer">
        <button
          class="footer-btn"
          @click="openCenter"
        >
          {{ footerLabel }}
        </button>
      </div>
    </div>

    <MessagesModal
      :open="modalOpen"
      :initial-message="selectedMessage"
      @close="closeCenter"
    />
  </div>
</template>

<script setup lang="ts">
// <script setup> + TS：顶层即 setup()。
import { computed, onMounted, onUnmounted, ref, watch } from "vue";

import { getPushHealth, type PushHealthRes, sendPushHealthTest } from "../../api/push";
import {
  ensureUnreadLoaded,
  markAllRead,
  toggleNotificationPolling,
  useNotificationAttentionNotificationId,
  useNotificationAttentionSignal,
  useNotificationPollingEnabled,
  useUnreadCount,
  useUnreadList,
} from "../../registry/notifications.registry";
import {
  enableTodoReminderPushFromUserGesture,
  useTodoReminderPermission,
  useTodoReminderPushLastError,
  useTodoReminderPushReady,
  useTodoReminderPushSyncing,
  useTodoReminderSecureContext,
  useTodoReminderServiceWorkerScope,
} from "../../registry/todo-reminder.registry";
import type { NotificationItem, NotificationPriority } from "../../types/notification";
import { getAuthToken } from "../../utils/authStorage";
import { formatDateTime } from "../../utils/formatter";
import { message } from "../../utils/message";
import MessagesModal from "./MessagesModal.vue";

const previewLimit = 6;

const unreadCount = useUnreadCount();
const unreadList = useUnreadList();

const open = ref(false);
const loading = ref(false);
const modalOpen = ref(false);
const selectedMessage = ref<NotificationItem | null>(null);
const rootRef = ref<HTMLElement | null>(null);
const pollingEnabled = useNotificationPollingEnabled();
const attentionSignal = useNotificationAttentionSignal();
const attentionNotificationId = useNotificationAttentionNotificationId();
const reminderPermission = useTodoReminderPermission();
const pushReady = useTodoReminderPushReady();
const pushSyncing = useTodoReminderPushSyncing();
const pushLastError = useTodoReminderPushLastError();
const secureContext = useTodoReminderSecureContext();
const serviceWorkerScope = useTodoReminderServiceWorkerScope();
const pushHealth = ref<PushHealthRes | null>(null);
const healthLoading = ref(false);
const healthTesting = ref(false);
const highlightedId = ref("");
let clearHighlightTimer: ReturnType<typeof setTimeout> | null = null;

const previewList = computed(() => unreadList.value.slice(0, previewLimit));
const remaining = computed(() => Math.max(unreadCount.value - previewList.value.length, 0));
const hasUnread = computed(() => unreadCount.value > 0);
const footerLabel = computed(() => {
  if (remaining.value > 0) return `还有 ${remaining.value} 条未读`;
  return "查询全部消息";
});
const toggleLabel = computed(() => (pollingEnabled.value ? "关闭通知" : "开启通知"));

const pushPermissionText = computed(() => {
  if (reminderPermission.value === "unsupported") {
    return "当前浏览器不支持系统通知";
  }
  if (reminderPermission.value === "denied") {
    return "通知权限已拒绝，请在浏览器设置中开启";
  }
  if (reminderPermission.value === "granted") {
    return pushReady.value
      ? "系统通知已授权，Web Push 订阅已就绪"
      : "系统通知已授权，等待完成 Web Push 订阅";
  }
  return "系统通知未授权";
});

const pushActionLabel = computed(() => {
  if (pushSyncing.value) {
    return "同步中...";
  }
  if (reminderPermission.value === "unsupported") {
    return "不可用";
  }
  if (reminderPermission.value === "granted") {
    return pushReady.value ? "重新同步订阅" : "完成推送订阅";
  }
  return "启用系统通知";
});

const pushActionDisabled = computed(() => {
  if (pushSyncing.value) {
    return true;
  }
  return reminderPermission.value === "unsupported";
});

const secureContextText = computed(() => (secureContext.value ? "是" : "否"));

const swScopeText = computed(() => serviceWorkerScope.value || "未注册");

const vapidStatusText = computed(() => {
  if (!pushHealth.value) {
    return "未加载";
  }
  return pushHealth.value.vapidReady ? "可用" : "未就绪";
});

const subscriptionCountText = computed(() => {
  if (!pushHealth.value) {
    return "未加载";
  }
  return `${pushHealth.value.activeSubscriptionCount} / ${pushHealth.value.inactiveSubscriptionCount}`;
});

const latestDeliveryText = computed(() => {
  if (!pushHealth.value || !pushHealth.value.latestDelivery) {
    return "暂无";
  }
  const latestDelivery = pushHealth.value.latestDelivery;
  return `${latestDelivery.status} (${latestDelivery.priority}/${latestDelivery.msgType})`;
});

async function togglePopover() {
  open.value = !open.value;
  if (open.value) {
    await Promise.all([loadUnread(), refreshPushHealth()]);
  }
}

async function loadUnread() {
  loading.value = true;
  try {
    await ensureUnreadLoaded(false);
  } finally {
    loading.value = false;
  }
}

async function onMarkAllRead() {
  await markAllRead();
}

function togglePolling() {
  toggleNotificationPolling();
}

async function onEnablePush() {
  if (reminderPermission.value === "unsupported") {
    message.warning("当前浏览器环境不支持系统通知");
    return;
  }

  if (reminderPermission.value === "denied") {
    message.warning("通知权限已拒绝，请在浏览器设置中手动开启后再重试");
    console.warn("[web-push] enable action blocked because permission is denied");
    return;
  }

  console.info("[web-push] user clicked enable push action");
  const enabled = await enableTodoReminderPushFromUserGesture();
  if (enabled) {
    message.success("系统通知与 Web Push 订阅已启用");
    await refreshPushHealth();
    return;
  }

  if (reminderPermission.value === "default") {
    message.info("请在浏览器权限弹窗中选择允许通知");
    return;
  }

  message.warning(pushLastError.value || "推送订阅未完成，请稍后重试");
}

async function refreshPushHealth() {
  if (!getAuthToken()) {
    pushHealth.value = null;
    return;
  }

  healthLoading.value = true;
  try {
    console.info("[web-push] loading push health info...");
    pushHealth.value = await getPushHealth();
    console.info("[web-push] push health loaded", {
      activeSubscriptionCount: pushHealth.value.activeSubscriptionCount,
      inactiveSubscriptionCount: pushHealth.value.inactiveSubscriptionCount,
      latestDeliveryStatus: pushHealth.value.latestDelivery?.status || "",
    });
  } catch (error) {
    console.warn("[web-push] load push health failed", error);
  } finally {
    healthLoading.value = false;
  }
}

async function sendPushTest() {
  healthTesting.value = true;
  try {
    console.info("[web-push] sending high priority push health test...");
    const result = await sendPushHealthTest();
    console.info("[web-push] push health test sent", {
      deliveryId: result.delivery?.id || "",
      status: result.delivery?.status || "",
    });
    message.success(result.message || "健康检查测试消息已发送");
  } catch (error) {
    console.warn("[web-push] push health test failed", error);
  } finally {
    healthTesting.value = false;
    await refreshPushHealth();
  }
}

function formatNullableTime(value: string | null | undefined): string {
  if (!value) {
    return "暂无";
  }
  return formatDateTime(value);
}

function openCenter() {
  modalOpen.value = true;
  selectedMessage.value = null;
  open.value = false;
}

function openDetail(item: NotificationItem) {
  modalOpen.value = true;
  selectedMessage.value = item;
  open.value = false;
}

function closeCenter() {
  modalOpen.value = false;
  selectedMessage.value = null;
}

function onClickOutside(e: MouseEvent) {
  if (!open.value) return;
  const root = rootRef.value;
  if (!root) return;
  if (!root.contains(e.target as Node)) open.value = false;
}

function onKeyDown(e: KeyboardEvent) {
  if (e.key === "Escape") open.value = false;
}

function priorityClass(priority: NotificationPriority): string {
  const value = String(priority || "LOW").toUpperCase();
  if (value === "HIGH") return "high";
  if (value === "MEDIUM") return "medium";
  return "low";
}

watch(attentionSignal, async () => {
  const targetId = attentionNotificationId.value;
  if (!targetId) {
    return;
  }
  highlightedId.value = targetId;
  open.value = true;
  await loadUnread();
  if (clearHighlightTimer) {
    window.clearTimeout(clearHighlightTimer);
  }
  clearHighlightTimer = window.setTimeout(() => {
    highlightedId.value = "";
    clearHighlightTimer = null;
  }, 6000);
});

onMounted(() => {
  void ensureUnreadLoaded(false);
  void refreshPushHealth();
  document.addEventListener("mousedown", onClickOutside);
  window.addEventListener("keydown", onKeyDown);
});

onUnmounted(() => {
  document.removeEventListener("mousedown", onClickOutside);
  window.removeEventListener("keydown", onKeyDown);
  if (clearHighlightTimer) {
    window.clearTimeout(clearHighlightTimer);
    clearHighlightTimer = null;
  }
});
</script>

<style scoped>
.notify {
  position: relative;
  z-index: 4;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  width: 44px;
  height: 44px;
  padding: 0;
  color: var(--text-main);
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  appearance: none;
  -webkit-appearance: none;
  cursor: pointer;
}

.icon-btn:focus {
  outline: none;
}

.icon-btn:focus-visible {
  outline: none;
}

.bell-icon {
  width: 20px;
  height: 20px;
  color: var(--text-main);
  transition: transform 0.18s ease;
}

.icon-btn:hover .bell-icon {
  transform: scale(1.12);
}

.notify-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 7px;
  height: 7px;
  background: #ef4444;
  border-radius: 999px;
}

.popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 360px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18px);
  overflow: hidden;
  z-index: 8;
}

.popover-header {
  padding: 12px 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 800;
  border-bottom: 1px solid #f1f5f9;
}

.push-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

.push-health-panel {
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;
  display: grid;
  gap: 8px;
}

.push-health-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--text-main);
  font-weight: 700;
  font-size: 12px;
}

.push-health-actions {
  display: inline-flex;
  gap: 6px;
}

.push-health-btn {
  border: 1px solid #dbeafe;
  background: #f8fbff;
  color: #1e40af;
  border-radius: 8px;
  padding: 4px 8px;
  font-size: 12px;
  cursor: pointer;
}

.push-health-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.push-health-grid {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 4px 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.push-status-text {
  display: grid;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.3;
}

.push-error {
  color: #dc2626;
}

.push-enable-btn {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 8px;
  padding: 6px 10px;
  cursor: pointer;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 700;
}

.push-enable-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.header-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.toggle-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.toggle-btn svg {
  width: 18px;
  height: 18px;
}

.link {
  border: none;
  background: transparent;
  color: var(--primary-color);
  cursor: pointer;
  font-weight: 700;
}

.list {
  max-height: 320px;
  overflow-y: auto;
  padding: 8px 10px;
  display: grid;
  gap: 6px;
}

.item {
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  display: grid;
  gap: 6px;
}

.item:hover {
  background: #f8fafc;
}

.item.highlight {
  background: #eff6ff;
  box-shadow: inset 0 0 0 1px #bfdbfe;
}

.item-title {
  display: flex;
  gap: 6px;
  align-items: center;
  font-weight: 700;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ef4444;
}

.priority-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.priority-dot.low {
  background: #94a3b8;
}

.priority-dot.medium {
  background: #f59e0b;
}

.priority-dot.high {
  background: #ef4444;
}

.text {
  flex: 1;
}

.meta {
  font-size: 11px;
  color: var(--text-muted);
}

.popover-footer {
  border-top: 1px solid #f1f5f9;
  padding: 10px;
  display: flex;
  justify-content: center;
}

.footer-btn {
  border: none;
  background: transparent;
  color: var(--primary-color);
  font-weight: 700;
  cursor: pointer;
}
</style>
