"use client";

import { useEffect, useMemo, useState } from "react";

interface BzPaginationProps {
  total: number;
  pageSize: number;
  currentPage: number;
  pageSizes?: number[];
  onCurrentChange?: (pageNo: number) => void;
  onSizeChange?: (pageSize: number) => void;
}

export function BzPagination({
  total,
  pageSize,
  currentPage,
  pageSizes = [10, 20, 50, 100],
  onCurrentChange,
  onSizeChange,
}: BzPaginationProps) {
  const totalPages = useMemo(() => (total <= 0 ? 1 : Math.max(1, Math.ceil(total / pageSize))), [pageSize, total]);
  const isFirstPage = currentPage <= 1;
  const isLastPage = currentPage >= totalPages;
  const [jumpValue, setJumpValue] = useState(String(currentPage));

  useEffect(() => {
    setJumpValue(String(currentPage));
  }, [currentPage]);

  const pageTokens = useMemo<Array<number | "ellipsis">>(() => {
    const current = Math.min(Math.max(currentPage, 1), totalPages);
    if (totalPages <= 7) return Array.from({ length: totalPages }, (_, index) => index + 1);
    if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", totalPages];
    if (current >= totalPages - 3) return [1, "ellipsis", totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
    return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", totalPages];
  }, [currentPage, totalPages]);

  function goToPage(pageNo: number) {
    const clamped = Math.min(Math.max(pageNo, 1), totalPages);
    if (clamped === currentPage) return;
    onCurrentChange?.(clamped);
  }

  return (
    <div className="bz-pagination">
      <div className="bz-pagination__size">
        <span>每页</span>
        <select
          className="bz-pagination__size-select"
          value={pageSize}
          onChange={(event) => {
            const nextPageSize = Number(event.currentTarget.value);
            if (!Number.isFinite(nextPageSize) || nextPageSize <= 0) return;
            onSizeChange?.(nextPageSize);
          }}
        >
          {pageSizes.map((size) => (
            <option key={size} value={size}>
              {size}
            </option>
          ))}
        </select>
        <span>项</span>
      </div>
      <div className="bz-pagination__pages">
        <button className="bz-page-btn" type="button" disabled={isFirstPage} onClick={() => goToPage(1)}>
          首页
        </button>
        <button className="bz-page-btn" type="button" disabled={isFirstPage} onClick={() => goToPage(currentPage - 1)}>
          上一页
        </button>
        {pageTokens.map((token, index) =>
          typeof token === "number" ? (
            <button
              key={`${token}-${index}`}
              className={`bz-page-btn${token === currentPage ? " is-active" : ""}`}
              type="button"
              onClick={() => goToPage(token)}
            >
              {token}
            </button>
          ) : (
            <span key={`${token}-${index}`} className="bz-page-ellipsis">
              ...
            </span>
          ),
        )}
        <button className="bz-page-btn" type="button" disabled={isLastPage} onClick={() => goToPage(currentPage + 1)}>
          下一页
        </button>
        <button className="bz-page-btn" type="button" disabled={isLastPage} onClick={() => goToPage(totalPages)}>
          末页
        </button>
      </div>
      <form
        className="bz-pagination__jump"
        onSubmit={(event) => {
          event.preventDefault();
          const raw = Number(jumpValue);
          if (!Number.isFinite(raw)) return;
          goToPage(Math.trunc(raw));
        }}
      >
        <span>跳转</span>
        <input className="bz-pagination__jump-input" type="number" min={1} max={totalPages} value={jumpValue} onChange={(event) => setJumpValue(event.currentTarget.value)} />
        <span>页</span>
        <button className="bz-page-btn" type="submit">
          确定
        </button>
      </form>
    </div>
  );
}
