<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card v-if="queryPanelVisible" class="admin-panel admin-filter-card" shadow="never">
          <bz-form class="admin-filter-form" :class="{ 'is-collapsed': queryCollapsed }" @submit.prevent="applyFilters">
            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">关键词</div>
                <div class="admin-filter-control"><bz-input v-model="keywordDraft" placeholder="按编码或名称搜索" clearable @keyup.enter="applyFilters" /></div>
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
              <div class="admin-table-title">应用包管理</div>
              <div class="admin-table-tools">
                <bz-button v-if="canEdit" class="admin-toolbar-primary" type="primary" @click="openCreate">新增</bz-button>
                <button class="admin-vben-circle-button" :class="{ 'is-active': queryPanelVisible }" type="button" :title="queryPanelVisible ? '关闭搜索框' : '打开搜索框'" @click="queryPanelVisible = !queryPanelVisible">
                  <i class="admin-vben-circle-button__icon admin-vben-circle-button__icon--search" aria-hidden="true"></i>
                </button>
                <button class="admin-vben-circle-button" type="button" title="刷新列表" @click="reload">
                  <i class="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh" aria-hidden="true"></i>
                </button>
              </div>
            </div>
          </template>

          <div class="admin-table-surface">
            <bz-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无应用包" size="small">
              <bz-table-column prop="code" label="应用包编码" min-width="180" show-overflow-tooltip />
              <bz-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
              <bz-table-column label="类型" width="120">
                <template #default="scope"><bz-tag :type="resolveTagType(packageTypeMetaMap, scope.row.packageType)">{{ resolveLabel(packageTypeMetaMap, scope.row.packageType) }}</bz-tag></template>
              </bz-table-column>
              <bz-table-column label="默认包" width="100">
                <template #default="scope"><bz-tag :type="scope.row.defaultPackage ? 'success' : 'info'">{{ scope.row.defaultPackage ? "是" : "否" }}</bz-tag></template>
              </bz-table-column>
              <bz-table-column label="状态" width="100">
                <template #default="scope"><bz-tag :type="scope.row.enabled ? 'success' : 'warning'">{{ scope.row.enabled ? "启用" : "停用" }}</bz-tag></template>
              </bz-table-column>
              <bz-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip>
                <template #default="scope">{{ scope.row.description || "-" }}</template>
              </bz-table-column>
              <bz-table-column label="应用数" width="90">
                <template #default="scope">{{ scope.row.applicationAccesses.length }}</template>
              </bz-table-column>
              <bz-table-column label="操作" width="160" fixed="right">
                <template #default="scope"><AdminActionBar :actions="getRowActions(scope.row)" :more-actions="getRowMoreActions(scope.row)" /></template>
              </bz-table-column>
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

        <AdminEntityDrawer :open="drawerOpen" :loading="drawerLoading" :title="drawerMode === 'create' ? '新增应用包' : drawerMode === 'edit' ? '编辑应用包' : '应用包详情'" width="1080px" @close="drawerOpen = false">
          <div v-if="drawerMode === 'detail' && currentPackage" class="detail-grid">
            <div class="detail-field"><span class="detail-field__label">编码</span><span class="detail-field__value">{{ currentPackage.code }}</span></div>
            <div class="detail-field"><span class="detail-field__label">名称</span><span class="detail-field__value">{{ currentPackage.name }}</span></div>
            <div class="detail-field"><span class="detail-field__label">类型</span><span class="detail-field__value">{{ resolveLabel(packageTypeMetaMap, currentPackage.packageType) }}</span></div>
            <div class="detail-field"><span class="detail-field__label">默认包</span><span class="detail-field__value">{{ currentPackage.defaultPackage ? '是' : '否' }}</span></div>
            <div class="detail-field"><span class="detail-field__label">状态</span><span class="detail-field__value">{{ currentPackage.enabled ? '启用' : '停用' }}</span></div>
            <div class="detail-field detail-field--wide"><span class="detail-field__label">描述</span><span class="detail-field__value">{{ currentPackage.description || '-' }}</span></div>
          </div>

          <div v-if="drawerMode === 'detail' && currentPackage" class="package-access-list">
            <div v-for="access in currentPackage.applicationAccesses" :key="access.applicationId" class="package-access-card">
              <div class="package-access-card__head">
                <strong>{{ access.applicationName }}</strong>
                <bz-tag :type="access.featureAccessScope === 'FULL' ? 'success' : 'warning'">{{ access.featureAccessScope === "FULL" ? "完整功能" : "部分功能" }}</bz-tag>
              </div>
              <div class="package-access-card__meta">{{ access.applicationCode }}</div>
              <div v-if="access.features.length" class="package-access-card__feature-list">
                <span v-for="feature in access.features" :key="feature.id" class="package-feature-chip">{{ feature.name }}</span>
              </div>
            </div>
          </div>

          <template v-if="drawerMode !== 'detail'">
            <bz-form label-position="top">
              <div class="group-form-grid">
                <bz-form-item label="编码"><bz-input v-model="form.code" :disabled="drawerMode === 'edit'" /></bz-form-item>
                <bz-form-item label="名称"><bz-input v-model="form.name" /></bz-form-item>
                <bz-form-item label="类型"><bz-select v-model="form.packageType"><bz-option v-for="item in packageTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></bz-select></bz-form-item>
                <bz-form-item label="状态"><bz-switch v-model="form.enabled" /></bz-form-item>
                <bz-form-item label="默认包"><bz-switch v-model="form.defaultPackage" /></bz-form-item>
                <bz-form-item label="描述" class="group-form-grid__wide"><bz-text-field v-model="form.description" type="textarea" :rows="3" /></bz-form-item>
              </div>
            </bz-form>

            <div class="package-config-panel">
              <div class="package-config-panel__head">
                <div class="package-config-panel__title">应用授权</div>
                <div class="package-config-panel__meta">已选 {{ selectedApplicationCount }} 个应用</div>
              </div>
              <div class="package-config-panel__body">
                <div v-for="application in applications" :key="application.id" class="package-config-card">
                  <div class="package-config-card__top">
                    <label class="package-config-card__select">
                      <input type="checkbox" :checked="isApplicationSelected(application.id)" @change="toggleApplicationSelection(application.id, ($event.target as HTMLInputElement).checked)" />
                      <div>
                        <div class="package-config-card__name">{{ application.name }}</div>
                        <div class="package-config-card__code">{{ application.code }}</div>
                      </div>
                    </label>
                    <bz-tag :type="application.enabled ? 'success' : 'warning'">{{ application.enabled ? '启用' : '停用' }}</bz-tag>
                  </div>
                  <div class="package-config-card__path">{{ application.routePath || '-' }}</div>
                  <div v-if="isApplicationSelected(application.id)" class="package-config-card__scope">
                    <label><input type="radio" :name="`scope-${application.id}`" value="FULL" :checked="applicationScopeOf(application.id) === 'FULL'" @change="updateApplicationScope(application.id, 'FULL')" /> 完整功能</label>
                    <label><input type="radio" :name="`scope-${application.id}`" value="PARTIAL" :checked="applicationScopeOf(application.id) === 'PARTIAL'" @change="updateApplicationScope(application.id, 'PARTIAL')" /> 部分功能</label>
                  </div>
                  <div v-if="isApplicationSelected(application.id) && applicationScopeOf(application.id) === 'PARTIAL'" class="package-config-card__features">
                    <label v-for="feature in application.features" :key="feature.id" class="package-feature-option" :class="{ disabled: !feature.enabled }">
                      <input type="checkbox" :checked="isFeatureSelected(application.id, feature.id)" :disabled="!feature.enabled" @change="toggleFeatureSelection(application.id, feature.id, ($event.target as HTMLInputElement).checked)" />
                      <span>{{ feature.name }}</span>
                      <small>{{ feature.code }}</small>
                    </label>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template #footer>
            <bz-button @click="drawerOpen = false">{{ drawerMode === 'detail' ? '关闭' : '取消' }}</bz-button>
            <bz-button v-if="drawerMode !== 'detail'" type="primary" @click="submitPackage">确定</bz-button>
          </template>
        </AdminEntityDrawer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { batchListDictOptions } from "../api/dicts";
import {
  createUserFeaturePackage,
  deleteUserFeaturePackage,
  getUserFeaturePackage,
  listUserFeatureApplications,
  pageUserFeaturePackages,
  updateUserFeaturePackage,
  updateUserFeaturePackageStatus,
} from "../api/user-features";
import AdminActionBar from "../components/admin/AdminActionBar.vue";
import AdminEntityDrawer from "../components/admin/AdminEntityDrawer.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { AdminActionItem } from "../types/admin-action";
import type { DictItem } from "../types/dict-admin";
import type { PageResult } from "../types/page";
import type {
  SaveUserFeaturePackageRequest,
  UserFeatureAccessScope,
  UserFeatureApplicationEntry,
  UserFeaturePackageEntry,
} from "../types/user-feature";
import { bzConfirm } from "../utils/confirm";
import { message } from "../utils/message";

type TagType = "info" | "success" | "warning" | "danger";
type DictMeta = { label: string; tagType?: TagType };
type DrawerMode = "detail" | "edit" | "create";

const loading = ref(false);
const rows = ref<UserFeaturePackageEntry[]>([]);
const applications = ref<UserFeatureApplicationEntry[]>([]);
const page = ref<PageResult<UserFeaturePackageEntry>>({ pageNo: 1, pageSize: 10, numberOfElements: 0, totalPages: 0, totalElements: 0, elements: [] });
const queryPanelVisible = ref(false);
const queryCollapsed = ref(true);
const keywordDraft = ref("");
const enabledDraft = ref<"" | "true" | "false">("");
const appliedKeyword = ref("");
const appliedEnabled = ref<"" | "true" | "false">("");
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;

const drawerOpen = ref(false);
const drawerLoading = ref(false);
const drawerMode = ref<DrawerMode>("detail");
const currentPackage = ref<UserFeaturePackageEntry | null>(null);
const selectedApplications = ref<Record<string, { featureAccessScope: UserFeatureAccessScope; featureIds: string[] }>>({});
const form = reactive<SaveUserFeaturePackageRequest>({
  code: "",
  name: "",
  packageType: "CUSTOM",
  description: "",
  enabled: true,
  defaultPackage: false,
  applicationAccesses: [],
});

const packageTypeMetaMap = ref<Record<string, DictMeta>>({});
const packageTypeOptions = computed(() => Object.entries(packageTypeMetaMap.value).map(([value, meta]) => ({ value, label: meta.label })));
const canView = computed(() => hasResourceCodeAccess("user-feature-package-view"));
const canEdit = computed(() => hasResourceCodeAccess("user-feature-package-edit"));
const totalPages = computed(() => Math.max(1, page.value.totalPages || 1));
const isFirstPage = computed(() => pageNo.value <= 1);
const isLastPage = computed(() => pageNo.value >= totalPages.value);
const selectedApplicationCount = computed(() => Object.keys(selectedApplications.value).length);
const pageTokens = computed<Array<number | "ellipsis">>(() => buildTokens(pageNo.value, totalPages.value));

onMounted(async () => {
  await Promise.all([loadDictionaries(), loadApplications(), reload()]);
});

async function loadDictionaries() {
  try {
    const result = await batchListDictOptions(["USER_APPLICATION_PACKAGE_TYPE"]);
    packageTypeMetaMap.value = toMetaMap(result.USER_APPLICATION_PACKAGE_TYPE);
  } catch {
    packageTypeMetaMap.value = {
      DEFAULT: { label: "默认包", tagType: "info" },
      MEMBERSHIP: { label: "会员包", tagType: "success" },
      OPERATION: { label: "运营包", tagType: "warning" },
      ENTERPRISE: { label: "企业包", tagType: "danger" },
      CUSTOM: { label: "自定义", tagType: "info" },
    };
  }
}

async function loadApplications() {
  applications.value = await listUserFeatureApplications();
}

function toMetaMap(items?: DictItem[]): Record<string, DictMeta> {
  const map: Record<string, DictMeta> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = { label: item.itemLabel || item.itemValue, tagType: (item.tagType as TagType | null) || undefined };
  }
  return map;
}

function resolveLabel(metaMap: Record<string, DictMeta>, value?: string | null): string {
  if (!value) return "-";
  return metaMap[value]?.label || value;
}

function resolveTagType(metaMap: Record<string, DictMeta>, value?: string | null): string {
  if (!value) return "info";
  return metaMap[value]?.tagType || "info";
}

function buildTokens(currentPageNo: number, totalPageCount: number): Array<number | "ellipsis"> {
  const total = totalPageCount;
  const current = Math.min(Math.max(currentPageNo, 1), total);
  if (total <= 7) return Array.from({ length: total }, (_, index) => index + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", total];
  if (current >= total - 3) return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
}

async function reload() {
  if (!canView.value) {
    rows.value = [];
    page.value = { pageNo: 1, pageSize: pageSize.value, numberOfElements: 0, totalPages: 0, totalElements: 0, elements: [] };
    return;
  }
  loading.value = true;
  try {
    const result = await pageUserFeaturePackages({
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

function getRowActions(row: UserFeaturePackageEntry): AdminActionItem[] {
  const actions: AdminActionItem[] = [
    { key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) },
  ];
  if (canEdit.value) {
    actions.push({ key: `edit-${row.id}`, label: "编辑", tone: "edit", handler: () => openEdit(row.id) });
  }
  return actions;
}

function getRowMoreActions(row: UserFeaturePackageEntry): AdminActionItem[] {
  if (!canEdit.value) return [];
  return [
    { key: `toggle-${row.id}`, label: row.enabled ? "停用" : "启用", tone: row.enabled ? "disable" : "enable", handler: () => toggleStatus(row) },
    { key: `delete-${row.id}`, label: "删除", tone: "delete", handler: () => removePackage(row) },
  ];
}

function resetForm() {
  form.code = "";
  form.name = "";
  form.packageType = "CUSTOM";
  form.description = "";
  form.enabled = true;
  form.defaultPackage = false;
  form.applicationAccesses = [];
  selectedApplications.value = {};
}

function hydrateSelections(applicationAccesses: UserFeaturePackageEntry["applicationAccesses"]) {
  const next: Record<string, { featureAccessScope: UserFeatureAccessScope; featureIds: string[] }> = {};
  for (const access of applicationAccesses) {
    next[access.applicationId] = {
      featureAccessScope: access.featureAccessScope,
      featureIds: [...access.featureIds],
    };
  }
  selectedApplications.value = next;
}

async function openCreate() {
  resetForm();
  drawerMode.value = "create";
  currentPackage.value = null;
  drawerOpen.value = true;
}

async function openEdit(id: string) {
  drawerMode.value = "edit";
  drawerOpen.value = true;
  drawerLoading.value = true;
  try {
    const pkg = await getUserFeaturePackage(id);
    currentPackage.value = pkg;
    form.code = pkg.code;
    form.name = pkg.name;
    form.packageType = pkg.packageType;
    form.description = pkg.description || "";
    form.enabled = pkg.enabled;
    form.defaultPackage = pkg.defaultPackage;
    hydrateSelections(pkg.applicationAccesses);
  } finally {
    drawerLoading.value = false;
  }
}

async function openDetail(id: string) {
  drawerMode.value = "detail";
  drawerOpen.value = true;
  drawerLoading.value = true;
  try {
    currentPackage.value = await getUserFeaturePackage(id);
  } finally {
    drawerLoading.value = false;
  }
}

function isApplicationSelected(applicationId: string): boolean {
  return Boolean(selectedApplications.value[applicationId]);
}

function applicationScopeOf(applicationId: string): UserFeatureAccessScope {
  return selectedApplications.value[applicationId]?.featureAccessScope || "FULL";
}

function isFeatureSelected(applicationId: string, featureId: string): boolean {
  return selectedApplications.value[applicationId]?.featureIds.includes(featureId) || false;
}

function toggleApplicationSelection(applicationId: string, checked: boolean) {
  const next = { ...selectedApplications.value };
  if (checked) {
    next[applicationId] = next[applicationId] || { featureAccessScope: "FULL", featureIds: [] };
  } else {
    delete next[applicationId];
  }
  selectedApplications.value = next;
}

function updateApplicationScope(applicationId: string, scope: UserFeatureAccessScope) {
  const current = selectedApplications.value[applicationId] || { featureAccessScope: "FULL" as UserFeatureAccessScope, featureIds: [] };
  selectedApplications.value = {
    ...selectedApplications.value,
    [applicationId]: {
      featureAccessScope: scope,
      featureIds: scope === "FULL" ? [] : current.featureIds,
    },
  };
}

function toggleFeatureSelection(applicationId: string, featureId: string, checked: boolean) {
  const current = selectedApplications.value[applicationId];
  if (!current) return;
  const nextFeatureIds = new Set(current.featureIds);
  if (checked) nextFeatureIds.add(featureId);
  else nextFeatureIds.delete(featureId);
  selectedApplications.value = {
    ...selectedApplications.value,
    [applicationId]: {
      ...current,
      featureIds: Array.from(nextFeatureIds),
    },
  };
}

function buildPayload(): SaveUserFeaturePackageRequest {
  return {
    code: form.code.trim(),
    name: form.name.trim(),
    packageType: form.packageType,
    description: form.description?.trim() || null,
    enabled: form.enabled,
    defaultPackage: form.defaultPackage,
    applicationAccesses: Object.entries(selectedApplications.value).map(([applicationId, config]) => ({
      applicationId,
      featureAccessScope: config.featureAccessScope,
      featureIds: config.featureAccessScope === "FULL" ? [] : config.featureIds,
    })),
  };
}

async function submitPackage() {
  const payload = buildPayload();
  if (!payload.code || !payload.name) {
    message.warning("编码和名称不能为空");
    return;
  }
  if (drawerMode.value === "create") {
    await createUserFeaturePackage(payload);
    message.success("应用包已创建");
  } else if (currentPackage.value) {
    await updateUserFeaturePackage(currentPackage.value.id, payload);
    message.success("应用包已更新");
  }
  drawerOpen.value = false;
  await reload();
}

async function toggleStatus(row: UserFeaturePackageEntry) {
  const nextEnabled = !row.enabled;
  const confirmed = await bzConfirm({
    title: nextEnabled ? "启用应用包" : "停用应用包",
    message: `确认${nextEnabled ? "启用" : "停用"}：${row.name}？`,
    confirmText: nextEnabled ? "启用" : "停用",
    cancelText: "取消",
    danger: !nextEnabled,
  });
  if (!confirmed) return;
  await updateUserFeaturePackageStatus(row.id, nextEnabled);
  message.success(nextEnabled ? "已启用" : "已停用");
  await reload();
}

async function removePackage(row: UserFeaturePackageEntry) {
  const confirmed = await bzConfirm({
    title: "删除应用包",
    message: `确认删除：${row.name}？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) return;
  await deleteUserFeaturePackage(row.id);
  message.success("应用包已删除");
  await reload();
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
.detail-field--wide { grid-column: 1 / -1; }
.detail-field__label { font-size: 12px; font-weight: 700; color: #64748b; line-height: 1.75; }
.detail-field__value { color: #0f172a; font-size: 14px; line-height: 1.6; word-break: break-all; }
.group-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 16px; }
.group-form-grid__wide { grid-column: 1 / -1; }
.package-config-panel, .package-access-list { margin-top: 18px; }
.package-config-panel__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.package-config-panel__title { font-size: 14px; font-weight: 800; color: #0f172a; }
.package-config-panel__meta { font-size: 12px; color: #64748b; }
.package-config-panel__body, .package-access-list { display: grid; gap: 12px; }
.package-config-card, .package-access-card { border: 1px solid #e2e8f0; border-radius: 16px; background: #fff; padding: 14px 16px; display: grid; gap: 10px; }
.package-config-card__top, .package-access-card__head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.package-config-card__select { display: inline-flex; align-items: center; gap: 12px; cursor: pointer; }
.package-config-card__name { font-size: 14px; font-weight: 800; color: #0f172a; }
.package-config-card__code, .package-access-card__meta, .package-config-card__path { color: #64748b; font-size: 12px; }
.package-config-card__scope { display: inline-flex; gap: 16px; flex-wrap: wrap; color: #334155; font-size: 13px; }
.package-config-card__features { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.package-feature-option { display: flex; align-items: center; gap: 8px; min-width: 0; padding: 10px 12px; border-radius: 12px; background: #f8fafc; color: #0f172a; }
.package-feature-option small { color: #64748b; }
.package-feature-option.disabled { opacity: 0.55; }
.package-access-card__feature-list { display: flex; flex-wrap: wrap; gap: 8px; }
.package-feature-chip { display: inline-flex; align-items: center; padding: 4px 10px; border-radius: 999px; background: #eff6ff; color: #1d4ed8; font-size: 12px; font-weight: 700; }
</style>
