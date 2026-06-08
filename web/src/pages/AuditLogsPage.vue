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
                <div class="admin-filter-label">追踪ID</div>
                <div class="admin-filter-control">
                  <bz-input
                    v-model="traceIdDraft"
                    placeholder="按追踪ID搜索"
                    clearable
                    @keyup.enter="applyFilters"
                  />
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">操作人</div>
                <div class="admin-filter-control">
                  <bz-input
                    v-model="operatorUsernameDraft"
                    placeholder="按操作人搜索"
                    clearable
                    @keyup.enter="applyFilters"
                  />
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">资源</div>
                <div class="admin-filter-control">
                  <bz-select
                    v-model="auditResourceDraft"
                    placeholder="全部资源"
                    clearable
                  >
                    <bz-option
                      v-for="item in auditResourceOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </bz-select>
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">动作</div>
                <div class="admin-filter-control">
                  <bz-select
                    v-model="auditActionDraft"
                    placeholder="全部动作"
                    clearable
                  >
                    <bz-option
                      v-for="item in auditActionOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </bz-select>
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">等级</div>
                <div class="admin-filter-control">
                  <bz-select
                    v-model="auditLevelDraft"
                    placeholder="全部等级"
                    clearable
                  >
                    <bz-option
                      v-for="item in auditLevelOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </bz-select>
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">结果</div>
                <div class="admin-filter-control">
                  <bz-select
                    v-model="successDraft"
                    placeholder="全部结果"
                    clearable
                  >
                    <bz-option
                      label="成功"
                      value="true"
                    />
                    <bz-option
                      label="失败"
                      value="false"
                    />
                  </bz-select>
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
                >重置</bz-button
              >
              <bz-button
                class="admin-filter-primary"
                type="primary"
                native-type="submit"
                >搜索</bz-button
              >
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
              <div class="admin-table-title">审计日志</div>
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
            description="无权限查看审计日志"
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
                  prop="traceId"
                  label="追踪ID"
                  width="180"
                  show-overflow-tooltip
                />
                <bz-table-column
                  prop="operatorUsername"
                  label="操作人"
                  width="120"
                >
                  <template #default="scope">{{ scope.row.operatorUsername || "-" }}</template>
                </bz-table-column>
                <bz-table-column
                  label="用户类型"
                  width="110"
                >
                  <template #default="scope">
                    <bz-tag
                      size="small"
                      :type="resolveTagType(userTypeMetaMap, scope.row.operatorUserType)"
                    >
                      {{ resolveLabel(userTypeMetaMap, scope.row.operatorUserType) }}
                    </bz-tag>
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="资源"
                  width="140"
                >
                  <template #default="scope">
                    <bz-tag size="small">{{
                      resolveLabel(auditResourceMetaMap, scope.row.auditResource)
                    }}</bz-tag>
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="动作"
                  width="140"
                >
                  <template #default="scope">
                    <bz-tag size="small">{{
                      resolveLabel(auditActionMetaMap, scope.row.auditAction)
                    }}</bz-tag>
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="等级"
                  width="110"
                >
                  <template #default="scope">
                    <bz-tag
                      size="small"
                      :type="resolveTagType(auditLevelMetaMap, scope.row.auditLevel)"
                    >
                      {{ resolveLabel(auditLevelMetaMap, scope.row.auditLevel) }}
                    </bz-tag>
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
                  prop="requestUri"
                  label="请求地址"
                  min-width="240"
                  show-overflow-tooltip
                />
                <bz-table-column
                  prop="requestIp"
                  label="请求IP"
                  width="140"
                >
                  <template #default="scope">{{ scope.row.requestIp || "-" }}</template>
                </bz-table-column>
                <bz-table-column
                  label="时间"
                  width="180"
                >
                  <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
                </bz-table-column>
                <bz-table-column
                  label="操作"
                  width="88"
                  fixed="right"
                >
                  <template #default="scope">
                    <AdminActionBar :actions="getRowActions(scope.row)" />
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
                      >...</span
                    >
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

        <AdminEntityDrawer
          :open="detailOpen"
          :loading="detailLoading"
          title="审计日志详情"
          width="960px"
          @close="detailOpen = false"
        >
          <div
            v-if="detail"
            class="audit-detail-layout"
          >
            <section class="audit-detail-section">
              <div class="audit-detail-section__title">基础信息</div>
              <div class="audit-detail-grid">
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">追踪ID</span
                  ><span class="audit-detail-field__value">{{ detail.traceId || "-" }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">请求ID</span
                  ><span class="audit-detail-field__value">{{ detail.requestId || "-" }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">操作人</span
                  ><span class="audit-detail-field__value">{{
                    detail.operatorUsername || "-"
                  }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">用户类型</span
                  ><span class="audit-detail-field__value">{{
                    resolveLabel(userTypeMetaMap, detail.operatorUserType)
                  }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">资源</span
                  ><span class="audit-detail-field__value">{{
                    resolveLabel(auditResourceMetaMap, detail.auditResource)
                  }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">动作</span
                  ><span class="audit-detail-field__value">{{
                    resolveLabel(auditActionMetaMap, detail.auditAction)
                  }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">等级</span
                  ><span class="audit-detail-field__value">{{
                    resolveLabel(auditLevelMetaMap, detail.auditLevel)
                  }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">结果</span
                  ><span class="audit-detail-field__value">{{
                    detail.success ? "成功" : "失败"
                  }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">协议</span
                  ><span class="audit-detail-field__value">{{
                    resolveLabel(apiProtocolMetaMap, detail.protocol)
                  }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">方法</span
                  ><span class="audit-detail-field__value">{{
                    resolveLabel(apiMethodMetaMap, detail.httpMethod)
                  }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">请求IP</span
                  ><span class="audit-detail-field__value">{{ detail.requestIp || "-" }}</span>
                </div>
                <div class="audit-detail-field">
                  <span class="audit-detail-field__label">耗时</span
                  ><span class="audit-detail-field__value">{{
                    formatDuration(detail.durationMs)
                  }}</span>
                </div>
                <div class="audit-detail-field audit-detail-field--wide">
                  <span class="audit-detail-field__label">请求地址</span
                  ><span class="audit-detail-field__value">{{ detail.requestUri || "-" }}</span>
                </div>
                <div class="audit-detail-field audit-detail-field--wide">
                  <span class="audit-detail-field__label">路径模式</span
                  ><span class="audit-detail-field__value">{{ detail.pathPattern || "-" }}</span>
                </div>
                <div class="audit-detail-field audit-detail-field--wide">
                  <span class="audit-detail-field__label">审计描述</span
                  ><span class="audit-detail-field__value">{{
                    detail.auditDescription || "-"
                  }}</span>
                </div>
                <div class="audit-detail-field audit-detail-field--wide">
                  <span class="audit-detail-field__label">权限码</span
                  ><span class="audit-detail-field__value">{{
                    detail.permissionCodes.length ? detail.permissionCodes.join(", ") : "-"
                  }}</span>
                </div>
                <div class="audit-detail-field audit-detail-field--wide">
                  <span class="audit-detail-field__label">记录时间</span
                  ><span class="audit-detail-field__value">{{
                    formatDateTime(detail.createdAt)
                  }}</span>
                </div>
              </div>
            </section>

            <section class="audit-detail-section">
              <div class="audit-detail-section__title">请求与响应</div>
              <div class="audit-detail-text-grid">
                <div class="audit-detail-text-block">
                  <div class="audit-detail-text-block__label">请求参数</div>
                  <pre class="audit-detail-text-block__content">{{
                    detail.requestParamSummary || "-"
                  }}</pre>
                </div>
                <div class="audit-detail-text-block">
                  <div class="audit-detail-text-block__label">请求体</div>
                  <pre class="audit-detail-text-block__content">{{
                    detail.requestBodySummary || "-"
                  }}</pre>
                </div>
                <div class="audit-detail-text-block">
                  <div class="audit-detail-text-block__label">响应体</div>
                  <pre class="audit-detail-text-block__content">{{
                    detail.responseSummary || "-"
                  }}</pre>
                </div>
                <div class="audit-detail-text-block">
                  <div class="audit-detail-text-block__label">错误信息</div>
                  <pre class="audit-detail-text-block__content">{{
                    detail.errorMessage || detail.errorCode || "-"
                  }}</pre>
                </div>
                <div class="audit-detail-text-block audit-detail-text-block--wide">
                  <div class="audit-detail-text-block__label">User-Agent</div>
                  <pre class="audit-detail-text-block__content">{{ detail.userAgent || "-" }}</pre>
                </div>
              </div>
            </section>
          </div>

          <template #footer>
            <bz-button @click="detailOpen = false">关闭</bz-button>
          </template>
        </AdminEntityDrawer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { getAuditLog, pageAuditLogs } from "../api/audit-logs";
import { batchListDictOptions } from "../api/dicts";
import AdminActionBar from "../components/admin/AdminActionBar.vue";
import AdminEntityDrawer from "../components/admin/AdminEntityDrawer.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { AdminActionItem } from "../types/admin-action";
import type { AuditLevel, AuditLogEntry } from "../types/audit-log";
import type { DictItem } from "../types/dict-admin";
import type { PageResult } from "../types/page";
import {
  dateTimeInputToEpochMillisString,
  dateTimeInputToNextMinuteEpochMillisString,
  formatDateTime,
} from "../utils/formatter";

type DictMeta = { label: string; tagType?: string | null };

const AUDIT_DICT_CODES = [
  "AUDIT_RESOURCE",
  "AUDIT_ACTION",
  "AUDIT_LEVEL",
  "API_METHOD",
  "API_PROTOCOL",
  "USER_TYPE",
] as const;

const loading = ref(false);
const rows = ref<AuditLogEntry[]>([]);
const page = ref<PageResult<AuditLogEntry>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});

const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref<AuditLogEntry | null>(null);

const queryPanelVisible = ref(false);
const queryCollapsed = ref(true);
const traceIdDraft = ref("");
const operatorUsernameDraft = ref("");
const auditResourceDraft = ref("");
const auditActionDraft = ref("");
const auditLevelDraft = ref<"" | AuditLevel>("");
const successDraft = ref<"" | "true" | "false">("");
const startAtDraft = ref("");
const endAtDraft = ref("");

const appliedTraceId = ref("");
const appliedOperatorUsername = ref("");
const appliedAuditResource = ref("");
const appliedAuditAction = ref("");
const appliedAuditLevel = ref<"" | AuditLevel>("");
const appliedSuccess = ref<"" | "true" | "false">("");
const appliedStartAt = ref("");
const appliedEndAt = ref("");
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;

const auditResourceMetaMap = ref<Record<string, DictMeta>>({});
const auditActionMetaMap = ref<Record<string, DictMeta>>({});
const auditLevelMetaMap = ref<Record<string, DictMeta>>({});
const apiMethodMetaMap = ref<Record<string, DictMeta>>({});
const apiProtocolMetaMap = ref<Record<string, DictMeta>>({});
const userTypeMetaMap = ref<Record<string, DictMeta>>({});

const auditResourceOptions = computed(() => toOptions(auditResourceMetaMap.value));
const auditActionOptions = computed(() => toOptions(auditActionMetaMap.value));
const auditLevelOptions = computed(() => toOptions(auditLevelMetaMap.value));

const canView = computed(() => hasResourceCodeAccess("audit-log-view"));
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

onMounted(async () => {
  await Promise.all([loadDictionaries(), reload()]);
});

async function loadDictionaries() {
  try {
    const result = await batchListDictOptions([...AUDIT_DICT_CODES]);
    auditResourceMetaMap.value = toMetaMap(result.AUDIT_RESOURCE);
    auditActionMetaMap.value = toMetaMap(result.AUDIT_ACTION);
    auditLevelMetaMap.value = toMetaMap(result.AUDIT_LEVEL);
    apiMethodMetaMap.value = toMetaMap(result.API_METHOD);
    apiProtocolMetaMap.value = toMetaMap(result.API_PROTOCOL);
    userTypeMetaMap.value = toMetaMap(result.USER_TYPE);
  } catch {
    auditResourceMetaMap.value = {};
    auditActionMetaMap.value = {};
    auditLevelMetaMap.value = {};
    apiMethodMetaMap.value = {};
    apiProtocolMetaMap.value = {};
    userTypeMetaMap.value = {};
  }
}

function toMetaMap(items?: DictItem[]): Record<string, DictMeta> {
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

function toOptions(metaMap: Record<string, DictMeta>) {
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
    let result = await pageAuditLogs({
      traceId: appliedTraceId.value || undefined,
      operatorUsername: appliedOperatorUsername.value || undefined,
      auditResource: appliedAuditResource.value || undefined,
      auditAction: appliedAuditAction.value || undefined,
      auditLevel: appliedAuditLevel.value || undefined,
      success: appliedSuccess.value === "" ? undefined : appliedSuccess.value === "true",
      startAt: toInstant(appliedStartAt.value),
      endAt: toEndExclusive(appliedEndAt.value),
      page: { pageNo: requestedPageNo, pageSize: pageSize.value },
    });

    if (result.totalElements > 0 && requestedPageNo > Math.max(1, result.totalPages)) {
      pageNo.value = Math.max(1, result.totalPages);
      result = await pageAuditLogs({
        traceId: appliedTraceId.value || undefined,
        operatorUsername: appliedOperatorUsername.value || undefined,
        auditResource: appliedAuditResource.value || undefined,
        auditAction: appliedAuditAction.value || undefined,
        auditLevel: appliedAuditLevel.value || undefined,
        success: appliedSuccess.value === "" ? undefined : appliedSuccess.value === "true",
        startAt: toInstant(appliedStartAt.value),
        endAt: toEndExclusive(appliedEndAt.value),
        page: { pageNo: pageNo.value, pageSize: pageSize.value },
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
  appliedTraceId.value = traceIdDraft.value.trim();
  appliedOperatorUsername.value = operatorUsernameDraft.value.trim();
  appliedAuditResource.value = auditResourceDraft.value;
  appliedAuditAction.value = auditActionDraft.value;
  appliedAuditLevel.value = auditLevelDraft.value;
  appliedSuccess.value = successDraft.value;
  appliedStartAt.value = startAtDraft.value;
  appliedEndAt.value = endAtDraft.value;
  pageNo.value = 1;
  await reload();
}

async function resetFilters() {
  traceIdDraft.value = "";
  operatorUsernameDraft.value = "";
  auditResourceDraft.value = "";
  auditActionDraft.value = "";
  auditLevelDraft.value = "";
  successDraft.value = "";
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
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === pageSize.value)
    return;
  pageSize.value = nextPageSize;
  pageNo.value = 1;
  void reload();
}

function getRowActions(row: AuditLogEntry): AdminActionItem[] {
  return [
    { key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) },
  ];
}

async function openDetail(id: string) {
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getAuditLog(id);
  } finally {
    detailLoading.value = false;
  }
}

function formatDuration(value?: number | null): string {
  if (value === null || value === undefined) return "-";
  return `${value} ms`;
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
  font-size: 12px;
  color: var(--text-main);
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

.audit-detail-layout {
  display: grid;
  gap: 18px;
}

.audit-detail-section {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  padding: 18px;
}

.audit-detail-section__title {
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.audit-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.audit-detail-field {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  min-width: 0;
}

.audit-detail-field--wide {
  grid-column: 1 / -1;
}

.audit-detail-field__label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  line-height: 1.75;
}

.audit-detail-field__value {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.audit-detail-text-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.audit-detail-text-block {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
  padding: 12px;
  min-width: 0;
}

.audit-detail-text-block--wide {
  grid-column: 1 / -1;
}

.audit-detail-text-block__label {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.audit-detail-text-block__content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.6;
  font-family: inherit;
}

@media (max-width: 900px) {
  .audit-detail-grid,
  .audit-detail-text-grid {
    grid-template-columns: 1fr;
  }
}
</style>
