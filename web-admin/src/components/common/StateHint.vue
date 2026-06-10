<template>
  <div
    class="state-hint"
    :class="`state-hint--${type}`"
  >
    <span>{{ message }}</span>
    <button
      v-if="actionText"
      class="hint-action"
      type="button"
      :disabled="actionDisabled"
      @click="$emit('action')"
    >
      {{ actionText }}
    </button>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    message: string;
    type?: "info" | "error" | "warning";
    actionText?: string;
    actionDisabled?: boolean;
  }>(),
  {
    type: "info",
    actionText: "",
    actionDisabled: false,
  },
);

defineEmits<{
  action: [];
}>();
</script>

<style scoped>
.state-hint {
  padding: 12px 14px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.state-hint--error {
  color: #b91c1c;
}

.state-hint--warning {
  color: #92400e;
}

.hint-action {
  border: 1px solid currentColor;
  background: transparent;
  color: inherit;
  border-radius: 8px;
  padding: 4px 10px;
  cursor: pointer;
  font-size: 12px;
}

.hint-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
