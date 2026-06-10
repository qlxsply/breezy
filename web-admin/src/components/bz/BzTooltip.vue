<template>
  <span
    class="bz-tooltip"
    :title="content"
  >
    <slot />
  </span>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzTooltip",
});

withDefaults(
  defineProps<{
    content?: string;
    placement?: string;
  }>(),
  {
    content: "",
    placement: "top",
  },
);
</script>

<style scoped>
.bz-tooltip {
  display: inline-flex;
}
</style>
