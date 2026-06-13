<!-- /src/components/common/StatusBar.vue -->
<template>
  <div class="status-bar"></div>
</template>

<style scoped>
.status-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: var(--status-bar-height);
  background: #fff;
  color: var(--text-main);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-top: 1px solid var(--border-color);
  box-sizing: border-box;
  z-index: 1200;
}
</style>
