<template>
  <ul class="bz-dropdown-menu">
    <slot />
  </ul>
</template>

<script setup lang="ts">
defineOptions({
  name: "BzDropdownMenu",
});
</script>

<style scoped>
.bz-dropdown-menu {
  margin: 0;
  padding: 6px;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
