import type { CSSProperties, ReactNode } from "react";

import { BzLoading } from "./BzLoading";
import { BzOverflowTooltip } from "./BzOverflowTooltip";
import styles from "./BzSimpleTable.module.css";

export interface BzSimpleTableColumn<Row> {
  key: keyof Row | string;
  title: ReactNode;
  width?: number | string;
  minWidth?: number | string;
  align?: "left" | "center" | "right";
  render?: (row: Row, rowIndex: number) => ReactNode;
  text?: (row: Row, rowIndex: number) => string;
}

interface BzSimpleTableProps<Row> {
  data: Row[];
  columns: Array<BzSimpleTableColumn<Row>>;
  rowKey?: keyof Row | ((row: Row, rowIndex: number) => string | number);
  loading?: boolean;
  emptyText?: string;
}

function resolveCssSize(value?: number | string): string | undefined {
  if (value === undefined) return undefined;
  return typeof value === "number" ? `${value}px` : value;
}

export function BzSimpleTable<Row>({
  data,
  columns,
  rowKey,
  loading = false,
  emptyText = "暂无数据",
}: BzSimpleTableProps<Row>) {
  function resolveRowKey(row: Row, rowIndex: number): string | number {
    if (typeof rowKey === "function") return rowKey(row, rowIndex);
    if (rowKey) return String(row[rowKey] as string | number);
    return rowIndex;
  }

  function resolveColumnStyle(column: BzSimpleTableColumn<Row>): CSSProperties {
    return {
      width: resolveCssSize(column.width),
      minWidth: resolveCssSize(column.minWidth),
    };
  }

  function renderDefaultCell(row: Row, rowIndex: number, column: BzSimpleTableColumn<Row>) {
    const text = column.text
      ? column.text(row, rowIndex)
      : String((row as Record<string, unknown>)[String(column.key)] ?? "-");
    return (
      <BzOverflowTooltip text={text}>
        <span className={styles.cellText}>{text}</span>
      </BzOverflowTooltip>
    );
  }

  return (
    <BzLoading
      loading={loading}
      className={styles.loadingWrap}
    >
      <div className={styles.table}>
        <table className={styles.inner}>
          <thead>
            <tr>
              {columns.map((column) => (
                <th
                  key={String(column.key)}
                  style={resolveColumnStyle(column)}
                  className={column.align ? styles[column.align] : undefined}
                >
                  {column.title}
                </th>
              ))}
            </tr>
          </thead>
          {data.length > 0 ? (
            <tbody>
              {data.map((row, rowIndex) => (
                <tr key={String(resolveRowKey(row, rowIndex))}>
                  {columns.map((column) => (
                    <td
                      key={String(column.key)}
                      style={resolveColumnStyle(column)}
                      className={column.align ? styles[column.align] : undefined}
                    >
                      {column.render
                        ? column.render(row, rowIndex)
                        : renderDefaultCell(row, rowIndex, column)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          ) : (
            <tbody>
              <tr>
                <td
                  className={styles.empty}
                  colSpan={Math.max(columns.length, 1)}
                >
                  {emptyText}
                </td>
              </tr>
            </tbody>
          )}
        </table>
      </div>
    </BzLoading>
  );
}
