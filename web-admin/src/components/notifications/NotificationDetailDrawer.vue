<!-- /src/components/notifications/NotificationDetailDrawer.vue -->
<template>
  <div
    class="drawer"
    :class="{ open }"
  >
    <div class="drawer-header">
      <div class="drawer-title">消息详情</div>
      <button
        class="close"
        @click="$emit('close')"
      >
        ✕
      </button>
    </div>

    <div
      v-if="item"
      class="drawer-body"
    >
      <div class="meta">
        <span :class="['priority-dot', priorityClass(item.priority)]" />
        <span class="time">{{ formatDateTime(item.createdAt) }}</span>
      </div>

      <div class="title">{{ item.title }}</div>
      <div class="content">{{ item.content || "暂无详情" }}</div>
    </div>
    <div
      v-else
      class="drawer-body empty"
    >
      请选择一条消息
    </div>
  </div>
</template>

<script setup lang="ts">
// <script setup> + TS：顶层即 setup()。
import type { NotificationItem, NotificationPriority } from "../../types/notification";
import { formatDateTime } from "../../utils/formatter";

defineProps<{
  item: NotificationItem | null;
  open: boolean;
}>();

defineEmits<{
  (e: "close"): void;
}>();

function priorityClass(priority: NotificationPriority): string {
  const value = String(priority || "LOW").toUpperCase();
  if (value === "HIGH") return "high";
  if (value === "MEDIUM") return "medium";
  return "low";
}
</script>

<style scoped>
.drawer {
  position: absolute;
  top: 0;
  right: 0;
  width: 320px;
  height: 100%;
  background: #fff;
  border-left: 1px solid var(--border-color);
  box-shadow: -8px 0 16px rgba(0, 0, 0, 0.08);
  transform: translateX(100%);
  transition: transform 0.2s ease;
  display: flex;
  flex-direction: column;
}

.drawer.open {
  transform: translateX(0);
}

.drawer-header {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 800;
}

.close {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
}

.drawer-body {
  padding: 16px;
  display: grid;
  gap: 12px;
  overflow-y: auto;
}

.drawer-body.empty {
  color: var(--text-muted);
  font-style: italic;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.title {
  font-weight: 900;
  font-size: 14px;
}

.content {
  font-size: 13px;
  color: var(--text-main);
  line-height: 1.6;
}

.time {
  color: var(--text-muted);
}

.priority-dot {
  width: 10px;
  height: 10px;
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
</style>
