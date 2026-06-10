<template>
  <div
    class="bz-input"
    :class="{ 'is-disabled': disabled }"
  >
    <input
      ref="inputRef"
      class="bz-input__inner"
      :value="textValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :type="type"
      @input="onInput"
      @change="onChange"
      @keyup="onKeyUp"
      @focus="onFocus"
      @blur="onBlur"
    />
    <button
      v-if="clearable && hasValue && !disabled && !readonly"
      class="bz-input__clear"
      type="button"
      @click="clearValue"
    >
      <BzIconClose :size="20" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";

import BzIconClose from "./BzIconClose.vue";

defineOptions({
  name: "BzInput",
});

const props = withDefaults(
  defineProps<{
    modelValue?: string | number | null;
    placeholder?: string;
    clearable?: boolean;
    disabled?: boolean;
    readonly?: boolean;
    type?: "text" | "password" | "number" | "search";
  }>(),
  {
    modelValue: "",
    placeholder: "",
    clearable: false,
    disabled: false,
    readonly: false,
    type: "text",
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: string): void;
  (e: "change", value: string): void;
  (e: "keyup", event: KeyboardEvent): void;
  (e: "focus", event: FocusEvent): void;
  (e: "blur", event: FocusEvent): void;
}>();

const textValue = computed(() => String(props.modelValue ?? ""));
const hasValue = computed(() => textValue.value.length > 0);
const inputRef = ref<HTMLInputElement | null>(null);

defineExpose({
  focus: () => {
    inputRef.value?.focus();
  },
  blur: () => {
    inputRef.value?.blur();
  },
});

function onInput(event: Event) {
  const target = event.target as HTMLInputElement;
  emit("update:modelValue", target.value);
}

function onChange(event: Event) {
  const target = event.target as HTMLInputElement;
  emit("change", target.value);
}

function onKeyUp(event: KeyboardEvent) {
  emit("keyup", event);
}

function onFocus(event: FocusEvent) {
  emit("focus", event);
}

function onBlur(event: FocusEvent) {
  emit("blur", event);
}

function clearValue() {
  emit("update:modelValue", "");
  emit("change", "");
}
</script>

<style scoped>
.bz-input {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 100%;
  min-height: 32px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fff;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.bz-input:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

.bz-input.is-disabled {
  background: #f8fafc;
}

.bz-input__inner {
  flex: 1;
  width: 100%;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  padding: 0 34px 0 10px;
  height: 30px;
  color: var(--text-main);
  font-size: 13px;
}

.bz-input__inner:disabled {
  cursor: not-allowed;
  color: #94a3b8;
}

.bz-input__clear {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  color: #94a3b8;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background-color 0.16s ease;
}

.bz-input__clear:hover {
  color: #64748b;
  background: #f1f5f9;
}
</style>
