<template>
  <div class="bz-empty">
    <div class="bz-empty__icon">◻</div>
    <div class="bz-empty__text">{{ description }}</div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzEmpty",
});

withDefaults(
  defineProps<{
    description?: string;
  }>(),
  {
    description: "暂无数据",
  },
);
</script>

<style scoped>
.bz-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
  padding: 24px 8px;
  text-align: center;
}

.bz-empty__icon {
  width: 28px;
  height: 28px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.bz-empty__text {
  font-size: 13px;
}
</style>
