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
                <div class="admin-filter-label">关键字</div>
                <div class="admin-filter-control">
                  <bz-input
                    v-model="keywordDraft"
                    placeholder="搜索模块、路径、处理器、访问类型"
                    clearable
                    @keyup.enter="applyFilters"
                  />
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">模块</div>
                <div class="admin-filter-control">
                  <bz-select
                    v-model="moduleDraft"
                    placeholder="请选择模块"
                    clearable
                  >
                    <bz-option
                      v-for="option in moduleOptions"
                      :key="option"
                      :label="option"
                      :value="option"
                    />
                  </bz-select>
                </div>
              </div>
            </bz-form-item>

            <bz-form-item
              v-if="!queryCollapsed"
              class="admin-filter-item"
            >
              <div class="admin-filter-field">
                <div class="admin-filter-label">状态</div>
                <div class="admin-filter-control">
                  <bz-select
                    v-model="statusDraft"
                    placeholder="请选择状态"
                    clearable
                  >
                    <bz-option
                      label="启用"
                      value="enabled"
                    />
                    <bz-option
                      label="停用"
                      value="disabled"
                    />
                  </bz-select>
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
              <button
                class="admin-filter-toggle"
                type="button"
                @click="queryCollapsed = !queryCollapsed"
              >
                <span>{{ queryCollapsed ? "展开" : "收起" }}</span>
                <i
                  class="admin-filter-toggle__icon"
                  :class="queryCollapsed ? 'is-down' : 'is-up'"
                  aria-hidden="true"
                ></i>
              </button>
            </div>
          </bz-form>
        </bz-card>

        <bz-card
          class="admin-panel admin-table-card"
          shadow="never"
        >
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">接口列表</div>
              <div class="admin-table-tools">
                <bz-button
                  class="admin-toolbar-primary"
                  type="primary"
                  @click="reload"
                >
                  <span class="admin-toolbar-primary__content">
                    <i
                      class="admin-toolbar-primary__icon admin-toolbar-primary__icon--reload"
                      aria-hidden="true"
                    ></i>
                    <span>刷新接口</span>
                  </span>
                </bz-button>
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

          <div class="admin-table-surface">
            <ApiTable
              :rows="pagedRows"
              :loading="loading"
              :can-publish="canPublish"
              :can-disable="canDisable"
              @publish="onPublish"
              @disable="onDisable"
            />
          </div>

          <div
            v-if="filteredRows.length > 0"
            class="dict-pagination-bar"
          >
            <div class="dict-pagination-summary">共 {{ filteredRows.length }} 条记录</div>
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
        </bz-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { disableApi, listApis, publishApi } from "../api/apis";
import { batchListDictOptions } from "../api/dicts";
import ApiTable from "../components/apis-admin/ApiTable.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { ApiEntry } from "../types/api-admin";
import type { DictItem } from "../types/dict-admin";

const API_DICT_CODES = ["API_METHOD", "API_PROTOCOL", "API_ACCESS_TYPE"] as const;

const USER_TYPE_LABELS: Record<string, string> = {
  SYSTEM: "系统账号",
  INTERNAL: "账号",
  EXTERNAL: "用户",
  GUEST: "游客",
};

const loading = ref(false);
const rows = ref<ApiEntry[]>([]);
const queryPanelVisible = ref(false);
const queryCollapsed = ref(false);
const keywordDraft = ref("");
const moduleDraft = ref<string | undefined>();
const statusDraft = ref<string | undefined>();
const appliedKeyword = ref("");
const appliedModule = ref<string | undefined>();
const appliedStatus = ref<string | undefined>();
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100, 200] as const;

const methodLabelMap = ref<Record<string, string>>({});
const protocolLabelMap = ref<Record<string, string>>({});
const accessTypeLabelMap = ref<Record<string, string>>({});

const canPublish = computed(() => hasResourceCodeAccess("api-manage-publish"));
const canDisable = computed(() => hasResourceCodeAccess("api-manage-disable"));
const moduleOptions = computed(() =>
  Array.from(
    new Set(
      rows.value.map((item) => item.module?.trim()).filter((item): item is string => Boolean(item)),
    ),
  ).sort((left, right) => left.localeCompare(right)),
);

onMounted(async () => {
  await Promise.all([loadDictionaries(), reload()]);
});

async function loadDictionaries() {
  try {
    const result = await batchListDictOptions([...API_DICT_CODES]);
    methodLabelMap.value = toLabelMap(result.API_METHOD);
    protocolLabelMap.value = toLabelMap(result.API_PROTOCOL);
    accessTypeLabelMap.value = toLabelMap(result.API_ACCESS_TYPE);
  } catch {
    methodLabelMap.value = {};
    protocolLabelMap.value = {};
    accessTypeLabelMap.value = {};
  }
}

function toLabelMap(items?: DictItem[]): Record<string, string> {
  const map: Record<string, string> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = item.itemLabel || item.itemValue;
  }
  return map;
}

async function reload() {
  loading.value = true;
  try {
    rows.value = await listApis();
    syncPageNoWithinRange();
  } finally {
    loading.value = false;
  }
}

const enrichedRows = computed<ApiEntry[]>(() =>
  rows.value.map((api) => ({
    ...api,
    protocolLabel: protocolLabelMap.value[api.protocol] || api.protocol,
    httpMethodLabel: methodLabelMap.value[api.httpMethod] || api.httpMethod,
    accessTypeLabel: accessTypeLabelMap.value[api.accessType] || api.accessType,
    userTypeLabels: resolveUserTypeLabels(api.userTypes),
    auditTooltip: buildAuditTooltip(api),
  })),
);

const filteredRows = computed(() => {
  const kw = appliedKeyword.value.trim().toLowerCase();
  const moduleValue = appliedModule.value?.trim() || "";
  const statusValue = appliedStatus.value?.trim() || "";

  return enrichedRows.value.filter((api) => {
    if (moduleValue && api.module !== moduleValue) {
      return false;
    }
    if (statusValue === "enabled" && !api.enabled) {
      return false;
    }
    if (statusValue === "disabled" && api.enabled) {
      return false;
    }
    if (!kw) {
      return true;
    }
    const text = [
      api.module,
      api.protocol,
      api.protocolLabel,
      api.httpMethod,
      api.httpMethodLabel,
      api.pathPattern,
      api.handlerClass,
      api.handlerMethod,
      api.accessType,
      api.accessTypeLabel,
      api.userTypes,
      ...(api.userTypeLabels || []),
      api.auditResource,
      api.auditAction,
      api.auditDescription,
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    return text.includes(kw);
  });
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)),
);
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
const pagedRows = computed(() => {
  const start = (pageNo.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

function applyFilters() {
  appliedKeyword.value = keywordDraft.value;
  appliedModule.value = moduleDraft.value;
  appliedStatus.value = statusDraft.value;
  pageNo.value = 1;
}

function resetFilters() {
  keywordDraft.value = "";
  moduleDraft.value = undefined;
  statusDraft.value = undefined;
  applyFilters();
}

function goToPage(nextPage: number) {
  const target = Math.min(Math.max(nextPage, 1), totalPages.value);
  if (target === pageNo.value) return;
  pageNo.value = target;
}

function syncPageNoWithinRange() {
  const maxPageNo = Math.max(1, totalPages.value);
  if (pageNo.value > maxPageNo) {
    pageNo.value = maxPageNo;
  }
}

function handlePageSizeSelect(event: Event) {
  const target = event.target as HTMLSelectElement;
  const nextPageSize = Number(target.value);
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === pageSize.value) {
    return;
  }
  pageSize.value = nextPageSize;
  pageNo.value = 1;
}

function resolveUserTypeLabels(raw?: string): string[] {
  const codes = parseUserTypes(raw);
  return codes.map((code) => USER_TYPE_LABELS[code] || code);
}

function parseUserTypes(raw?: string): string[] {
  const normalized = raw?.trim();
  if (!normalized) {
    return [];
  }
  try {
    const parsed = JSON.parse(normalized);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => String(item).trim()).filter(Boolean);
    }
  } catch {
    // ignore
  }
  return normalized
    .replace(/^\[|\]$/g, "")
    .split(",")
    .map((item) => item.replace(/^["'\s]+|["'\s]+$/g, ""))
    .filter(Boolean);
}

function buildAuditTooltip(api: ApiEntry): string {
  if (!api.auditDeclared) {
    return "";
  }
  const lines = [
    api.auditResource ? `审计资源：${api.auditResource}` : "",
    api.auditAction ? `审计动作：${api.auditAction}` : "",
    api.auditDescription ? `审计描述：${api.auditDescription}` : "",
  ].filter(Boolean);
  return lines.join("\n");
}

async function onPublish(api: ApiEntry) {
  if (!canPublish.value) return;
  await publishApi(api.id);
  await reload();
}

async function onDisable(api: ApiEntry) {
  if (!canDisable.value) return;
  await disableApi(api.id);
  await reload();
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

.dict-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease;
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
