<!-- /src/components/resources-admin/ResourceTable.vue -->
<template>
  <bz-table
    v-loading="loading"
    :data="treeRows"
    :row-key="rowKey"
    empty-text="暂无数据"
    size="small"
  >
    <bz-table-column
      label="资源名称"
      :width="320"
    >
      <template #default="scope">
        <div
          class="name-block"
          :style="indentStyle(scope.row.depth)"
        >
          <div class="tree-ops">
            <bz-button
              v-if="hasChildren(scope.row.row.id)"
              link
              class="tree-toggle"
              @click.stop="toggleExpand(scope.row.row.id)"
            >
              <bz-icon :class="['toggle-icon', isExpanded(scope.row.row.id) ? 'expanded' : '']">
                <span class="toggle-caret">›</span>
              </bz-icon>
            </bz-button>
            <span
              v-else
              class="tree-toggle-placeholder"
            ></span>
          </div>
          <div class="row-icon">{{ scope.row.row.icon || "📦" }}</div>
          <div class="name-info">
            <div class="name">
              <span
                class="name-text"
                :title="scope.row.row.name"
                >{{ scope.row.row.name }}</span
              >
            </div>
            <div
              v-if="scope.row.row.description"
              class="desc"
              :title="scope.row.row.description"
            >
              {{ scope.row.row.description }}
            </div>
          </div>
        </div>
      </template>
    </bz-table-column>
    <bz-table-column
      label="编码"
      width="150"
      show-overflow-tooltip
    >
      <template #default="scope">
        <span class="mono">{{ scope.row.row.code }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      label="类型"
      width="110"
    >
      <template #default="scope">
        <bz-tag size="small">{{ scope.row.row.type }}</bz-tag>
      </template>
    </bz-table-column>
    <bz-table-column
      label="入口"
      width="100"
    >
      <template #default="scope">
        <bz-tag size="small">{{
          scope.row.row.type === "MENU" ? scope.row.row.scope : "-"
        }}</bz-tag>
      </template>
    </bz-table-column>
    <bz-table-column
      label="打开"
      width="100"
    >
      <template #default="scope">
        <bz-tag size="small">{{ scope.row.row.openMode }}</bz-tag>
      </template>
    </bz-table-column>
    <bz-table-column
      label="URL"
      min-width="100"
      show-overflow-tooltip
    >
      <template #default="scope">
        <span class="mono">{{ scope.row.row.url || "-" }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      label="加载资源"
      min-width="260"
      show-overflow-tooltip
    >
      <template #default="scope">
        <span class="mono">{{ scope.row.row.loadTarget || "-" }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      label="级别"
      width="50"
    >
      <template #default="scope">
        <bz-tag
          size="small"
          :type="scope.row.row.level === 'SYSTEM' ? 'danger' : 'info'"
        >
          {{ scope.row.row.level === "SYSTEM" ? "系统" : "自定义" }}
        </bz-tag>
      </template>
    </bz-table-column>
    <bz-table-column
      label="游客"
      width="50"
    >
      <template #default="scope">
        <bz-tag
          v-if="scope.row.row.guestAccess"
          size="small"
          type="success"
          >允许</bz-tag
        >
        <bz-tag
          v-else
          size="small"
          type="danger"
          >禁止</bz-tag
        >
      </template>
    </bz-table-column>
    <bz-table-column
      label="状态"
      width="50"
    >
      <template #default="scope">
        <div class="status-cell">
          <bz-tag
            size="small"
            :type="effectiveEnabled(scope.row.row) ? 'success' : 'info'"
          >
            {{ effectiveEnabled(scope.row.row) ? "启用" : "停用" }}
          </bz-tag>
          <bz-tooltip
            v-if="scope.row.row.missingApis"
            content="API缺失"
            placement="top"
          >
            <bz-tag
              size="small"
              type="warning"
              >API缺失</bz-tag
            >
          </bz-tooltip>
          <bz-tooltip
            v-if="isAncestorDisabled(scope.row.row)"
            content="父级已停用"
            placement="top"
          >
            <bz-tag
              size="small"
              type="warning"
              >父级停用</bz-tag
            >
          </bz-tooltip>
        </div>
      </template>
    </bz-table-column>
    <bz-table-column
      label="操作"
      width="200"
      fixed="right"
    >
      <template #default="scope">
        <div class="action-buttons">
          <bz-button
            v-if="canApi"
            size="small"
            @click="$emit('api', scope.row.row)"
            >API</bz-button
          >
          <bz-button
            v-if="canEdit"
            size="small"
            :disabled="scope.row.row.level === 'SYSTEM'"
            @click="$emit('edit', scope.row.row)"
            >编辑</bz-button
          >
          <bz-button
            v-if="canDelete"
            size="small"
            type="danger"
            :disabled="scope.row.row.level === 'SYSTEM'"
            @click="$emit('remove', scope.row.row)"
            >删除</bz-button
          >
        </div>
      </template>
    </bz-table-column>
  </bz-table>
</template>

<script setup lang="ts">
// <script setup>：顶层即 setup()。
import { computed, ref, watch } from "vue";

import type { ResourceEntry } from "../../types/resource-admin";

// defineProps<T>()：声明 props 类型。
const props = defineProps<{
  rows: ResourceEntry[];
  loading: boolean;
  expandAll?: boolean;
  expandSignal?: number;
  canEdit?: boolean;
  canDelete?: boolean;
  canApi?: boolean;
}>();

const canEdit = computed(() => props.canEdit !== false);
const canDelete = computed(() => props.canDelete !== false);
const canApi = computed(() => props.canApi !== false);

// defineEmits<T>()：声明事件名和参数。
defineEmits<{
  (e: "edit", row: ResourceEntry): void;
  (e: "remove", row: ResourceEntry): void;
  (e: "api", row: ResourceEntry): void;
}>();

const resourceMap = computed(() => {
  const map = new Map<string, ResourceEntry>();
  props.rows.forEach((r) => map.set(r.id, r));
  return map;
});

const expandedIds = ref<Set<string>>(new Set());
const hasCustomExpand = ref(false);

watch(
  () => props.rows,
  (rows) => {
    const expandableIds = collectExpandableIds(rows);
    if (!hasCustomExpand.value) {
      expandedIds.value = expandableIds;
      return;
    }
    const next = new Set<string>();
    expandedIds.value.forEach((id) => {
      if (expandableIds.has(id)) next.add(id);
    });
    expandedIds.value = next;
  },
  { immediate: true },
);

watch(
  () => [props.expandSignal, props.expandAll],
  ([signal, expandAll]) => {
    if (signal === undefined || signal === 0) return;
    hasCustomExpand.value = true;
    if (expandAll) {
      expandedIds.value = collectExpandableIds(props.rows);
      return;
    }
    expandedIds.value = new Set();
  },
  { immediate: true },
);

const childrenMap = computed(() => {
  const idSet = new Set(props.rows.map((r) => r.id));
  const childrenMap = new Map<string | null, ResourceEntry[]>();

  const addChild = (parentId: string | null, row: ResourceEntry) => {
    const list = childrenMap.get(parentId) ?? [];
    list.push(row);
    childrenMap.set(parentId, list);
  };

  props.rows.forEach((row) => {
    const rawParent = row.parentId ?? null;
    const parentId = rawParent && idSet.has(rawParent) ? rawParent : null;
    addChild(parentId, row);
  });

  childrenMap.forEach((list) => {
    list.sort((a, b) => {
      if (a.orderNo !== b.orderNo) return a.orderNo - b.orderNo;
      return a.name.localeCompare(b.name);
    });
  });

  return childrenMap;
});

const treeRows = computed(() => {
  const flat: Array<{ row: ResourceEntry; depth: number }> = [];
  const walk = (parentId: string | null, depth: number) => {
    const list = childrenMap.value.get(parentId);
    if (!list) return;
    list.forEach((row) => {
      flat.push({ row, depth });
      if (hasChildren(row.id) && expandedIds.value.has(row.id)) {
        walk(row.id, depth + 1);
      }
    });
  };

  walk(null, 0);
  return flat;
});

const rowKey = (item: { row: ResourceEntry }) => item.row.id;

function collectExpandableIds(rows: ResourceEntry[]): Set<string> {
  const idSet = new Set(rows.map((row) => row.id));
  const parentIds = new Set<string>();
  rows.forEach((row) => {
    const parentId = row.parentId ?? null;
    if (parentId && idSet.has(parentId)) {
      parentIds.add(parentId);
    }
  });
  return parentIds;
}

function indentStyle(depth: number) {
  const offset = depth * 24;
  return { paddingLeft: `${offset}px` };
}

function hasChildren(id: string): boolean {
  const list = childrenMap.value.get(id);
  return Boolean(list && list.length > 0);
}

function isExpanded(id: string): boolean {
  return expandedIds.value.has(id);
}

function toggleExpand(id: string) {
  const next = new Set(expandedIds.value);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  expandedIds.value = next;
  hasCustomExpand.value = true;
}

function isAncestorDisabled(row: ResourceEntry): boolean {
  let parentId = row.parentId ?? null;
  while (parentId) {
    const parent = resourceMap.value.get(parentId);
    if (!parent) return false;
    if (!parent.enabled) return true;
    parentId = parent.parentId ?? null;
  }
  return false;
}

function effectiveEnabled(row: ResourceEntry): boolean {
  if (!row.enabled) return false;
  return !isAncestorDisabled(row);
}
</script>

<style scoped>
.name-block {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.tree-ops {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tree-toggle {
  padding: 0;
  min-height: 16px;
}

.toggle-icon {
  transition: transform 0.2s ease;
}

.toggle-caret {
  display: inline-block;
  line-height: 1;
}

.toggle-icon.expanded {
  transform: rotate(90deg);
}

.tree-toggle-placeholder {
  width: 16px;
  height: 16px;
}

.row-icon {
  font-size: 16px;
  margin-top: 2px;
  flex-shrink: 0;
}

.name-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.name {
  font-weight: 500;
  color: var(--text-main);
  min-width: 0;
}

.name-text {
  display: inline-block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.desc {
  color: var(--text-muted);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mono {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
  font-size: 13px;
  color: var(--text-muted);
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
</style>
