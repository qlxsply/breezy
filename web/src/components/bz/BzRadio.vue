<template>
  <label
    class="bz-radio"
    :class="{ 'is-disabled': mergedDisabled }"
  >
    <input
      type="radio"
      :checked="checked"
      :disabled="mergedDisabled"
      @change="onChange"
    />
    <span class="dot"></span>
    <span
      ><slot>{{ labelText }}</slot></span
    >
  </label>
</template>

<script setup lang="ts">
import { computed, inject } from "vue";

import { bzRadioGroupContextKey } from "./radioContext";

defineOptions({
  name: "BzRadio",
});

const props = withDefaults(
  defineProps<{
    modelValue?: unknown;
    label?: unknown;
    disabled?: boolean;
  }>(),
  {
    modelValue: undefined,
    label: undefined,
    disabled: false,
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: unknown): void;
  (e: "change", value: unknown): void;
}>();

const group = inject(bzRadioGroupContextKey, null);

const mergedDisabled = computed(() => props.disabled || group?.disabled === true);
const checked = computed(() => {
  if (group) {
    return group.isChecked(props.label);
  }
  return props.modelValue === true;
});
const labelText = computed(() => (typeof props.label === "string" ? props.label : ""));

function onChange() {
  if (group) {
    group.update(props.label);
    return;
  }
  emit("update:modelValue", true);
  emit("change", true);
}
</script>

<style scoped>
.bz-radio {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
}

.bz-radio input {
  display: none;
}

.dot {
  width: 14px;
  height: 14px;
  border: 1px solid #94a3b8;
  border-radius: 50%;
  position: relative;
}

input:checked + .dot {
  border-color: #3b82f6;
}

input:checked + .dot::after {
  content: "";
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #3b82f6;
  top: 2px;
  left: 2px;
}

.bz-radio.is-disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
