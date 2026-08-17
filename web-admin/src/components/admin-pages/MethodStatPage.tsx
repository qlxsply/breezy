"use client";

import {
  clearAllMethodStat,
  clearMethodStat,
  getMethodStatGlobalSwitch,
  getMethodStatStatsDetail,
  pageMethodStatStats,
  updateAllMethodStatMethodSwitch,
  updateMethodStatGlobalSwitch,
  updateMethodStatMethodSwitch,
} from "@admin/api/method-stat";
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
  BzOverflowTooltip,
  BzPagination,
  BzSelect,
  BzTable,
  type BzTableColumn,
  BzTag,
} from "@admin/components/bz";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type {
  MethodStatSortBy,
  MethodStatSortDirection,
  MethodStatStatsItem,
} from "@admin/types/method-stat";
import type { PageResult } from "@admin/types/page";
import { useCallback, useEffect, useRef, useState } from "react";

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

function createEmptyPage(pageSize: number): PageResult<MethodStatStatsItem> {
  return {
    pageNo: 1,
    pageSize,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  };
}

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
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<MethodStatStatsItem[]>([]);
  const [page, setPage] = useState<PageResult<MethodStatStatsItem>>(createEmptyPage(10));
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);

  const [methodNameDraft, setMethodNameDraft] = useState("");
  const [collectStatusDraft, setCollectStatusDraft] = useState<CollectStatusFilter>("");
  const [sortByDraft, setSortByDraft] = useState<MethodStatSortBy>("TOTAL_CALLS");
  const [sortDirectionDraft, setSortDirectionDraft] = useState<MethodStatSortDirection>("DESC");
  const [appliedMethodName, setAppliedMethodName] = useState("");
  const [appliedCollectStatus, setAppliedCollectStatus] = useState<CollectStatusFilter>("");
  const [appliedSortBy, setAppliedSortBy] = useState<MethodStatSortBy>("TOTAL_CALLS");
  const [appliedSortDirection, setAppliedSortDirection] = useState<MethodStatSortDirection>("DESC");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [globalSwitchEnabled, setGlobalSwitchEnabled] = useState(false);
  const [globalSwitchLoading, setGlobalSwitchLoading] = useState(false);
  const [batchSwitchLoading, setBatchSwitchLoading] = useState(false);
  const [clearingAll, setClearingAll] = useState(false);
  const [methodSwitchLoadingKeys, setMethodSwitchLoadingKeys] = useState<Set<string>>(new Set());
  const [clearingMethodKeys, setClearingMethodKeys] = useState<Set<string>>(new Set());

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<MethodStatStatsItem | null>(null);

  const canStatsView = hasResourceCodeAccess("method-stat-view");
  const canSwitchView = hasResourceCodeAccess("method-stat-switch-view");
  const canSwitchEdit = hasResourceCodeAccess("method-stat-switch-edit");
  const canStatClear = hasResourceCodeAccess("method-stat-clear");
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const pageNoRef = useRef(pageNo);
  const pageSizeRef = useRef(pageSize);
  const methodNameRef = useRef(appliedMethodName);
  const collectStatusRef = useRef(appliedCollectStatus);
  const sortByRef = useRef(appliedSortBy);
  const sortDirectionRef = useRef(appliedSortDirection);
  const canStatsViewRef = useRef(canStatsView);
  pageNoRef.current = pageNo;
  pageSizeRef.current = pageSize;
  methodNameRef.current = appliedMethodName;
  collectStatusRef.current = appliedCollectStatus;
  sortByRef.current = appliedSortBy;
  sortDirectionRef.current = appliedSortDirection;
  canStatsViewRef.current = canStatsView;

  const reload = useCallback(async (silent = false) => {
    const currentPageSize = pageSizeRef.current;
    if (!canStatsViewRef.current) {
      const emptyPage = createEmptyPage(currentPageSize);
      setPage(emptyPage);
      setRows([]);
      return;
    }

    if (!silent) setLoading(true);
    try {
      const request = (requestedPageNo: number) =>
        pageMethodStatStats({
          methodName: methodNameRef.current.trim() || undefined,
          collectEnabled:
            collectStatusRef.current === "" ? undefined : collectStatusRef.current === "true",
          page: { pageNo: requestedPageNo, pageSize: currentPageSize },
          sort: {
            orders: [{ field: sortByRef.current, direction: sortDirectionRef.current }],
          },
        });

      const requestedPageNo = pageNoRef.current;
      let result = await request(requestedPageNo);
      if (result.totalElements > 0 && requestedPageNo > Math.max(1, result.totalPages)) {
        const lastPage = Math.max(1, result.totalPages);
        setPageNo(lastPage);
        result = await request(lastPage);
      }
      setPage(result);
      setRows(result.elements);
      setPageNo(result.pageNo || 1);
      setPageSize(result.pageSize || currentPageSize);
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  const loadGlobalSwitch = useCallback(async () => {
    if (!canSwitchView) {
      setGlobalSwitchEnabled(false);
      return;
    }
    const state = await getMethodStatGlobalSwitch();
    setGlobalSwitchEnabled(state.enabled);
  }, [canSwitchView]);

  useEffect(() => {
    void reload();
  }, [
    appliedCollectStatus,
    appliedMethodName,
    appliedSortBy,
    appliedSortDirection,
    pageNo,
    pageSize,
    reload,
  ]);

  useEffect(() => {
    void loadGlobalSwitch();
  }, [loadGlobalSwitch]);

  function applyFilters() {
    setAppliedMethodName(methodNameDraft.trim());
    setAppliedCollectStatus(collectStatusDraft);
    setAppliedSortBy(sortByDraft);
    setAppliedSortDirection(sortDirectionDraft);
    setPageNo(1);
  }

  function resetFilters() {
    setMethodNameDraft("");
    setCollectStatusDraft("");
    setSortByDraft("TOTAL_CALLS");
    setSortDirectionDraft("DESC");
    setAppliedMethodName("");
    setAppliedCollectStatus("");
    setAppliedSortBy("TOTAL_CALLS");
    setAppliedSortDirection("DESC");
    setPageNo(1);
  }

  async function refreshPage() {
    await Promise.allSettled([reload(), loadGlobalSwitch()]);
  }

  async function handleGlobalSwitchChange(enabled: boolean) {
    if (!canSwitchEdit) {
      message.warning("无权限维护统计开关");
      return;
    }
    if (globalSwitchLoading) return;

    setGlobalSwitchLoading(true);
    try {
      const updated = await updateMethodStatGlobalSwitch(enabled);
      setGlobalSwitchEnabled(updated.enabled);
      await reload(true);
      if (!updated.enabled) {
        setDetailOpen(false);
        setDetail(null);
      }
    } finally {
      setGlobalSwitchLoading(false);
    }
  }

  async function handleSetAllMethodSwitch(enabled: boolean) {
    if (!canSwitchEdit) return;
    if (!globalSwitchEnabled) {
      message.warning("请先开启采集功能");
      return;
    }
    if (batchSwitchLoading) return;

    setBatchSwitchLoading(true);
    try {
      await updateAllMethodStatMethodSwitch(enabled);
      await reload(true);
      await refreshDetail();
    } finally {
      setBatchSwitchLoading(false);
    }
  }

  async function handleMethodSwitchChange(row: MethodStatStatsItem) {
    if (!canSwitchEdit || methodSwitchLoadingKeys.has(row.key)) return;
    const enabled = !row.methodSwitchEnabled;
    setMethodSwitchLoadingKeys((current) => new Set(current).add(row.key));
    try {
      await updateMethodStatMethodSwitch(row.key, enabled);
      await reload(true);
      if (detail?.key === row.key) await openDetail(row.key);
    } finally {
      setMethodSwitchLoadingKeys((current) => {
        const next = new Set(current);
        next.delete(row.key);
        return next;
      });
    }
  }

  async function handleClearMethod(row: MethodStatStatsItem) {
    if (!canStatClear || clearingMethodKeys.has(row.key)) return;
    setClearingMethodKeys((current) => new Set(current).add(row.key));
    try {
      await clearMethodStat(row.key);
      await reload(true);
      if (detail?.key === row.key) await openDetail(row.key);
    } finally {
      setClearingMethodKeys((current) => {
        const next = new Set(current);
        next.delete(row.key);
        return next;
      });
    }
  }

  async function handleClearAll() {
    if (!canStatClear || clearingAll) return;
    setClearingAll(true);
    try {
      await clearAllMethodStat();
      await reload(true);
      await refreshDetail();
    } finally {
      setClearingAll(false);
    }
  }

  async function openDetail(key: string) {
    if (!canStatsView) return;
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      setDetail(await getMethodStatStatsDetail(key));
    } finally {
      setDetailLoading(false);
    }
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
      className: "method-stat-col-method is-sticky-left",
      headerClassName: "method-stat-col-method is-sticky-left",
      render: (row) => (
        <BzOverflowTooltip text={row.methodSignature}>
          <span className="method-stat-method">{formatMethodDisplay(row)}</span>
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
        className="method-stat-page"
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
              onSubmit={(event) => {
                event.preventDefault();
                applyFilters();
              }}
            >
              <BzFormItem className="admin-query-field">
                <div className="admin-query-field__label">方法</div>
                <div className="admin-query-field__control">
                  <BzInput
                    modelValue={methodNameDraft}
                    placeholder="按包名、类名、方法名或签名搜索"
                    clearable
                    onValueChange={setMethodNameDraft}
                    onKeyUp={(event) => {
                      if (event.key === "Enter") applyFilters();
                    }}
                  />
                </div>
              </BzFormItem>
              <BzFormItem className="admin-query-field">
                <div className="admin-query-field__label">采集状态</div>
                <div className="admin-query-field__control">
                  <BzSelect
                    modelValue={collectStatusDraft}
                    placeholder="全部"
                    clearable
                    onValueChange={(value) =>
                      setCollectStatusDraft((value ?? "") as CollectStatusFilter)
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
                </div>
              </BzFormItem>
              <BzFormItem className="admin-query-field">
                <div className="admin-query-field__label">排序字段</div>
                <div className="admin-query-field__control">
                  <BzSelect
                    modelValue={sortByDraft}
                    onValueChange={(value) =>
                      setSortByDraft((value ?? "TOTAL_CALLS") as MethodStatSortBy)
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
                </div>
              </BzFormItem>
              <BzFormItem className="admin-query-field">
                <div className="admin-query-field__label">排序方向</div>
                <div className="admin-query-field__control">
                  <BzSelect
                    modelValue={sortDirectionDraft}
                    onValueChange={(value) =>
                      setSortDirectionDraft((value ?? "DESC") as MethodStatSortDirection)
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
        businessActions={
          <>
            {canSwitchView ? (
              <BzButton
                className="method-stat-global-action"
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
          <div className="method-stat-table-scope">
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
          page.totalElements > 0 ? (
            <div className="dict-pagination-bar admin-list-table-footer">
              <div className="dict-pagination-summary">共 {page.totalElements} 条记录</div>
              <div className="dict-pagination-right">
                <BzPagination
                  total={page.totalElements}
                  pageSize={pageSize}
                  currentPage={pageNo}
                  pageSizes={PAGE_SIZE_OPTIONS}
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
        title="方法统计详情"
        width="1080px"
        loading={detailLoading}
        className="role-manage-drawer method-stat-detail-drawer"
        onClose={() => setDetailOpen(false)}
        footer={<BzButton onClick={() => setDetailOpen(false)}>关闭</BzButton>}
      >
        {detail ? (
          <div className="role-manage-shell">
            <section className="role-manage-section">
              <div className="role-manage-section__head">
                <div className="role-manage-section__title">方法信息</div>
              </div>
              <div className="role-info-table-wrap">
                <table
                  className="role-info-table"
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
                        className="method-stat-detail-code"
                        colSpan={3}
                      >
                        {detail.methodSignature}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section className="role-manage-section">
              <div className="role-manage-section__head">
                <div className="role-manage-section__title">采集状态</div>
              </div>
              <div className="role-info-table-wrap">
                <table
                  className="role-info-table"
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

            <section className="role-manage-section">
              <div className="role-manage-section__head">
                <div className="role-manage-section__title">统计指标</div>
              </div>
              <div className="role-info-table-wrap">
                <table
                  className="role-info-table method-stat-metrics-table"
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
