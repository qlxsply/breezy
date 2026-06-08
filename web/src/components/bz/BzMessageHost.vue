<template>
  <Teleport to="body">
    <div class="bz-message-host">
      <TransitionGroup
        name="bz-message-fade"
        tag="div"
        class="bz-message-list"
      >
        <BzMessage
          v-for="item in messages"
          :key="item.id"
          :type="item.type"
          :content="item.content"
          @click="removeBzMessage(item.id)"
        />
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import BzMessage from "./BzMessage.vue";
import { removeBzMessage, useBzMessageStore } from "./messageStore";

defineOptions({
  name: "BzMessageHost",
});

const { messages } = useBzMessageStore();
</script>

<style scoped>
.bz-message-host {
  position: fixed;
  top: calc(var(--app-header-height) + 12px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 1600;
  pointer-events: none;
}

.bz-message-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.bz-message-list :deep(.bz-message) {
  pointer-events: auto;
  cursor: pointer;
}

.bz-message-fade-enter-active,
.bz-message-fade-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.bz-message-fade-enter-from,
.bz-message-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
