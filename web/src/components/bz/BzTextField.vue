<template>
  <div class="bz-text-field">
    <div
      class="bz-text-field__control"
      :class="{ 'is-disabled': disabled, 'is-textarea': type === 'textarea' }"
    >
      <textarea
        v-if="type === 'textarea'"
        ref="textareaRef"
        class="bz-text-field__textarea"
        :value="textValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :rows="rows"
        :maxlength="maxlength"
        @input="onInput"
        @change="onChange"
        @focus="onFocus"
        @blur="onBlur"
      />
      <input
        v-else
        ref="inputRef"
        class="bz-text-field__input"
        :value="textValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :type="type"
        :maxlength="maxlength"
        @input="onInput"
        @change="onChange"
        @keyup="onKeyUp"
        @focus="onFocus"
        @blur="onBlur"
      />
      <button
        v-if="clearable && hasValue && !disabled && !readonly"
        class="bz-text-field__clear"
        type="button"
        @click="clearValue"
      >
        <BzIconClose :size="20" />
      </button>
    </div>
    <div
      v-if="showCounter"
      class="bz-text-field__helper"
    >
      <div
        v-if="typeof maxlength === 'number'"
        class="bz-text-field__counter"
      >
        {{ currentLength }}/{{ maxlength }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";

import BzIconClose from "./BzIconClose.vue";

defineOptions({
  name: "BzTextField",
});

const props = withDefaults(
  defineProps<{
    modelValue?: string | number | null;
    placeholder?: string;
    clearable?: boolean;
    disabled?: boolean;
    readonly?: boolean;
    type?: "text" | "password" | "number" | "search" | "textarea";
    rows?: number;
    maxlength?: number;
    showCounter?: boolean;
  }>(),
  {
    modelValue: "",
    placeholder: "",
    clearable: false,
    disabled: false,
    readonly: false,
    type: "text",
    rows: 3,
    maxlength: undefined,
    showCounter: false,
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: string): void;
  (e: "change", value: string): void;
  (e: "keyup", event: KeyboardEvent): void;
  (e: "focus", event: FocusEvent): void;
  (e: "blur", event: FocusEvent): void;
}>();

const inputRef = ref<HTMLInputElement | null>(null);
const textareaRef = ref<HTMLTextAreaElement | null>(null);
const textValue = computed(() => String(props.modelValue ?? ""));
const hasValue = computed(() => textValue.value.length > 0);
const currentLength = computed(() => textValue.value.length);

defineExpose({
  focus: () => {
    inputRef.value?.focus();
    textareaRef.value?.focus();
  },
  blur: () => {
    inputRef.value?.blur();
    textareaRef.value?.blur();
  },
});

function normalizeValue(value: string): string {
  if (typeof props.maxlength === "number" && props.maxlength >= 0) {
    return value.slice(0, props.maxlength);
  }
  return value;
}

function onInput(event: Event) {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement;
  const nextValue = normalizeValue(target.value);
  if (target.value !== nextValue) {
    target.value = nextValue;
  }
  emit("update:modelValue", nextValue);
}

function onChange(event: Event) {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement;
  emit("change", normalizeValue(target.value));
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
.bz-text-field {
  display: grid;
  gap: 6px;
  width: 100%;
}

.bz-text-field__control {
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

.bz-text-field__control.is-textarea {
  align-items: stretch;
}

.bz-text-field__control:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

.bz-text-field__control.is-disabled {
  background: #f8fafc;
}

.bz-text-field__input,
.bz-text-field__textarea {
  flex: 1;
  width: 100%;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text-main);
  font-size: 13px;
}

.bz-text-field__input {
  padding: 0 34px 0 10px;
  height: 30px;
}

.bz-text-field__textarea {
  resize: vertical;
  min-height: 72px;
  padding: 8px 10px;
}

.bz-text-field__input:disabled,
.bz-text-field__textarea:disabled {
  cursor: not-allowed;
  color: #94a3b8;
}

.bz-text-field__clear {
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

.bz-text-field__clear:hover {
  color: #64748b;
  background: #f1f5f9;
}

.bz-text-field__helper {
  min-height: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.bz-text-field__counter {
  text-align: right;
  font-size: 12px;
  color: #94a3b8;
}
</style>
