<template></template>

<script setup lang="ts">
import { inject, onBeforeUnmount, onMounted, reactive, useSlots, watchEffect } from "vue";

import type { BzTableCellScope, BzTableColumnDef, BzTableRow } from "./tableContext";
import { bzTableContextKey } from "./tableContext";

defineOptions({
  name: "BzTableColumn",
});

const props = withDefaults(
  defineProps<{
    type?: "default" | "selection";
    prop?: string;
    field?: string;
    label?: string;
    title?: string;
    width?: number | string;
    minWidth?: number | string;
    align?: "left" | "center" | "right";
    fixed?: "left" | "right";
    ellipsis?: boolean;
    showOverflowTooltip?: boolean;
    selectable?: (row: BzTableRow) => boolean;
  }>(),
  {
    type: "default",
    prop: "",
    field: "",
    label: "",
    title: "",
    width: undefined,
    minWidth: undefined,
    align: "left",
    fixed: undefined,
    ellipsis: false,
    showOverflowTooltip: false,
    selectable: undefined,
  },
);

const slots = useSlots();
const tableContext = inject(bzTableContextKey, null);
const columnId = Symbol("bzTableColumn");

const columnDef = reactive<BzTableColumnDef>({
  id: columnId,
  type: props.type,
  field: props.field || props.prop || undefined,
  title: props.title || props.label,
  width: props.width,
  minWidth: props.minWidth,
  align: props.align,
  fixed: props.fixed,
  ellipsis: props.ellipsis || props.showOverflowTooltip,
  selectable: props.selectable,
  renderCell: slots.default ? (scope: BzTableCellScope) => slots.default?.(scope) : undefined,
});

watchEffect(() => {
  columnDef.type = props.type;
  columnDef.field = props.field || props.prop || undefined;
  columnDef.title = props.title || props.label;
  columnDef.width = props.width;
  columnDef.minWidth = props.minWidth;
  columnDef.align = props.align;
  columnDef.fixed = props.fixed;
  columnDef.ellipsis = props.ellipsis || props.showOverflowTooltip;
  columnDef.selectable = props.selectable;
  columnDef.renderCell = slots.default
    ? (scope: BzTableCellScope) => slots.default?.(scope)
    : undefined;
});

onMounted(() => {
  tableContext?.registerColumn(columnDef);
});

onBeforeUnmount(() => {
  tableContext?.unregisterColumn(columnId);
});
</script>
