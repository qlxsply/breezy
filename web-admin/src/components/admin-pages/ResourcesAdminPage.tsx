"use client";

import { deleteResource, listPermissions, listResources } from "@admin/api/resources";
import { createAdminActionsColumn } from "@admin/components/admin/admin-actions-column";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import type { BzTableColumn } from "@admin/components/bz";
import {
  BzButton,
  BzCard,
  BzChevronIcon,
  BzFormItem,
  BzInput,
  BzOption,
  BzOverflowTooltip,
  BzSelect,
  BzTable,
  BzTag,
} from "@admin/components/bz";
import { ResourceManageDrawer } from "@admin/components/resources-admin/ResourceManageDrawer";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { refreshRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type {
  ManageResourceType,
  ResourceManageEntry,
  ResourcePermissionOption,
} from "@admin/types/resource-manage";
import { useEffect, useMemo, useState } from "react";

type DrawerMode = "create" | "detail" | "edit";

interface ResourceTableRow {
  row: ResourceManageEntry;
  level: number;
}

const RESOURCE_TYPE_LABEL: Record<ManageResourceType, string> = {
  DIRECTORY: "目录",
  MENU: "菜单",
  FUNCTION: "功能",
  BUTTON: "按钮",
};

const ALLOWED_CHILDREN: Record<ManageResourceType, ManageResourceType[]> = {
  DIRECTORY: ["DIRECTORY", "MENU"],
  MENU: ["MENU", "FUNCTION", "BUTTON"],
  FUNCTION: ["BUTTON"],
  BUTTON: [],
};

const ROOT_ALLOWED_TYPES: ManageResourceType[] = ["DIRECTORY", "MENU"];

function canHaveChildren(resourceType: ManageResourceType): boolean {
  return ALLOWED_CHILDREN[resourceType].length > 0;
}

export function ResourcesAdminPage() {
  const [rows, setRows] = useState<ResourceManageEntry[]>([]);
  const [permissions, setPermissions] = useState<ResourcePermissionOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [typeFilterDraft, setTypeFilterDraft] = useState("");
  const [enabledFilterDraft, setEnabledFilterDraft] = useState("");
  const [builtinFilterDraft, setBuiltinFilterDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [enabledFilter, setEnabledFilter] = useState("");
  const [builtinFilter, setBuiltinFilter] = useState("");
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<DrawerMode>("create");
  const [drawerResourceId, setDrawerResourceId] = useState<string | null>(null);
  const [drawerParentId, setDrawerParentId] = useState<string | null>(null);

  const canView = hasResourceCodeAccess("resource-manage-view");
  const canCreate = hasResourceCodeAccess("resource-manage-create");
  const canEdit = hasResourceCodeAccess("resource-manage-edit");
  const canDelete = hasResourceCodeAccess("resource-manage-delete");
  const canPermissionView = hasResourceCodeAccess("resource-manage-permission-view");
  const canPermissionEdit = hasResourceCodeAccess("resource-manage-permission-edit");
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  useEffect(() => {
    void reload();
  }, []);

  const flatRows = useMemo(() => flattenRows(rows), [rows]);

  const rowMap = useMemo(() => {
    const map = new Map<string, ResourceManageEntry>();
    flatRows.forEach((item) => map.set(item.id, item));
    return map;
  }, [flatRows]);

  const hasActiveFilter = Boolean(keyword.trim() || typeFilter || enabledFilter || builtinFilter);

  const filteredRoots = useMemo(
    () => filterTree(rows, keyword.trim().toLowerCase(), typeFilter, enabledFilter, builtinFilter),
    [rows, keyword, typeFilter, enabledFilter, builtinFilter],
  );

  const tableRows = useMemo(
    () => flattenVisibleRows(filteredRoots, expandedIds, hasActiveFilter),
    [expandedIds, filteredRoots, hasActiveFilter],
  );

  const columns = useMemo<Array<BzTableColumn<ResourceTableRow>>>(() => {
    const baseColumns: Array<BzTableColumn<ResourceTableRow>> = [
      {
        key: "name",
        title: "资源名称",
        width: 320,
        className: "resource-manage-col-name is-sticky-left",
        headerClassName: "resource-manage-col-name is-sticky-left",
        render: ({ row, level }) => {
          const hasChildren = row.children.length > 0;
          const expanded = expandedIds.has(row.id);
          return (
            <div className="resource-name-cell">
              <span
                className="resource-indent"
                aria-hidden="true"
              >
                {Array.from({ length: level }).map((_, index) => (
                  <span
                    key={index}
                    className="resource-indent__unit"
                  />
                ))}
              </span>
              <button
                className={`resource-toggle${!hasChildren ? " is-placeholder" : ""}`}
                type="button"
                onClick={() => hasChildren && toggleExpand(row.id)}
              >
                <BzChevronIcon direction={expanded || hasActiveFilter ? "down" : "right"} />
              </button>
              <span className="resource-name-main cell-ellipsis">{row.name}</span>
            </div>
          );
        },
      },
      {
        key: "type",
        title: "类型",
        width: 88,
        render: ({ row }) => <BzTag size="small">{RESOURCE_TYPE_LABEL[row.resourceType]}</BzTag>,
      },
      {
        key: "code",
        title: "编码",
        width: 180,
        render: ({ row }) => <span className="resource-mono cell-ellipsis">{row.code}</span>,
      },
      {
        key: "path",
        title: "路由路径",
        width: 180,
        render: ({ row }) => {
          const text = row.path || "-";
          return (
            <BzOverflowTooltip text={text}>
              <span className="resource-mono cell-ellipsis">{text}</span>
            </BzOverflowTooltip>
          );
        },
      },
      {
        key: "component",
        title: "组件路径",
        width: 260,
        render: ({ row }) => {
          const text = row.component || "-";
          return (
            <BzOverflowTooltip text={text}>
              <span className="resource-mono cell-ellipsis">{text}</span>
            </BzOverflowTooltip>
          );
        },
      },
      {
        key: "sortNo",
        title: "排序",
        width: 72,
        render: ({ row }) => row.sortNo,
      },
      {
        key: "visible",
        title: "可见",
        width: 72,
        render: ({ row }) => (row.visible ? "是" : "否"),
      },
      {
        key: "enabled",
        title: "启用",
        width: 72,
        render: ({ row }) => (
          <span className={row.enabled ? "resource-status-on" : "resource-status-off"}>
            {row.enabled ? "启用" : "停用"}
          </span>
        ),
      },
      {
        key: "defaultEntry",
        title: "默认入口",
        width: 88,
        render: ({ row }) => (row.defaultEntry ? "是" : "否"),
      },
      {
        key: "systemBuiltin",
        title: "内置",
        width: 72,
        render: ({ row }) => (row.systemBuiltin ? "是" : "否"),
      },
      {
        key: "remark",
        title: "备注",
        width: 180,
        render: ({ row }) => {
          const text = row.remark || "-";
          return (
            <BzOverflowTooltip text={text}>
              <span className="cell-ellipsis">{text}</span>
            </BzOverflowTooltip>
          );
        },
      },
    ];
    const actionsColumn = createAdminActionsColumn({
      rows: tableRows,
      getActions: ({ row }) => {
        const childrenAllowed = canHaveChildren(row.resourceType);
        const actions: AdminActionItem[] = [];
        actions.push({
          key: "detail",
          label: "详情",
          tone: "detail",
          handler: () => void openEdit(row, "detail"),
        });
        if (canEdit) {
          actions.push({
            key: "edit",
            label: "编辑",
            tone: "edit",
            handler: () => void openEdit(row, "edit"),
          });
        }
        if (canDelete) {
          actions.push({
            key: "delete",
            label: "删除",
            tone: "delete",
            disabled: row.systemBuiltin,
            handler: () => void onDelete(row),
          });
        }
        if (canCreate) {
          actions.push({
            key: "create-child",
            label: "新增子项",
            tone: "neutral",
            disabled: !childrenAllowed,
            handler: () => openCreateChild(row),
          });
        }
        return actions;
      },
      stickyClassName: "resource-manage-col-actions is-sticky-right",
      stickyHeaderClassName: "resource-manage-col-actions is-sticky-right",
    });
    return actionsColumn ? [...baseColumns, actionsColumn] : baseColumns;
  }, [canCreate, canDelete, canEdit, expandedIds, hasActiveFilter, tableRows]);

  async function reload() {
    if (!canView) return;
    setLoading(true);
    try {
      const [resourceTree, permissionRows] = await Promise.all([
        listResources(),
        canPermissionView || canPermissionEdit
          ? listPermissions()
          : Promise.resolve<ResourcePermissionOption[]>([]),
      ]);
      setRows(resourceTree);
      setPermissions(permissionRows);
      setExpandedIds(new Set());
    } finally {
      setLoading(false);
    }
  }

  function applyFilters() {
    setKeyword(keywordDraft.trim());
    setTypeFilter(typeFilterDraft);
    setEnabledFilter(enabledFilterDraft);
    setBuiltinFilter(builtinFilterDraft);
    setExpandedIds(new Set(flattenRows(rows).map((item) => item.id)));
  }

  function openCreateRoot() {
    if (!canCreate) return;
    setDrawerMode("create");
    setDrawerResourceId(null);
    setDrawerParentId(null);
    setDrawerOpen(true);
  }

  function openCreateChild(target: ResourceManageEntry) {
    if (!canCreate) return;
    if (!canHaveChildren(target.resourceType)) return;
    setDrawerMode("create");
    setDrawerResourceId(null);
    setDrawerParentId(target.id);
    setDrawerOpen(true);
  }

  function openEdit(target: ResourceManageEntry, mode: DrawerMode = "edit") {
    if (mode === "edit" && !canEdit) return;
    if (mode === "detail" && !canView) return;
    setDrawerMode(mode);
    setDrawerResourceId(target.id);
    setDrawerParentId(null);
    setDrawerOpen(true);
  }

  function closeDrawer() {
    setDrawerOpen(false);
    setDrawerResourceId(null);
    setDrawerParentId(null);
  }

  async function onDelete(target: ResourceManageEntry) {
    if (!canDelete) return;
    if (target.systemBuiltin) {
      message.warning("系统内置资源不允许删除");
      return;
    }
    const descendantCount = countDescendants(rowMap.get(target.id) ?? target);
    const confirmed = await bzConfirm({
      title: "删除资源",
      content:
        descendantCount > 0
          ? `确认删除「${target.name}」及其 ${descendantCount} 个子资源？`
          : `确认删除「${target.name}」？`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteResource(target.id);
    await refreshRegistryLoaded();
    message.success("删除成功");
    await reload();
  }

  function toggleExpand(id: string) {
    setExpandedIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function expandAll() {
    setExpandedIds(new Set(flattenRows(filteredRoots).map((item) => item.id)));
  }

  function collapseAll() {
    setExpandedIds(new Set());
  }

  function resetFilters() {
    setKeywordDraft("");
    setTypeFilterDraft("");
    setEnabledFilterDraft("");
    setBuiltinFilterDraft("");
    setKeyword("");
    setTypeFilter("");
    setEnabledFilter("");
    setBuiltinFilter("");
    setExpandedIds(new Set());
  }

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          <BzCard
            className="admin-panel admin-table-card resource-manage-card"
            shadow="never"
          >
            <div className="resource-manage-region">
              {queryPanelVisible ? (
                <div className="resource-manage-query-panel">
                  <div
                    ref={queryCardRef}
                    className={[
                      "admin-query-layout",
                      querySingleRow
                        ? "is-single-row"
                        : queryExpanded
                          ? "is-expanded"
                          : "is-collapsed",
                    ].join(" ")}
                  >
                    <form
                      ref={queryGridRef}
                      className="bz-form admin-query-grid"
                      onSubmit={(event) => {
                        event.preventDefault();
                        applyFilters();
                      }}
                    >
                      <BzFormItem className="admin-query-field">
                        <div className="admin-query-field__label">关键字</div>
                        <div className="admin-query-field__control">
                          <BzInput
                            modelValue={keywordDraft}
                            placeholder="搜索资源名称 / 编码 / 路径 / 组件"
                            clearable
                            onValueChange={setKeywordDraft}
                            onKeyUp={(event) => {
                              if (event.key === "Enter") applyFilters();
                            }}
                          />
                        </div>
                      </BzFormItem>
                      <BzFormItem className="admin-query-field">
                        <div className="admin-query-field__label">资源类型</div>
                        <div className="admin-query-field__control">
                          <BzSelect
                            modelValue={typeFilterDraft}
                            placeholder="全部类型"
                            clearable
                            onValueChange={(value) => setTypeFilterDraft(value ?? "")}
                          >
                            <BzOption
                              value="DIRECTORY"
                              label="目录"
                            />
                            <BzOption
                              value="MENU"
                              label="菜单"
                            />
                            <BzOption
                              value="FUNCTION"
                              label="功能"
                            />
                            <BzOption
                              value="BUTTON"
                              label="按钮"
                            />
                          </BzSelect>
                        </div>
                      </BzFormItem>
                      <BzFormItem className="admin-query-field">
                        <div className="admin-query-field__label">启用状态</div>
                        <div className="admin-query-field__control">
                          <BzSelect
                            modelValue={enabledFilterDraft}
                            placeholder="全部状态"
                            clearable
                            onValueChange={(value) => setEnabledFilterDraft(value ?? "")}
                          >
                            <BzOption
                              value="true"
                              label="启用"
                            />
                            <BzOption
                              value="false"
                              label="停用"
                            />
                          </BzSelect>
                        </div>
                      </BzFormItem>
                      <BzFormItem className="admin-query-field">
                        <div className="admin-query-field__label">内置状态</div>
                        <div className="admin-query-field__control">
                          <BzSelect
                            modelValue={builtinFilterDraft}
                            placeholder="全部"
                            clearable
                            onValueChange={(value) => setBuiltinFilterDraft(value ?? "")}
                          >
                            <BzOption
                              value="true"
                              label="系统内置"
                            />
                            <BzOption
                              value="false"
                              label="非内置"
                            />
                          </BzSelect>
                        </div>
                      </BzFormItem>
                      <div className="admin-query-actions">
                        <BzButton
                          className="admin-filter-secondary"
                          nativeType="button"
                          onClick={resetFilters}
                        >
                          重置
                        </BzButton>
                        <BzButton
                          className="admin-filter-primary"
                          buttonType="primary"
                          nativeType="button"
                          onClick={applyFilters}
                        >
                          搜索
                        </BzButton>
                        {!querySingleRow ? (
                          <button
                            className="admin-filter-toggle"
                            type="button"
                            aria-expanded={queryExpanded}
                            onClick={() => setQueryExpanded((value) => !value)}
                          >
                            <span>{queryExpanded ? "收起" : "展开"}</span>
                            <i
                              className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`}
                              aria-hidden="true"
                            />
                          </button>
                        ) : null}
                      </div>
                    </form>
                  </div>
                </div>
              ) : null}

              <div className="resource-manage-toolbar-row">
                <div className="resource-manage-business-actions">
                  {canCreate ? (
                    <BzButton
                      className="admin-toolbar-primary"
                      buttonType="primary"
                      onClick={openCreateRoot}
                    >
                      新增
                    </BzButton>
                  ) : null}
                </div>
                <div className="resource-manage-query-tools">
                  <button
                    className="admin-vben-circle-button"
                    type="button"
                    title="全部展开"
                    onClick={expandAll}
                  >
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M4 6h16" />
                      <path d="M7 12h10" />
                      <path d="M10 18h4" />
                    </svg>
                  </button>
                  <button
                    className="admin-vben-circle-button"
                    type="button"
                    title="全部收起"
                    onClick={collapseAll}
                  >
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M4 6h16" />
                      <path d="M7 12h10" />
                    </svg>
                  </button>
                  <AdminTableTools
                    queryPanelVisible={queryPanelVisible}
                    onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
                    onRefresh={() => reload()}
                  />
                </div>
              </div>

              <div className="admin-table-surface resource-manage-table-area resource-manage-table-scope">
                <BzTable
                  data={tableRows}
                  columns={columns}
                  rowKey={(item) => item.row.id}
                  loading={loading}
                  emptyText="暂无数据"
                  size="small"
                />
              </div>

              <div className="resource-manage-table-footer">
                <div>共 {flatRows.length} 条记录</div>
              </div>
            </div>
          </BzCard>
        </div>

        {drawerOpen ? (
          <ResourceManageDrawer
            mode={drawerMode}
            resourceId={drawerResourceId}
            parentId={drawerParentId}
            allResources={rows}
            permissions={permissions}
            canEdit={canEdit}
            canPermissionEdit={canPermissionEdit}
            onClose={closeDrawer}
            onSaved={() => void reload()}
          />
        ) : null}
      </div>
    </div>
  );
}

function flattenRows(rows: ResourceManageEntry[]): ResourceManageEntry[] {
  const result: ResourceManageEntry[] = [];
  rows.forEach((row) => {
    result.push(row);
    result.push(...flattenRows(row.children));
  });
  return result;
}

function flattenVisibleRows(
  rows: ResourceManageEntry[],
  expandedIds: Set<string>,
  forceExpand: boolean,
): ResourceTableRow[] {
  const result: ResourceTableRow[] = [];
  const walk = (items: ResourceManageEntry[], level: number) => {
    items.forEach((row) => {
      result.push({ row, level });
      if (row.children.length > 0 && (forceExpand || expandedIds.has(row.id))) {
        walk(row.children, level + 1);
      }
    });
  };
  walk(rows, 0);
  return result;
}

function filterTree(
  rows: ResourceManageEntry[],
  keyword: string,
  typeFilter: string,
  enabledFilter: string,
  builtinFilter: string,
): ResourceManageEntry[] {
  return rows
    .map((row) => {
      const children = filterTree(row.children, keyword, typeFilter, enabledFilter, builtinFilter);
      const selfMatched = matchRow(row, keyword, typeFilter, enabledFilter, builtinFilter);
      if (!keyword && !typeFilter && !enabledFilter && !builtinFilter) {
        return { ...row, children };
      }
      if (selfMatched || children.length > 0) {
        return { ...row, children };
      }
      return null;
    })
    .filter((row): row is ResourceManageEntry => Boolean(row));
}

function matchRow(
  row: ResourceManageEntry,
  keyword: string,
  typeFilter: string,
  enabledFilter: string,
  builtinFilter: string,
): boolean {
  if (typeFilter && row.resourceType !== typeFilter) return false;
  if (enabledFilter && String(row.enabled) !== enabledFilter) return false;
  if (builtinFilter && String(row.systemBuiltin) !== builtinFilter) return false;
  if (!keyword) return true;
  return [row.name, row.code, row.path, row.component]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword));
}

function countDescendants(row: ResourceManageEntry): number {
  return flattenRows(row.children).length;
}
