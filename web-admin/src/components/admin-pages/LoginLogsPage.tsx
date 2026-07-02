"use client";

import { pageLoginLogs } from "@admin/api/login-logs";
import { batchListDictOptions } from "@admin/api/dicts";
import { AdminDateTimeRangeField, buildAdminDateTimeRangeSubmitParams } from "@admin/components/admin/AdminDateTimeRangeField";
import { AdminActionBar } from "@admin/components/admin/AdminActionBar";
import { AdminDetailTable } from "@admin/components/admin/AdminDetailTable";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import { formatDateTime } from "@admin/core/formatter";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { DictItem } from "@admin/types/dict-admin";
import type { LoginLogEntry } from "@admin/types/login-log";
import type { PageResult } from "@admin/types/page";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzEmpty } from "../bz/BzEmpty";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOverflowTooltip } from "../bz/BzOverflowTooltip";
import { BzPagination } from "../bz/BzPagination";
import type { BzTableColumn } from "../bz/BzTable";
import { BzTable } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

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

  const canView = hasResourceCodeAccess("login-log-view");

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
    return [{ key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row) }];
  }

  const detailSections = useMemo(
    () =>
      detail
        ? [
            {
              title: "基础信息",
              fields: [
                { label: "账号", value: detail.username || "-" },
                {
                  label: "事件",
                  value: (
                    <BzTag
                      size="small"
                      type={
                        resolveTagType(loginEventMetaMap, detail.eventType) as
                          | "info"
                          | "warning"
                          | "danger"
                          | "success"
                      }
                    >
                      {resolveLabel(loginEventMetaMap, detail.eventType)}
                    </BzTag>
                  ),
                },
                {
                  label: "结果",
                  value: (
                    <BzTag size="small" type={detail.success ? "success" : "danger"}>
                      {detail.success ? "成功" : "失败"}
                    </BzTag>
                  ),
                },
                { label: "IP", value: <span className="admin-log-mono">{detail.loginIp || "-"}</span> },
                { label: "用户ID", value: <span className="admin-log-mono">{detail.userId || "-"}</span> },
                { label: "操作人ID", value: <span className="admin-log-mono">{detail.operatorId || "-"}</span> },
                {
                  label: "会话ID",
                  value: <span className="admin-log-mono">{detail.sessionId || "-"}</span>,
                  span: "full" as const,
                },
                { label: "记录时间", value: formatDateTime(detail.occurredAt) },
              ],
            },
            {
              title: "说明信息",
              fields: [
                {
                  label: "失败原因",
                  value: <pre className="admin-log-pre">{detail.failureReason || "-"}</pre>,
                  span: "full" as const,
                  multiline: true,
                },
                {
                  label: "备注",
                  value: <pre className="admin-log-pre">{detail.remark || "-"}</pre>,
                  span: "full" as const,
                  multiline: true,
                },
              ],
            },
          ]
        : [],
    [detail, loginEventMetaMap],
  );

  const columns = useMemo<Array<BzTableColumn<LoginLogEntry>>>(
    () => [
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
              resolveTagType(loginEventMetaMap, row.eventType) as "info" | "warning" | "danger" | "success"
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
      {
        key: "actions",
        title: "操作",
        width: 88,
        className: "is-fixed-right",
        render: (row) => <AdminActionBar actions={getRowActions(row)} />,
      },
    ],
    [loginEventMetaMap],
  );

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
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          {queryPanelVisible ? (
            <BzCard
              className="admin-panel admin-filter-card"
              shadow="never"
            >
              <div
                ref={queryCardRef}
                className={[
                  "admin-query-layout",
                  querySingleRow ? "is-single-row" : queryExpanded ? "is-expanded" : "is-collapsed",
                ].join(" ")}
              >
                <div className="admin-query-header">
                  <div className="admin-query-title">筛选条件</div>
                </div>
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
                        onRangeChange={({ start, end }) => {
                          setStartAtDraft(start);
                          setEndAtDraft(end);
                        }}
                      />
                    </div>
                  </BzFormItem>
                  <div className="admin-query-actions">
                    <BzButton className="admin-filter-secondary" nativeType="button" onClick={resetFilters}>
                      重置
                    </BzButton>
                    <BzButton className="admin-filter-primary" buttonType="primary" nativeType="button" onClick={applyFilters}>
                      搜索
                    </BzButton>
                    {!querySingleRow ? (
                      <button className="admin-filter-toggle" type="button" aria-expanded={queryExpanded} onClick={() => setQueryExpanded((v) => !v)}>
                        <span>{queryExpanded ? "收起" : "展开"}</span>
                        <i className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`} aria-hidden="true" />
                      </button>
                    ) : null}
                  </div>
                </form>
              </div>
            </BzCard>
          ) : null}

          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">登录日志</div>
                <div className="admin-table-tools">
                  <AdminTableTools
                    queryPanelVisible={queryPanelVisible}
                    onToggleQueryPanel={() => setQueryPanelVisible((v) => !v)}
                    onRefresh={() => reload()}
                  />
                </div>
              </div>
            }
          >
            {!canView ? (
              <BzEmpty description="无权限查看登录日志" />
            ) : (
              <>
                <div className="admin-table-surface">
                  <BzTable
                    data={rows}
                    columns={columns}
                    rowKey="id"
                    loading={loading}
                    emptyText="暂无日志"
                    size="small"
                  />
                </div>

                {page.totalElements > 0 ? (
                  <div className="dict-pagination-bar">
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
                ) : null}
              </>
            )}
          </BzCard>

          <AdminEntityDrawer
            open={detailOpen}
            title="登录日志详情"
            width="960px"
            onClose={closeDetail}
            footer={<BzButton onClick={closeDetail}>关闭</BzButton>}
          >
            {detail ? (
              <AdminDetailTable sections={detailSections} />
            ) : null}
          </AdminEntityDrawer>
        </div>
      </div>
    </div>
  );
}

function buildRequestRange(start: string, end: string): { startAt?: string; endAt?: string } {
  const range = buildAdminDateTimeRangeSubmitParams(start, end);
  return {
    startAt: range.startTimestamp || undefined,
    endAt: range.endTimestamp || undefined,
  };
}
