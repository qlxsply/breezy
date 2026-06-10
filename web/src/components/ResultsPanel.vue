<!-- /src/components/ResultsPanel.vue -->
<template>
  <div
    v-if="open"
    class="results-panel"
    @mousedown.stop
  >
    <div
      ref="scrollRef"
      class="results-list"
    >
      <div
        v-if="flatCount === 0"
        class="no-results"
      >
        <div class="no-results-icon">🔍</div>
        <div class="no-results-text">未找到匹配项 (尝试切换 指令/名称 模式)</div>
      </div>
      <!-- 分组渲染 -->
      <template
        v-for="(g, gi) in groups"
        v-else
        :key="`g-${gi}`"
      >
        <div
          v-for="(it, ii) in g.items"
          :key="keyOf(it, gi, ii)"
          class="result-item"
          :class="{ selected: flatIndexOf(gi, ii) === selectedIndex }"
          :data-idx="flatIndexOf(gi, ii)"
          @mouseenter="$emit('hover', flatIndexOf(gi, ii))"
          @click="$emit('clickItem', flatIndexOf(gi, ii))"
        >
          <div class="item-icon">{{ iconOf(it) }}</div>

          <div class="item-body">
            <span class="item-name">{{ nameOf(it) }}</span>
            <span class="item-shortcut">{{ shortcutOf(it) }}</span>
          </div>

          <div class="item-action">执行 ↵</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";

import type { ResultGroup, ResultItem } from "../types/user-tools";

const props = defineProps<{
  open: boolean;
  groups: ResultGroup[];
  selectedIndex: number;
}>();

const emit = defineEmits<{
  (e: "hover", index: number): void;
  (e: "clickItem", index: number): void;
  (e: "mountedScroll", el: HTMLElement): void;
}>();

const scrollRef = ref<HTMLElement | null>(null);

onMounted(() => {
  if (scrollRef.value) emit("mountedScroll", scrollRef.value);
});

function ensureSelectedVisible() {
  if (!props.open || !scrollRef.value) {
    return;
  }
  if (props.selectedIndex < 0) {
    return;
  }
  nextTick(() => {
    const container = scrollRef.value;
    if (!container) {
      return;
    }
    const el = container.querySelector<HTMLElement>(`[data-idx="${props.selectedIndex}"]`);
    if (!el) {
      return;
    }
    el.scrollIntoView({ block: "nearest" });
  });
}

watch(
  () => [props.selectedIndex, props.groups, props.open],
  () => {
    ensureSelectedVisible();
  },
  { immediate: true },
);

const flatCount = computed(() => props.groups.reduce((sum, g) => sum + g.items.length, 0));

function flatIndexOf(gi: number, ii: number): number {
  let base = 0;
  for (let i = 0; i < gi; i++) base += props.groups[i].items.length;
  return base + ii;
}

function keyOf(it: ResultItem, gi: number, ii: number) {
  return `${it.kind}:${it.resource.code}:${gi}:${ii}`;
}

function iconOf(it: ResultItem) {
  return it.resource.icon || "⚙️";
}

function nameOf(it: ResultItem) {
  return it.resource.name;
}

function shortcutOf(it: ResultItem) {
  return it.resource.code;
}
</script>

<style scoped>
.results-panel {
  margin-top: 12px;
  width: min(900px, calc(100% - 40px));
  background: #fff;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 100;
}

.results-list {
  max-height: 520px;
  overflow-y: auto;
}

.no-results {
  padding: 60px 20px;
  text-align: center;
  color: var(--text-muted);
}
.no-results-icon {
  font-size: 40px;
  margin-bottom: 12px;
  opacity: 0.5;
}
.no-results-text {
  font-size: 14px;
  font-weight: 500;
}

.result-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  transition: all 0.1s;
  border-bottom: 1px solid #f8fafc;
}

.result-item.selected {
  background-color: var(--highlight-bg);
  border-left: 4px solid var(--primary-color);
  padding-left: 16px;
}

.item-icon {
  font-size: 26px;
  margin-right: 18px;
  width: 34px;
  text-align: center;
}

.item-body {
  flex: 1;
}

.item-name {
  font-weight: 600;
  font-size: 15px;
}

.item-shortcut {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: 8px;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

.item-action {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
}
</style>
