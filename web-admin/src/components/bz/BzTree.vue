<template>
  <div class="bz-tree">
    <TreeNode
      v-for="rootNode in data"
      :key="String(readNodeKey(rootNode))"
      :node="rootNode"
      :level="0"
      :expanded-keys="expandedKeys"
      :default-expand-all="defaultExpandAll"
      :props-map="propsMap"
    >
      <template #default="scope">
        <slot
          name="default"
          v-bind="scope"
        >
          <span>{{ readLabel(scope.data) }}</span>
        </slot>
      </template>
    </TreeNode>
  </div>
</template>

<script setup lang="ts">
import type { PropType, VNode } from "vue";
import { computed, defineComponent, h, ref } from "vue";

defineOptions({
  name: "BzTree",
});

type TreeNodeData = Record<string, unknown>;

interface TreePropsMap {
  label?: string;
  children?: string;
}

const props = withDefaults(
  defineProps<{
    data: TreeNodeData[];
    nodeKey?: string;
    defaultExpandAll?: boolean;
    props?: TreePropsMap;
  }>(),
  {
    data: () => [],
    nodeKey: "id",
    defaultExpandAll: false,
    props: () => ({
      label: "label",
      children: "children",
    }),
  },
);

const expandedKeys = ref(new Set<string>());

const propsMap = computed(() => ({
  label: props.props.label || "label",
  children: props.props.children || "children",
}));

function readNodeKey(node: TreeNodeData): string {
  const value = node[props.nodeKey];
  return value === undefined || value === null ? JSON.stringify(node) : String(value);
}

function readLabel(node: TreeNodeData): string {
  const value = node[propsMap.value.label];
  return value === undefined || value === null ? "-" : String(value);
}

const TreeNode = defineComponent({
  name: "BzTreeNode",
  props: {
    node: {
      type: Object as PropType<TreeNodeData>,
      required: true,
    },
    level: {
      type: Number,
      required: true,
    },
    expandedKeys: {
      type: Object as PropType<Set<string>>,
      required: true,
    },
    defaultExpandAll: {
      type: Boolean,
      required: true,
    },
    propsMap: {
      type: Object as PropType<{ label: string; children: string }>,
      required: true,
    },
  },
  setup(nodeProps, { slots }) {
    const key = computed(() => {
      const value = nodeProps.node[props.nodeKey];
      return value === undefined || value === null ? JSON.stringify(nodeProps.node) : String(value);
    });

    const children = computed(() => {
      const value = nodeProps.node[nodeProps.propsMap.children];
      return Array.isArray(value) ? (value as TreeNodeData[]) : [];
    });

    const isLeaf = computed(() => children.value.length === 0);
    const expanded = computed(() => {
      if (nodeProps.defaultExpandAll) {
        return true;
      }
      return nodeProps.expandedKeys.has(key.value);
    });

    function toggle() {
      if (isLeaf.value || nodeProps.defaultExpandAll) {
        return;
      }
      const next = new Set(nodeProps.expandedKeys);
      if (next.has(key.value)) {
        next.delete(key.value);
      } else {
        next.add(key.value);
      }
      expandedKeys.value = next;
    }

    function renderChildren(): VNode[] {
      if (!expanded.value || isLeaf.value) {
        return [];
      }
      return children.value.map((child) =>
        h(
          TreeNode,
          {
            node: child,
            level: nodeProps.level + 1,
            expandedKeys: expandedKeys.value,
            defaultExpandAll: nodeProps.defaultExpandAll,
            propsMap: nodeProps.propsMap,
          },
          slots,
        ),
      );
    }

    return () =>
      h("div", { class: "bz-tree-node" }, [
        h(
          "div",
          {
            class: "bz-tree-node__line",
            style: { paddingLeft: `${nodeProps.level * 16}px` },
          },
          [
            h(
              "button",
              {
                class: "bz-tree-node__toggle",
                type: "button",
                onClick: toggle,
                disabled: isLeaf.value,
              },
              isLeaf.value ? "" : expanded.value ? "▾" : "▸",
            ),
            h(
              "div",
              { class: "bz-tree-node__content" },
              slots.default
                ? slots.default({ data: nodeProps.node })
                : [String(nodeProps.node[nodeProps.propsMap.label] ?? "-")],
            ),
          ],
        ),
        ...renderChildren(),
      ]);
  },
});
</script>

<style scoped>
.bz-tree {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: #fff;
  padding: 8px;
}

:deep(.bz-tree-node__line) {
  display: flex;
  align-items: center;
  min-height: 28px;
  border-radius: 6px;
}

:deep(.bz-tree-node__line:hover) {
  background: #f8fafc;
}

:deep(.bz-tree-node__toggle) {
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
}

:deep(.bz-tree-node__toggle:disabled) {
  cursor: default;
  opacity: 0.35;
}

:deep(.bz-tree-node__content) {
  flex: 1;
  min-width: 0;
}
</style>
