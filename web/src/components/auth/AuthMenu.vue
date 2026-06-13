<!-- /src/components/auth/AuthMenu.vue -->
<template>
  <div
    ref="rootRef"
    class="auth-menu"
  >
    <div v-if="!isAuthenticated" class="guest-actions">
      <button class="btn btn-ghost" type="button" @click="openRegisterPanel">注册</button>
      <button class="btn btn-primary" type="button" @click="openLoginPanel">登录</button>
    </div>

    <button
      v-else
      class="avatar-button"
      type="button"
      title="当前用户"
      @click="toggleDropdown"
    >
      <span class="avatar-face">{{ avatarText }}</span>
      <span class="avatar-status"></span>
    </button>

    <div
      v-if="dropdownOpen"
      class="dropdown"
    >
      <button
        class="dropdown-item"
        @click="onProfile"
      >
        Profile
      </button>
      <button
        class="dropdown-item"
        @click="onLogout"
      >
        Logout
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { isAuthenticated as authFlag, logout, useAuthUser } from "../../registry/auth.registry";
import { openLoginDialog, openRegisterDialog } from "../../registry/auth-dialog.registry";
import { refreshUserToolPermissions } from "../../registry/user-tool-permissions.registry";

const router = useRouter();
const route = useRoute();
const user = useAuthUser();
const isAuthenticated = authFlag;

const dropdownOpen = ref(false);
const rootRef = ref<HTMLElement | null>(null);
const avatarText = computed(() => {
  const source = (user.value?.username || "用户").trim();
  return source.slice(0, 1).toUpperCase() || "用";
});

function openLoginPanel() {
  openLoginDialog("", route.fullPath);
}

function openRegisterPanel() {
  openRegisterDialog(route.fullPath);
}

function toggleDropdown() {
  dropdownOpen.value = !dropdownOpen.value;
}

async function onLogout() {
  dropdownOpen.value = false;
  await logout();
  await refreshUserToolPermissions();
  router.push({ path: "/" });
}

function onProfile() {
  dropdownOpen.value = false;
  router.push({ path: "/profile", state: { fromHome: true } });
}

function onClickOutside(e: MouseEvent) {
  if (!dropdownOpen.value) return;
  const root = rootRef.value;
  if (!root) return;
  if (!root.contains(e.target as Node)) dropdownOpen.value = false;
}

onMounted(() => document.addEventListener("mousedown", onClickOutside));
onUnmounted(() => document.removeEventListener("mousedown", onClickOutside));
</script>

<style scoped>
.auth-menu {
  position: relative;
  display: inline-flex;
  align-items: center;
  z-index: 4;
}

.guest-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.btn {
  border: 1px solid var(--border-color);
  background: #fff;
  min-height: 40px;
  padding: 0 15px;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 700;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease;
}

.btn:hover {
  transform: translateY(-1px);
  border-color: #cbd5e1;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.btn-ghost {
  color: #334155;
  background: transparent;
}

.btn-primary {
  color: #fff;
  border-color: transparent;
  background: #2563eb;
  box-shadow: 0 12px 26px rgba(37, 99, 235, 0.24);
}

.btn-primary:hover {
  background: #1d4ed8;
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

.avatar-face {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: #ffffff;
  background: linear-gradient(135deg, #f59e0b 0%, #fb7185 55%, #60a5fa 100%);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.avatar-status {
  position: absolute;
  right: 5px;
  bottom: 7px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #22c55e;
  border: 2px solid #ffffff;
}

.dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 220px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18px);
  overflow: hidden;
  z-index: 8;
}

.dropdown-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 13px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-weight: 600;
  color: var(--text-main);
}

.dropdown-item:hover {
  background: #f8fafc;
}
</style>
