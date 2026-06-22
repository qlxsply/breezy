import { AdminActionBar } from "@admin/components/admin/AdminActionBar";
import { BzOverflowTooltip, BzTable, type BzTableColumn, BzTag, BzTooltip } from "@admin/components/bz";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { ApiEntry } from "@admin/types/api-admin";

export function ApiTable({
  rows,
  loading,
  canPublish,
  canDisable,
  onPublish,
  onDisable,
}: {
  rows: ApiEntry[];
  loading: boolean;
  canPublish?: boolean;
  canDisable?: boolean;
  onPublish: (api: ApiEntry) => void;
  onDisable: (api: ApiEntry) => void;
}) {
  const baseColumns: Array<BzTableColumn<ApiEntry>> = [
    {
      key: "module",
      title: "模块",
      width: 100,
      render: (row: ApiEntry) => renderTextCell(row.module || "-", "mono"),
    },
    {
      key: "protocol",
      title: "协议",
      width: 100,
      render: (row: ApiEntry) => renderTag(row.protocolLabel || row.protocol || "-"),
    },
    {
      key: "httpMethod",
      title: "方法",
      width: 100,
      render: (row: ApiEntry) => (
        <BzTag
          className="api-table__tag"
          size="small"
          type={methodTagType(row.httpMethod)}
          title={row.httpMethodLabel || row.httpMethod || "-"}
        >
          <span className="api-table__tag-label">{row.httpMethodLabel || row.httpMethod || "-"}</span>
        </BzTag>
      ),
    },
    {
      key: "pathPattern",
      title: "路径",
      minWidth: 420,
      render: (row: ApiEntry) => renderTextCell(row.pathPattern, "mono", true),
    },
    {
      key: "handlerClass",
      title: "处理类",
      minWidth: 300,
      render: (row: ApiEntry) => renderTextCell(row.handlerClass || "-", "mono subdued", true),
    },
    {
      key: "handlerMethod",
      title: "处理方法",
      minWidth: 180,
      render: (row: ApiEntry) => renderTextCell(row.handlerMethod || "-", "mono subdued", true),
    },
    {
      key: "permissionDeclared",
      title: "权限声明",
      width: 100,
      render: (row: ApiEntry) => (
        <BzTag
          className="api-table__tag"
          size="small"
          type={row.permissionDeclared ? "success" : "warning"}
          title={row.permissionDeclared ? "已声明" : "未声明"}
        >
          <span className="api-table__tag-label">{row.permissionDeclared ? "已声明" : "未声明"}</span>
        </BzTag>
      ),
    },
    {
      key: "accessType",
      title: "访问类型",
      width: 100,
      render: (row: ApiEntry) => (
        <BzTag
          className="api-table__tag"
          size="small"
          type={accessTagType(row.accessType)}
          title={row.accessTypeLabel || row.accessType || "-"}
        >
          <span className="api-table__tag-label">{row.accessTypeLabel || row.accessType || "-"}</span>
        </BzTag>
      ),
    },
    {
      key: "userTypes",
      title: "用户类型",
      width: 100,
      render: (row: ApiEntry) =>
        row.userTypeLabels?.length ? (
          <div className="api-table__tag-list">
            {row.userTypeLabels.map((label) => (
              <BzTag
                key={label}
                className="api-table__tag"
                size="small"
                type="info"
                title={label}
              >
                <span className="api-table__tag-label">{label}</span>
              </BzTag>
            ))}
          </div>
        ) : (
          renderTextCell("-")
        ),
    },
    {
      key: "audit",
      title: "审计",
      width: 100,
      render: (row: ApiEntry) => (
        <BzTooltip content={row.auditTooltip || ""}>
          <span>
            <BzTag
              className="api-table__tag"
              size="small"
              type={row.auditDeclared ? "success" : "info"}
              title={row.auditDeclared ? "已开启" : "未开启"}
            >
              <span className="api-table__tag-label">{row.auditDeclared ? "已开启" : "未开启"}</span>
            </BzTag>
          </span>
        </BzTooltip>
      ),
    },
    {
      key: "enabled",
      title: "状态",
      width: 100,
      render: (row: ApiEntry) => (
        <BzTag
          className="api-table__tag"
          size="small"
          type={row.enabled ? "success" : "danger"}
          title={row.enabled ? "启用" : "停用"}
        >
          <span className="api-table__tag-label">{row.enabled ? "启用" : "停用"}</span>
        </BzTag>
      ),
    },
    {
      key: "actions",
      title: "操作",
      width: 90,
      className: "api-table__actions-cell",
      render: (row: ApiEntry) => (
        <AdminActionBar
          actions={getRowActions(row, canPublish, canDisable, onPublish, onDisable)}
        />
      ),
    },
  ];

  const columns: Array<BzTableColumn<ApiEntry>> = baseColumns.map((column) => ({
    ...column,
    className: ["api-table__cell", column.className].filter(Boolean).join(" "),
    headerClassName: ["api-table__header-cell", column.headerClassName].filter(Boolean).join(" "),
  }));

  return (
    <div className="api-table-scope">
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

function renderTextCell(value: string, className?: string, withOverflowTooltip = false) {
  const content = <span className={["api-table__text", className].filter(Boolean).join(" ")}>{value}</span>;
  if (!withOverflowTooltip) {
    return content;
  }
  return <BzOverflowTooltip text={value}>{content}</BzOverflowTooltip>;
}

function renderTag(label: string, type: "info" | "warning" | "danger" | "success" = "info") {
  return (
    <BzTag
      className="api-table__tag"
      size="small"
      type={type}
      title={label}
    >
      <span className="api-table__tag-label">{label}</span>
    </BzTag>
  );
}

function methodTagType(method?: string): "info" | "success" | "warning" | "danger" {
  switch ((method || "").toUpperCase()) {
    case "POST":
      return "success";
    case "PUT":
    case "PATCH":
      return "warning";
    case "DELETE":
      return "danger";
    default:
      return "info";
  }
}

function accessTagType(accessType?: string): "info" | "success" | "warning" | "danger" {
  switch ((accessType || "").toUpperCase()) {
    case "AUTHORIZED":
      return "warning";
    case "AUTHENTICATED":
      return "success";
    default:
      return "info";
  }
}

function getRowActions(
  row: ApiEntry,
  canPublish: boolean | undefined,
  canDisable: boolean | undefined,
  onPublish: (api: ApiEntry) => void,
  onDisable: (api: ApiEntry) => void,
): AdminActionItem[] {
  const actions: AdminActionItem[] = [];
  if (canPublish && !row.enabled)
    actions.push({ key: "enable", label: "启用", tone: "enable", handler: () => onPublish(row) });
  if (canDisable && row.enabled)
    actions.push({ key: "disable", label: "停用", tone: "disable", handler: () => onDisable(row) });
  return actions;
}
