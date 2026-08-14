"use client";

import { batchListDictOptions } from "@admin/api/dicts";
import {
  createUserFeaturePackage,
  deleteUserFeaturePackage,
  getUserFeaturePackage,
  listUserFeatureApplications,
  pageUserFeaturePackages,
  updateUserFeaturePackage,
  updateUserFeaturePackageStatus,
} from "@admin/api/user-features";
import { createAdminActionsColumn } from "@admin/components/admin/admin-actions-column";
import { AdminListPageTemplate } from "@admin/components/admin/AdminListPageTemplate";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import {
  BzButton,
  BzFormItem,
  BzInput,
  BzOption,
  BzPagination,
  BzSelect,
  BzTable,
  BzTag,
  BzTooltip,
} from "@admin/components/bz";
import type { BzTableColumn } from "@admin/components/bz/BzTable";
import {
  type UserFeaturePackageDrawerMode,
  UserFeaturePackageManageDrawer,
} from "@admin/components/user-feature-packages/UserFeaturePackageManageDrawer";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { useIsRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import type {
  SaveUserFeaturePackageRequest,
  UserFeatureApplicationEntry,
  UserFeaturePackageEntry,
} from "@admin/types/user-feature";
import { useEffect, useMemo, useRef, useState } from "react";

const pageSizeOptions = [10, 20, 30, 50, 100];
type TagType = "info" | "success" | "warning" | "danger";
type DictMeta = { label: string; tagType?: TagType };

function toMetaMap(items?: DictItem[]): Record<string, DictMeta> {
  const map: Record<string, DictMeta> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = {
      label: item.itemLabel || item.itemValue,
      tagType: (item.tagType as TagType | null) || undefined,
    };
  }
  return map;
}

function resolveLabel(metaMap: Record<string, DictMeta>, value?: string | null): string {
  if (!value) return "-";
  return metaMap[value]?.label || value;
}

function resolveTagType(metaMap: Record<string, DictMeta>, value?: string | null): string {
  if (!value) return "info";
  return metaMap[value]?.tagType || "info";
}

export function UserFeaturePackagesPage() {
  const permissionsLoaded = useIsRegistryLoaded();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<UserFeaturePackageEntry[]>([]);
  const [applications, setApplications] = useState<UserFeatureApplicationEntry[]>([]);
  const [page, setPage] = useState<PageResult<UserFeaturePackageEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [enabledDraft, setEnabledDraft] = useState<"" | "true" | "false">("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedEnabled, setAppliedEnabled] = useState<"" | "true" | "false">("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [drawerMode, setDrawerMode] = useState<UserFeaturePackageDrawerMode>("detail");
  const [currentPackage, setCurrentPackage] = useState<UserFeaturePackageEntry | null>(null);
  const [packageTypeMetaMap, setPackageTypeMetaMap] = useState<Record<string, DictMeta>>({});
  const [saving, setSaving] = useState(false);

  const metadataLoadedRef = useRef(false);

  const canView = hasResourceCodeAccess("user-feature-package-view");
  const canEdit = hasResourceCodeAccess("user-feature-package-edit");

  const packageTypeOptions = useMemo(
    () => Object.entries(packageTypeMetaMap).map(([value, meta]) => ({ value, label: meta.label })),
    [packageTypeMetaMap],
  );

  async function reload() {
    if (!canView) {
      setRows([]);
      setPage({
        pageNo: 1,
        pageSize,
        numberOfElements: 0,
        totalPages: 0,
        totalElements: 0,
        elements: [],
      });
      return;
    }
    setLoading(true);
    try {
      const result = await pageUserFeaturePackages({
        keyword: appliedKeyword || undefined,
        enabled: appliedEnabled === "" ? undefined : appliedEnabled === "true",
        page: { pageNo, pageSize },
        sort: {
          orders: [
            { field: "displayOrder", direction: "ASC" },
            { field: "packageName", direction: "ASC" },
          ],
        },
      });
      setPage(result);
      setPageNo(result.pageNo || 1);
      setPageSize(result.pageSize || pageSize);
      setRows(result.elements);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!permissionsLoaded || metadataLoadedRef.current) return;
    metadataLoadedRef.current = true;
    void (async () => {
      try {
        const dictResult = await batchListDictOptions(["USER_APPLICATION_PACKAGE_TYPE"]);
        setPackageTypeMetaMap(toMetaMap(dictResult.USER_APPLICATION_PACKAGE_TYPE));
      } catch {
        setPackageTypeMetaMap({
          DEFAULT: { label: "默认包", tagType: "info" },
          MEMBERSHIP: { label: "会员包", tagType: "success" },
          OPERATION: { label: "运营包", tagType: "warning" },
          ENTERPRISE: { label: "企业包", tagType: "danger" },
          CUSTOM: { label: "自定义", tagType: "info" },
        });
      }
      if (canView || canEdit) {
        try {
          setApplications(await listUserFeatureApplications());
        } catch {
          setApplications([]);
        }
      }
    })();
  }, [permissionsLoaded, canView, canEdit]);

  useEffect(() => {
    if (!permissionsLoaded) return;
    void reload();
  }, [permissionsLoaded, pageNo, pageSize, appliedKeyword, appliedEnabled]);

  function applyFilters() {
    setAppliedKeyword(keywordDraft.trim());
    setAppliedEnabled(enabledDraft);
    setPageNo(1);
  }

  function resetFilters() {
    setKeywordDraft("");
    setEnabledDraft("");
    setAppliedKeyword("");
    setAppliedEnabled("");
    setPageNo(1);
  }

  function getRowActions(row: UserFeaturePackageEntry): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      {
        key: `detail-${row.id}`,
        label: "详情",
        level: "default",
        onClick: () => openDetail(row.id),
      },
    ];
    if (canEdit) {
      actions.push({
        key: `edit-${row.id}`,
        label: "编辑",
        level: "primary",
        onClick: () => openEdit(row.id),
      });
    }
    return actions;
  }

  function getRowMoreActions(row: UserFeaturePackageEntry): AdminActionItem[] {
    if (!canEdit) return [];
    return [
      {
        key: `toggle-${row.id}`,
        label: row.enabled ? "停用" : "启用",
        level: row.enabled ? "warning" : "success",
        onClick: () => toggleStatus(row),
      },
      {
        key: `delete-${row.id}`,
        label: "删除",
        level: "danger",
        onClick: () => removePackage(row),
      },
    ];
  }

  function openCreate() {
    setDrawerMode("create");
    setCurrentPackage(null);
    setDrawerOpen(true);
  }

  async function openEdit(id: string) {
    setCurrentPackage(null);
    setDrawerMode("edit");
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      setCurrentPackage(await getUserFeaturePackage(id));
    } finally {
      setDrawerLoading(false);
    }
  }

  async function openDetail(id: string) {
    setCurrentPackage(null);
    setDrawerMode("detail");
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      setCurrentPackage(await getUserFeaturePackage(id));
    } finally {
      setDrawerLoading(false);
    }
  }

  async function submitPackage(payload: SaveUserFeaturePackageRequest) {
    if (saving) return;
    if (!payload.code || !payload.name) {
      message.warning("编码和名称不能为空");
      return;
    }
    setSaving(true);
    try {
      if (drawerMode === "create") {
        await createUserFeaturePackage(payload);
        message.success("应用包已创建");
      } else if (currentPackage) {
        await updateUserFeaturePackage(currentPackage.id, payload);
        message.success("应用包已更新");
      }
      setDrawerOpen(false);
      await reload();
    } finally {
      setSaving(false);
    }
  }

  async function toggleStatus(row: UserFeaturePackageEntry) {
    const nextEnabled = !row.enabled;
    const confirmed = await bzConfirm({
      title: nextEnabled ? "启用应用包" : "停用应用包",
      content: `确认${nextEnabled ? "启用" : "停用"}：${row.name}？`,
      confirmText: nextEnabled ? "启用" : "停用",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await updateUserFeaturePackageStatus(row.id, nextEnabled);
    message.success(nextEnabled ? "已启用" : "已停用");
    await reload();
  }

  async function removePackage(row: UserFeaturePackageEntry) {
    const confirmed = await bzConfirm({
      title: "删除应用包",
      content: `确认删除：${row.name}？`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteUserFeaturePackage(row.id);
    message.success("应用包已删除");
    await reload();
  }

  const columns = useMemo<Array<BzTableColumn<UserFeaturePackageEntry>>>(() => {
    const baseColumns: Array<BzTableColumn<UserFeaturePackageEntry>> = [
      {
        key: "code",
        title: "编码",
        width: 200,
        className: "admin-freeze-col--feature-package-code is-sticky-left",
        headerClassName: "admin-freeze-col--feature-package-code is-sticky-left",
        render: (row) => <>{row.code}</>,
      },
      { key: "name", title: "名称", minWidth: 160, render: (row) => <>{row.name}</> },
      {
        key: "packageType",
        title: "类型",
        width: 120,
        render: (row) => (
          <BzTag type={resolveTagType(packageTypeMetaMap, row.packageType) as TagType}>
            {resolveLabel(packageTypeMetaMap, row.packageType)}
          </BzTag>
        ),
      },
      {
        key: "defaultPackage",
        title: "默认包",
        width: 120,
        render: (row) => (
          <BzTag type={row.defaultPackage ? "success" : "info"}>
            {row.defaultPackage ? "是" : "否"}
          </BzTag>
        ),
      },
      {
        key: "enabled",
        title: "状态",
        width: 120,
        render: (row) => (
          <BzTag type={row.enabled ? "success" : "danger"}>{row.enabled ? "启用" : "停用"}</BzTag>
        ),
      },
      {
        key: "description",
        title: "描述",
        width: 260,
        className: "user-feature-package-description-column",
        headerClassName: "user-feature-package-description-column",
        render: (row) => {
          const description = row.description || "-";
          return (
            <div className="user-feature-package-description-wrap">
              <BzTooltip content={description}>
                <span className="user-feature-package-description-ellipsis">{description}</span>
              </BzTooltip>
            </div>
          );
        },
      },
      {
        key: "applicationAccesses",
        title: "应用数",
        width: 120,
        render: (row) => <>{row.applicationAccesses.length}</>,
      },
    ];
    const actionsColumn = createAdminActionsColumn({
      rows,
      getActions: (row) => [...getRowActions(row), ...getRowMoreActions(row)],
    });
    return actionsColumn ? [...baseColumns, actionsColumn] : baseColumns;
  }, [packageTypeMetaMap, canEdit, rows]);

  return (
    <>
      <AdminListPageTemplate
        queryPanelVisible={queryPanelVisible}
        queryPanel={
          <div
            ref={queryCardRef}
            className={[
              "admin-query-layout",
              querySingleRow ? "is-single-row" : queryExpanded ? "is-expanded" : "is-collapsed",
            ].join(" ")}
          >
            <form
              ref={queryGridRef}
              className="bz-form admin-query-grid"
              onSubmit={(e) => {
                e.preventDefault();
                applyFilters();
              }}
            >
              <BzFormItem className="admin-query-field">
                <div className="admin-query-field__label">关键词</div>
                <div className="admin-query-field__control">
                  <BzInput
                    modelValue={keywordDraft}
                    placeholder="按编码或名称搜索"
                    clearable
                    onValueChange={setKeywordDraft}
                    onKeyUp={(e) => e.key === "Enter" && applyFilters()}
                  />
                </div>
              </BzFormItem>
              <BzFormItem className="admin-query-field">
                <div className="admin-query-field__label">状态</div>
                <div className="admin-query-field__control">
                  <BzSelect
                    modelValue={enabledDraft}
                    placeholder="全部状态"
                    clearable
                    onValueChange={(v) => setEnabledDraft((v || "") as "" | "true" | "false")}
                  >
                    <BzOption
                      label="启用"
                      value="true"
                    />
                    <BzOption
                      label="停用"
                      value="false"
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
        }
        businessActions={
          canEdit ? (
            <BzButton
              className="admin-toolbar-primary"
              buttonType="primary"
              onClick={openCreate}
            >
              新增
            </BzButton>
          ) : null
        }
        queryTools={
          <AdminTableTools
            queryPanelVisible={queryPanelVisible}
            onToggleQueryPanel={() => setQueryPanelVisible((v) => !v)}
            onRefresh={() => void reload()}
          />
        }
        table={
          <BzTable
            columns={columns}
            data={rows}
            loading={loading}
            rowKey="id"
            emptyText="暂无应用包"
            size="small"
          />
        }
        footer={
          page.totalElements > 0 ? (
            <div className="dict-pagination-bar admin-list-table-footer">
              <div className="dict-pagination-summary">共 {page.totalElements} 条记录</div>
              <div className="dict-pagination-right">
                <BzPagination
                  total={page.totalElements}
                  pageSize={pageSize}
                  currentPage={pageNo}
                  pageSizes={pageSizeOptions}
                  onCurrentChange={setPageNo}
                  onSizeChange={(size) => {
                    if (!Number.isFinite(size) || size <= 0 || size === pageSize) return;
                    setPageSize(size);
                    setPageNo(1);
                  }}
                />
              </div>
            </div>
          ) : null
        }
      />

      <UserFeaturePackageManageDrawer
        open={drawerOpen}
        mode={drawerMode}
        packageEntry={currentPackage}
        applications={applications}
        packageTypeOptions={packageTypeOptions}
        loading={drawerLoading}
        saving={saving}
        onClose={() => setDrawerOpen(false)}
        onSubmit={submitPackage}
      />
    </>
  );
}
