<template>
  <bz-dialog
    :model-value="true"
    :title="confirming ? `确认角色权限变更 - ${roleName}` : `角色权限分配 - ${roleName}`"
    width="980px"
    class="role-permission-dialog"
    @close="$emit('close')"
  >
    <div
      v-if="!confirming"
      class="permission-dialog-shell"
    >
      <div class="permission-dialog-toolbar">
        <bz-input
          v-model="keyword"
          placeholder="搜索目录、菜单、按钮或权限码"
          clearable
        />
        <bz-button
          class="permission-toolbar-button"
          @click="expandAll"
        >
          全部展开
        </bz-button>
        <bz-button
          class="permission-toolbar-button"
          @click="collapseAll"
        >
          全部收起
        </bz-button>
        <bz-button
          class="permission-toolbar-button"
          @click="clearAll"
        >
          清空选择
        </bz-button>
      </div>

      <section class="permission-panel">
        <div class="permission-panel__head">
          <div>
            <div class="permission-panel__title">可选权限</div>
            <div class="permission-panel__hint">
              目录仅作为分组展示；选择按钮权限时会自动带上所属菜单；选择菜单不会自动选择按钮。
            </div>
          </div>
          <div class="permission-panel__meta">
            {{ filteredResourceCount }} / {{ resources.length }} 项
          </div>
        </div>

        <div
          v-loading="loading"
          class="permission-tree-wrap"
        >
          <div
            v-if="filteredRoots.length === 0"
            class="permission-empty"
          >
            暂无可授权资源
          </div>

          <RolePermissionTreeNode
            v-for="node in filteredRoots"
            :key="node.row.id"
            :node="node"
            :expanded-ids="displayExpandedIds"
            :selected-ids="selectedNodeIds"
            :can-edit="canSave !== false"
            @toggle-expand="toggleExpand"
            @toggle-select="onToggleSelect"
          />
        </div>
      </section>
    </div>

    <div
      v-else
      class="permission-dialog-shell"
    >
      <section class="permission-panel">
        <div class="permission-panel__head">
          <div>
            <div class="permission-panel__title">确认权限变更</div>
            <div class="permission-panel__hint">
              绿色边框表示新增权限；红色删除线表示移除权限；未变化节点仅作为层级路径展示。
            </div>
          </div>
          <div class="permission-panel__meta">
            {{ diffSummaryText }}
          </div>
        </div>

        <div class="permission-tree-wrap">
          <div
            v-if="diffRoots.length === 0"
            class="permission-empty"
          >
            权限未发生变更
          </div>

          <RolePermissionTreeNode
            v-for="node in diffRoots"
            :key="node.row.id"
            :node="node"
            :expanded-ids="diffExpandedIds"
            :selected-ids="diffSelectedNodeIds"
            :can-edit="false"
            :diff-status-by-id="diffStatusById"
            readonly
          />
        </div>
      </section>
    </div>

    <template #footer>
      <div class="permission-dialog-footer">
        <div class="permission-dialog-footer__summary">
          {{ confirming ? "确认保存后，受影响用户需要重新登录后权限才会完全生效。" : summaryText }}
        </div>
        <div class="permission-dialog-footer__actions">
          <template v-if="!confirming">
            <bz-button @click="$emit('close')">取消</bz-button>
            <bz-button
              v-if="canSave !== false"
              type="primary"
              @click="submit"
            >
              保存
            </bz-button>
          </template>

          <template v-else>
            <bz-button @click="backToEdit">返回</bz-button>
            <bz-button
              type="primary"
              @click="confirmSubmit"
            >
              确认
            </bz-button>
          </template>
        </div>
      </div>
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";

import type { RoleGrantResourceEntry, RoleGrantSelection } from "../../types/role-admin";
import RolePermissionTreeNode, {
  type RolePermissionTreeNodeView,
} from "./RolePermissionTreeNode.vue";

type DiffStatus = "added" | "removed";

const props = defineProps<{
  roleName: string;
  resources: RoleGrantResourceEntry[];
  selection: RoleGrantSelection;
  loading?: boolean;
  canSave?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "submit", selection: RoleGrantSelection): void;
}>();

const keyword = ref("");
const expandedIds = ref(new Set<string>());
const confirming = ref(false);
const pendingSelection = ref<RoleGrantSelection | null>(null);

const selectedMenuNodeIds = ref(new Set<string>());
const selectedFunctionNodeIds = ref(new Set<string>());

const resourceMap = computed(() => {
  const map = new Map<string, RoleGrantResourceEntry>();
  props.resources.forEach((row) => map.set(row.id, row));
  return map;
});

const childrenMap = computed(() => {
  const map = new Map<string | null, RoleGrantResourceEntry[]>();

  const addChild = (parentId: string | null, row: RoleGrantResourceEntry) => {
    const list = map.get(parentId) ?? [];
    list.push(row);
    map.set(parentId, list);
  };

  props.resources.forEach((row) => addChild(row.parentId ?? null, row));

  map.forEach((rows) => {
    rows.sort((left, right) => left.orderNo - right.orderNo || left.name.localeCompare(right.name));
  });

  return map;
});

watch(
  () => [props.selection, props.resources] as const,
  () => {
    const selected = buildSelectedNodeIdSets();

    selectedMenuNodeIds.value = selected.menuNodeIds;
    selectedFunctionNodeIds.value = selected.functionNodeIds;
    expandedIds.value = new Set<string>();
    confirming.value = false;
    pendingSelection.value = null;
  },
  { immediate: true },
);

const selectedNodeIds = computed(() => {
  const result = new Set<string>();

  selectedMenuNodeIds.value.forEach((id) => result.add(id));
  selectedFunctionNodeIds.value.forEach((id) => {
    result.add(id);

    collectAncestorIds(id).forEach((ancestorId) => {
      const ancestor = resourceMap.value.get(ancestorId);
      if (ancestor?.type === "MENU") {
        result.add(ancestorId);
      }
    });
  });

  return result;
});

const keywordText = computed(() => keyword.value.trim().toLowerCase());

const filteredTreeResult = computed(() => {
  const autoExpandedIds = new Set<string>();
  const roots = buildTree(null, keywordText.value, autoExpandedIds);

  return {
    roots,
    autoExpandedIds,
  };
});

const filteredRoots = computed(() => filteredTreeResult.value.roots);

const displayExpandedIds = computed(() => {
  if (!keywordText.value) {
    return expandedIds.value;
  }

  return new Set<string>([...expandedIds.value, ...filteredTreeResult.value.autoExpandedIds]);
});

const filteredResourceCount = computed(() => {
  let count = 0;

  walkTree(filteredRoots.value, () => {
    count += 1;
  });

  return count;
});

const summarySelection = computed(() => buildSubmitSelection());

const summaryText = computed(() => {
  const menuCount = summarySelection.value.menuIds.length;
  const functionCount = summarySelection.value.functionIds.length;

  if (menuCount === 0 && functionCount === 0) {
    return "未选择权限";
  }

  return `已选择菜单 ${menuCount} 项，按钮权限 ${functionCount} 项`;
});

const diff = computed(() =>
  buildSelectionDiff(props.selection, pendingSelection.value ?? summarySelection.value),
);

const diffStatusById = computed(() => {
  const map = new Map<string, DiffStatus>();

  props.resources.forEach((resource) => {
    if (resource.type === "MENU" && resource.menuId) {
      if (diff.value.addedMenuIds.has(resource.menuId)) {
        map.set(resource.id, "added");
      } else if (diff.value.removedMenuIds.has(resource.menuId)) {
        map.set(resource.id, "removed");
      }
    }

    if (resource.type === "BUTTON" && resource.functionId) {
      if (diff.value.addedFunctionIds.has(resource.functionId)) {
        map.set(resource.id, "added");
      } else if (diff.value.removedFunctionIds.has(resource.functionId)) {
        map.set(resource.id, "removed");
      }
    }
  });

  return map;
});

const diffRoots = computed(() => buildDiffTree(null));

const diffExpandedIds = computed(() => {
  const ids = new Set<string>();

  walkTree(diffRoots.value, (node) => {
    if (node.children.some((child) => child.row.type !== "BUTTON")) {
      ids.add(node.row.id);
    }
  });

  return ids;
});

const diffSelectedNodeIds = computed(() => {
  const ids = new Set<string>();

  buildNodeIdsFromSelection(props.selection).forEach((id) => ids.add(id));
  buildNodeIdsFromSelection(pendingSelection.value ?? summarySelection.value).forEach((id) =>
    ids.add(id),
  );

  return ids;
});

const diffSummaryText = computed(() => {
  const addedCount = diff.value.addedMenuIds.size + diff.value.addedFunctionIds.size;
  const removedCount = diff.value.removedMenuIds.size + diff.value.removedFunctionIds.size;

  return `新增 ${addedCount} 项 / 移除 ${removedCount} 项`;
});

function buildSelectedNodeIdSets(): {
  menuNodeIds: Set<string>;
  functionNodeIds: Set<string>;
} {
  const menuNodeIds = new Set<string>();
  const functionNodeIds = new Set<string>();

  const menuIdSet = new Set((props.selection.menuIds || []).map(String));
  const functionIdSet = new Set((props.selection.functionIds || []).map(String));

  props.resources.forEach((resource) => {
    if (resource.functionId && functionIdSet.has(resource.functionId)) {
      functionNodeIds.add(resource.id);
    }
  });

  const functionAncestorMenuNodeIds = new Set<string>();

  functionNodeIds.forEach((functionNodeId) => {
    collectAncestorIds(functionNodeId).forEach((ancestorId) => {
      const ancestor = resourceMap.value.get(ancestorId);
      if (ancestor?.type === "MENU") {
        functionAncestorMenuNodeIds.add(ancestorId);
      }
    });
  });

  props.resources.forEach((resource) => {
    if (!resource.menuId || !menuIdSet.has(resource.menuId)) {
      return;
    }

    if (resource.type !== "MENU") {
      return;
    }

    if (functionAncestorMenuNodeIds.has(resource.id)) {
      return;
    }

    menuNodeIds.add(resource.id);
  });

  return {
    menuNodeIds,
    functionNodeIds,
  };
}

function buildTree(
  parentId: string | null,
  kw: string,
  autoExpandedIds: Set<string>,
): RolePermissionTreeNodeView[] {
  const rows = childrenMap.value.get(parentId) ?? [];

  return rows
    .map((row) => {
      const children = buildTree(row.id, kw, autoExpandedIds);
      const matched =
        !kw ||
        [row.name, row.code, row.description, ...(row.permissionCodes || [])]
          .filter(Boolean)
          .join(" ")
          .toLowerCase()
          .includes(kw);

      if (!matched && children.length === 0) {
        return null;
      }

      if (kw && children.length > 0) {
        autoExpandedIds.add(row.id);
      }

      return { row, children } satisfies RolePermissionTreeNodeView;
    })
    .filter((item): item is RolePermissionTreeNodeView => Boolean(item));
}

function buildDiffTree(parentId: string | null): RolePermissionTreeNodeView[] {
  const rows = childrenMap.value.get(parentId) ?? [];

  return rows
    .map((row) => {
      const children = buildDiffTree(row.id);
      const changed = diffStatusById.value.has(row.id);

      if (!changed && children.length === 0) {
        return null;
      }

      return { row, children } satisfies RolePermissionTreeNodeView;
    })
    .filter((item): item is RolePermissionTreeNodeView => Boolean(item));
}

function walkTree(
  nodes: RolePermissionTreeNodeView[],
  handler: (node: RolePermissionTreeNodeView) => void,
) {
  for (const node of nodes) {
    handler(node);

    if (node.children.length > 0) {
      walkTree(node.children, handler);
    }
  }
}

function toggleExpand(nodeId: string) {
  if (expandedIds.value.has(nodeId)) {
    expandedIds.value.delete(nodeId);
  } else {
    expandedIds.value.add(nodeId);
  }
}

function expandAll() {
  expandedIds.value = collectExpandableIds(props.resources);
}

function collapseAll() {
  expandedIds.value = new Set<string>();
}

function clearAll() {
  selectedMenuNodeIds.value = new Set<string>();
  selectedFunctionNodeIds.value = new Set<string>();
}

function collectExpandableIds(resources: RoleGrantResourceEntry[]): Set<string> {
  const expanded = new Set<string>();

  resources.forEach((row) => {
    const nestedChildren = (childrenMap.value.get(row.id) ?? []).filter(
      (child) => child.type !== "BUTTON",
    );

    if (nestedChildren.length > 0) {
      expanded.add(row.id);
    }
  });

  return expanded;
}

function toggleSelect(nodeId: string, checked: boolean) {
  if (props.canSave === false) {
    return;
  }

  const row = resourceMap.value.get(nodeId);

  if (!row || !row.enabled || row.type === "DIRECTORY") {
    return;
  }

  if (row.type === "BUTTON") {
    const nextFunctionNodeIds = new Set(selectedFunctionNodeIds.value);

    if (checked) {
      nextFunctionNodeIds.add(nodeId);
    } else {
      nextFunctionNodeIds.delete(nodeId);
    }

    selectedFunctionNodeIds.value = nextFunctionNodeIds;
    return;
  }

  if (row.type === "MENU") {
    const nextMenuNodeIds = new Set(selectedMenuNodeIds.value);
    const nextFunctionNodeIds = new Set(selectedFunctionNodeIds.value);

    if (checked) {
      nextMenuNodeIds.add(nodeId);
    } else {
      nextMenuNodeIds.delete(nodeId);

      collectDescendantButtonIds(nodeId).forEach((buttonNodeId) => {
        nextFunctionNodeIds.delete(buttonNodeId);
      });
    }

    selectedMenuNodeIds.value = nextMenuNodeIds;
    selectedFunctionNodeIds.value = nextFunctionNodeIds;
  }
}

function onToggleSelect(payload: { id: string; checked: boolean }) {
  toggleSelect(payload.id, payload.checked);
}

function collectDescendantButtonIds(nodeId: string): string[] {
  const result: string[] = [];
  const children = childrenMap.value.get(nodeId) ?? [];

  children.forEach((child) => {
    if (child.type === "BUTTON") {
      result.push(child.id);
    }

    result.push(...collectDescendantButtonIds(child.id));
  });

  return result;
}

function collectAncestorIds(nodeId: string): string[] {
  const result: string[] = [];
  let current = resourceMap.value.get(nodeId)?.parentId ?? null;

  while (current) {
    result.push(current);
    current = resourceMap.value.get(current)?.parentId ?? null;
  }

  return result;
}

function buildSubmitSelection(): RoleGrantSelection {
  const menuIds = new Set<string>();
  const functionIds = new Set<string>();

  props.resources.forEach((resource) => {
    if (resource.menuId && selectedNodeIds.value.has(resource.id)) {
      menuIds.add(resource.menuId);
    }

    if (resource.functionId && selectedFunctionNodeIds.value.has(resource.id)) {
      functionIds.add(resource.functionId);
    }
  });

  return {
    menuIds: Array.from(menuIds),
    functionIds: Array.from(functionIds),
  };
}

function buildSelectionDiff(before: RoleGrantSelection, after: RoleGrantSelection) {
  const beforeMenuIds = new Set((before.menuIds || []).map(String));
  const afterMenuIds = new Set((after.menuIds || []).map(String));
  const beforeFunctionIds = new Set((before.functionIds || []).map(String));
  const afterFunctionIds = new Set((after.functionIds || []).map(String));

  const addedMenuIds = new Set([...afterMenuIds].filter((id) => !beforeMenuIds.has(id)));
  const removedMenuIds = new Set([...beforeMenuIds].filter((id) => !afterMenuIds.has(id)));
  const addedFunctionIds = new Set(
    [...afterFunctionIds].filter((id) => !beforeFunctionIds.has(id)),
  );
  const removedFunctionIds = new Set(
    [...beforeFunctionIds].filter((id) => !afterFunctionIds.has(id)),
  );

  return {
    addedMenuIds,
    removedMenuIds,
    addedFunctionIds,
    removedFunctionIds,
    changed:
      addedMenuIds.size > 0 ||
      removedMenuIds.size > 0 ||
      addedFunctionIds.size > 0 ||
      removedFunctionIds.size > 0,
  };
}

function buildNodeIdsFromSelection(selection: RoleGrantSelection): Set<string> {
  const ids = new Set<string>();
  const menuIds = new Set((selection.menuIds || []).map(String));
  const functionIds = new Set((selection.functionIds || []).map(String));

  props.resources.forEach((resource) => {
    if (resource.menuId && menuIds.has(resource.menuId)) {
      ids.add(resource.id);
    }

    if (resource.functionId && functionIds.has(resource.functionId)) {
      ids.add(resource.id);
    }
  });

  return ids;
}

function submit() {
  if (props.canSave === false) {
    return;
  }

  const nextSelection = buildSubmitSelection();
  const nextDiff = buildSelectionDiff(props.selection, nextSelection);

  if (!nextDiff.changed) {
    emit("submit", nextSelection);
    return;
  }

  pendingSelection.value = nextSelection;
  confirming.value = true;
}

function backToEdit() {
  confirming.value = false;
}

function confirmSubmit() {
  if (!pendingSelection.value) {
    return;
  }

  emit("submit", pendingSelection.value);
}
</script>

<style scoped>
.permission-dialog-shell {
  display: grid;
  gap: 14px;
}

.permission-dialog-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.permission-dialog-toolbar :deep(.bz-input) {
  flex: 1 1 auto;
  min-width: 0;
}

.permission-toolbar-button {
  flex: 0 0 auto;
  min-width: 86px;
  white-space: nowrap;
}

.permission-toolbar-button :deep(button),
.permission-toolbar-button :deep(.bz-button) {
  white-space: nowrap;
}

.permission-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 560px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
}

.permission-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.permission-panel__title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.permission-panel__hint {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.permission-panel__meta {
  flex: 0 0 auto;
  font-size: 12px;
  color: #6b7280;
  line-height: 20px;
}

.permission-tree-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px 14px 18px;
}

.permission-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 14px;
  text-align: center;
  padding: 24px;
}

.permission-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.permission-dialog-footer__summary {
  color: #6b7280;
  font-size: 13px;
}

.permission-dialog-footer__actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 820px) {
  .permission-dialog-toolbar {
    flex-wrap: wrap;
  }

  .permission-panel {
    height: 520px;
  }

  .permission-dialog-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .permission-dialog-footer__actions {
    justify-content: flex-end;
  }
}
</style>
