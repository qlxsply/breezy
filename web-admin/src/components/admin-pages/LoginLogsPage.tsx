"use client";

import { pageLoginLogs } from "@admin/api/login-logs";
import {
  dateTimeInputToEpochMillisString,
  dateTimeInputToNextMinuteEpochMillisString,
  formatDateTime,
} from "@admin/core/formatter";
import { hasResourceCodeAccess } from "@admin/core/registry/permissions-registry";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import type { LoginLogEntry } from "@admin/types/login-log";
import type { PageResult } from "@admin/types/page";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzDatePicker } from "../bz/BzDatePicker";
import { BzEmpty } from "../bz/BzEmpty";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzPagination } from "../bz/BzPagination";
import type { BzTableColumn } from "../bz/BzTable";
import { BzTable } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

export function LoginLogsPage() {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<LoginLogEntry[]>([]);
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
      let result = await pageLoginLogs({
        userAccount: appliedAccountRef.current || undefined,
        startAt: toInstant(appliedStartAtRef.current),
        endAt: toEndExclusive(appliedEndAtRef.current),
        page: { pageNo: pn, pageSize: ps },
      });
      if (result.totalElements > 0 && pn > Math.max(1, result.totalPages)) {
        setPageNo(Math.max(1, result.totalPages));
        result = await pageLoginLogs({
          userAccount: appliedAccountRef.current || undefined,
          startAt: toInstant(appliedStartAtRef.current),
          endAt: toEndExclusive(appliedEndAtRef.current),
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
        width: 100,
        render: (row) => <BzTag size="small">{row.eventType}</BzTag>,
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
        render: (row) => <>{row.failureReason || "-"}</>,
      },
      {
        key: "remark",
        title: "备注",
        minWidth: 220,
        render: (row) => <>{row.remark || "-"}</>,
      },
      {
        key: "occurredAt",
        title: "时间",
        width: 180,
        render: (row) => <>{formatDateTime(row.occurredAt)}</>,
      },
    ],
    [],
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
                    <div className="admin-query-field__label">开始时间</div>
                    <div className="admin-query-field__control">
                      <BzDatePicker modelValue={startAtDraft} type="datetime" placeholder="开始时间" clearable onValueChange={setStartAtDraft} />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">结束时间</div>
                    <div className="admin-query-field__control">
                      <BzDatePicker modelValue={endAtDraft} type="datetime" placeholder="结束时间" clearable onValueChange={setEndAtDraft} />
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
        </div>
      </div>
    </div>
  );
}

function toInstant(value: string): string | undefined {
  return dateTimeInputToEpochMillisString(value) ?? undefined;
}

function toEndExclusive(value: string): string | undefined {
  return dateTimeInputToNextMinuteEpochMillisString(value) ?? undefined;
}
