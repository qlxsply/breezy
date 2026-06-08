<!-- /src/components/users-admin/ResourcePermissionTree.vue -->
<template>
  <div class="tree">
    <div
      v-for="item in treeRows"
      :key="item.row.id"
      class="tree-row"
    >
      <div
        class="tree-cell"
        :style="indentStyle(item.depth)"
      >
        <bz-checkbox
          :model-value="isChecked(item.row)"
          :disabled="isDisabled(item.row)"
          @change="() => toggle(item.row)"
        />
        <span class="tree-label">
          {{ item.row.name }}
          <span class="tree-code">{{ item.row.code }}</span>
        </span>
        <span
          v-if="isInheritedMenu(item.row)"
          class="tag"
          >继承</span
        >
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// <script setup> + TS：顶层即 setup()。
import { computed } from "vue";

import type { ResourceEntry } from "../../types/resource-admin";

const props = defineProps<{
  resources: ResourceEntry[];
  modelValue: string[];
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: string[]): void;
}>();

const resourceMap = computed(() => {
  const map = new Map<string, ResourceEntry>();
  props.resources.forEach((r) => map.set(r.id, r));
  return map;
});

const childrenMap = computed(() => {
  const idSet = new Set(props.resources.map((r) => r.id));
  const map = new Map<string | null, ResourceEntry[]>();
  const addChild = (parentId: string | null, row: ResourceEntry) => {
    const list = map.get(parentId) ?? [];
    list.push(row);
    map.set(parentId, list);
  };

  props.resources.forEach((row) => {
    const rawParent = row.parentId ?? null;
    const parentId = rawParent && idSet.has(rawParent) ? rawParent : null;
    addChild(parentId, row);
  });

  map.forEach((list) => {
    list.sort((a, b) => {
      if (a.orderNo !== b.orderNo) return a.orderNo - b.orderNo;
      return a.name.localeCompare(b.name);
    });
  });

  return map;
});

const treeRows = computed(() => {
  const flat: Array<{ row: ResourceEntry; depth: number }> = [];
  const walk = (parentId: string | null, depth: number) => {
    const list = childrenMap.value.get(parentId);
    if (!list) return;
    list.forEach((row) => {
      flat.push({ row, depth });
      walk(row.id, depth + 1);
    });
  };
  walk(null, 0);
  return flat;
});

const selectedSet = computed(() => new Set(props.modelValue));

function isMenuEffective(row: ResourceEntry): boolean {
  if (row.type !== "MENU") return false;
  if (selectedSet.value.has(row.id)) return true;
  let parentId = row.parentId ?? null;
  while (parentId) {
    if (selectedSet.value.has(parentId)) return true;
    const parent = resourceMap.value.get(parentId);
    if (!parent) return false;
    parentId = parent.parentId ?? null;
  }
  return false;
}

function isInheritedMenu(row: ResourceEntry): boolean {
  return row.type === "MENU" && !selectedSet.value.has(row.id) && isMenuEffective(row);
}

function isChecked(row: ResourceEntry): boolean {
  if (row.type === "MENU") return isMenuEffective(row);
  return selectedSet.value.has(row.id);
}

function isDisabled(row: ResourceEntry): boolean {
  if (!row.enabled) return true;
  return row.type === "MENU" && isInheritedMenu(row);
}

function toggle(row: ResourceEntry) {
  const next = new Set(selectedSet.value);
  if (row.type === "MENU") {
    if (next.has(row.id)) next.delete(row.id);
    else next.add(row.id);
    emit("update:modelValue", Array.from(next));
    return;
  }
  if (next.has(row.id)) next.delete(row.id);
  else next.add(row.id);
  emit("update:modelValue", Array.from(next));
}

function indentStyle(depth: number) {
  if (!depth) return undefined;
  return { paddingLeft: `${depth * 16}px` };
}
</script>

<style scoped>
.tree {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tree-row {
  display: flex;
  align-items: center;
}

.tree-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
}

.tree-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
}

.tree-code {
  margin-left: 6px;
  font-size: 11px;
  color: var(--text-muted);
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.tag {
  margin-left: 8px;
  padding: 2px 6px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 11px;
  font-weight: 700;
}
</style>
