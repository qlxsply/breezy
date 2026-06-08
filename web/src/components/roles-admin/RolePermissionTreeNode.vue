<template>
  <div class="permission-tree-node">
    <div
      class="permission-node-row"
      :class="[{ 'is-directory': node.row.type === 'DIRECTORY' }, diffClass]"
    >
      <button
        class="permission-node-toggle"
        :class="{ 'is-placeholder': !hasNestedChildren }"
        type="button"
        @click="onToggleExpand"
      >
        {{ hasNestedChildren ? (expanded ? "▾" : "▸") : "▸" }}
      </button>

      <span
        v-if="readonly || !selectable"
        class="permission-node-checkbox-placeholder"
      ></span>

      <input
        v-else
        class="permission-node-checkbox"
        type="checkbox"
        :checked="checked"
        :disabled="!canEdit || !node.row.enabled"
        :indeterminate.prop="indeterminate"
        @change="onToggleSelect"
      />

      <span class="permission-node-name">{{ node.row.name }}</span>

      <span
        class="permission-tag"
        :class="typeClass"
      >
        {{ typeLabel }}
      </span>

      <span
        v-if="!node.row.enabled"
        class="permission-tag permission-tag-disabled"
      >
        停用
      </span>
    </div>

    <div
      v-if="buttonChildren.length > 0"
      class="permission-button-tags"
    >
      <template
        v-for="child in buttonChildren"
        :key="child.row.id"
      >
        <label
          v-if="!readonly"
          class="permission-button-tag"
          :class="{
            'is-checked': isNodeChecked(child),
            'is-disabled': !canEdit || !child.row.enabled,
          }"
        >
          <input
            class="permission-button-tag__checkbox"
            type="checkbox"
            :checked="isNodeChecked(child)"
            :disabled="!canEdit || !child.row.enabled"
            @change="onToggleChildSelect(child, $event)"
          />
          <span class="permission-button-tag__name">{{ child.row.name }}</span>
        </label>

        <span
          v-else
          class="permission-button-tag is-readonly"
          :class="[{ 'is-checked': isNodeChecked(child) }, resolveDiffClass(child)]"
        >
          <span class="permission-button-tag__name">{{ child.row.name }}</span>
        </span>
      </template>
    </div>

    <div
      v-if="expanded && nestedChildren.length > 0"
      class="permission-tree-children"
    >
      <RolePermissionTreeNode
        v-for="child in nestedChildren"
        :key="child.row.id"
        :node="child"
        :expanded-ids="expandedIds"
        :selected-ids="selectedIds"
        :can-edit="canEdit"
        :readonly="readonly"
        :diff-status-by-id="diffStatusById"
        @toggle-expand="$emit('toggleExpand', $event)"
        @toggle-select="$emit('toggleSelect', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import type { RoleGrantResourceEntry } from "../../types/role-admin";

type DiffStatus = "added" | "removed";

export interface RolePermissionTreeNodeView {
  row: RoleGrantResourceEntry;
  children: RolePermissionTreeNodeView[];
}

const props = defineProps<{
  node: RolePermissionTreeNodeView;
  expandedIds: Set<string>;
  selectedIds: Set<string>;
  canEdit: boolean;
  readonly?: boolean;
  diffStatusById?: Map<string, DiffStatus>;
}>();

const emit = defineEmits<{
  (e: "toggleExpand", id: string): void;
  (e: "toggleSelect", payload: { id: string; checked: boolean }): void;
}>();

const buttonChildren = computed(() =>
  props.node.children.filter((child) => child.row.type === "BUTTON"),
);
const nestedChildren = computed(() =>
  props.node.children.filter((child) => child.row.type !== "BUTTON"),
);

const hasNestedChildren = computed(() => nestedChildren.value.length > 0);
const expanded = computed(() => props.expandedIds.has(props.node.row.id));
const checked = computed(() => props.selectedIds.has(props.node.row.id));
const selectable = computed(() => props.node.row.type !== "DIRECTORY");

const diffClass = computed(() => resolveDiffClass(props.node));

const indeterminate = computed(() => {
  if (!selectable.value || checked.value) {
    return false;
  }

  return hasSelectedDescendant(props.node);
});

const typeLabel = computed(() => {
  if (props.node.row.type === "DIRECTORY") return "目录";
  if (props.node.row.type === "MENU") return "菜单";
  return "按钮";
});

const typeClass = computed(() => {
  if (props.node.row.type === "DIRECTORY") return "permission-tag-dir";
  if (props.node.row.type === "MENU") return "permission-tag-menu";
  return "permission-tag-button";
});

function resolveDiffClass(node: RolePermissionTreeNodeView): string {
  const status = props.diffStatusById?.get(node.row.id);

  if (status === "added") {
    return "is-diff-added";
  }

  if (status === "removed") {
    return "is-diff-removed";
  }

  return "";
}

function hasSelectedDescendant(node: RolePermissionTreeNodeView): boolean {
  return node.children.some(
    (child) => props.selectedIds.has(child.row.id) || hasSelectedDescendant(child),
  );
}

function isNodeChecked(node: RolePermissionTreeNodeView): boolean {
  return props.selectedIds.has(node.row.id);
}

function onToggleExpand() {
  if (!hasNestedChildren.value) {
    return;
  }

  emit("toggleExpand", props.node.row.id);
}

function onToggleSelect(event: Event) {
  if (!selectable.value) {
    return;
  }

  emit("toggleSelect", {
    id: props.node.row.id,
    checked: (event.target as HTMLInputElement).checked,
  });
}

function onToggleChildSelect(node: RolePermissionTreeNodeView, event: Event) {
  emit("toggleSelect", {
    id: node.row.id,
    checked: (event.target as HTMLInputElement).checked,
  });
}
</script>

<style scoped>
.permission-tree-node {
  display: grid;
  gap: 4px;
  margin: 2px 0;
}

.permission-tree-children {
  margin-left: 22px;
}

.permission-node-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 4px 6px;
  border-radius: 8px;
  white-space: nowrap;
}

.permission-node-row:hover {
  background: #f3f4f6;
}

.permission-node-row.is-directory .permission-node-name {
  font-weight: 600;
  color: #374151;
}

.permission-node-row.is-diff-added {
  border: 1px solid #86efac;
  background: #f0fdf4;
}

.permission-node-row.is-diff-added .permission-node-name {
  color: #15803d;
  font-weight: 600;
}

.permission-node-row.is-diff-removed {
  color: #b91c1c;
  background: #fef2f2;
}

.permission-node-row.is-diff-removed .permission-node-name {
  color: #b91c1c;
  text-decoration: line-through;
  text-decoration-thickness: 2px;
}

.permission-node-toggle {
  width: 18px;
  height: 18px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  padding: 0;
}

.permission-node-toggle.is-placeholder {
  visibility: hidden;
}

.permission-node-checkbox,
.permission-node-checkbox-placeholder {
  width: 15px;
  height: 15px;
  flex: 0 0 15px;
}

.permission-node-name {
  color: #111827;
}

.permission-tag {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 20px;
}

.permission-tag-dir {
  background: #eef2ff;
  color: #4338ca;
}

.permission-tag-menu {
  background: #ecfdf5;
  color: #047857;
}

.permission-tag-button {
  background: #fff7ed;
  color: #c2410c;
}

.permission-tag-disabled {
  background: #f3f4f6;
  color: #9ca3af;
}

.permission-button-tags {
  margin-left: 39px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 2px 0 6px;
}

.permission-button-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 220px;
  min-height: 28px;
  padding: 4px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #ffffff;
  color: #374151;
  font-size: 12px;
  cursor: pointer;
  user-select: none;
}

.permission-button-tag:hover {
  border-color: #fdba74;
  background: #fff7ed;
}

.permission-button-tag.is-checked {
  border-color: #fb923c;
  background: #fff7ed;
  color: #c2410c;
}

.permission-button-tag.is-diff-added {
  border-color: #86efac;
  background: #f0fdf4;
  color: #15803d;
  font-weight: 600;
}

.permission-button-tag.is-diff-removed {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.permission-button-tag.is-diff-removed .permission-button-tag__name {
  text-decoration: line-through;
  text-decoration-thickness: 2px;
}

.permission-button-tag.is-disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.permission-button-tag.is-readonly {
  cursor: default;
}

.permission-button-tag__checkbox {
  width: 14px;
  height: 14px;
}

.permission-button-tag__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
