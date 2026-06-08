<template>
  <div
    ref="containerRef"
    class="json-diff-viewer"
    :style="viewerStyle"
  >
    <div class="diff-header">
      <div class="diff-name left">{{ leftLabel }}</div>
      <div
        class="split-column head"
        aria-hidden="true"
      ></div>
      <div class="diff-name right">{{ rightLabel }}</div>
    </div>

    <div class="diff-body">
      <div
        v-for="(row, index) in rows"
        :key="index"
        class="diff-row"
      >
        <div
          class="diff-side left"
          :class="`is-${row.leftKind}`"
        >
          <span class="line-no">{{ row.leftLineNo ?? "" }}</span>
          <span class="line-text">{{ row.leftText }}</span>
        </div>
        <div
          class="split-column"
          aria-hidden="true"
        ></div>
        <div
          class="diff-side right"
          :class="`is-${row.rightKind}`"
        >
          <span class="line-no">{{ row.rightLineNo ?? "" }}</span>
          <span class="line-text">{{ row.rightText }}</span>
        </div>
      </div>
    </div>

    <div
      class="drag-line"
      role="separator"
      aria-label="拖动调整左右宽度"
      @mousedown="startResize"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from "vue";

interface JsonDiffLineRow {
  leftLineNo: number | null;
  rightLineNo: number | null;
  leftText: string;
  rightText: string;
  leftKind: "same" | "removed" | "added" | "changed" | "placeholder";
  rightKind: "same" | "removed" | "added" | "changed" | "placeholder";
}

defineProps<{
  leftLabel: string;
  rightLabel: string;
  rows: JsonDiffLineRow[];
}>();

const containerRef = ref<HTMLElement | null>(null);
const leftWidthPercent = ref(50);
const MIN_WIDTH = 20;
const MAX_WIDTH = 80;

const viewerStyle = computed(() => ({
  "--left-width": `${leftWidthPercent.value}%`,
}));

function startResize(event: MouseEvent) {
  event.preventDefault();
  window.addEventListener("mousemove", onResizeMove);
  window.addEventListener("mouseup", stopResize);
}

function onResizeMove(event: MouseEvent) {
  const container = containerRef.value;
  if (!container) {
    return;
  }
  const rect = container.getBoundingClientRect();
  if (rect.width <= 0) {
    return;
  }
  const relativeX = event.clientX - rect.left;
  const percent = (relativeX / rect.width) * 100;
  leftWidthPercent.value = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, percent));
}

function stopResize() {
  window.removeEventListener("mousemove", onResizeMove);
  window.removeEventListener("mouseup", stopResize);
}

onBeforeUnmount(() => {
  stopResize();
});
</script>

<style scoped>
.json-diff-viewer {
  --split-width: 8px;
  position: relative;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #ffffff;
  overflow: hidden;
  min-height: 0;
  height: 100%;
}

.diff-header {
  display: grid;
  grid-template-columns: var(--left-width) var(--split-width) calc(
      100% - var(--left-width) - var(--split-width)
    );
  border-bottom: 1px solid #dbe3ef;
  background: #f8fafc;
}

.diff-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 8px 10px;
  font-size: 12px;
  color: #334155;
  font-weight: 700;
}

.diff-body {
  height: calc(100% - 34px);
  overflow: auto;
  font-family: SFMono-Regular, Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 12px;
  line-height: 1.7;
}

.diff-row {
  display: grid;
  grid-template-columns: var(--left-width) var(--split-width) calc(
      100% - var(--left-width) - var(--split-width)
    );
}

.diff-side {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: stretch;
  border-bottom: 1px solid #eef2f7;
  min-width: 0;
}

.split-column {
  border-bottom: 1px solid #eef2f7;
  background: #f1f5f9;
}

.split-column.head {
  border-bottom: none;
  border-left: 1px solid #dbe3ef;
  border-right: 1px solid #dbe3ef;
}

.line-no {
  text-align: right;
  padding: 0 8px 0 4px;
  color: #94a3b8;
  user-select: none;
  border-right: 1px solid #eef2f7;
}

.line-text {
  padding: 0 10px;
  white-space: pre;
  overflow-x: auto;
}

.is-removed {
  background: #fee2e2;
}

.is-added {
  background: #dcfce7;
}

.left.is-changed {
  background: #fee2e2;
}

.right.is-changed {
  background: #dcfce7;
}

.is-placeholder {
  background: repeating-linear-gradient(-45deg, #f8fafc, #f8fafc 6px, #f1f5f9 6px, #f1f5f9 12px);
}

.drag-line {
  position: absolute;
  top: 0;
  bottom: 0;
  left: calc(var(--left-width) - (var(--split-width) / 2));
  width: var(--split-width);
  cursor: col-resize;
  background: rgba(59, 130, 246, 0.06);
  transition: background-color 0.16s ease;
}

.drag-line:hover {
  background: rgba(59, 130, 246, 0.2);
}
</style>
