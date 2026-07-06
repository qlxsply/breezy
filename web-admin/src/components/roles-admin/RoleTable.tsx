import { formatDateTime } from "../../core/formatter";
import type { AdminActionItem } from "../../types/admin-action";
import type { RoleEntry } from "../../types/role-admin";
import { AdminActionBar } from "../admin/AdminActionBar";
import type { BzTableColumn } from "../bz/BzTable";
import { BzOverflowTooltip } from "../bz/BzOverflowTooltip";
import { BzTable } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

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
      { key: "detail", label: "详情", tone: "detail", handler: () => onDetail(role) },
    ];
    if (canEdit) {
      actions.push({ key: "edit", label: "编辑", tone: "edit", handler: () => onEdit(role) });
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
      render: (row) => <span className="role-table-mono role-table-text">{row.code}</span>,
    },
    {
      key: "name",
      title: "名称",
      minWidth: 180,
      render: (row) => (
        <BzOverflowTooltip text={row.name}>
          <span className="role-table-text role-table-name">{row.name}</span>
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
      render: (row) => <span className="role-table-text">{row.createdBy || "-"}</span>,
    },
    {
      key: "createdAt",
      title: "创建时间",
      width: 170,
      render: (row) => <span className="role-table-text">{formatDateTime(row.createdAt)}</span>,
    },
    {
      key: "updatedBy",
      title: "更新人",
      width: 140,
      render: (row) => <span className="role-table-text">{row.updatedBy || "-"}</span>,
    },
    {
      key: "updatedAt",
      title: "更新时间",
      width: 170,
      render: (row) => <span className="role-table-text">{formatDateTime(row.updatedAt)}</span>,
    },
    {
      key: "actions",
      title: "操作",
      width: 220,
      render: (row) => {
        const actions = getActions(row);
        return <AdminActionBar actions={actions} />;
      },
    },
  ];

  return (
    <div className="role-manage-table-scope">
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
