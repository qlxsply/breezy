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
  <AuthDrawer
    v-if="loginDialogOpen"
    :mode="loginDialogMode"
    :message="loginDialogError"
    :message-type="loginDialogMessageType"
    :submitting="authSubmitting"
    @close="closeLoginDialog"
    @switch-mode="setLoginDialogMode"
    @submit-login="handleLoginDialogSubmit"
    @submit-register="handleRegisterDialogSubmit"
  />
  <StatusBar />
  <BzMessageHost />
  <BzConfirmHost />
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { registerExternalUser } from "./api/public-users";
import AuthDrawer from "./components/auth/AuthDrawer.vue";
import StatusBar from "./components/common/StatusBar.vue";
import AppHeader from "./layout/AppHeader.vue";
import { login } from "./registry/auth.registry";
import {
  closeLoginDialog,
  getLoginDialogRedirectPath,
  setLoginDialogError,
  setLoginDialogMode,
  setLoginDialogSuccess,
  useLoginDialogError,
  useLoginDialogMessageType,
  useLoginDialogMode,
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
const loginDialogMode = useLoginDialogMode();
const loginDialogMessageType = useLoginDialogMessageType();
const authSubmitting = ref(false);

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
    authSubmitting.value = true;
    if (!payload.username || !payload.password) {
      setLoginDialogError("用户名和密码不能为空");
      return;
    }

    await login(payload.username, payload.password);
    await refreshUserToolsLoaded();
    await refreshUserToolPermissions();
    await initDynamicRoutes();

    const redirectPath = getLoginDialogRedirectPath();
    closeLoginDialog();

    if (redirectPath && !redirectPath.startsWith("/admin")) {
      await router.replace(redirectPath);
      return;
    }

    await router.replace({ path: route.fullPath, query: route.query, hash: route.hash });
  } catch (error) {
    setLoginDialogError(error instanceof Error ? error.message : "登录失败");
  } finally {
    authSubmitting.value = false;
  }
}

async function handleRegisterDialogSubmit(payload: {
  username: string;
  password: string;
  confirmPassword: string;
}) {
  try {
    authSubmitting.value = true;
    if (!payload.username || !payload.password || !payload.confirmPassword) {
      setLoginDialogError("用户名、密码和确认密码不能为空");
      return;
    }

    if (payload.password.trim() !== payload.confirmPassword.trim()) {
      setLoginDialogError("两次输入的密码不一致");
      return;
    }

    await registerExternalUser({
      username: payload.username,
      password: payload.password.trim(),
    });

    setLoginDialogMode("login");
    setLoginDialogSuccess("注册成功，请使用新账户登录。");
  } catch (error) {
    setLoginDialogError(error instanceof Error ? error.message : "注册失败");
  } finally {
    authSubmitting.value = false;
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
  padding: calc(var(--app-header-height)) 0 calc(var(--status-bar-height));
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .app-content {
    padding: calc(var(--app-header-height)) 0 calc(var(--status-bar-height));
  }
}
</style>
