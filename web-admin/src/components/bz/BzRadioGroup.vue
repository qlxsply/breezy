<template>
  <div
    class="bz-radio-group"
    :class="{ 'is-button': isButton }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { provide } from "vue";

import { bzRadioGroupContextKey } from "./radioContext";

defineOptions({
  name: "BzRadioGroup",
});

const props = withDefaults(
  defineProps<{
    modelValue: unknown;
    disabled?: boolean;
    size?: "small" | "medium" | "large";
  }>(),
  {
    disabled: false,
    size: "medium",
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: unknown): void;
  (e: "change", value: unknown): void;
}>();

const isButton = props.size !== undefined;

function update(value: unknown) {
  emit("update:modelValue", value);
  emit("change", value);
}

provide(bzRadioGroupContextKey, {
  isChecked: (value) => props.modelValue === value,
  disabled: props.disabled,
  update,
  isButton,
});
</script>

<style scoped>
.bz-radio-group {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.bz-radio-group.is-button {
  gap: 6px;
}
</style>
