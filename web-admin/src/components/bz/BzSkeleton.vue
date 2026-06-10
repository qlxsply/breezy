<template>
  <div
    class="bz-skeleton"
    :class="{ 'is-animated': animated }"
  >
    <div
      v-for="idx in 3"
      :key="idx"
      class="line"
    ></div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzSkeleton",
});

withDefaults(
  defineProps<{
    animated?: boolean;
  }>(),
  {
    animated: false,
  },
);
</script>

<style scoped>
.bz-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.line {
  height: 12px;
  border-radius: 999px;
  background: #e2e8f0;
}

.line:nth-child(2) {
  width: 82%;
}

.line:nth-child(3) {
  width: 60%;
}

.bz-skeleton.is-animated .line {
  animation: bz-skeleton-pulse 1.2s ease-in-out infinite;
}

@keyframes bz-skeleton-pulse {
  0%,
  100% {
    opacity: 0.52;
  }
  50% {
    opacity: 1;
  }
}
</style>
