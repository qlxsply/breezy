"use client";

import { useDateTimePreferences } from "@admin/features/auth/public/session";
import { batchListDictionaryOptions as batchListDictOptions } from "@admin/features/dicts/public/dictionary-client";
import type { PublicDictionaryItem as DictItem } from "@admin/features/dicts/public/types";
import { usePermission } from "@admin/features/resources/permissions";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { message } from "@admin/shared/lib/feedback/message";
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
import { BzOption } from "@admin/shared/ui/bz/BzOption";
import { BzOverflowTooltip } from "@admin/shared/ui/bz/BzOverflowTooltip";
import { BzSelect } from "@admin/shared/ui/bz/BzSelect";
import type { BzTableColumn } from "@admin/shared/ui/bz/BzTable";
import { BzTable } from "@admin/shared/ui/bz/BzTable";
import { BzTag } from "@admin/shared/ui/bz/BzTag";
import { useEffect, useMemo, useRef, useState } from "react";

import { getAuditLog, pageAuditLogs } from "./api/client";
import styles from "./AuditLogsPage.module.css";
import type { AuditLevel, AuditLogEntry } from "./model/types";
import { AUDIT_LOG_PERMISSIONS } from "./permissions";

type DictMeta = { label: string; tagType?: string | null };

const AUDIT_DICT_CODES = [
  "AUDIT_RESOURCE",
  "AUDIT_ACTION",
  "AUDIT_LEVEL",
  "API_METHOD",
  "API_PROTOCOL",
  "USER_TYPE",
] as const;

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const INITIAL_FILTERS = {
  traceId: "",
  operatorUsername: "",
  auditResource: "",
  auditAction: "",
  auditLevel: "" as "" | AuditLevel,
  success: "" as "" | "true" | "false",
  startAt: "",
  endAt: "",
};

type AuditLogFilters = typeof INITIAL_FILTERS;

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
  const { dateTimePattern, timeZone } = useDateTimePreferences();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<AuditLogEntry | null>(null);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
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

  const canView = usePermission(AUDIT_LOG_PERMISSIONS.view);
  const detailControllerRef = useRef<AbortController | null>(null);
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
  } = useAdminPagedQuery<AuditLogEntry, AuditLogFilters>({
    initialFilters: INITIAL_FILTERS,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canView,
    normalizeFilters: (filters) => ({
      ...filters,
      traceId: filters.traceId.trim(),
      operatorUsername: filters.operatorUsername.trim(),
    }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) => {
      const range = buildRequestRange(filters.startAt, filters.endAt);
      return pageAuditLogs(
        {
          traceId: filters.traceId || undefined,
          operatorUsername: filters.operatorUsername || undefined,
          auditResource: filters.auditResource || undefined,
          auditAction: filters.auditAction || undefined,
          auditLevel: filters.auditLevel || undefined,
          success: filters.success === "" ? undefined : filters.success === "true",
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
    void batchListDictOptions([...AUDIT_DICT_CODES], { signal: controller.signal })
      .then((result) => {
        setAuditResourceMetaMap(toDictMetaMap(result.AUDIT_RESOURCE));
        setAuditActionMetaMap(toDictMetaMap(result.AUDIT_ACTION));
        setAuditLevelMetaMap(toDictMetaMap(result.AUDIT_LEVEL));
        setApiMethodMetaMap(toDictMetaMap(result.API_METHOD));
        setApiProtocolMetaMap(toDictMetaMap(result.API_PROTOCOL));
        setUserTypeMetaMap(toDictMetaMap(result.USER_TYPE));
      })
      .catch(() => undefined);
    return () => controller.abort();
  }, []);

  useEffect(() => () => detailControllerRef.current?.abort(), []);

  function buildRequestRange(start: string, end: string): { startAt?: string; endAt?: string } {
    const range = buildDateTimeRangeSubmitValue(start, end);
    return {
      startAt: range.startTimestamp || undefined,
      endAt: range.endTimestamp || undefined,
    };
  }

  function getRowActions(row: AuditLogEntry): AdminActionItem[] {
    return [
      {
        key: `detail-${row.id}`,
        label: "详情",
        level: "default",
        onClick: () => openDetail(row.id),
      },
    ];
  }

  async function openDetail(id: string) {
    detailControllerRef.current?.abort();
    const controller = new AbortController();
    detailControllerRef.current = controller;
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const nextDetail = await getAuditLog(id, { signal: controller.signal });
      if (detailControllerRef.current === controller && !controller.signal.aborted) {
        setDetail(nextDetail);
      }
    } catch (cause) {
      if (!controller.signal.aborted && detailControllerRef.current === controller) {
        message.error(cause instanceof Error ? cause.message : "审计日志详情加载失败");
        setDetailOpen(false);
      }
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

  function formatDuration(value?: number | null): string {
    if (value === null || value === undefined) return "-";
    return `${value} ms`;
  }

  const columns: Array<BzTableColumn<AuditLogEntry>> = [
    {
      key: "traceId",
      title: "追踪ID",
      width: 180,
      render: (row) => {
        const text = row.traceId || "-";
        return (
          <BzOverflowTooltip text={text}>
            <span className={`${styles.mono} ${styles.cellText}`}>{text}</span>
          </BzOverflowTooltip>
        );
      },
    },
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
              "info" | "warning" | "danger" | "success"
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
              "info" | "warning" | "danger" | "success"
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
      render: (row) => {
        const text = row.requestUri || "-";
        return (
          <BzOverflowTooltip text={text}>
            <span className={styles.cellText}>{text}</span>
          </BzOverflowTooltip>
        );
      },
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
  ];
  const actionsColumn = createAdminActionsColumn({ rows, getActions: getRowActions });
  if (actionsColumn) columns.push(actionsColumn);

  return (
    <AdminListPageTemplate
      queryPanelVisible={queryPanelVisible}
      queryPanel={
        <AdminSearchForm
          visible={queryPanelVisible}
          onSubmit={submit}
          onReset={reset}
        >
          <AdminSearchField label="追踪ID">
            <BzInput
              modelValue={draftFilters.traceId}
              placeholder="按追踪ID搜索"
              clearable
              onValueChange={(traceId) => setDraftFilters((filters) => ({ ...filters, traceId }))}
            />
          </AdminSearchField>
          <AdminSearchField label="操作人">
            <BzInput
              modelValue={draftFilters.operatorUsername}
              placeholder="按操作人搜索"
              clearable
              onValueChange={(operatorUsername) =>
                setDraftFilters((filters) => ({ ...filters, operatorUsername }))
              }
            />
          </AdminSearchField>
          <AdminSearchField label="资源">
            <BzSelect
              modelValue={draftFilters.auditResource}
              placeholder="全部资源"
              clearable
              onValueChange={(auditResource) =>
                setDraftFilters((filters) => ({ ...filters, auditResource: auditResource ?? "" }))
              }
            >
              {auditResourceOptions.map((item) => (
                <BzOption
                  key={item.value}
                  label={item.label}
                  value={item.value}
                />
              ))}
            </BzSelect>
          </AdminSearchField>
          <AdminSearchField label="动作">
            <BzSelect
              modelValue={draftFilters.auditAction}
              placeholder="全部动作"
              clearable
              onValueChange={(auditAction) =>
                setDraftFilters((filters) => ({ ...filters, auditAction: auditAction ?? "" }))
              }
            >
              {auditActionOptions.map((item) => (
                <BzOption
                  key={item.value}
                  label={item.label}
                  value={item.value}
                />
              ))}
            </BzSelect>
          </AdminSearchField>
          <AdminSearchField label="等级">
            <BzSelect
              modelValue={draftFilters.auditLevel}
              placeholder="全部等级"
              clearable
              onValueChange={(auditLevel) =>
                setDraftFilters((filters) => ({
                  ...filters,
                  auditLevel: (auditLevel ?? "") as "" | AuditLevel,
                }))
              }
            >
              {auditLevelOptions.map((item) => (
                <BzOption
                  key={item.value}
                  label={item.label}
                  value={item.value}
                />
              ))}
            </BzSelect>
          </AdminSearchField>
          <AdminSearchField label="结果">
            <BzSelect
              modelValue={draftFilters.success}
              placeholder="全部结果"
              clearable
              onValueChange={(success) =>
                setDraftFilters((filters) => ({
                  ...filters,
                  success: (success ?? "") as "" | "true" | "false",
                }))
              }
            >
              <BzOption
                label="成功"
                value="true"
              />
              <BzOption
                label="失败"
                value="false"
              />
            </BzSelect>
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
          <BzEmpty description="无权限查看审计日志" />
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
          loading={detailLoading}
          title="审计日志详情"
          width="1180px"
          className={entityStyles.manageDrawer}
          onClose={closeDetail}
          footer={<BzButton onClick={closeDetail}>关闭</BzButton>}
        >
          {detail ? (
            <div className={entityStyles.shell}>
              <section className={entityStyles.section}>
                <div className={entityStyles.sectionHead}>
                  <div className={entityStyles.sectionTitle}>基础信息</div>
                </div>

                <div className={entityStyles.infoTableWrap}>
                  <table
                    className={entityStyles.infoTable}
                    aria-label="审计日志基础信息"
                  >
                    <tbody>
                      <tr>
                        <th>追踪ID</th>
                        <td>
                          <span className={styles.mono}>{detail.traceId || "-"}</span>
                        </td>
                        <th>请求ID</th>
                        <td>
                          <span className={styles.mono}>{detail.requestId || "-"}</span>
                        </td>
                        <th>操作人</th>
                        <td>{detail.operatorUsername || "-"}</td>
                      </tr>
                      <tr>
                        <th>用户类型</th>
                        <td>
                          <BzTag
                            size="small"
                            type={
                              resolveTagType(userTypeMetaMap, detail.operatorUserType) as
                                "info" | "warning" | "danger" | "success"
                            }
                          >
                            {resolveLabel(userTypeMetaMap, detail.operatorUserType)}
                          </BzTag>
                        </td>
                        <th>资源</th>
                        <td>
                          <BzTag size="small">
                            {resolveLabel(auditResourceMetaMap, detail.auditResource)}
                          </BzTag>
                        </td>
                        <th>动作</th>
                        <td>
                          <BzTag size="small">
                            {resolveLabel(auditActionMetaMap, detail.auditAction)}
                          </BzTag>
                        </td>
                      </tr>
                      <tr>
                        <th>等级</th>
                        <td>
                          <BzTag
                            size="small"
                            type={
                              resolveTagType(auditLevelMetaMap, detail.auditLevel) as
                                "info" | "warning" | "danger" | "success"
                            }
                          >
                            {resolveLabel(auditLevelMetaMap, detail.auditLevel)}
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
                        <th>记录时间</th>
                        <td>{formatDateTime(detail.createdAt)}</td>
                      </tr>
                      <tr>
                        <th>耗时</th>
                        <td>
                          <span className={styles.mono}>{formatDuration(detail.durationMs)}</span>
                        </td>
                        <th>协议</th>
                        <td>{resolveLabel(apiProtocolMetaMap, detail.protocol)}</td>
                        <th>方法</th>
                        <td>{resolveLabel(apiMethodMetaMap, detail.httpMethod)}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>

              <section className={entityStyles.section}>
                <div className={entityStyles.sectionHead}>
                  <div className={entityStyles.sectionTitle}>请求信息</div>
                </div>

                <div className={entityStyles.infoTableWrap}>
                  <table
                    className={entityStyles.infoTable}
                    aria-label="审计日志请求信息"
                  >
                    <tbody>
                      <tr>
                        <th>请求IP</th>
                        <td>
                          <span className={styles.mono}>{detail.requestIp || "-"}</span>
                        </td>
                        <th>请求地址</th>
                        <td colSpan={3}>
                          <span className={styles.mono}>{detail.requestUri || "-"}</span>
                        </td>
                      </tr>
                      <tr>
                        <th>路径模式</th>
                        <td colSpan={5}>
                          <span className={styles.mono}>{detail.pathPattern || "-"}</span>
                        </td>
                      </tr>
                      <tr>
                        <th>User-Agent</th>
                        <td colSpan={5}>
                          <pre className={styles.pre}>{detail.userAgent || "-"}</pre>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>

              <section className={entityStyles.section}>
                <div className={entityStyles.sectionHead}>
                  <div className={entityStyles.sectionTitle}>审计摘要</div>
                </div>

                <div className={entityStyles.infoTableWrap}>
                  <table
                    className={entityStyles.infoTable}
                    aria-label="审计日志摘要"
                  >
                    <tbody>
                      <tr>
                        <th>权限码</th>
                        <td colSpan={5}>
                          <span className={styles.mono}>
                            {detail.permissionCodes.length
                              ? detail.permissionCodes.join(", ")
                              : "-"}
                          </span>
                        </td>
                      </tr>
                      <tr>
                        <th>审计描述</th>
                        <td colSpan={5}>
                          <pre className={styles.pre}>{detail.auditDescription || "-"}</pre>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>

              <section className={entityStyles.section}>
                <div className={entityStyles.sectionHead}>
                  <div className={entityStyles.sectionTitle}>请求与响应</div>
                </div>

                <div className={entityStyles.infoTableWrap}>
                  <table
                    className={entityStyles.infoTable}
                    aria-label="审计日志请求与响应"
                  >
                    <tbody>
                      <tr>
                        <th>请求参数</th>
                        <td colSpan={5}>
                          <pre className={styles.pre}>{detail.requestParamSummary || "-"}</pre>
                        </td>
                      </tr>
                      <tr>
                        <th>请求体</th>
                        <td colSpan={5}>
                          <pre className={styles.pre}>{detail.requestBodySummary || "-"}</pre>
                        </td>
                      </tr>
                      <tr>
                        <th>响应体</th>
                        <td colSpan={5}>
                          <pre className={styles.pre}>{detail.responseSummary || "-"}</pre>
                        </td>
                      </tr>
                      <tr>
                        <th>错误信息</th>
                        <td colSpan={5}>
                          <pre className={styles.pre}>
                            {detail.errorMessage || detail.errorCode || "-"}
                          </pre>
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
