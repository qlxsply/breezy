"use client";

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
import { useCallback, useEffect, useRef, useState } from "react";

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

const itemOptions: Array<{ label: string; value: DiagnosticItem }> = [
  { label: "JVM", value: "JVM" },
  { label: "操作系统", value: "OS" },
  { label: "线程", value: "THREAD" },
  { label: "HTTP", value: "HTTP" },
  { label: "连接池", value: "DB_POOL" },
  { label: "SQL", value: "SQL" },
  { label: "JFR", value: "JFR" },
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

export function DiagnosticPage() {
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [status, setStatus] = useState<DiagnosticSession | null>(null);
  const [capability, setCapability] = useState<DiagnosticCapability | null>(null);
  const [latestSnapshot, setLatestSnapshot] = useState<DiagnosticSnapshot | null>(null);
  const [events, setEvents] = useState<DiagnosticEvent[]>([]);
  const [history, setHistory] = useState<DiagnosticSnapshot[]>([]);

  const [form, setForm] = useState<DiagnosticConfigPayload>({
    intervalMs: 5000,
    historyCapacity: 180,
    eventCapacity: 300,
    items: ["JVM", "OS", "THREAD", "HTTP", "DB_POOL", "SQL", "JFR"],
    deepMode: false,
    slowRequestThresholdMs: 1000,
    slowSqlThresholdMs: 500,
    ttlSeconds: 1800,
  });

  const canView = hasResourceCodeAccess("diagnostic-view");
  const canStart = hasResourceCodeAccess("diagnostic-start");
  const canEdit = hasResourceCodeAccess("diagnostic-edit");
  const canStop = hasResourceCodeAccess("diagnostic-stop");
  const isActive = status?.status === "ACTIVE";

  const canViewRef = useRef(canView);
  canViewRef.current = canView;

  const syncForm = useCallback((nextStatus: DiagnosticSession) => {
    const config = nextStatus.config;
    setForm({
      intervalMs: config.intervalMs,
      historyCapacity: config.historyCapacity,
      eventCapacity: config.eventCapacity,
      items: [...config.items],
      deepMode: config.deepMode,
      slowRequestThresholdMs: config.slowRequestThresholdMs,
      slowSqlThresholdMs: config.slowSqlThresholdMs,
      ttlSeconds: config.ttlSeconds,
    });
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
    reloadAll(true);
    const timer = window.setInterval(() => {
      if (canViewRef.current) {
        reloadAll(false);
      }
    }, 5000);
    return () => {
      window.clearInterval(timer);
    };
  }, [reloadAll]);

  async function handleStart() {
    setActionLoading(true);
    try {
      const result = await startDiagnostic({ ...form, items: [...form.items] });
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
      const result = await updateDiagnosticConfig({ ...form, items: [...form.items] });
      setStatus(result);
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

  function toggleItem(item: DiagnosticItem, checked: boolean) {
    setForm((prev) => {
      if (checked) {
        return { ...prev, items: [...prev.items, item] };
      }
      return { ...prev, items: prev.items.filter((v) => v !== item) };
    });
  }

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

  return (
    <div className="app-shell">
      <div className="content">
        <div className="list-page-stack">
          <section className="list-page-actions">
            <div className="list-page-actions-main">
              {canView ? (
                <BzButton
                  loading={loading}
                  onClick={() => reloadAll(true)}
                >
                  刷新
                </BzButton>
              ) : null}
              {canStart && !isActive ? (
                <BzButton
                  buttonType="primary"
                  loading={actionLoading}
                  onClick={handleStart}
                >
                  开启诊断
                </BzButton>
              ) : null}
              {canEdit && isActive ? (
                <BzButton
                  buttonType="primary"
                  loading={actionLoading}
                  onClick={handleUpdate}
                >
                  更新配置
                </BzButton>
              ) : null}
              {canStop && isActive ? (
                <BzButton
                  buttonType="danger"
                  loading={actionLoading}
                  onClick={handleStop}
                >
                  停止并清空
                </BzButton>
              ) : null}
            </div>

            <div className="list-page-actions-side">
              <BzTag type={isActive ? "success" : "info"}>{isActive ? "运行中" : "未开启"}</BzTag>
              <span className="status-side-text">
                剩余 TTL: {status?.remainingTtlSeconds ?? 0} 秒
              </span>
            </div>
          </section>

          <BzCard
            className="list-page-query-card"
            shadow="never"
          >
            {!canView ? (
              <BzEmpty description="无权限查看运行时诊断" />
            ) : (
              <>
                <div className="headline-row">
                  <div className="headline-block">
                    <div className="headline-label">JFR</div>
                    <BzTag type={capability?.jfrAvailable ? "success" : "info"}>
                      {capability?.jfrAvailable ? "可用" : "不可用"}
                    </BzTag>
                  </div>
                  <div className="headline-block">
                    <div className="headline-label">数据源</div>
                    <div className="headline-value">
                      {capability?.dataSourceNames?.join(" / ") || "-"}
                    </div>
                  </div>
                  <div className="headline-block">
                    <div className="headline-label">采集项</div>
                    <div className="headline-value">{status?.config.items.join(" / ") || "-"}</div>
                  </div>
                </div>

                <div className="summary-grid">
                  <div className="summary-card">
                    <div className="summary-title">JVM</div>
                    <div className="summary-line">
                      堆使用: {formatBytes(latestSnapshot?.jvm?.heapUsedBytes)}
                    </div>
                    <div className="summary-line">
                      GC 次数: {latestSnapshot?.jvm?.gcCollectionCount ?? 0}
                    </div>
                    <div className="summary-line">
                      进程 CPU: {formatPercent(latestSnapshot?.jvm?.processCpuLoad)}
                    </div>
                  </div>

                  <div className="summary-card">
                    <div className="summary-title">线程</div>
                    <div className="summary-line">
                      线程总数: {latestSnapshot?.thread?.threadCount ?? 0}
                    </div>
                    <div className="summary-line">
                      阻塞线程: {latestSnapshot?.thread?.blockedCount ?? 0}
                    </div>
                    <div className="summary-line">
                      死锁数: {latestSnapshot?.thread?.deadlockedThreadIds.length ?? 0}
                    </div>
                  </div>

                  <div className="summary-card">
                    <div className="summary-title">HTTP</div>
                    <div className="summary-line">
                      进行中: {latestSnapshot?.http?.inFlightRequests ?? 0}
                    </div>
                    <div className="summary-line">
                      总请求: {latestSnapshot?.http?.totalRequests ?? 0}
                    </div>
                    <div className="summary-line">
                      P95 / P99: {latestSnapshot?.http?.p95DurationMs ?? 0} /{" "}
                      {latestSnapshot?.http?.p99DurationMs ?? 0} ms
                    </div>
                  </div>

                  <div className="summary-card">
                    <div className="summary-title">数据库</div>
                    <div className="summary-line">
                      连接池: {latestSnapshot?.dbPool?.poolCount ?? 0}
                    </div>
                    <div className="summary-line">
                      活跃连接: {latestSnapshot?.dbPool?.activeConnections ?? 0}
                    </div>
                    <div className="summary-line">
                      SQL 次数: {latestSnapshot?.sql?.totalExecutions ?? 0}
                    </div>
                  </div>
                </div>
              </>
            )}
          </BzCard>

          {canView ? (
            <BzCard
              className="list-page-query-card"
              shadow="never"
            >
              <BzForm
                className="diagnostic-form"
                onSubmit={(e) => e.preventDefault()}
              >
                <div className="config-grid">
                  <BzFormItem label="采样间隔(ms)">
                    <BzInput
                      modelValue={form.intervalMs}
                      type="number"
                      onValueChange={(v) => setForm((prev) => ({ ...prev, intervalMs: Number(v) }))}
                    />
                  </BzFormItem>
                  <BzFormItem label="历史容量">
                    <BzInput
                      modelValue={form.historyCapacity}
                      type="number"
                      onValueChange={(v) =>
                        setForm((prev) => ({ ...prev, historyCapacity: Number(v) }))
                      }
                    />
                  </BzFormItem>
                  <BzFormItem label="事件容量">
                    <BzInput
                      modelValue={form.eventCapacity}
                      type="number"
                      onValueChange={(v) =>
                        setForm((prev) => ({ ...prev, eventCapacity: Number(v) }))
                      }
                    />
                  </BzFormItem>
                  <BzFormItem label="慢请求阈值(ms)">
                    <BzInput
                      modelValue={form.slowRequestThresholdMs}
                      type="number"
                      onValueChange={(v) =>
                        setForm((prev) => ({ ...prev, slowRequestThresholdMs: Number(v) }))
                      }
                    />
                  </BzFormItem>
                  <BzFormItem label="慢 SQL 阈值(ms)">
                    <BzInput
                      modelValue={form.slowSqlThresholdMs}
                      type="number"
                      onValueChange={(v) =>
                        setForm((prev) => ({ ...prev, slowSqlThresholdMs: Number(v) }))
                      }
                    />
                  </BzFormItem>
                  <BzFormItem label="最长持续时间(秒)">
                    <BzInput
                      modelValue={form.ttlSeconds}
                      type="number"
                      onValueChange={(v) => setForm((prev) => ({ ...prev, ttlSeconds: Number(v) }))}
                    />
                  </BzFormItem>
                </div>

                <div className="config-row">
                  <div className="config-label">深度模式</div>
                  <BzSwitch
                    modelValue={form.deepMode}
                    onValueChange={(v) => setForm((prev) => ({ ...prev, deepMode: v }))}
                  />
                </div>

                <div className="config-row">
                  <div className="config-label">采集项</div>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: "12px 24px" }}>
                    {itemOptions.map((item) => (
                      <BzCheckbox
                        key={item.value}
                        modelValue={form.items.includes(item.value)}
                        onValueChange={(checked) => toggleItem(item.value, checked)}
                      >
                        {item.label}
                      </BzCheckbox>
                    ))}
                  </div>
                </div>
              </BzForm>
            </BzCard>
          ) : null}

          {canView ? (
            <BzCard className="list-page-result-card">
              <div className="section-header">
                <span>最近事件</span>
                <span className="section-subtitle">最新 {events.length} 条</span>
              </div>

              <BzTable
                loading={loading}
                data={events}
                columns={eventColumns}
                emptyText="暂无事件"
                size="small"
              />
            </BzCard>
          ) : null}

          {canView ? (
            <BzCard className="list-page-result-card">
              <div className="section-header">
                <span>快照历史</span>
                <span className="section-subtitle">最新 {history.length} 条</span>
              </div>

              <BzTable
                loading={loading}
                data={history}
                columns={historyColumns}
                emptyText="暂无快照"
                size="small"
              />
            </BzCard>
          ) : null}
        </div>
      </div>
    </div>
  );
}
