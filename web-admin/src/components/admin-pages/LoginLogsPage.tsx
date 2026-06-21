"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { pageLoginLogs } from "@admin/api/login-logs";
import { dateTimeInputToEpochMillisString, dateTimeInputToNextMinuteEpochMillisString, formatDateTime } from "@admin/core/formatter";
import { hasResourceCodeAccess } from "@admin/registry/permissions.registry";
import type { LoginLogEntry } from "@admin/types/login-log";
import type { PageResult } from "@admin/types/page";
import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzDatePicker } from "../bz/BzDatePicker";
import { BzEmpty } from "../bz/BzEmpty";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
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
        pageSize: pageSize,
        numberOfElements: 0,
        totalPages: 0,
        totalElements: 0,
        elements: [],
      });
      return;
    }
    reload();
  }, [pageNo, pageSize, appliedAccount, appliedStartAt, appliedEndAt, canView, reload]);

  const totalPages = Math.max(1, page.totalPages || 1);
  const isFirstPage = pageNo <= 1;
  const isLastPage = pageNo >= totalPages;
  const pageTokens = useMemo(() => {
    const total = totalPages;
    const current = Math.min(Math.max(pageNo, 1), total);
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", total];
    if (current >= total - 3) return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
    return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
  }, [totalPages, pageNo]);

  const columns = useMemo<Array<BzTableColumn<LoginLogEntry>>>(() => [
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
        <BzTag size="small" type={row.success ? "success" : "danger"}>
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
      render: (row) => <div className="ua">{row.failureReason || "-"}</div>,
    },
    {
      key: "remark",
      title: "备注",
      minWidth: 220,
      render: (row) => <div className="msg">{row.remark || "-"}</div>,
    },
    {
      key: "occurredAt",
      title: "时间",
      width: 180,
      render: (row) => <>{formatDateTime(row.occurredAt)}</>,
    },
  ], []);

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

  function goToPage(target: number) {
    const next = Math.min(Math.max(target, 1), totalPages);
    if (next === pageNo) return;
    setPageNo(next);
  }

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          {queryPanelVisible ? (
            <BzCard className="admin-panel admin-filter-card" shadow="never">
              <BzForm className="admin-filter-form" onSubmit={(e) => { e.preventDefault(); applyFilters(); }}>
                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">账号</div>
                    <div className="admin-filter-control">
                      <BzInput
                        modelValue={accountDraft}
                        placeholder="按账号搜索"
                        clearable
                        onValueChange={setAccountDraft}
                        onKeyUp={(e) => { if (e.key === "Enter") applyFilters(); }}
                      />
                    </div>
                  </div>
                </BzFormItem>
                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">开始时间</div>
                    <div className="admin-filter-control">
                      <BzDatePicker
                        modelValue={startAtDraft}
                        type="datetime"
                        placeholder="开始时间"
                        clearable
                        onValueChange={setStartAtDraft}
                      />
                    </div>
                  </div>
                </BzFormItem>
                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">结束时间</div>
                    <div className="admin-filter-control">
                      <BzDatePicker
                        modelValue={endAtDraft}
                        type="datetime"
                        placeholder="结束时间"
                        clearable
                        onValueChange={setEndAtDraft}
                      />
                    </div>
                  </div>
                </BzFormItem>
                <div className="admin-filter-actions">
                  <BzButton className="admin-filter-secondary" onClick={resetFilters}>重置</BzButton>
                  <BzButton className="admin-filter-primary" buttonType="primary" nativeType="submit">搜索</BzButton>
                  <div className="admin-filter-toggle-placeholder" aria-hidden="true" />
                </div>
              </BzForm>
            </BzCard>
          ) : null}

          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">登录日志</div>
                <div className="admin-table-tools">
                  <button
                    className={`admin-vben-circle-button${queryPanelVisible ? " is-active" : ""}`}
                    type="button"
                    title={queryPanelVisible ? "关闭搜索框" : "打开搜索框"}
                    onClick={() => setQueryPanelVisible((v) => !v)}
                  >
                    <i className="admin-vben-circle-button__icon admin-vben-circle-button__icon--search" aria-hidden="true" />
                  </button>
                  <button
                    className="admin-vben-circle-button"
                    type="button"
                    title="刷新列表"
                    onClick={() => reload()}
                  >
                    <i className="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh" aria-hidden="true" />
                  </button>
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
                      <label className="dict-page-size">
                        <select
                          className="dict-page-size__select"
                          value={pageSize}
                          onChange={(e) => { setPageSize(Number(e.target.value)); setPageNo(1); }}
                        >
                          {pageSizeOptions.map((s) => (
                            <option key={s} value={s}>{s}条/页</option>
                          ))}
                        </select>
                      </label>
                      <div className="dict-page-list">
                        <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isFirstPage} onClick={() => goToPage(1)}>
                          <span aria-hidden="true">|&lt;</span>
                        </button>
                        <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isFirstPage} onClick={() => goToPage(pageNo - 1)}>
                          <span aria-hidden="true">&lt;</span>
                        </button>
                        {pageTokens.map((token, i) =>
                          typeof token === "number" ? (
                            <button
                              key={i}
                              className={`dict-page-btn${token === pageNo ? " is-active" : ""}`}
                              type="button"
                              onClick={() => goToPage(token)}
                            >
                              {token}
                            </button>
                          ) : (
                            <span key={i} className="dict-page-ellipsis">...</span>
                          ),
                        )}
                        <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isLastPage} onClick={() => goToPage(pageNo + 1)}>
                          <span aria-hidden="true">&gt;</span>
                        </button>
                        <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isLastPage} onClick={() => goToPage(totalPages)}>
                          <span aria-hidden="true">&gt;|</span>
                        </button>
                      </div>
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
