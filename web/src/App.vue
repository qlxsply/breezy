<!-- /src/App.vue -->
<template>
  <RouterView v-slot="{ Component }">
    <component
      :is="Component"
      v-if="layoutMode === 'blank'"
    />
    <div
      v-else
      class="app-layout"
    >
      <AppHeader
        :prefix="headerConfig.prefix"
        :title="headerConfig.title"
        :show-home="headerConfig.showHome"
      />
      <main class="app-content">
        <component :is="Component" />
      </main>
    </div>
  </RouterView>
  <LoginDialog
    v-if="loginDialogOpen"
    :error="loginDialogError"
    @close="closeLoginDialog"
    @submit="handleLoginDialogSubmit"
  />
  <StatusBar />
  <BzMessageHost />
  <BzConfirmHost />
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

import LoginDialog from "./components/auth/LoginDialog.vue";
import StatusBar from "./components/common/StatusBar.vue";
import AppHeader from "./layout/AppHeader.vue";
import { login } from "./registry/auth.registry";
import {
  closeLoginDialog,
  setLoginDialogError,
  useLoginDialogError,
  useLoginDialogOpen,
} from "./registry/auth-dialog.registry";
import { refreshUserToolPermissions } from "./registry/user-tool-permissions.registry";
import { refreshUserToolsLoaded } from "./registry/user-tools.registry";
import { initDynamicRoutes } from "./router";

interface HeaderConfig {
  prefix?: string;
  title?: string;
  showHome?: boolean;
}

const route = useRoute();
const router = useRouter();
const loginDialogOpen = useLoginDialogOpen();
const loginDialogError = useLoginDialogError();

type LayoutMode = "default" | "blank";

const layoutMode = computed<LayoutMode>(() => {
  const layout = typeof route.meta?.layout === "string" ? route.meta.layout : "";
  return layout === "blank" ? "blank" : "default";
});

const headerConfig = computed<HeaderConfig>(() => {
  const metaHeader = (route.meta?.header as HeaderConfig | undefined) ?? {};
  if (route.name === "home") {
    return { ...metaHeader, showHome: false };
  }
  if (route.name === "not-found") {
    const fromHome = Boolean((route as { state?: { fromHome?: boolean } }).state?.fromHome);
    return { ...metaHeader, showHome: fromHome };
  }
  return { ...metaHeader, showHome: metaHeader.showHome ?? true };
});

async function handleLoginDialogSubmit(payload: { username: string; password: string }) {
  try {
    await login(payload.username, payload.password);
    await refreshUserToolsLoaded();
    await refreshUserToolPermissions();
    await initDynamicRoutes();
    closeLoginDialog();
    await router.replace({
      path: route.fullPath,
      query: route.query,
      hash: route.hash,
    });
  } catch (error) {
    setLoginDialogError(error instanceof Error ? error.message : "登录失败");
  }
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  background: var(--bg-color);
}

.app-content {
  height: 100vh;
  overflow: auto;
  padding: calc(var(--app-header-height) + 16px) 0 calc(var(--status-bar-height) + 16px);
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .app-content {
    padding: calc(var(--app-header-height) + 12px) 0 calc(var(--status-bar-height) + 12px);
  }
}
</style>
