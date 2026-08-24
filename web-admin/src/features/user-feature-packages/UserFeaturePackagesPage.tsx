"use client";

import { batchListDictionaryOptions as batchListDictOptions } from "@admin/features/dicts/public/dictionary-client";
import type { PublicDictionaryItem as DictItem } from "@admin/features/dicts/public/types";
import { usePermission } from "@admin/features/resources/permissions";
import {
  listUserFeatureApplications,
  type UserFeatureApplicationEntry,
} from "@admin/features/user-feature-applications/public/catalog";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { bzConfirm } from "@admin/shared/lib/feedback/confirm";
import { message } from "@admin/shared/lib/feedback/message";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import { AdminListPageTemplate } from "@admin/shared/ui/admin/AdminListPageTemplate";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { AdminSearchField, AdminSearchForm } from "@admin/shared/ui/admin/AdminSearchForm";
import { AdminTablePagination } from "@admin/shared/ui/admin/AdminTablePagination";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import {
  BzAlert,
  BzButton,
  BzInput,
  BzOption,
  BzSelect,
  BzTable,
  BzTag,
  BzTooltip,
} from "@admin/shared/ui/bz";
import type { BzTableColumn } from "@admin/shared/ui/bz/BzTable";
import { useEffect, useMemo, useRef, useState } from "react";

import {
  createUserFeaturePackage,
  deleteUserFeaturePackage,
  getUserFeaturePackage,
  pageUserFeaturePackages,
  updateUserFeaturePackage,
  updateUserFeaturePackageStatus,
} from "./api/client";
import type { SaveUserFeaturePackageRequest } from "./api/payload";
import type { UserFeaturePackageEntry } from "./model/types";
import { USER_FEATURE_PACKAGE_PERMISSIONS } from "./permissions";
import {
  type UserFeaturePackageDrawerMode,
  UserFeaturePackageManageDrawer,
} from "./ui/UserFeaturePackageManageDrawer";
import styles from "./UserFeaturePackagesPage.module.css";

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const INITIAL_FILTERS = { keyword: "", enabled: "" as "" | "true" | "false" };
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
  const [applications, setApplications] = useState<UserFeatureApplicationEntry[]>([]);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [drawerMode, setDrawerMode] = useState<UserFeaturePackageDrawerMode>("detail");
  const [currentPackage, setCurrentPackage] = useState<UserFeaturePackageEntry | null>(null);
  const [packageTypeMetaMap, setPackageTypeMetaMap] = useState<Record<string, DictMeta>>({});
  const [saving, setSaving] = useState(false);

  const canView = usePermission(USER_FEATURE_PACKAGE_PERMISSIONS.view);
  const canEdit = usePermission(USER_FEATURE_PACKAGE_PERMISSIONS.edit);
  const metadataControllerRef = useRef<AbortController | null>(null);
  const drawerControllerRef = useRef<AbortController | null>(null);
  const mutationLockRef = useRef(false);
  const {
    page,
    draftFilters,
    setDraftFilters,
    pageNo,
    pageSize,
    loading,
    error,
    submit,
    reset,
    refresh,
    setPageNo,
    setPageSize,
  } = useAdminPagedQuery<UserFeaturePackageEntry, typeof INITIAL_FILTERS>({
    initialFilters: INITIAL_FILTERS,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canView,
    normalizeFilters: (filters) => ({ ...filters, keyword: filters.keyword.trim() }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) =>
      pageUserFeaturePackages(
        {
          keyword: filters.keyword || undefined,
          enabled: filters.enabled === "" ? undefined : filters.enabled === "true",
          page: { pageNo: targetPage, pageSize: targetSize },
          sort: {
            orders: [
              { field: "displayOrder", direction: "ASC" },
              { field: "packageName", direction: "ASC" },
            ],
          },
        },
        { signal },
      ),
  });
  const rows = page.elements;

  const packageTypeOptions = useMemo(
    () => Object.entries(packageTypeMetaMap).map(([value, meta]) => ({ value, label: meta.label })),
    [packageTypeMetaMap],
  );

  useEffect(() => {
    const controller = new AbortController();
    metadataControllerRef.current = controller;
    void (async () => {
      try {
        const dictResult = await batchListDictOptions(["USER_APPLICATION_PACKAGE_TYPE"], {
          signal: controller.signal,
        });
        if (controller.signal.aborted) return;
        setPackageTypeMetaMap(toMetaMap(dictResult.USER_APPLICATION_PACKAGE_TYPE));
      } catch {
        if (controller.signal.aborted) return;
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
          const nextApplications = await listUserFeatureApplications({ signal: controller.signal });
          if (!controller.signal.aborted) setApplications(nextApplications);
        } catch {
          if (!controller.signal.aborted) setApplications([]);
        }
      }
    })();
    return () => controller.abort();
  }, [canView, canEdit]);

  useEffect(
    () => () => {
      metadataControllerRef.current?.abort();
      drawerControllerRef.current?.abort();
    },
    [],
  );

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
    drawerControllerRef.current?.abort();
    setDrawerMode("create");
    setCurrentPackage(null);
    setDrawerOpen(true);
  }

  async function openEdit(id: string) {
    drawerControllerRef.current?.abort();
    const controller = new AbortController();
    drawerControllerRef.current = controller;
    setCurrentPackage(null);
    setDrawerMode("edit");
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      const nextPackage = await getUserFeaturePackage(id, { signal: controller.signal });
      if (drawerControllerRef.current === controller && !controller.signal.aborted)
        setCurrentPackage(nextPackage);
    } catch (cause) {
      if (!controller.signal.aborted) {
        message.error(cause instanceof Error ? cause.message : "应用包加载失败");
        setDrawerOpen(false);
      }
    } finally {
      if (drawerControllerRef.current === controller) {
        drawerControllerRef.current = null;
        setDrawerLoading(false);
      }
    }
  }

  async function openDetail(id: string) {
    drawerControllerRef.current?.abort();
    const controller = new AbortController();
    drawerControllerRef.current = controller;
    setCurrentPackage(null);
    setDrawerMode("detail");
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      const nextPackage = await getUserFeaturePackage(id, { signal: controller.signal });
      if (drawerControllerRef.current === controller && !controller.signal.aborted)
        setCurrentPackage(nextPackage);
    } catch (cause) {
      if (!controller.signal.aborted) {
        message.error(cause instanceof Error ? cause.message : "应用包加载失败");
        setDrawerOpen(false);
      }
    } finally {
      if (drawerControllerRef.current === controller) {
        drawerControllerRef.current = null;
        setDrawerLoading(false);
      }
    }
  }

  async function submitPackage(payload: SaveUserFeaturePackageRequest) {
    if (mutationLockRef.current) return;
    if (!payload.code || !payload.name) {
      message.warning("编码和名称不能为空");
      return;
    }
    mutationLockRef.current = true;
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
      await refresh();
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : "应用包保存失败");
    } finally {
      mutationLockRef.current = false;
      setSaving(false);
    }
  }

  async function toggleStatus(row: UserFeaturePackageEntry) {
    if (mutationLockRef.current) return;
    const nextEnabled = !row.enabled;
    const confirmed = await bzConfirm({
      title: nextEnabled ? "启用应用包" : "停用应用包",
      content: `确认${nextEnabled ? "启用" : "停用"}：${row.name}？`,
      confirmText: nextEnabled ? "启用" : "停用",
      cancelText: "取消",
    });
    if (!confirmed) return;
    mutationLockRef.current = true;
    try {
      await updateUserFeaturePackageStatus(row.id, nextEnabled);
      message.success(nextEnabled ? "已启用" : "已停用");
      await refresh();
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : "应用包状态更新失败");
    } finally {
      mutationLockRef.current = false;
    }
  }

  async function removePackage(row: UserFeaturePackageEntry) {
    if (mutationLockRef.current) return;
    const confirmed = await bzConfirm({
      title: "删除应用包",
      content: `确认删除：${row.name}？`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    mutationLockRef.current = true;
    try {
      await deleteUserFeaturePackage(row.id);
      message.success("应用包已删除");
      await refresh();
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : "应用包删除失败");
    } finally {
      mutationLockRef.current = false;
    }
  }

  function closeDrawer() {
    drawerControllerRef.current?.abort();
    drawerControllerRef.current = null;
    setDrawerLoading(false);
    setDrawerOpen(false);
  }

  const columns = useMemo<Array<BzTableColumn<UserFeaturePackageEntry>>>(() => {
    const baseColumns: Array<BzTableColumn<UserFeaturePackageEntry>> = [
      {
        key: "code",
        title: "编码",
        width: 200,
        className: `${styles.codeColumn} is-sticky-left`,
        headerClassName: `${styles.codeColumn} is-sticky-left`,
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
        className: styles.descriptionColumn,
        headerClassName: styles.descriptionColumn,
        render: (row) => {
          const description = row.description || "-";
          return (
            <div className={styles.descriptionWrap}>
              <BzTooltip content={description}>
                <span className={styles.descriptionEllipsis}>{description}</span>
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
          <AdminSearchForm
            visible={queryPanelVisible}
            onSubmit={submit}
            onReset={reset}
          >
            <AdminSearchField label="关键词">
              <BzInput
                modelValue={draftFilters.keyword}
                placeholder="按编码或名称搜索"
                clearable
                onValueChange={(keyword) => setDraftFilters((filters) => ({ ...filters, keyword }))}
              />
            </AdminSearchField>
            <AdminSearchField label="状态">
              <BzSelect
                modelValue={draftFilters.enabled}
                placeholder="全部状态"
                clearable
                onValueChange={(enabled) =>
                  setDraftFilters((filters) => ({
                    ...filters,
                    enabled: (enabled || "") as "" | "true" | "false",
                  }))
                }
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
            </AdminSearchField>
          </AdminSearchForm>
        }
        businessActions={
          canEdit ? (
            <BzButton
              className={layoutStyles.toolbarPrimary}
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
            onRefresh={() => void refresh()}
          />
        }
        table={
          <>
            {error ? (
              <BzAlert
                title={error.message}
                type="error"
                closable={false}
              />
            ) : null}
            <BzTable
              columns={columns}
              data={rows}
              loading={loading}
              rowKey="id"
              emptyText="暂无应用包"
              size="small"
            />
          </>
        }
        footer={
          <AdminTablePagination
            total={page.totalElements}
            pageNo={pageNo}
            pageSize={pageSize}
            pageSizes={PAGE_SIZE_OPTIONS}
            onPageChange={setPageNo}
            onPageSizeChange={setPageSize}
          />
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
        onClose={closeDrawer}
        onSubmit={submitPackage}
      />
    </>
  );
}
