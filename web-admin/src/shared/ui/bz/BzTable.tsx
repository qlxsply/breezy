import type { CSSProperties, DragEvent, MouseEvent, ReactNode } from "react";

import { BzLoading } from "./BzLoading";

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
  rowClassName?: (row: Row, rowIndex: number) => string | undefined;
  rowSelection?: BzTableRowSelection<Row>;
  onRowDragOver?: (event: DragEvent<HTMLTableRowElement>, row: Row, rowIndex: number) => void;
  onRowDrop?: (event: DragEvent<HTMLTableRowElement>, row: Row, rowIndex: number) => void;
  onRowDoubleClick?: (row: Row, rowIndex: number) => void;
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
      className="bz-table__loading-wrap"
    >
      <div className={`bz-table bz-table--${size}`}>
        <div className="bz-table__scroll">
          <table className="bz-table__inner">
            <thead>
              <tr>
                {rowSelection ? (
                  <th
                    className={`bz-table__selection-cell${rowSelection.columnClassName ? ` ${rowSelection.columnClassName}` : ""}`}
                    style={{
                      width: resolveCssSize(rowSelection.columnWidth ?? 48),
                      minWidth: resolveCssSize(rowSelection.columnWidth ?? 48),
                    }}
                  >
                    <input
                      className="bz-table__selection-checkbox"
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
                    className={column.headerClassName}
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
                        rowSelection && rowSelectable ? "is-selectable" : "",
                        rowSelection && !rowSelectable ? "is-selection-disabled" : "",
                        rowSelection && rowSelected ? "is-selected" : "",
                      ]
                        .filter(Boolean)
                        .join(" ")}
                      onDragOver={(event) => onRowDragOver?.(event, row, rowIndex)}
                      onDrop={(event) => onRowDrop?.(event, row, rowIndex)}
                      onDoubleClick={() => onRowDoubleClick?.(row, rowIndex)}
                    >
                      {rowSelection ? (
                        <td
                          className={`bz-table__selection-cell${rowSelection.columnClassName ? ` ${rowSelection.columnClassName}` : ""}`}
                          style={{
                            width: resolveCssSize(rowSelection.columnWidth ?? 48),
                            minWidth: resolveCssSize(rowSelection.columnWidth ?? 48),
                          }}
                        >
                          <input
                            className="bz-table__selection-checkbox"
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
                          className={column.className}
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
                    className="bz-table__empty"
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
