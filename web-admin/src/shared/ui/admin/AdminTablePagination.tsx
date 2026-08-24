import type { ReactNode } from "react";

import { BzPagination } from "../bz/BzPagination";
import styles from "./AdminTablePagination.module.css";

interface AdminTablePaginationProps {
  total: number;
  pageNo: number;
  pageSize: number;
  pageSizes?: readonly number[];
  summary?: ReactNode;
  showSizeChanger?: boolean;
  showFirstLast?: boolean;
  showJumper?: boolean;
  hideWhenEmpty?: boolean;
  onPageChange: (pageNo: number) => void;
  onPageSizeChange: (pageSize: number) => void;
}

export function AdminTablePagination({
  total,
  pageNo,
  pageSize,
  pageSizes,
  summary,
  showSizeChanger = true,
  showFirstLast = true,
  showJumper = true,
  hideWhenEmpty = true,
  onPageChange,
  onPageSizeChange,
}: AdminTablePaginationProps) {
  if (hideWhenEmpty && total <= 0) return null;

  return (
    <div className={styles.pagination}>
      <div className={styles.summary}>{summary ?? `共 ${total} 条记录`}</div>
      <div className={styles.controls}>
        <BzPagination
          total={total}
          pageSize={pageSize}
          currentPage={pageNo}
          pageSizes={pageSizes ? [...pageSizes] : undefined}
          showSizeChanger={showSizeChanger}
          showFirstLast={showFirstLast}
          showJumper={showJumper}
          onCurrentChange={onPageChange}
          onSizeChange={onPageSizeChange}
        />
      </div>
    </div>
  );
}
