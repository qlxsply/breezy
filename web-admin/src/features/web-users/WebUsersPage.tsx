"use client";

import { usePermission } from "@admin/features/resources/permissions";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { bzConfirm } from "@admin/shared/lib/feedback/confirm";
import { message } from "@admin/shared/lib/feedback/message";
import { formatDateTime } from "@admin/shared/lib/formatter";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import { AdminListPageTemplate } from "@admin/shared/ui/admin/AdminListPageTemplate";
import { AdminSearchField, AdminSearchForm } from "@admin/shared/ui/admin/AdminSearchForm";
import { AdminTablePagination } from "@admin/shared/ui/admin/AdminTablePagination";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import {
  BzAlert,
  BzInput,
  BzOption,
  BzSelect,
  BzTable,
  type BzTableColumn,
  BzTag,
} from "@admin/shared/ui/bz";
import { useMemo, useRef, useState } from "react";

import { pageExternalUsers, updateExternalUser } from "./api/client";
import type { ExternalUserEntry, ExternalUserStatus } from "./model/types";
import { WEB_USER_PERMISSIONS } from "./permissions";
import { WebUserFeatureDrawer, type WebUserFeatureDrawerMode } from "./ui/WebUserFeatureDrawer";

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const INITIAL_FILTERS = {
  keyword: "",
  status: "" as "" | ExternalUserStatus,
};

export function WebUsersPage() {
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<WebUserFeatureDrawerMode>("detail");
  const [drawerUserId, setDrawerUserId] = useState<string | null>(null);
  const canView = usePermission(WEB_USER_PERMISSIONS.view);
  const canEdit = usePermission(WEB_USER_PERMISSIONS.edit);
  const canFeatureView = usePermission(WEB_USER_PERMISSIONS.featureView);
  const canFeatureSave = usePermission(WEB_USER_PERMISSIONS.featureEdit);
  const canPackageView = usePermission(WEB_USER_PERMISSIONS.packageView);
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
  } = useAdminPagedQuery<ExternalUserEntry, typeof INITIAL_FILTERS>({
    initialFilters: INITIAL_FILTERS,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canView,
    normalizeFilters: (filters) => ({ ...filters, keyword: filters.keyword.trim() }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) =>
      pageExternalUsers(
        {
          keyword: filters.keyword || undefined,
          status: filters.status || undefined,
          page: { pageNo: targetPage, pageSize: targetSize },
        },
        { signal },
      ),
  });
  const rows = page.elements;

  function openDrawer(userId: string, mode: WebUserFeatureDrawerMode) {
    setDrawerUserId(userId);
    setDrawerMode(mode);
    setDrawerOpen(true);
  }

  function getRowActions(row: ExternalUserEntry): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      {
        key: `detail-${row.id}`,
        label: "详情",
        level: "default",
        onClick: () => openDrawer(row.id, "detail"),
      },
    ];
    if (canFeatureView && canFeatureSave && canPackageView) {
      actions.push({
        key: `maintain-${row.id}`,
        label: "维护",
        level: "primary",
        onClick: () => openDrawer(row.id, "maintain"),
      });
    }
    if (canEdit) {
      actions.push({
        key: `toggle-${row.id}`,
        label: row.status === "ACTIVE" ? "停用" : "启用",
        level: row.status === "ACTIVE" ? "warning" : "success",
        disabled: row.status === "CANCELLED",
        onClick: () => toggleStatus(row),
      });
    }
    return actions;
  }

  async function toggleStatus(row: ExternalUserEntry) {
    if (!canEdit || row.status === "CANCELLED" || mutationLockRef.current) return;
    const nextStatus: ExternalUserStatus = row.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    const confirmed = await bzConfirm({
      title: nextStatus === "ACTIVE" ? "启用用户" : "停用用户",
      content: `确认${nextStatus === "ACTIVE" ? "启用" : "停用"}：${row.account}？`,
      confirmText: nextStatus === "ACTIVE" ? "启用" : "停用",
      cancelText: "取消",
    });
    if (!confirmed) return;
    mutationLockRef.current = true;
    try {
      await updateExternalUser(row.id, nextStatus);
      message.success(nextStatus === "ACTIVE" ? "已启用" : "已停用");
      await refresh();
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : "用户状态更新失败");
    } finally {
      mutationLockRef.current = false;
    }
  }

  const columns = useMemo<Array<BzTableColumn<ExternalUserEntry>>>(() => {
    const baseColumns: Array<BzTableColumn<ExternalUserEntry>> = [
      { key: "account", title: "账号", width: 200, render: (row) => <>{row.account}</> },
      {
        key: "nickname",
        title: "昵称",
        minWidth: 160,
        render: (row) => <>{row.nickname || "-"}</>,
      },
      {
        key: "status",
        title: "状态",
        width: 100,
        render: (row) => (
          <BzTag type={resolveStatusType(row.status)}>{resolveStatusLabel(row.status)}</BzTag>
        ),
      },
      {
        key: "lastLoginAt",
        title: "最近登录",
        width: 200,
        render: (row) => <>{formatDateTime(row.lastLoginAt) || "-"}</>,
      },
      {
        key: "createdAt",
        title: "创建时间",
        width: 200,
        render: (row) => <>{formatDateTime(row.createdAt) || "-"}</>,
      },
    ];
    const actionsColumn = createAdminActionsColumn({ rows, getActions: getRowActions });
    return actionsColumn ? [...baseColumns, actionsColumn] : baseColumns;
  }, [canEdit, canFeatureView, canFeatureSave, canPackageView, rows]);

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
                placeholder="按账号或昵称搜索"
                clearable
                onValueChange={(keyword) => setDraftFilters((filters) => ({ ...filters, keyword }))}
              />
            </AdminSearchField>
            <AdminSearchField label="状态">
              <BzSelect
                modelValue={draftFilters.status}
                placeholder="全部状态"
                clearable
                onValueChange={(status) =>
                  setDraftFilters((filters) => ({
                    ...filters,
                    status: (status || "") as "" | ExternalUserStatus,
                  }))
                }
              >
                <BzOption
                  label="启用"
                  value="ACTIVE"
                />
                <BzOption
                  label="停用"
                  value="DISABLED"
                />
                <BzOption
                  label="已注销"
                  value="CANCELLED"
                />
              </BzSelect>
            </AdminSearchField>
          </AdminSearchForm>
        }
        queryTools={
          <AdminTableTools
            queryPanelVisible={queryPanelVisible}
            onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
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
              emptyText="暂无用户"
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

      <WebUserFeatureDrawer
        open={drawerOpen}
        mode={drawerMode}
        userId={drawerUserId}
        canViewFeatures={canFeatureView}
        canManageFeatures={canFeatureSave}
        canManagePackages={canPackageView}
        onClose={() => setDrawerOpen(false)}
      />
    </>
  );
}

function resolveStatusLabel(status: ExternalUserStatus): string {
  if (status === "ACTIVE") return "启用";
  if (status === "DISABLED") return "停用";
  return "已注销";
}

function resolveStatusType(status: ExternalUserStatus): "success" | "danger" {
  return status === "ACTIVE" ? "success" : "danger";
}
