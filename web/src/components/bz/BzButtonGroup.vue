<template>
  <div class="bz-button-group">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzButtonGroup",
});
</script>

<style scoped>
.bz-button-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
