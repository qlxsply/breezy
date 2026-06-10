<template>
  <div
    class="bz-container"
    :class="{ 'is-vertical': direction === 'vertical' }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzContainer",
});

withDefaults(
  defineProps<{
    direction?: "horizontal" | "vertical";
  }>(),
  {
    direction: "horizontal",
  },
);
</script>

<style scoped>
.bz-container {
  display: flex;
  min-height: 0;
  min-width: 0;
}

.bz-container.is-vertical {
  flex-direction: column;
}
</style>
