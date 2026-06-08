<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <div class="sidebar-brand__logo">B</div>
        <div>
          <strong>Breezy Admin</strong>
          <span>独立后台前端</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <template
          v-for="node in tree"
          :key="node.id"
        >
          <div class="sidebar-group">
            <button
              v-if="node.to"
              class="sidebar-item"
              :class="{ active: isActive(node.to) }"
              type="button"
              @click="router.push(node.to)"
            >
              {{ node.name }}
            </button>
            <div
              v-else
              class="sidebar-directory"
            >
              {{ node.name }}
            </div>

            <div
              v-if="node.children.length > 0"
              class="sidebar-children"
            >
              <button
                v-for="child in node.children"
                :key="child.id"
                class="sidebar-item sidebar-item--child"
                :class="{ active: child.to ? isActive(child.to) : false }"
                type="button"
                @click="onNodeClick(child)"
              >
                {{ child.name }}
              </button>
            </div>
          </div>
        </template>
      </nav>
    </aside>

    <section class="admin-main">
      <header class="admin-topbar">
        <div>
          <div class="admin-topbar__label">后台内容工作区</div>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="admin-topbar__actions">
          <span class="admin-topbar__user">{{ account }}</span>
          <BzButton @click="handleLogout">退出登录</BzButton>
        </div>
      </header>

      <main class="admin-content">
        <slot />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useAdminMenuTree } from "@admin/registry/admin-menu-resources";
import { logout, useAuthUser } from "@admin/registry/auth";
import type { AdminMenuTreeNode } from "@admin/types/admin-menu-resource";
import { BzButton } from "@shared/components/bz";
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();
const authUser = useAuthUser();
const tree = useAdminMenuTree();

const pageTitle = computed(() => String(route.meta.title || "后台页面"));
const account = computed(() => authUser.value?.account || "后台账号");

function isActive(path: string): boolean {
  return normalize(path) === normalize(route.path);
}

function normalize(path: string): string {
  return path.length > 1 && path.endsWith("/") ? path.slice(0, -1) : path;
}

function resolveFirstChildTarget(node: AdminMenuTreeNode): string | null {
  for (const child of node.children) {
    if (child.to) {
      return child.to;
    }
    const nested = resolveFirstChildTarget(child);
    if (nested) {
      return nested;
    }
  }
  return null;
}

function onNodeClick(node: AdminMenuTreeNode): void {
  const target = node.to || resolveFirstChildTarget(node);
  if (target) {
    void router.push(target);
  }
}

async function handleLogout() {
  await logout();
  await router.push("/login");
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  background: #f4f7fb;
}
.admin-sidebar {
  padding: 18px 16px;
  border-right: 1px solid #e2e8f0;
  background: #0f172a;
  color: #fff;
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 10px 18px;
}
.sidebar-brand__logo {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: #2563eb;
  font-weight: 800;
}
.sidebar-brand strong,
.sidebar-brand span {
  display: block;
}
.sidebar-brand span {
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
}
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.sidebar-directory {
  padding: 8px 10px;
  color: rgba(255, 255, 255, 0.54);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.sidebar-children {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 6px;
}
.sidebar-item {
  width: 100%;
  min-height: 38px;
  padding: 0 12px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: rgba(255, 255, 255, 0.82);
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
  white-space: nowrap;
}
.sidebar-item:hover,
.sidebar-item.active {
  background: rgba(59, 130, 246, 0.22);
  color: #fff;
}
.sidebar-item--child {
  padding-left: 18px;
}
.admin-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 16px;
}
.admin-topbar__label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.admin-topbar h1 {
  margin: 8px 0 0;
  color: #0f172a;
  font-size: 28px;
  letter-spacing: -0.03em;
}
.admin-topbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.admin-topbar__user {
  color: #475569;
  font-size: 13px;
}
.admin-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 24px 24px;
}
@media (max-width: 960px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }
  .admin-sidebar {
    border-right: none;
    border-bottom: 1px solid #1e293b;
  }
  .admin-topbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
