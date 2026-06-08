<!-- /src/pages/LoginLogsPage.vue -->
<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card
          v-if="queryPanelVisible"
          class="admin-panel admin-filter-card"
          shadow="never"
        >
          <bz-form
            class="admin-filter-form"
            :class="{ 'is-collapsed': queryCollapsed }"
            @submit.prevent="applyFilters"
          >
            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">账号</div>
                <div class="admin-filter-control">
                  <bz-input
                    v-model="accountDraft"
                    placeholder="按账号搜索"
                    clearable
                    @keyup.enter="applyFilters"
                  />
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">开始时间</div>
                <div class="admin-filter-control">
                  <bz-date-picker
                    v-model="startAtDraft"
                    type="datetime"
                    value-format="YYYY-MM-DDTHH:mm"
                    placeholder="开始时间"
                    clearable
                  />
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">结束时间</div>
                <div class="admin-filter-control">
                  <bz-date-picker
                    v-model="endAtDraft"
                    type="datetime"
                    value-format="YYYY-MM-DDTHH:mm"
                    placeholder="结束时间"
                    clearable
                  />
                </div>
              </div>
            </bz-form-item>

            <div class="admin-filter-actions">
              <bz-button
                class="admin-filter-secondary"
                @click="resetFilters"
              >
                重置
              </bz-button>
              <bz-button
                class="admin-filter-primary"
                type="primary"
                native-type="submit"
              >
                搜索
              </bz-button>
              <div
                class="admin-filter-toggle-placeholder"
                aria-hidden="true"
              ></div>
            </div>
          </bz-form>
        </bz-card>

        <bz-card
          class="admin-panel admin-table-card"
          shadow="never"
        >
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">登录日志</div>
              <div class="admin-table-tools">
                <button
                  class="admin-vben-circle-button"
                  :class="{ 'is-active': queryPanelVisible }"
                  type="button"
                  :title="queryPanelVisible ? '关闭搜索框' : '打开搜索框'"
                  @click="queryPanelVisible = !queryPanelVisible"
                >
                  <i
                    class="admin-vben-circle-button__icon admin-vben-circle-button__icon--search"
                    aria-hidden="true"
                  ></i>
                </button>
                <button
                  class="admin-vben-circle-button"
                  type="button"
                  title="刷新列表"
                  @click="reload"
                >
                  <i
                    class="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh"
                    aria-hidden="true"
                  ></i>
                </button>
              </div>
            </div>
          </template>

          <bz-empty
            v-if="!canView"
            description="无权限查看登录日志"
          />

          <template v-else>
            <div class="admin-table-surface">
              <bz-table
                v-loading="loading"
                :data="rows"
                empty-text="暂无日志"
                size="small"
              >
                <bz-table-column
                  prop="username"
                  label="账号"
                  width="120"
                >
                  <template #default="scope">
                    {{ scope.row.username || "-" }}
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="事件"
                  width="100"
                >
                  <template #default="scope">
                    <bz-tag size="small">{{ scope.row.eventType }}</bz-tag>
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="结果"
                  width="90"
                >
                  <template #default="scope">
                    <bz-tag
                      size="small"
                      :type="scope.row.success ? 'success' : 'danger'"
                    >
                      {{ scope.row.success ? "成功" : "失败" }}
                    </bz-tag>
                  </template>
                </bz-table-column>
                <bz-table-column
                  prop="loginIp"
                  label="IP"
                  width="140"
                >
                  <template #default="scope">
                    {{ scope.row.loginIp || "-" }}
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="失败原因"
                  min-width="180"
                >
                  <template #default="scope">
                    <div class="ua">{{ scope.row.failureReason || "-" }}</div>
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="备注"
                  min-width="220"
                  show-overflow-tooltip
                >
                  <template #default="scope">
                    <div class="msg">{{ scope.row.remark || "-" }}</div>
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="时间"
                  width="180"
                >
                  <template #default="scope">
                    {{ formatDateTime(scope.row.occurredAt) }}
                  </template>
                </bz-table-column>
              </bz-table>
            </div>

            <div
              v-if="page.totalElements > 0"
              class="dict-pagination-bar"
            >
              <div class="dict-pagination-summary">共 {{ page.totalElements }} 条记录</div>
              <div class="dict-pagination-right">
                <label class="dict-page-size">
                  <select
                    class="dict-page-size__select"
                    :value="pageSize"
                    @change="handlePageSizeSelect"
                  >
                    <option
                      v-for="size in pageSizeOptions"
                      :key="size"
                      :value="size"
                    >
                      {{ size }}条/页
                    </option>
                  </select>
                </label>
                <div class="dict-page-list">
                  <button
                    class="dict-page-btn dict-page-btn--icon"
                    type="button"
                    :disabled="isFirstPage"
                    @click="goToPage(1)"
                  >
                    <span aria-hidden="true">|&lt;</span>
                  </button>
                  <button
                    class="dict-page-btn dict-page-btn--icon"
                    type="button"
                    :disabled="isFirstPage"
                    @click="goToPage(pageNo - 1)"
                  >
                    <span aria-hidden="true">&lt;</span>
                  </button>
                  <template
                    v-for="(token, tokenIndex) in pageTokens"
                    :key="`${String(token)}-${tokenIndex}`"
                  >
                    <button
                      v-if="typeof token === 'number'"
                      class="dict-page-btn"
                      :class="{ 'is-active': token === pageNo }"
                      type="button"
                      @click="goToPage(token)"
                    >
                      {{ token }}
                    </button>
                    <span
                      v-else
                      class="dict-page-ellipsis"
                    >
                      ...
                    </span>
                  </template>
                  <button
                    class="dict-page-btn dict-page-btn--icon"
                    type="button"
                    :disabled="isLastPage"
                    @click="goToPage(pageNo + 1)"
                  >
                    <span aria-hidden="true">&gt;</span>
                  </button>
                  <button
                    class="dict-page-btn dict-page-btn--icon"
                    type="button"
                    :disabled="isLastPage"
                    @click="goToPage(totalPages)"
                  >
                    <span aria-hidden="true">&gt;|</span>
                  </button>
                </div>
              </div>
            </div>
          </template>
        </bz-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { pageLoginLogs } from "../api/login-logs";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { LoginLogEntry } from "../types/login-log";
import type { PageResult } from "../types/page";
import {
  dateTimeInputToEpochMillisString,
  dateTimeInputToNextMinuteEpochMillisString,
  formatDateTime,
} from "../utils/formatter";

const loading = ref(false);
const rows = ref<LoginLogEntry[]>([]);
const page = ref<PageResult<LoginLogEntry>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});

const queryPanelVisible = ref(false);
const queryCollapsed = ref(true);
const accountDraft = ref("");
const startAtDraft = ref("");
const endAtDraft = ref("");
const appliedAccount = ref("");
const appliedStartAt = ref("");
const appliedEndAt = ref("");
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;

const canView = computed(() => hasResourceCodeAccess("login-log-view"));
const totalPages = computed(() => Math.max(1, page.value.totalPages || 1));
const isFirstPage = computed(() => pageNo.value <= 1);
const isLastPage = computed(() => pageNo.value >= totalPages.value);
const pageTokens = computed<Array<number | "ellipsis">>(() => {
  const total = totalPages.value;
  const current = Math.min(Math.max(pageNo.value, 1), total);
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1);
  }
  if (current <= 4) {
    return [1, 2, 3, 4, 5, "ellipsis", total];
  }
  if (current >= total - 3) {
    return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  }
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
});

onMounted(() => {
  void reload();
});

async function reload() {
  if (!canView.value) {
    rows.value = [];
    page.value = {
      pageNo: 1,
      pageSize: pageSize.value,
      numberOfElements: 0,
      totalPages: 0,
      totalElements: 0,
      elements: [],
    };
    return;
  }

  loading.value = true;
  try {
    const requestedPageNo = pageNo.value;
    let result = await pageLoginLogs({
      userAccount: appliedAccount.value || undefined,
      startAt: toInstant(appliedStartAt.value),
      endAt: toEndExclusive(appliedEndAt.value),
      page: {
        pageNo: requestedPageNo,
        pageSize: pageSize.value,
      },
    });

    if (result.totalElements > 0 && requestedPageNo > Math.max(1, result.totalPages)) {
      pageNo.value = Math.max(1, result.totalPages);
      result = await pageLoginLogs({
        userAccount: appliedAccount.value || undefined,
        startAt: toInstant(appliedStartAt.value),
        endAt: toEndExclusive(appliedEndAt.value),
        page: {
          pageNo: pageNo.value,
          pageSize: pageSize.value,
        },
      });
    }

    page.value = result;
    pageNo.value = result.pageNo || 1;
    pageSize.value = result.pageSize || pageSize.value;
    rows.value = result.elements;
  } finally {
    loading.value = false;
  }
}

async function applyFilters() {
  appliedAccount.value = accountDraft.value.trim();
  appliedStartAt.value = startAtDraft.value;
  appliedEndAt.value = endAtDraft.value;
  pageNo.value = 1;
  await reload();
}

async function resetFilters() {
  accountDraft.value = "";
  startAtDraft.value = "";
  endAtDraft.value = "";
  await applyFilters();
}

function goToPage(nextPage: number) {
  const target = Math.min(Math.max(nextPage, 1), totalPages.value);
  if (target === pageNo.value) return;
  pageNo.value = target;
  void reload();
}

function handlePageSizeSelect(event: Event) {
  const nextPageSize = Number((event.target as HTMLSelectElement).value);
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === pageSize.value) {
    return;
  }
  pageSize.value = nextPageSize;
  pageNo.value = 1;
  void reload();
}

function toInstant(value: string): string | undefined {
  return dateTimeInputToEpochMillisString(value) ?? undefined;
}

function toEndExclusive(value: string): string | undefined {
  return dateTimeInputToNextMinuteEpochMillisString(value) ?? undefined;
}
</script>

<style scoped>
.content {
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}

.ua {
  font-size: 12px;
  color: var(--text-muted);
}

.msg {
  margin-top: 4px;
  font-size: 12px;
}

.dict-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 14px 20px 18px;
  color: #475569;
}

.dict-pagination-summary {
  font-size: 13px;
  color: #64748b;
}

.dict-pagination-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.dict-page-size__select {
  min-width: 92px;
  height: 32px;
  padding: 0 12px;
  color: #0f172a;
  background: #ffffff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  font-size: 13px;
}

.dict-page-list {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.dict-page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 10px;
  color: #334155;
  background: #ffffff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.dict-page-btn:hover:not(:disabled) {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.dict-page-btn.is-active {
  color: #ffffff;
  background: #1677ff;
  border-color: #1677ff;
}

.dict-page-btn:disabled {
  opacity: 0.52;
  cursor: not-allowed;
}

.dict-page-ellipsis {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  color: #94a3b8;
}
</style>
