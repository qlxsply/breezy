<template>
  <BzDialog
    :model-value="Boolean(currentConfirm)"
    :title="currentConfirm?.title || '确认操作'"
    :confirm-text="currentConfirm?.confirmText || '确定'"
    :cancel-text="currentConfirm?.cancelText || '取消'"
    :close-on-overlay="currentConfirm?.closeOnOverlay || false"
    @update:model-value="onModelValueUpdate"
    @confirm="onConfirm"
    @cancel="onCancel"
  >
    <div class="bz-confirm-message">{{ currentConfirm?.message }}</div>
    <template #footer>
      <button
        class="bz-confirm-btn bz-confirm-btn--default"
        type="button"
        @click="onCancel"
      >
        {{ currentConfirm?.cancelText || "取消" }}
      </button>
      <button
        class="bz-confirm-btn"
        :class="currentConfirm?.danger ? 'bz-confirm-btn--danger' : 'bz-confirm-btn--primary'"
        type="button"
        @click="onConfirm"
      >
        {{ currentConfirm?.confirmText || "确定" }}
      </button>
    </template>
  </BzDialog>
</template>

<script setup lang="ts">
import BzDialog from "./BzDialog.vue";
import { resolveCurrentConfirm, useBzConfirmStore } from "./confirmStore";

defineOptions({
  name: "BzConfirmHost",
});

const { currentConfirm } = useBzConfirmStore();

function onConfirm() {
  resolveCurrentConfirm(true);
}

function onCancel() {
  resolveCurrentConfirm(false);
}

function onModelValueUpdate(visible: boolean) {
  if (!visible) {
    onCancel();
  }
}
</script>

<style scoped>
.bz-confirm-message {
  white-space: pre-wrap;
  line-height: 1.6;
}

.bz-confirm-btn {
  min-height: 32px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: #fff;
  padding: 0 14px;
  cursor: pointer;
  font-size: 13px;
}

.bz-confirm-btn--default {
  color: var(--text-main);
}

.bz-confirm-btn--primary {
  background: #3b82f6;
  border-color: #3b82f6;
  color: #fff;
}

.bz-confirm-btn--danger {
  background: #ef4444;
  border-color: #ef4444;
  color: #fff;
}
</style>
