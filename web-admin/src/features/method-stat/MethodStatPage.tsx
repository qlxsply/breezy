"use client";

import { usePermission } from "@admin/features/resources/permissions";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { message } from "@admin/shared/lib/feedback/message";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import entityStyles from "@admin/shared/ui/admin/AdminEntity.module.css";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import { AdminListPageTemplate } from "@admin/shared/ui/admin/AdminListPageTemplate";
import { AdminSearchField, AdminSearchForm } from "@admin/shared/ui/admin/AdminSearchForm";
import { AdminTablePagination } from "@admin/shared/ui/admin/AdminTablePagination";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import {
  BzAlert,
  BzButton,
  BzInput,
  BzOption,
  BzOverflowTooltip,
  BzSelect,
  BzTable,
  type BzTableColumn,
  BzTag,
} from "@admin/shared/ui/bz";
import { useCallback, useEffect, useRef, useState } from "react";

import {
  clearAllMethodStat,
  clearMethodStat,
  getMethodStatGlobalSwitch,
  getMethodStatStatsDetail,
  pageMethodStatStats,
  updateAllMethodStatMethodSwitch,
  updateMethodStatGlobalSwitch,
  updateMethodStatMethodSwitch,
} from "./api/client";
import styles from "./MethodStatPage.module.css";
import type { MethodStatSortBy, MethodStatSortDirection, MethodStatStatsItem } from "./model/types";
import { METHOD_STAT_PERMISSIONS } from "./permissions";

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
type CollectStatusFilter = "" | "true" | "false";

const SORT_OPTIONS: Array<{ label: string; value: MethodStatSortBy }> = [
  { label: "累计调用", value: "TOTAL_CALLS" },
  { label: "累计成功", value: "TOTAL_SUCCESS" },
  { label: "累计失败", value: "TOTAL_FAILURE" },
  { label: "1分钟调用", value: "RECENT_1M_CALLS" },
  { label: "1小时调用", value: "RECENT_1H_CALLS" },
  { label: "1天调用", value: "RECENT_1D_CALLS" },
  { label: "平均耗时", value: "DURATION_AVG" },
  { label: "P95耗时", value: "DURATION_P95" },
  { label: "P99耗时", value: "DURATION_P99" },
  { label: "方法名", value: "METHOD_NAME" },
  { label: "唯一Key", value: "KEY" },
];

function formatNumber(value: number): string {
  return Number.isFinite(value) ? value.toLocaleString("zh-CN") : "0";
}

function formatDecimal(value: number): string {
  return Number.isFinite(value) ? value.toFixed(2) : "0.00";
}

function formatSuccessRate(success: number, total: number): string {
  if (total <= 0) return "0.00%";
  return `${((success / total) * 100).toFixed(2)}%`;
}

function formatMethodDisplay(row: MethodStatStatsItem): string {
  return `${row.className}.${row.methodName}`;
}

export function MethodStatPage() {
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);

  const [globalSwitchEnabled, setGlobalSwitchEnabled] = useState(false);
  const [globalSwitchLoading, setGlobalSwitchLoading] = useState(false);
  const [batchSwitchLoading, setBatchSwitchLoading] = useState(false);
  const [clearingAll, setClearingAll] = useState(false);
  const [methodSwitchLoadingKeys, setMethodSwitchLoadingKeys] = useState<Set<string>>(new Set());
  const [clearingMethodKeys, setClearingMethodKeys] = useState<Set<string>>(new Set());

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<MethodStatStatsItem | null>(null);

  const canStatsView = usePermission(METHOD_STAT_PERMISSIONS.view);
  const canSwitchView = usePermission(METHOD_STAT_PERMISSIONS.switchView);
  const canSwitchEdit = usePermission(METHOD_STAT_PERMISSIONS.switchEdit);
  const canStatClear = usePermission(METHOD_STAT_PERMISSIONS.clear);
  const detailControllerRef = useRef<AbortController | null>(null);
  const mutationLockRef = useRef(false);
  const initialFilters = useRef({
    methodName: "",
    collectStatus: "" as CollectStatusFilter,
    sortBy: "TOTAL_CALLS" as MethodStatSortBy,
    sortDirection: "DESC" as MethodStatSortDirection,
  }).current;
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
  } = useAdminPagedQuery<MethodStatStatsItem, typeof initialFilters>({
    initialFilters,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canStatsView,
    normalizeFilters: (filters) => ({ ...filters, methodName: filters.methodName.trim() }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) =>
      pageMethodStatStats(
        {
          methodName: filters.methodName || undefined,
          collectEnabled:
            filters.collectStatus === "" ? undefined : filters.collectStatus === "true",
          page: { pageNo: targetPage, pageSize: targetSize },
          sort: { orders: [{ field: filters.sortBy, direction: filters.sortDirection }] },
        },
        { signal },
      ),
  });
  const rows = page.elements;

  const loadGlobalSwitch = useCallback(
    async (signal?: AbortSignal) => {
      if (!canSwitchView) {
        setGlobalSwitchEnabled(false);
        return;
      }
      try {
        const state = await getMethodStatGlobalSwitch({ signal });
        if (!signal?.aborted) setGlobalSwitchEnabled(state.enabled);
      } catch (cause) {
        if (!signal?.aborted) {
          message.error(cause instanceof Error ? cause.message : "统计开关加载失败");
        }
      }
    },
    [canSwitchView],
  );

  useEffect(() => {
    const controller = new AbortController();
    void loadGlobalSwitch(controller.signal);
    return () => controller.abort();
  }, [loadGlobalSwitch]);

  useEffect(() => () => detailControllerRef.current?.abort(), []);

  async function refreshPage() {
    await Promise.allSettled([refresh(), loadGlobalSwitch()]);
  }

  async function handleGlobalSwitchChange(enabled: boolean) {
    if (!canSwitchEdit) {
      message.warning("无权限维护统计开关");
      return;
    }
    if (mutationLockRef.current) return;

    mutationLockRef.current = true;
    setGlobalSwitchLoading(true);
    try {
      const updated = await updateMethodStatGlobalSwitch(enabled);
      setGlobalSwitchEnabled(updated.enabled);
      await refresh();
      if (!updated.enabled) {
        setDetailOpen(false);
        setDetail(null);
      }
    } finally {
      mutationLockRef.current = false;
      setGlobalSwitchLoading(false);
    }
  }

  async function handleSetAllMethodSwitch(enabled: boolean) {
    if (!canSwitchEdit) return;
    if (!globalSwitchEnabled) {
      message.warning("请先开启采集功能");
      return;
    }
    if (mutationLockRef.current) return;

    mutationLockRef.current = true;
    setBatchSwitchLoading(true);
    try {
      await updateAllMethodStatMethodSwitch(enabled);
      await refresh();
      await refreshDetail();
    } finally {
      mutationLockRef.current = false;
      setBatchSwitchLoading(false);
    }
  }

  async function handleMethodSwitchChange(row: MethodStatStatsItem) {
    if (!canSwitchEdit || mutationLockRef.current || methodSwitchLoadingKeys.has(row.key)) return;
    const enabled = !row.methodSwitchEnabled;
    mutationLockRef.current = true;
    setMethodSwitchLoadingKeys((current) => new Set(current).add(row.key));
    try {
      await updateMethodStatMethodSwitch(row.key, enabled);
      await refresh();
      if (detail?.key === row.key) await openDetail(row.key);
    } finally {
      mutationLockRef.current = false;
      setMethodSwitchLoadingKeys((current) => {
        const next = new Set(current);
        next.delete(row.key);
        return next;
      });
    }
  }

  async function handleClearMethod(row: MethodStatStatsItem) {
    if (!canStatClear || mutationLockRef.current || clearingMethodKeys.has(row.key)) return;
    mutationLockRef.current = true;
    setClearingMethodKeys((current) => new Set(current).add(row.key));
    try {
      await clearMethodStat(row.key);
      await refresh();
      if (detail?.key === row.key) await openDetail(row.key);
    } finally {
      mutationLockRef.current = false;
      setClearingMethodKeys((current) => {
        const next = new Set(current);
        next.delete(row.key);
        return next;
      });
    }
  }

  async function handleClearAll() {
    if (!canStatClear || mutationLockRef.current) return;
    mutationLockRef.current = true;
    setClearingAll(true);
    try {
      await clearAllMethodStat();
      await refresh();
      await refreshDetail();
    } finally {
      mutationLockRef.current = false;
      setClearingAll(false);
    }
  }

  async function openDetail(key: string) {
    if (!canStatsView) return;
    detailControllerRef.current?.abort();
    const controller = new AbortController();
    detailControllerRef.current = controller;
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const nextDetail = await getMethodStatStatsDetail(key, { signal: controller.signal });
      if (detailControllerRef.current === controller && !controller.signal.aborted) {
        setDetail(nextDetail);
      }
    } catch (cause) {
      if (!controller.signal.aborted && detailControllerRef.current === controller) {
        message.error(cause instanceof Error ? cause.message : "方法统计详情加载失败");
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

  async function refreshDetail() {
    if (detailOpen && detail) await openDetail(detail.key);
  }

  function getActions(row: MethodStatStatsItem): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      {
        key: "detail",
        label: "详情",
        level: "default",
        onClick: () => void openDetail(row.key),
      },
    ];
    if (canSwitchEdit) {
      actions.push({
        key: "toggle",
        label: row.methodSwitchEnabled ? "关闭" : "开启",
        level: row.methodSwitchEnabled ? "warning" : "success",
        disabled: !globalSwitchEnabled || methodSwitchLoadingKeys.has(row.key),
        onClick: () => void handleMethodSwitchChange(row),
      });
    }
    if (canStatClear) {
      actions.push({
        key: "clear",
        label: "清空",
        level: "danger",
        disabled: clearingMethodKeys.has(row.key),
        onClick: () => void handleClearMethod(row),
      });
    }
    return actions;
  }

  const columns: Array<BzTableColumn<MethodStatStatsItem>> = [
    {
      key: "method",
      title: "方法",
      width: 420,
      className: `${styles.methodColumn} is-sticky-left`,
      headerClassName: `${styles.methodColumn} is-sticky-left`,
      render: (row) => (
        <BzOverflowTooltip text={row.methodSignature}>
          <span className={styles.method}>{formatMethodDisplay(row)}</span>
        </BzOverflowTooltip>
      ),
    },
    {
      key: "status",
      title: "采集状态",
      width: 100,
      render: (row) => (
        <BzTag type={row.collectEnabled ? "success" : "info"}>
          {row.collectEnabled ? "采集中" : "已关闭"}
        </BzTag>
      ),
    },
    {
      key: "totalCalls",
      title: "累计调用",
      width: 100,
      render: (row) => formatNumber(row.totalCalls),
    },
    {
      key: "totalSuccess",
      title: "成功",
      width: 100,
      render: (row) => formatNumber(row.totalSuccess),
    },
    {
      key: "totalFailure",
      title: "失败",
      width: 100,
      render: (row) => formatNumber(row.totalFailure),
    },
    {
      key: "successRate",
      title: "成功率",
      width: 100,
      render: (row) => formatSuccessRate(row.totalSuccess, row.totalCalls),
    },
    {
      key: "recent1MinuteCalls",
      title: "近1分钟",
      width: 100,
      render: (row) => formatNumber(row.recent1MinuteCalls),
    },
    {
      key: "recent1HourCalls",
      title: "近1小时",
      width: 100,
      render: (row) => formatNumber(row.recent1HourCalls),
    },
    {
      key: "recent1DayCalls",
      title: "近1天",
      width: 100,
      render: (row) => formatNumber(row.recent1DayCalls),
    },
    {
      key: "durationAvg",
      title: "平均耗时(ms)",
      width: 100,
      render: (row) => formatDecimal(row.durationAvg),
    },
    {
      key: "durationP95",
      title: "P95(ms)",
      width: 100,
      render: (row) => formatNumber(row.durationP95),
    },
    {
      key: "durationMax",
      title: "最大耗时(ms)",
      width: 100,
      render: (row) => formatNumber(row.durationMax),
    },
  ];
  const actionsColumn = createAdminActionsColumn({ rows, getActions });
  if (actionsColumn) columns.push(actionsColumn);

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
            <AdminSearchField label="方法">
              <BzInput
                modelValue={draftFilters.methodName}
                placeholder="按包名、类名、方法名或签名搜索"
                clearable
                onValueChange={(methodName) =>
                  setDraftFilters((filters) => ({ ...filters, methodName }))
                }
              />
            </AdminSearchField>
            <AdminSearchField label="采集状态">
              <BzSelect
                modelValue={draftFilters.collectStatus}
                placeholder="全部"
                clearable
                onValueChange={(collectStatus) =>
                  setDraftFilters((filters) => ({
                    ...filters,
                    collectStatus: (collectStatus ?? "") as CollectStatusFilter,
                  }))
                }
              >
                <BzOption
                  label="采集中"
                  value="true"
                />
                <BzOption
                  label="已关闭"
                  value="false"
                />
              </BzSelect>
            </AdminSearchField>
            <AdminSearchField label="排序字段">
              <BzSelect
                modelValue={draftFilters.sortBy}
                onValueChange={(sortBy) =>
                  setDraftFilters((filters) => ({
                    ...filters,
                    sortBy: (sortBy ?? "TOTAL_CALLS") as MethodStatSortBy,
                  }))
                }
              >
                {SORT_OPTIONS.map((option) => (
                  <BzOption
                    key={option.value}
                    label={option.label}
                    value={option.value}
                  />
                ))}
              </BzSelect>
            </AdminSearchField>
            <AdminSearchField label="排序方向">
              <BzSelect
                modelValue={draftFilters.sortDirection}
                onValueChange={(sortDirection) =>
                  setDraftFilters((filters) => ({
                    ...filters,
                    sortDirection: (sortDirection ?? "DESC") as MethodStatSortDirection,
                  }))
                }
              >
                <BzOption
                  label="降序"
                  value="DESC"
                />
                <BzOption
                  label="升序"
                  value="ASC"
                />
              </BzSelect>
            </AdminSearchField>
          </AdminSearchForm>
        }
        businessActions={
          <>
            {canSwitchView ? (
              <BzButton
                className={styles.globalAction}
                buttonType={globalSwitchEnabled ? "danger" : "primary"}
                disabled={!canSwitchEdit || globalSwitchLoading}
                onClick={() => void handleGlobalSwitchChange(!globalSwitchEnabled)}
              >
                {globalSwitchEnabled ? "停止采集" : "开启采集"}
              </BzButton>
            ) : null}
            {globalSwitchEnabled && canSwitchEdit ? (
              <>
                <BzButton
                  disabled={batchSwitchLoading}
                  onClick={() => void handleSetAllMethodSwitch(true)}
                >
                  全部开启
                </BzButton>
                <BzButton
                  disabled={batchSwitchLoading}
                  onClick={() => void handleSetAllMethodSwitch(false)}
                >
                  全部关闭
                </BzButton>
              </>
            ) : null}
            {globalSwitchEnabled && canStatClear ? (
              <BzButton
                buttonType="danger"
                disabled={clearingAll}
                onClick={() => void handleClearAll()}
              >
                全部清空
              </BzButton>
            ) : null}
          </>
        }
        queryTools={
          <AdminTableTools
            queryPanelVisible={queryPanelVisible}
            onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
            onRefresh={() => void refreshPage()}
          />
        }
        table={
          <div className={styles.tableScope}>
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
              rowKey="key"
              loading={loading}
              emptyText={canStatsView ? "暂无统计数据" : "无权限查看统计数据"}
              size="small"
            />
          </div>
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
        title="方法统计详情"
        width="1080px"
        loading={detailLoading}
        className={`${entityStyles.manageDrawer} ${styles.detailDrawer}`}
        onClose={closeDetail}
        footer={<BzButton onClick={closeDetail}>关闭</BzButton>}
      >
        {detail ? (
          <div className={entityStyles.shell}>
            <section className={entityStyles.section}>
              <div className={entityStyles.sectionHead}>
                <div className={entityStyles.sectionTitle}>方法信息</div>
              </div>
              <div className={entityStyles.infoTableWrap}>
                <table
                  className={entityStyles.infoTable}
                  aria-label="方法信息"
                >
                  <tbody>
                    <tr>
                      <th>包名</th>
                      <td colSpan={3}>{detail.packageName}</td>
                    </tr>
                    <tr>
                      <th>类名</th>
                      <td colSpan={3}>{detail.className}</td>
                    </tr>
                    <tr>
                      <th>方法名</th>
                      <td colSpan={3}>{detail.methodName}</td>
                    </tr>
                    <tr>
                      <th>方法签名</th>
                      <td
                        className={styles.detailCode}
                        colSpan={3}
                      >
                        {detail.methodSignature}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section className={entityStyles.section}>
              <div className={entityStyles.sectionHead}>
                <div className={entityStyles.sectionTitle}>采集状态</div>
              </div>
              <div className={entityStyles.infoTableWrap}>
                <table
                  className={entityStyles.infoTable}
                  aria-label="采集状态"
                >
                  <tbody>
                    <tr>
                      <th>全局开关</th>
                      <td>{detail.globalSwitchEnabled ? "开启" : "关闭"}</td>
                      <th>方法开关</th>
                      <td>{detail.methodSwitchEnabled ? "开启" : "关闭"}</td>
                      <th>采集状态</th>
                      <td>
                        <BzTag type={detail.collectEnabled ? "success" : "info"}>
                          {detail.collectEnabled ? "采集中" : "已关闭"}
                        </BzTag>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section className={entityStyles.section}>
              <div className={entityStyles.sectionHead}>
                <div className={entityStyles.sectionTitle}>统计指标</div>
              </div>
              <div className={entityStyles.infoTableWrap}>
                <table
                  className={`${entityStyles.infoTable} ${styles.metricsTable}`}
                  aria-label="统计指标"
                >
                  <tbody>
                    <tr>
                      <th>累计调用</th>
                      <td>{formatNumber(detail.totalCalls)}</td>
                      <th>累计成功</th>
                      <td>{formatNumber(detail.totalSuccess)}</td>
                      <th>累计失败</th>
                      <td>{formatNumber(detail.totalFailure)}</td>
                    </tr>
                    <tr>
                      <th>成功率</th>
                      <td>{formatSuccessRate(detail.totalSuccess, detail.totalCalls)}</td>
                      <th>近1分钟调用</th>
                      <td>{formatNumber(detail.recent1MinuteCalls)}</td>
                      <th>近1分钟成功</th>
                      <td>{formatNumber(detail.recent1MinuteSuccess)}</td>
                    </tr>
                    <tr>
                      <th>近1分钟失败</th>
                      <td>{formatNumber(detail.recent1MinuteFailure)}</td>
                      <th>近1小时调用</th>
                      <td>{formatNumber(detail.recent1HourCalls)}</td>
                      <th>近1小时成功</th>
                      <td>{formatNumber(detail.recent1HourSuccess)}</td>
                    </tr>
                    <tr>
                      <th>近1小时失败</th>
                      <td>{formatNumber(detail.recent1HourFailure)}</td>
                      <th>近1天调用</th>
                      <td>{formatNumber(detail.recent1DayCalls)}</td>
                      <th>近1天成功</th>
                      <td>{formatNumber(detail.recent1DaySuccess)}</td>
                    </tr>
                    <tr>
                      <th>近1天失败</th>
                      <td>{formatNumber(detail.recent1DayFailure)}</td>
                      <th>耗时样本</th>
                      <td>{formatNumber(detail.durationSampleSize)}</td>
                      <th>最小耗时(ms)</th>
                      <td>{formatNumber(detail.durationMin)}</td>
                    </tr>
                    <tr>
                      <th>平均耗时(ms)</th>
                      <td>{formatDecimal(detail.durationAvg)}</td>
                      <th>P50(ms)</th>
                      <td>{formatNumber(detail.durationP50)}</td>
                      <th>P90(ms)</th>
                      <td>{formatNumber(detail.durationP90)}</td>
                    </tr>
                    <tr>
                      <th>P95(ms)</th>
                      <td>{formatNumber(detail.durationP95)}</td>
                      <th>P99(ms)</th>
                      <td>{formatNumber(detail.durationP99)}</td>
                      <th>最大耗时(ms)</th>
                      <td>{formatNumber(detail.durationMax)}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        ) : null}
      </AdminEntityDrawer>
    </>
  );
}
