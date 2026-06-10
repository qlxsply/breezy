<template>
  <svg
    class="bz-icon-close"
    viewBox="0 0 20 20"
    fill="none"
    aria-hidden="true"
    :style="iconStyle"
  >
    <circle
      cx="10"
      cy="10"
      r="9"
      fill="currentColor"
      fill-opacity="0.14"
    />
    <path
      d="M6 6L14 14"
      stroke="currentColor"
      stroke-width="2.2"
      stroke-linecap="round"
    />
    <path
      d="M14 6L6 14"
      stroke="currentColor"
      stroke-width="2.2"
      stroke-linecap="round"
    />
  </svg>
</template>

<script setup lang="ts">
import { computed } from "vue";

defineOptions({
  name: "BzIconClose",
});

const props = withDefaults(
  defineProps<{
    size?: number;
  }>(),
  {
    size: 20,
  },
);

const iconStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
}));
</script>

<style scoped>
.bz-icon-close {
  display: block;
}
</style>
