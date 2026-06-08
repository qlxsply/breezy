<template>
  <button
    class="bz-menu-item"
    :class="{ 'is-active': isActive }"
    type="button"
    @click="onClick"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed, inject } from "vue";

import { bzMenuContextKey } from "./menuContext";

defineOptions({
  name: "BzMenuItem",
});

const props = withDefaults(
  defineProps<{
    index: string;
  }>(),
  {
    index: "",
  },
);

const menuContext = inject(bzMenuContextKey, null);
const isActive = computed(() => menuContext?.activeKey === props.index);

function onClick() {
  menuContext?.select(props.index);
}
</script>

<style scoped>
.bz-menu-item {
  width: 100%;
  border: 1px solid transparent;
  border-radius: 8px;
  min-height: 36px;
  padding: 0 10px;
  background: transparent;
  color: var(--text-main);
  text-align: left;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.bz-menu-item:hover {
  background: #f8fafc;
}

.bz-menu-item.is-active {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}
</style>
