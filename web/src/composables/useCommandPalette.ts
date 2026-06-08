// /src/composables/useCommandPalette.ts
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";

import { ensureRegistryLoaded } from "../registry/bootstrap";
import { getConfigAction } from "../registry/configs.registry";
import { ensurePermissionsLoaded, hasMenuAccess } from "../registry/permissions.registry";
import { getResourceMap, getResources } from "../registry/resources.registry";
import type {
  ConfigActionContext,
  MenuResource,
  ResultGroup,
  ResultItem,
  SearchBadge,
} from "../types/command";
import { message } from "../utils/message";
import { DEFAULT_SCORE_OPTIONS, scoreText } from "../utils/search";

/**
 * 提升 searchMode 到模块级作用域并增加持久化
 * 确保在非页面刷新的情况下，切换的模式能够持久化，刷新后通过 localStorage 恢复
 */
const SEARCH_MODE_KEY = "breezy_search_mode";
const storedSearchMode = localStorage.getItem(SEARCH_MODE_KEY);
const initialSearchMode =
  storedSearchMode === "name" || storedSearchMode === "shortcut" ? storedSearchMode : "shortcut";
const searchMode = ref<"shortcut" | "name">(initialSearchMode);

watch(searchMode, (val) => {
  localStorage.setItem(SEARCH_MODE_KEY, val);
});

/**
 * 命令面板 Composable
 * - 统一评分（完全匹配/前缀/includes/fuzzy）
 * - 输出 resultGroups（用于分组UI）
 */
export function useCommandPalette(options: {
  onExecuteConfig: (resource: MenuResource) => void;
  autoExecute?: { enabled: boolean; delayMs: number };
  enableFuzzy?: boolean;
}) {
  const searchQuery = ref("");
  const isOpen = ref(false);
  const selectedIndex = ref(0);

  const inputEl = ref<HTMLInputElement | null>(null);
  const scrollContainerEl = ref<HTMLElement | null>(null);

  const hasTriggeredLoad = ref(false);

  // 统一触发资源和权限加载
  const triggerLoad = async () => {
    if (hasTriggeredLoad.value) return;
    hasTriggeredLoad.value = true;
    try {
      await Promise.all([ensurePermissionsLoaded(), ensureRegistryLoaded()]);
    } catch (e) {
      console.error("[command-palette] init load failed", e);
    }
  };

  // 监听输入触发加载和面板唤醒
  watch(searchQuery, (val) => {
    if (val.trim()) {
      triggerLoad();
      // 只要有有效输入，确保面板是打开的（解决 md 控制指令后焦点未丢失但面板关闭的问题）
      if (!isOpen.value) isOpen.value = true;
    }
  });

  const scoreOpt = computed(() => ({
    ...DEFAULT_SCORE_OPTIONS,
    enableFuzzy: options.enableFuzzy ?? true,
  }));

  const resourceMap = computed(() => getResourceMap());

  /**
   * 预过滤全量可用菜单，仅包含 MODAL 和 PAGE
   */
  const toolMenus = computed(() => {
    const map = resourceMap.value;
    return (getResources() as MenuResource[]).filter((r) => {
      if (r.type !== "MENU") return false;
      if (r.openMode !== "MODAL" && r.openMode !== "PAGE") return false;
      if (r.scope !== "TOOL") return false;
      return hasMenuAccess(r, map);
    });
  });

  const searchBadge = computed<SearchBadge>(() => {
    if (searchQuery.value.trimStart().startsWith("+")) {
      return { label: "工具+", type: "tool", className: "tool" };
    }
    const raw = searchQuery.value.trimStart();
    if (raw.startsWith("++")) return { label: "工具++", type: "tool", className: "tool" };
    if (raw.startsWith("+")) return { label: "工具", type: "tool", className: "tool" };

    return {
      label: searchMode.value === "shortcut" ? "工具指令" : "工具名称",
      type: "tool",
      className: "tool",
    };
  });

  const placeholder = computed(() => {
    return searchMode.value === "shortcut" ? "输入工具编码进行匹配" : "输入工具名称关键字";
  });

  function resolveSearchContext(raw: string): {
    target: "tool";
    typed: string;
    isNewTab: boolean;
  } {
    const trimmedLeft = raw.trimStart();
    if (trimmedLeft.startsWith("+")) {
      return {
        target: "tool",
        typed: trimmedLeft.replace(/^\++/, "").trim(),
        isNewTab: true,
      };
    }
    if (trimmedLeft.startsWith("++")) {
      return { target: "tool", typed: trimmedLeft.slice(2).trim(), isNewTab: true };
    }
    if (trimmedLeft.startsWith("+")) {
      return { target: "tool", typed: trimmedLeft.slice(1).trim(), isNewTab: false };
    }
    return { target: "tool", typed: raw.trim(), isNewTab: false };
  }

  const resultGroups = computed<ResultGroup[]>(() => {
    const raw = searchQuery.value;
    const { typed } = resolveSearchContext(raw);
    const typedLower = typed.toLowerCase();
    const normalizedRaw = raw.trim().toLowerCase();

    const scopeMenus = toolMenus.value;
    const groupTitle = typed ? "工具匹配结果" : "工具";

    // 1. 如果没有输入搜索词，直接展示该分类下的全部项（按 orderNo 排序）
    if (!typed) {
      const items = [...scopeMenus]
        .sort((a, b) => (a.orderNo ?? 0) - (b.orderNo ?? 0) || a.name.localeCompare(b.name))
        .map<ResultItem>((item) => ({
          kind: item.scope.toLowerCase(),
          resource: item,
          score: 1,
        }));
      return [{ title: groupTitle, items }];
    }

    // 2. 执行打分匹配
    const isControlCommand = normalizedRaw === "md" || normalizedRaw === "abt";

    const scored = scopeMenus
      .map((s) => {
        const codeRaw = (s.code || "").toLowerCase();
        const nameRaw = (s.name || "").toLowerCase();
        const codeClean = codeRaw.startsWith("/") ? codeRaw.slice(1) : codeRaw;
        const typedClean = typedLower.startsWith("/") ? typedLower.slice(1) : typedLower;

        let score = 0;
        // 特殊控制指令直接最高分
        if (isControlCommand && (codeRaw === normalizedRaw || codeRaw === typedLower)) {
          score = 5000;
        } else if (searchMode.value === "shortcut") {
          score = scoreText(codeClean, typedClean, scoreOpt.value);
          if (codeRaw === typedLower || codeClean === typedClean) score += 1000;
        } else {
          score = scoreText(nameRaw, typedLower, scoreOpt.value);
          // 名称模式下，如果输入的是指令代码，也给一点基础分
          if (codeClean.includes(typedClean)) score += 10;
        }
        return { item: s, score };
      })
      .filter((x) => x.score > 0)
      .sort((a, b) => b.score - a.score || (a.item.orderNo ?? 0) - (b.item.orderNo ?? 0))
      .map<ResultItem>((x) => ({
        kind: x.item.scope.toLowerCase(),
        resource: x.item,
        score: x.score,
      }));

    return [{ title: scored.length > 0 ? "工具匹配结果" : "未找到可用工具", items: scored }];
  });

  const flatList = computed<ResultItem[]>(() => {
    return resultGroups.value.flatMap((g) => g.items);
  });

  watch(flatList, () => {
    if (flatList.value.length === 0) {
      selectedIndex.value = 0;
      return;
    }
    if (selectedIndex.value < 0 || selectedIndex.value >= flatList.value.length) {
      selectedIndex.value = 0;
    }
  });

  function moveSelection(step: number) {
    const total = flatList.value.length;
    if (total === 0) return;
    const next = selectedIndex.value + step;
    if (next < 0) {
      selectedIndex.value = total - 1;
    } else if (next >= total) {
      selectedIndex.value = 0;
    } else {
      selectedIndex.value = next;
    }
    ensureSelectedVisible();
  }

  function onKeyDown(e: KeyboardEvent) {
    if (!isOpen.value) return;

    if (e.key === "Tab") {
      e.preventDefault();
      focusInput();
      return;
    }

    if (e.key === "ArrowDown") {
      e.preventDefault();
      moveSelection(1);
      return;
    }

    if (e.key === "ArrowUp") {
      e.preventDefault();
      moveSelection(-1);
      return;
    }

    if (e.key === "Enter") {
      e.preventDefault();
      executeSelected();
      return;
    }

    if (e.key === "Escape") {
      e.preventDefault();
      closePanel();
    }
  }

  function ensureSelectedVisible() {
    nextTick(() => {
      const container = scrollContainerEl.value;
      if (!container) return;
      const el = container.querySelector<HTMLElement>(`[data-idx="${selectedIndex.value}"]`);
      if (!el) return;
      el.scrollIntoView({ block: "nearest" });
    });
  }

  function executeSelected() {
    const item = flatList.value[selectedIndex.value];
    if (!item) return;

    const code = item.resource.code;
    const isControl = code === "md" || code === "abt";

    options.onExecuteConfig(item.resource);

    if (isControl) {
      // 如果是控制指令，执行后不关闭面板，清空输入并保留前缀（如果是以斜杠开始的）
      const raw = searchQuery.value.trim();
      searchQuery.value = raw.startsWith("/") ? "/" : "";
      // 保持面板开启，并让输入框保持焦点
      isOpen.value = true;
      nextTick(() => focusInput());
    } else {
      searchQuery.value = "";
      closePanel();
    }
  }

  function openPanel() {
    isOpen.value = true;
    triggerLoad(); // 打开面板即尝试加载
  }
  function closePanel() {
    isOpen.value = false;
  }
  function focusInput() {
    inputEl.value?.focus();
  }

  function globalKeyHandler(e: KeyboardEvent) {
    const hasModal = document.querySelector(".mask, .modal, .dialog");
    if (hasModal) return;

    if (e.key === "Tab") {
      const active = document.activeElement;
      const isTyping =
        active &&
        (active.tagName === "INPUT" ||
          active.tagName === "TEXTAREA" ||
          active.tagName === "SELECT");
      if (isTyping && active !== inputEl.value) return;
      e.preventDefault();
      openPanel();
      focusInput();
      return;
    }

    const active = document.activeElement;
    if (active && (active.tagName === "INPUT" || active.tagName === "TEXTAREA")) {
      // 唤醒面板：针对 + / ? 等字符进行特殊检查
      if (!isOpen.value && /^[a-zA-Z0-9/\-?+]$/.test(e.key)) {
        isOpen.value = true;
      }
      return;
    }

    if (/^[a-zA-Z0-9/\-?+]$/.test(e.key)) {
      openPanel();
      focusInput();
    }
    if (e.key === "Escape") closePanel();
  }

  onMounted(() => window.addEventListener("keydown", globalKeyHandler));
  onUnmounted(() => window.removeEventListener("keydown", globalKeyHandler));

  const configCtx: ConfigActionContext = {
    toggleSearchMode: () => {
      searchMode.value = searchMode.value === "shortcut" ? "name" : "shortcut";
    },
    showAbout: () => {
      message.info("极简工具箱Pro（Vue3 + TS + 分组搜索版）");
    },
  };

  function executeConfigAction(code: string) {
    const found = getConfigAction(code);
    if (!found) return;
    found.action(configCtx);
  }

  return {
    searchQuery,
    isOpen,
    selectedIndex,
    searchMode,
    inputEl,
    scrollContainerEl,
    placeholder,
    searchBadge,
    resultGroups,
    flatList,
    resolveSearchContext,
    openPanel,
    closePanel,
    focusInput,
    onKeyDown,
    executeSelected,
    executeConfigAction,
  };
}
