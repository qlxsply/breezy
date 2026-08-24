"use client";

import { usePermission } from "@admin/features/resources/permissions";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { bzConfirm } from "@admin/shared/lib/feedback/confirm";
import { message } from "@admin/shared/lib/feedback/message";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import entityStyles from "@admin/shared/ui/admin/AdminEntity.module.css";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
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
  type BzTableColumn,
  BzTag,
} from "@admin/shared/ui/bz";
import { useEffect, useMemo, useRef, useState } from "react";

import {
  getUserFeatureApplication,
  pageUserFeatureApplications,
  updateUserFeatureApplicationStatus,
} from "./api/client";
import type { UserFeatureApplicationEntry, UserFeatureItemEntry } from "./model/types";
import { USER_FEATURE_APPLICATION_PERMISSIONS } from "./permissions";
import styles from "./UserFeatureApplicationsPage.module.css";

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const INITIAL_FILTERS = { keyword: "", enabled: "" as "" | "true" | "false" };

function resolveAppIconUrl(icon?: string | null): string | null {
  if (!icon) return null;
  return `/app-icons/${icon}.svg`;
}

export function UserFeatureApplicationsPage() {
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<UserFeatureApplicationEntry | null>(null);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const canView = usePermission(USER_FEATURE_APPLICATION_PERMISSIONS.view);
  const canToggle = usePermission(USER_FEATURE_APPLICATION_PERMISSIONS.edit);
  const detailControllerRef = useRef<AbortController | null>(null);
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
  } = useAdminPagedQuery<UserFeatureApplicationEntry, typeof INITIAL_FILTERS>({
    initialFilters: INITIAL_FILTERS,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canView,
    normalizeFilters: (filters) => ({ ...filters, keyword: filters.keyword.trim() }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) =>
      pageUserFeatureApplications(
        {
          keyword: filters.keyword || undefined,
          enabled: filters.enabled === "" ? undefined : filters.enabled === "true",
          page: { pageNo: targetPage, pageSize: targetSize },
        },
        { signal },
      ),
  });
  const rows = page.elements;

  useEffect(() => () => detailControllerRef.current?.abort(), []);

  function getRowActions(row: UserFeatureApplicationEntry): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      {
        key: `detail-${row.id}`,
        label: "详情",
        level: "default",
        onClick: () => openDetail(row.id),
      },
    ];
    if (canToggle)
      actions.push({
        key: `toggle-${row.id}`,
        label: row.enabled ? "停用" : "启用",
        level: row.enabled ? "warning" : "success",
        onClick: () => toggleStatus(row),
      });
    return actions;
  }

  async function openDetail(id: string) {
    detailControllerRef.current?.abort();
    const controller = new AbortController();
    detailControllerRef.current = controller;
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const nextDetail = await getUserFeatureApplication(id, { signal: controller.signal });
      if (detailControllerRef.current === controller && !controller.signal.aborted)
        setDetail(nextDetail);
    } catch (cause) {
      if (!controller.signal.aborted)
        message.error(cause instanceof Error ? cause.message : "应用详情加载失败");
    } finally {
      if (detailControllerRef.current === controller) {
        detailControllerRef.current = null;
        setDetailLoading(false);
      }
    }
  }

  function closeDetail() {
    detailControllerRef.current?.abort();
    detailControllerRef.current = null;
    setDetailLoading(false);
    setDetailOpen(false);
    setDetail(null);
  }

  async function toggleStatus(row: UserFeatureApplicationEntry) {
    if (!canToggle || mutationLockRef.current) return;
    const nextEnabled = !row.enabled;
    const confirmed = await bzConfirm({
      title: nextEnabled ? "启用应用" : "停用应用",
      content: `确认${nextEnabled ? "启用" : "停用"}：${row.name}？`,
      confirmText: nextEnabled ? "启用" : "停用",
      cancelText: "取消",
    });
    if (!confirmed) return;
    mutationLockRef.current = true;
    try {
      await updateUserFeatureApplicationStatus(row.id, nextEnabled);
      message.success(nextEnabled ? "已启用" : "已停用");
      await refresh();
      if (detailOpen && detail?.id === row.id) await openDetail(row.id);
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : "应用状态更新失败");
    } finally {
      mutationLockRef.current = false;
    }
  }

  const columns = useMemo<Array<BzTableColumn<UserFeatureApplicationEntry>>>(() => {
    const baseColumns: Array<BzTableColumn<UserFeatureApplicationEntry>> = [
      {
        key: "icon",
        title: "图标",
        width: 52,
        className: `${styles.iconColumn} is-sticky-left`,
        headerClassName: `${styles.iconColumn} is-sticky-left`,
        render: (row) => {
          const iconUrl = resolveAppIconUrl(row.icon);
          return (
            <div className={styles.iconCell}>
              {iconUrl ? (
                <img
                  src={iconUrl}
                  alt={row.name}
                  className={styles.iconImage}
                />
              ) : (
                <span className={styles.iconFallback}>-</span>
              )}
            </div>
          );
        },
      },
      {
        key: "code",
        title: "应用编码",
        width: 200,
        className: `${styles.codeColumn} is-sticky-left`,
        headerClassName: `${styles.codeColumn} is-sticky-left`,
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
              emptyText="暂无应用"
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

      <AdminEntityDrawer
        open={detailOpen}
        loading={detailLoading}
        title="应用详情"
        width="1180px"
        className={entityStyles.manageDrawer}
        onClose={closeDetail}
        footer={<BzButton onClick={closeDetail}>关闭</BzButton>}
      >
        {detail ? (
          <div className={entityStyles.shell}>
            <section className={entityStyles.section}>
              <div className={entityStyles.sectionHead}>
                <div className={entityStyles.sectionTitle}>应用信息</div>
              </div>
              <div className={entityStyles.infoTableWrap}>
                <table
                  className={entityStyles.infoTable}
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
                            className={styles.detailIcon}
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

            <section className={entityStyles.section}>
              <div className={entityStyles.sectionHead}>
                <div className={entityStyles.sectionTitle}>应用功能</div>
                <div className={entityStyles.sectionStat}>共 {detail.features.length} 项</div>
              </div>
              <div className={`${layoutStyles.tableSurface} ${styles.detailTable}`}>
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
