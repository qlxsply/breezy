<!-- /src/components/notifications/MessagesModal.vue -->
<template>
  <bz-dialog
    :model-value="open"
    title="消息中心"
    width="1100px"
    top="8vh"
    :close-on-click-modal="false"
    @close="$emit('close')"
  >
    <div class="tabs">
      <bz-radio-group
        v-model="activeTab"
        size="small"
      >
        <bz-radio-button label="all">全部</bz-radio-button>
        <bz-radio-button label="unread">未读</bz-radio-button>
      </bz-radio-group>
    </div>

    <div class="body">
      <div
        ref="listRef"
        v-loading="loading && messages.length === 0"
        class="list"
        @scroll="onScroll"
      >
        <bz-empty
          v-if="!loading && messages.length === 0"
          description="暂无消息"
        />

        <div
          v-for="msg in messages"
          :key="msg.id"
          class="item"
          :class="{ read: msg.read, selected: selected?.id === msg.id }"
          @click="selectMessage(msg)"
        >
          <div class="item-title">
            <span
              v-if="!msg.read"
              class="dot"
            />
            <span :class="['priority-dot', priorityClass(msg.priority)]" />
            <span class="text">{{ msg.title }}</span>
          </div>
          <div class="item-meta">{{ formatDateTime(msg.createdAt) }}</div>
        </div>

        <bz-text
          v-if="loading && messages.length > 0"
          type="info"
          class="hint"
          >加载中…</bz-text
        >
        <bz-text
          v-if="!loading && !hasMore && messages.length > 0"
          type="info"
          class="hint"
          >没有更多了</bz-text
        >
      </div>

      <NotificationDetailDrawer
        :open="Boolean(selected)"
        :item="selected"
        @close="selected = null"
      />
    </div>
  </bz-dialog>
</template>

<script setup lang="ts">
// <script setup> + TS：顶层即 setup()。
import { computed, ref, watch } from "vue";

import { listNotifications } from "../../api/notifications";
import { markRead } from "../../registry/notifications.registry";
import type { NotificationItem, NotificationPriority } from "../../types/notification";
import type { PageResult } from "../../types/page";
import { formatDateTime } from "../../utils/formatter";
import NotificationDetailDrawer from "./NotificationDetailDrawer.vue";

const props = defineProps<{
  open: boolean;
  initialMessage?: NotificationItem | null;
}>();

defineEmits<{
  (e: "close"): void;
}>();

const activeTab = ref<"all" | "unread">("all");
const messages = ref<NotificationItem[]>([]);
const loading = ref(false);
const page = ref(1);
const hasMore = ref(true);
const selected = ref<NotificationItem | null>(null);
const listRef = ref<HTMLElement | null>(null);
const pageSize = 20;

const queryStatus = computed(() => (activeTab.value === "unread" ? "unread" : "all"));

watch(
  () => props.open,
  (open) => {
    if (!open) return;
    selected.value = props.initialMessage ?? null;
    if (selected.value && !selected.value.read) {
      void markRead(selected.value.id);
    }
    resetAndLoad();
  },
);

watch(
  () => activeTab.value,
  () => {
    if (!props.open) return;
    resetAndLoad();
  },
);

async function resetAndLoad() {
  messages.value = [];
  page.value = 1;
  hasMore.value = true;
  await loadMore();
}

async function loadMore() {
  if (loading.value || !hasMore.value) return;
  loading.value = true;
  try {
    const res = await listNotifications({
      status: queryStatus.value,
      page: page.value,
      size: pageSize,
    });
    const { items, hasMore: moreFlag } = normalizePage(res);
    if (page.value === 1) messages.value = items;
    else messages.value = [...messages.value, ...items];
    if (moreFlag !== undefined) {
      hasMore.value = moreFlag;
    } else {
      hasMore.value = items.length >= pageSize;
    }
    if (items.length > 0) page.value += 1;
  } finally {
    loading.value = false;
  }
}

function normalizePage(
  payload:
    | NotificationItem[]
    | { items?: NotificationItem[]; hasMore?: boolean }
    | PageResult<NotificationItem>,
) {
  if (Array.isArray(payload)) return { items: payload, hasMore: undefined };
  if (payload && typeof payload === "object" && "elements" in payload) {
    const items = Array.isArray(payload.elements) ? payload.elements : [];
    const hasMore = payload.pageNo < payload.totalPages;
    return { items, hasMore };
  }
  const items = Array.isArray(payload.items) ? payload.items : [];
  return { items, hasMore: payload.hasMore };
}

async function selectMessage(msg: NotificationItem) {
  selected.value = msg;
  if (!msg.read) {
    await markRead(msg.id);
    msg.read = true;
  }
}

function onScroll() {
  const el = listRef.value;
  if (!el || loading.value || !hasMore.value) return;
  const threshold = 80;
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - threshold) {
    void loadMore();
  }
}

function priorityClass(priority: NotificationPriority): string {
  const value = String(priority || "LOW").toUpperCase();
  if (value === "HIGH") return "high";
  if (value === "MEDIUM") return "medium";
  return "low";
}
</script>

<style scoped>
.tabs {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.body {
  flex: 1;
  position: relative;
  display: flex;
}

.list {
  flex: 1;
  padding: 12px 16px;
  overflow-y: auto;
}

.item {
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  display: grid;
  gap: 6px;
}

.item:hover {
  background: #f8fafc;
}

.item.selected {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.item.read .text {
  color: var(--text-muted);
}

.item-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}

.text {
  flex: 1;
}

.item-meta {
  font-size: 12px;
  color: var(--text-muted);
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ef4444;
}

.priority-dot {
  width: 8px;
  height: 8px;
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

.hint {
  padding: 14px 4px;
  color: var(--text-muted);
  text-align: center;
}
</style>
