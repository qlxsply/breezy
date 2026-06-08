<template>
  <span
    class="bz-tag"
    :class="`bz-tag--${type}`"
  >
    <slot />
  </span>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzTag",
});

withDefaults(
  defineProps<{
    type?: "info" | "success" | "warning" | "danger";
  }>(),
  {
    type: "info",
  },
);
</script>

<style scoped>
.bz-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  padding: 4px 8px;
  border: 1px solid transparent;
}

.bz-tag--info {
  background: #f1f5f9;
  color: #475569;
  border-color: #cbd5e1;
}

.bz-tag--success {
  background: #ecfdf3;
  color: #15803d;
  border-color: #86efac;
}

.bz-tag--warning {
  background: #fffbeb;
  color: #b45309;
  border-color: #fcd34d;
}

.bz-tag--danger {
  background: #fef2f2;
  color: #b91c1c;
  border-color: #fca5a5;
}
</style>
