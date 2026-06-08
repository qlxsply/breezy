<template>
  <div
    class="bz-table"
    :class="[`bz-table--${size}`]"
  >
    <div class="bz-table__column-slot">
      <slot />
    </div>

    <div class="bz-table__scroll">
      <table class="bz-table__inner">
        <thead>
          <tr>
            <th
              v-for="headerCol in columns"
              :key="String(headerCol.id)"
              :style="columnStyle(headerCol)"
              :class="[headerClass(headerCol), { 'is-fixed-right': headerCol.fixed === 'right' }]"
            >
              <template v-if="headerCol.type === 'selection'">
                <input
                  ref="selectAllRef"
                  class="bz-table__checkbox"
                  type="checkbox"
                  :checked="allSelected"
                  @change="onSelectAllChange"
                />
              </template>
              <template v-else>
                {{ headerCol.title }}
              </template>
            </th>
          </tr>
        </thead>

        <tbody v-if="data.length > 0">
          <tr
            v-for="(rowItem, rowIdx) in data"
            :key="String(resolveRowKey(rowItem, rowIdx))"
            @dblclick="onRowDblClick(rowItem, rowIdx)"
          >
            <td
              v-for="bodyCol in columns"
              :key="String(bodyCol.id)"
              :style="columnStyle(bodyCol)"
              :class="[cellClass(bodyCol), { 'is-fixed-right': bodyCol.fixed === 'right' }]"
              @mouseenter="onCellMouseEnter($event, bodyCol)"
              @mouseleave="onCellMouseLeave($event, bodyCol)"
            >
              <template v-if="bodyCol.type === 'selection'">
                <input
                  class="bz-table__checkbox"
                  type="checkbox"
                  :checked="isRowSelected(rowItem, rowIdx)"
                  :disabled="!isRowSelectable(rowItem, bodyCol)"
                  @change="onRowSelectChange($event, rowItem, rowIdx)"
                />
              </template>
              <TableCellRenderer
                v-else
                :column="bodyCol"
                :row="rowItem"
                :row-index="rowIdx"
              />
            </td>
          </tr>
        </tbody>

        <tbody v-else>
          <tr>
            <td
              class="bz-table__empty"
              :colspan="Math.max(columns.length, 1)"
            >
              {{ emptyText }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div
      v-if="loading"
      class="bz-table__loading"
    >
      <div class="bz-table__loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <teleport to="body">
      <div
        v-if="overflowPopover.visible"
        ref="overflowPopoverRef"
        :class="['bz-table__overflow-popover', `is-${overflowPopover.placement}`]"
        :style="overflowPopoverStyle"
        @mouseenter="onPopoverMouseEnter"
        @mouseleave="onPopoverMouseLeave"
      >
        <div class="bz-table__overflow-content">{{ overflowPopover.text }}</div>
        <button
          type="button"
          class="bz-table__overflow-copy"
          aria-label="复制全部内容"
          @click.stop="copyOverflowText"
        >
          <span
            class="bz-table__copy-glyph"
            aria-hidden="true"
          >
            <svg
              v-if="overflowPopover.copied"
              class="bz-table__copy-svg is-check"
              viewBox="0 0 24 24"
              fill="none"
            >
              <circle
                cx="12"
                cy="12"
                r="8"
                stroke="currentColor"
                stroke-width="1.9"
              />
              <path
                d="M8.4 12.3l2.2 2.2 5-5"
                stroke="currentColor"
                stroke-width="1.9"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <svg
              v-else
              class="bz-table__copy-svg is-copy"
              viewBox="0 0 24 24"
              fill="none"
            >
              <rect
                x="9"
                y="9"
                width="9"
                height="9"
                rx="2.2"
                stroke="currentColor"
                stroke-width="1.8"
              />
              <path
                d="M6.5 14.8V7.8A1.8 1.8 0 0 1 8.3 6h7"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
              />
            </svg>
          </span>
        </button>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import type { PropType, StyleValue } from "vue";
import {
  computed,
  defineComponent,
  h,
  nextTick,
  onBeforeUnmount,
  onMounted,
  provide,
  ref,
  watch,
} from "vue";

import type { BzTableColumnDef, BzTableRow } from "./tableContext";
import { bzTableContextKey } from "./tableContext";

defineOptions({
  name: "BzTable",
});

interface BzTableTooltipOptions {
  placement?: string;
  offset?: number;
  maxWidth?: number;
}

type PopoverPlacement = "top" | "bottom";

interface ParsedPlacement {
  vertical: PopoverPlacement;
  align: "start" | "center" | "end";
}

const props = withDefaults(
  defineProps<{
    data: BzTableRow[];
    rowKey?: string | ((row: BzTableRow) => string | number);
    loading?: boolean;
    emptyText?: string;
    size?: "small" | "medium";
    tooltipOptions?: BzTableTooltipOptions;
  }>(),
  {
    rowKey: "",
    loading: false,
    emptyText: "暂无数据",
    size: "medium",
    tooltipOptions: () => ({
      placement: "bottom-start",
      offset: 0,
      maxWidth: 560,
    }),
  },
);

const emit = defineEmits<{
  (e: "selection-change", rows: BzTableRow[]): void;
  (e: "row-dblclick", row: BzTableRow, rowIndex: number): void;
}>();

const columns = ref<BzTableColumnDef[]>([]);
const selectedKeys = ref<Set<string | number>>(new Set());
const selectAllRef = ref<HTMLInputElement | null>(null);
const overflowPopoverRef = ref<HTMLDivElement | null>(null);
const hidePopoverTimer = ref<number | undefined>(undefined);
const isHoveringAnchorCell = ref(false);
const isHoveringPopover = ref(false);

const overflowPopover = ref({
  visible: false,
  text: "",
  left: 0,
  top: 0,
  placement: "bottom" as PopoverPlacement,
  copied: false,
  anchor: null as HTMLElement | null,
});

const overflowPopoverStyle = computed(() => ({
  left: `${overflowPopover.value.left}px`,
  top: `${overflowPopover.value.top}px`,
  maxWidth: `${props.tooltipOptions.maxWidth ?? 560}px`,
}));

function registerColumn(column: BzTableColumnDef) {
  if (columns.value.some((item) => item.id === column.id)) {
    return;
  }
  columns.value.push(column);
}

function unregisterColumn(id: symbol) {
  columns.value = columns.value.filter((item) => item.id !== id);
}

provide(bzTableContextKey, {
  registerColumn,
  unregisterColumn,
});

const selectionColumn = computed(() => columns.value.find((column) => column.type === "selection"));

const selectableKeys = computed<Array<string | number>>(() => {
  if (!selectionColumn.value) {
    return [];
  }
  return props.data
    .filter((row) => isRowSelectable(row, selectionColumn.value))
    .map((row, index) => resolveRowKey(row, index));
});

const allSelected = computed(() => {
  if (selectableKeys.value.length === 0) {
    return false;
  }
  return selectableKeys.value.every((key) => selectedKeys.value.has(key));
});

const indeterminate = computed(() => {
  if (selectableKeys.value.length === 0) {
    return false;
  }
  const selectedCount = selectableKeys.value.filter((key) => selectedKeys.value.has(key)).length;
  return selectedCount > 0 && selectedCount < selectableKeys.value.length;
});

watch(
  indeterminate,
  (value) => {
    if (selectAllRef.value) {
      selectAllRef.value.indeterminate = value;
    }
  },
  { immediate: true },
);

watch(
  () => props.data,
  () => {
    const validKeys = new Set(props.data.map((row, index) => resolveRowKey(row, index)));
    const next = new Set<string | number>();
    selectedKeys.value.forEach((key) => {
      if (validKeys.has(key)) {
        next.add(key);
      }
    });
    selectedKeys.value = next;
    emitSelectionChange();
  },
  { deep: true },
);

function resolveRowKey(row: BzTableRow, index: number): string | number {
  if (typeof props.rowKey === "function") {
    return props.rowKey(row);
  }
  if (typeof props.rowKey === "string" && props.rowKey.length > 0) {
    const value = row[props.rowKey];
    if (typeof value === "string" || typeof value === "number") {
      return value;
    }
  }
  return index;
}

function isRowSelectable(row: BzTableRow, column?: BzTableColumnDef): boolean {
  if (!column?.selectable) {
    return true;
  }
  return column.selectable(row);
}

function isRowSelected(row: BzTableRow, index: number): boolean {
  return selectedKeys.value.has(resolveRowKey(row, index));
}

function onSelectAllChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const next = new Set(selectedKeys.value);
  if (target.checked) {
    selectableKeys.value.forEach((key) => next.add(key));
  } else {
    selectableKeys.value.forEach((key) => next.delete(key));
  }
  selectedKeys.value = next;
  emitSelectionChange();
}

function onRowSelectChange(event: Event, row: BzTableRow, index: number) {
  const target = event.target as HTMLInputElement;
  const key = resolveRowKey(row, index);
  const next = new Set(selectedKeys.value);
  if (target.checked) {
    next.add(key);
  } else {
    next.delete(key);
  }
  selectedKeys.value = next;
  emitSelectionChange();
}

function onRowDblClick(row: BzTableRow, rowIndex: number) {
  emit("row-dblclick", row, rowIndex);
}

function emitSelectionChange() {
  const rows = props.data.filter((row, index) => selectedKeys.value.has(resolveRowKey(row, index)));
  emit("selection-change", rows);
}

function clearSelection() {
  selectedKeys.value = new Set();
  emitSelectionChange();
}

function headerClass(column: BzTableColumnDef): string {
  return `is-${column.align ?? "left"}`;
}

function cellClass(column: BzTableColumnDef): string[] {
  const classes = [`is-${column.align ?? "left"}`];
  if (column.ellipsis) {
    classes.push("is-ellipsis");
  }
  return classes;
}

function onCellMouseEnter(event: MouseEvent, column: BzTableColumnDef) {
  if (!column.ellipsis) {
    return;
  }

  const cell = event.currentTarget as HTMLTableCellElement | null;
  if (!cell) {
    return;
  }

  const text = (cell.innerText || "").trim();
  if (!text || text === "-") {
    return;
  }

  const hasOverflow = cell.scrollWidth > cell.clientWidth || cell.scrollHeight > cell.clientHeight;
  if (!hasOverflow) {
    return;
  }

  const anchor = resolveAnchorElement(event, cell);
  showOverflowPopover(anchor, text);
}

function onCellMouseLeave(event: MouseEvent, column: BzTableColumnDef) {
  isHoveringAnchorCell.value = false;
  if (!column.ellipsis) {
    return;
  }

  if (isInPopover(event.relatedTarget)) {
    isHoveringPopover.value = true;
    clearHidePopoverTimer();
    return;
  }

  scheduleHidePopover(360);
}

function onPopoverMouseEnter() {
  isHoveringPopover.value = true;
  clearHidePopoverTimer();
}

function onPopoverMouseLeave(event: MouseEvent) {
  isHoveringPopover.value = false;
  if (isInAnchor(event.relatedTarget)) {
    isHoveringAnchorCell.value = true;
    clearHidePopoverTimer();
    return;
  }
  scheduleHidePopover(320);
}

function showOverflowPopover(anchor: HTMLElement, text: string) {
  clearHidePopoverTimer();
  isHoveringAnchorCell.value = true;
  isHoveringPopover.value = false;
  overflowPopover.value.visible = true;
  overflowPopover.value.text = text;
  overflowPopover.value.anchor = anchor;
  overflowPopover.value.copied = false;
  void positionOverflowPopover();
}

function scheduleHidePopover(delay: number) {
  clearHidePopoverTimer();
  hidePopoverTimer.value = window.setTimeout(() => {
    hidePopoverTimer.value = undefined;
    if (isHoveringAnchorCell.value || isHoveringPopover.value) {
      return;
    }
    overflowPopover.value.visible = false;
    overflowPopover.value.anchor = null;
    overflowPopover.value.copied = false;
    isHoveringAnchorCell.value = false;
    isHoveringPopover.value = false;
  }, delay);
}

function clearHidePopoverTimer() {
  if (hidePopoverTimer.value === undefined) {
    return;
  }
  window.clearTimeout(hidePopoverTimer.value);
  hidePopoverTimer.value = undefined;
}

function resolveAnchorElement(event: MouseEvent, cell: HTMLTableCellElement): HTMLElement {
  const pointNode = document.elementFromPoint(event.clientX, event.clientY);
  if (pointNode instanceof HTMLElement && cell.contains(pointNode)) {
    const textHost = findTextHost(pointNode, cell);
    if (textHost) {
      return textHost;
    }
  }

  const firstTextHost = findFirstTextHost(cell);
  if (firstTextHost) {
    return firstTextHost;
  }

  return cell;
}

function findTextHost(start: HTMLElement, boundary: HTMLElement): HTMLElement | null {
  let current: HTMLElement | null = start;
  while (current && current !== boundary) {
    if (isTextHost(current)) {
      return current;
    }
    current = current.parentElement;
  }
  return null;
}

function findFirstTextHost(root: HTMLElement): HTMLElement | null {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT);
  let node = walker.nextNode();
  while (node) {
    if (node instanceof HTMLElement && isTextHost(node)) {
      return node;
    }
    node = walker.nextNode();
  }
  return null;
}

function isTextHost(element: HTMLElement): boolean {
  const tag = element.tagName;
  if (
    tag === "BUTTON" ||
    tag === "INPUT" ||
    tag === "TEXTAREA" ||
    tag === "SVG" ||
    tag === "PATH"
  ) {
    return false;
  }
  return (element.innerText || "").trim().length > 0;
}

function isInPopover(target: EventTarget | null): boolean {
  return target instanceof Node && !!overflowPopoverRef.value?.contains(target);
}

function isInAnchor(target: EventTarget | null): boolean {
  return target instanceof Node && !!overflowPopover.value.anchor?.contains(target);
}

async function positionOverflowPopover() {
  const anchor = overflowPopover.value.anchor;
  if (!anchor || !overflowPopover.value.visible) {
    return;
  }

  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  const padding = 8;
  const placement = parsePlacement(props.tooltipOptions.placement);
  const offset = props.tooltipOptions.offset ?? 0;
  const arrowSize = 4;
  const distance = offset + arrowSize;
  const rect = anchor.getBoundingClientRect();

  if (rect.width === 0 && rect.height === 0) {
    scheduleHidePopover(0);
    return;
  }

  await nextTick();
  const popoverWidth =
    overflowPopoverRef.value?.offsetWidth ??
    Math.min(props.tooltipOptions.maxWidth ?? 560, viewportWidth - padding * 2);
  const popoverHeight = overflowPopoverRef.value?.offsetHeight ?? 0;
  const topCandidate = rect.top - popoverHeight - distance;
  const bottomCandidate = rect.bottom + distance;
  const canShowTop = topCandidate >= padding;
  const canShowBottom = bottomCandidate + popoverHeight <= viewportHeight - padding;

  let finalPlacement: PopoverPlacement = placement.vertical;
  if (placement.vertical === "bottom" && !canShowBottom && canShowTop) {
    finalPlacement = "top";
  } else if (placement.vertical === "top" && !canShowTop && canShowBottom) {
    finalPlacement = "bottom";
  }

  overflowPopover.value.placement = finalPlacement;
  overflowPopover.value.top =
    finalPlacement === "bottom"
      ? Math.min(viewportHeight - popoverHeight - padding, bottomCandidate)
      : Math.max(padding, topCandidate);

  let alignedLeft = rect.left;
  if (placement.align === "center") {
    alignedLeft = rect.left + rect.width / 2 - popoverWidth / 2;
  } else if (placement.align === "end") {
    alignedLeft = rect.right - popoverWidth;
  }
  overflowPopover.value.left = Math.max(
    padding,
    Math.min(alignedLeft, viewportWidth - popoverWidth - padding),
  );
}

function parsePlacement(rawPlacement?: string): ParsedPlacement {
  const value = (rawPlacement ?? "bottom-start").toLowerCase();
  const [verticalRaw, alignRaw] = value.split("-");
  const vertical: PopoverPlacement = verticalRaw === "top" ? "top" : "bottom";
  const align = alignRaw === "end" ? "end" : alignRaw === "center" ? "center" : "start";
  return { vertical, align };
}

async function copyOverflowText() {
  const text = overflowPopover.value.text;
  if (!text) {
    return;
  }

  const copied = await copyText(text);
  overflowPopover.value.copied = copied;
  if (!copied) {
    return;
  }

  window.setTimeout(() => {
    overflowPopover.value.copied = false;
  }, 1200);
}

async function copyText(text: string): Promise<boolean> {
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // ignore and fallback
    }
  }

  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "true");
  textarea.style.position = "fixed";
  textarea.style.left = "-9999px";
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand("copy");
  document.body.removeChild(textarea);
  return copied;
}

function columnStyle(column: BzTableColumnDef): StyleValue {
  const style: Record<string, string> = {};
  if (column.width !== undefined) {
    style.width = normalizeSize(column.width);
  }
  if (column.minWidth !== undefined) {
    style.minWidth = normalizeSize(column.minWidth);
  }
  if (column.fixed === "right") {
    style.position = "sticky";
    style.right = "0";
    style.zIndex = "2";
    style.background = "#fff";
  }
  return style;
}

function normalizeSize(size: number | string): string {
  if (typeof size === "number") {
    return `${size}px`;
  }
  if (/^\d+$/.test(size)) {
    return `${size}px`;
  }
  return size;
}

const TableCellRenderer = defineComponent({
  name: "TableCellRenderer",
  props: {
    column: {
      type: Object as PropType<BzTableColumnDef>,
      required: true,
    },
    row: {
      type: Object as PropType<BzTableRow>,
      required: true,
    },
    rowIndex: {
      type: Number,
      required: true,
    },
  },
  setup(cellProps) {
    return () => {
      if (cellProps.column.renderCell) {
        return cellProps.column.renderCell({ row: cellProps.row, $index: cellProps.rowIndex });
      }
      if (!cellProps.column.field) {
        return "-";
      }
      const value = cellProps.row[cellProps.column.field];
      const text = value === null || value === undefined || value === "" ? "-" : String(value);
      if (cellProps.column.ellipsis) {
        return h("span", { class: "bz-table__text" }, text);
      }
      return text;
    };
  },
});

defineExpose<{
  clearSelection: () => void;
}>({
  clearSelection,
});

onMounted(() => {
  if (selectAllRef.value) {
    selectAllRef.value.indeterminate = indeterminate.value;
  }
  window.addEventListener("resize", positionOverflowPopover);
  window.addEventListener("scroll", positionOverflowPopover, true);
});

onBeforeUnmount(() => {
  clearHidePopoverTimer();
  window.removeEventListener("resize", positionOverflowPopover);
  window.removeEventListener("scroll", positionOverflowPopover, true);
  selectedKeys.value = new Set();
});
</script>

<style scoped>
.bz-table {
  position: relative;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.bz-table__column-slot {
  display: none;
}

.bz-table__scroll {
  overflow: auto;
}

.bz-table__inner {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}

.bz-table th,
.bz-table td {
  border-bottom: 1px solid #e2e8f0;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text-main);
  vertical-align: middle;
  background: #fff;
}

.bz-table th {
  background: #f8fafc;
  font-weight: 600;
}

.bz-table tr:last-child td {
  border-bottom: none;
}

.bz-table .is-left {
  text-align: left;
}

.bz-table .is-center {
  text-align: center;
}

.bz-table .is-right {
  text-align: right;
}

.bz-table .is-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bz-table__text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bz-table__overflow-popover {
  position: fixed;
  z-index: 4000;
  min-width: 120px;
  max-width: min(560px, calc(100vw - 16px));
  background: #334155;
  color: #e2e8f0;
  border: 1px solid #475569;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.24);
  padding: 7px 34px 7px 10px;
  box-sizing: border-box;
}

.bz-table__overflow-popover::after {
  content: "";
  position: absolute;
  left: 12px;
  width: 7px;
  height: 7px;
  background: #334155;
  border-left: 1px solid #475569;
  border-top: 1px solid #475569;
  transform: rotate(45deg);
}

.bz-table__overflow-popover.is-bottom::after {
  top: -4px;
}

.bz-table__overflow-popover.is-top::after {
  bottom: -4px;
  transform: rotate(225deg);
}

.bz-table__overflow-content {
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  overflow-x: auto;
  overflow-y: hidden;
  word-break: normal;
  max-width: 100%;
  padding-bottom: 1px;
  user-select: text;
}

.bz-table__overflow-copy {
  position: absolute;
  top: 5px;
  right: 6px;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 4px;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.bz-table__overflow-copy:hover {
  background: rgba(148, 163, 184, 0.22);
}

.bz-table__copy-glyph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.bz-table__copy-svg {
  width: 14px;
  height: 14px;
  display: block;
  transition:
    color 0.16s ease,
    transform 0.16s ease;
}

.bz-table__copy-svg.is-copy {
  color: #cbd5e1;
}

.bz-table__copy-svg.is-check {
  color: #34d399;
}

.bz-table__overflow-copy:hover .bz-table__copy-svg.is-copy {
  color: #93c5fd;
}

.bz-table__overflow-copy:active .bz-table__copy-svg {
  transform: scale(0.94);
}

.bz-table__checkbox {
  width: 14px;
  height: 14px;
  cursor: pointer;
}

.bz-table__empty {
  text-align: center;
  color: var(--text-muted);
  padding: 24px 12px;
}

.bz-table__loading {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.68);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 13px;
}

.bz-table__loading-spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  border-top-color: #3b82f6;
  animation: bz-table-spin 0.8s linear infinite;
}

.bz-table--small th,
.bz-table--small td {
  padding-top: 8px;
  padding-bottom: 8px;
}

@keyframes bz-table-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
