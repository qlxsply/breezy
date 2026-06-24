import { formatDateTime } from "../../core/formatter";
import type { AdminActionItem } from "../../types/admin-action";
import type { RoleEntry } from "../../types/role-admin";
import { AdminActionBar } from "../admin/AdminActionBar";
import type { BzTableColumn } from "../bz/BzTable";
import { BzTable } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

interface RoleTableActions {
  canEdit: boolean;
  canDelete: boolean;
  canPermissions: boolean;
  onEdit: (role: RoleEntry) => void;
  onPermissions: (role: RoleEntry) => void;
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
  canPermissions,
  onEdit,
  onPermissions,
  onRemove,
}: RoleTableProps) {
  function getActions(role: RoleEntry) {
    const actions: AdminActionItem[] = [];
    if (canEdit) {
      actions.push({ key: "edit", label: "编辑", tone: "edit", handler: () => onEdit(role) });
    }
    if (canPermissions) {
      actions.push({
        key: "permissions",
        label: "授权",
        tone: "detail",
        handler: () => onPermissions(role),
      });
    }
    if (canDelete) {
      actions.push({ key: "delete", label: "删除", tone: "delete", handler: () => onRemove(role) });
    }
    return actions;
  }

  const columns: Array<BzTableColumn<RoleEntry>> = [
    {
      key: "code",
      title: "编码",
      width: 160,
      render: (row) => <span className="mono">{row.code}</span>,
    },
    {
      key: "name",
      title: "名称",
      minWidth: 180,
      render: (row) => <div className="name">{row.name}</div>,
    },
    {
      key: "enabled",
      title: "状态",
      width: 100,
      render: (row) => (
        <BzTag type={row.enabled ? "success" : "danger"}>{row.enabled ? "启用" : "停用"}</BzTag>
      ),
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
        const actions = getActions(row);
        return (
          <AdminActionBar
            actions={actions.filter((a) => a.key === "edit" || a.key === "permissions")}
          />
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
