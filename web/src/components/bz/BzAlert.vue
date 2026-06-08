<template>
  <div
    class="bz-alert"
    :class="`bz-alert--${type}`"
  >
    <span
      v-if="showIcon"
      class="bz-alert__icon"
      aria-hidden="true"
      >{{ icon }}</span
    >
    <div class="bz-alert__content">
      <slot>
        {{ title }}
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

defineOptions({
  name: "BzAlert",
});

const props = withDefaults(
  defineProps<{
    title?: string;
    type?: "info" | "success" | "warning" | "error";
    showIcon?: boolean;
  }>(),
  {
    title: "",
    type: "info",
    showIcon: false,
  },
);

const icon = computed(() => {
  if (props.type === "success") return "✓";
  if (props.type === "warning") return "!";
  if (props.type === "error") return "×";
  return "i";
});
</script>

<style scoped>
.bz-alert {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  border-radius: 10px;
  padding: 10px 12px;
  color: #1d4ed8;
  font-size: 13px;
}

.bz-alert__icon {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.bz-alert--success {
  border-color: #86efac;
  background: #ecfdf3;
  color: #15803d;
}

.bz-alert--warning {
  border-color: #fcd34d;
  background: #fffbeb;
  color: #b45309;
}

.bz-alert--error {
  border-color: #fca5a5;
  background: #fef2f2;
  color: #b91c1c;
}
</style>
