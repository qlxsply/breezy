<template>
  <span class="bz-icon">
    <slot />
  </span>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzIcon",
});
</script>

<style scoped>
.bz-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
</style>
