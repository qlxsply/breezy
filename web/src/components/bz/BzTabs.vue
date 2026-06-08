<template>
  <div class="bz-tabs">
    <div class="bz-tabs__nav">
      <button
        v-for="pane in panes"
        :key="pane.name"
        class="bz-tabs__tab"
        :class="{ 'is-active': pane.name === currentName }"
        type="button"
        @click="select(pane.name)"
      >
        {{ pane.label }}
      </button>
    </div>
    <div class="bz-tabs__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, provide, ref, watch } from "vue";

import type { BzTabPaneItem } from "./tabsContext";
import { bzTabsContextKey } from "./tabsContext";

defineOptions({
  name: "BzTabs",
});

const props = withDefaults(
  defineProps<{
    modelValue?: string;
  }>(),
  {
    modelValue: "",
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: string): void;
  (e: "tab-change", value: string): void;
}>();

const panes = ref<BzTabPaneItem[]>([]);
const currentName = computed(() => props.modelValue || panes.value[0]?.name || "");
const activeName = computed(() => currentName.value);

function registerPane(pane: BzTabPaneItem) {
  if (panes.value.some((item) => item.name === pane.name)) {
    return;
  }
  panes.value.push(pane);
}

function unregisterPane(name: string) {
  panes.value = panes.value.filter((item) => item.name !== name);
}

function select(name: string) {
  emit("update:modelValue", name);
  emit("tab-change", name);
}

const activeNameRef = ref(activeName.value);
watch(
  () => activeName.value,
  (value) => {
    activeNameRef.value = value;
  },
  { immediate: true },
);
provide(bzTabsContextKey, {
  activeName: activeNameRef,
  registerPane,
  unregisterPane,
});
</script>

<style scoped>
.bz-tabs {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bz-tabs__nav {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}

.bz-tabs__tab {
  min-height: 32px;
  padding: 0 14px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: #fff;
  color: var(--text-main);
  cursor: pointer;
}

.bz-tabs__tab.is-active {
  border-color: #3b82f6;
  background: #3b82f6;
  color: #fff;
}
</style>
