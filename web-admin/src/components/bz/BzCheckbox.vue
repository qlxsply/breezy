<template>
  <label
    class="bz-checkbox"
    :class="{ 'is-disabled': mergedDisabled }"
  >
    <input
      type="checkbox"
      :checked="checked"
      :disabled="mergedDisabled"
      @change="onChange"
    />
    <span class="indicator"></span>
    <span class="label"
      ><slot>{{ labelText }}</slot></span
    >
  </label>
</template>

<script setup lang="ts">
import { computed, inject } from "vue";

import { bzCheckboxGroupContextKey } from "./checkboxContext";

defineOptions({
  name: "BzCheckbox",
});

const props = withDefaults(
  defineProps<{
    modelValue?: boolean;
    label?: unknown;
    disabled?: boolean;
  }>(),
  {
    modelValue: false,
    label: undefined,
    disabled: false,
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "change", value: boolean): void;
}>();

const group = inject(bzCheckboxGroupContextKey, null);

const mergedDisabled = computed(() => props.disabled || group?.disabled === true);
const checked = computed(() => {
  if (group && props.label !== undefined) {
    return group.isChecked(props.label);
  }
  return props.modelValue;
});

const labelText = computed(() => {
  if (typeof props.label === "string") {
    return props.label;
  }
  return "";
});

function onChange(event: Event) {
  const target = event.target as HTMLInputElement;
  if (group && props.label !== undefined) {
    group.toggle(props.label, target.checked);
    return;
  }
  emit("update:modelValue", target.checked);
  emit("change", target.checked);
}
</script>

<style scoped>
.bz-checkbox {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-main);
}

.bz-checkbox input {
  display: none;
}

.indicator {
  width: 14px;
  height: 14px;
  border: 1px solid #94a3b8;
  border-radius: 4px;
  background: #fff;
  position: relative;
}

input:checked + .indicator {
  border-color: #3b82f6;
  background: #3b82f6;
}

input:checked + .indicator::after {
  content: "";
  position: absolute;
  left: 4px;
  top: 1px;
  width: 4px;
  height: 8px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.bz-checkbox.is-disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
