<template>
  <div class="grant-tree">
    <div class="toolbar">
      <bz-input
        v-model="keyword"
        placeholder="搜索菜单、功能、按钮或权限码"
        clearable
      />
      <div class="toolbar-meta">已选 {{ selectedSet.size }} 项</div>
    </div>

    <div
      v-loading="loading"
      class="tree-panel"
    >
      <bz-empty
        v-if="!loading && treeRows.length === 0"
        description="暂无可授权资源"
      />

      <div
        v-else
        class="tree-list"
      >
        <div
          v-for="item in treeRows"
          :key="item.row.id"
          class="tree-row"
          :class="{ disabled: !item.row.enabled }"
        >
          <div
            class="tree-cell"
            :style="indentStyle(item.depth)"
          >
            <bz-checkbox
              v-if="item.row.selectable"
              :model-value="isChecked(item.row)"
              :disabled="isDisabled(item.row)"
              @change="() => toggle(item.row)"
            />
            <span
              v-else
              class="tree-placeholder"
            />

            <div class="tree-copy">
              <div class="tree-main">
                <span class="tree-name">{{ item.row.name }}</span>
                <span class="tree-code">{{ item.row.code }}</span>
                <bz-tag
                  size="small"
                  :type="typeTagType(item.row.type)"
                >
                  {{ typeLabel(item.row.type) }}
                </bz-tag>
                <bz-tag
                  v-if="!item.row.enabled"
                  size="small"
                  type="warning"
                >
                  已停用
                </bz-tag>
                <span
                  v-if="!item.row.selectable && hasSelectedDescendant(item.row.id)"
                  class="tree-hint"
                >
                  含已授权子项
                </span>
              </div>

              <div
                v-if="item.row.description"
                class="tree-desc"
              >
                {{ item.row.description }}
              </div>

              <div
                v-if="item.row.permissionCodes.length"
                class="tree-permissions"
              >
                <bz-tag
                  v-for="permissionCode in item.row.permissionCodes"
                  :key="permissionCode"
                  size="small"
                  type="info"
                >
                  {{ permissionCode }}
                </bz-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";

import type { RoleGrantResourceEntry } from "../../types/role-admin";

const props = defineProps<{
  resources: RoleGrantResourceEntry[];
  modelValue: string[];
  loading?: boolean;
  canEdit?: boolean;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: string[]): void;
}>();

const keyword = ref("");
const selectedSet = ref(new Set<string>());

watch(
  () => props.modelValue,
  (next) => {
    selectedSet.value = new Set((next || []).map((item) => String(item)));
  },
  { immediate: true },
);

const resourceMap = computed(() => {
  const map = new Map<string, RoleGrantResourceEntry>();
  props.resources.forEach((resource) => map.set(resource.id, resource));
  return map;
});

const childrenMap = computed(() => {
  const map = new Map<string | null, RoleGrantResourceEntry[]>();
  const idSet = new Set(props.resources.map((resource) => resource.id));

  const addChild = (parentId: string | null, row: RoleGrantResourceEntry) => {
    const list = map.get(parentId) ?? [];
    list.push(row);
    map.set(parentId, list);
  };

  props.resources.forEach((row) => {
    const rawParent = row.parentId ?? null;
    const parentId = rawParent && idSet.has(rawParent) ? rawParent : null;
    addChild(parentId, row);
  });

  map.forEach((rows) => {
    rows.sort((left, right) => {
      if (left.orderNo !== right.orderNo) return left.orderNo - right.orderNo;
      return left.name.localeCompare(right.name);
    });
  });

  return map;
});

const visibleNodeIds = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) {
    return null;
  }
  const matchedIds = new Set<string>();
  props.resources.forEach((row) => {
    const text = [row.name, row.code, row.description, ...row.permissionCodes]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    if (text.includes(kw)) {
      matchedIds.add(row.id);
      let parentId = row.parentId ?? null;
      while (parentId) {
        matchedIds.add(parentId);
        parentId = resourceMap.value.get(parentId)?.parentId ?? null;
      }
    }
  });
  return matchedIds;
});

const treeRows = computed(() => {
  const flat: Array<{ row: RoleGrantResourceEntry; depth: number }> = [];
  const visibleIds = visibleNodeIds.value;

  const walk = (parentId: string | null, depth: number) => {
    const list = childrenMap.value.get(parentId);
    if (!list) return;
    list.forEach((row) => {
      if (visibleIds && !visibleIds.has(row.id)) {
        return;
      }
      flat.push({ row, depth });
      walk(row.id, depth + 1);
    });
  };

  walk(null, 0);
  return flat;
});

function isChecked(row: RoleGrantResourceEntry) {
  if (!row.functionId) {
    return hasSelectedDescendant(row.id);
  }
  return selectedSet.value.has(row.functionId);
}

function isDisabled(row: RoleGrantResourceEntry) {
  return props.canEdit === false || !row.enabled || !row.selectable || !row.functionId;
}

function hasSelectedDescendant(nodeId: string): boolean {
  const children = childrenMap.value.get(nodeId);
  if (!children || children.length === 0) {
    return false;
  }
  return children.some((child) => {
    if (child.functionId && selectedSet.value.has(child.functionId)) {
      return true;
    }
    return hasSelectedDescendant(child.id);
  });
}

function collectSelectableDescendantFunctionIds(nodeId: string, collector: Set<string>): void {
  const children = childrenMap.value.get(nodeId);
  if (!children || children.length === 0) {
    return;
  }
  children.forEach((child) => {
    if (child.selectable && child.functionId && child.enabled) {
      collector.add(child.functionId);
    }
    collectSelectableDescendantFunctionIds(child.id, collector);
  });
}

function toggle(row: RoleGrantResourceEntry) {
  if (isDisabled(row) || !row.functionId) {
    return;
  }
  const next = new Set(selectedSet.value);
  const related = new Set<string>([row.functionId]);
  collectSelectableDescendantFunctionIds(row.id, related);
  if (next.has(row.functionId)) {
    related.forEach((functionId) => next.delete(functionId));
  } else {
    related.forEach((functionId) => next.add(functionId));
  }
  selectedSet.value = next;
  emit("update:modelValue", Array.from(next));
}

function indentStyle(depth: number) {
  if (!depth) return undefined;
  return { paddingLeft: `${depth * 18}px` };
}

function typeLabel(type: string) {
  switch ((type || "").toUpperCase()) {
    case "MENU":
      return "菜单";
    case "PAGE":
      return "页面";
    case "QUERY":
      return "查询";
    case "ACTION":
      return "操作";
    case "BUTTON":
      return "按钮";
    case "INVISIBLE":
      return "隐藏";
    default:
      return type || "-";
  }
}

function typeTagType(type: string): "info" | "success" | "warning" | "primary" {
  switch ((type || "").toUpperCase()) {
    case "BUTTON":
      return "warning";
    case "ACTION":
      return "success";
    case "MENU":
      return "info";
    default:
      return "primary";
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-meta {
  color: var(--text-muted);
  font-size: 12px;
  white-space: nowrap;
}

.tree-panel {
  margin-top: 12px;
  max-height: 540px;
  overflow: auto;
}

.tree-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tree-row {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
}

.tree-row.disabled {
  opacity: 0.7;
}

.tree-cell {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
}

.tree-placeholder {
  width: 16px;
  flex: 0 0 16px;
}

.tree-copy {
  flex: 1;
  min-width: 0;
}

.tree-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.tree-name {
  font-size: 13px;
  font-weight: 800;
  color: var(--text-main);
}

.tree-code {
  font-size: 11px;
  color: var(--text-muted);
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.tree-desc {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.6;
}

.tree-permissions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tree-hint {
  font-size: 11px;
  color: #0369a1;
  background: #e0f2fe;
  border-radius: 999px;
  padding: 2px 8px;
  font-weight: 700;
}
</style>
