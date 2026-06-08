<template>
  <span
    class="bz-text"
    :class="`bz-text--${type}`"
  >
    <slot />
  </span>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzText",
});

withDefaults(
  defineProps<{
    type?: "primary" | "success" | "warning" | "danger" | "info";
  }>(),
  {
    type: "info",
  },
);
</script>

<style scoped>
.bz-text {
  font-size: 13px;
  color: var(--text-main);
}

.bz-text--primary {
  color: #2563eb;
}

.bz-text--success {
  color: #16a34a;
}

.bz-text--warning {
  color: #d97706;
}

.bz-text--danger {
  color: #dc2626;
}

.bz-text--info {
  color: #64748b;
}
</style>
