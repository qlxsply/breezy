"use client";

import { usePermission } from "@admin/features/resources/permissions";
import { message } from "@admin/shared/lib/feedback/message";
import { formatDateTime, formatDecimal } from "@admin/shared/lib/formatter";
import { type AdminDetailSection, AdminDetailTable } from "@admin/shared/ui/admin/AdminDetailTable";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import { BzCard } from "@admin/shared/ui/bz/BzCard";
import { BzCheckbox } from "@admin/shared/ui/bz/BzCheckbox";
import { BzEmpty } from "@admin/shared/ui/bz/BzEmpty";
import { BzInput } from "@admin/shared/ui/bz/BzInput";
import { BzSwitch } from "@admin/shared/ui/bz/BzSwitch";
import type { BzTableColumn } from "@admin/shared/ui/bz/BzTable";
import { BzTable } from "@admin/shared/ui/bz/BzTable";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import {
  getDiagnosticCapabilities,
  getDiagnosticEvents,
  getDiagnosticHistory,
  getDiagnosticStatus,
  getLatestDiagnosticSnapshot,
  startDiagnostic,
  stopDiagnostic,
  updateDiagnosticConfig,
} from "./api/client";
import styles from "./DiagnosticPage.module.css";
import type {
  DiagnosticCapability,
  DiagnosticConfig,
  DiagnosticEvent,
  DiagnosticItem,
  DiagnosticSession,
  DiagnosticSnapshot,
} from "./model/types";
import { DIAGNOSTIC_PERMISSIONS } from "./permissions";

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

function createDefaultConfig(): DiagnosticConfig {
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

function copyConfig(config: DiagnosticSession["config"]): DiagnosticConfig {
  return { ...config, items: [...config.items] };
}

function isAbortError(cause: unknown): boolean {
  return cause instanceof DOMException && cause.name === "AbortError";
}

function ToolIcon({ kind }: { kind: "refresh" | "settings" | "start" | "stop" }) {
  if (kind === "refresh") {
    return (
      <svg
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <path
          d="M20 11a8 8 0 0 0-13.66-5.66L4 8"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M4 4v4h4"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M4 13a8 8 0 0 0 13.66 5.66L20 16"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M20 20v-4h-4"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    );
  }
  if (kind === "settings") {
    return (
      <svg
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <path
          d="M10.4 2.8h3.2l.64 2.27c.35.11.69.25 1.02.42l2.08-1.04 2.26 2.26-1.04 2.08c.17.33.31.67.42 1.02l2.27.64v3.2l-2.27.64c-.11.35-.25.69-.42 1.02l1.04 2.08-2.26 2.26-2.08-1.04c-.33.17-.67.31-1.02.42l-.64 2.27h-3.2l-.64-2.27a6.8 6.8 0 0 1-1.02-.42l-2.08 1.04-2.26-2.26 1.04-2.08a6.8 6.8 0 0 1-.42-1.02l-2.27-.64v-3.2l2.27-.64c.11-.35.25-.69.42-1.02L4.45 6.71l2.26-2.26 2.08 1.04c.33-.17.67-.31 1.02-.42z"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinejoin="round"
        />
        <circle
          cx="12"
          cy="12"
          r="3.2"
          stroke="currentColor"
          strokeWidth="1.8"
        />
      </svg>
    );
  }
  if (kind === "start") {
    return (
      <svg
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <path
          d="M8 6.5v11l9-5.5-9-5.5z"
          fill="currentColor"
        />
      </svg>
    );
  }
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <rect
        x="7"
        y="7"
        width="10"
        height="10"
        rx="1.5"
        fill="currentColor"
      />
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
      className={[
        layoutStyles.circleButton,
        styles.toolButton,
        active ? styles.active : "",
        danger ? styles.danger : "",
      ]
        .filter(Boolean)
        .join(" ")}
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
  const [config, setConfig] = useState<DiagnosticConfig>(createDefaultConfig());
  const [drawerForm, setDrawerForm] = useState<DiagnosticConfig>(createDefaultConfig());

  const canView = usePermission(DIAGNOSTIC_PERMISSIONS.view);
  const canStart = usePermission(DIAGNOSTIC_PERMISSIONS.start);
  const canEdit = usePermission(DIAGNOSTIC_PERMISSIONS.edit);
  const canStop = usePermission(DIAGNOSTIC_PERMISSIONS.stop);
  const isActive = status?.status === "ACTIVE";

  const canViewRef = useRef(canView);
  canViewRef.current = canView;
  const reloadControllerRef = useRef<AbortController | null>(null);
  const reloadGenerationRef = useRef(0);
  const actionControllerRef = useRef<AbortController | null>(null);
  const actionInFlightRef = useRef(false);

  const syncConfig = useCallback((nextStatus: DiagnosticSession) => {
    setConfig(copyConfig(nextStatus.config));
  }, []);

  const reloadAll = useCallback(
    async (showLoading = true) => {
      if (!canViewRef.current) return;
      const generation = ++reloadGenerationRef.current;
      reloadControllerRef.current?.abort();
      const controller = new AbortController();
      reloadControllerRef.current = controller;
      setLoading(showLoading);
      try {
        const options = { signal: controller.signal };
        const [nextStatus, nextCapability, nextSnapshot, nextEvents, nextHistory] =
          await Promise.all([
            getDiagnosticStatus(options),
            getDiagnosticCapabilities(options),
            getLatestDiagnosticSnapshot(options),
            getDiagnosticEvents(60, undefined, options),
            getDiagnosticHistory(30, options),
          ]);
        if (
          controller.signal.aborted ||
          generation !== reloadGenerationRef.current ||
          !canViewRef.current
        ) {
          return;
        }
        setStatus(nextStatus);
        setCapability(nextCapability);
        syncConfig(nextStatus);
        setLatestSnapshot(nextSnapshot);
        setEvents(nextEvents);
        setHistory(nextHistory);
      } catch (cause) {
        if (!isAbortError(cause) && showLoading) {
          message.error(cause instanceof Error ? cause.message : "诊断数据加载失败");
        }
      } finally {
        if (generation === reloadGenerationRef.current) {
          reloadControllerRef.current = null;
          setLoading(false);
        }
      }
    },
    [syncConfig],
  );

  useEffect(() => {
    if (!canView) {
      reloadGenerationRef.current += 1;
      reloadControllerRef.current?.abort();
      reloadControllerRef.current = null;
      setLoading(false);
      setStatus(null);
      setCapability(null);
      setLatestSnapshot(null);
      setEvents([]);
      setHistory([]);
      setDrawerOpen(false);
      return;
    }
    let disposed = false;
    let timer: number | undefined;
    const poll = async (showLoading: boolean) => {
      if (!actionInFlightRef.current) await reloadAll(showLoading);
      if (!disposed) {
        timer = window.setTimeout(() => void poll(false), 5000);
      }
    };
    void poll(true);
    return () => {
      disposed = true;
      if (timer !== undefined) window.clearTimeout(timer);
      reloadGenerationRef.current += 1;
      reloadControllerRef.current?.abort();
      reloadControllerRef.current = null;
    };
  }, [canView, reloadAll]);

  useEffect(() => {
    if (!canEdit) setDrawerOpen(false);
  }, [canEdit]);

  useEffect(
    () => () => {
      actionControllerRef.current?.abort();
      actionControllerRef.current = null;
      actionInFlightRef.current = false;
    },
    [],
  );

  async function handleStart() {
    if (!canStart || actionInFlightRef.current) return;
    actionInFlightRef.current = true;
    reloadGenerationRef.current += 1;
    reloadControllerRef.current?.abort();
    const controller = new AbortController();
    actionControllerRef.current = controller;
    setActionLoading(true);
    try {
      const result = await startDiagnostic(
        { ...config, items: [...config.items] },
        { signal: controller.signal },
      );
      if (actionControllerRef.current !== controller || controller.signal.aborted) return;
      setStatus(result);
      syncConfig(result);
      message.success("运行时诊断已开启");
      await reloadAll(false);
    } catch (cause) {
      if (!controller.signal.aborted) {
        message.error(cause instanceof Error ? cause.message : "运行时诊断开启失败");
      }
    } finally {
      if (actionControllerRef.current === controller) {
        actionControllerRef.current = null;
        actionInFlightRef.current = false;
        setActionLoading(false);
      }
    }
  }

  async function handleUpdate() {
    if (!canEdit || actionInFlightRef.current) return;
    actionInFlightRef.current = true;
    reloadGenerationRef.current += 1;
    reloadControllerRef.current?.abort();
    const controller = new AbortController();
    actionControllerRef.current = controller;
    setActionLoading(true);
    try {
      const payload = { ...drawerForm, items: [...drawerForm.items] };
      const result = await updateDiagnosticConfig(payload, { signal: controller.signal });
      if (actionControllerRef.current !== controller || controller.signal.aborted) return;
      setStatus(result);
      syncConfig(result);
      setDrawerOpen(false);
      message.success("诊断配置已更新");
      await reloadAll(false);
    } catch (cause) {
      if (!controller.signal.aborted) {
        message.error(cause instanceof Error ? cause.message : "诊断配置更新失败");
      }
    } finally {
      if (actionControllerRef.current === controller) {
        actionControllerRef.current = null;
        actionInFlightRef.current = false;
        setActionLoading(false);
      }
    }
  }

  async function handleStop() {
    if (!canStop || actionInFlightRef.current) return;
    actionInFlightRef.current = true;
    reloadGenerationRef.current += 1;
    reloadControllerRef.current?.abort();
    const controller = new AbortController();
    actionControllerRef.current = controller;
    setActionLoading(true);
    try {
      await stopDiagnostic({ signal: controller.signal });
      if (actionControllerRef.current !== controller || controller.signal.aborted) return;
      message.success("运行时诊断已停止");
      await reloadAll(false);
    } catch (cause) {
      if (!controller.signal.aborted) {
        message.error(cause instanceof Error ? cause.message : "运行时诊断停止失败");
      }
    } finally {
      if (actionControllerRef.current === controller) {
        actionControllerRef.current = null;
        actionInFlightRef.current = false;
        setActionLoading(false);
      }
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
          <div className={styles.eventDetails}>{renderEventDetails(row.details)}</div>
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
      render: (row) =>
        `${row.dbPool?.activeConnections ?? 0} / ${row.dbPool?.totalConnections ?? 0}`,
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
        return (
          <BzCheckbox
            modelValue={checked}
            onValueChange={(value) => toggleDrawerItem(row.value, value)}
          />
        );
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
            <div className={styles.formControl}>
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
            <div className={styles.formControl}>
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
        return <span className={styles.textMuted}>无专属阈值配置</span>;
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

  const overviewSections = useMemo<AdminDetailSection[]>(
    () => [
      {
        title: "运行概览",
        fields: [
          { label: "运行状态", value: isActive ? "采集中" : "未开启" },
          { label: "剩余 TTL", value: `${status?.remainingTtlSeconds ?? 0} 秒` },
          {
            label: "最近采样",
            value: latestSnapshot?.capturedAt ? formatDateTime(latestSnapshot.capturedAt) : "-",
          },
          { label: "JFR", value: capability?.jfrAvailable ? "可用" : "不可用" },
          {
            label: "数据源",
            value: capability?.dataSourceNames?.length
              ? capability.dataSourceNames.join(" / ")
              : "-",
            span: "full",
          },
          {
            label: "采集项",
            value: status?.config.items.length ? status.config.items.join(" / ") : "-",
            span: "full",
            multiline: true,
          },
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
    ],
    [
      capability?.dataSourceNames,
      capability?.jfrAvailable,
      isActive,
      latestSnapshot,
      runtimeDetails,
      status?.config.items,
      status?.remainingTtlSeconds,
    ],
  );

  const drawerFooter = (
    <>
      <BzButton onClick={() => setDrawerOpen(false)}>取消</BzButton>
      <BzButton
        buttonType="primary"
        loading={actionLoading}
        onClick={handleUpdate}
      >
        确认
      </BzButton>
    </>
  );

  const configSections = useMemo<AdminDetailSection[]>(
    () => [
      {
        title: "通用配置",
        fields: [
          {
            label: "采样间隔(ms)",
            value: (
              <div className={styles.formControl}>
                <BzInput
                  modelValue={drawerForm.intervalMs}
                  type="number"
                  onValueChange={(value) =>
                    setDrawerForm((prev) => ({ ...prev, intervalMs: Number(value) }))
                  }
                />
              </div>
            ),
          },
          {
            label: "历史容量",
            value: (
              <div className={styles.formControl}>
                <BzInput
                  modelValue={drawerForm.historyCapacity}
                  type="number"
                  onValueChange={(value) =>
                    setDrawerForm((prev) => ({ ...prev, historyCapacity: Number(value) }))
                  }
                />
              </div>
            ),
          },
          {
            label: "事件容量",
            value: (
              <div className={styles.formControl}>
                <BzInput
                  modelValue={drawerForm.eventCapacity}
                  type="number"
                  onValueChange={(value) =>
                    setDrawerForm((prev) => ({ ...prev, eventCapacity: Number(value) }))
                  }
                />
              </div>
            ),
          },
          {
            label: "最长持续时间(秒)",
            value: (
              <div className={styles.formControl}>
                <BzInput
                  modelValue={drawerForm.ttlSeconds}
                  type="number"
                  onValueChange={(value) =>
                    setDrawerForm((prev) => ({ ...prev, ttlSeconds: Number(value) }))
                  }
                />
              </div>
            ),
          },
          {
            label: "深度模式",
            value: (
              <BzSwitch
                modelValue={drawerForm.deepMode}
                onValueChange={(value) => setDrawerForm((prev) => ({ ...prev, deepMode: value }))}
              />
            ),
          },
          {
            label: "模式说明",
            value: "适合短时间排障，会带来更高采样成本。",
            span: "full",
            multiline: true,
          },
        ],
      },
    ],
    [
      drawerForm.deepMode,
      drawerForm.eventCapacity,
      drawerForm.historyCapacity,
      drawerForm.intervalMs,
      drawerForm.ttlSeconds,
    ],
  );

  return (
    <div className={layoutStyles.page}>
      <div className={layoutStyles.content}>
        <div className={layoutStyles.pageStack}>
          <BzCard
            className={`${layoutStyles.panel} ${layoutStyles.tableCard} ${layoutStyles.listCard}`}
            shadow="never"
          >
            <div className={layoutStyles.listRegion}>
              <div className={layoutStyles.listToolbarRow}>
                <div className={layoutStyles.listBusinessActions}>
                  <span className={layoutStyles.batchToolbarSummary}>
                    {isActive ? "运行时诊断采集中" : "运行时诊断未开启"}
                  </span>
                </div>
                <div className={layoutStyles.listQueryTools}>
                  {canEdit && isActive ? (
                    <ToolButton
                      title="诊断设置"
                      kind="settings"
                      disabled={actionLoading}
                      onClick={openDrawer}
                    />
                  ) : null}
                  {canStart && !isActive ? (
                    <ToolButton
                      title="开启诊断"
                      kind="start"
                      disabled={actionLoading}
                      onClick={() => void handleStart()}
                    />
                  ) : null}
                  {canStop && isActive ? (
                    <ToolButton
                      title="停止诊断"
                      kind="stop"
                      active
                      disabled={actionLoading}
                      onClick={() => void handleStop()}
                    />
                  ) : null}
                  {canView ? (
                    <ToolButton
                      title="刷新数据"
                      kind="refresh"
                      disabled={loading || actionLoading}
                      onClick={() => void reloadAll(true)}
                    />
                  ) : null}
                </div>
              </div>
              {!canView ? (
                <BzEmpty description="无权限查看运行时诊断" />
              ) : (
                <div className={`${layoutStyles.pageStack} ${styles.pageBody}`}>
                  <AdminDetailTable sections={overviewSections} />
                  <section className={`${styles.selectionSection} ${styles.sectionCard}`}>
                    <div className={styles.selectionHeader}>
                      <div className={styles.selectionTitle}>最近事件</div>
                      <div className={styles.selectionMeta}>最新 {events.length} 条</div>
                    </div>
                    <div className={styles.selectionTableWrap}>
                      <BzTable
                        loading={loading}
                        data={events}
                        columns={eventColumns}
                        emptyText="暂无事件"
                        size="small"
                      />
                    </div>
                  </section>
                  <section className={`${styles.selectionSection} ${styles.sectionCard}`}>
                    <div className={styles.selectionHeader}>
                      <div className={styles.selectionTitle}>快照历史</div>
                      <div className={styles.selectionMeta}>最新 {history.length} 条</div>
                    </div>
                    <div className={styles.selectionTableWrap}>
                      <BzTable
                        loading={loading}
                        data={history}
                        columns={historyColumns}
                        emptyText="暂无快照"
                        size="small"
                      />
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
        <div className={styles.drawerShell}>
          <AdminDetailTable sections={configSections} />
          <section className={`${styles.selectionSection} ${styles.configBlock}`}>
            <div className={styles.selectionHeader}>
              <div className={styles.selectionTitle}>采集项配置</div>
              <div className={styles.selectionMeta}>共 {itemOptions.length} 项</div>
            </div>
            <div className={styles.selectionTableWrap}>
              <BzTable
                data={itemOptions}
                columns={itemConfigColumns}
                rowKey="value"
                emptyText="暂无采集项"
                size="small"
              />
            </div>
          </section>
        </div>
      </AdminEntityDrawer>
    </div>
  );
}
