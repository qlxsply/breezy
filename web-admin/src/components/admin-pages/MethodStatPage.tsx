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
import {
  BzButton,
  BzCard,
  BzDialog,
  BzEmpty,
  BzForm,
  BzFormItem,
  BzInput,
  BzLoading,
  BzOption,
  BzPagination,
  BzSelect,
  BzSwitch,
  BzTable,
  type BzTableColumn,
  BzTag,
  BzTooltip,
} from "@admin/components/bz";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type {
  MethodStatMatchMode,
  MethodStatSortBy,
  MethodStatSortDirection,
  MethodStatStatsItem,
} from "@admin/types/method-stat";
import type { PageResult } from "@admin/types/page";
import { useEffect, useMemo, useState } from "react";

const SORT_OPTIONS: Array<{ label: string; value: MethodStatSortBy }> = [
  { label: "累计调用", value: "TOTAL_CALLS" },
  { label: "累计成功", value: "TOTAL_SUCCESS" },
  { label: "累计失败", value: "TOTAL_FAILURE" },
  { label: "1分钟调用", value: "RECENT_1M_CALLS" },
  { label: "1小时调用", value: "RECENT_1H_CALLS" },
  { label: "1天调用", value: "RECENT_1D_CALLS" },
  { label: "耗时平均", value: "DURATION_AVG" },
  { label: "耗时P95", value: "DURATION_P95" },
  { label: "耗时P99", value: "DURATION_P99" },
  { label: "方法名", value: "METHOD_NAME" },
  { label: "唯一Key", value: "KEY" },
];

function normalizeText(value: string): string | undefined {
  const normalized = value.trim();
  if (!normalized) return undefined;
  return normalized;
}

function formatNumber(value: number): string {
  return value.toLocaleString("zh-CN");
}

function formatDecimal(value: number): string {
  return value.toFixed(2);
}

function formatSuccessRate(success: number, total: number): string {
  if (total <= 0) return "0.00%";
  return `${((success / total) * 100).toFixed(2)}%`;
}

function formatMethodDisplay(className: string, methodName: string): string {
  return `${className}.${methodName}`;
}

export function MethodStatPage() {
  const [statsLoading, setStatsLoading] = useState(false);
  const [globalSwitchLoading, setGlobalSwitchLoading] = useState(false);
  const [clearingAll, setClearingAll] = useState(false);
  const [batchSwitchLoading, setBatchSwitchLoading] = useState(false);

  const [methodSwitchLoadingKeys, setMethodSwitchLoadingKeys] = useState<Set<string>>(new Set());
  const [clearingMethodKeys, setClearingMethodKeys] = useState<Set<string>>(new Set());

  const [globalSwitchEnabled, setGlobalSwitchEnabled] = useState(false);

  const [methodName, setMethodName] = useState("");
  const [matchMode, setMatchMode] = useState<MethodStatMatchMode>("FUZZY");
  const [sortBy, setSortBy] = useState<MethodStatSortBy>("TOTAL_CALLS");
  const [sortDirection, setSortDirection] = useState<MethodStatSortDirection>("DESC");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [statsPage, setStatsPage] = useState<PageResult<MethodStatStatsItem>>({
    pageNo: 1,
    pageSize: 20,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });

  const [detailVisible, setDetailVisible] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailData, setDetailData] = useState<MethodStatStatsItem | null>(null);

  const canStatsView = hasResourceCodeAccess("method-stat-view");
  const canSwitchView = hasResourceCodeAccess("method-stat-switch-view");
  const canSwitchEdit = hasResourceCodeAccess("method-stat-switch-edit");
  const canStatClear = hasResourceCodeAccess("method-stat-clear");

  const statsTotalPages = useMemo(() => {
    if (pageSize <= 0) return 0;
    return Math.ceil(statsPage.totalElements / pageSize);
  }, [statsPage.totalElements, pageSize]);

  function addMethodSwitchLoading(key: string) {
    setMethodSwitchLoadingKeys((prev) => new Set(prev).add(key));
  }

  function removeMethodSwitchLoading(key: string) {
    setMethodSwitchLoadingKeys((prev) => {
      const next = new Set(prev);
      next.delete(key);
      return next;
    });
  }

  function isMethodSwitchLoading(key: string): boolean {
    return methodSwitchLoadingKeys.has(key);
  }

  function addMethodClearing(key: string) {
    setClearingMethodKeys((prev) => new Set(prev).add(key));
  }

  function removeMethodClearing(key: string) {
    setClearingMethodKeys((prev) => {
      const next = new Set(prev);
      next.delete(key);
      return next;
    });
  }

  function isClearingMethod(key: string): boolean {
    return clearingMethodKeys.has(key);
  }

  async function loadGlobalSwitch() {
    if (!canSwitchView) {
      setGlobalSwitchEnabled(false);
      return;
    }
    const state = await getMethodStatGlobalSwitch();
    setGlobalSwitchEnabled(state.enabled);
  }

  async function loadStats(pNo?: number) {
    if (!canStatsView) {
      setStatsPage({
        pageNo: 1,
        pageSize,
        numberOfElements: 0,
        totalPages: 0,
        totalElements: 0,
        elements: [],
      });
      return;
    }
    if (typeof pNo === "number") {
      setPageNo(pNo);
    }
    setStatsLoading(true);
    try {
      const page = await pageMethodStatStats({
        methodName: normalizeText(methodName),
        matchMode,
        page: { pageNo: typeof pNo === "number" ? pNo : pageNo, pageSize },
        sort: { orders: [{ field: sortBy, direction: sortDirection }] },
      });
      setStatsPage(page);
      setPageNo(page.pageNo || 1);
      setPageSize(page.pageSize || pageSize);
    } finally {
      setStatsLoading(false);
    }
  }

  async function handleSearch() {
    await loadStats(1);
  }

  async function handleReset() {
    setMethodName("");
    setMatchMode("FUZZY");
    setSortBy("TOTAL_CALLS");
    setSortDirection("DESC");
    setPageNo(1);
    setStatsLoading(true);
    try {
      const page = await pageMethodStatStats({
        methodName: undefined,
        matchMode: "FUZZY",
        page: { pageNo: 1, pageSize },
        sort: {
          orders: [
            {
              field: "TOTAL_CALLS" as MethodStatSortBy,
              direction: "DESC" as MethodStatSortDirection,
            },
          ],
        },
      });
      setStatsPage(page);
      setPageNo(page.pageNo || 1);
    } finally {
      setStatsLoading(false);
    }
  }

  async function handleGlobalSwitchChange(next: boolean) {
    if (!canSwitchEdit) {
      setGlobalSwitchEnabled(!next);
      message.warning("无权限维护统计开关");
      return;
    }
    if (globalSwitchLoading) return;

    setGlobalSwitchLoading(true);
    try {
      const updated = await updateMethodStatGlobalSwitch(next);
      setGlobalSwitchEnabled(updated.enabled);
      message.success(updated.enabled ? "采集功能已开启" : "采集功能已关闭");
      await loadStats();
      if (!updated.enabled) {
        setDetailVisible(false);
        return;
      }
      if (detailVisible && detailData) {
        await openStatsDetail(detailData.key);
      }
    } catch {
      setGlobalSwitchEnabled(!next);
    } finally {
      setGlobalSwitchLoading(false);
    }
  }

  async function handleToggleMethodSwitch(row: MethodStatStatsItem) {
    await handleMethodSwitchChange(row, !row.methodSwitchEnabled);
  }

  async function handleMethodSwitchChange(row: MethodStatStatsItem, next: boolean) {
    if (!canSwitchEdit) {
      message.warning("无权限维护方法开关");
      return;
    }
    if (isMethodSwitchLoading(row.key)) return;

    const previous = row.methodSwitchEnabled;

    setStatsPage((prev) => ({
      ...prev,
      elements: prev.elements.map((e) =>
        e.key === row.key
          ? { ...e, methodSwitchEnabled: next, collectEnabled: next && globalSwitchEnabled }
          : e,
      ),
    }));

    addMethodSwitchLoading(row.key);
    try {
      await updateMethodStatMethodSwitch(row.key, next);
      message.success(next ? "方法统计已开启" : "方法统计已关闭");
      await loadStats();
      if (detailVisible && detailData?.key === row.key) {
        await openStatsDetail(row.key);
      }
    } catch {
      setStatsPage((prev) => ({
        ...prev,
        elements: prev.elements.map((e) =>
          e.key === row.key
            ? {
                ...e,
                methodSwitchEnabled: previous,
                collectEnabled: previous && globalSwitchEnabled,
              }
            : e,
        ),
      }));
    } finally {
      removeMethodSwitchLoading(row.key);
    }
  }

  async function handleSetAllMethodSwitch(enabled: boolean) {
    if (!canSwitchEdit) {
      message.warning("无权限维护方法开关");
      return;
    }
    if (!globalSwitchEnabled) {
      message.warning("请先开启采集功能");
      return;
    }
    if (batchSwitchLoading) return;

    setBatchSwitchLoading(true);
    try {
      await updateAllMethodStatMethodSwitch(enabled);
      message.success(enabled ? "已开启全部方法采集" : "已关闭全部方法采集");
      await loadStats();
      if (detailVisible && detailData) {
        await openStatsDetail(detailData.key);
      }
    } finally {
      setBatchSwitchLoading(false);
    }
  }

  async function handleClearMethodStats(key: string) {
    if (!canStatClear) {
      message.warning("无权限清空统计数据");
      return;
    }
    if (isClearingMethod(key)) return;

    addMethodClearing(key);
    try {
      await clearMethodStat(key);
      message.success("方法统计已清空");
      await loadStats();
      if (detailVisible && detailData?.key === key) {
        await openStatsDetail(key);
      }
    } finally {
      removeMethodClearing(key);
    }
  }

  async function handleClearAllStats() {
    if (!canStatClear) {
      message.warning("无权限清空统计数据");
      return;
    }
    if (clearingAll) return;

    setClearingAll(true);
    try {
      await clearAllMethodStat();
      message.success("统计数据已清空");
      await loadStats();
      if (detailVisible && detailData) {
        await openStatsDetail(detailData.key);
      }
    } finally {
      setClearingAll(false);
    }
  }

  async function openStatsDetail(key: string) {
    if (!canStatsView) {
      message.warning("无权限查看统计详情");
      return;
    }
    setDetailVisible(true);
    setDetailLoading(true);
    try {
      setDetailData(await getMethodStatStatsDetail(key));
    } finally {
      setDetailLoading(false);
    }
  }

  function closeStatsDetail() {
    setDetailVisible(false);
  }

  async function initializePage() {
    if (canSwitchView) {
      await loadGlobalSwitch();
    }
    await loadStats();
  }

  useEffect(() => {
    void initializePage();
  }, []);

  const columns: Array<BzTableColumn<MethodStatStatsItem>> = [
    {
      key: "method",
      title: "方法",
      minWidth: 260,
      render: (row) => (
        <BzTooltip content={row.methodSignature}>
          <span style={{ fontWeight: 600, lineHeight: 1.4 }}>
            {formatMethodDisplay(row.className, row.methodName)}
          </span>
        </BzTooltip>
      ),
    },
    {
      key: "status",
      title: "状态",
      width: 120,
      render: (row) => (
        <BzTag
          size="small"
          type={row.collectEnabled ? "success" : "info"}
        >
          {row.collectEnabled ? "采集中" : "已关闭"}
        </BzTag>
      ),
    },
    {
      key: "totalCalls",
      title: "累计调用",
      width: 120,
      render: (row) => <>{formatNumber(row.totalCalls)}</>,
    },
    {
      key: "windowCalls",
      title: "窗口调用",
      minWidth: 170,
      render: (row) => (
        <div>
          <div style={{ fontSize: 12, lineHeight: 1.45 }}>
            1分：{formatNumber(row.recent1MinuteCalls)}
          </div>
          <div style={{ fontSize: 12, lineHeight: 1.45 }}>
            1时：{formatNumber(row.recent1HourCalls)}
          </div>
          <div style={{ fontSize: 12, lineHeight: 1.45 }}>
            1天：{formatNumber(row.recent1DayCalls)}
          </div>
        </div>
      ),
    },
    {
      key: "successFailure",
      title: "成功 / 失败",
      minWidth: 160,
      render: (row) => (
        <div>
          <div style={{ fontSize: 12, lineHeight: 1.45, color: "#16a34a" }}>
            成功：{formatNumber(row.totalSuccess)}
          </div>
          <div style={{ fontSize: 12, lineHeight: 1.45, color: "#dc2626" }}>
            失败：{formatNumber(row.totalFailure)}
          </div>
          <div style={{ fontSize: 12, lineHeight: 1.45 }}>
            成功率：{formatSuccessRate(row.totalSuccess, row.totalCalls)}
          </div>
        </div>
      ),
    },
    {
      key: "duration",
      title: "耗时(ms)",
      minWidth: 180,
      render: (row) => (
        <div>
          <div style={{ fontSize: 12, lineHeight: 1.45 }}>
            avg：{formatDecimal(row.durationAvg)}
          </div>
          <div style={{ fontSize: 12, lineHeight: 1.45 }}>p95：{formatNumber(row.durationP95)}</div>
          <div style={{ fontSize: 12, lineHeight: 1.45 }}>max：{formatNumber(row.durationMax)}</div>
          <div style={{ fontSize: 12, lineHeight: 1.45 }}>
            样本：{formatNumber(row.durationSampleSize)}
          </div>
        </div>
      ),
    },
    {
      key: "actions",
      title: "操作",
      width: 260,
      className: "is-fixed-right",
      render: (row) => (
        <div style={{ display: "inline-flex", alignItems: "center", gap: 8 }}>
          {canSwitchEdit ? (
            <BzButton
              size="small"
              disabled={isMethodSwitchLoading(row.key)}
              loading={isMethodSwitchLoading(row.key)}
              onClick={() => handleToggleMethodSwitch(row)}
            >
              {row.methodSwitchEnabled ? "关闭" : "开启"}
            </BzButton>
          ) : null}
          <BzButton
            size="small"
            onClick={() => void openStatsDetail(row.key)}
          >
            详情
          </BzButton>
          {canStatClear ? (
            <BzButton
              size="small"
              buttonType="danger"
              disabled={isClearingMethod(row.key)}
              loading={isClearingMethod(row.key)}
              onClick={() => void handleClearMethodStats(row.key)}
            >
              清空
            </BzButton>
          ) : null}
        </div>
      ),
    },
  ];

  return (
    <div className="admin-page">
      <div className="content">
        <div className="list-page-stack">
          <section className="list-page-actions">
            <div className="list-page-actions-main">
              {canSwitchView && canSwitchEdit ? (
                <>
                  <BzButton
                    disabled={!globalSwitchEnabled || batchSwitchLoading}
                    loading={batchSwitchLoading}
                    onClick={() => void handleSetAllMethodSwitch(true)}
                  >
                    全部开启
                  </BzButton>
                  <BzButton
                    disabled={!globalSwitchEnabled || batchSwitchLoading}
                    loading={batchSwitchLoading}
                    onClick={() => void handleSetAllMethodSwitch(false)}
                  >
                    全部关闭
                  </BzButton>
                </>
              ) : null}
              {canStatClear ? (
                <BzButton
                  loading={clearingAll}
                  onClick={() => void handleClearAllStats()}
                >
                  全部清空
                </BzButton>
              ) : null}
            </div>

            {canSwitchView && canSwitchEdit ? (
              <div style={{ display: "inline-flex", alignItems: "center", gap: 10 }}>
                <span style={{ fontSize: 13, color: "var(--text-muted, #64748b)" }}>采集功能</span>
                <BzSwitch
                  modelValue={globalSwitchEnabled}
                  disabled={globalSwitchLoading}
                  onValueChange={(value) => void handleGlobalSwitchChange(value)}
                />
              </div>
            ) : null}
          </section>

          <BzCard
            className="list-page-query-card"
            shadow="never"
          >
            <BzForm
              className="list-page-filter-form"
              inline
              onSubmit={(e) => e.preventDefault()}
            >
              <BzFormItem className="list-page-filter-item">
                <div className="list-page-filter-field">
                  <div className="list-page-filter-label">方法名</div>
                  <BzInput
                    className="list-page-filter-control"
                    modelValue={methodName}
                    placeholder="输入方法名"
                    clearable
                    onValueChange={setMethodName}
                    onKeyUp={(e) => {
                      if (e.key === "Enter") void handleSearch();
                    }}
                  />
                </div>
              </BzFormItem>

              <BzFormItem className="list-page-filter-item">
                <div className="list-page-filter-field">
                  <div className="list-page-filter-label">匹配模式</div>
                  <BzSelect
                    modelValue={matchMode}
                    className="match-mode-select"
                    onValueChange={(v) => setMatchMode((v ?? "FUZZY") as MethodStatMatchMode)}
                  >
                    <BzOption
                      label="模糊"
                      value="FUZZY"
                    />
                    <BzOption
                      label="精确"
                      value="EXACT"
                    />
                  </BzSelect>
                </div>
              </BzFormItem>

              <BzFormItem className="list-page-filter-item sort-field-item">
                <div className="list-page-filter-field">
                  <div className="list-page-filter-label">排序规则</div>
                  <div className="list-page-sort-group">
                    <BzSelect
                      modelValue={sortBy}
                      className="sort-field-select"
                      onValueChange={(v) => setSortBy((v ?? "TOTAL_CALLS") as MethodStatSortBy)}
                    >
                      {SORT_OPTIONS.map((option) => (
                        <BzOption
                          key={option.value}
                          label={option.label}
                          value={option.value}
                        />
                      ))}
                    </BzSelect>
                    <BzSelect
                      modelValue={sortDirection}
                      className="sort-order-select"
                      onValueChange={(v) =>
                        setSortDirection((v ?? "DESC") as MethodStatSortDirection)
                      }
                    >
                      <BzOption
                        label="升序"
                        value="ASC"
                      />
                      <BzOption
                        label="降序"
                        value="DESC"
                      />
                    </BzSelect>
                  </div>
                </div>
              </BzFormItem>

              <BzFormItem className="list-page-filter-actions">
                <BzButton
                  buttonType="primary"
                  onClick={() => void handleSearch()}
                >
                  搜索
                </BzButton>
                <BzButton onClick={() => void handleReset()}>重置</BzButton>
              </BzFormItem>
            </BzForm>
          </BzCard>

          <BzCard
            className="list-page-result-card"
            shadow="never"
          >
            {!canStatsView ? (
              <BzEmpty description="无权限查看统计结果" />
            ) : (
              <>
                <BzTable
                  data={statsPage.elements}
                  columns={columns}
                  loading={statsLoading}
                  emptyText="暂无统计数据"
                  size="small"
                />

                <div className="list-page-pagination">
                  <div className="list-page-pagination-summary">
                    <span>总计 {statsPage.totalElements} 项</span>
                    <span>，共 {statsTotalPages} 页</span>
                  </div>
                  {statsPage.totalElements > 0 ? (
                    <BzPagination
                      total={statsPage.totalElements}
                      pageSizes={[10, 20, 50, 100]}
                      pageSize={pageSize}
                      currentPage={pageNo}
                      onCurrentChange={(p) => {
                        setPageNo(p);
                        void loadStats(p);
                      }}
                      onSizeChange={(ps) => {
                        if (!Number.isFinite(ps) || ps <= 0 || ps === pageSize) return;
                        setPageSize(ps);
                        setPageNo(1);
                        void loadStats(1);
                      }}
                    />
                  ) : null}
                </div>
              </>
            )}
          </BzCard>
        </div>
      </div>

      <BzDialog
        modelValue={detailVisible}
        title="方法统计详情"
        width={760}
        onClose={closeStatsDetail}
        onUpdateModelValue={(v) => {
          if (!v) closeStatsDetail();
        }}
        footer={<BzButton onClick={closeStatsDetail}>关闭</BzButton>}
      >
        <BzLoading loading={detailLoading}>
          {detailData ? (
            <>
              <div style={{ marginBottom: 12 }}>
                <div style={{ fontSize: 12, color: "var(--text-muted, #64748b)", marginBottom: 4 }}>
                  包名
                </div>
                <div style={{ fontSize: 13, lineHeight: 1.5, wordBreak: "break-all" }}>
                  {detailData.packageName}
                </div>
              </div>
              <div style={{ marginBottom: 12 }}>
                <div style={{ fontSize: 12, color: "var(--text-muted, #64748b)", marginBottom: 4 }}>
                  类名
                </div>
                <div style={{ fontSize: 13, lineHeight: 1.5, wordBreak: "break-all" }}>
                  {detailData.className}
                </div>
              </div>
              <div style={{ marginBottom: 12 }}>
                <div style={{ fontSize: 12, color: "var(--text-muted, #64748b)", marginBottom: 4 }}>
                  方法名
                </div>
                <div style={{ fontSize: 13, lineHeight: 1.5, wordBreak: "break-all" }}>
                  {detailData.methodName}
                </div>
              </div>
              <div style={{ marginBottom: 12 }}>
                <div style={{ fontSize: 12, color: "var(--text-muted, #64748b)", marginBottom: 4 }}>
                  唯一Key
                </div>
                <div
                  style={{
                    fontFamily:
                      "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, Liberation Mono, Courier New, monospace",
                    fontSize: 12,
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    display: "inline-block",
                    maxWidth: "100%",
                  }}
                >
                  {detailData.key}
                </div>
              </div>

              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
                  gap: 8,
                }}
              >
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    累计调用
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatNumber(detailData.totalCalls)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    累计成功
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatNumber(detailData.totalSuccess)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    累计失败
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatNumber(detailData.totalFailure)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    1分钟调用
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatNumber(detailData.recent1MinuteCalls)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    1小时调用
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatNumber(detailData.recent1HourCalls)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>1天调用</span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatNumber(detailData.recent1DayCalls)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    耗时avg(ms)
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatDecimal(detailData.durationAvg)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    耗时p95(ms)
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatNumber(detailData.durationP95)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    耗时max(ms)
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatNumber(detailData.durationMax)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>样本数</span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {formatNumber(detailData.durationSampleSize)}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    方法开关
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {detailData.methodSwitchEnabled ? "开启" : "关闭"}
                  </span>
                </div>
                <div
                  style={{
                    border: "1px solid var(--border-color, #e5e7eb)",
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <span style={{ fontSize: 12, color: "var(--text-muted, #64748b)" }}>
                    采集状态
                  </span>
                  <span
                    style={{ fontSize: 13, color: "var(--text-main, #0f172a)", fontWeight: 600 }}
                  >
                    {detailData.collectEnabled ? "采集中" : "已关闭"}
                  </span>
                </div>
              </div>
            </>
          ) : null}
        </BzLoading>
      </BzDialog>
    </div>
  );
}
