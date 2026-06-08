<template>
  <div
    class="bz-select"
    :class="{ 'is-disabled': disabled }"
  >
    <select
      class="bz-select__inner"
      :value="selectValue"
      :disabled="disabled"
      @change="onChange"
    >
      <option value="">{{ placeholder || "请选择" }}</option>
      <slot />
    </select>

    <button
      v-if="clearable && hasValue && !disabled"
      class="bz-select__clear"
      type="button"
      @click="clearValue"
    >
      <BzIconClose :size="20" />
    </button>
    <span class="bz-select__arrow">▾</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import BzIconClose from "./BzIconClose.vue";

defineOptions({
  name: "BzSelect",
});

const props = withDefaults(
  defineProps<{
    modelValue?: string | number | null;
    placeholder?: string;
    clearable?: boolean;
    disabled?: boolean;
  }>(),
  {
    modelValue: "",
    placeholder: "",
    clearable: false,
    disabled: false,
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: string | undefined): void;
  (e: "change", value: string | undefined): void;
}>();

const selectValue = computed(() => String(props.modelValue ?? ""));
const hasValue = computed(() => selectValue.value !== "");

function onChange(event: Event) {
  const target = event.target as HTMLSelectElement;
  const value = target.value === "" ? undefined : target.value;
  emit("update:modelValue", value);
  emit("change", value);
}

function clearValue() {
  emit("update:modelValue", undefined);
  emit("change", undefined);
}
</script>

<style scoped>
.bz-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fff;
}

.bz-select:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

.bz-select.is-disabled {
  background: #f8fafc;
}

.bz-select__inner {
  width: 100%;
  height: 30px;
  border: none;
  outline: none;
  appearance: none;
  background: transparent;
  padding: 0 58px 0 10px;
  color: var(--text-main);
  font-size: 13px;
}

.bz-select__inner:disabled {
  cursor: not-allowed;
  color: #94a3b8;
}

.bz-select__arrow {
  position: absolute;
  right: 10px;
  color: #64748b;
  pointer-events: none;
  font-size: 12px;
}

.bz-select__clear {
  position: absolute;
  right: 24px;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background-color 0.16s ease;
}

.bz-select__clear:hover {
  color: #64748b;
  background: #f1f5f9;
}
</style>
