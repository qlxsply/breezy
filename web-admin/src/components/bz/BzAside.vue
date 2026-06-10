<template>
  <aside
    class="bz-aside"
    :style="asideStyle"
  >
    <slot />
  </aside>
</template>

<script setup lang="ts">
import { computed } from "vue";

defineOptions({
  name: "BzAside",
});

const props = withDefaults(
  defineProps<{
    width?: string;
  }>(),
  {
    width: "220px",
  },
);

const asideStyle = computed(() => ({
  width: props.width,
  flex: `0 0 ${props.width}`,
}));
</script>

<style scoped>
.bz-aside {
  min-height: 0;
  min-width: 0;
}
</style>
