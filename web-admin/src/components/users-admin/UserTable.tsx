import { formatDateTime } from "../../core/formatter";
import type { AdminActionItem } from "../../types/admin-action";
import type { UserEntry } from "../../types/user-admin";
import { AdminActionBar } from "../admin/AdminActionBar";
import type { BzTableColumn } from "../bz/BzTable";
import { BzTable } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

interface UserTypeMeta {
  label: string;
  tagType?: string | null;
}

interface UserTableProps {
  rows: UserEntry[];
  loading: boolean;
  canEdit: boolean;
  canToggle: boolean;
  canReset: boolean;
  canRoles: boolean;
  canDelete: boolean;
  userTypeMetaMap?: Record<string, UserTypeMeta>;
  onEdit: (user: UserEntry) => void;
  onToggle: (user: UserEntry) => void;
  onReset: (user: UserEntry) => void;
  onRemove: (user: UserEntry) => void;
}

export function UserTable({
  rows,
  loading,
  canEdit,
  canToggle,
  canReset,
  canRoles,
  canDelete,
  userTypeMetaMap = {},
  onEdit,
  onToggle,
  onReset,
  onRemove,
}: UserTableProps) {
  function resolveUserTypeLabel(userType: string): string {
    return userTypeMetaMap[userType]?.label || userType;
  }
  function resolveUserTypeTagType(userType: string): "info" | "success" | "warning" | "danger" {
    const t = userTypeMetaMap[userType]?.tagType;
    if (t === "success" || t === "warning" || t === "danger" || t === "info") return t;
    return "info";
  }
  function getActions(user: UserEntry): AdminActionItem[] {
    const actions: AdminActionItem[] = [];
    if (canEdit || canRoles)
      actions.push({ key: "edit", label: "维护", tone: "edit", handler: () => onEdit(user) });
    if (canToggle)
      actions.push({
        key: "toggle",
        label: user.status === "ENABLED" ? "停用" : "启用",
        tone: user.status === "ENABLED" ? "disable" : "enable",
        handler: () => onToggle(user),
      });
    if (canReset) actions.push({ key: "reset", label: "重置密码", handler: () => onReset(user) });
    if (canDelete)
      actions.push({ key: "delete", label: "删除", tone: "delete", handler: () => onRemove(user) });
    return actions;
  }

  const columns: Array<BzTableColumn<UserEntry>> = [
    {
      key: "username",
      title: "账号",
      minWidth: 180,
      render: (row) => <div className="name">{row.username}</div>,
    },
    { key: "nickname", title: "昵称", minWidth: 160, render: (row) => row.nickname || "-" },
    {
      key: "userType",
      title: "类型",
      width: 120,
      render: (row) => (
        <BzTag type={resolveUserTypeTagType(row.userType)}>
          {resolveUserTypeLabel(row.userType)}
        </BzTag>
      ),
    },
    {
      key: "status",
      title: "状态",
      width: 110,
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
      render: (row) => formatDateTime(row.createdAt),
    },
    { key: "updatedBy", title: "更新人", width: 140, render: (row) => row.updatedBy || "-" },
    {
      key: "updatedAt",
      title: "更新时间",
      width: 170,
      render: (row) => formatDateTime(row.updatedAt),
    },
    {
      key: "actions",
      title: "操作",
      width: 180,
      render: (row) => {
        const all = getActions(row);
        return (
          <AdminActionBar actions={all.filter((a) => a.key === "edit")} />
        );
      },
    },
  ];

  return (
    <BzTable
      data={rows}
      columns={columns}
      rowKey="id"
      loading={loading}
      emptyText="暂无数据"
      size="small"
    />
  );
}
