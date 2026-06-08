<template>
  <div
    v-show="visible"
    class="bz-tab-pane"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted } from "vue";

import { bzTabsContextKey } from "./tabsContext";

defineOptions({
  name: "BzTabPane",
});

const props = withDefaults(
  defineProps<{
    label?: string;
    name: string;
  }>(),
  {
    label: "",
  },
);

const tabsContext = inject(bzTabsContextKey, null);

const visible = computed(() => {
  if (!tabsContext) {
    return true;
  }
  return tabsContext.activeName.value === props.name;
});

onMounted(() => {
  tabsContext?.registerPane({ name: props.name, label: props.label || props.name });
});

onBeforeUnmount(() => {
  tabsContext?.unregisterPane(props.name);
});
</script>

<style scoped>
.bz-tab-pane {
  min-width: 0;
}
</style>
