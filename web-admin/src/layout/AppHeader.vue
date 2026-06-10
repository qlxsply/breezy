<!-- /src/layout/AppHeader.vue -->
<template>
  <div class="top-nav">
    <!-- 返回主页（可按需隐藏） -->
    <RouterLink
      v-if="props.showHome"
      class="home-btn"
      :to="{ name: 'home' }"
      title="返回主页"
    >
      <svg
        class="home-icon"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path
          fill="none"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="1.6"
          d="M4 10.5L12 4l8 6.5v8a1.5 1.5 0 0 1-1.5 1.5H6.5A1.5 1.5 0 0 1 5 18.5zM9 20v-6h6v6"
        />
      </svg>
      主页
    </RouterLink>

    <!-- 标题区域：工具页显示“工具：xxx”，设置页显示“设置：xxx” -->
    <div
      v-if="title"
      class="divider"
    >
      <span class="muted">{{ prefix }}</span>
      <span class="title">{{ title }}</span>
      <div
        v-if="showShortcutHelp"
        ref="helpWrapRef"
        class="shortcut-help"
      >
        <button
          class="help-btn"
          type="button"
          aria-label="查看快捷键"
          @click.stop="toggleHelp"
        >
          <svg
            class="help-icon"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <circle
              cx="12"
              cy="12"
              r="9"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
            />
            <path
              d="M9.6 9.2a2.6 2.6 0 0 1 5.2 0c0 1.6-1.3 2.2-2.1 2.6-.7.4-1 .7-1 1.5v.6"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.6"
            />
            <circle
              cx="12"
              cy="17.5"
              r="1"
              fill="currentColor"
            />
          </svg>
        </button>
        <div
          v-if="helpOpen"
          class="help-pop"
          @click.stop
        >
          <div class="help-title">快捷键</div>
          <div
            v-if="shortcuts.length === 0"
            class="help-empty"
          >
            暂无快捷键
          </div>
          <ul
            v-else
            class="help-list"
          >
            <li
              v-for="(item, index) in shortcuts"
              :key="`${item.keys}-${index}`"
              class="help-item"
            >
              <span class="help-key">{{ item.keys }}</span>
              <span class="help-action">{{ item.action }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <div class="right">
      <NotificationMenu v-if="isAuthenticated" />
      <AuthMenu />
    </div>
  </div>
</template>

<script setup lang="ts">
// System Header：只放置全局通用内容（主页/标题/用户菜单）。
import { computed, onMounted, onUnmounted, ref } from "vue";

import AuthMenu from "../components/auth/AuthMenu.vue";
import NotificationMenu from "../components/notifications/NotificationMenu.vue";
import { isAuthenticated } from "../registry/auth.registry";
import { usePageShortcuts } from "../registry/shortcuts.registry";

const props = withDefaults(
  defineProps<{
    prefix?: string; // “工具：” or “设置：”
    title?: string;
    showHome?: boolean;
  }>(),
  {
    prefix: "",
    title: "",
    showHome: true,
  },
);

const prefix = computed(() => props.prefix ?? "");

const helpWrapRef = ref<HTMLElement | null>(null);
const helpOpen = ref(false);
const showShortcutHelp = computed(
  () => prefix.value.startsWith("工具") && shortcuts.value.length > 0,
);
const shortcuts = usePageShortcuts();

function toggleHelp() {
  if (!showShortcutHelp.value) return;
  helpOpen.value = !helpOpen.value;
}

function handleClickOutside(event: MouseEvent) {
  if (!helpOpen.value) return;
  const target = event.target as Node | null;
  if (!target || !helpWrapRef.value) return;
  if (!helpWrapRef.value.contains(target)) {
    helpOpen.value = false;
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === "Escape") {
    helpOpen.value = false;
  }
}

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
  window.addEventListener("keydown", handleKeydown);
});

onUnmounted(() => {
  document.removeEventListener("click", handleClickOutside);
  window.removeEventListener("keydown", handleKeydown);
});
</script>

<style scoped>
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--app-header-height);
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  z-index: 101;
  box-sizing: border-box;
}

.home-btn {
  cursor: pointer;
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--primary-color);
  font-weight: 700;
  text-decoration: none;
}

.home-icon {
  width: 18px;
  height: 18px;
}

.divider {
  margin-left: 20px;
  color: var(--text-muted);
  font-size: 14px;
  border-left: 1px solid var(--border-color);
  padding-left: 20px;
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.muted {
  color: var(--text-muted);
}

.title {
  color: var(--text-main);
  font-weight: 700;
}

.shortcut-help {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.help-btn {
  width: 22px;
  height: 22px;
  padding: 0;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease;
}

.help-btn:hover {
  color: var(--text-main);
  background: transparent;
}

.help-icon {
  width: 16px;
  height: 16px;
}

.help-pop {
  position: absolute;
  top: 30px;
  left: 0;
  min-width: 220px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12);
  z-index: 120;
}

.help-title {
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 8px;
}

.help-empty {
  color: var(--text-muted);
  font-size: 12px;
}

.help-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 6px;
}

.help-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: var(--text-main);
}

.help-key {
  font-weight: 700;
  color: #1d4ed8;
  white-space: nowrap;
}

.help-action {
  color: var(--text-muted);
  text-align: right;
}

.right {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 12px;
}
</style>
