<template>
  <div class="node">
    <div
      class="node-row"
      :class="{ root: isRoot }"
      :style="{ '--depth': depth }"
    >
      <button
        v-if="isContainer"
        class="toggle"
        type="button"
        @click="handleToggle"
      >
        {{ collapsed ? ">" : "v" }}
      </button>
      <span
        v-else
        class="toggle spacer"
      />

      <span
        v-if="displayKey"
        class="key"
        :class="{ highlight: isKeyMatch }"
        >{{ displayKey }}</span
      >
      <span
        v-if="displayKey"
        class="colon"
        >:</span
      >

      <template v-if="isContainer">
        <span class="brace">{{ openBrace }}</span>
        <span
          v-if="collapsed"
          class="meta"
          >{{ summary }}</span
        >
        <span
          v-if="collapsed"
          class="brace"
          >{{ closeBrace }}</span
        >
      </template>
      <template v-else>
        <template v-if="editing">
          <input
            ref="editInputRef"
            v-model="editValue"
            class="edit-input"
            @keydown.enter.prevent="commitEdit"
            @keydown.esc.prevent="cancelEdit"
            @blur="commitEdit"
          />
        </template>
        <template v-else>
          <span
            class="value"
            :class="[valueClass, { highlight: isValueMatch, editable: isEditable }]"
            @dblclick.stop="startEdit"
          >
            {{ formattedValue }}
          </span>
          <button
            v-if="canParseValue"
            class="parse-btn"
            type="button"
            @click="handleParseValue"
          >
            解析
          </button>
        </template>
      </template>
    </div>

    <div
      v-if="isContainer && !collapsed"
      class="children"
    >
      <JsonTreeNode
        v-for="child in childNodes"
        :key="child.path"
        :value="child.value"
        :name="child.name"
        :path="child.path"
        :depth="child.depth"
      />
      <div class="node-row closing">
        <span class="toggle spacer" />
        <span class="brace">{{ closeBrace }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref } from "vue";

import { type JsonTreeContext, JsonTreeContextKey } from "./jsonTreeContext";
import JsonTreeNode from "./JsonTreeNode.vue";

interface JsonTreeNodeProps {
  value: unknown;
  name?: string | number;
  path: string;
  isRoot?: boolean;
  depth?: number;
}

const props = defineProps<JsonTreeNodeProps>();

const injectedContext = inject(JsonTreeContextKey, null);
if (!injectedContext) {
  throw new Error("JsonTreeNode must be used within JsonTreeView");
}
const context: JsonTreeContext = injectedContext;

const isArray = computed(() => Array.isArray(props.value));
const isObject = computed(() => {
  return props.value !== null && typeof props.value === "object" && !Array.isArray(props.value);
});
const isContainer = computed(() => isArray.value || isObject.value);
const isEditable = computed(() => !isContainer.value && props.value !== undefined);

const collapsed = computed(() => context.isCollapsed(props.path));
const depth = computed(() => props.depth ?? 0);
const editing = ref(false);
const editValue = ref("");
const editInputRef = ref<HTMLInputElement | null>(null);

const displayKey = computed(() => {
  if (props.isRoot) {
    return props.name ? String(props.name) : "";
  }
  if (props.name === undefined || props.name === null) {
    return "";
  }
  const nameText = String(props.name);
  if (nameText.startsWith("[") && nameText.endsWith("]")) {
    return nameText;
  }
  if (typeof props.name === "number") {
    return `[${props.name}]`;
  }
  return nameText;
});

const openBrace = computed(() => (isArray.value ? "[" : "{"));
const closeBrace = computed(() => (isArray.value ? "]" : "}"));

const childNodes = computed(() => {
  if (isArray.value) {
    return (props.value as unknown[]).map((item, index) => ({
      name: `[${index}]`,
      value: item,
      path: `${props.path}[${index}]`,
      depth: resolveDepth() + 1,
    }));
  }
  if (isObject.value) {
    return Object.entries(props.value as Record<string, unknown>).map(([key, item]) => ({
      name: key,
      value: item,
      path: `${props.path}.${key}`,
      depth: resolveDepth() + 1,
    }));
  }
  return [];
});

const isKeyMatch = computed(() => context.isKeyMatch(props.path));
const isValueMatch = computed(() => context.isValueMatch(props.path));

const summary = computed(() => {
  const count = childNodes.value.length;
  if (count === 0) {
    return isArray.value ? "" : "";
  }
  return `${count} items`;
});

const formattedValue = computed(() => {
  const value = props.value;
  if (value === null) {
    return "null";
  }
  if (typeof value === "string") {
    return `"${value}"`;
  }
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  if (value === undefined) {
    return "undefined";
  }
  return String(value);
});

const valueClass = computed(() => {
  const value = props.value;
  if (value === null || value === undefined) {
    return "null";
  }
  if (typeof value === "string") {
    return "string";
  }
  if (typeof value === "number") {
    return "number";
  }
  if (typeof value === "boolean") {
    return "boolean";
  }
  return "unknown";
});

const formattedEditValue = computed(() => {
  const value = props.value;
  if (value === null) {
    return "null";
  }
  if (value === undefined) {
    return "";
  }
  if (typeof value === "string") {
    return value;
  }
  return String(value);
});

const parseCandidate = computed(() => {
  if (typeof props.value !== "string") {
    return null;
  }
  const trimmed = props.value.trim();
  if (!trimmed) {
    return null;
  }
  try {
    const parsed = JSON.parse(trimmed);
    if (parsed !== null && typeof parsed === "object") {
      return parsed as unknown;
    }
  } catch (_err) {
    return null;
  }
  return null;
});

const canParseValue = computed(() => parseCandidate.value !== null);

function handleToggle() {
  if (!isContainer.value) {
    return;
  }
  context.toggle(props.path);
}

function handleParseValue() {
  if (!canParseValue.value) {
    return;
  }
  context.parseValue(props.path, parseCandidate.value);
}

function startEdit() {
  if (!isEditable.value) {
    return;
  }
  editing.value = true;
  editValue.value = formattedEditValue.value;
  nextTick(() => {
    editInputRef.value?.focus();
    editInputRef.value?.select();
  });
}

function commitEdit() {
  if (!editing.value) {
    return;
  }
  context.updateValue(props.path, editValue.value);
  editing.value = false;
}

function cancelEdit() {
  editing.value = false;
}

function resolveDepth(): number {
  return props.depth ?? 0;
}
</script>

<style scoped>
.node {
  padding-left: 6px;
}

.node-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  white-space: nowrap;
  padding: 2px 4px;
  border-radius: 6px;
  background: rgba(148, 163, 184, calc(0.04 * var(--depth, 0)));
}

.node-row.root {
  padding-left: 0;
}

.toggle {
  width: 18px;
  height: 18px;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  font-size: 10px;
  padding: 0;
  margin-top: 2px;
}

.toggle.spacer {
  display: inline-block;
  width: 18px;
  height: 18px;
}

.key {
  color: #7dd3fc;
}

.key.highlight,
.value.highlight {
  background: rgba(250, 204, 21, 0.2);
  box-shadow: 0 0 0 1px rgba(250, 204, 21, 0.35);
  border-radius: 4px;
  padding: 0 3px;
}

.colon {
  color: #94a3b8;
}

.brace {
  color: #fbbf24;
}

.meta {
  color: #94a3b8;
  margin-left: 6px;
  margin-right: 6px;
  font-size: 11px;
}

.children {
  margin-left: 18px;
}

.node-row.closing {
  margin-top: 2px;
}

.value.string {
  color: #a7f3d0;
}

.value.editable {
  cursor: text;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.5);
}

.value.number {
  color: #fcd34d;
}

.value.boolean {
  color: #fca5a5;
}

.value.null {
  color: #cbd5f5;
}

.value.unknown {
  color: #e2e8f0;
}

.edit-input {
  min-width: 120px;
  border: 1px solid rgba(148, 163, 184, 0.4);
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.6);
  color: #e2e8f0;
  padding: 2px 6px;
  font-size: 12px;
}

.parse-btn {
  margin-left: 6px;
  border: 1px solid rgba(148, 163, 184, 0.5);
  background: transparent;
  color: #e2e8f0;
  border-radius: 6px;
  font-size: 11px;
  padding: 0 6px;
  height: 18px;
  cursor: pointer;
}

.parse-btn:hover {
  border-color: #fbbf24;
  color: #fbbf24;
}

@media (max-width: 768px) {
  .node-row {
    white-space: normal;
    flex-wrap: wrap;
  }
}
</style>
