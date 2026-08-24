import type { CSSProperties, DragEvent, MouseEvent, ReactNode } from "react";

import { BzLoading } from "./BzLoading";
import styles from "./BzTable.module.css";

export interface BzTableColumn<Row> {
  key: string;
  title: string;
  width?: number | string;
  minWidth?: number | string;
  className?: string;
  headerClassName?: string;
  disableRowSelection?: boolean;
  headerRender?: () => ReactNode;
  render?: (row: Row, rowIndex: number) => ReactNode;
}

export interface BzTableRowSelection<Row> {
  selectedRowKeys: readonly (string | number)[];
  isRowSelectable?: (row: Row, rowIndex: number) => boolean;
  onToggle: (row: Row, selected: boolean, rowIndex: number) => void;
  onToggleCurrentPage?: (rows: Row[], selected: boolean) => void;
  columnWidth?: number | string;
  columnClassName?: string;
}

interface BzTableProps<Row> {
  data: Row[];
  columns: Array<BzTableColumn<Row>>;
  rowKey?: keyof Row | ((row: Row, rowIndex: number) => string | number);
  loading?: boolean;
  emptyText?: string;
  size?: "small" | "medium";
  className?: string;
  scrollClassName?: string;
  tableClassName?: string;
  rowClassName?: (row: Row, rowIndex: number) => string | undefined;
  rowSelection?: BzTableRowSelection<Row>;
  onRowDragOver?: (event: DragEvent<HTMLTableRowElement>, row: Row, rowIndex: number) => void;
  onRowDrop?: (event: DragEvent<HTMLTableRowElement>, row: Row, rowIndex: number) => void;
  onRowDoubleClick?: (row: Row, rowIndex: number) => void;
}

const internalClassNames: Record<string, string> = {
  "is-fixed-left": styles.fixedLeft,
  "is-fixed-right": styles.fixedRight,
  "is-sticky-left": styles.stickyLeft,
  "is-sticky-right": styles.stickyRight,
};

function resolveClassName(value?: string): string | undefined {
  if (!value) return undefined;
  return value
    .split(/\s+/)
    .map((name) => internalClassNames[name] ?? name)
    .join(" ");
}

function resolveCssSize(value?: number | string): string | undefined {
  if (value === undefined) return undefined;
  return typeof value === "number" ? `${value}px` : value;
}

export function BzTable<Row>({
  data,
  columns,
  rowKey,
  loading = false,
  emptyText = "暂无数据",
  size = "medium",
  className,
  scrollClassName,
  tableClassName,
  rowClassName,
  rowSelection,
  onRowDragOver,
  onRowDrop,
  onRowDoubleClick,
}: BzTableProps<Row>) {
  function resolveRowKey(row: Row, rowIndex: number): string | number {
    if (typeof rowKey === "function") return rowKey(row, rowIndex);
    if (rowKey) return String(row[rowKey] as string | number);
    return rowIndex;
  }

  function resolveColumnStyle(column: BzTableColumn<Row>): CSSProperties {
    return {
      width: resolveCssSize(column.width),
      minWidth: resolveCssSize(column.minWidth ?? column.width),
    };
  }

  const selectedKeys = new Set(rowSelection?.selectedRowKeys.map(String));
  const selectableEntries = rowSelection
    ? data
        .map((row, rowIndex) => ({ row, rowIndex }))
        .filter(({ row, rowIndex }) => rowSelection.isRowSelectable?.(row, rowIndex) !== false)
    : [];
  const allCurrentPageSelected =
    selectableEntries.length > 0 &&
    selectableEntries.every(({ row, rowIndex }) =>
      selectedKeys.has(String(resolveRowKey(row, rowIndex))),
    );
  const someCurrentPageSelected =
    !allCurrentPageSelected &&
    selectableEntries.some(({ row, rowIndex }) =>
      selectedKeys.has(String(resolveRowKey(row, rowIndex))),
    );

  function toggleFromCell(
    event: MouseEvent<HTMLTableCellElement>,
    row: Row,
    rowIndex: number,
    column: BzTableColumn<Row>,
  ) {
    if (!rowSelection || column.disableRowSelection) return;
    if (rowSelection.isRowSelectable?.(row, rowIndex) === false) return;
    const target = event.target;
    if (
      target instanceof Element &&
      target.closest("button, a, input, select, textarea, label, [role='button'], [role='link']")
    ) {
      return;
    }
    const selected = selectedKeys.has(String(resolveRowKey(row, rowIndex)));
    rowSelection.onToggle(row, !selected, rowIndex);
  }

  return (
    <BzLoading
      loading={loading}
      className={styles.loadingWrap}
    >
      <div className={[styles.table, styles[size], className].filter(Boolean).join(" ")}>
        <div className={[styles.scroll, scrollClassName].filter(Boolean).join(" ")}>
          <table className={[styles.inner, tableClassName].filter(Boolean).join(" ")}>
            <thead>
              <tr>
                {rowSelection ? (
                  <th
                    className={[
                      styles.selectionCell,
                      resolveClassName(rowSelection.columnClassName),
                    ]
                      .filter(Boolean)
                      .join(" ")}
                    style={{
                      width: resolveCssSize(rowSelection.columnWidth ?? 48),
                      minWidth: resolveCssSize(rowSelection.columnWidth ?? 48),
                    }}
                  >
                    <input
                      className={styles.selectionCheckbox}
                      type="checkbox"
                      checked={allCurrentPageSelected}
                      disabled={selectableEntries.length === 0}
                      ref={(element) => {
                        if (element) element.indeterminate = someCurrentPageSelected;
                      }}
                      onChange={(event) =>
                        rowSelection.onToggleCurrentPage?.(
                          selectableEntries.map(({ row }) => row),
                          event.currentTarget.checked,
                        )
                      }
                    />
                  </th>
                ) : null}
                {columns.map((column) => (
                  <th
                    key={column.key}
                    style={resolveColumnStyle(column)}
                    className={resolveClassName(column.headerClassName)}
                  >
                    {column.headerRender ? column.headerRender() : column.title}
                  </th>
                ))}
              </tr>
            </thead>
            {data.length > 0 ? (
              <tbody>
                {data.map((row, rowIndex) => {
                  const rowSelectable = rowSelection?.isRowSelectable?.(row, rowIndex) !== false;
                  const rowSelected = selectedKeys.has(String(resolveRowKey(row, rowIndex)));
                  const customRowClass = rowClassName?.(row, rowIndex);
                  return (
                    <tr
                      key={String(resolveRowKey(row, rowIndex))}
                      className={[
                        customRowClass,
                        rowSelection && rowSelectable ? styles.selectable : "",
                        rowSelection && !rowSelectable ? styles.selectionDisabled : "",
                        rowSelection && rowSelected ? styles.selected : "",
                      ]
                        .filter(Boolean)
                        .join(" ")}
                      onDragOver={(event) => onRowDragOver?.(event, row, rowIndex)}
                      onDrop={(event) => onRowDrop?.(event, row, rowIndex)}
                      onDoubleClick={() => onRowDoubleClick?.(row, rowIndex)}
                    >
                      {rowSelection ? (
                        <td
                          className={[
                            styles.selectionCell,
                            resolveClassName(rowSelection.columnClassName),
                          ]
                            .filter(Boolean)
                            .join(" ")}
                          style={{
                            width: resolveCssSize(rowSelection.columnWidth ?? 48),
                            minWidth: resolveCssSize(rowSelection.columnWidth ?? 48),
                          }}
                        >
                          <input
                            className={styles.selectionCheckbox}
                            type="checkbox"
                            checked={rowSelected}
                            disabled={!rowSelectable}
                            onChange={(event) =>
                              rowSelection.onToggle(row, event.currentTarget.checked, rowIndex)
                            }
                          />
                        </td>
                      ) : null}
                      {columns.map((column) => (
                        <td
                          key={column.key}
                          style={resolveColumnStyle(column)}
                          className={resolveClassName(column.className)}
                          onClickCapture={(event) => toggleFromCell(event, row, rowIndex, column)}
                        >
                          {column.render ? column.render(row, rowIndex) : null}
                        </td>
                      ))}
                    </tr>
                  );
                })}
              </tbody>
            ) : (
              <tbody>
                <tr>
                  <td
                    className={styles.empty}
                    colSpan={Math.max(columns.length + (rowSelection ? 1 : 0), 1)}
                  >
                    {emptyText}
                  </td>
                </tr>
              </tbody>
            )}
          </table>
        </div>
      </div>
    </BzLoading>
  );
}
