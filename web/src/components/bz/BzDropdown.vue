<template>
  <div
    ref="rootRef"
    class="bz-dropdown"
  >
    <div
      ref="triggerRef"
      class="bz-dropdown__trigger"
      @click.stop="toggle"
    >
      <slot />
    </div>
    <Teleport to="body">
      <div
        v-if="open"
        ref="panelRef"
        class="bz-dropdown__panel"
        :style="panelStyle"
      >
        <slot name="dropdown" />
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, provide, ref, watch } from "vue";

import { bzDropdownContextKey } from "./dropdownContext";

defineOptions({
  name: "BzDropdown",
});

const props = withDefaults(
  defineProps<{
    minWidth?: number;
    offset?: number;
  }>(),
  {
    minWidth: 120,
    offset: 6,
  },
);

const rootRef = ref<HTMLElement | null>(null);
const triggerRef = ref<HTMLElement | null>(null);
const panelRef = ref<HTMLElement | null>(null);
const open = ref(false);
const panelStyle = ref<Record<string, string>>({
  minWidth: `${props.minWidth}px`,
});

function bindFloatingListeners() {
  window.addEventListener("resize", updatePosition);
  window.addEventListener("scroll", updatePosition, true);
}

function unbindFloatingListeners() {
  window.removeEventListener("resize", updatePosition);
  window.removeEventListener("scroll", updatePosition, true);
}

function updatePosition() {
  if (!triggerRef.value || !panelRef.value) {
    return;
  }

  const triggerRect = triggerRef.value.getBoundingClientRect();
  const panelRect = panelRef.value.getBoundingClientRect();
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  const margin = 8;

  let left = triggerRect.right + props.offset;
  if (left + panelRect.width > viewportWidth - margin) {
    left = Math.min(
      Math.max(triggerRect.left, margin),
      Math.max(margin, viewportWidth - margin - panelRect.width),
    );
  }
  if (left < margin) {
    left = margin;
  }
  if (left + panelRect.width > viewportWidth - margin) {
    left = Math.max(margin, viewportWidth - margin - panelRect.width);
  }

  const spaceBelow = viewportHeight - triggerRect.bottom - margin;
  const spaceAbove = triggerRect.top - margin;

  let top = triggerRect.bottom + props.offset;
  if (panelRect.height > spaceBelow && spaceAbove > spaceBelow) {
    top = triggerRect.top - props.offset - panelRect.height;
  }
  if (top < margin) {
    top = margin;
  }
  if (top + panelRect.height > viewportHeight - margin) {
    top = Math.max(margin, viewportHeight - margin - panelRect.height);
  }

  panelStyle.value = {
    position: "fixed",
    top: `${top}px`,
    left: `${left}px`,
    minWidth: `${props.minWidth}px`,
  };
}

function close() {
  if (!open.value) {
    return;
  }
  open.value = false;
  unbindFloatingListeners();
}

function toggle() {
  if (open.value) {
    close();
    return;
  }
  open.value = true;
  nextTick(() => {
    updatePosition();
    bindFloatingListeners();
  });
}

function handleClickOutside(event: MouseEvent) {
  if (!rootRef.value || !panelRef.value) {
    return;
  }
  const target = event.target as Node;
  const clickedTrigger = rootRef.value.contains(target);
  const clickedPanel = panelRef.value.contains(target);
  if (!clickedTrigger && !clickedPanel) {
    close();
  }
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === "Escape") {
    close();
  }
}

watch(open, (value) => {
  if (!value) {
    unbindFloatingListeners();
  }
});

provide(bzDropdownContextKey, {
  close,
});

onMounted(() => {
  document.addEventListener("mousedown", handleClickOutside);
  document.addEventListener("keydown", handleEscape);
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", handleClickOutside);
  document.removeEventListener("keydown", handleEscape);
  unbindFloatingListeners();
});
</script>

<style scoped>
.bz-dropdown {
  position: relative;
  display: inline-flex;
}

.bz-dropdown__trigger {
  display: inline-flex;
}

.bz-dropdown__panel {
  position: fixed;
  z-index: 2200;
  min-width: 120px;
  max-height: min(320px, 70vh);
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}
</style>
