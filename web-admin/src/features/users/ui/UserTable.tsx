import type { UserEntry } from "@admin/features/users/model/types";
import { formatDateTime } from "@admin/shared/lib/formatter";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import { BzOverflowTooltip } from "@admin/shared/ui/bz/BzOverflowTooltip";
import type { BzTableColumn } from "@admin/shared/ui/bz/BzTable";
import { BzTable } from "@admin/shared/ui/bz/BzTable";
import { BzTag } from "@admin/shared/ui/bz/BzTag";

import styles from "./UserTable.module.css";

interface UserTypeMeta {
  label: string;
  tagType?: string | null;
}

interface UserTableProps {
  rows: UserEntry[];
  loading: boolean;
  batchMode?: boolean;
  selectedIds?: string[];
  canEdit: boolean;
  canToggle: boolean;
  canReset: boolean;
  canRoles: boolean;
  canDelete: boolean;
  userTypeMetaMap?: Record<string, UserTypeMeta>;
  onDetail: (user: UserEntry) => void;
  onEdit: (user: UserEntry) => void;
  onToggle: (user: UserEntry) => void;
  onReset: (user: UserEntry) => void;
  onRemove: (user: UserEntry) => void;
  onToggleSelect?: (user: UserEntry, checked: boolean) => void;
  onToggleSelectAll?: (checked: boolean) => void;
}

export function UserTable({
  rows,
  loading,
  batchMode = false,
  selectedIds = [],
  canEdit,
  canToggle,
  canReset,
  canRoles,
  canDelete,
  userTypeMetaMap = {},
  onDetail,
  onEdit,
  onToggle,
  onReset,
  onRemove,
  onToggleSelect,
  onToggleSelectAll,
}: UserTableProps) {
  function isProtectedUser(user: UserEntry): boolean {
    return user.userType === "SYSTEM" || (user.userType === "ADMIN" && user.username === "admin");
  }

  function resolveUserTypeLabel(userType: string): string {
    return userTypeMetaMap[userType]?.label || userType;
  }
  function resolveUserTypeTagType(userType: string): "info" | "success" | "warning" | "danger" {
    const t = userTypeMetaMap[userType]?.tagType;
    if (t === "success" || t === "warning" || t === "danger" || t === "info") return t;
    return "info";
  }

  function getActions(user: UserEntry): AdminActionItem[] {
    const maintainDisabled = isProtectedUser(user);
    const toggleDisabled = isProtectedUser(user);
    const resetDisabled = user.userType === "SYSTEM" || user.username === "admin";
    const actions: AdminActionItem[] = [
      { key: "detail", label: "详情", level: "default", onClick: () => onDetail(user) },
    ];
    if (canEdit || canRoles)
      actions.push({
        key: "edit",
        label: "维护",
        level: "primary",
        disabled: maintainDisabled,
        onClick: () => onEdit(user),
      });
    if (canToggle)
      actions.push({
        key: "toggle",
        label: user.status === "ENABLED" ? "停用" : "启用",
        level: user.status === "ENABLED" ? "warning" : "success",
        disabled: toggleDisabled,
        onClick: () => onToggle(user),
      });
    if (canReset)
      actions.push({
        key: "reset",
        label: "重置密码",
        disabled: resetDisabled,
        level: "warning",
        onClick: () => onReset(user),
      });
    if (canDelete)
      actions.push({
        key: "delete",
        label: "删除",
        level: "danger",
        disabled: maintainDisabled,
        onClick: () => onRemove(user),
      });
    return actions;
  }

  const columns: Array<BzTableColumn<UserEntry>> = [
    {
      key: "username",
      title: "账号",
      width: 200,
      className: `${styles.usernameColumn} is-sticky-left${batchMode ? " has-select-offset" : ""}`,
      headerClassName: `${styles.usernameColumn} is-sticky-left${batchMode ? " has-select-offset" : ""}`,
      render: (row) => (
        <BzOverflowTooltip text={row.username}>
          <span className={`${styles.text} ${styles.primaryText}`}>{row.username}</span>
        </BzOverflowTooltip>
      ),
    },
    {
      key: "nickname",
      title: "昵称",
      minWidth: 160,
      render: (row) => <span className={styles.text}>{row.nickname || "-"}</span>,
    },
    {
      key: "userType",
      title: "类型",
      width: 100,
      render: (row) => (
        <BzTag type={resolveUserTypeTagType(row.userType)}>
          {resolveUserTypeLabel(row.userType)}
        </BzTag>
      ),
    },
    {
      key: "status",
      title: "状态",
      width: 100,
      render: (row) => {
        const active = row.status === "ENABLED";
        return <BzTag type={active ? "success" : "danger"}>{active ? "启用" : "停用"}</BzTag>;
      },
    },
    { key: "createdBy", title: "创建人", width: 140, render: (row) => row.createdBy || "-" },
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
    stickyClassName: `${styles.actionsColumn} is-fixed-right`,
    stickyHeaderClassName: `${styles.actionsColumn} is-fixed-right`,
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
        rowSelection={
          batchMode
            ? {
                selectedRowKeys: selectedIds,
                isRowSelectable: (row) => !isProtectedUser(row),
                onToggle: (row, selected) => onToggleSelect?.(row, selected),
                onToggleCurrentPage: (_rows, selected) => onToggleSelectAll?.(selected),
                columnClassName: `${styles.selectColumn} is-sticky-left`,
              }
            : undefined
        }
      />
    </div>
  );
}
