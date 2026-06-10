<template>
  <div class="bz-form-item">
    <label
      v-if="label"
      class="bz-form-item__label"
      >{{ label }}</label
    >
    <div class="bz-form-item__content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzFormItem",
});

withDefaults(
  defineProps<{
    label?: string;
  }>(),
  {
    label: "",
  },
);
</script>

<style scoped>
.bz-form-item {
  display: inline-flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 0;
}

.bz-form-item__label {
  font-size: 12px;
  line-height: 16px;
  color: var(--text-muted);
}

.bz-form-item__content {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
