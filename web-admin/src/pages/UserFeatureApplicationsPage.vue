<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card v-if="queryPanelVisible" class="admin-panel admin-filter-card" shadow="never">
          <bz-form class="admin-filter-form" :class="{ 'is-collapsed': queryCollapsed }" @submit.prevent="applyFilters">
            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">关键词</div>
                <div class="admin-filter-control">
                  <bz-input v-model="keywordDraft" placeholder="按编码或名称搜索" clearable @keyup.enter="applyFilters" />
                </div>
              </div>
            </bz-form-item>
            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">状态</div>
                <div class="admin-filter-control">
                  <bz-select v-model="enabledDraft" placeholder="全部状态" clearable>
                    <bz-option label="启用" value="true" />
                    <bz-option label="停用" value="false" />
                  </bz-select>
                </div>
              </div>
            </bz-form-item>
            <div class="admin-filter-actions">
              <bz-button class="admin-filter-secondary" @click="resetFilters">重置</bz-button>
              <bz-button class="admin-filter-primary" type="primary" native-type="submit">搜索</bz-button>
              <div class="admin-filter-toggle-placeholder" aria-hidden="true"></div>
            </div>
          </bz-form>
        </bz-card>

        <bz-card class="admin-panel admin-table-card" shadow="never">
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">应用配置</div>
              <div class="admin-table-tools">
                <button
                  class="admin-vben-circle-button"
                  :class="{ 'is-active': queryPanelVisible }"
                  type="button"
                  :title="queryPanelVisible ? '关闭搜索框' : '打开搜索框'"
                  @click="queryPanelVisible = !queryPanelVisible"
                >
                  <i class="admin-vben-circle-button__icon admin-vben-circle-button__icon--search" aria-hidden="true"></i>
                </button>
                <button class="admin-vben-circle-button" type="button" title="刷新列表" @click="reload">
                  <i class="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh" aria-hidden="true"></i>
                </button>
              </div>
            </div>
          </template>

          <div class="admin-table-surface">
              <bz-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无应用" size="small">
                <bz-table-column label="图标" width="80" align="center">
                  <template #default="scope">
                    <div class="application-icon-cell">
                      <img
                        v-if="resolveIconUrl(scope.row.icon)"
                        :src="resolveIconUrl(scope.row.icon) || undefined"
                        :alt="scope.row.name"
                        class="application-icon-cell__image"
                      />
                      <span v-else class="application-icon-cell__fallback">-</span>
                    </div>
                  </template>
                </bz-table-column>
                <bz-table-column prop="code" label="应用编码" min-width="160" show-overflow-tooltip />
                <bz-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
              <bz-table-column prop="routePath" label="路由" min-width="160" show-overflow-tooltip>
                <template #default="scope">{{ scope.row.routePath || "-" }}</template>
              </bz-table-column>
              <bz-table-column prop="componentPath" label="组件" min-width="220" show-overflow-tooltip>
                <template #default="scope">{{ scope.row.componentPath || "-" }}</template>
              </bz-table-column>
              <bz-table-column label="状态" width="100">
                <template #default="scope">
                  <bz-tag :type="scope.row.enabled ? 'success' : 'warning'">{{ scope.row.enabled ? "启用" : "停用" }}</bz-tag>
                </template>
              </bz-table-column>
              <bz-table-column label="功能数" width="90">
                <template #default="scope">{{ scope.row.featureCount }}</template>
              </bz-table-column>
              <bz-table-column label="权限绑定" width="100">
                <template #default="scope">{{ scope.row.permissionBindingCount }}</template>
              </bz-table-column>
              <bz-table-column label="操作" width="120" fixed="right">
                <template #default="scope">
                  <AdminActionBar :actions="getRowActions(scope.row)" />
                </template>
              </bz-table-column>
            </bz-table>
          </div>

          <div v-if="page.totalElements > 0" class="dict-pagination-bar">
            <div class="dict-pagination-summary">共 {{ page.totalElements }} 条记录</div>
            <div class="dict-pagination-right">
              <label class="dict-page-size">
                <select class="dict-page-size__select" :value="pageSize" @change="handlePageSizeSelect">
                  <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}条/页</option>
                </select>
              </label>
              <div class="dict-page-list">
                <button class="dict-page-btn dict-page-btn--icon" type="button" :disabled="isFirstPage" @click="goToPage(1)"><span aria-hidden="true">|&lt;</span></button>
                <button class="dict-page-btn dict-page-btn--icon" type="button" :disabled="isFirstPage" @click="goToPage(pageNo - 1)"><span aria-hidden="true">&lt;</span></button>
                <template v-for="(token, tokenIndex) in pageTokens" :key="`${String(token)}-${tokenIndex}`">
                  <button v-if="typeof token === 'number'" class="dict-page-btn" :class="{ 'is-active': token === pageNo }" type="button" @click="goToPage(token)">{{ token }}</button>
                  <span v-else class="dict-page-ellipsis">...</span>
                </template>
                <button class="dict-page-btn dict-page-btn--icon" type="button" :disabled="isLastPage" @click="goToPage(pageNo + 1)"><span aria-hidden="true">&gt;</span></button>
                <button class="dict-page-btn dict-page-btn--icon" type="button" :disabled="isLastPage" @click="goToPage(totalPages)"><span aria-hidden="true">&gt;|</span></button>
              </div>
            </div>
          </div>
        </bz-card>

        <AdminEntityDrawer :open="detailOpen" :loading="detailLoading" title="应用详情" width="980px" @close="detailOpen = false">
          <div v-if="detail" class="detail-grid">
            <div class="detail-field"><span class="detail-field__label">应用编码</span><span class="detail-field__value">{{ detail.code }}</span></div>
            <div class="detail-field"><span class="detail-field__label">名称</span><span class="detail-field__value">{{ detail.name }}</span></div>
            <div class="detail-field"><span class="detail-field__label">路由</span><span class="detail-field__value">{{ detail.routePath || "-" }}</span></div>
            <div class="detail-field"><span class="detail-field__label">组件</span><span class="detail-field__value">{{ detail.componentPath || "-" }}</span></div>
            <div class="detail-field"><span class="detail-field__label">状态</span><span class="detail-field__value">{{ detail.enabled ? "启用" : "停用" }}</span></div>
            <div class="detail-field">
              <span class="detail-field__label">图标</span>
              <span class="detail-field__value detail-field__value--icon">
                <img
                  v-if="resolveIconUrl(detail.icon)"
                  :src="resolveIconUrl(detail.icon) || undefined"
                  :alt="detail.name"
                  class="application-detail-icon"
                />
                <span>{{ detail.icon || "-" }}</span>
              </span>
            </div>
            <div class="detail-field detail-field--wide"><span class="detail-field__label">描述</span><span class="detail-field__value">{{ detail.description || "-" }}</span></div>
          </div>

          <div v-if="detail" class="app-feature-panel">
            <div class="app-feature-panel__head">
              <div class="app-feature-panel__title">应用功能</div>
              <div class="app-feature-panel__meta">共 {{ detail.features.length }} 项</div>
            </div>
            <div class="admin-table-surface">
              <bz-table :data="detail.features" row-key="id" size="small" empty-text="暂无功能">
                <bz-table-column prop="code" label="功能编码" min-width="180" show-overflow-tooltip />
                <bz-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
                <bz-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip>
                  <template #default="scope">{{ scope.row.description || "-" }}</template>
                </bz-table-column>
                <bz-table-column label="状态" width="100">
                  <template #default="scope"><bz-tag :type="scope.row.enabled ? 'success' : 'warning'">{{ scope.row.enabled ? "启用" : "停用" }}</bz-tag></template>
                </bz-table-column>
                <bz-table-column label="权限码" min-width="220" show-overflow-tooltip>
                  <template #default="scope">{{ scope.row.permissionCodes.length ? scope.row.permissionCodes.join(", ") : "-" }}</template>
                </bz-table-column>
              </bz-table>
            </div>
          </div>
          <template #footer><bz-button @click="detailOpen = false">关闭</bz-button></template>
        </AdminEntityDrawer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import {
  getUserFeatureApplication,
  pageUserFeatureApplications,
  updateUserFeatureApplicationStatus,
} from "../api/user-features";
import AdminActionBar from "../components/admin/AdminActionBar.vue";
import AdminEntityDrawer from "../components/admin/AdminEntityDrawer.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { AdminActionItem } from "../types/admin-action";
import type { PageResult } from "../types/page";
import type { UserFeatureApplicationEntry } from "../types/user-feature";
import { bzConfirm } from "../utils/confirm";
import { message } from "../utils/message";
import { resolveResourceIconUrl } from "../utils/resource-icon";

const loading = ref(false);
const rows = ref<UserFeatureApplicationEntry[]>([]);
const page = ref<PageResult<UserFeatureApplicationEntry>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});
const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref<UserFeatureApplicationEntry | null>(null);
const queryPanelVisible = ref(false);
const queryCollapsed = ref(true);
const keywordDraft = ref("");
const enabledDraft = ref<"" | "true" | "false">("");
const appliedKeyword = ref("");
const appliedEnabled = ref<"" | "true" | "false">("");
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;

const canView = computed(() => hasResourceCodeAccess("user-feature-application-view"));
const canToggle = computed(() => hasResourceCodeAccess("user-feature-application-edit"));
const totalPages = computed(() => Math.max(1, page.value.totalPages || 1));
const isFirstPage = computed(() => pageNo.value <= 1);
const isLastPage = computed(() => pageNo.value >= totalPages.value);
const pageTokens = computed<Array<number | "ellipsis">>(() => {
  const total = totalPages.value;
  const current = Math.min(Math.max(pageNo.value, 1), total);
  if (total <= 7) return Array.from({ length: total }, (_, index) => index + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", total];
  if (current >= total - 3) return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
});

onMounted(() => {
  void reload();
});

function resolveIconUrl(icon?: string | null) {
  return resolveResourceIconUrl(icon);
}

async function reload() {
  if (!canView.value) {
    rows.value = [];
    page.value = { pageNo: 1, pageSize: pageSize.value, numberOfElements: 0, totalPages: 0, totalElements: 0, elements: [] };
    return;
  }
  loading.value = true;
  try {
    const result = await pageUserFeatureApplications({
      keyword: appliedKeyword.value || undefined,
      enabled: appliedEnabled.value === "" ? "" : appliedEnabled.value === "true",
      page: { pageNo: pageNo.value, pageSize: pageSize.value },
    });
    page.value = result;
    pageNo.value = result.pageNo || 1;
    pageSize.value = result.pageSize || pageSize.value;
    rows.value = result.elements;
  } finally {
    loading.value = false;
  }
}

async function applyFilters() {
  appliedKeyword.value = keywordDraft.value.trim();
  appliedEnabled.value = enabledDraft.value;
  pageNo.value = 1;
  await reload();
}

async function resetFilters() {
  keywordDraft.value = "";
  enabledDraft.value = "";
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
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === pageSize.value) return;
  pageSize.value = nextPageSize;
  pageNo.value = 1;
  void reload();
}

function getRowActions(row: UserFeatureApplicationEntry): AdminActionItem[] {
  const actions: AdminActionItem[] = [
    { key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) },
  ];
  if (canToggle.value) {
    actions.push({
      key: `toggle-${row.id}`,
      label: row.enabled ? "停用" : "启用",
      tone: row.enabled ? "disable" : "enable",
      handler: () => toggleStatus(row),
    });
  }
  return actions;
}

async function openDetail(id: string) {
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getUserFeatureApplication(id);
  } finally {
    detailLoading.value = false;
  }
}

async function toggleStatus(row: UserFeatureApplicationEntry) {
  if (!canToggle.value) return;
  const nextEnabled = !row.enabled;
  const confirmed = await bzConfirm({
    title: nextEnabled ? "启用应用" : "停用应用",
    message: `确认${nextEnabled ? "启用" : "停用"}：${row.name}？`,
    confirmText: nextEnabled ? "启用" : "停用",
    cancelText: "取消",
    danger: !nextEnabled,
  });
  if (!confirmed) return;
  await updateUserFeatureApplicationStatus(row.id, nextEnabled);
  message.success(nextEnabled ? "已启用" : "已停用");
  await reload();
  if (detailOpen.value && detail.value?.id === row.id) {
    detail.value = await getUserFeatureApplication(row.id);
  }
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
  background: #fff;
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
  background: #fff;
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
  color: #fff;
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

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.detail-field {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  min-width: 0;
}

.detail-field--wide {
  grid-column: 1 / -1;
}

.detail-field__label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  line-height: 1.75;
}

.detail-field__value {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.detail-field__value--icon {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.application-icon-cell {
  width: 28px;
  height: 28px;
  margin: 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.application-icon-cell__image,
.application-detail-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.application-icon-cell__fallback {
  color: #94a3b8;
}

.app-feature-panel {
  margin-top: 18px;
}

.app-feature-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.app-feature-panel__title {
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
}

.app-feature-panel__meta {
  font-size: 12px;
  color: #64748b;
}
</style>
