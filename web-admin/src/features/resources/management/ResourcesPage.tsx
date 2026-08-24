"use client";

import {
  deleteResource,
  listPermissions,
  listResources,
} from "@admin/features/resources/management/api/client";
import type {
  ManageResourceType,
  ResourceManageEntry,
  ResourcePermissionOption,
} from "@admin/features/resources/management/model/types";
import { RESOURCE_MANAGEMENT_PERMISSIONS } from "@admin/features/resources/management/permissions";
import styles from "@admin/features/resources/management/ResourcesPage.module.css";
import { ResourceManageDrawer } from "@admin/features/resources/management/ui/ResourceManageDrawer";
import { usePermission } from "@admin/features/resources/permissions";
import { refreshResources } from "@admin/features/resources/service/resource-service";
import { useAdminQueryPanelLayout } from "@admin/shared/hooks/useAdminQueryPanelLayout";
import { bzConfirm } from "@admin/shared/lib/feedback/confirm";
import { message } from "@admin/shared/lib/feedback/message";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import type { BzTableColumn } from "@admin/shared/ui/bz";
import {
  BzAlert,
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
} from "@admin/shared/ui/bz";
import { useEffect, useMemo, useRef, useState } from "react";

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

function canHaveChildren(resourceType: ManageResourceType): boolean {
  return ALLOWED_CHILDREN[resourceType].length > 0;
}

export function ResourcesPage() {
  const [rows, setRows] = useState<ResourceManageEntry[]>([]);
  const [permissions, setPermissions] = useState<ResourcePermissionOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState("");
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
  const reloadControllerRef = useRef<AbortController | null>(null);
  const reloadGenerationRef = useRef(0);

  const canView = usePermission(RESOURCE_MANAGEMENT_PERMISSIONS.view);
  const canCreate = usePermission(RESOURCE_MANAGEMENT_PERMISSIONS.create);
  const canEdit = usePermission(RESOURCE_MANAGEMENT_PERMISSIONS.edit);
  const canDelete = usePermission(RESOURCE_MANAGEMENT_PERMISSIONS.delete);
  const canPermissionView = usePermission(RESOURCE_MANAGEMENT_PERMISSIONS.permissionView);
  const canPermissionEdit = usePermission(RESOURCE_MANAGEMENT_PERMISSIONS.permissionEdit);
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  useEffect(() => {
    if (canView) void reload();
    else setLoading(false);

    return () => {
      reloadGenerationRef.current += 1;
      reloadControllerRef.current?.abort();
      reloadControllerRef.current = null;
    };
  }, [canPermissionEdit, canPermissionView, canView]);

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
        className: `${styles.colName} ${styles.stickyLeft}`,
        headerClassName: `${styles.colName} ${styles.stickyLeft}`,
        render: ({ row, level }) => {
          const hasChildren = row.children.length > 0;
          const expanded = expandedIds.has(row.id);
          return (
            <div className={styles.nameCell}>
              <span
                className={styles.indent}
                aria-hidden="true"
              >
                {Array.from({ length: level }).map((_, index) => (
                  <span
                    key={index}
                    className={styles.indentUnit}
                  />
                ))}
              </span>
              <button
                className={`${styles.toggle}${!hasChildren ? ` ${styles.placeholder}` : ""}`}
                type="button"
                onClick={() => hasChildren && toggleExpand(row.id)}
              >
                <BzChevronIcon
                  className={styles.chevron}
                  direction={expanded || hasActiveFilter ? "down" : "right"}
                />
              </button>
              <span className={`${styles.nameMain} ${styles.cellEllipsis}`}>{row.name}</span>
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
        render: ({ row }) => (
          <span className={`${styles.mono} ${styles.cellEllipsis}`}>{row.code}</span>
        ),
      },
      {
        key: "path",
        title: "路由路径",
        width: 180,
        render: ({ row }) => {
          const text = row.path || "-";
          return (
            <BzOverflowTooltip text={text}>
              <span className={`${styles.mono} ${styles.cellEllipsis}`}>{text}</span>
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
              <span className={`${styles.mono} ${styles.cellEllipsis}`}>{text}</span>
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
          <span className={row.enabled ? styles.statusOn : styles.statusOff}>
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
              <span className={styles.cellEllipsis}>{text}</span>
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
          level: "default",
          onClick: () => void openEdit(row, "detail"),
        });
        if (canEdit) {
          actions.push({
            key: "edit",
            label: "编辑",
            level: "primary",
            onClick: () => void openEdit(row, "edit"),
          });
        }
        if (canDelete) {
          actions.push({
            key: "delete",
            label: "删除",
            level: "danger",
            disabled: row.systemBuiltin,
            onClick: () => void onDelete(row),
          });
        }
        if (canCreate) {
          actions.push({
            key: "create-child",
            label: "新增子项",
            level: "primary",
            disabled: !childrenAllowed,
            onClick: () => openCreateChild(row),
          });
        }
        return actions;
      },
      stickyClassName: `${styles.colActions} ${styles.stickyRight}`,
      stickyHeaderClassName: `${styles.colActions} ${styles.stickyRight}`,
    });
    return actionsColumn ? [...baseColumns, actionsColumn] : baseColumns;
  }, [canCreate, canDelete, canEdit, expandedIds, hasActiveFilter, tableRows]);

  async function reload() {
    if (!canView) return;
    reloadControllerRef.current?.abort();
    const controller = new AbortController();
    const generation = ++reloadGenerationRef.current;
    reloadControllerRef.current = controller;
    setLoading(true);
    setLoadError("");
    try {
      const [resourceTree, permissionRows] = await Promise.all([
        listResources({ signal: controller.signal }),
        canPermissionView || canPermissionEdit
          ? listPermissions({ signal: controller.signal })
          : Promise.resolve<ResourcePermissionOption[]>([]),
      ]);
      if (generation !== reloadGenerationRef.current) return;
      setRows(resourceTree);
      setPermissions(permissionRows);
    } catch (error: unknown) {
      if (generation !== reloadGenerationRef.current || isAbortError(error)) return;
      setLoadError(error instanceof Error && error.message ? error.message : "资源列表加载失败");
    } finally {
      if (generation === reloadGenerationRef.current) {
        setLoading(false);
        reloadControllerRef.current = null;
      }
    }
  }

  async function synchronizeResourceTrees() {
    // Runtime refresh temporarily removes this route; the remounted page reloads the management tree.
    await refreshResources();
  }

  function applyFilters() {
    setKeyword(keywordDraft.trim());
    setTypeFilter(typeFilterDraft);
    setEnabledFilter(enabledFilterDraft);
    setBuiltinFilter(builtinFilterDraft);
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
    await synchronizeResourceTrees();
    message.success("删除成功");
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
  }

  return (
    <div className={`${layoutStyles.page} ${styles.root}`}>
      <div className={layoutStyles.content}>
        <div className={layoutStyles.pageStack}>
          <BzCard
            className={`${layoutStyles.panel} ${layoutStyles.tableCard} ${styles.card}`}
            shadow="never"
          >
            <div className={styles.region}>
              {queryPanelVisible ? (
                <div className={styles.queryPanel}>
                  <div
                    ref={queryCardRef}
                    className={[
                      layoutStyles.queryLayout,
                      querySingleRow
                        ? layoutStyles.singleRow
                        : queryExpanded
                          ? layoutStyles.expanded
                          : layoutStyles.collapsed,
                    ].join(" ")}
                  >
                    <form
                      ref={queryGridRef}
                      className={layoutStyles.queryGrid}
                      onSubmit={(event) => {
                        event.preventDefault();
                        applyFilters();
                      }}
                    >
                      <BzFormItem className={layoutStyles.queryField}>
                        <div className={layoutStyles.queryFieldLabel}>关键字</div>
                        <div className={layoutStyles.queryFieldControl}>
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
                      <BzFormItem className={layoutStyles.queryField}>
                        <div className={layoutStyles.queryFieldLabel}>资源类型</div>
                        <div className={layoutStyles.queryFieldControl}>
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
                      <BzFormItem className={layoutStyles.queryField}>
                        <div className={layoutStyles.queryFieldLabel}>启用状态</div>
                        <div className={layoutStyles.queryFieldControl}>
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
                      <BzFormItem className={layoutStyles.queryField}>
                        <div className={layoutStyles.queryFieldLabel}>内置状态</div>
                        <div className={layoutStyles.queryFieldControl}>
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
                      <div className={layoutStyles.queryActions}>
                        <BzButton
                          className={layoutStyles.filterSecondary}
                          nativeType="button"
                          onClick={resetFilters}
                        >
                          重置
                        </BzButton>
                        <BzButton
                          className={layoutStyles.filterPrimary}
                          buttonType="primary"
                          nativeType="button"
                          onClick={applyFilters}
                        >
                          搜索
                        </BzButton>
                        {!querySingleRow ? (
                          <button
                            className={layoutStyles.filterToggle}
                            type="button"
                            aria-expanded={queryExpanded}
                            onClick={() => setQueryExpanded((value) => !value)}
                          >
                            <span>{queryExpanded ? "收起" : "展开"}</span>
                            <i
                              className={`${layoutStyles.filterToggleIcon} ${queryExpanded ? layoutStyles.up : layoutStyles.down}`}
                              aria-hidden="true"
                            />
                          </button>
                        ) : null}
                      </div>
                    </form>
                  </div>
                </div>
              ) : null}

              <div className={styles.toolbarRow}>
                <div className={styles.businessActions}>
                  {canCreate ? (
                    <BzButton
                      className={layoutStyles.toolbarPrimary}
                      buttonType="primary"
                      onClick={openCreateRoot}
                    >
                      新增
                    </BzButton>
                  ) : null}
                </div>
                <div className={styles.queryTools}>
                  <button
                    className={`${layoutStyles.circleButton} ${styles.circleButton}`}
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
                    className={`${layoutStyles.circleButton} ${styles.circleButton}`}
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

              <div
                className={`${layoutStyles.tableSurface} ${styles.tableArea} ${styles.tableScope}`}
              >
                {loadError ? (
                  <BzAlert
                    key={loadError}
                    title={loadError}
                    type="error"
                    closable={false}
                  />
                ) : null}
                <BzTable
                  data={tableRows}
                  columns={columns}
                  rowKey={(item) => item.row.id}
                  loading={loading}
                  emptyText="暂无数据"
                  size="small"
                />
              </div>

              <div className={styles.tableFooter}>
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
            canEdit={drawerMode === "create" ? canCreate : canEdit}
            canPermissionView={canPermissionView || canPermissionEdit}
            canPermissionEdit={canPermissionEdit}
            onClose={closeDrawer}
            onSaved={synchronizeResourceTrees}
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

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}
