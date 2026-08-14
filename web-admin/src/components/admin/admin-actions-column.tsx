import {
  AdminActionBar,
  estimateAdminActionBarWidth,
} from "@admin/components/admin/AdminActionBar";
import type { BzTableColumn } from "@admin/components/bz/BzTable";
import type { AdminActionItem } from "@admin/types/admin-action";

interface CreateAdminActionsColumnOptions<Row> {
  rows?: Row[];
  getActions: (row: Row) => AdminActionItem[];
  key?: string;
  title?: string;
  width?: number;
  minWidth?: number;
  stickyClassName?: string;
  stickyHeaderClassName?: string;
}

export function estimateAdminActionsColumnWidth<Row>(
  rows: Row[],
  getActions: (row: Row) => AdminActionItem[],
): number {
  const widths = rows.map((row) => estimateAdminActionBarWidth(getActions(row)));
  const maxWidth = widths.length > 0 ? Math.max(...widths) : 0;
  return maxWidth;
}

export function createAdminActionsColumn<Row>({
  rows = [],
  getActions,
  key = "actions",
  title = "操作",
  width,
  minWidth,
  stickyClassName = "is-fixed-right",
  stickyHeaderClassName = "is-fixed-right",
}: CreateAdminActionsColumnOptions<Row>): BzTableColumn<Row> | null {
  const hasVisibleActions = rows.some((row) => getActions(row).length > 0);
  if (!hasVisibleActions && rows.length > 0) return null;

  const resolvedWidth = width ?? estimateAdminActionsColumnWidth(rows, getActions);
  const useIntrinsicWidth =
    width === undefined &&
    stickyClassName === "is-fixed-right" &&
    stickyHeaderClassName === "is-fixed-right";

  return {
    key,
    title,
    width: useIntrinsicWidth ? "1%" : resolvedWidth,
    minWidth: minWidth ?? resolvedWidth,
    disableRowSelection: true,
    className: ["admin-actions-column", stickyClassName].filter(Boolean).join(" "),
    headerClassName: ["admin-actions-column", stickyHeaderClassName].filter(Boolean).join(" "),
    render: (row) => <AdminActionBar actions={getActions(row)} />,
  };
}
