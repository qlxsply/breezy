import type { CSSProperties, ReactNode } from "react";

import { BzLoading } from "./BzLoading";

export interface BzTableColumn<Row> {
  key: string;
  title: string;
  width?: number | string;
  minWidth?: number | string;
  className?: string;
  headerClassName?: string;
  render?: (row: Row, rowIndex: number) => ReactNode;
}

interface BzTableProps<Row> {
  data: Row[];
  columns: Array<BzTableColumn<Row>>;
  rowKey?: keyof Row | ((row: Row, rowIndex: number) => string | number);
  loading?: boolean;
  emptyText?: string;
  size?: "small" | "medium";
  onRowDoubleClick?: (row: Row, rowIndex: number) => void;
}

function resolveCssSize(value?: number | string): string | undefined {
  if (value === undefined) return undefined;
  return typeof value === "number" ? `${value}px` : value;
}

export function BzTable<Row>({ data, columns, rowKey, loading = false, emptyText = "暂无数据", size = "medium", onRowDoubleClick }: BzTableProps<Row>) {
  function resolveRowKey(row: Row, rowIndex: number): string | number {
    if (typeof rowKey === "function") return rowKey(row, rowIndex);
    if (rowKey) return String(row[rowKey] as string | number);
    return rowIndex;
  }

  function resolveColumnStyle(column: BzTableColumn<Row>): CSSProperties {
    return {
      width: resolveCssSize(column.width),
      minWidth: resolveCssSize(column.minWidth),
    };
  }

  return (
    <BzLoading loading={loading} className="bz-table__loading-wrap">
      <div className={`bz-table bz-table--${size}`}>
        <div className="bz-table__scroll">
          <table className="bz-table__inner">
            <thead>
              <tr>
                {columns.map((column) => (
                  <th key={column.key} style={resolveColumnStyle(column)} className={column.headerClassName}>
                    {column.title}
                  </th>
                ))}
              </tr>
            </thead>
            {data.length > 0 ? (
              <tbody>
                {data.map((row, rowIndex) => (
                  <tr key={String(resolveRowKey(row, rowIndex))} onDoubleClick={() => onRowDoubleClick?.(row, rowIndex)}>
                    {columns.map((column) => (
                      <td key={column.key} style={resolveColumnStyle(column)} className={column.className}>
                        {column.render ? column.render(row, rowIndex) : null}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            ) : (
              <tbody>
                <tr>
                  <td className="bz-table__empty" colSpan={Math.max(columns.length, 1)}>
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
