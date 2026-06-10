<!-- /src/pages/HomePage.vue -->
<template>
  <div class="app-shell">
    <!-- ref 是模板引用语法：把这个 DOM 绑定到 script 里的 rootRef 变量。 -->
    <div
      ref="rootRef"
      class="main-container"
    >
      <!-- v-model 是双向绑定语法糖（等价于 :modelValue + @update:modelValue）。 -->
      <!-- :prop 是 v-bind 缩写，@event 是 v-on 缩写，用于绑定组件属性/监听事件。 -->
      <!-- auto-focus 是布尔 prop，裸写表示 true。@mountedInput 为组件自定义事件。 -->
      <SearchBox
        v-model="searchQuery"
        :badge-label="searchBadge.label"
        :badge-class="searchBadge.className"
        :placeholder="placeholder"
        :open="isOpen"
        auto-focus
        @focus="openPanel"
        @keydown="onKeyDown"
        @mounted-input="bindInputEl"
      />

      <!-- 自定义组件事件：@hover/@clickItem/@mountedScroll 来自 ResultsPanel 的 emits。 -->
      <ResultsPanel
        :open="isOpen"
        :groups="resultGroups"
        :selected-index="selectedIndex"
        @hover="onHover"
        @click-item="onClickItem"
        @mounted-scroll="bindScrollEl"
      />

      <div
        v-if="modalResource"
        class="resource-modal"
        @mousedown.self="closeModal"
      >
        <div class="modal-card">
          <div class="modal-header">
            <span class="modal-icon">{{ modalResource.icon || "🧩" }}</span>
            <span class="modal-title">{{ modalResource.name }}</span>
            <button
              class="modal-close"
              @click="closeModal"
            >
              关闭
            </button>
          </div>
          <div class="modal-body">
            <component
              :is="modalComponent"
              v-if="modalComponent"
            />
            <div
              v-else
              class="modal-missing"
            >
              未加载该资源组件。
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// <script setup> 是 Vue SFC 语法糖：顶层代码就是 setup() 的内容。
// lang="ts" 表示此处使用 TypeScript。
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";

import ResultsPanel from "../components/ResultsPanel.vue";
import SearchBox from "../components/SearchBox.vue";
import { useCommandPalette } from "../composables/useCommandPalette";
import { hasToolPageAccess } from "../registry/user-tool-permissions.registry";
import { getUserToolMap } from "../registry/user-tools.registry";
import type { ToolPageEntry } from "../types/user-tools";
import { resolveResourceComponent } from "../utils/resourceLoader";

// useRouter 来自 vue-router，提供路由跳转与解析能力。
const router = useRouter();
// ref<T>() 泛型：指定引用值类型。这里允许 HTMLElement 或 null。
const rootRef = ref<HTMLElement | null>(null);
const modalResource = ref<ToolPageEntry | null>(null);
const modalComponent = computed(() => resolveResourceComponent(modalResource.value?.loadTarget));
const resourceMap = computed(() => getUserToolMap());

function resolveMenuUrl(resource: ToolPageEntry): string {
  if (resource.url && resource.url.trim()) return resource.url.trim();
  return resource.code.startsWith("/") ? resource.code : `/${resource.code}`;
}

function openModal(resource: ToolPageEntry) {
  modalResource.value = resource;
}

function closeModal() {
  modalResource.value = null;
}

// 对象解构赋值：从 composable 返回对象里取出多个状态和函数。
const {
  searchQuery,
  isOpen,
  selectedIndex,
  placeholder,
  searchBadge,
  resultGroups,

  inputEl,
  scrollContainerEl,

  resolveSearchContext,
  openPanel,
  closePanel,
  onKeyDown,
  executeSelected,
  executeConfigAction,
} = useCommandPalette({
  onExecuteConfig: (config) => {
    if (!hasToolPageAccess(config, resourceMap.value)) return;

    const { isNewTab } = resolveSearchContext(searchQuery.value);

    if (config.openMode === "MODAL") {
      openModal(config);
      return;
    }
    if (config.openMode === "PAGE") {
      const path = resolveMenuUrl(config);
      if (!path) return;
      if (isNewTab) {
        const routeData = router.resolve({ path });
        window.open(routeData.href, "_blank");
      } else {
        router.push({ path, state: { fromHome: true } });
      }
      return;
    }

    executeConfigAction(config.code);
  },

  enableFuzzy: true,
  autoExecute: { enabled: true, delayMs: 250 },
});

// 显式参数类型：el 必须是 HTMLInputElement。
function bindInputEl(el: HTMLInputElement) {
  inputEl.value = el;
}

// 显式参数类型：el 必须是 HTMLElement。
function bindScrollEl(el: HTMLElement) {
  scrollContainerEl.value = el;
}

function onHover(index: number) {
  selectedIndex.value = index;
}

function onClickItem(index: number) {
  selectedIndex.value = index;
  executeSelected();
}

/**
 * 点击外部关闭面板（更稳）
 */
function onDocMouseDown(e: MouseEvent) {
  if (!isOpen.value) return;
  const root = rootRef.value;
  if (!root) return;
  // 类型断言：把 e.target 当作 Node 使用。
  const target = e.target as Node;
  if (!root.contains(target)) closePanel();
}

// onMounted/onUnmounted 是 Vue 生命周期钩子：组件挂载/卸载时触发。
onMounted(() => document.addEventListener("mousedown", onDocMouseDown));
onUnmounted(() => document.removeEventListener("mousedown", onDocMouseDown));

const onModalKeyDown = (event: KeyboardEvent) => {
  if (!modalResource.value) return;
  if (event.key === "Escape") closeModal();
};

onMounted(() => {
  window.addEventListener("keydown", onModalKeyDown);
});
onUnmounted(() => {
  window.removeEventListener("keydown", onModalKeyDown);
});
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 12vh;
}

.home-entry-bar {
  margin-top: 18px;
  display: flex;
  gap: 12px;
}
.home-entry-btn {
  height: 40px;
  padding: 0 16px;
  border: none;
  border-radius: 10px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
}
.home-entry-btn--ghost {
  background: #fff;
  color: #334155;
  border: 1px solid #dbe1ea;
}

.resource-modal {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 120;
  padding: 20px;
  box-sizing: border-box;
}

.modal-card {
  width: min(960px, 100%);
  max-height: 90vh;
  background: #fff;
  border-radius: 18px;
  border: 1px solid var(--border-color);
  box-shadow: 0 24px 35px -10px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-bottom: 1px solid #f1f5f9;
  font-weight: 800;
}

.modal-icon {
  font-size: 22px;
}

.modal-title {
  flex: 1;
}

.modal-close {
  border: 1px solid var(--border-color);
  background: #fff;
  border-radius: 10px;
  padding: 6px 12px;
  cursor: pointer;
}

.modal-body {
  padding: 16px;
  overflow: auto;
}

.modal-missing {
  color: var(--text-muted);
  font-style: italic;
}
</style>
