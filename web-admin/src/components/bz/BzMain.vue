<template>
  <main class="bz-main">
    <slot />
  </main>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzMain",
});
</script>

<style scoped>
.bz-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
}
</style>
