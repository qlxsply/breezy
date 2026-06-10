<template>
  <div
    class="bz-message"
    :class="`bz-message--${type}`"
  >
    <span
      class="bz-message__icon"
      aria-hidden="true"
      >{{ iconText }}</span
    >
    <span class="bz-message__content">{{ content }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import type { BzMessageType } from "./messageStore";

defineOptions({
  name: "BzMessage",
});

const props = defineProps<{
  type: BzMessageType;
  content: string;
}>();

const iconText = computed(() => {
  if (props.type === "success") {
    return "✓";
  }
  if (props.type === "warning") {
    return "!";
  }
  if (props.type === "error") {
    return "×";
  }
  return "i";
});
</script>

<style scoped>
.bz-message {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  max-width: 420px;
  border-radius: 10px;
  border: 1px solid transparent;
  padding: 8px 12px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.12);
  color: #1f2937;
  font-size: 13px;
  line-height: 1.4;
}

.bz-message__icon {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.bz-message__content {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bz-message--info {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.bz-message--info .bz-message__icon {
  background: #dbeafe;
}

.bz-message--success {
  border-color: #86efac;
  background: #ecfdf3;
  color: #15803d;
}

.bz-message--success .bz-message__icon {
  background: #bbf7d0;
}

.bz-message--warning {
  border-color: #fcd34d;
  background: #fffbeb;
  color: #b45309;
}

.bz-message--warning .bz-message__icon {
  background: #fde68a;
}

.bz-message--error {
  border-color: #fca5a5;
  background: #fef2f2;
  color: #b91c1c;
}

.bz-message--error .bz-message__icon {
  background: #fecaca;
}
</style>
