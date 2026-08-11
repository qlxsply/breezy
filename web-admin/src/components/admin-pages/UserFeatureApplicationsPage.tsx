"use client";

import {
  getUserFeatureApplication,
  pageUserFeatureApplications,
  updateUserFeatureApplicationStatus,
} from "@admin/api/user-features";
import { createAdminActionsColumn } from "@admin/components/admin/admin-actions-column";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
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
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { useIsRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { PageResult } from "@admin/types/page";
import type { UserFeatureApplicationEntry, UserFeatureItemEntry } from "@admin/types/user-feature";
import { useEffect, useMemo, useRef, useState } from "react";

const pageSizeOptions = [10, 20, 30, 50, 100];

function resolveAppIconUrl(icon?: string | null): string | null {
  if (!icon) return null;
  return `/app-icons/${icon}.svg`;
}

export function UserFeatureApplicationsPage() {
  const permissionsLoaded = useIsRegistryLoaded();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<UserFeatureApplicationEntry[]>([]);
  const [page, setPage] = useState<PageResult<UserFeatureApplicationEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<UserFeatureApplicationEntry | null>(null);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [enabledDraft, setEnabledDraft] = useState<"" | "true" | "false">("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedEnabled, setAppliedEnabled] = useState<"" | "true" | "false">("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);
  const loadedRef = useRef(false);

  const canView = hasResourceCodeAccess("user-feature-application-view");
  const canToggle = hasResourceCodeAccess("user-feature-application-edit");

  useEffect(() => {
    if (loadedRef.current) return;
    if (!permissionsLoaded) return;
    loadedRef.current = true;
    void reload();
  }, [permissionsLoaded]);

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
      const result = await pageUserFeatureApplications({
        keyword: appliedKeyword || undefined,
        enabled: appliedEnabled === "" ? undefined : appliedEnabled === "true",
        page: { pageNo, pageSize },
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
    if (loadedRef.current) void reload();
  }, [pageNo, pageSize, appliedKeyword, appliedEnabled]);

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

  function getRowActions(row: UserFeatureApplicationEntry): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      { key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) },
    ];
    if (canToggle)
      actions.push({
        key: `toggle-${row.id}`,
        label: row.enabled ? "停用" : "启用",
        tone: row.enabled ? "disable" : "enable",
        handler: () => toggleStatus(row),
      });
    return actions;
  }

  async function openDetail(id: string) {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      setDetail(await getUserFeatureApplication(id));
    } finally {
      setDetailLoading(false);
    }
  }

  async function toggleStatus(row: UserFeatureApplicationEntry) {
    if (!canToggle) return;
    const nextEnabled = !row.enabled;
    const confirmed = await bzConfirm({
      title: nextEnabled ? "启用应用" : "停用应用",
      content: `确认${nextEnabled ? "启用" : "停用"}：${row.name}？`,
      confirmText: nextEnabled ? "启用" : "停用",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await updateUserFeatureApplicationStatus(row.id, nextEnabled);
    message.success(nextEnabled ? "已启用" : "已停用");
    await reload();
    if (detailOpen && detail?.id === row.id) setDetail(await getUserFeatureApplication(row.id));
  }

  const columns = useMemo<Array<BzTableColumn<UserFeatureApplicationEntry>>>(() => {
    const baseColumns: Array<BzTableColumn<UserFeatureApplicationEntry>> = [
      {
        key: "icon",
        title: "图标",
        width: 80,
        className: "admin-freeze-col--feature-application-icon is-sticky-left",
        headerClassName: "admin-freeze-col--feature-application-icon is-sticky-left",
        render: (row) => {
          const iconUrl = resolveAppIconUrl(row.icon);
          return (
            <div className="application-icon-cell">
              {iconUrl ? (
                <img
                  src={iconUrl}
                  alt={row.name}
                  className="application-icon-cell__image"
                />
              ) : (
                <span className="application-icon-cell__fallback">-</span>
              )}
            </div>
          );
        },
      },
      {
        key: "code",
        title: "应用编码",
        minWidth: 160,
        className: "admin-freeze-col--feature-application-code is-sticky-left",
        headerClassName: "admin-freeze-col--feature-application-code is-sticky-left",
        render: (row) => <>{row.code}</>,
      },
      { key: "name", title: "名称", minWidth: 160, render: (row) => <>{row.name}</> },
      {
        key: "routePath",
        title: "路由",
        minWidth: 160,
        render: (row) => <>{row.routePath || "-"}</>,
      },
      {
        key: "componentPath",
        title: "组件",
        minWidth: 220,
        render: (row) => <>{row.componentPath || "-"}</>,
      },
      {
        key: "enabled",
        title: "状态",
        width: 100,
        render: (row) => (
          <BzTag type={row.enabled ? "success" : "danger"}>{row.enabled ? "启用" : "停用"}</BzTag>
        ),
      },
      { key: "featureCount", title: "功能数", width: 90, render: (row) => <>{row.featureCount}</> },
      {
        key: "permissionBindingCount",
        title: "权限绑定",
        width: 100,
        render: (row) => <>{row.permissionBindingCount}</>,
      },
    ];
    const actionsColumn = createAdminActionsColumn({ rows, getActions: getRowActions });
    return actionsColumn ? [...baseColumns, actionsColumn] : baseColumns;
  }, [canToggle, rows]);

  const featureColumns = useMemo(
    () =>
      [
        {
          key: "code",
          title: "功能编码",
          minWidth: 180,
          render: (row: UserFeatureItemEntry) => <>{row.code || "-"}</>,
        },
        {
          key: "name",
          title: "名称",
          minWidth: 160,
          render: (row: UserFeatureItemEntry) => <>{row.name || "-"}</>,
        },
        {
          key: "description",
          title: "描述",
          minWidth: 220,
          render: (row: UserFeatureItemEntry) => <>{row.description || "-"}</>,
        },
        {
          key: "enabled",
          title: "状态",
          width: 100,
          render: (row: UserFeatureItemEntry) => (
            <BzTag type={row.enabled ? "success" : "warning"}>
              {row.enabled ? "启用" : "停用"}
            </BzTag>
          ),
        },
        {
          key: "permissionCodes",
          title: "权限码",
          minWidth: 220,
          render: (row: UserFeatureItemEntry) => (
            <>{row.permissionCodes.length ? row.permissionCodes.join(", ") : "-"}</>
          ),
        },
      ] as Array<BzTableColumn<UserFeatureItemEntry>>,
    [],
  );

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
            emptyText="暂无应用"
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

      <AdminEntityDrawer
        open={detailOpen}
        loading={detailLoading}
        title="应用详情"
        width="1180px"
        className="role-manage-drawer"
        onClose={() => setDetailOpen(false)}
        footer={<BzButton onClick={() => setDetailOpen(false)}>关闭</BzButton>}
      >
        {detail ? (
          <div className="role-manage-shell">
            <section className="role-manage-section">
              <div className="role-manage-section__head">
                <div className="role-manage-section__title">应用信息</div>
              </div>
              <div className="role-info-table-wrap">
                <table
                  className="role-info-table"
                  aria-label="应用详情"
                >
                  <tbody>
                    <tr>
                      <th>应用编码</th>
                      <td>{detail.code}</td>
                      <th>名称</th>
                      <td>{detail.name}</td>
                      <th>状态</th>
                      <td>{detail.enabled ? "启用" : "停用"}</td>
                    </tr>
                    <tr>
                      <th>路由</th>
                      <td>{detail.routePath || "-"}</td>
                      <th>组件</th>
                      <td>{detail.componentPath || "-"}</td>
                      <th>图标</th>
                      <td>
                        {resolveAppIconUrl(detail.icon) ? (
                          <img
                            src={resolveAppIconUrl(detail.icon)!}
                            alt={detail.name}
                            className="application-detail-icon"
                          />
                        ) : null}{" "}
                        {detail.icon || "-"}
                      </td>
                    </tr>
                    <tr>
                      <th>描述</th>
                      <td colSpan={5}>{detail.description || "-"}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section className="role-manage-section">
              <div className="role-manage-section__head">
                <div className="role-manage-section__title">应用功能</div>
                <div className="role-manage-section__stat">共 {detail.features.length} 项</div>
              </div>
              <div className="admin-table-surface user-feature-application-detail-table">
                <BzTable
                  columns={featureColumns}
                  data={detail.features}
                  rowKey="id"
                  size="small"
                  emptyText="暂无功能"
                />
              </div>
            </section>
          </div>
        ) : null}
      </AdminEntityDrawer>
    </>
  );
}
