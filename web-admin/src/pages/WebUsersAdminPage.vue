<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card v-if="queryPanelVisible" class="admin-panel admin-filter-card" shadow="never">
          <bz-form class="admin-filter-form" :class="{ 'is-collapsed': queryCollapsed }" @submit.prevent="applyFilters">
            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field"><div class="admin-filter-label">关键词</div><div class="admin-filter-control"><bz-input v-model="keywordDraft" placeholder="按账号或昵称搜索" clearable @keyup.enter="applyFilters" /></div></div>
            </bz-form-item>
            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field"><div class="admin-filter-label">状态</div><div class="admin-filter-control"><bz-select v-model="statusDraft" placeholder="全部状态" clearable><bz-option label="启用" value="ACTIVE" /><bz-option label="停用" value="DISABLED" /><bz-option label="已注销" value="CANCELLED" /></bz-select></div></div>
            </bz-form-item>
            <div class="admin-filter-actions"><bz-button class="admin-filter-secondary" @click="resetFilters">重置</bz-button><bz-button class="admin-filter-primary" type="primary" native-type="submit">搜索</bz-button><div class="admin-filter-toggle-placeholder" aria-hidden="true"></div></div>
          </bz-form>
        </bz-card>

        <bz-card class="admin-panel admin-table-card" shadow="never">
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">用户管理</div>
              <div class="admin-table-tools">
                <button class="admin-vben-circle-button" :class="{ 'is-active': queryPanelVisible }" type="button" :title="queryPanelVisible ? '关闭搜索框' : '打开搜索框'" @click="queryPanelVisible = !queryPanelVisible"><i class="admin-vben-circle-button__icon admin-vben-circle-button__icon--search" aria-hidden="true"></i></button>
                <button class="admin-vben-circle-button" type="button" title="刷新列表" @click="reload"><i class="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh" aria-hidden="true"></i></button>
              </div>
            </div>
          </template>

          <div class="admin-table-surface">
            <bz-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无用户" size="small">
              <bz-table-column prop="account" label="账号" min-width="180" show-overflow-tooltip />
              <bz-table-column prop="nickname" label="昵称" min-width="160" show-overflow-tooltip />
              <bz-table-column label="状态" width="100"><template #default="scope"><bz-tag :type="resolveStatusType(scope.row.status)">{{ resolveStatusLabel(scope.row.status) }}</bz-tag></template></bz-table-column>
              <bz-table-column label="最近登录" min-width="160"><template #default="scope">{{ formatDateTime(scope.row.lastLoginAt) || '-' }}</template></bz-table-column>
              <bz-table-column label="创建时间" min-width="160"><template #default="scope">{{ formatDateTime(scope.row.createdAt) || '-' }}</template></bz-table-column>
              <bz-table-column label="操作" width="160" fixed="right"><template #default="scope"><AdminActionBar :actions="getRowActions(scope.row)" /></template></bz-table-column>
            </bz-table>
          </div>

          <div v-if="page.totalElements > 0" class="dict-pagination-bar">
            <div class="dict-pagination-summary">共 {{ page.totalElements }} 条记录</div>
            <div class="dict-pagination-right">
              <label class="dict-page-size"><select class="dict-page-size__select" :value="pageSize" @change="handlePageSizeSelect"><option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}条/页</option></select></label>
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

        <AdminEntityDrawer :open="detailOpen" :loading="detailLoading" title="用户详情" width="860px" @close="detailOpen = false">
          <div v-if="detail" class="detail-grid">
            <div class="detail-field"><span class="detail-field__label">账号</span><span class="detail-field__value">{{ detail.account }}</span></div>
            <div class="detail-field"><span class="detail-field__label">昵称</span><span class="detail-field__value">{{ detail.nickname || '-' }}</span></div>
            <div class="detail-field"><span class="detail-field__label">状态</span><span class="detail-field__value">{{ resolveStatusLabel(detail.status) }}</span></div>
            <div class="detail-field"><span class="detail-field__label">最近登录</span><span class="detail-field__value">{{ formatDateTime(detail.lastLoginAt) || '-' }}</span></div>
            <div class="detail-field"><span class="detail-field__label">创建时间</span><span class="detail-field__value">{{ formatDateTime(detail.createdAt) || '-' }}</span></div>
            <div class="detail-field"><span class="detail-field__label">更新时间</span><span class="detail-field__value">{{ formatDateTime(detail.updatedAt) || '-' }}</span></div>
          </div>
          <template #footer><bz-button @click="detailOpen = false">关闭</bz-button></template>
        </AdminEntityDrawer>

        <AdminEntityDrawer :open="featureOpen" :loading="featureLoading" title="用户应用功能配置" width="1180px" @close="featureOpen = false">
          <div v-if="featureManagement" class="feature-manage-layout">
            <div class="feature-manage-cards">
              <div class="feature-manage-card"><span>当前用户</span><strong>{{ featureManagement.account }}</strong></div>
              <div class="feature-manage-card"><span>已选应用包</span><strong>{{ selectedPackageIds.length }}</strong></div>
              <div class="feature-manage-card"><span>应用数</span><strong>{{ applicationStates.length }}</strong></div>
              <div class="feature-manage-card"><span>功能项</span><strong>{{ flattenedFeatures.length }}</strong></div>
            </div>

            <div class="feature-section">
              <div class="feature-section__head">
                <div class="feature-section__title">用户应用包</div>
                <div class="feature-section__meta">勾选后参与权限继承计算</div>
              </div>
              <div class="package-option-grid">
                <label v-for="pkg in packageEntries" :key="pkg.id" class="package-option-card">
                  <input type="checkbox" :checked="selectedPackageIds.includes(pkg.id)" @change="togglePackageSelection(pkg.id, ($event.target as HTMLInputElement).checked)" />
                  <div class="package-option-card__body">
                    <div class="package-option-card__top"><strong>{{ pkg.name }}</strong><bz-tag :type="pkg.defaultPackage ? 'success' : 'info'">{{ pkg.defaultPackage ? '默认包' : '普通包' }}</bz-tag></div>
                    <div class="package-option-card__code">{{ pkg.code }}</div>
                    <div class="package-option-card__desc">{{ pkg.description || '无描述' }}</div>
                  </div>
                </label>
              </div>
            </div>

            <div class="feature-section">
              <div class="feature-section__head"><div class="feature-section__title">应用特例</div><div class="feature-section__meta">优先级高于应用包授权</div></div>
              <div class="admin-table-surface">
                <bz-table :data="applicationStates" row-key="id" size="small" empty-text="暂无应用">
                  <bz-table-column prop="name" label="应用" min-width="180" show-overflow-tooltip>
                    <template #default="scope"><div class="table-title-cell"><strong>{{ scope.row.name }}</strong><small>{{ scope.row.code }}</small></div></template>
                  </bz-table-column>
                  <bz-table-column label="继承可见" width="100"><template #default="scope"><bz-tag :type="scope.row.inheritedVisible ? 'success' : 'info'">{{ scope.row.inheritedVisible ? '是' : '否' }}</bz-tag></template></bz-table-column>
                  <bz-table-column label="继承范围" width="110"><template #default="scope">{{ scope.row.packageAccessScope }}</template></bz-table-column>
                  <bz-table-column label="最终可见" width="100"><template #default="scope"><bz-tag :type="scope.row.effectiveVisible ? 'success' : 'warning'">{{ scope.row.effectiveVisible ? '是' : '否' }}</bz-tag></template></bz-table-column>
                  <bz-table-column label="特例类型" width="140">
                    <template #default="scope">
                      <select class="inline-select" :value="scope.row.overrideType" @change="updateApplicationOverride(scope.row.id, ($event.target as HTMLSelectElement).value as UserFeatureOverrideType)">
                        <option value="NONE">继承</option>
                        <option value="ENABLE">单独启用</option>
                        <option value="DISABLE">单独禁用</option>
                      </select>
                    </template>
                  </bz-table-column>
                  <bz-table-column label="启用范围" width="140">
                    <template #default="scope">
                      <select class="inline-select" :disabled="scope.row.overrideType !== 'ENABLE'" :value="scope.row.overrideAccessScope || 'FULL'" @change="updateApplicationOverrideScope(scope.row.id, ($event.target as HTMLSelectElement).value as UserFeatureAccessScope)">
                        <option value="FULL">完整功能</option>
                        <option value="PARTIAL">部分功能</option>
                      </select>
                    </template>
                  </bz-table-column>
                </bz-table>
              </div>
            </div>

            <div class="feature-section">
              <div class="feature-section__head"><div class="feature-section__title">功能特例</div><div class="feature-section__meta">用于补充单个功能的启用或禁用</div></div>
              <div class="feature-filter-bar">
                <bz-input v-model="featureKeyword" placeholder="搜索应用、功能编码或名称" clearable />
                <select class="inline-select inline-select--filter" v-model="featureOverrideFilter">
                  <option value="">全部特例</option>
                  <option value="NONE">继承</option>
                  <option value="ENABLE">单独启用</option>
                  <option value="DISABLE">单独禁用</option>
                </select>
              </div>
              <div class="admin-table-surface">
                <bz-table :data="filteredFeatures" row-key="id" size="small" empty-text="暂无功能项">
                  <bz-table-column label="所属应用" min-width="160" show-overflow-tooltip>
                    <template #default="scope">{{ applicationName(scope.row.applicationId) }}</template>
                  </bz-table-column>
                  <bz-table-column prop="code" label="功能编码" min-width="170" show-overflow-tooltip />
                  <bz-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
                  <bz-table-column label="继承可用" width="100"><template #default="scope"><bz-tag :type="scope.row.inheritedEnabled ? 'success' : 'info'">{{ scope.row.inheritedEnabled ? '是' : '否' }}</bz-tag></template></bz-table-column>
                  <bz-table-column label="最终可用" width="100"><template #default="scope"><bz-tag :type="scope.row.effectiveEnabled ? 'success' : 'warning'">{{ scope.row.effectiveEnabled ? '是' : '否' }}</bz-tag></template></bz-table-column>
                  <bz-table-column label="特例类型" width="140">
                    <template #default="scope">
                      <select class="inline-select" :value="scope.row.overrideType" @change="updateFeatureOverride(scope.row.applicationId, scope.row.id, ($event.target as HTMLSelectElement).value as UserFeatureOverrideType)">
                        <option value="NONE">继承</option>
                        <option value="ENABLE">单独启用</option>
                        <option value="DISABLE">单独禁用</option>
                      </select>
                    </template>
                  </bz-table-column>
                </bz-table>
              </div>
            </div>
          </div>
          <template #footer><bz-button @click="featureOpen = false">取消</bz-button><bz-button v-if="canFeatureSave" type="primary" @click="saveFeatureManagement">确定</bz-button></template>
        </AdminEntityDrawer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { getExternalUser, pageExternalUsers, updateExternalUser } from "../api/external-users";
import { getUserFeatureUserManagement, pageUserFeaturePackages, saveUserFeatureUserManagement } from "../api/user-features";
import AdminActionBar from "../components/admin/AdminActionBar.vue";
import AdminEntityDrawer from "../components/admin/AdminEntityDrawer.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { AdminActionItem } from "../types/admin-action";
import type { ExternalUserEntry, ExternalUserStatus } from "../types/external-user-admin";
import type { PageResult } from "../types/page";
import type {
  UserFeatureAccessScope,
  UserFeatureOverrideType,
  UserFeaturePackageEntry,
  UserFeatureUserApplicationEntry,
  UserFeatureUserManagementEntry,
} from "../types/user-feature";
import { bzConfirm } from "../utils/confirm";
import { formatDateTime } from "../utils/formatter";
import { message } from "../utils/message";

const loading = ref(false);
const rows = ref<ExternalUserEntry[]>([]);
const page = ref<PageResult<ExternalUserEntry>>({ pageNo: 1, pageSize: 10, numberOfElements: 0, totalPages: 0, totalElements: 0, elements: [] });
const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref<ExternalUserEntry | null>(null);
const featureOpen = ref(false);
const featureLoading = ref(false);
const featureUser = ref<ExternalUserEntry | null>(null);
const featureManagement = ref<UserFeatureUserManagementEntry | null>(null);
const packageEntries = ref<UserFeaturePackageEntry[]>([]);
const applicationStates = ref<UserFeatureUserApplicationEntry[]>([]);
const selectedPackageIds = ref<string[]>([]);

const queryPanelVisible = ref(false);
const queryCollapsed = ref(true);
const keywordDraft = ref("");
const statusDraft = ref<"" | ExternalUserStatus>("");
const appliedKeyword = ref("");
const appliedStatus = ref<"" | ExternalUserStatus>("");
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;
const featureKeyword = ref("");
const featureOverrideFilter = ref<"" | UserFeatureOverrideType>("");

const canView = computed(() => hasResourceCodeAccess("web-user-manage-view"));
const canEdit = computed(() => hasResourceCodeAccess("web-user-manage-edit"));
const canFeatureManage = computed(() => hasResourceCodeAccess("user-feature-user-view") || hasResourceCodeAccess("user-feature-user-edit"));
const canFeatureSave = computed(() => hasResourceCodeAccess("user-feature-user-edit"));
const totalPages = computed(() => Math.max(1, page.value.totalPages || 1));
const isFirstPage = computed(() => pageNo.value <= 1);
const isLastPage = computed(() => pageNo.value >= totalPages.value);
const pageTokens = computed<Array<number | "ellipsis">>(() => buildTokens(pageNo.value, totalPages.value));
const flattenedFeatures = computed(() => applicationStates.value.flatMap((application) => application.features));
const filteredFeatures = computed(() => {
  const kw = featureKeyword.value.trim().toLowerCase();
  return flattenedFeatures.value.filter((item) => {
    if (featureOverrideFilter.value && item.overrideType !== featureOverrideFilter.value) return false;
    if (!kw) return true;
    return (
      item.code.toLowerCase().includes(kw) ||
      item.name.toLowerCase().includes(kw) ||
      applicationName(item.applicationId).toLowerCase().includes(kw)
    );
  });
});

onMounted(() => {
  void reload();
});

function buildTokens(currentPageNo: number, totalPageCount: number): Array<number | "ellipsis"> {
  const total = totalPageCount;
  const current = Math.min(Math.max(currentPageNo, 1), total);
  if (total <= 7) return Array.from({ length: total }, (_, index) => index + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", total];
  if (current >= total - 3) return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
}

function resolveStatusLabel(status: ExternalUserStatus): string {
  if (status === "ACTIVE") return "启用";
  if (status === "DISABLED") return "停用";
  return "已注销";
}

function resolveStatusType(status: ExternalUserStatus): string {
  if (status === "ACTIVE") return "success";
  if (status === "DISABLED") return "warning";
  return "danger";
}

async function reload() {
  if (!canView.value) {
    rows.value = [];
    page.value = { pageNo: 1, pageSize: pageSize.value, numberOfElements: 0, totalPages: 0, totalElements: 0, elements: [] };
    return;
  }
  loading.value = true;
  try {
    const result = await pageExternalUsers({ keyword: appliedKeyword.value || undefined, status: appliedStatus.value, pageNo: pageNo.value, pageSize: pageSize.value });
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
  appliedStatus.value = statusDraft.value;
  pageNo.value = 1;
  await reload();
}

async function resetFilters() {
  keywordDraft.value = "";
  statusDraft.value = "";
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

function getRowActions(row: ExternalUserEntry): AdminActionItem[] {
  const actions: AdminActionItem[] = [{ key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) }];
  if (canFeatureManage.value) actions.push({ key: `feature-${row.id}`, label: "功能", tone: "detail", handler: () => openFeatureManagement(row) });
  if (canEdit.value && row.status !== "CANCELLED") actions.push({ key: `toggle-${row.id}`, label: row.status === "ACTIVE" ? "停用" : "启用", tone: row.status === "ACTIVE" ? "disable" : "enable", handler: () => toggleStatus(row) });
  return actions;
}

async function openDetail(id: string) {
  detailOpen.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getExternalUser(id);
  } finally {
    detailLoading.value = false;
  }
}

async function toggleStatus(row: ExternalUserEntry) {
  if (!canEdit.value || row.status === "CANCELLED") return;
  const nextStatus: ExternalUserStatus = row.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
  const confirmed = await bzConfirm({ title: nextStatus === "ACTIVE" ? "启用用户" : "停用用户", message: `确认${nextStatus === "ACTIVE" ? "启用" : "停用"}：${row.account}？`, confirmText: nextStatus === "ACTIVE" ? "启用" : "停用", cancelText: "取消", danger: nextStatus !== "ACTIVE" });
  if (!confirmed) return;
  await updateExternalUser(row.id, { status: nextStatus });
  message.success(nextStatus === "ACTIVE" ? "已启用" : "已停用");
  await reload();
}

async function openFeatureManagement(user: ExternalUserEntry) {
  featureOpen.value = true;
  featureLoading.value = true;
  featureUser.value = user;
  try {
    const [management, packagePage] = await Promise.all([
      getUserFeatureUserManagement(user.id),
      pageUserFeaturePackages({ enabled: true, page: { pageNo: 1, pageSize: 200 } }),
    ]);
    featureManagement.value = management;
    packageEntries.value = packagePage.elements;
    selectedPackageIds.value = [...management.packageIds];
    applicationStates.value = management.applications.map((application) => ({
      ...application,
      features: application.features.map((feature) => ({ ...feature })),
    }));
    recomputeEffectiveState();
  } finally {
    featureLoading.value = false;
  }
}

function packageAccessForApplication(applicationId: string) {
  const accesses = packageEntries.value
    .filter((pkg) => selectedPackageIds.value.includes(pkg.id))
    .flatMap((pkg) => pkg.applicationAccesses.filter((access) => access.applicationId === applicationId));
  const fullAccess = accesses.some((access) => access.featureAccessScope === "FULL");
  const featureIds = new Set(accesses.flatMap((access) => access.featureAccessScope === "PARTIAL" ? access.featureIds : []));
  return {
    inheritedVisible: accesses.length > 0,
    packageAccessScope: (fullAccess ? "FULL" : accesses.length > 0 ? "PARTIAL" : "NONE") as
      | "NONE"
      | UserFeatureAccessScope,
    fullAccess,
    featureIds,
  };
}

function recomputeEffectiveState() {
  applicationStates.value = applicationStates.value.map((application) => {
    const pkgAccess = packageAccessForApplication(application.id);
    const effectiveVisible = application.overrideType === "DISABLE" ? false : application.overrideType === "ENABLE" ? true : pkgAccess.inheritedVisible;
    const features = application.features.map((feature) => {
      const inheritedEnabled = pkgAccess.fullAccess || pkgAccess.featureIds.has(feature.id);
      let effectiveEnabled = inheritedEnabled;
      if (application.overrideType === "DISABLE") {
        effectiveEnabled = false;
      } else if (feature.overrideType === "DISABLE") {
        effectiveEnabled = false;
      } else if (application.overrideType === "ENABLE" && application.overrideAccessScope === "FULL") {
        effectiveEnabled = true;
      } else if (application.overrideType === "ENABLE" && application.overrideAccessScope === "PARTIAL") {
        effectiveEnabled = feature.overrideType === "ENABLE";
      } else if (!inheritedEnabled && feature.overrideType === "ENABLE" && pkgAccess.inheritedVisible) {
        effectiveEnabled = true;
      }
      return { ...feature, inheritedEnabled, effectiveEnabled };
    });
    return { ...application, inheritedVisible: pkgAccess.inheritedVisible, packageAccessScope: pkgAccess.packageAccessScope, effectiveVisible, features };
  });
}

function togglePackageSelection(packageId: string, checked: boolean) {
  const next = new Set(selectedPackageIds.value);
  if (checked) next.add(packageId); else next.delete(packageId);
  selectedPackageIds.value = Array.from(next);
  recomputeEffectiveState();
}

function updateApplicationOverride(applicationId: string, overrideType: UserFeatureOverrideType) {
  applicationStates.value = applicationStates.value.map((application) => application.id === applicationId ? { ...application, overrideType, overrideAccessScope: overrideType === 'ENABLE' ? (application.overrideAccessScope || 'FULL') : null } : application);
  recomputeEffectiveState();
}

function updateApplicationOverrideScope(applicationId: string, scope: UserFeatureAccessScope) {
  applicationStates.value = applicationStates.value.map((application) => application.id === applicationId ? { ...application, overrideAccessScope: scope } : application);
  recomputeEffectiveState();
}

function updateFeatureOverride(applicationId: string, featureId: string, overrideType: UserFeatureOverrideType) {
  applicationStates.value = applicationStates.value.map((application) => application.id !== applicationId ? application : {
    ...application,
    features: application.features.map((feature) => feature.id === featureId ? { ...feature, overrideType } : feature),
  });
  recomputeEffectiveState();
}

function applicationName(applicationId: string): string {
  return applicationStates.value.find((application) => application.id === applicationId)?.name || applicationId;
}

async function saveFeatureManagement() {
  if (!featureUser.value || !canFeatureSave.value) return;
  await saveUserFeatureUserManagement(featureUser.value.id, {
    packageIds: selectedPackageIds.value,
    applicationOverrides: applicationStates.value
      .filter((application) => application.overrideType !== "NONE")
      .map((application) => ({ applicationId: application.id, overrideType: application.overrideType, featureAccessScope: application.overrideType === 'ENABLE' ? (application.overrideAccessScope || 'FULL') : undefined })),
    featureOverrides: applicationStates.value.flatMap((application) => application.features
      .filter((feature) => feature.overrideType !== "NONE")
      .map((feature) => ({ applicationId: application.id, featureId: feature.id, overrideType: feature.overrideType }))),
  });
  message.success("用户功能配置已保存");
  featureOpen.value = false;
}
</script>

<style scoped>
.content { flex: 1; min-height: 0; width: 100%; overflow-y: auto; box-sizing: border-box; }
.dict-pagination-bar { display: flex; align-items: center; justify-content: space-between; gap: 14px; flex-wrap: wrap; padding: 14px 20px 18px; color: #475569; }
.dict-pagination-summary { font-size: 13px; color: #64748b; }
.dict-pagination-right { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.dict-page-size__select { min-width: 92px; height: 32px; padding: 0 12px; color: #0f172a; background: #ffffff; border: 1px solid #dbe1ea; border-radius: 8px; font-size: 13px; }
.dict-page-list { display: inline-flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.dict-page-btn { display: inline-flex; align-items: center; justify-content: center; min-width: 32px; height: 32px; padding: 0 10px; color: #334155; background: #ffffff; border: 1px solid #dbe1ea; border-radius: 8px; cursor: pointer; font-size: 13px; }
.dict-page-btn:hover:not(:disabled) { color: #2563eb; background: #eff6ff; border-color: #bfdbfe; }
.dict-page-btn.is-active { color: #ffffff; background: #1677ff; border-color: #1677ff; }
.dict-page-btn:disabled { opacity: 0.52; cursor: not-allowed; }
.dict-page-ellipsis { display: inline-flex; align-items: center; justify-content: center; min-width: 20px; color: #94a3b8; }
.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 16px; }
.detail-field { display: grid; grid-template-columns: 88px minmax(0, 1fr); gap: 10px; align-items: start; min-width: 0; }
.detail-field__label { font-size: 12px; font-weight: 700; color: #64748b; line-height: 1.75; }
.detail-field__value { color: #0f172a; font-size: 14px; line-height: 1.6; word-break: break-all; }
.feature-manage-layout { display: grid; gap: 18px; }
.feature-manage-cards { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.feature-manage-card { padding: 14px 16px; border: 1px solid #e5e7eb; border-radius: 14px; background: #fff; display: grid; gap: 8px; }
.feature-manage-card span { color: #64748b; font-size: 12px; }
.feature-manage-card strong { color: #0f172a; font-size: 24px; font-weight: 800; }
.feature-section { display: grid; gap: 12px; }
.feature-section__head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.feature-section__title { font-size: 14px; font-weight: 800; color: #0f172a; }
.feature-section__meta { font-size: 12px; color: #64748b; }
.package-option-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.package-option-card { display: flex; align-items: flex-start; gap: 12px; padding: 14px 16px; border: 1px solid #e2e8f0; border-radius: 16px; background: #fff; cursor: pointer; }
.package-option-card__body { display: grid; gap: 6px; min-width: 0; }
.package-option-card__top { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.package-option-card__code, .package-option-card__desc { color: #64748b; font-size: 12px; }
.table-title-cell { display: grid; gap: 4px; }
.table-title-cell strong { color: #0f172a; }
.table-title-cell small { color: #64748b; }
.inline-select { width: 100%; height: 32px; padding: 0 10px; color: #0f172a; background: #fff; border: 1px solid #dbe1ea; border-radius: 8px; font-size: 13px; }
.inline-select--filter { width: 140px; }
.feature-filter-bar { display: flex; gap: 12px; align-items: center; }
</style>
