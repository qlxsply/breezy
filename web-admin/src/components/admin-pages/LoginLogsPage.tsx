"use client";

import { pageLoginLogs } from "@admin/api/login-logs";
import { useDateTimePreferences } from "@admin/features/auth/model/use-date-time-preferences";
import { batchListDictionaryOptions as batchListDictOptions } from "@admin/features/dicts/public/dictionary-client";
import type { DictionaryItem as DictItem } from "@admin/features/dicts/public/types";
import { usePermission } from "@admin/features/resources/model/resource-store";
import { useAdminQueryPanelLayout } from "@admin/shared/hooks/useAdminQueryPanelLayout";
import { buildDateTimeRangeSubmitValue, formatDateTime } from "@admin/shared/lib/formatter";
import type { PageResult } from "@admin/shared/types/pagination";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import { AdminDateTimeRangeField } from "@admin/shared/ui/admin/AdminDateTimeRangeField";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import { AdminListPageTemplate } from "@admin/shared/ui/admin/AdminListPageTemplate";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import { BzEmpty } from "@admin/shared/ui/bz/BzEmpty";
import { BzFormItem } from "@admin/shared/ui/bz/BzFormItem";
import { BzInput } from "@admin/shared/ui/bz/BzInput";
import { BzOverflowTooltip } from "@admin/shared/ui/bz/BzOverflowTooltip";
import { BzPagination } from "@admin/shared/ui/bz/BzPagination";
import type { BzTableColumn } from "@admin/shared/ui/bz/BzTable";
import { BzTable } from "@admin/shared/ui/bz/BzTable";
import { BzTag } from "@admin/shared/ui/bz/BzTag";
import type { LoginLogEntry } from "@admin/types/login-log";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

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

export function LoginLogsPage() {
  const { dateTimePattern, timeZone } = useDateTimePreferences();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<LoginLogEntry[]>([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<LoginLogEntry | null>(null);
  const [page, setPage] = useState<PageResult<LoginLogEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });

  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [accountDraft, setAccountDraft] = useState("");
  const [startAtDraft, setStartAtDraft] = useState("");
  const [endAtDraft, setEndAtDraft] = useState("");
  const [appliedAccount, setAppliedAccount] = useState("");
  const [appliedStartAt, setAppliedStartAt] = useState("");
  const [appliedEndAt, setAppliedEndAt] = useState("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const pageSizeOptions = [10, 20, 30, 50, 100];
  const [loginEventMetaMap, setLoginEventMetaMap] = useState<Record<string, DictMeta>>({});
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const canView = usePermission("login-log-view");

  const pageNoRef = useRef(pageNo);
  const pageSizeRef = useRef(pageSize);
  const appliedAccountRef = useRef(appliedAccount);
  const appliedStartAtRef = useRef(appliedStartAt);
  const appliedEndAtRef = useRef(appliedEndAt);
  pageNoRef.current = pageNo;
  pageSizeRef.current = pageSize;
  appliedAccountRef.current = appliedAccount;
  appliedStartAtRef.current = appliedStartAt;
  appliedEndAtRef.current = appliedEndAt;

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      const pn = pageNoRef.current;
      const ps = pageSizeRef.current;
      const range = buildRequestRange(appliedStartAtRef.current, appliedEndAtRef.current);
      let result = await pageLoginLogs({
        userAccount: appliedAccountRef.current || undefined,
        startAt: range.startAt,
        endAt: range.endAt,
        page: { pageNo: pn, pageSize: ps },
      });
      if (result.totalElements > 0 && pn > Math.max(1, result.totalPages)) {
        setPageNo(Math.max(1, result.totalPages));
        result = await pageLoginLogs({
          userAccount: appliedAccountRef.current || undefined,
          startAt: range.startAt,
          endAt: range.endAt,
          page: { pageNo: Math.max(1, result.totalPages), pageSize: ps },
        });
      }
      setPage(result);
      setPageNo(result.pageNo || 1);
      setPageSize(result.pageSize || ps);
      setRows(result.elements);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadDictionaries = useCallback(async () => {
    try {
      const result = await batchListDictOptions(["LOGIN_EVENT"]);
      setLoginEventMetaMap(toDictMetaMap(result.LOGIN_EVENT));
    } catch {
      setLoginEventMetaMap({});
    }
  }, []);

  useEffect(() => {
    loadDictionaries();
  }, [loadDictionaries]);

  useEffect(() => {
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
    reload();
  }, [pageNo, pageSize, appliedAccount, appliedStartAt, appliedEndAt, canView, reload]);

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
              <span className="cell-text">{text}</span>
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
              <span className="cell-text">{text}</span>
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

  async function applyFilters() {
    setAppliedAccount(accountDraft.trim());
    setAppliedStartAt(startAtDraft);
    setAppliedEndAt(endAtDraft);
    setPageNo(1);
  }

  async function resetFilters() {
    setAccountDraft("");
    setStartAtDraft("");
    setEndAtDraft("");
    setPageNo(1);
    setAppliedAccount("");
    setAppliedStartAt("");
    setAppliedEndAt("");
  }

  return (
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
              <div className="admin-query-field__label">账号</div>
              <div className="admin-query-field__control">
                <BzInput
                  modelValue={accountDraft}
                  placeholder="按账号搜索"
                  clearable
                  onValueChange={setAccountDraft}
                  onKeyUp={(e) => {
                    if (e.key === "Enter") applyFilters();
                  }}
                />
              </div>
            </BzFormItem>
            <BzFormItem className="admin-query-field">
              <div className="admin-query-field__label">时间</div>
              <div className="admin-query-field__control">
                <AdminDateTimeRangeField
                  startValue={startAtDraft}
                  endValue={endAtDraft}
                  dateTimePattern={dateTimePattern}
                  timeZone={timeZone}
                  onRangeChange={({ start, end }) => {
                    setStartAtDraft(start);
                    setEndAtDraft(end);
                  }}
                />
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
                  onClick={() => setQueryExpanded((v) => !v)}
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
          onRefresh={() => reload()}
        />
      }
      table={
        !canView ? (
          <BzEmpty description="无权限查看登录日志" />
        ) : (
          <BzTable
            data={rows}
            columns={columns}
            rowKey="id"
            loading={loading}
            emptyText="暂无日志"
            size="small"
          />
        )
      }
      footer={
        canView && page.totalElements > 0 ? (
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
      overlays={
        <AdminEntityDrawer
          open={detailOpen}
          title="登录日志详情"
          width="1180px"
          className="admin-entity-manage-drawer"
          onClose={closeDetail}
          footer={<BzButton onClick={closeDetail}>关闭</BzButton>}
        >
          {detail ? (
            <div className="admin-entity-shell">
              <section className="admin-entity-section">
                <div className="admin-entity-section__head">
                  <div className="admin-entity-section__title">登录日志信息</div>
                </div>

                <div className="admin-info-table-wrap">
                  <table
                    className="admin-info-table"
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
                          <span className="admin-log-mono">{detail.loginIp || "-"}</span>
                        </td>
                        <th>用户ID</th>
                        <td>
                          <span className="admin-log-mono">{detail.userId || "-"}</span>
                        </td>
                        <th>操作人ID</th>
                        <td>
                          <span className="admin-log-mono">{detail.operatorId || "-"}</span>
                        </td>
                      </tr>
                      <tr>
                        <th>会话ID</th>
                        <td colSpan={5}>
                          <span className="admin-log-mono">{detail.sessionId || "-"}</span>
                        </td>
                      </tr>
                      <tr>
                        <th>记录时间</th>
                        <td colSpan={5}>{formatDateTime(detail.occurredAt)}</td>
                      </tr>
                      <tr>
                        <th>失败原因</th>
                        <td colSpan={5}>
                          <pre className="admin-log-pre">{detail.failureReason || "-"}</pre>
                        </td>
                      </tr>
                      <tr>
                        <th>备注</th>
                        <td colSpan={5}>
                          <pre className="admin-log-pre">{detail.remark || "-"}</pre>
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
