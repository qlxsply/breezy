"use client";

import { useDateTimePreferences } from "@admin/features/auth/public/session";
import { batchListDictionaryOptions as batchListDictOptions } from "@admin/features/dicts/public/dictionary-client";
import type { PublicDictionaryItem as DictItem } from "@admin/features/dicts/public/types";
import { usePermission } from "@admin/features/resources/permissions";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { buildDateTimeRangeSubmitValue, formatDateTime } from "@admin/shared/lib/formatter";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import { AdminDateTimeRangeField } from "@admin/shared/ui/admin/AdminDateTimeRangeField";
import entityStyles from "@admin/shared/ui/admin/AdminEntity.module.css";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import { AdminListPageTemplate } from "@admin/shared/ui/admin/AdminListPageTemplate";
import { AdminSearchField, AdminSearchForm } from "@admin/shared/ui/admin/AdminSearchForm";
import { AdminTablePagination } from "@admin/shared/ui/admin/AdminTablePagination";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import { BzAlert } from "@admin/shared/ui/bz/BzAlert";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import { BzEmpty } from "@admin/shared/ui/bz/BzEmpty";
import { BzInput } from "@admin/shared/ui/bz/BzInput";
import { BzOverflowTooltip } from "@admin/shared/ui/bz/BzOverflowTooltip";
import type { BzTableColumn } from "@admin/shared/ui/bz/BzTable";
import { BzTable } from "@admin/shared/ui/bz/BzTable";
import { BzTag } from "@admin/shared/ui/bz/BzTag";
import { useEffect, useMemo, useState } from "react";

import { pageLoginLogs } from "./api/client";
import styles from "./LoginLogsPage.module.css";
import type { LoginLogEntry } from "./model/types";
import { LOGIN_LOG_PERMISSIONS } from "./permissions";

type DictMeta = { label: string; tagType?: string | null };

function toDictMetaMap(items?: DictItem[]): Record<string, DictMeta> {
  const map: Record<string, DictMeta> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = {
      label: item.itemLabel || item.itemValue,
      tagType: item.tagType || undefined,
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

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const INITIAL_FILTERS = { account: "", startAt: "", endAt: "" };
type LoginLogFilters = typeof INITIAL_FILTERS;

export function LoginLogsPage() {
  const { dateTimePattern, timeZone } = useDateTimePreferences();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<LoginLogEntry | null>(null);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [loginEventMetaMap, setLoginEventMetaMap] = useState<Record<string, DictMeta>>({});
  const canView = usePermission(LOGIN_LOG_PERMISSIONS.view);
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
  } = useAdminPagedQuery<LoginLogEntry, LoginLogFilters>({
    initialFilters: INITIAL_FILTERS,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canView,
    normalizeFilters: (filters) => ({ ...filters, account: filters.account.trim() }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) => {
      const range = buildRequestRange(filters.startAt, filters.endAt);
      return pageLoginLogs(
        {
          userAccount: filters.account || undefined,
          startAt: range.startAt,
          endAt: range.endAt,
          page: { pageNo: targetPage, pageSize: targetSize },
        },
        { signal },
      );
    },
  });
  const rows = page.elements;

  useEffect(() => {
    const controller = new AbortController();
    void batchListDictOptions(["LOGIN_EVENT"], { signal: controller.signal })
      .then((result) => setLoginEventMetaMap(toDictMetaMap(result.LOGIN_EVENT)))
      .catch(() => undefined);
    return () => controller.abort();
  }, []);

  function openDetail(row: LoginLogEntry) {
    setDetail(row);
    setDetailOpen(true);
  }

  function closeDetail() {
    setDetailOpen(false);
    setDetail(null);
  }

  function getRowActions(row: LoginLogEntry): AdminActionItem[] {
    return [
      { key: `detail-${row.id}`, label: "详情", level: "default", onClick: () => openDetail(row) },
    ];
  }

  const columns = useMemo<Array<BzTableColumn<LoginLogEntry>>>(() => {
    const baseColumns: Array<BzTableColumn<LoginLogEntry>> = [
      {
        key: "username",
        title: "账号",
        width: 120,
        render: (row) => <>{row.username || "-"}</>,
      },
      {
        key: "eventType",
        title: "事件",
        width: 120,
        render: (row) => (
          <BzTag
            size="small"
            type={
              resolveTagType(loginEventMetaMap, row.eventType) as
                "info" | "warning" | "danger" | "success"
            }
          >
            {resolveLabel(loginEventMetaMap, row.eventType)}
          </BzTag>
        ),
      },
      {
        key: "success",
        title: "结果",
        width: 90,
        render: (row) => (
          <BzTag
            size="small"
            type={row.success ? "success" : "danger"}
          >
            {row.success ? "成功" : "失败"}
          </BzTag>
        ),
      },
      {
        key: "loginIp",
        title: "IP",
        width: 140,
        render: (row) => <>{row.loginIp || "-"}</>,
      },
      {
        key: "failureReason",
        title: "失败原因",
        minWidth: 180,
        render: (row) => {
          const text = row.failureReason || "-";
          return (
            <BzOverflowTooltip text={text}>
              <span className={styles.cellText}>{text}</span>
            </BzOverflowTooltip>
          );
        },
      },
      {
        key: "remark",
        title: "备注",
        minWidth: 220,
        render: (row) => {
          const text = row.remark || "-";
          return (
            <BzOverflowTooltip text={text}>
              <span className={styles.cellText}>{text}</span>
            </BzOverflowTooltip>
          );
        },
      },
      {
        key: "occurredAt",
        title: "时间",
        width: 180,
        render: (row) => <>{formatDateTime(row.occurredAt)}</>,
      },
    ];
    const actionsColumn = createAdminActionsColumn({ rows, getActions: getRowActions });
    return actionsColumn ? [...baseColumns, actionsColumn] : baseColumns;
  }, [loginEventMetaMap, rows]);

  return (
    <AdminListPageTemplate
      queryPanelVisible={queryPanelVisible}
      queryPanel={
        <AdminSearchForm
          visible={queryPanelVisible}
          onSubmit={submit}
          onReset={reset}
        >
          <AdminSearchField label="账号">
            <BzInput
              modelValue={draftFilters.account}
              placeholder="按账号搜索"
              clearable
              onValueChange={(account) => setDraftFilters((filters) => ({ ...filters, account }))}
            />
          </AdminSearchField>
          <AdminSearchField label="时间">
            <AdminDateTimeRangeField
              startValue={draftFilters.startAt}
              endValue={draftFilters.endAt}
              dateTimePattern={dateTimePattern}
              timeZone={timeZone}
              onRangeChange={({ start, end }) =>
                setDraftFilters((filters) => ({ ...filters, startAt: start, endAt: end }))
              }
            />
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
        !canView ? (
          <BzEmpty description="无权限查看登录日志" />
        ) : (
          <>
            {error ? (
              <BzAlert
                key={error.message}
                title={error.message}
                type="error"
                closable={false}
              />
            ) : null}
            <BzTable
              data={rows}
              columns={columns}
              rowKey="id"
              loading={loading}
              emptyText="暂无日志"
              size="small"
            />
          </>
        )
      }
      footer={
        canView ? (
          <AdminTablePagination
            total={page.totalElements}
            pageNo={pageNo}
            pageSize={pageSize}
            pageSizes={PAGE_SIZE_OPTIONS}
            onPageChange={setPageNo}
            onPageSizeChange={setPageSize}
          />
        ) : null
      }
      overlays={
        <AdminEntityDrawer
          open={detailOpen}
          title="登录日志详情"
          width="1180px"
          className={entityStyles.manageDrawer}
          onClose={closeDetail}
          footer={<BzButton onClick={closeDetail}>关闭</BzButton>}
        >
          {detail ? (
            <div className={entityStyles.shell}>
              <section className={entityStyles.section}>
                <div className={entityStyles.sectionHead}>
                  <div className={entityStyles.sectionTitle}>登录日志信息</div>
                </div>

                <div className={entityStyles.infoTableWrap}>
                  <table
                    className={entityStyles.infoTable}
                    aria-label="登录日志详情"
                  >
                    <tbody>
                      <tr>
                        <th>账号</th>
                        <td>{detail.username || "-"}</td>
                        <th>事件</th>
                        <td>
                          <BzTag
                            size="small"
                            type={
                              resolveTagType(loginEventMetaMap, detail.eventType) as
                                "info" | "warning" | "danger" | "success"
                            }
                          >
                            {resolveLabel(loginEventMetaMap, detail.eventType)}
                          </BzTag>
                        </td>
                        <th>结果</th>
                        <td>
                          <BzTag
                            size="small"
                            type={detail.success ? "success" : "danger"}
                          >
                            {detail.success ? "成功" : "失败"}
                          </BzTag>
                        </td>
                      </tr>
                      <tr>
                        <th>IP</th>
                        <td>
                          <span className={styles.mono}>{detail.loginIp || "-"}</span>
                        </td>
                        <th>用户ID</th>
                        <td>
                          <span className={styles.mono}>{detail.userId || "-"}</span>
                        </td>
                        <th>操作人ID</th>
                        <td>
                          <span className={styles.mono}>{detail.operatorId || "-"}</span>
                        </td>
                      </tr>
                      <tr>
                        <th>会话ID</th>
                        <td colSpan={5}>
                          <span className={styles.mono}>{detail.sessionId || "-"}</span>
                        </td>
                      </tr>
                      <tr>
                        <th>记录时间</th>
                        <td colSpan={5}>{formatDateTime(detail.occurredAt)}</td>
                      </tr>
                      <tr>
                        <th>失败原因</th>
                        <td colSpan={5}>
                          <pre className={styles.pre}>{detail.failureReason || "-"}</pre>
                        </td>
                      </tr>
                      <tr>
                        <th>备注</th>
                        <td colSpan={5}>
                          <pre className={styles.pre}>{detail.remark || "-"}</pre>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>
            </div>
          ) : null}
        </AdminEntityDrawer>
      }
    />
  );
}

function buildRequestRange(start: string, end: string): { startAt?: string; endAt?: string } {
  const range = buildDateTimeRangeSubmitValue(start, end);
  return {
    startAt: range.startTimestamp || undefined,
    endAt: range.endTimestamp || undefined,
  };
}
