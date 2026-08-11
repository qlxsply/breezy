"use client";

import { pageExternalUsers, updateExternalUser } from "@admin/api/external-users";
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
  type BzTableColumn,
  BzTag,
} from "@admin/components/bz";
import {
  WebUserFeatureDrawer,
  type WebUserFeatureDrawerMode,
} from "@admin/components/web-users-admin/WebUserFeatureDrawer";
import { bzConfirm } from "@admin/core/confirm";
import { formatDateTime } from "@admin/core/formatter";
import { message } from "@admin/core/message";
import { useIsRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { ExternalUserEntry, ExternalUserStatus } from "@admin/types/external-user-admin";
import type { PageResult } from "@admin/types/page";
import { useEffect, useMemo, useState } from "react";

const pageSizeOptions = [10, 20, 30, 50, 100];

export function WebUsersAdminPage() {
  const permissionsLoaded = useIsRegistryLoaded();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<ExternalUserEntry[]>([]);
  const [page, setPage] = useState<PageResult<ExternalUserEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [statusDraft, setStatusDraft] = useState<"" | ExternalUserStatus>("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedStatus, setAppliedStatus] = useState<"" | ExternalUserStatus>("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<WebUserFeatureDrawerMode>("detail");
  const [drawerUserId, setDrawerUserId] = useState<string | null>(null);
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);
  const canView = hasResourceCodeAccess("web-user-manage-view");
  const canEdit = hasResourceCodeAccess("web-user-manage-edit");
  const canFeatureView = hasResourceCodeAccess("user-feature-user-view");
  const canFeatureSave = hasResourceCodeAccess("user-feature-user-edit");
  const canPackageView = hasResourceCodeAccess("user-feature-package-view");

  useEffect(() => {
    if (!permissionsLoaded) return;
    void reload();
  }, [permissionsLoaded, pageNo, pageSize, appliedKeyword, appliedStatus]);

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
      const result = await pageExternalUsers({
        keyword: appliedKeyword || undefined,
        status: appliedStatus || undefined,
        pageNo,
        pageSize,
      });
      setPage(result);
      setPageNo(result.pageNo || 1);
      setPageSize(result.pageSize || pageSize);
      setRows(result.elements);
    } finally {
      setLoading(false);
    }
  }

  function applyFilters() {
    setAppliedKeyword(keywordDraft.trim());
    setAppliedStatus(statusDraft);
    setPageNo(1);
  }

  function resetFilters() {
    setKeywordDraft("");
    setStatusDraft("");
    setAppliedKeyword("");
    setAppliedStatus("");
    setPageNo(1);
  }

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
        tone: "detail",
        handler: () => openDrawer(row.id, "detail"),
      },
    ];
    if (canFeatureView && canFeatureSave && canPackageView) {
      actions.push({
        key: `maintain-${row.id}`,
        label: "维护",
        tone: "edit",
        handler: () => openDrawer(row.id, "maintain"),
      });
    }
    if (canEdit && row.status !== "CANCELLED") {
      actions.push({
        key: `toggle-${row.id}`,
        label: row.status === "ACTIVE" ? "停用" : "启用",
        tone: row.status === "ACTIVE" ? "disable" : "enable",
        handler: () => toggleStatus(row),
      });
    }
    return actions;
  }

  async function toggleStatus(row: ExternalUserEntry) {
    if (!canEdit || row.status === "CANCELLED") return;
    const nextStatus: ExternalUserStatus = row.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    const confirmed = await bzConfirm({
      title: nextStatus === "ACTIVE" ? "启用用户" : "停用用户",
      content: `确认${nextStatus === "ACTIVE" ? "启用" : "停用"}：${row.account}？`,
      confirmText: nextStatus === "ACTIVE" ? "启用" : "停用",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await updateExternalUser(row.id, { status: nextStatus });
    message.success(nextStatus === "ACTIVE" ? "已启用" : "已停用");
    void reload();
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
  }, [canEdit, canFeatureView, canFeatureSave, rows]);

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
              onSubmit={(event) => {
                event.preventDefault();
                applyFilters();
              }}
            >
              <BzFormItem className="admin-query-field">
                <div className="admin-query-field__label">关键词</div>
                <div className="admin-query-field__control">
                  <BzInput
                    modelValue={keywordDraft}
                    placeholder="按账号或昵称搜索"
                    clearable
                    onValueChange={setKeywordDraft}
                    onKeyUp={(event) => event.key === "Enter" && applyFilters()}
                  />
                </div>
              </BzFormItem>
              <BzFormItem className="admin-query-field">
                <div className="admin-query-field__label">状态</div>
                <div className="admin-query-field__control">
                  <BzSelect
                    modelValue={statusDraft}
                    placeholder="全部状态"
                    clearable
                    onValueChange={(value) =>
                      setStatusDraft((value || "") as "" | ExternalUserStatus)
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
        queryTools={
          <AdminTableTools
            queryPanelVisible={queryPanelVisible}
            onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
            onRefresh={() => void reload()}
          />
        }
        table={
          <BzTable
            columns={columns}
            data={rows}
            loading={loading}
            emptyText="暂无用户"
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
