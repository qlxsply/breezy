import type { RoleEntry } from "@admin/features/roles/model/types";
import { formatDateTime } from "@admin/shared/lib/formatter";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import { BzOverflowTooltip } from "@admin/shared/ui/bz/BzOverflowTooltip";
import type { BzTableColumn } from "@admin/shared/ui/bz/BzTable";
import { BzTable } from "@admin/shared/ui/bz/BzTable";
import { BzTag } from "@admin/shared/ui/bz/BzTag";

import styles from "./RoleTable.module.css";

interface RoleTableActions {
  canEdit: boolean;
  canDelete: boolean;
  onDetail: (role: RoleEntry) => void;
  onEdit: (role: RoleEntry) => void;
  onRemove: (role: RoleEntry) => void;
}

interface RoleTableProps extends RoleTableActions {
  rows: RoleEntry[];
  loading: boolean;
}

export function RoleTable({
  rows,
  loading,
  canEdit,
  canDelete,
  onDetail,
  onEdit,
  onRemove,
}: RoleTableProps) {
  function getActions(role: RoleEntry) {
    const actions: AdminActionItem[] = [
      { key: "detail", label: "详情", level: "default", onClick: () => onDetail(role) },
    ];
    if (canEdit) {
      actions.push({ key: "edit", label: "编辑", level: "primary", onClick: () => onEdit(role) });
    }
    if (canDelete) {
      actions.push({
        key: "delete",
        label: "删除",
        level: "danger",
        onClick: () => onRemove(role),
      });
    }
    return actions;
  }

  const columns: Array<BzTableColumn<RoleEntry>> = [
    {
      key: "code",
      title: "编码",
      width: 200,
      className: `${styles.codeColumn} is-sticky-left`,
      headerClassName: `${styles.codeColumn} is-sticky-left`,
      render: (row) => <span className={`${styles.mono} ${styles.text}`}>{row.code}</span>,
    },
    {
      key: "name",
      title: "名称",
      minWidth: 180,
      render: (row) => (
        <BzOverflowTooltip text={row.name}>
          <span className={`${styles.text} ${styles.name}`}>{row.name}</span>
        </BzOverflowTooltip>
      ),
    },
    {
      key: "enabled",
      title: "状态",
      width: 100,
      render: (row) => (
        <BzTag type={row.enabled ? "success" : "danger"}>{row.enabled ? "启用" : "停用"}</BzTag>
      ),
    },
    {
      key: "createdBy",
      title: "创建人",
      width: 140,
      render: (row) => <span className={styles.text}>{row.createdBy || "-"}</span>,
    },
    {
      key: "createdAt",
      title: "创建时间",
      width: 170,
      render: (row) => <span className={styles.text}>{formatDateTime(row.createdAt)}</span>,
    },
    {
      key: "updatedBy",
      title: "更新人",
      width: 140,
      render: (row) => <span className={styles.text}>{row.updatedBy || "-"}</span>,
    },
    {
      key: "updatedAt",
      title: "更新时间",
      width: 170,
      render: (row) => <span className={styles.text}>{formatDateTime(row.updatedAt)}</span>,
    },
  ];

  const actionsColumn = createAdminActionsColumn({
    rows,
    getActions,
    stickyClassName: `${styles.actionsColumn} is-sticky-right`,
    stickyHeaderClassName: `${styles.actionsColumn} is-sticky-right`,
  });
  if (actionsColumn) columns.push(actionsColumn);

  return (
    <div className={styles.tableScope}>
      <BzTable
        data={rows}
        columns={columns}
        rowKey="id"
        loading={loading}
        emptyText="暂无数据"
        size="small"
      />
    </div>
  );
}
