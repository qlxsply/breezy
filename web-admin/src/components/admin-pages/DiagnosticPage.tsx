"use client";

import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { AdminDetailTable, type AdminDetailSection } from "@admin/components/admin/AdminDetailTable";
import {
  getDiagnosticCapabilities,
  getDiagnosticEvents,
  getDiagnosticHistory,
  getDiagnosticStatus,
  getLatestDiagnosticSnapshot,
  startDiagnostic,
  stopDiagnostic,
  updateDiagnosticConfig,
} from "@admin/api/diagnostic";
import { formatDateTime, formatDecimal } from "@admin/core/formatter";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type {
  DiagnosticCapability,
  DiagnosticConfigPayload,
  DiagnosticEvent,
  DiagnosticItem,
  DiagnosticSession,
  DiagnosticSnapshot,
} from "@admin/types/diagnostic";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzCheckbox } from "../bz/BzCheckbox";
import { BzEmpty } from "../bz/BzEmpty";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzSwitch } from "../bz/BzSwitch";
import type { BzTableColumn } from "../bz/BzTable";
import { BzTable } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

const itemOptions: Array<{ label: string; value: DiagnosticItem; description: string }> = [
  { label: "JVM", value: "JVM", description: "关注堆、非堆、GC 与进程 CPU。" },
  { label: "操作系统", value: "OS", description: "采集主机、CPU、内存与磁盘概况。" },
  { label: "线程", value: "THREAD", description: "采集线程数量、阻塞、等待与死锁情况。" },
  { label: "HTTP", value: "HTTP", description: "统计请求量、延迟分位与慢请求。" },
  { label: "连接池", value: "DB_POOL", description: "采集连接池活跃连接、空闲连接与等待线程。" },
  { label: "SQL", value: "SQL", description: "聚合 SQL 执行次数、耗时与慢 SQL。" },
  { label: "JFR", value: "JFR", description: "采集 JFR 事件，如 GC、异常与线程阻塞。" },
];

function formatBytes(value: number | null | undefined): string {
  if (value === null || value === undefined || value < 0) {
    return "-";
  }
  if (value < 1024) {
    return `${value} B`;
  }
  const units = ["KB", "MB", "GB", "TB"];
  let current = value / 1024;
  let unitIndex = 0;
  while (current >= 1024 && unitIndex < units.length - 1) {
    current /= 1024;
    unitIndex++;
  }
  return `${formatDecimal(current)} ${units[unitIndex]}`;
}

function formatPercent(value: number | null | undefined): string {
  if (value === null || value === undefined || value < 0) {
    return "-";
  }
  return `${(value * 100).toFixed(2)}%`;
}

function formatInteger(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  return value.toLocaleString("zh-CN");
}

function renderEventDetails(details: Record<string, unknown>): string {
  const entries = Object.entries(details ?? {});
  if (entries.length === 0) {
    return "-";
  }
  return entries
    .slice(0, 4)
    .map(([key, value]) => `${key}=${String(value)}`)
    .join(" | ");
}

function createDefaultConfig(): DiagnosticConfigPayload {
  return {
    intervalMs: 5000,
    historyCapacity: 180,
    eventCapacity: 300,
    items: ["JVM", "OS", "THREAD", "HTTP", "DB_POOL", "SQL", "JFR"],
    deepMode: false,
    slowRequestThresholdMs: 1000,
    slowSqlThresholdMs: 500,
    ttlSeconds: 1800,
  };
}

function ToolIcon({ kind }: { kind: "refresh" | "settings" | "start" | "stop" }) {
  if (kind === "refresh") {
    return (
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M20 11a8 8 0 0 0-13.66-5.66L4 8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M4 4v4h4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M4 13a8 8 0 0 0 13.66 5.66L20 16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M20 20v-4h-4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }
  if (kind === "settings") {
    return (
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M10.4 2.8h3.2l.64 2.27c.35.11.69.25 1.02.42l2.08-1.04 2.26 2.26-1.04 2.08c.17.33.31.67.42 1.02l2.27.64v3.2l-2.27.64c-.11.35-.25.69-.42 1.02l1.04 2.08-2.26 2.26-2.08-1.04c-.33.17-.67.31-1.02.42l-.64 2.27h-3.2l-.64-2.27a6.8 6.8 0 0 1-1.02-.42l-2.08 1.04-2.26-2.26 1.04-2.08a6.8 6.8 0 0 1-.42-1.02l-2.27-.64v-3.2l2.27-.64c.11-.35.25-.69.42-1.02L4.45 6.71l2.26-2.26 2.08 1.04c.33-.17.67-.31 1.02-.42z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
        <circle cx="12" cy="12" r="3.2" stroke="currentColor" strokeWidth="1.8" />
      </svg>
    );
  }
  if (kind === "start") {
    return (
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M8 6.5v11l9-5.5-9-5.5z" fill="currentColor" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="7" y="7" width="10" height="10" rx="1.5" fill="currentColor" />
    </svg>
  );
}

function ToolButton({
  title,
  kind,
  disabled = false,
  active = false,
  danger = false,
  onClick,
}: {
  title: string;
  kind: "refresh" | "settings" | "start" | "stop";
  disabled?: boolean;
  active?: boolean;
  danger?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      className={`admin-vben-circle-button diagnostic-tool-button${active ? " is-active" : ""}${danger ? " is-danger" : ""}`}
      type="button"
      title={title}
      aria-label={title}
      disabled={disabled}
      onClick={onClick}
    >
      <ToolIcon kind={kind} />
    </button>
  );
}

export function DiagnosticPage() {
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [status, setStatus] = useState<DiagnosticSession | null>(null);
  const [capability, setCapability] = useState<DiagnosticCapability | null>(null);
  const [latestSnapshot, setLatestSnapshot] = useState<DiagnosticSnapshot | null>(null);
  const [events, setEvents] = useState<DiagnosticEvent[]>([]);
  const [history, setHistory] = useState<DiagnosticSnapshot[]>([]);
  const [config, setConfig] = useState<DiagnosticConfigPayload>(createDefaultConfig());
  const [drawerForm, setDrawerForm] = useState<DiagnosticConfigPayload>(createDefaultConfig());

  const canView = hasResourceCodeAccess("diagnostic-view");
  const canStart = hasResourceCodeAccess("diagnostic-start");
  const canEdit = hasResourceCodeAccess("diagnostic-edit");
  const canStop = hasResourceCodeAccess("diagnostic-stop");
  const isActive = status?.status === "ACTIVE";

  const canViewRef = useRef(canView);
  canViewRef.current = canView;

  const syncForm = useCallback((nextStatus: DiagnosticSession) => {
    const nextConfig = {
      intervalMs: nextStatus.config.intervalMs,
      historyCapacity: nextStatus.config.historyCapacity,
      eventCapacity: nextStatus.config.eventCapacity,
      items: [...nextStatus.config.items],
      deepMode: nextStatus.config.deepMode,
      slowRequestThresholdMs: nextStatus.config.slowRequestThresholdMs,
      slowSqlThresholdMs: nextStatus.config.slowSqlThresholdMs,
      ttlSeconds: nextStatus.config.ttlSeconds,
    };
    setConfig(nextConfig);
    setDrawerForm(nextConfig);
  }, []);

  const reloadAll = useCallback(
    async (showLoading = true) => {
      if (!canViewRef.current) {
        return;
      }
      if (showLoading) {
        setLoading(true);
      }
      try {
        const [nextStatus, nextCapability] = await Promise.all([
          getDiagnosticStatus(),
          getDiagnosticCapabilities(),
        ]);
        setStatus(nextStatus);
        setCapability(nextCapability);
        syncForm(nextStatus);

        const [nextSnapshot, nextEvents, nextHistory] = await Promise.all([
          getLatestDiagnosticSnapshot(),
          getDiagnosticEvents(60),
          getDiagnosticHistory(30),
        ]);
        setLatestSnapshot(nextSnapshot);
        setEvents(nextEvents);
        setHistory(nextHistory);
      } finally {
        if (showLoading) {
          setLoading(false);
        }
      }
    },
    [syncForm],
  );

  useEffect(() => {
    void reloadAll(true);
    const timer = window.setInterval(() => {
      if (canViewRef.current) {
        void reloadAll(false);
      }
    }, 5000);
    return () => {
      window.clearInterval(timer);
    };
  }, [reloadAll]);

  async function handleStart() {
    setActionLoading(true);
    try {
      const result = await startDiagnostic({ ...config, items: [...config.items] });
      setStatus(result);
      message.success("运行时诊断已开启");
      await reloadAll(false);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleUpdate() {
    setActionLoading(true);
    try {
      const payload = { ...drawerForm, items: [...drawerForm.items] };
      const result = await updateDiagnosticConfig(payload);
      setStatus(result);
      syncForm(result);
      setDrawerOpen(false);
      message.success("诊断配置已更新");
      await reloadAll(false);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleStop() {
    setActionLoading(true);
    try {
      await stopDiagnostic();
      message.success("运行时诊断已停止");
      await reloadAll(false);
    } finally {
      setActionLoading(false);
    }
  }

  function openDrawer() {
    setDrawerForm({ ...config, items: [...config.items] });
    setDrawerOpen(true);
  }

  function toggleDrawerItem(item: DiagnosticItem, checked: boolean) {
    setDrawerForm((prev) => {
      if (checked) {
        return { ...prev, items: [...prev.items, item] };
      }
      return { ...prev, items: prev.items.filter((value) => value !== item) };
    });
  }

  const activeItems = useMemo(() => new Set(drawerForm.items), [drawerForm.items]);

  const eventColumns: Array<BzTableColumn<DiagnosticEvent>> = [
    { key: "type", title: "类型", width: 180, render: (row) => row.type },
    { key: "title", title: "标题", width: 140, render: (row) => row.title },
    {
      key: "content",
      title: "内容",
      minWidth: 260,
      render: (row) => (
        <div>
          <div>{row.message || "-"}</div>
          <div className="event-details">{renderEventDetails(row.details)}</div>
        </div>
      ),
    },
    {
      key: "time",
      title: "时间",
      width: 180,
      render: (row) => formatDateTime(row.happenedAt),
    },
  ];

  const historyColumns: Array<BzTableColumn<DiagnosticSnapshot>> = [
    {
      key: "capturedAt",
      title: "采样时间",
      width: 180,
      render: (row) => formatDateTime(row.capturedAt),
    },
    {
      key: "heap",
      title: "堆使用",
      width: 160,
      render: (row) => formatBytes(row.jvm?.heapUsedBytes),
    },
    {
      key: "cpu",
      title: "进程 CPU",
      width: 140,
      render: (row) => formatPercent(row.jvm?.processCpuLoad),
    },
    {
      key: "http",
      title: "HTTP",
      width: 180,
      render: (row) => `${row.http?.totalRequests ?? 0} / ${row.http?.inFlightRequests ?? 0}`,
    },
    {
      key: "pool",
      title: "连接池",
      width: 160,
      render: (row) => `${row.dbPool?.activeConnections ?? 0} / ${row.dbPool?.totalConnections ?? 0}`,
    },
    {
      key: "sql",
      title: "SQL",
      width: 160,
      render: (row) => `${row.sql?.totalExecutions ?? 0} 次`,
    },
  ];

  const itemConfigColumns: Array<BzTableColumn<(typeof itemOptions)[number]>> = [
    {
      key: "enabled",
      title: "启用",
      width: 68,
      render: (row) => {
        const checked = activeItems.has(row.value);
        return <BzCheckbox modelValue={checked} onValueChange={(value) => toggleDrawerItem(row.value, value)} />;
      },
    },
    {
      key: "label",
      title: "采集项",
      width: 120,
      render: (row) => row.label,
    },
    {
      key: "description",
      title: "说明",
      minWidth: 260,
      render: (row) => row.description,
    },
    {
      key: "threshold",
      title: "专属配置",
      width: 260,
      render: (row) => {
        const checked = activeItems.has(row.value);
        if (row.value === "HTTP") {
          return (
            <div className="admin-detail-form-control">
              <BzInput
                disabled={!checked}
                modelValue={drawerForm.slowRequestThresholdMs}
                type="number"
                onValueChange={(value) =>
                  setDrawerForm((prev) => ({ ...prev, slowRequestThresholdMs: Number(value) }))
                }
              />
            </div>
          );
        }
        if (row.value === "SQL") {
          return (
            <div className="admin-detail-form-control">
              <BzInput
                disabled={!checked}
                modelValue={drawerForm.slowSqlThresholdMs}
                type="number"
                onValueChange={(value) =>
                  setDrawerForm((prev) => ({ ...prev, slowSqlThresholdMs: Number(value) }))
                }
              />
            </div>
          );
        }
        return <span className="text-muted">无专属阈值配置</span>;
      },
    },
  ];

  const runtimeDetails = [
    {
      title: "JVM",
      rows: [
        ["堆使用", formatBytes(latestSnapshot?.jvm?.heapUsedBytes)],
        ["非堆使用", formatBytes(latestSnapshot?.jvm?.nonHeapUsedBytes)],
        ["GC 总耗时", `${latestSnapshot?.jvm?.gcCollectionTimeMs ?? 0} ms`],
        ["进程 CPU", formatPercent(latestSnapshot?.jvm?.processCpuLoad)],
      ],
    },
    {
      title: "线程",
      rows: [
        ["总线程数", formatInteger(latestSnapshot?.thread?.threadCount)],
        ["峰值线程", formatInteger(latestSnapshot?.thread?.peakThreadCount)],
        ["阻塞线程", formatInteger(latestSnapshot?.thread?.blockedCount)],
        ["等待线程", formatInteger(latestSnapshot?.thread?.waitingCount)],
      ],
    },
    {
      title: "HTTP",
      rows: [
        ["总请求", formatInteger(latestSnapshot?.http?.totalRequests)],
        ["进行中", formatInteger(latestSnapshot?.http?.inFlightRequests)],
        ["慢请求", formatInteger(latestSnapshot?.http?.slowRequestCount)],
        ["错误请求", formatInteger(latestSnapshot?.http?.errorRequestCount)],
      ],
    },
    {
      title: "数据库",
      rows: [
        ["连接池数", formatInteger(latestSnapshot?.dbPool?.poolCount)],
        ["活跃连接", formatInteger(latestSnapshot?.dbPool?.activeConnections)],
        ["等待线程", formatInteger(latestSnapshot?.dbPool?.waitingThreads)],
        ["SQL 次数", formatInteger(latestSnapshot?.sql?.totalExecutions)],
      ],
    },
  ];

  const overviewSections = useMemo<AdminDetailSection[]>(() => [
    {
      title: "运行概览",
      fields: [
        { label: "运行状态", value: isActive ? "采集中" : "未开启" },
        { label: "剩余 TTL", value: `${status?.remainingTtlSeconds ?? 0} 秒` },
        { label: "最近采样", value: latestSnapshot?.capturedAt ? formatDateTime(latestSnapshot.capturedAt) : "-" },
        { label: "JFR", value: capability?.jfrAvailable ? "可用" : "不可用" },
        { label: "数据源", value: capability?.dataSourceNames?.length ? capability.dataSourceNames.join(" / ") : "-", span: "full" },
        { label: "采集项", value: status?.config.items.length ? status.config.items.join(" / ") : "-", span: "full", multiline: true },
      ],
    },
    {
      title: "核心指标",
      fields: [
        { label: "JVM 堆使用", value: formatBytes(latestSnapshot?.jvm?.heapUsedBytes) },
        { label: "进程 CPU", value: formatPercent(latestSnapshot?.jvm?.processCpuLoad) },
        { label: "线程数", value: formatInteger(latestSnapshot?.thread?.threadCount) },
        { label: "阻塞线程", value: formatInteger(latestSnapshot?.thread?.blockedCount) },
        { label: "P95 延迟", value: `${latestSnapshot?.http?.p95DurationMs ?? 0} ms` },
        { label: "慢 SQL", value: formatInteger(latestSnapshot?.sql?.slowSqlCount) },
      ],
    },
    ...runtimeDetails.map((section) => ({
      title: section.title,
      fields: section.rows.map(([label, value]) => ({ label, value })),
    })),
  ], [capability?.dataSourceNames, capability?.jfrAvailable, isActive, latestSnapshot, runtimeDetails, status?.config.items, status?.remainingTtlSeconds]);

  const drawerFooter = (
    <>
      <BzButton onClick={() => setDrawerOpen(false)}>取消</BzButton>
      <BzButton buttonType="primary" loading={actionLoading} onClick={handleUpdate}>
        确认
      </BzButton>
    </>
  );

  const configSections = useMemo<AdminDetailSection[]>(() => [
    {
      title: "通用配置",
      fields: [
        {
          label: "采样间隔(ms)",
          value: (
            <div className="admin-detail-form-control">
              <BzInput modelValue={drawerForm.intervalMs} type="number" onValueChange={(value) => setDrawerForm((prev) => ({ ...prev, intervalMs: Number(value) }))} />
            </div>
          ),
        },
        {
          label: "历史容量",
          value: (
            <div className="admin-detail-form-control">
              <BzInput modelValue={drawerForm.historyCapacity} type="number" onValueChange={(value) => setDrawerForm((prev) => ({ ...prev, historyCapacity: Number(value) }))} />
            </div>
          ),
        },
        {
          label: "事件容量",
          value: (
            <div className="admin-detail-form-control">
              <BzInput modelValue={drawerForm.eventCapacity} type="number" onValueChange={(value) => setDrawerForm((prev) => ({ ...prev, eventCapacity: Number(value) }))} />
            </div>
          ),
        },
        {
          label: "最长持续时间(秒)",
          value: (
            <div className="admin-detail-form-control">
              <BzInput modelValue={drawerForm.ttlSeconds} type="number" onValueChange={(value) => setDrawerForm((prev) => ({ ...prev, ttlSeconds: Number(value) }))} />
            </div>
          ),
        },
        {
          label: "深度模式",
          value: <BzSwitch modelValue={drawerForm.deepMode} onValueChange={(value) => setDrawerForm((prev) => ({ ...prev, deepMode: value }))} />,
        },
        {
          label: "模式说明",
          value: "适合短时间排障，会带来更高采样成本。",
          span: "full",
          multiline: true,
        },
      ],
    },
  ], [drawerForm.deepMode, drawerForm.eventCapacity, drawerForm.historyCapacity, drawerForm.intervalMs, drawerForm.ttlSeconds]);

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          <BzCard
            className="admin-panel admin-table-card admin-list-card"
            shadow="never"
          >
            <div className="admin-list-region">
              <div className="admin-list-toolbar-row">
                <div className="admin-list-business-actions">
                  <span className="admin-batch-toolbar__summary">{isActive ? "运行时诊断采集中" : "运行时诊断未开启"}</span>
                </div>
                <div className="admin-list-query-tools">
                  {canEdit && isActive ? <ToolButton title="诊断设置" kind="settings" disabled={actionLoading} onClick={openDrawer} /> : null}
                  {canStart && !isActive ? <ToolButton title="开启诊断" kind="start" disabled={actionLoading} onClick={() => void handleStart()} /> : null}
                  {canStop && isActive ? <ToolButton title="停止诊断" kind="stop" active disabled={actionLoading} onClick={() => void handleStop()} /> : null}
                  {canView ? <ToolButton title="刷新数据" kind="refresh" disabled={loading || actionLoading} onClick={() => void reloadAll(true)} /> : null}
                </div>
              </div>
            {!canView ? (
              <BzEmpty description="无权限查看运行时诊断" />
            ) : (
              <div className="admin-page-stack diagnostic-page-body">
                <AdminDetailTable sections={overviewSections} />
                <section className="admin-selection-section diagnostic-section-card">
                  <div className="admin-selection-section__header">
                    <div className="admin-selection-section__title">最近事件</div>
                    <div className="admin-selection-section__meta">最新 {events.length} 条</div>
                  </div>
                  <div className="admin-selection-table-wrap">
                    <BzTable loading={loading} data={events} columns={eventColumns} emptyText="暂无事件" size="small" />
                  </div>
                </section>
                <section className="admin-selection-section diagnostic-section-card">
                  <div className="admin-selection-section__header">
                    <div className="admin-selection-section__title">快照历史</div>
                    <div className="admin-selection-section__meta">最新 {history.length} 条</div>
                  </div>
                  <div className="admin-selection-table-wrap">
                    <BzTable loading={loading} data={history} columns={historyColumns} emptyText="暂无快照" size="small" />
                  </div>
                </section>
              </div>
            )}
            </div>
          </BzCard>
        </div>
      </div>

      <AdminEntityDrawer
        open={drawerOpen}
        title="诊断设置"
        width="560px"
        loading={actionLoading}
        onClose={() => setDrawerOpen(false)}
        footer={drawerFooter}
      >
        <div className="diagnostic-drawer-shell">
          <AdminDetailTable sections={configSections} />
          <section className="admin-selection-section diagnostic-config-block">
            <div className="admin-selection-section__header">
              <div className="admin-selection-section__title">采集项配置</div>
              <div className="admin-selection-section__meta">共 {itemOptions.length} 项</div>
            </div>
            <div className="admin-selection-table-wrap">
              <BzTable data={itemOptions} columns={itemConfigColumns} rowKey="value" emptyText="暂无采集项" size="small" />
            </div>
          </section>
        </div>
      </AdminEntityDrawer>

    </div>
  );
}
