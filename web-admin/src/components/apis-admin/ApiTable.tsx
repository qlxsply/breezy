import { AdminActionBar } from "@admin/components/admin/AdminActionBar";
import { BzTable, type BzTableColumn, BzTag, BzTooltip } from "@admin/components/bz";
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
  const columns: Array<BzTableColumn<ApiEntry>> = [
    {
      key: "module",
      title: "模块",
      width: 120,
      render: (row) => <span className="mono">{row.module || "-"}</span>,
    },
    {
      key: "protocol",
      title: "协议",
      width: 110,
      render: (row) => <BzTag size="small">{row.protocolLabel || row.protocol}</BzTag>,
    },
    {
      key: "httpMethod",
      title: "方法",
      width: 110,
      render: (row) => (
        <BzTag
          size="small"
          type={methodTagType(row.httpMethod)}
        >
          {row.httpMethodLabel || row.httpMethod}
        </BzTag>
      ),
    },
    {
      key: "pathPattern",
      title: "路径",
      minWidth: 420,
      render: (row) => <span className="mono">{row.pathPattern}</span>,
    },
    {
      key: "handler",
      title: "处理器",
      minWidth: 380,
      render: (row) => (
        <span className="mono subdued">{formatHandler(row.handlerClass, row.handlerMethod)}</span>
      ),
    },
    {
      key: "permissionDeclared",
      title: "权限声明",
      width: 110,
      render: (row) => (
        <BzTag
          size="small"
          type={row.permissionDeclared ? "success" : "warning"}
        >
          {row.permissionDeclared ? "已声明" : "未声明"}
        </BzTag>
      ),
    },
    {
      key: "accessType",
      title: "访问类型",
      width: 130,
      render: (row) => (
        <BzTag
          size="small"
          type={accessTagType(row.accessType)}
        >
          {row.accessTypeLabel || row.accessType}
        </BzTag>
      ),
    },
    {
      key: "userTypes",
      title: "用户类型",
      minWidth: 140,
      render: (row) =>
        row.userTypeLabels?.length ? (
          <div className="tag-stack">
            {row.userTypeLabels.map((label) => (
              <BzTag
                key={label}
                size="small"
                type="info"
              >
                {label}
              </BzTag>
            ))}
          </div>
        ) : (
          <span>-</span>
        ),
    },
    {
      key: "audit",
      title: "审计",
      width: 110,
      render: (row) => (
        <BzTooltip content={row.auditTooltip || ""}>
          <span>
            <BzTag
              size="small"
              type={row.auditDeclared ? "success" : "info"}
            >
              {row.auditDeclared ? "已开启" : "未开启"}
            </BzTag>
          </span>
        </BzTooltip>
      ),
    },
    {
      key: "enabled",
      title: "状态",
      width: 100,
      render: (row) => (
        <BzTag
          size="small"
          type={row.enabled ? "success" : "danger"}
        >
          {row.enabled ? "启用" : "停用"}
        </BzTag>
      ),
    },
    {
      key: "actions",
      title: "操作",
      width: 88,
      render: (row) => (
        <AdminActionBar
          actions={getRowActions(row, canPublish, canDisable, onPublish, onDisable)}
        />
      ),
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

function formatHandler(handlerClass?: string, handlerMethod?: string) {
  if (handlerClass && handlerMethod) return `${handlerClass}#${handlerMethod}`;
  return handlerClass || handlerMethod || "-";
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
