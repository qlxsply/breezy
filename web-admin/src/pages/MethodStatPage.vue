<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section class="list-page-actions">
          <div class="list-page-actions-main">
            <bz-button
              v-if="canSwitchView && canSwitchEdit"
              :disabled="!globalSwitchEnabled || batchSwitchLoading"
              :loading="batchSwitchLoading"
              @click="handleSetAllMethodSwitch(true)"
              >全部开启</bz-button
            >
            <bz-button
              v-if="canSwitchView && canSwitchEdit"
              :disabled="!globalSwitchEnabled || batchSwitchLoading"
              :loading="batchSwitchLoading"
              @click="handleSetAllMethodSwitch(false)"
              >全部关闭</bz-button
            >
            <bz-button
              v-if="canStatClear"
              :loading="clearingAll"
              @click="handleClearAllStats"
              >全部清空</bz-button
            >
          </div>

          <div
            v-if="canSwitchView && canSwitchEdit"
            class="list-page-actions-side"
          >
            <span class="switch-label">采集功能</span>
            <bz-switch
              v-model="globalSwitchEnabled"
              :disabled="globalSwitchLoading"
              @change="handleGlobalSwitchChange"
            />
          </div>
        </section>

        <bz-card
          class="list-page-query-card"
          shadow="never"
        >
          <bz-form
            class="list-page-filter-form"
            :inline="true"
            @submit.prevent
          >
            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">方法名</div>
                <bz-input
                  v-model="filters.methodName"
                  class="list-page-filter-control"
                  placeholder="输入方法名"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </div>
            </bz-form-item>

            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">匹配模式</div>
                <bz-select
                  v-model="filters.matchMode"
                  class="match-mode-select"
                >
                  <bz-option
                    label="模糊"
                    value="FUZZY"
                  />
                  <bz-option
                    label="精确"
                    value="EXACT"
                  />
                </bz-select>
              </div>
            </bz-form-item>

            <bz-form-item class="list-page-filter-item sort-field-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">排序规则</div>
                <div class="list-page-sort-group">
                  <bz-select
                    v-model="filters.sortBy"
                    class="sort-field-select"
                  >
                    <bz-option
                      v-for="option in sortOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </bz-select>
                  <bz-select
                    v-model="filters.sortDirection"
                    class="sort-order-select"
                  >
                    <bz-option
                      label="升序"
                      value="ASC"
                    />
                    <bz-option
                      label="降序"
                      value="DESC"
                    />
                  </bz-select>
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="list-page-filter-actions">
              <bz-button
                type="primary"
                @click="handleSearch"
                >搜索</bz-button
              >
              <bz-button @click="handleReset">重置</bz-button>
            </bz-form-item>
          </bz-form>
        </bz-card>

        <bz-card class="list-page-result-card">
          <bz-empty
            v-if="!canStatsView"
            description="无权限查看统计结果"
          />

          <template v-else>
            <bz-table
              v-loading="statsLoading"
              :data="statsPage.elements"
              empty-text="暂无统计数据"
              size="small"
            >
              <bz-table-column
                label="方法"
                min-width="260"
              >
                <template #default="scope">
                  <bz-tooltip
                    :content="scope.row.methodSignature"
                    placement="top"
                  >
                    <span class="method-display">{{
                      formatMethodDisplay(scope.row.className, scope.row.methodName)
                    }}</span>
                  </bz-tooltip>
                </template>
              </bz-table-column>

              <bz-table-column
                label="状态"
                width="120"
              >
                <template #default="scope">
                  <bz-tag
                    size="small"
                    :type="scope.row.collectEnabled ? 'success' : 'info'"
                    >{{ scope.row.collectEnabled ? "采集中" : "已关闭" }}</bz-tag
                  >
                </template>
              </bz-table-column>

              <bz-table-column
                prop="totalCalls"
                label="累计调用"
                width="120"
              >
                <template #default="scope">
                  {{ formatNumber(scope.row.totalCalls) }}
                </template>
              </bz-table-column>

              <bz-table-column
                label="窗口调用"
                min-width="170"
              >
                <template #default="scope">
                  <div class="metric-line">
                    1分：{{ formatNumber(scope.row.recent1MinuteCalls) }}
                  </div>
                  <div class="metric-line">1时：{{ formatNumber(scope.row.recent1HourCalls) }}</div>
                  <div class="metric-line">1天：{{ formatNumber(scope.row.recent1DayCalls) }}</div>
                </template>
              </bz-table-column>

              <bz-table-column
                label="成功 / 失败"
                min-width="160"
              >
                <template #default="scope">
                  <div class="metric-line success-text">
                    成功：{{ formatNumber(scope.row.totalSuccess) }}
                  </div>
                  <div class="metric-line danger-text">
                    失败：{{ formatNumber(scope.row.totalFailure) }}
                  </div>
                  <div class="metric-line">
                    成功率：{{ formatSuccessRate(scope.row.totalSuccess, scope.row.totalCalls) }}
                  </div>
                </template>
              </bz-table-column>

              <bz-table-column
                label="耗时(ms)"
                min-width="180"
              >
                <template #default="scope">
                  <div class="metric-line">avg：{{ formatDecimal(scope.row.durationAvg) }}</div>
                  <div class="metric-line">p95：{{ formatNumber(scope.row.durationP95) }}</div>
                  <div class="metric-line">max：{{ formatNumber(scope.row.durationMax) }}</div>
                  <div class="metric-line">
                    样本：{{ formatNumber(scope.row.durationSampleSize) }}
                  </div>
                </template>
              </bz-table-column>

              <bz-table-column
                label="操作"
                width="260"
                fixed="right"
              >
                <template #default="scope">
                  <div class="action-buttons">
                    <bz-button
                      v-if="canSwitchEdit"
                      size="small"
                      :disabled="isMethodSwitchLoading(scope.row.key)"
                      :loading="isMethodSwitchLoading(scope.row.key)"
                      @click="handleToggleMethodSwitch(scope.row)"
                      >{{ scope.row.methodSwitchEnabled ? "关闭" : "开启" }}</bz-button
                    >
                    <bz-button
                      size="small"
                      @click="openStatsDetail(scope.row.key)"
                      >详情</bz-button
                    >
                    <bz-button
                      v-if="canStatClear"
                      size="small"
                      type="danger"
                      :disabled="isClearingMethod(scope.row.key)"
                      :loading="isClearingMethod(scope.row.key)"
                      @click="handleClearMethodStats(scope.row.key)"
                      >清空</bz-button
                    >
                  </div>
                </template>
              </bz-table-column>
            </bz-table>

            <div class="list-page-pagination">
              <div class="list-page-pagination-summary">
                <span>总计 {{ statsPage.totalElements }} 项</span>
                <span>，共 {{ statsTotalPages }} 页</span>
              </div>
              <bz-pagination
                v-if="statsPage.totalElements > 0"
                :total="statsPage.totalElements"
                :page-sizes="[10, 20, 50, 100]"
                :page-size="filters.statsPageSize"
                :current-page="filters.statsPageNo"
                @current-change="changeStatsPage"
                @size-change="changeStatsPageSize"
              />
            </div>
          </template>
        </bz-card>
      </div>
    </div>

    <bz-dialog
      v-model="detailDialog.visible"
      title="方法统计详情"
      width="760px"
      @close="closeStatsDetail"
    >
      <div v-loading="detailDialog.loading">
        <template v-if="detailDialog.data">
          <div class="detail-block">
            <div class="detail-label">包名</div>
            <div class="detail-value">{{ detailDialog.data.packageName }}</div>
          </div>
          <div class="detail-block">
            <div class="detail-label">类名</div>
            <div class="detail-value">{{ detailDialog.data.className }}</div>
          </div>
          <div class="detail-block">
            <div class="detail-label">方法名</div>
            <div class="detail-value">{{ detailDialog.data.methodName }}</div>
          </div>
          <div class="detail-block">
            <div class="detail-label">唯一Key</div>
            <div class="detail-value key-text">{{ detailDialog.data.key }}</div>
          </div>

          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item-label">累计调用</span>
              <span class="detail-item-value">{{
                formatNumber(detailDialog.data.totalCalls)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">累计成功</span>
              <span class="detail-item-value">{{
                formatNumber(detailDialog.data.totalSuccess)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">累计失败</span>
              <span class="detail-item-value">{{
                formatNumber(detailDialog.data.totalFailure)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">1分钟调用</span>
              <span class="detail-item-value">{{
                formatNumber(detailDialog.data.recent1MinuteCalls)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">1小时调用</span>
              <span class="detail-item-value">{{
                formatNumber(detailDialog.data.recent1HourCalls)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">1天调用</span>
              <span class="detail-item-value">{{
                formatNumber(detailDialog.data.recent1DayCalls)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">耗时avg(ms)</span>
              <span class="detail-item-value">{{
                formatDecimal(detailDialog.data.durationAvg)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">耗时p95(ms)</span>
              <span class="detail-item-value">{{
                formatNumber(detailDialog.data.durationP95)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">耗时max(ms)</span>
              <span class="detail-item-value">{{
                formatNumber(detailDialog.data.durationMax)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">样本数</span>
              <span class="detail-item-value">{{
                formatNumber(detailDialog.data.durationSampleSize)
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">方法开关</span>
              <span class="detail-item-value">{{
                detailDialog.data.methodSwitchEnabled ? "开启" : "关闭"
              }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item-label">采集状态</span>
              <span class="detail-item-value">{{
                detailDialog.data.collectEnabled ? "采集中" : "已关闭"
              }}</span>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <bz-button @click="closeStatsDetail">关闭</bz-button>
      </template>
    </bz-dialog>
  </div>
</template>

<script setup lang="ts">
import { hasAdminResourceCodeAccess } from "@admin/registry/admin-permissions";
import { message } from "@shared/utils/message";
import { computed, onMounted, reactive, ref } from "vue";

import {
  clearAllMethodStat,
  clearMethodStat,
  getMethodStatGlobalSwitch,
  getMethodStatStatsDetail,
  pageMethodStatStats,
  updateAllMethodStatMethodSwitch,
  updateMethodStatGlobalSwitch,
  updateMethodStatMethodSwitch,
} from "../api/method-stat";
import type {
  MethodStatMatchMode,
  MethodStatSortBy,
  MethodStatSortDirection,
  MethodStatStatsItem,
} from "../types/method-stat";
import type { PageResult } from "../types/page";

interface MethodSwitchRow {
  key: string;
  methodSwitchEnabled: boolean;
  collectEnabled?: boolean;
}

interface MethodStatFilterState {
  methodName: string;
  matchMode: MethodStatMatchMode;
  sortBy: MethodStatSortBy;
  sortDirection: MethodStatSortDirection;
  statsPageNo: number;
  statsPageSize: number;
}

interface DetailDialogState {
  visible: boolean;
  loading: boolean;
  data: MethodStatStatsItem | null;
}

const statsLoading = ref(false);
const globalSwitchLoading = ref(false);
const clearingAll = ref(false);
const batchSwitchLoading = ref(false);

const methodSwitchLoadingKeys = ref<Set<string>>(new Set());
const clearingMethodKeys = ref<Set<string>>(new Set());

const globalSwitchEnabled = ref(false);

const filters = reactive<MethodStatFilterState>({
  methodName: "",
  matchMode: "FUZZY",
  sortBy: "TOTAL_CALLS",
  sortDirection: "DESC",
  statsPageNo: 1,
  statsPageSize: 20,
});

const statsPage = reactive<PageResult<MethodStatStatsItem>>({
  pageNo: 1,
  pageSize: 20,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});

const detailDialog = reactive<DetailDialogState>({
  visible: false,
  loading: false,
  data: null,
});

const sortOptions: Array<{ label: string; value: MethodStatSortBy }> = [
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

const canStatsView = computed(() => hasAdminResourceCodeAccess("method-stat-view"));
const canSwitchView = computed(() => hasAdminResourceCodeAccess("method-stat-switch-view"));
const canSwitchEdit = computed(() => hasAdminResourceCodeAccess("method-stat-switch-edit"));
const canStatClear = computed(() => hasAdminResourceCodeAccess("method-stat-clear"));

const statsTotalPages = computed(() => {
  if (filters.statsPageSize <= 0) {
    return 0;
  }
  return Math.ceil(statsPage.totalElements / filters.statsPageSize);
});

onMounted(() => {
  void initializePage();
});

async function initializePage() {
  if (canSwitchView.value) {
    await loadGlobalSwitch();
  }
  await loadStats();
}

async function loadGlobalSwitch() {
  if (!canSwitchView.value) {
    globalSwitchEnabled.value = false;
    return;
  }
  const state = await getMethodStatGlobalSwitch();
  globalSwitchEnabled.value = state.enabled;
}

async function loadStats(pageNo?: number) {
  if (!canStatsView.value) {
    Object.assign(statsPage, {
      pageNo: 1,
      pageSize: filters.statsPageSize,
      numberOfElements: 0,
      totalPages: 0,
      totalElements: 0,
      elements: [],
    });
    return;
  }
  if (typeof pageNo === "number") {
    filters.statsPageNo = pageNo;
  }
  statsLoading.value = true;
  try {
    const page = await pageMethodStatStats({
      methodName: normalizeText(filters.methodName),
      matchMode: filters.matchMode,
      page: {
        pageNo: filters.statsPageNo,
        pageSize: filters.statsPageSize,
      },
      sort: {
        orders: [
          {
            field: filters.sortBy,
            direction: filters.sortDirection,
          },
        ],
      },
    });
    Object.assign(statsPage, page);
  } finally {
    statsLoading.value = false;
  }
}

async function handleSearch() {
  await loadStats(1);
}

async function handleReset() {
  filters.methodName = "";
  filters.matchMode = "FUZZY";
  filters.sortBy = "TOTAL_CALLS";
  filters.sortDirection = "DESC";
  filters.statsPageNo = 1;
  await loadStats();
}

async function changeStatsPage(pageNo: number) {
  await loadStats(pageNo);
}

async function changeStatsPageSize(pageSize: number) {
  filters.statsPageSize = pageSize;
  await loadStats(1);
}

async function handleGlobalSwitchChange(next: boolean) {
  if (!canSwitchEdit.value) {
    globalSwitchEnabled.value = !next;
    message.warning("无权限维护统计开关");
    return;
  }
  if (globalSwitchLoading.value) {
    return;
  }
  globalSwitchLoading.value = true;
  try {
    const updated = await updateMethodStatGlobalSwitch(next);
    globalSwitchEnabled.value = updated.enabled;
    message.success(updated.enabled ? "采集功能已开启" : "采集功能已关闭");
    await loadStats();
    if (!updated.enabled) {
      closeStatsDetail();
      return;
    }
    if (detailDialog.visible && detailDialog.data) {
      await openStatsDetail(detailDialog.data.key);
    }
  } catch (_error) {
    globalSwitchEnabled.value = !next;
  } finally {
    globalSwitchLoading.value = false;
  }
}

async function handleMethodSwitchChange(row: MethodSwitchRow, next: boolean) {
  if (!canSwitchEdit.value) {
    message.warning("无权限维护方法开关");
    return;
  }
  if (isMethodSwitchLoading(row.key)) {
    return;
  }

  const previous = row.methodSwitchEnabled;
  row.methodSwitchEnabled = next;
  if (row.collectEnabled !== undefined) {
    row.collectEnabled = next && globalSwitchEnabled.value;
  }

  addMethodSwitchLoading(row.key);
  try {
    await updateMethodStatMethodSwitch(row.key, next);
    message.success(next ? "方法统计已开启" : "方法统计已关闭");
    await loadStats();
    if (detailDialog.visible && detailDialog.data?.key === row.key) {
      await openStatsDetail(row.key);
    }
  } catch (_error) {
    row.methodSwitchEnabled = previous;
    if (row.collectEnabled !== undefined) {
      row.collectEnabled = previous && globalSwitchEnabled.value;
    }
  } finally {
    removeMethodSwitchLoading(row.key);
  }
}

function handleToggleMethodSwitch(row: MethodStatStatsItem) {
  void handleMethodSwitchChange(row, !row.methodSwitchEnabled);
}

async function handleSetAllMethodSwitch(enabled: boolean) {
  if (!canSwitchEdit.value) {
    message.warning("无权限维护方法开关");
    return;
  }
  if (!globalSwitchEnabled.value) {
    message.warning("请先开启采集功能");
    return;
  }
  if (batchSwitchLoading.value) {
    return;
  }

  batchSwitchLoading.value = true;
  try {
    await updateAllMethodStatMethodSwitch(enabled);
    message.success(enabled ? "已开启全部方法采集" : "已关闭全部方法采集");
    await loadStats();
    if (detailDialog.visible && detailDialog.data) {
      await openStatsDetail(detailDialog.data.key);
    }
  } finally {
    batchSwitchLoading.value = false;
  }
}

async function handleClearMethodStats(key: string) {
  if (!canStatClear.value) {
    message.warning("无权限清空统计数据");
    return;
  }
  if (isClearingMethod(key)) {
    return;
  }

  addMethodClearing(key);
  try {
    await clearMethodStat(key);
    message.success("方法统计已清空");
    await loadStats();
    if (detailDialog.visible && detailDialog.data?.key === key) {
      await openStatsDetail(key);
    }
  } finally {
    removeMethodClearing(key);
  }
}

async function handleClearAllStats() {
  if (!canStatClear.value) {
    message.warning("无权限清空统计数据");
    return;
  }
  if (clearingAll.value) {
    return;
  }

  clearingAll.value = true;
  try {
    await clearAllMethodStat();
    message.success("统计数据已清空");
    await loadStats();
    if (detailDialog.visible && detailDialog.data) {
      await openStatsDetail(detailDialog.data.key);
    }
  } finally {
    clearingAll.value = false;
  }
}

async function openStatsDetail(key: string) {
  if (!canStatsView.value) {
    message.warning("无权限查看统计详情");
    return;
  }
  detailDialog.visible = true;
  detailDialog.loading = true;
  try {
    detailDialog.data = await getMethodStatStatsDetail(key);
  } finally {
    detailDialog.loading = false;
  }
}

function closeStatsDetail() {
  detailDialog.visible = false;
}

function addMethodSwitchLoading(key: string) {
  methodSwitchLoadingKeys.value = new Set(methodSwitchLoadingKeys.value).add(key);
}

function removeMethodSwitchLoading(key: string) {
  const next = new Set(methodSwitchLoadingKeys.value);
  next.delete(key);
  methodSwitchLoadingKeys.value = next;
}

function isMethodSwitchLoading(key: string): boolean {
  return methodSwitchLoadingKeys.value.has(key);
}

function addMethodClearing(key: string) {
  clearingMethodKeys.value = new Set(clearingMethodKeys.value).add(key);
}

function removeMethodClearing(key: string) {
  const next = new Set(clearingMethodKeys.value);
  next.delete(key);
  clearingMethodKeys.value = next;
}

function isClearingMethod(key: string): boolean {
  return clearingMethodKeys.value.has(key);
}

function normalizeText(value: string): string | undefined {
  const normalized = value.trim();
  if (!normalized) {
    return undefined;
  }
  return normalized;
}

function formatMethodDisplay(className: string, methodName: string): string {
  return `${className}.${methodName}`;
}

function formatNumber(value: number): string {
  return value.toLocaleString("zh-CN");
}

function formatDecimal(value: number): string {
  return value.toFixed(2);
}

function formatSuccessRate(success: number, total: number): string {
  if (total <= 0) {
    return "0.00%";
  }
  return `${((success / total) * 100).toFixed(2)}%`;
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

.list-page-actions-side {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.switch-label {
  font-size: 13px;
  color: var(--text-muted);
}

.sort-field-item {
  min-width: 340px;
}

.match-mode-select {
  width: 120px;
}

.sort-field-select {
  width: 160px;
}

.sort-order-select {
  width: 120px;
}

.method-display {
  font-weight: 600;
  line-height: 1.4;
}

.metric-line {
  font-size: 12px;
  line-height: 1.45;
}

.success-text {
  color: #16a34a;
}

.danger-text {
  color: #dc2626;
}

.key-text {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 100%;
}

.action-buttons {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.detail-block {
  margin-bottom: 12px;
}

.detail-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.detail-value {
  font-size: 13px;
  line-height: 1.5;
  word-break: break-all;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px;
}

.detail-item {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item-label {
  font-size: 12px;
  color: var(--text-muted);
}

.detail-item-value {
  font-size: 13px;
  color: var(--text-main);
  font-weight: 600;
}

@media (max-width: 768px) {
  .content {
    padding: 12px;
  }

  .sort-field-item,
  .match-mode-select,
  .sort-field-select,
  .sort-order-select {
    width: 100%;
    min-width: 0;
  }

  .list-page-actions-side {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
