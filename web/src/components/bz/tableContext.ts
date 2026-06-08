import type { InjectionKey, VNodeChild } from "vue";

export type BzTableRow = Record<string, unknown>;

export interface BzTableCellScope {
  row: BzTableRow;
  $index: number;
}

export interface BzTableColumnDef {
  id: symbol;
  type: "default" | "selection";
  field?: string;
  title?: string;
  width?: number | string;
  minWidth?: number | string;
  align?: "left" | "center" | "right";
  fixed?: "left" | "right";
  ellipsis?: boolean;
  selectable?: (row: BzTableRow) => boolean;
  renderCell?: (scope: BzTableCellScope) => VNodeChild;
}

export interface BzTableContext {
  registerColumn: (column: BzTableColumnDef) => void;
  unregisterColumn: (id: symbol) => void;
}

export const bzTableContextKey: InjectionKey<BzTableContext> = Symbol("bzTableContext");
