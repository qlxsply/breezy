<template>
  <Teleport to="body">
    <Transition name="bz-dialog-fade">
      <div
        v-if="modelValue"
        class="bz-dialog-overlay"
        :style="overlayStyle"
        @click="onOverlayClick"
      >
        <div
          class="bz-dialog"
          :style="dialogStyle"
          role="dialog"
          aria-modal="true"
          @click.stop
        >
          <header class="bz-dialog__header">
            <div class="bz-dialog__title">{{ title }}</div>
            <button
              v-if="showClose"
              class="bz-dialog__close"
              type="button"
              @click="handleCancel"
            >
              <BzIconClose :size="16" />
            </button>
          </header>

          <section class="bz-dialog__body">
            <slot />
          </section>

          <footer class="bz-dialog__footer">
            <slot name="footer">
              <button
                class="bz-dialog__btn bz-dialog__btn--default"
                type="button"
                @click="handleCancel"
              >
                {{ cancelText }}
              </button>
              <button
                class="bz-dialog__btn bz-dialog__btn--primary"
                type="button"
                @click="handleConfirm"
              >
                {{ confirmText }}
              </button>
            </slot>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from "vue";

import BzIconClose from "./BzIconClose.vue";

const OPEN_DIALOG_STACK: symbol[] = [];

function pushDialog(id: symbol) {
  if (!OPEN_DIALOG_STACK.includes(id)) {
    OPEN_DIALOG_STACK.push(id);
  }
  syncBodyOverflow();
}

function removeDialog(id: symbol) {
  const index = OPEN_DIALOG_STACK.indexOf(id);
  if (index >= 0) {
    OPEN_DIALOG_STACK.splice(index, 1);
  }
  syncBodyOverflow();
}

function topDialog(): symbol | undefined {
  return OPEN_DIALOG_STACK[OPEN_DIALOG_STACK.length - 1];
}

function syncBodyOverflow() {
  document.body.style.overflow = OPEN_DIALOG_STACK.length > 0 ? "hidden" : "";
}

defineOptions({
  name: "BzDialog",
});

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    title?: string;
    width?: number | string;
    maxWidth?: number | string;
    top?: string;
    closeOnOverlay?: boolean;
    closeOnClickModal?: boolean;
    closeOnEsc?: boolean;
    showClose?: boolean;
    confirmText?: string;
    cancelText?: string;
  }>(),
  {
    title: "提示",
    width: 520,
    maxWidth: "min(92vw, 720px)",
    top: "",
    closeOnOverlay: true,
    closeOnClickModal: undefined,
    closeOnEsc: true,
    showClose: true,
    confirmText: "确定",
    cancelText: "取消",
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "confirm"): void;
  (e: "cancel"): void;
  (e: "close"): void;
}>();

const instanceId = Symbol("bz-dialog-instance");

const dialogStyle = computed(() => {
  let width: string;
  if (typeof props.width === "number") {
    width = `${props.width}px`;
  } else if (/^\d+$/.test(props.width)) {
    width = `${props.width}px`;
  } else {
    width = props.width;
  }

  let maxWidth: string;
  if (typeof props.maxWidth === "number") {
    maxWidth = `${props.maxWidth}px`;
  } else if (/^\d+$/.test(props.maxWidth)) {
    maxWidth = `${props.maxWidth}px`;
  } else {
    maxWidth = props.maxWidth;
  }

  return { width, maxWidth };
});

const overlayStyle = computed(() => {
  const top = (props.top ?? "").trim();
  if (!top) {
    return {};
  }
  return {
    alignItems: "flex-start",
    paddingTop: top,
  };
});

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      pushDialog(instanceId);
      return;
    }
    removeDialog(instanceId);
  },
  { immediate: true },
);

function handleCancel() {
  emit("cancel");
  emit("close");
  emit("update:modelValue", false);
}

function handleConfirm() {
  emit("confirm");
}

function onOverlayClick() {
  const allowCloseOnOverlay = props.closeOnClickModal ?? props.closeOnOverlay;
  if (!allowCloseOnOverlay) {
    return;
  }
  handleCancel();
}

function onKeyDown(event: KeyboardEvent) {
  if (!props.modelValue || !props.closeOnEsc) {
    return;
  }
  if (topDialog() !== instanceId) {
    return;
  }
  if (event.key === "Escape") {
    event.preventDefault();
    event.stopImmediatePropagation();
    handleCancel();
  }
}

window.addEventListener("keydown", onKeyDown);

onBeforeUnmount(() => {
  window.removeEventListener("keydown", onKeyDown);
  removeDialog(instanceId);
});
</script>

<style scoped>
.bz-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.44);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1700;
  padding: 20px;
  box-sizing: border-box;
}

.bz-dialog {
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: #fff;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.24);
  overflow: hidden;
}

.bz-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.bz-dialog__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
}

.bz-dialog__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  border-radius: 6px;
  transition:
    color 0.16s ease,
    background-color 0.16s ease;
}

.bz-dialog__close:hover {
  color: #334155;
  background: #f1f5f9;
}

.bz-dialog__body {
  padding: 16px;
  color: var(--text-main);
  font-size: 13px;
}

.bz-dialog__footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.bz-dialog__btn {
  min-height: 32px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}

.bz-dialog__btn--primary {
  border-color: #3b82f6;
  background: #3b82f6;
  color: #fff;
}

.bz-dialog-fade-enter-active,
.bz-dialog-fade-leave-active {
  transition: opacity 0.2s ease;
}

.bz-dialog-fade-enter-from,
.bz-dialog-fade-leave-to {
  opacity: 0;
}
</style>
