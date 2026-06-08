<template>
  <nav class="bz-menu">
    <slot />
  </nav>
</template>

<script setup lang="ts">
import { provide, ref, watch } from "vue";

import { bzMenuContextKey } from "./menuContext";

defineOptions({
  name: "BzMenu",
});

const props = withDefaults(
  defineProps<{
    defaultActive?: string;
  }>(),
  {
    defaultActive: "",
  },
);

const emit = defineEmits<{
  (e: "select", key: string): void;
}>();

const activeKey = ref(props.defaultActive);

watch(
  () => props.defaultActive,
  (value) => {
    activeKey.value = value;
  },
);

function select(key: string) {
  activeKey.value = key;
  emit("select", key);
}

provide(bzMenuContextKey, {
  get activeKey() {
    return activeKey.value;
  },
  select,
});
</script>

<style scoped>
.bz-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
