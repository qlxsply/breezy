<template>
  <li>
    <button
      class="bz-dropdown-item"
      :class="{ 'is-disabled': disabled }"
      type="button"
      :disabled="disabled"
      @click="onClick"
    >
      <slot />
    </button>
  </li>
</template>

<script setup lang="ts">
import { inject } from "vue";

import { bzDropdownContextKey } from "./dropdownContext";

defineOptions({
  name: "BzDropdownItem",
});

withDefaults(
  defineProps<{
    disabled?: boolean;
  }>(),
  {
    disabled: false,
  },
);

const emit = defineEmits<{
  (e: "click"): void;
}>();

const dropdownContext = inject(bzDropdownContextKey, null);

function onClick() {
  emit("click");
  dropdownContext?.close();
}
</script>

<style scoped>
.bz-dropdown-item {
  width: 100%;
  border: none;
  border-radius: 6px;
  background: transparent;
  text-align: left;
  font-size: 13px;
  padding: 7px 10px;
  color: var(--text-main);
  cursor: pointer;
}

.bz-dropdown-item:hover:not(.is-disabled) {
  background: #f1f5f9;
}

.bz-dropdown-item.is-disabled {
  color: #94a3b8;
  cursor: not-allowed;
}
</style>
