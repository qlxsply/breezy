<template>
  <div class="app-shell">
    <bz-container class="management-container">
      <bz-aside
        width="220px"
        class="sidebar"
      >
        <bz-menu
          :default-active="activeTab"
          @select="handleMenuSelect"
        >
          <bz-menu-item
            v-for="menu in clinicMenus"
            :key="menu.id"
            :index="menu.id"
          >
            <span class="icon">{{ menu.icon }}</span>
            <span class="label">{{ menu.name }}</span>
          </bz-menu-item>
        </bz-menu>
      </bz-aside>
      <bz-main class="main-content">
        <component
          :is="activeTabComponent"
          v-if="activeTabComponent"
        />
        <bz-empty
          v-else
          description="请选择左侧菜单开始管理。"
        />
      </bz-main>
    </bz-container>
  </div>
</template>

<script setup lang="ts">
import { type Component, computed, defineAsyncComponent, onMounted, ref } from "vue";

import { getUserTools } from "../../registry/user-tools.registry";

const activeTab = ref<string>("");

const clinicMenuSeeds = [
  { id: "tool-clinic-catalog", name: "诊所商品", icon: "📝" },
  { id: "tool-clinic-inventory", name: "库存台账", icon: "📎" },
  { id: "tool-clinic-ledger", name: "流水台账", icon: "📛" },
  { id: "tool-clinic-purchase", name: "采购管理", icon: "📦" },
  { id: "tool-clinic-sale", name: "销售管理", icon: "💸" },
];

const clinicMenus = computed(() => {
  const allowed = new Set(
    getUserTools()
      .filter((resource) => resource.type === "MENU")
      .filter((resource) => resource.parentId === "tool-clinic")
      .map((resource) => resource.id),
  );
  return clinicMenuSeeds.filter((menu) => allowed.has(menu.id));
});

const tabComponents: Record<string, Component> = {
  "tool-clinic-catalog": defineAsyncComponent(() => import("./ClinicCatalogPage.vue")),
  "tool-clinic-inventory": defineAsyncComponent(() => import("./ClinicInventoryPage.vue")),
  "tool-clinic-ledger": defineAsyncComponent(() => import("./ClinicLedgerPage.vue")),
  "tool-clinic-purchase": defineAsyncComponent(() => import("./ClinicPurchasePage.vue")),
  "tool-clinic-sale": defineAsyncComponent(() => import("./ClinicSalePage.vue")),
};

const activeTabComponent = computed(() => {
  const allowed = new Set(clinicMenus.value.map((menu) => menu.id));
  if (!allowed.has(activeTab.value)) return null;
  return tabComponents[activeTab.value];
});

function handleMenuSelect(key: string) {
  activeTab.value = key;
}

onMounted(() => {
  if (clinicMenus.value.length > 0) {
    activeTab.value = clinicMenus.value[0].id;
  } else {
    activeTab.value = "";
  }
});
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.management-container {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #f8fafc;
}

.sidebar {
  border-right: 1px solid var(--border-color);
  background: #fff;
  padding: 12px 8px;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  padding: 0;
}

.icon {
  margin-right: 6px;
}

:deep(.app-shell) {
  height: auto !important;
}

.main-content :deep(.top-nav) {
  display: none !important;
}

:deep(.content) {
  padding: 16px 24px 32px !important;
}
</style>
