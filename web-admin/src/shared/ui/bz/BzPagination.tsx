"use client";

import { useEffect, useMemo, useState } from "react";

import styles from "./BzPagination.module.css";

interface BzPaginationProps {
  total: number;
  pageSize: number;
  currentPage: number;
  pageSizes?: number[];
  showSizeChanger?: boolean;
  showFirstLast?: boolean;
  showJumper?: boolean;
  onCurrentChange?: (pageNo: number) => void;
  onSizeChange?: (pageSize: number) => void;
}

export function BzPagination({
  total,
  pageSize,
  currentPage,
  pageSizes = [10, 20, 50, 100],
  showSizeChanger = true,
  showFirstLast = true,
  showJumper = true,
  onCurrentChange,
  onSizeChange,
}: BzPaginationProps) {
  const totalPages = useMemo(
    () => (total <= 0 ? 1 : Math.max(1, Math.ceil(total / pageSize))),
    [pageSize, total],
  );
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
    if (current >= totalPages - 3)
      return [
        1,
        "ellipsis",
        totalPages - 4,
        totalPages - 3,
        totalPages - 2,
        totalPages - 1,
        totalPages,
      ];
    return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", totalPages];
  }, [currentPage, totalPages]);

  function goToPage(pageNo: number) {
    const clamped = Math.min(Math.max(pageNo, 1), totalPages);
    if (clamped === currentPage) return;
    onCurrentChange?.(clamped);
  }

  return (
    <div className={styles.pagination}>
      {showSizeChanger ? (
        <div className={styles.size}>
          <span>每页</span>
          <select
            className={styles.sizeSelect}
            aria-label="每页条数"
            value={pageSize}
            onChange={(event) => {
              const nextPageSize = Number(event.currentTarget.value);
              if (!Number.isFinite(nextPageSize) || nextPageSize <= 0) return;
              onSizeChange?.(nextPageSize);
            }}
          >
            {pageSizes.map((size) => (
              <option
                key={size}
                value={size}
              >
                {size}
              </option>
            ))}
          </select>
          <span>项</span>
        </div>
      ) : null}
      <div className={styles.pages}>
        {showFirstLast ? (
          <button
            className={styles.pageButton}
            type="button"
            disabled={isFirstPage}
            onClick={() => goToPage(1)}
          >
            首页
          </button>
        ) : null}
        <button
          className={styles.pageButton}
          type="button"
          disabled={isFirstPage}
          onClick={() => goToPage(currentPage - 1)}
        >
          上一页
        </button>
        {pageTokens.map((token, index) =>
          typeof token === "number" ? (
            <button
              key={`${token}-${index}`}
              className={[styles.pageButton, token === currentPage ? styles.active : ""]
                .filter(Boolean)
                .join(" ")}
              type="button"
              aria-current={token === currentPage ? "page" : undefined}
              onClick={() => goToPage(token)}
            >
              {token}
            </button>
          ) : (
            <span
              key={`${token}-${index}`}
              className={styles.ellipsis}
            >
              ...
            </span>
          ),
        )}
        <button
          className={styles.pageButton}
          type="button"
          disabled={isLastPage}
          onClick={() => goToPage(currentPage + 1)}
        >
          下一页
        </button>
        {showFirstLast ? (
          <button
            className={styles.pageButton}
            type="button"
            disabled={isLastPage}
            onClick={() => goToPage(totalPages)}
          >
            末页
          </button>
        ) : null}
      </div>
      {showJumper ? (
        <form
          className={styles.jump}
          onSubmit={(event) => {
            event.preventDefault();
            const raw = Number(jumpValue);
            if (!Number.isFinite(raw)) return;
            goToPage(Math.trunc(raw));
          }}
        >
          <span>跳转</span>
          <input
            className={styles.jumpInput}
            type="number"
            min={1}
            max={totalPages}
            value={jumpValue}
            onChange={(event) => setJumpValue(event.currentTarget.value)}
          />
          <span>页</span>
          <button
            className={styles.pageButton}
            type="submit"
          >
            确定
          </button>
        </form>
      ) : null}
    </div>
  );
}
