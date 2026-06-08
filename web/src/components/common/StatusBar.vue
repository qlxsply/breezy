<!-- /src/components/common/StatusBar.vue -->
<template>
  <div
    ref="barRef"
    class="status-bar"
  >
    <div class="status-left">
      <button
        class="history-btn"
        type="button"
        aria-label="消息记录"
        @click="toggleHistory"
      >
        <svg
          class="history-icon"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <circle
            cx="12"
            cy="12"
            r="8"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
          />
          <path
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.8"
            d="M12 8v5"
          />
          <circle
            cx="12"
            cy="16.5"
            r="1"
            fill="currentColor"
          />
        </svg>
      </button>
      <Transition
        name="status-slide"
        mode="out-in"
      >
        <div
          v-if="latestMessage"
          :key="latestMessage.id"
          :class="['status-message', latestMessage.type]"
          role="status"
        >
          <span
            class="status-icon"
            aria-hidden="true"
          >
            <svg
              v-if="latestMessage.type === 'success'"
              viewBox="0 0 24 24"
            >
              <circle
                cx="12"
                cy="12"
                r="9"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M8 12.5l2.5 2.5L16 9"
              />
            </svg>
            <svg
              v-else-if="latestMessage.type === 'error'"
              viewBox="0 0 24 24"
            >
              <circle
                cx="12"
                cy="12"
                r="9"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-width="2"
                d="M9 9l6 6M15 9l-6 6"
              />
            </svg>
            <svg
              v-else-if="latestMessage.type === 'warning'"
              viewBox="0 0 24 24"
            >
              <path
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 4l9 16H3z"
              />
              <path
                fill="currentColor"
                d="M11 9h2v6h-2z"
              />
              <circle
                cx="12"
                cy="17"
                r="1"
                fill="currentColor"
              />
            </svg>
            <svg
              v-else
              viewBox="0 0 24 24"
            >
              <circle
                cx="12"
                cy="12"
                r="9"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              />
              <path
                fill="currentColor"
                d="M11 10h2v7h-2z"
              />
              <circle
                cx="12"
                cy="7"
                r="1"
                fill="currentColor"
              />
            </svg>
          </span>
          <span class="status-text">{{ latestMessage.content }}</span>
        </div>
      </Transition>
      <div
        v-if="historyOpen"
        class="history-panel"
        role="dialog"
      >
        <div class="history-title">最近消息</div>
        <div
          v-if="messageHistory.length === 0"
          class="history-empty"
        >
          暂无记录
        </div>
        <div
          v-else
          class="history-list"
        >
          <div
            v-for="item in messageHistory"
            :key="item.id"
            :class="['history-item', item.type]"
          >
            <span
              class="history-item-icon"
              aria-hidden="true"
            >
              <svg
                v-if="item.type === 'success'"
                viewBox="0 0 24 24"
              >
                <circle
                  cx="12"
                  cy="12"
                  r="9"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                />
                <path
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M8 12.5l2.5 2.5L16 9"
                />
              </svg>
              <svg
                v-else-if="item.type === 'error'"
                viewBox="0 0 24 24"
              >
                <circle
                  cx="12"
                  cy="12"
                  r="9"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                />
                <path
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-width="2"
                  d="M9 9l6 6M15 9l-6 6"
                />
              </svg>
              <svg
                v-else-if="item.type === 'warning'"
                viewBox="0 0 24 24"
              >
                <path
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 4l9 16H3z"
                />
                <path
                  fill="currentColor"
                  d="M11 9h2v6h-2z"
                />
                <circle
                  cx="12"
                  cy="17"
                  r="1"
                  fill="currentColor"
                />
              </svg>
              <svg
                v-else
                viewBox="0 0 24 24"
              >
                <circle
                  cx="12"
                  cy="12"
                  r="9"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                />
                <path
                  fill="currentColor"
                  d="M11 10h2v7h-2z"
                />
                <circle
                  cx="12"
                  cy="7"
                  r="1"
                  fill="currentColor"
                />
              </svg>
            </span>
            <span class="history-item-text">{{ item.content }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";

import { useMessageStore } from "../../utils/message";

const { latestMessage, messageHistory } = useMessageStore();
const historyOpen = ref(false);
const barRef = ref<HTMLDivElement | null>(null);

function toggleHistory() {
  historyOpen.value = !historyOpen.value;
}

function handleDocumentClick(event: MouseEvent) {
  if (!historyOpen.value) {
    return;
  }
  const target = event.target as Node | null;
  if (!target || !barRef.value) {
    return;
  }
  if (!barRef.value.contains(target)) {
    historyOpen.value = false;
  }
}

onMounted(() => {
  document.addEventListener("click", handleDocumentClick);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleDocumentClick);
});
</script>

<style scoped>
.status-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: var(--status-bar-height);
  background: #fff;
  color: var(--text-main);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-top: 1px solid var(--border-color);
  box-sizing: border-box;
  z-index: 1200;
}

.status-left {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.history-btn {
  border: none;
  background: transparent;
  color: var(--text-main);
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.history-btn:hover {
  color: var(--primary-color);
}

.history-icon {
  width: 20px;
  height: 20px;
}

.history-panel {
  position: absolute;
  left: 0;
  bottom: calc(var(--status-bar-height) + 8px);
  width: 320px;
  max-height: 240px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  box-shadow: 0 12px 20px rgba(15, 23, 42, 0.12);
  padding: 10px 12px;
  box-sizing: border-box;
  overflow: hidden;
}

.history-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 8px;
}

.history-empty {
  font-size: 12px;
  color: var(--text-muted);
  padding: 6px 0 4px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 190px;
  overflow-y: auto;
  padding-right: 4px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-main);
}

.history-item.success {
  color: #16a34a;
}

.history-item.error {
  color: #dc2626;
}

.history-item.warning {
  color: #f59e0b;
}

.history-item.info {
  color: #2563eb;
}

.history-item-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-icon {
  width: 14px;
  height: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.history-item-icon svg {
  width: 14px;
  height: 14px;
  display: block;
}

.status-message {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-main);
  max-width: 60vw;
}

.status-icon {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-icon svg {
  width: 16px;
  height: 16px;
  display: block;
}

.status-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.status-message.success {
  color: var(--success-color);
}

.status-message.error {
  color: #ef4444;
}

.status-message.warning {
  color: #f59e0b;
}

.status-message.info {
  color: var(--primary-color);
}

.status-slide-enter-active,
.status-slide-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.status-slide-enter-from,
.status-slide-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (max-width: 768px) {
  .history-panel {
    width: 260px;
  }

  .status-message {
    max-width: 55vw;
  }
}
</style>
