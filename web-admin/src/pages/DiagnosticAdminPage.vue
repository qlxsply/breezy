<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section class="list-page-actions">
          <div class="list-page-actions-main">
            <bz-button
              v-if="canView"
              :loading="loading"
              @click="reloadAll"
            >
              刷新
            </bz-button>
            <bz-button
              v-if="canStart && !isActive"
              type="primary"
              :loading="actionLoading"
              @click="handleStart"
            >
              开启诊断
            </bz-button>
            <bz-button
              v-if="canEdit && isActive"
              type="primary"
              :loading="actionLoading"
              @click="handleUpdate"
            >
              更新配置
            </bz-button>
            <bz-button
              v-if="canStop && isActive"
              type="danger"
              :loading="actionLoading"
              @click="handleStop"
            >
              停止并清空
            </bz-button>
          </div>

          <div class="list-page-actions-side">
            <bz-tag :type="isActive ? 'success' : 'info'">
              {{ isActive ? "运行中" : "未开启" }}
            </bz-tag>
            <span class="status-side-text"
              >剩余 TTL: {{ status?.remainingTtlSeconds ?? 0 }} 秒</span
            >
          </div>
        </section>

        <bz-card
          class="list-page-query-card"
          shadow="never"
        >
          <bz-empty
            v-if="!canView"
            description="无权限查看运行时诊断"
          />

          <template v-else>
            <div class="headline-row">
              <div class="headline-block">
                <div class="headline-label">JFR</div>
                <bz-tag :type="capability?.jfrAvailable ? 'success' : 'info'">
                  {{ capability?.jfrAvailable ? "可用" : "不可用" }}
                </bz-tag>
              </div>
              <div class="headline-block">
                <div class="headline-label">数据源</div>
                <div class="headline-value">
                  {{ capability?.dataSourceNames?.join(" / ") || "-" }}
                </div>
              </div>
              <div class="headline-block">
                <div class="headline-label">采集项</div>
                <div class="headline-value">{{ status?.config.items.join(" / ") || "-" }}</div>
              </div>
            </div>

            <div class="summary-grid">
              <div class="summary-card">
                <div class="summary-title">JVM</div>
                <div class="summary-line">
                  堆使用: {{ formatBytes(latestSnapshot?.jvm?.heapUsedBytes) }}
                </div>
                <div class="summary-line">
                  GC 次数: {{ latestSnapshot?.jvm?.gcCollectionCount ?? 0 }}
                </div>
                <div class="summary-line">
                  进程 CPU: {{ formatPercent(latestSnapshot?.jvm?.processCpuLoad) }}
                </div>
              </div>

              <div class="summary-card">
                <div class="summary-title">线程</div>
                <div class="summary-line">
                  线程总数: {{ latestSnapshot?.thread?.threadCount ?? 0 }}
                </div>
                <div class="summary-line">
                  阻塞线程: {{ latestSnapshot?.thread?.blockedCount ?? 0 }}
                </div>
                <div class="summary-line">
                  死锁数: {{ latestSnapshot?.thread?.deadlockedThreadIds.length ?? 0 }}
                </div>
              </div>

              <div class="summary-card">
                <div class="summary-title">HTTP</div>
                <div class="summary-line">
                  进行中: {{ latestSnapshot?.http?.inFlightRequests ?? 0 }}
                </div>
                <div class="summary-line">
                  总请求: {{ latestSnapshot?.http?.totalRequests ?? 0 }}
                </div>
                <div class="summary-line">
                  P95 / P99: {{ latestSnapshot?.http?.p95DurationMs ?? 0 }} /
                  {{ latestSnapshot?.http?.p99DurationMs ?? 0 }} ms
                </div>
              </div>

              <div class="summary-card">
                <div class="summary-title">数据库</div>
                <div class="summary-line">连接池: {{ latestSnapshot?.dbPool?.poolCount ?? 0 }}</div>
                <div class="summary-line">
                  活跃连接: {{ latestSnapshot?.dbPool?.activeConnections ?? 0 }}
                </div>
                <div class="summary-line">
                  SQL 次数: {{ latestSnapshot?.sql?.totalExecutions ?? 0 }}
                </div>
              </div>
            </div>
          </template>
        </bz-card>

        <bz-card
          v-if="canView"
          class="list-page-query-card"
          shadow="never"
        >
          <bz-form
            class="diagnostic-form"
            label-position="top"
            @submit.prevent
          >
            <div class="config-grid">
              <bz-form-item label="采样间隔(ms)">
                <bz-input
                  v-model="form.intervalMs"
                  type="number"
                />
              </bz-form-item>
              <bz-form-item label="历史容量">
                <bz-input
                  v-model="form.historyCapacity"
                  type="number"
                />
              </bz-form-item>
              <bz-form-item label="事件容量">
                <bz-input
                  v-model="form.eventCapacity"
                  type="number"
                />
              </bz-form-item>
              <bz-form-item label="慢请求阈值(ms)">
                <bz-input
                  v-model="form.slowRequestThresholdMs"
                  type="number"
                />
              </bz-form-item>
              <bz-form-item label="慢 SQL 阈值(ms)">
                <bz-input
                  v-model="form.slowSqlThresholdMs"
                  type="number"
                />
              </bz-form-item>
              <bz-form-item label="最长持续时间(秒)">
                <bz-input
                  v-model="form.ttlSeconds"
                  type="number"
                />
              </bz-form-item>
            </div>

            <div class="config-row">
              <div class="config-label">深度模式</div>
              <bz-switch v-model="form.deepMode" />
            </div>

            <div class="config-row">
              <div class="config-label">采集项</div>
              <bz-checkbox-group v-model="form.items">
                <bz-checkbox
                  v-for="item in itemOptions"
                  :key="item.value"
                  :label="item.value"
                >
                  {{ item.label }}
                </bz-checkbox>
              </bz-checkbox-group>
            </div>
          </bz-form>
        </bz-card>

        <bz-card
          v-if="canView"
          class="list-page-result-card"
        >
          <div class="section-header">
            <span>最近事件</span>
            <span class="section-subtitle">最新 {{ events.length }} 条</span>
          </div>

          <bz-table
            v-loading="loading"
            :data="events"
            empty-text="暂无事件"
            size="small"
          >
            <bz-table-column
              prop="type"
              label="类型"
              width="180"
            />
            <bz-table-column
              prop="title"
              label="标题"
              width="140"
            />
            <bz-table-column
              label="内容"
              min-width="260"
            >
              <template #default="scope">
                <div>{{ scope.row.message || "-" }}</div>
                <div class="event-details">{{ renderEventDetails(scope.row.details) }}</div>
              </template>
            </bz-table-column>
            <bz-table-column
              label="时间"
              width="180"
            >
              <template #default="scope">
                {{ formatDateTime(scope.row.happenedAt) }}
              </template>
            </bz-table-column>
          </bz-table>
        </bz-card>

        <bz-card
          v-if="canView"
          class="list-page-result-card"
        >
          <div class="section-header">
            <span>快照历史</span>
            <span class="section-subtitle">最新 {{ history.length }} 条</span>
          </div>

          <bz-table
            v-loading="loading"
            :data="history"
            empty-text="暂无快照"
            size="small"
          >
            <bz-table-column
              label="采样时间"
              width="180"
            >
              <template #default="scope">
                {{ formatDateTime(scope.row.capturedAt) }}
              </template>
            </bz-table-column>
            <bz-table-column
              label="堆使用"
              width="160"
            >
              <template #default="scope">
                {{ formatBytes(scope.row.jvm?.heapUsedBytes) }}
              </template>
            </bz-table-column>
            <bz-table-column
              label="进程 CPU"
              width="140"
            >
              <template #default="scope">
                {{ formatPercent(scope.row.jvm?.processCpuLoad) }}
              </template>
            </bz-table-column>
            <bz-table-column
              label="HTTP"
              width="180"
            >
              <template #default="scope">
                {{ scope.row.http?.totalRequests ?? 0 }} /
                {{ scope.row.http?.inFlightRequests ?? 0 }}
              </template>
            </bz-table-column>
            <bz-table-column
              label="连接池"
              width="160"
            >
              <template #default="scope">
                {{ scope.row.dbPool?.activeConnections ?? 0 }} /
                {{ scope.row.dbPool?.totalConnections ?? 0 }}
              </template>
            </bz-table-column>
            <bz-table-column
              label="SQL"
              width="160"
            >
              <template #default="scope">{{ scope.row.sql?.totalExecutions ?? 0 }} 次</template>
            </bz-table-column>
          </bz-table>
        </bz-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
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
import { hasAdminResourceCodeAccess } from "@admin/registry/admin-permissions";
import type {
  DiagnosticCapability,
  DiagnosticConfigPayload,
  DiagnosticEvent,
  DiagnosticItem,
  DiagnosticSession,
  DiagnosticSnapshot,
} from "@admin/types/diagnostic";
import { formatDateTime, formatDecimal } from "@shared/utils/formatter";
import { message as toast } from "@shared/utils/message";
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";

const loading = ref(false);
const actionLoading = ref(false);
const status = ref<DiagnosticSession | null>(null);
const capability = ref<DiagnosticCapability | null>(null);
const latestSnapshot = ref<DiagnosticSnapshot | null>(null);
const events = ref<DiagnosticEvent[]>([]);
const history = ref<DiagnosticSnapshot[]>([]);

const canView = computed(() => hasAdminResourceCodeAccess("diagnostic-view"));
const canStart = computed(() => hasAdminResourceCodeAccess("diagnostic-start"));
const canEdit = computed(() => hasAdminResourceCodeAccess("diagnostic-edit"));
const canStop = computed(() => hasAdminResourceCodeAccess("diagnostic-stop"));
const isActive = computed(() => status.value?.status === "ACTIVE");

const itemOptions: Array<{ label: string; value: DiagnosticItem }> = [
  { label: "JVM", value: "JVM" },
  { label: "操作系统", value: "OS" },
  { label: "线程", value: "THREAD" },
  { label: "HTTP", value: "HTTP" },
  { label: "连接池", value: "DB_POOL" },
  { label: "SQL", value: "SQL" },
  { label: "JFR", value: "JFR" },
];

const form = reactive<DiagnosticConfigPayload>({
  intervalMs: 5000,
  historyCapacity: 180,
  eventCapacity: 300,
  items: ["JVM", "OS", "THREAD", "HTTP", "DB_POOL", "SQL", "JFR"],
  deepMode: false,
  slowRequestThresholdMs: 1000,
  slowSqlThresholdMs: 500,
  ttlSeconds: 1800,
});

let refreshTimer: number | null = null;

onMounted(() => {
  void reloadAll();
  refreshTimer = window.setInterval(() => {
    if (canView.value) {
      void reloadAll(false);
    }
  }, 5000);
});

onUnmounted(() => {
  if (refreshTimer !== null) {
    window.clearInterval(refreshTimer);
  }
});

async function reloadAll(showLoading = true) {
  if (!canView.value) {
    return;
  }
  if (showLoading) {
    loading.value = true;
  }
  try {
    const [nextStatus, nextCapability] = await Promise.all([
      getDiagnosticStatus(),
      getDiagnosticCapabilities(),
    ]);
    status.value = nextStatus;
    capability.value = nextCapability;
    syncForm(nextStatus);

    const [nextSnapshot, nextEvents, nextHistory] = await Promise.all([
      getLatestDiagnosticSnapshot(),
      getDiagnosticEvents(60),
      getDiagnosticHistory(30),
    ]);
    latestSnapshot.value = nextSnapshot;
    events.value = nextEvents;
    history.value = nextHistory;
  } finally {
    if (showLoading) {
      loading.value = false;
    }
  }
}

async function handleStart() {
  actionLoading.value = true;
  try {
    status.value = await startDiagnostic({ ...form, items: [...form.items] });
    toast.success("运行时诊断已开启");
    await reloadAll(false);
  } finally {
    actionLoading.value = false;
  }
}

async function handleUpdate() {
  actionLoading.value = true;
  try {
    status.value = await updateDiagnosticConfig({ ...form, items: [...form.items] });
    toast.success("诊断配置已更新");
    await reloadAll(false);
  } finally {
    actionLoading.value = false;
  }
}

async function handleStop() {
  actionLoading.value = true;
  try {
    await stopDiagnostic();
    toast.success("运行时诊断已停止");
    await reloadAll(false);
  } finally {
    actionLoading.value = false;
  }
}

function syncForm(nextStatus: DiagnosticSession) {
  const { config } = nextStatus;
  form.intervalMs = config.intervalMs;
  form.historyCapacity = config.historyCapacity;
  form.eventCapacity = config.eventCapacity;
  form.items = [...config.items];
  form.deepMode = config.deepMode;
  form.slowRequestThresholdMs = config.slowRequestThresholdMs;
  form.slowSqlThresholdMs = config.slowSqlThresholdMs;
  form.ttlSeconds = config.ttlSeconds;
}

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
    unitIndex += 1;
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
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  overflow-y: auto;
  max-width: none;
  margin: 0;
  width: 100%;
  padding: 16px 24px;
  box-sizing: border-box;
}

.headline-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.headline-block {
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: #f8fafc;
}

.headline-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.headline-value {
  font-size: 14px;
  color: var(--text-main);
  word-break: break-all;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.summary-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: #fff;
  padding: 14px 16px;
}

.summary-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 10px;
}

.summary-line {
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.diagnostic-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.config-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.config-label {
  font-size: 13px;
  color: var(--text-main);
  font-weight: 600;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.section-subtitle {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 400;
}

.event-details {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.status-side-text {
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 960px) {
  .headline-row,
  .summary-grid,
  .config-grid {
    grid-template-columns: 1fr;
  }

  .list-page-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
