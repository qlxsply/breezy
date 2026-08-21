import type { ApiTableRow } from "@admin/features/apis/ui/types";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import {
  BzOverflowTooltip,
  BzTable,
  type BzTableColumn,
  BzTag,
  BzTooltip,
} from "@admin/shared/ui/bz";

import styles from "./ApiTable.module.css";

export function ApiTable({
  rows,
  loading,
  canDetail,
  canMaintain,
  canPublish,
  canDisable,
  onDetail,
  onMaintain,
  onPublish,
  onDisable,
}: {
  rows: ApiTableRow[];
  loading: boolean;
  canDetail?: boolean;
  canMaintain?: boolean;
  canPublish?: boolean;
  canDisable?: boolean;
  onDetail: (api: ApiTableRow) => void;
  onMaintain: (api: ApiTableRow) => void;
  onPublish: (api: ApiTableRow) => void;
  onDisable: (api: ApiTableRow) => void;
}) {
  const baseColumns: Array<BzTableColumn<ApiTableRow>> = [
    {
      key: "module",
      title: "模块",
      width: 100,
      render: (row: ApiTableRow) => renderTextCell(row.module || "-", styles.mono),
    },
    {
      key: "protocol",
      title: "协议",
      width: 100,
      render: (row: ApiTableRow) => renderTag(row.protocolLabel || row.protocol || "-"),
    },
    {
      key: "httpMethod",
      title: "方法",
      width: 100,
      render: (row: ApiTableRow) => (
        <BzTag
          className={styles.tag}
          size="small"
          type={methodTagType(row.httpMethod)}
          title={row.httpMethodLabel || row.httpMethod || "-"}
        >
          <span className={styles.tagLabel}>{row.httpMethodLabel || row.httpMethod || "-"}</span>
        </BzTag>
      ),
    },
    {
      key: "pathPattern",
      title: "路径",
      minWidth: 420,
      render: (row: ApiTableRow) => renderTextCell(row.pathPattern, styles.mono, true),
    },
    {
      key: "handlerClass",
      title: "处理类",
      minWidth: 300,
      render: (row: ApiTableRow) =>
        renderTextCell(row.handlerClass || "-", `${styles.mono} ${styles.subdued}`, true),
    },
    {
      key: "handlerMethod",
      title: "处理方法",
      minWidth: 180,
      render: (row: ApiTableRow) =>
        renderTextCell(row.handlerMethod || "-", `${styles.mono} ${styles.subdued}`, true),
    },
    {
      key: "permissionDeclared",
      title: "权限声明",
      width: 100,
      render: (row: ApiTableRow) => (
        <BzTag
          className={styles.tag}
          size="small"
          type={row.permissionDeclared ? "success" : "warning"}
          title={row.permissionDeclared ? "已声明" : "未声明"}
        >
          <span className={styles.tagLabel}>{row.permissionDeclared ? "已声明" : "未声明"}</span>
        </BzTag>
      ),
    },
    {
      key: "accessType",
      title: "访问类型",
      width: 100,
      render: (row: ApiTableRow) => (
        <BzTag
          className={styles.tag}
          size="small"
          type={accessTagType(row.accessType)}
          title={row.accessTypeLabel || row.accessType || "-"}
        >
          <span className={styles.tagLabel}>{row.accessTypeLabel || row.accessType || "-"}</span>
        </BzTag>
      ),
    },
    {
      key: "userType",
      title: "用户类型",
      width: 100,
      render: (row: ApiTableRow) =>
        row.userTypeLabel ? (
          <BzTag
            className={styles.tag}
            size="small"
            type="info"
            title={row.userTypeLabel}
          >
            <span className={styles.tagLabel}>{row.userTypeLabel}</span>
          </BzTag>
        ) : (
          renderTextCell("-")
        ),
    },
    {
      key: "audit",
      title: "审计",
      width: 100,
      render: (row: ApiTableRow) => (
        <BzTooltip content={row.auditTooltip || ""}>
          <span>
            <BzTag
              className={styles.tag}
              size="small"
              type={row.auditDeclared ? "success" : "info"}
              title={row.auditDeclared ? "已开启" : "未开启"}
            >
              <span className={styles.tagLabel}>{row.auditDeclared ? "已开启" : "未开启"}</span>
            </BzTag>
          </span>
        </BzTooltip>
      ),
    },
    {
      key: "enabled",
      title: "状态",
      width: 100,
      render: (row: ApiTableRow) => (
        <BzTag
          className={styles.tag}
          size="small"
          type={row.enabled ? "success" : "danger"}
          title={row.enabled ? "启用" : "停用"}
        >
          <span className={styles.tagLabel}>{row.enabled ? "启用" : "停用"}</span>
        </BzTag>
      ),
    },
  ];

  const actionsColumn = createAdminActionsColumn({
    rows,
    getActions: (row) =>
      getRowActions(
        row,
        canDetail,
        canMaintain,
        canPublish,
        canDisable,
        onDetail,
        onMaintain,
        onPublish,
        onDisable,
      ),
    stickyClassName: `${styles.actionsCell} is-fixed-right`,
    stickyHeaderClassName: `${styles.actionsCell} is-fixed-right`,
  });
  if (actionsColumn) baseColumns.push(actionsColumn);

  const columns: Array<BzTableColumn<ApiTableRow>> = baseColumns.map((column) => ({
    ...column,
    className: [styles.cell, column.className].filter(Boolean).join(" "),
    headerClassName: [styles.headerCell, column.headerClassName].filter(Boolean).join(" "),
  }));

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

function renderTextCell(value: string, className?: string, withOverflowTooltip = false) {
  const content = (
    <span className={[styles.text, className].filter(Boolean).join(" ")}>{value}</span>
  );
  if (!withOverflowTooltip) {
    return content;
  }
  return <BzOverflowTooltip text={value}>{content}</BzOverflowTooltip>;
}

function renderTag(label: string, type: "info" | "warning" | "danger" | "success" = "info") {
  return (
    <BzTag
      className={styles.tag}
      size="small"
      type={type}
      title={label}
    >
      <span className={styles.tagLabel}>{label}</span>
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
  row: ApiTableRow,
  canDetail: boolean | undefined,
  canMaintain: boolean | undefined,
  canPublish: boolean | undefined,
  canDisable: boolean | undefined,
  onDetail: (api: ApiTableRow) => void,
  onMaintain: (api: ApiTableRow) => void,
  onPublish: (api: ApiTableRow) => void,
  onDisable: (api: ApiTableRow) => void,
): AdminActionItem[] {
  const actions: AdminActionItem[] = [];
  if (canDetail)
    actions.push({ key: "detail", label: "详情", level: "default", onClick: () => onDetail(row) });
  if (canMaintain)
    actions.push({
      key: "maintain",
      label: "维护",
      level: "primary",
      onClick: () => onMaintain(row),
    });
  if (canPublish)
    actions.push({
      key: "enable",
      label: "启用",
      level: "success",
      disabled: row.enabled,
      onClick: () => onPublish(row),
    });
  if (canDisable)
    actions.push({
      key: "disable",
      label: "停用",
      level: "warning",
      disabled: !row.enabled,
      onClick: () => onDisable(row),
    });
  return actions;
}
