<template>
  <div class="bz-checkbox-group">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { provide } from "vue";

import { bzCheckboxGroupContextKey } from "./checkboxContext";

defineOptions({
  name: "BzCheckboxGroup",
});

const props = withDefaults(
  defineProps<{
    modelValue: unknown[];
    disabled?: boolean;
  }>(),
  {
    modelValue: () => [],
    disabled: false,
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: unknown[]): void;
  (e: "change", value: unknown[]): void;
}>();

function isChecked(value: unknown): boolean {
  return props.modelValue.some((item) => item === value);
}

function toggle(value: unknown, checked: boolean) {
  const next = [...props.modelValue];
  const index = next.findIndex((item) => item === value);
  if (checked && index < 0) {
    next.push(value);
  }
  if (!checked && index >= 0) {
    next.splice(index, 1);
  }
  emit("update:modelValue", next);
  emit("change", next);
}

provide(bzCheckboxGroupContextKey, {
  isChecked,
  toggle,
  disabled: props.disabled,
});
</script>

<style scoped>
.bz-checkbox-group {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
