import { formatDateTime } from "../../core/formatter";
import type { AdminActionItem } from "../../types/admin-action";
import type { UserEntry } from "../../types/user-admin";
import { createAdminActionsColumn } from "../admin/admin-actions-column";
import type { BzTableColumn } from "../bz/BzTable";
import { BzOverflowTooltip } from "../bz/BzOverflowTooltip";
import { BzTable } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

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
    return user.userType === "SYSTEM" || (user.userType === "INTERNAL" && user.username === "admin");
  }

  function resolveUserTypeLabel(userType: string): string {
    return userTypeMetaMap[userType]?.label || userType;
  }
  function resolveUserTypeTagType(userType: string): "info" | "success" | "warning" | "danger" {
    const t = userTypeMetaMap[userType]?.tagType;
    if (t === "success" || t === "warning" || t === "danger" || t === "info") return t;
    return "info";
  }

  const currentPageSelectableRows = rows.filter((row) => !isProtectedUser(row));
  const currentPageSelectableIds = currentPageSelectableRows.map((row) => row.id);
  const allCurrentPageSelected =
    currentPageSelectableIds.length > 0 &&
    currentPageSelectableIds.every((id) => selectedIds.includes(id));
  const someCurrentPageSelected =
    !allCurrentPageSelected && currentPageSelectableIds.some((id) => selectedIds.includes(id));

  function getActions(user: UserEntry): AdminActionItem[] {
    const maintainDisabled = isProtectedUser(user);
    const toggleDisabled = isProtectedUser(user);
    const resetDisabled = user.userType === "SYSTEM" || user.username === "admin";
    const actions: AdminActionItem[] = [
      { key: "detail", label: "详情", tone: "detail", handler: () => onDetail(user) },
    ];
    if (canEdit || canRoles)
      actions.push({ key: "edit", label: "维护", tone: "edit", disabled: maintainDisabled, handler: () => onEdit(user) });
    if (canToggle)
      actions.push({
        key: "toggle",
        label: user.status === "ENABLED" ? "停用" : "启用",
        tone: user.status === "ENABLED" ? "disable" : "enable",
        disabled: toggleDisabled,
        handler: () => onToggle(user),
      });
    if (canReset) actions.push({ key: "reset", label: "重置密码", disabled: resetDisabled, handler: () => onReset(user) });
    if (canDelete)
      actions.push({ key: "delete", label: "删除", tone: "delete", handler: () => onRemove(user) });
    return actions;
  }

  const columns: Array<BzTableColumn<UserEntry>> = [
    ...(batchMode
      ? [
          {
            key: "select",
            title: "",
            width: 48,
            className: "user-manage-col-select is-sticky-left",
            headerClassName: "user-manage-col-select is-sticky-left",
            headerRender: () => (
              <input
                className="user-manage-checkbox"
                type="checkbox"
                checked={allCurrentPageSelected}
                disabled={currentPageSelectableIds.length === 0}
                ref={(el) => {
                  if (el) el.indeterminate = someCurrentPageSelected;
                }}
                onChange={(event) => onToggleSelectAll?.(event.target.checked)}
              />
            ),
            render: (row: UserEntry) => (
              <input
                className="user-manage-checkbox"
                type="checkbox"
                checked={selectedIds.includes(row.id)}
                disabled={isProtectedUser(row)}
                onChange={(event) => onToggleSelect?.(row, event.target.checked)}
              />
            ),
          } as BzTableColumn<UserEntry>,
        ]
      : []),
    {
      key: "username",
      title: "账号",
      width: 200,
      className: `user-manage-col-username is-sticky-left${batchMode ? " has-select-offset" : ""}`,
      headerClassName: `user-manage-col-username is-sticky-left${batchMode ? " has-select-offset" : ""}`,
      render: (row) => (
        <BzOverflowTooltip text={row.username}>
          <span className="user-manage-text user-manage-text--primary">{row.username}</span>
        </BzOverflowTooltip>
      ),
    },
    {
      key: "nickname",
      title: "昵称",
      minWidth: 160,
      render: (row) => <span className="user-manage-text">{row.nickname || "-"}</span>,
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
      render: (row) => <span className="user-manage-text">{formatDateTime(row.createdAt)}</span>,
    },
    {
      key: "updatedBy",
      title: "更新人",
      width: 140,
      render: (row) => <span className="user-manage-text">{row.updatedBy || "-"}</span>,
    },
    {
      key: "updatedAt",
      title: "更新时间",
      width: 170,
      render: (row) => <span className="user-manage-text">{formatDateTime(row.updatedAt)}</span>,
    },
  ];

  const actionsColumn = createAdminActionsColumn({
    rows,
    getActions,
    stickyClassName: "user-manage-col-actions is-fixed-right",
    stickyHeaderClassName: "user-manage-col-actions is-fixed-right",
  });
  if (actionsColumn) columns.push(actionsColumn);

  return (
    <div className="user-manage-table-scope">
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
