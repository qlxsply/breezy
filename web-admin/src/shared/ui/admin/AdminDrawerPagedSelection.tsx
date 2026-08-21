"use client";

import { TableInput } from "@admin/shared/ui/admin/inputs";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import { BzLoading } from "@admin/shared/ui/bz/BzLoading";
import { BzPagination } from "@admin/shared/ui/bz/BzPagination";
import type { ReactNode } from "react";
import { useEffect, useMemo, useRef } from "react";

export interface AdminDrawerPagedSelectionColumn<T> {
  key: string;
  title: ReactNode;
  render: (row: T) => ReactNode;
}

interface AdminDrawerPagedSelectionProps<T> {
  title: string;
  rows: T[];
  rowKey: (row: T) => string;
  columns: Array<AdminDrawerPagedSelectionColumn<T>>;
  gridTemplateColumns: string;
  selectedKeys: string[];
  total: number;
  pageNo: number;
  pageSize: number;
  keyword: string;
  loading?: boolean;
  editable?: boolean;
  searchPlaceholder?: string;
  emptyText?: string;
  isRowSelectable?: (row: T) => boolean;
  onKeywordChange: (value: string) => void;
  onSearch: () => void;
  onReset: () => void;
  onPageChange: (pageNo: number) => void;
  onSelectedKeysChange: (keys: string[]) => void;
}

export function AdminDrawerPagedSelection<T>({
  title,
  rows,
  rowKey,
  columns,
  gridTemplateColumns,
  selectedKeys,
  total,
  pageNo,
  pageSize,
  keyword,
  loading = false,
  editable = false,
  searchPlaceholder = "请输入搜索条件",
  emptyText = "暂无数据",
  isRowSelectable = () => true,
  onKeywordChange,
  onSearch,
  onReset,
  onPageChange,
  onSelectedKeysChange,
}: AdminDrawerPagedSelectionProps<T>) {
  const selectAllRef = useRef<HTMLInputElement | null>(null);
  const selectedSet = useMemo(() => new Set(selectedKeys), [selectedKeys]);
  const currentPageKeys = useMemo(
    () => rows.filter(isRowSelectable).map(rowKey),
    [isRowSelectable, rowKey, rows],
  );
  const currentPageAllSelected =
    currentPageKeys.length > 0 && currentPageKeys.every((key) => selectedSet.has(key));
  const currentPageSomeSelected = currentPageKeys.some((key) => selectedSet.has(key));

  useEffect(() => {
    if (selectAllRef.current) {
      selectAllRef.current.indeterminate = !currentPageAllSelected && currentPageSomeSelected;
    }
  }, [currentPageAllSelected, currentPageSomeSelected]);

  function updateRow(key: string, checked: boolean) {
    const next = new Set(selectedSet);
    if (checked) next.add(key);
    else next.delete(key);
    onSelectedKeysChange(Array.from(next));
  }

  function updateCurrentPage(checked: boolean) {
    const next = new Set(selectedSet);
    currentPageKeys.forEach((key) => {
      if (checked) next.add(key);
      else next.delete(key);
    });
    onSelectedKeysChange(Array.from(next));
  }

  const template = `44px ${gridTemplateColumns}`;

  return (
    <section
      className={`admin-entity-section admin-drawer-paged-selection${editable ? " is-editable" : ""}`}
    >
      <div className="admin-entity-section__head">
        <div className="admin-entity-section__title">{title}</div>
      </div>

      <div className="admin-drawer-paged-selection__toolbar">
        <TableInput
          value={keyword}
          placeholder={searchPlaceholder}
          className="admin-drawer-paged-selection__search"
          onValueChange={onKeywordChange}
          onKeyUp={(event) => {
            if (event.key === "Enter") onSearch();
          }}
        />
        <div className="admin-drawer-paged-selection__actions">
          <BzButton onClick={onReset}>重置</BzButton>
          <BzButton onClick={onSearch}>搜索</BzButton>
        </div>
      </div>

      <div className="admin-grid-table admin-drawer-paged-selection__table">
        <div className="admin-grid-table__viewport">
          <div
            className="admin-grid-table__row admin-grid-table__row--head"
            style={{ gridTemplateColumns: template }}
          >
            <div className="admin-grid-table__cell admin-grid-table__cell--check">
              {editable ? (
                <input
                  ref={selectAllRef}
                  className="admin-node-checkbox"
                  type="checkbox"
                  checked={currentPageAllSelected}
                  disabled={currentPageKeys.length === 0}
                  onChange={(event) => updateCurrentPage(event.target.checked)}
                />
              ) : null}
            </div>
            {columns.map((column) => (
              <div
                className="admin-grid-table__cell"
                key={column.key}
              >
                {column.title}
              </div>
            ))}
          </div>

          <BzLoading
            loading={loading && rows.length > 0}
            text="加载中..."
          >
            <div className="admin-grid-table__body">
              {rows.length === 0 ? (
                <div className="admin-permission-empty-state">
                  {loading ? "加载中..." : emptyText}
                </div>
              ) : (
                rows.map((row) => {
                  const key = rowKey(row);
                  const selectable = isRowSelectable(row);
                  const selected = selectedSet.has(key);
                  return (
                    <div
                      key={key}
                      className={`admin-grid-table__row admin-drawer-paged-selection__row${selected ? " is-selected" : ""}`}
                      style={{ gridTemplateColumns: template }}
                      aria-selected={selected}
                      onClick={() => {
                        if (editable && selectable) updateRow(key, !selected);
                      }}
                    >
                      <div className="admin-grid-table__cell admin-grid-table__cell--check">
                        <input
                          className="admin-node-checkbox"
                          type="checkbox"
                          checked={selected}
                          disabled={!editable || !selectable}
                          onClick={(event) => event.stopPropagation()}
                          onChange={(event) => updateRow(key, event.target.checked)}
                        />
                      </div>
                      {columns.map((column) => (
                        <div
                          className="admin-grid-table__cell"
                          key={column.key}
                        >
                          {column.render(row)}
                        </div>
                      ))}
                    </div>
                  );
                })
              )}
            </div>
          </BzLoading>
        </div>

        {total > 0 ? (
          <div className="dict-pagination-bar admin-grid-table__footer">
            <div className="dict-pagination-summary">
              共 {total} 条记录，已选 {selectedKeys.length} 项
            </div>
            <div className="dict-pagination-right">
              <BzPagination
                total={total}
                pageSize={pageSize}
                currentPage={pageNo}
                showSizeChanger={false}
                showFirstLast={false}
                showJumper={false}
                onCurrentChange={onPageChange}
              />
            </div>
          </div>
        ) : null}
      </div>
    </section>
  );
}
