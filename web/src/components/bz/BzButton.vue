<template>
  <button
    class="bz-button"
    :class="[
      `bz-button--${type}`,
      `bz-button--${size}`,
      { 'is-loading': loading, 'is-link': link || text, 'is-text': text },
    ]"
    :disabled="isDisabled"
    :type="nativeType"
    @click="onClick"
  >
    <span
      v-if="loading"
      class="bz-button-spinner"
      aria-hidden="true"
    ></span>
    <span class="bz-button-text"><slot /></span>
  </button>
</template>

<script setup lang="ts">
import { computed } from "vue";

defineOptions({
  name: "BzButton",
});

const props = withDefaults(
  defineProps<{
    type?: "default" | "primary" | "danger" | "success" | "warning";
    size?: "small" | "medium" | "large";
    loading?: boolean;
    disabled?: boolean;
    link?: boolean;
    text?: boolean;
    nativeType?: "button" | "submit" | "reset";
  }>(),
  {
    type: "default",
    size: "medium",
    loading: false,
    disabled: false,
    link: false,
    text: false,
    nativeType: "button",
  },
);

const emit = defineEmits<{
  (e: "click", event: MouseEvent): void;
}>();

const isDisabled = computed(() => props.disabled || props.loading);

function onClick(event: MouseEvent) {
  if (isDisabled.value) {
    event.preventDefault();
    return;
  }
  emit("click", event);
}
</script>

<style scoped>
.bz-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fff;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease;
}

.bz-button:hover:not(:disabled) {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.bz-button:disabled {
  cursor: not-allowed;
  opacity: 0.66;
}

.bz-button--small {
  min-height: 28px;
  padding: 0 10px;
  font-size: 12px;
}

.bz-button--large {
  min-height: 36px;
  padding: 0 18px;
  font-size: 14px;
}

.bz-button--primary {
  border-color: #3b82f6;
  background: #3b82f6;
  color: #fff;
}

.bz-button--primary:hover:not(:disabled) {
  border-color: #2563eb;
  background: #2563eb;
}

.bz-button--danger {
  border-color: #ef4444;
  background: #ef4444;
  color: #fff;
}

.bz-button--danger:hover:not(:disabled) {
  border-color: #dc2626;
  background: #dc2626;
}

.bz-button--success {
  border-color: #22c55e;
  background: #22c55e;
  color: #fff;
}

.bz-button--warning {
  border-color: #f59e0b;
  background: #f59e0b;
  color: #fff;
}

.bz-button.is-link {
  border-color: transparent;
  background: transparent;
  color: #3b82f6;
  padding: 0;
}

.bz-button.is-text {
  min-height: 24px;
}

.bz-button--danger.is-text {
  color: #dc2626;
}

.bz-button.is-link:hover:not(:disabled) {
  background: transparent;
  color: #2563eb;
}

.bz-button-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: bz-rotate 0.8s linear infinite;
}

.bz-button--default .bz-button-spinner,
.bz-button.is-link .bz-button-spinner {
  border-color: #cbd5e1;
  border-top-color: currentColor;
}

.bz-button-text {
  white-space: nowrap;
}

@keyframes bz-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
