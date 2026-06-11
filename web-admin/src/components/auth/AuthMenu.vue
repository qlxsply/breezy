<!-- /src/components/auth/AuthMenu.vue -->
<template>
  <div
    ref="rootRef"
    class="auth-menu"
  >
    <button
      v-if="!isAuthenticated"
      class="btn"
      @click="openLoginPage"
    >
      登录
    </button>

    <div
      v-else
      class="profile"
      @click="toggleDropdown"
    >
      <span class="avatar">👤</span>
      <span class="name">{{ user?.username || "用户" }}</span>
      <span class="caret">▾</span>
    </div>

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
import { onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { isAuthenticated as authFlag, logout, useAuthUser } from "../../registry/auth.registry";
import { refreshPermissions } from "../../registry/permissions.registry";

const router = useRouter();
const route = useRoute();
const user = useAuthUser();
const isAuthenticated = authFlag;

const dropdownOpen = ref(false);
const rootRef = ref<HTMLElement | null>(null);

function openLoginPage() {
  void router.push({ path: "/admin/login", query: { redirect: route.fullPath } });
}

function toggleDropdown() {
  dropdownOpen.value = !dropdownOpen.value;
}

async function onLogout() {
  dropdownOpen.value = false;
  await logout();
  await refreshPermissions();
  router.push({ path: "/admin/login" });
}

function onProfile() {
  dropdownOpen.value = false;
  router.push({ path: "/admin/profile" });
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
}

.btn {
  border: 1px solid var(--border-color);
  background: #fff;
  padding: 6px 12px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 700;
}

.profile {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
}

.avatar {
  font-size: 16px;
}

.name {
  font-weight: 700;
}

.caret {
  color: var(--text-muted);
}

.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 12px 22px -8px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  min-width: 160px;
  z-index: 20;
}

.dropdown-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-weight: 600;
}

.dropdown-item:hover {
  background: #f8fafc;
}
</style>
