"use client";

import { getAuditLog, pageAuditLogs } from "@admin/api/audit-logs";
import { batchListDictOptions } from "@admin/api/dicts";
import {
  dateTimeInputToEpochMillisString,
  dateTimeInputToNextMinuteEpochMillisString,
  formatDateTime,
} from "@admin/core/formatter";
import { hasResourceCodeAccess } from "@admin/core/registry/permissions-registry";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { AuditLevel, AuditLogEntry } from "@admin/types/audit-log";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { AdminActionBar } from "../admin/AdminActionBar";
import { AdminEntityDrawer } from "../admin/AdminEntityDrawer";
import { AdminTableTools } from "../admin/AdminTableTools";
import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzDatePicker } from "../bz/BzDatePicker";
import { BzEmpty } from "../bz/BzEmpty";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOption } from "../bz/BzOption";
import { BzPagination } from "../bz/BzPagination";
import { BzSelect } from "../bz/BzSelect";
import type { BzTableColumn } from "../bz/BzTable";
import { BzTable } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

type DictMeta = { label: string; tagType?: string | null };

const AUDIT_DICT_CODES = [
  "AUDIT_RESOURCE",
  "AUDIT_ACTION",
  "AUDIT_LEVEL",
  "API_METHOD",
  "API_PROTOCOL",
  "USER_TYPE",
] as const;

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

function toOptions(metaMap: Record<string, DictMeta>): Array<{ label: string; value: string }> {
  return Object.entries(metaMap).map(([value, meta]) => ({ value, label: meta.label }));
}

function resolveLabel(metaMap: Record<string, DictMeta>, value?: string | null): string {
  if (!value) return "-";
  return metaMap[value]?.label || value;
}

function resolveTagType(metaMap: Record<string, DictMeta>, value?: string | null): string {
  if (!value) return "info";
  return metaMap[value]?.tagType || "info";
}

export function AuditLogsPage() {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<AuditLogEntry[]>([]);
  const [page, setPage] = useState<PageResult<AuditLogEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<AuditLogEntry | null>(null);

  const [queryPanelVisible, setQueryPanelVisible] = useState(false);

  const [traceIdDraft, setTraceIdDraft] = useState("");
  const [operatorUsernameDraft, setOperatorUsernameDraft] = useState("");
  const [auditResourceDraft, setAuditResourceDraft] = useState("");
  const [auditActionDraft, setAuditActionDraft] = useState("");
  const [auditLevelDraft, setAuditLevelDraft] = useState<"" | AuditLevel>("");
  const [successDraft, setSuccessDraft] = useState<"" | "true" | "false">("");
  const [startAtDraft, setStartAtDraft] = useState("");
  const [endAtDraft, setEndAtDraft] = useState("");

  const [appliedTraceId, setAppliedTraceId] = useState("");
  const [appliedOperatorUsername, setAppliedOperatorUsername] = useState("");
  const [appliedAuditResource, setAppliedAuditResource] = useState("");
  const [appliedAuditAction, setAppliedAuditAction] = useState("");
  const [appliedAuditLevel, setAppliedAuditLevel] = useState<"" | AuditLevel>("");
  const [appliedSuccess, setAppliedSuccess] = useState<"" | "true" | "false">("");
  const [appliedStartAt, setAppliedStartAt] = useState("");
  const [appliedEndAt, setAppliedEndAt] = useState("");

  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const pageSizeOptions = [10, 20, 30, 50, 100];
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const [auditResourceMetaMap, setAuditResourceMetaMap] = useState<Record<string, DictMeta>>({});
  const [auditActionMetaMap, setAuditActionMetaMap] = useState<Record<string, DictMeta>>({});
  const [auditLevelMetaMap, setAuditLevelMetaMap] = useState<Record<string, DictMeta>>({});
  const [apiMethodMetaMap, setApiMethodMetaMap] = useState<Record<string, DictMeta>>({});
  const [apiProtocolMetaMap, setApiProtocolMetaMap] = useState<Record<string, DictMeta>>({});
  const [userTypeMetaMap, setUserTypeMetaMap] = useState<Record<string, DictMeta>>({});

  const auditResourceOptions = useMemo(
    () => toOptions(auditResourceMetaMap),
    [auditResourceMetaMap],
  );
  const auditActionOptions = useMemo(() => toOptions(auditActionMetaMap), [auditActionMetaMap]);
  const auditLevelOptions = useMemo(() => toOptions(auditLevelMetaMap), [auditLevelMetaMap]);

  const canView = hasResourceCodeAccess("audit-log-view");

  const pageNoRef = useRef(pageNo);
  const pageSizeRef = useRef(pageSize);
  const appliedTraceIdRef = useRef(appliedTraceId);
  const appliedOperatorUsernameRef = useRef(appliedOperatorUsername);
  const appliedAuditResourceRef = useRef(appliedAuditResource);
  const appliedAuditActionRef = useRef(appliedAuditAction);
  const appliedAuditLevelRef = useRef(appliedAuditLevel);
  const appliedSuccessRef = useRef(appliedSuccess);
  const appliedStartAtRef = useRef(appliedStartAt);
  const appliedEndAtRef = useRef(appliedEndAt);
  pageNoRef.current = pageNo;
  pageSizeRef.current = pageSize;
  appliedTraceIdRef.current = appliedTraceId;
  appliedOperatorUsernameRef.current = appliedOperatorUsername;
  appliedAuditResourceRef.current = appliedAuditResource;
  appliedAuditActionRef.current = appliedAuditAction;
  appliedAuditLevelRef.current = appliedAuditLevel;
  appliedSuccessRef.current = appliedSuccess;
  appliedStartAtRef.current = appliedStartAt;
  appliedEndAtRef.current = appliedEndAt;

  const loadDictionaries = useCallback(async () => {
    try {
      const result = await batchListDictOptions([...AUDIT_DICT_CODES]);
      setAuditResourceMetaMap(toDictMetaMap(result.AUDIT_RESOURCE));
      setAuditActionMetaMap(toDictMetaMap(result.AUDIT_ACTION));
      setAuditLevelMetaMap(toDictMetaMap(result.AUDIT_LEVEL));
      setApiMethodMetaMap(toDictMetaMap(result.API_METHOD));
      setApiProtocolMetaMap(toDictMetaMap(result.API_PROTOCOL));
      setUserTypeMetaMap(toDictMetaMap(result.USER_TYPE));
    } catch {
      setAuditResourceMetaMap({});
      setAuditActionMetaMap({});
      setAuditLevelMetaMap({});
      setApiMethodMetaMap({});
      setApiProtocolMetaMap({});
      setUserTypeMetaMap({});
    }
  }, []);

  const reload = useCallback(async () => {
    if (!canView) {
      setRows([]);
      setPage({
        pageNo: 1,
        pageSize: pageSizeRef.current,
        numberOfElements: 0,
        totalPages: 0,
        totalElements: 0,
        elements: [],
      });
      return;
    }

    setLoading(true);
    try {
      const pn = pageNoRef.current;
      const ps = pageSizeRef.current;
      const successFilter =
        appliedSuccessRef.current === "" ? undefined : appliedSuccessRef.current === "true";
      const startAtFilter = toInstant(appliedStartAtRef.current);
      const endAtFilter = toEndExclusive(appliedEndAtRef.current);

      let result = await pageAuditLogs({
        traceId: appliedTraceIdRef.current || undefined,
        operatorUsername: appliedOperatorUsernameRef.current || undefined,
        auditResource: appliedAuditResourceRef.current || undefined,
        auditAction: appliedAuditActionRef.current || undefined,
        auditLevel: appliedAuditLevelRef.current || undefined,
        success: successFilter,
        startAt: startAtFilter,
        endAt: endAtFilter,
        page: { pageNo: pn, pageSize: ps },
      });

      if (result.totalElements > 0 && pn > Math.max(1, result.totalPages)) {
        setPageNo(Math.max(1, result.totalPages));
        result = await pageAuditLogs({
          traceId: appliedTraceIdRef.current || undefined,
          operatorUsername: appliedOperatorUsernameRef.current || undefined,
          auditResource: appliedAuditResourceRef.current || undefined,
          auditAction: appliedAuditActionRef.current || undefined,
          auditLevel: appliedAuditLevelRef.current || undefined,
          success: successFilter,
          startAt: startAtFilter,
          endAt: endAtFilter,
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
  }, [canView]);

  useEffect(() => {
    loadDictionaries();
  }, [loadDictionaries]);

  useEffect(() => {
    reload();
  }, [
    pageNo,
    pageSize,
    appliedTraceId,
    appliedOperatorUsername,
    appliedAuditResource,
    appliedAuditAction,
    appliedAuditLevel,
    appliedSuccess,
    appliedStartAt,
    appliedEndAt,
    reload,
  ]);

  function toInstant(value: string): string | undefined {
    return dateTimeInputToEpochMillisString(value) ?? undefined;
  }

  function toEndExclusive(value: string): string | undefined {
    return dateTimeInputToNextMinuteEpochMillisString(value) ?? undefined;
  }

  async function applyFilters() {
    setAppliedTraceId(traceIdDraft.trim());
    setAppliedOperatorUsername(operatorUsernameDraft.trim());
    setAppliedAuditResource(auditResourceDraft);
    setAppliedAuditAction(auditActionDraft);
    setAppliedAuditLevel(auditLevelDraft);
    setAppliedSuccess(successDraft);
    setAppliedStartAt(startAtDraft);
    setAppliedEndAt(endAtDraft);
    setPageNo(1);
  }

  async function resetFilters() {
    setTraceIdDraft("");
    setOperatorUsernameDraft("");
    setAuditResourceDraft("");
    setAuditActionDraft("");
    setAuditLevelDraft("");
    setSuccessDraft("");
    setStartAtDraft("");
    setEndAtDraft("");
    setPageNo(1);
    setAppliedTraceId("");
    setAppliedOperatorUsername("");
    setAppliedAuditResource("");
    setAppliedAuditAction("");
    setAppliedAuditLevel("");
    setAppliedSuccess("");
    setAppliedStartAt("");
    setAppliedEndAt("");
  }

  function getRowActions(row: AuditLogEntry): AdminActionItem[] {
    return [
      { key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) },
    ];
  }

  async function openDetail(id: string) {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      setDetail(await getAuditLog(id));
    } finally {
      setDetailLoading(false);
    }
  }

  function formatDuration(value?: number | null): string {
    if (value === null || value === undefined) return "-";
    return `${value} ms`;
  }

  const columns: Array<BzTableColumn<AuditLogEntry>> = [
    { key: "traceId", title: "追踪ID", width: 180, render: (row) => row.traceId || "-" },
    {
      key: "operatorUsername",
      title: "操作人",
      width: 120,
      render: (row) => row.operatorUsername || "-",
    },
    {
      key: "operatorUserType",
      title: "用户类型",
      width: 110,
      render: (row) => (
        <BzTag
          size="small"
          type={
            resolveTagType(userTypeMetaMap, row.operatorUserType) as
              | "info"
              | "warning"
              | "danger"
              | "success"
          }
        >
          {resolveLabel(userTypeMetaMap, row.operatorUserType)}
        </BzTag>
      ),
    },
    {
      key: "auditResource",
      title: "资源",
      width: 140,
      render: (row) => (
        <BzTag size="small">{resolveLabel(auditResourceMetaMap, row.auditResource)}</BzTag>
      ),
    },
    {
      key: "auditAction",
      title: "动作",
      width: 140,
      render: (row) => (
        <BzTag size="small">{resolveLabel(auditActionMetaMap, row.auditAction)}</BzTag>
      ),
    },
    {
      key: "auditLevel",
      title: "等级",
      width: 110,
      render: (row) => (
        <BzTag
          size="small"
          type={
            resolveTagType(auditLevelMetaMap, row.auditLevel) as
              | "info"
              | "warning"
              | "danger"
              | "success"
          }
        >
          {resolveLabel(auditLevelMetaMap, row.auditLevel)}
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
      key: "requestUri",
      title: "请求地址",
      minWidth: 240,
      render: (row) => row.requestUri || "-",
    },
    {
      key: "requestIp",
      title: "请求IP",
      width: 140,
      render: (row) => row.requestIp || "-",
    },
    {
      key: "createdAt",
      title: "时间",
      width: 180,
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      key: "actions",
      title: "操作",
      width: 88,
      className: "is-fixed-right",
      render: (row) => <AdminActionBar actions={getRowActions(row)} />,
    },
  ];

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
                    <div className="admin-query-field__label">追踪ID</div>
                    <div className="admin-query-field__control">
                      <BzInput modelValue={traceIdDraft} placeholder="按追踪ID搜索" clearable onValueChange={setTraceIdDraft} onKeyUp={(e) => e.key === "Enter" && applyFilters()} />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">操作人</div>
                    <div className="admin-query-field__control">
                      <BzInput modelValue={operatorUsernameDraft} placeholder="按操作人搜索" clearable onValueChange={setOperatorUsernameDraft} onKeyUp={(e) => e.key === "Enter" && applyFilters()} />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">资源</div>
                    <div className="admin-query-field__control">
                      <BzSelect modelValue={auditResourceDraft} placeholder="全部资源" clearable onValueChange={(v) => setAuditResourceDraft(v ?? "") }>
                        {auditResourceOptions.map((item) => <BzOption key={item.value} label={item.label} value={item.value} />)}
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">动作</div>
                    <div className="admin-query-field__control">
                      <BzSelect modelValue={auditActionDraft} placeholder="全部动作" clearable onValueChange={(v) => setAuditActionDraft(v ?? "") }>
                        {auditActionOptions.map((item) => <BzOption key={item.value} label={item.label} value={item.value} />)}
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">等级</div>
                    <div className="admin-query-field__control">
                      <BzSelect modelValue={auditLevelDraft} placeholder="全部等级" clearable onValueChange={(v) => setAuditLevelDraft((v ?? "") as "" | AuditLevel)}>
                        {auditLevelOptions.map((item) => <BzOption key={item.value} label={item.label} value={item.value} />)}
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">结果</div>
                    <div className="admin-query-field__control">
                      <BzSelect modelValue={successDraft} placeholder="全部结果" clearable onValueChange={(v) => setSuccessDraft((v ?? "") as "" | "true" | "false") }>
                        <BzOption label="成功" value="true" />
                        <BzOption label="失败" value="false" />
                      </BzSelect>
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
                    <BzButton className="admin-filter-secondary" nativeType="button" onClick={resetFilters}>重置</BzButton>
                    <BzButton className="admin-filter-primary" buttonType="primary" nativeType="button" onClick={applyFilters}>搜索</BzButton>
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
                <div className="admin-table-title">审计日志</div>
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
              <BzEmpty description="无权限查看审计日志" />
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
            loading={detailLoading}
            title="审计日志详情"
            width="960px"
            onClose={() => setDetailOpen(false)}
            footer={<BzButton onClick={() => setDetailOpen(false)}>关闭</BzButton>}
          >
            {detail ? (
              <div className="audit-detail-layout">
                <section className="audit-detail-section">
                  <div className="audit-detail-section__title">基础信息</div>
                  <div className="audit-detail-grid">
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">追踪ID</span>
                      <span className="audit-detail-field__value">{detail.traceId || "-"}</span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">请求ID</span>
                      <span className="audit-detail-field__value">{detail.requestId || "-"}</span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">操作人</span>
                      <span className="audit-detail-field__value">
                        {detail.operatorUsername || "-"}
                      </span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">用户类型</span>
                      <span className="audit-detail-field__value">
                        {resolveLabel(userTypeMetaMap, detail.operatorUserType)}
                      </span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">资源</span>
                      <span className="audit-detail-field__value">
                        {resolveLabel(auditResourceMetaMap, detail.auditResource)}
                      </span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">动作</span>
                      <span className="audit-detail-field__value">
                        {resolveLabel(auditActionMetaMap, detail.auditAction)}
                      </span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">等级</span>
                      <span className="audit-detail-field__value">
                        {resolveLabel(auditLevelMetaMap, detail.auditLevel)}
                      </span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">结果</span>
                      <span className="audit-detail-field__value">
                        {detail.success ? "成功" : "失败"}
                      </span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">协议</span>
                      <span className="audit-detail-field__value">
                        {resolveLabel(apiProtocolMetaMap, detail.protocol)}
                      </span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">方法</span>
                      <span className="audit-detail-field__value">
                        {resolveLabel(apiMethodMetaMap, detail.httpMethod)}
                      </span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">请求IP</span>
                      <span className="audit-detail-field__value">{detail.requestIp || "-"}</span>
                    </div>
                    <div className="audit-detail-field">
                      <span className="audit-detail-field__label">耗时</span>
                      <span className="audit-detail-field__value">
                        {formatDuration(detail.durationMs)}
                      </span>
                    </div>
                    <div className="audit-detail-field audit-detail-field--wide">
                      <span className="audit-detail-field__label">请求地址</span>
                      <span className="audit-detail-field__value">{detail.requestUri || "-"}</span>
                    </div>
                    <div className="audit-detail-field audit-detail-field--wide">
                      <span className="audit-detail-field__label">路径模式</span>
                      <span className="audit-detail-field__value">{detail.pathPattern || "-"}</span>
                    </div>
                    <div className="audit-detail-field audit-detail-field--wide">
                      <span className="audit-detail-field__label">审计描述</span>
                      <span className="audit-detail-field__value">
                        {detail.auditDescription || "-"}
                      </span>
                    </div>
                    <div className="audit-detail-field audit-detail-field--wide">
                      <span className="audit-detail-field__label">权限码</span>
                      <span className="audit-detail-field__value">
                        {detail.permissionCodes.length ? detail.permissionCodes.join(", ") : "-"}
                      </span>
                    </div>
                    <div className="audit-detail-field audit-detail-field--wide">
                      <span className="audit-detail-field__label">记录时间</span>
                      <span className="audit-detail-field__value">
                        {formatDateTime(detail.createdAt)}
                      </span>
                    </div>
                  </div>
                </section>

                <section className="audit-detail-section">
                  <div className="audit-detail-section__title">请求与响应</div>
                  <div className="audit-detail-text-grid">
                    <div className="audit-detail-text-block">
                      <div className="audit-detail-text-block__label">请求参数</div>
                      <pre className="audit-detail-text-block__content">
                        {detail.requestParamSummary || "-"}
                      </pre>
                    </div>
                    <div className="audit-detail-text-block">
                      <div className="audit-detail-text-block__label">请求体</div>
                      <pre className="audit-detail-text-block__content">
                        {detail.requestBodySummary || "-"}
                      </pre>
                    </div>
                    <div className="audit-detail-text-block">
                      <div className="audit-detail-text-block__label">响应体</div>
                      <pre className="audit-detail-text-block__content">
                        {detail.responseSummary || "-"}
                      </pre>
                    </div>
                    <div className="audit-detail-text-block">
                      <div className="audit-detail-text-block__label">错误信息</div>
                      <pre className="audit-detail-text-block__content">
                        {detail.errorMessage || detail.errorCode || "-"}
                      </pre>
                    </div>
                    <div className="audit-detail-text-block audit-detail-text-block--wide">
                      <div className="audit-detail-text-block__label">User-Agent</div>
                      <pre className="audit-detail-text-block__content">
                        {detail.userAgent || "-"}
                      </pre>
                    </div>
                  </div>
                </section>
              </div>
            ) : null}
          </AdminEntityDrawer>
        </div>
      </div>
    </div>
  );
}
