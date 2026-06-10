<template>
  <div
    class="bz-input-number"
    :class="{ 'is-disabled': disabled }"
  >
    <button
      class="step-btn"
      type="button"
      :disabled="disabled"
      @click="stepDown"
    >
      -
    </button>
    <input
      class="number-input"
      type="number"
      :value="displayValue"
      :min="min"
      :max="max"
      :step="step"
      :disabled="disabled"
      @input="onInput"
      @change="onChange"
    />
    <button
      class="step-btn"
      type="button"
      :disabled="disabled"
      @click="stepUp"
    >
      +
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

defineOptions({
  name: "BzInputNumber",
});

const props = withDefaults(
  defineProps<{
    modelValue?: number;
    min?: number;
    max?: number;
    step?: number;
    disabled?: boolean;
  }>(),
  {
    modelValue: 0,
    min: Number.NEGATIVE_INFINITY,
    max: Number.POSITIVE_INFINITY,
    step: 1,
    disabled: false,
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: number): void;
  (e: "change", value: number): void;
}>();

const displayValue = computed(() => props.modelValue ?? 0);

function normalize(value: number): number {
  return Math.min(props.max, Math.max(props.min, value));
}

function setValue(value: number) {
  const next = normalize(value);
  emit("update:modelValue", next);
  emit("change", next);
}

function onInput(event: Event) {
  const target = event.target as HTMLInputElement;
  const parsed = Number(target.value);
  if (Number.isFinite(parsed)) {
    emit("update:modelValue", normalize(parsed));
  }
}

function onChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const parsed = Number(target.value);
  if (!Number.isFinite(parsed)) {
    setValue(props.min > Number.NEGATIVE_INFINITY ? props.min : 0);
    return;
  }
  setValue(parsed);
}

function stepUp() {
  setValue((props.modelValue ?? 0) + props.step);
}

function stepDown() {
  setValue((props.modelValue ?? 0) - props.step);
}
</script>

<style scoped>
.bz-input-number {
  display: inline-flex;
  align-items: center;
  height: 32px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.step-btn {
  width: 28px;
  height: 30px;
  border: none;
  background: #f8fafc;
  color: #475569;
  cursor: pointer;
}

.number-input {
  width: 72px;
  height: 30px;
  border: none;
  outline: none;
  text-align: center;
  font-size: 13px;
}

.bz-input-number.is-disabled {
  opacity: 0.7;
}

.bz-input-number.is-disabled .step-btn,
.bz-input-number.is-disabled .number-input {
  cursor: not-allowed;
}
</style>
