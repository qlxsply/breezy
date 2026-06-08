<!-- /src/App.vue -->
<template>
  <RouterView v-slot="{ Component }">
    <AdminLayout v-if="layoutMode === 'admin'">
      <component :is="Component" />
    </AdminLayout>
    <component
      :is="Component"
      v-else-if="layoutMode === 'blank'"
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
  <StatusBar v-if="layoutMode !== 'admin'" />
  <BzMessageHost />
  <BzConfirmHost />
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

import StatusBar from "./components/common/StatusBar.vue";
import AdminLayout from "./layout/AdminLayout.vue";
import AppHeader from "./layout/AppHeader.vue";

interface HeaderConfig {
  prefix?: string;
  title?: string;
  showHome?: boolean;
}

const route = useRoute();

type LayoutMode = "default" | "admin" | "blank";

const layoutMode = computed<LayoutMode>(() => {
  const layout = typeof route.meta?.layout === "string" ? route.meta.layout : "";
  if (layout === "admin" || layout === "blank") {
    return layout;
  }
  return "default";
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
