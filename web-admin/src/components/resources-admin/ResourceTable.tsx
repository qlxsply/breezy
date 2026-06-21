import { useMemo, useState } from "react";

import type { ResourceEntry } from "../../types/resource-admin";
import { BzButton } from "../bz/BzButton";
import { BzIcon } from "../bz/BzIcon";
import { BzTag } from "../bz/BzTag";
import type { BzTableColumn } from "../bz/BzTable";
import { BzTable } from "../bz/BzTable";
import { BzTooltip } from "../bz/BzTooltip";

interface ResourceTableProps {
  rows: ResourceEntry[];
  loading: boolean;
  expandAll?: boolean;
  expandSignal?: number;
  canEdit?: boolean;
  canDelete?: boolean;
  canApi?: boolean;
  onEdit: (row: ResourceEntry) => void;
  onRemove: (row: ResourceEntry) => void;
  onApi: (row: ResourceEntry) => void;
}

function indentStyle(depth: number) {
  return { paddingLeft: `${depth * 24}px` };
}

function isAncestorDisabled(row: ResourceEntry, map: Map<string, ResourceEntry>): boolean {
  let parentId = row.parentId ?? null;
  while (parentId) {
    const parent = map.get(parentId);
    if (!parent) return false;
    if (!parent.enabled) return true;
    parentId = parent.parentId ?? null;
  }
  return false;
}

function effectiveEnabled(row: ResourceEntry, map: Map<string, ResourceEntry>): boolean {
  if (!row.enabled) return false;
  return !isAncestorDisabled(row, map);
}

export function ResourceTable({ rows, loading, canEdit = true, canDelete = true, canApi = true, onEdit, onRemove, onApi }: ResourceTableProps) {
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  const resourceMap = useMemo(() => {
    const map = new Map<string, ResourceEntry>();
    rows.forEach((r) => map.set(r.id, r));
    return map;
  }, [rows]);

  const childrenMap = useMemo(() => {
    const idSet = new Set(rows.map((r) => r.id));
    const cm = new Map<string | null, ResourceEntry[]>();

    const addChild = (parentId: string | null, row: ResourceEntry) => {
      const list = cm.get(parentId) ?? [];
      list.push(row);
      list.sort((a, b) => {
        if (a.orderNo !== b.orderNo) return a.orderNo - b.orderNo;
        return a.name.localeCompare(b.name);
      });
      cm.set(parentId, list);
    };

    rows.forEach((row) => {
      const rawParent = row.parentId ?? null;
      const parentId = rawParent && idSet.has(rawParent) ? rawParent : null;
      addChild(parentId, row);
    });

    return cm;
  }, [rows]);

  const hasChildren = (id: string) => {
    const list = childrenMap.get(id);
    return Boolean(list && list.length > 0);
  };

  const treeRows = useMemo(() => {
    const flat: Array<{ row: ResourceEntry; depth: number }> = [];
    const walk = (parentId: string | null, depth: number) => {
      const list = childrenMap.get(parentId);
      if (!list) return;
      list.forEach((row) => {
        flat.push({ row, depth });
        if (hasChildren(row.id) && expandedIds.has(row.id)) {
          walk(row.id, depth + 1);
        }
      });
    };
    walk(null, 0);
    return flat;
  }, [childrenMap, expandedIds]);

  function toggleExpand(id: string) {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  const isExpanded = (id: string) => expandedIds.has(id);

  const columns: Array<BzTableColumn<{ row: ResourceEntry; depth: number }>> = [
    {
      key: "name", title: "资源名称", width: 320,
      render: ({ row, depth }) => (
        <div className="name-block" style={indentStyle(depth)}>
          <div className="tree-ops">
            {hasChildren(row.id) ? (
              <BzButton link className="tree-toggle" onClick={() => toggleExpand(row.id)}>
                <BzIcon className={`toggle-icon${isExpanded(row.id) ? " expanded" : ""}`}>
                  <span className="toggle-caret">›</span>
                </BzIcon>
              </BzButton>
            ) : (
              <span className="tree-toggle-placeholder" />
            )}
          </div>
          <div className="row-icon">{row.icon || "📦"}</div>
          <div className="name-info">
            <div className="name">
              <span className="name-text" title={row.name}>{row.name}</span>
            </div>
            {row.description ? <div className="desc" title={row.description}>{row.description}</div> : null}
          </div>
        </div>
      ),
    },
    {
      key: "code", title: "编码", width: 150, className: "mono",
      render: ({ row }) => <span className="mono">{row.code}</span>,
    },
    {
      key: "type", title: "类型", width: 110,
      render: ({ row }) => <BzTag size="small">{row.type}</BzTag>,
    },
    {
      key: "scope", title: "入口", width: 100,
      render: ({ row }) => <BzTag size="small">{row.type === "MENU" ? row.scope : "-"}</BzTag>,
    },
    {
      key: "openMode", title: "打开", width: 100,
      render: ({ row }) => <BzTag size="small">{row.openMode}</BzTag>,
    },
    {
      key: "url", title: "URL", minWidth: 100,
      render: ({ row }) => <span className="mono">{row.url || "-"}</span>,
    },
    {
      key: "loadTarget", title: "加载资源", minWidth: 260,
      render: ({ row }) => <span className="mono">{row.loadTarget || "-"}</span>,
    },
    {
      key: "level", title: "级别", width: 50,
      render: ({ row }) => (
        <BzTag size="small" type={row.level === "SYSTEM" ? "danger" : "info"}>
          {row.level === "SYSTEM" ? "系统" : "自定义"}
        </BzTag>
      ),
    },
    {
      key: "guestAccess", title: "游客", width: 50,
      render: ({ row }) => (
        row.guestAccess
          ? <BzTag size="small" type="success">允许</BzTag>
          : <BzTag size="small" type="danger">禁止</BzTag>
      ),
    },
    {
      key: "status", title: "状态", width: 50,
      render: ({ row }) => (
        <div className="status-cell">
          <BzTag size="small" type={effectiveEnabled(row, resourceMap) ? "success" : "info"}>
            {effectiveEnabled(row, resourceMap) ? "启用" : "停用"}
          </BzTag>
          {row.missingApis ? (
            <BzTooltip content="API缺失">
              <BzTag size="small" type="warning">API缺失</BzTag>
            </BzTooltip>
          ) : null}
          {isAncestorDisabled(row, resourceMap) ? (
            <BzTooltip content="父级已停用">
              <BzTag size="small" type="warning">父级停用</BzTag>
            </BzTooltip>
          ) : null}
        </div>
      ),
    },
    {
      key: "actions", title: "操作", width: 200,
      render: ({ row }) => (
        <div className="action-buttons">
          {canApi ? <BzButton size="small" onClick={() => onApi(row)}>API</BzButton> : null}
          {canEdit ? <BzButton size="small" disabled={row.level === "SYSTEM"} onClick={() => onEdit(row)}>编辑</BzButton> : null}
          {canDelete ? <BzButton size="small" buttonType="danger" disabled={row.level === "SYSTEM"} onClick={() => onRemove(row)}>删除</BzButton> : null}
        </div>
      ),
    },
  ];

  return (
    <BzTable
      data={treeRows}
      columns={columns}
      rowKey={(item) => item.row.id}
      loading={loading}
      emptyText="暂无数据"
      size="small"
    />
  );
}
