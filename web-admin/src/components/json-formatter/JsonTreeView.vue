<template>
  <div class="json-tree">
    <JsonTreeNode
      :value="value"
      :path="rootPath"
      :name="rootLabel"
      :is-root="true"
      :depth="0"
    />
  </div>
</template>

<script setup lang="ts">
import { provide, reactive, watch } from "vue";

import { JsonTreeContextKey } from "./jsonTreeContext";
import JsonTreeNode from "./JsonTreeNode.vue";

export interface JsonTreeViewExpose {
  collapseAll: () => void;
  expandAll: () => void;
  expandPaths: (paths: string[]) => void;
}

interface JsonTreeViewProps {
  value: unknown;
  rootLabel?: string;
  keyMatches?: Set<string>;
  valueMatches?: Set<string>;
  onParseValue?: (path: string, value: unknown) => void;
  onUpdateValue?: (path: string, value: string) => void;
}

const props = defineProps<JsonTreeViewProps>();

const rootPath = "root";
const collapsedPaths = reactive(new Set<string>());

function isCollapsed(path: string): boolean {
  return collapsedPaths.has(path);
}

function toggle(path: string): void {
  if (collapsedPaths.has(path)) {
    collapsedPaths.delete(path);
  } else {
    collapsedPaths.add(path);
  }
}

function expandAll(): void {
  collapsedPaths.clear();
}

function expandPaths(paths: string[]): void {
  paths.forEach((path) => {
    getAncestorPaths(path).forEach((ancestor) => collapsedPaths.delete(ancestor));
  });
}

function collapseAll(): void {
  collapsedPaths.clear();
  collectContainerPaths(props.value, rootPath).forEach((path) => collapsedPaths.add(path));
}

function collectContainerPaths(value: unknown, path: string): string[] {
  if (!isContainer(value)) {
    return [];
  }
  const paths = [path];
  if (Array.isArray(value)) {
    value.forEach((item, index) => {
      paths.push(...collectContainerPaths(item, `${path}[${index}]`));
    });
  } else if (value && typeof value === "object") {
    Object.entries(value as Record<string, unknown>).forEach(([key, item]) => {
      paths.push(...collectContainerPaths(item, `${path}.${key}`));
    });
  }
  return paths;
}

function getAncestorPaths(path: string): string[] {
  const tokens = path.match(/(\[[^]]+\]|[^.[\]]+)/g) ?? [];
  const result: string[] = [];
  let current = "";
  tokens.forEach((token) => {
    if (token.startsWith("[")) {
      current += token;
    } else {
      current = current ? `${current}.${token}` : token;
    }
    result.push(current);
  });
  return result;
}

function isContainer(value: unknown): boolean {
  if (Array.isArray(value)) {
    return true;
  }
  return value !== null && typeof value === "object";
}

const emptyMatches = new Set<string>();

function isKeyMatch(path: string): boolean {
  return (props.keyMatches ?? emptyMatches).has(path);
}

function isValueMatch(path: string): boolean {
  return (props.valueMatches ?? emptyMatches).has(path);
}

function parseValue(path: string, value: unknown): void {
  if (props.onParseValue) {
    props.onParseValue(path, value);
  }
}

function updateValue(path: string, value: string): void {
  if (props.onUpdateValue) {
    props.onUpdateValue(path, value);
  }
}

provide(JsonTreeContextKey, {
  isCollapsed,
  toggle,
  isKeyMatch,
  isValueMatch,
  parseValue,
  updateValue,
});

watch(
  () => props.value,
  () => {
    expandAll();
  },
  { deep: false },
);

defineExpose<JsonTreeViewExpose>({ collapseAll, expandAll, expandPaths });
</script>

<style scoped>
.json-tree {
  background: #0f172a;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 12px;
  font-family: "SFMono-Regular", "Consolas", "Liberation Mono", "Menlo", monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow: auto;
  max-height: 100%;
}
</style>
