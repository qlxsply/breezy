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
            <bz-form-item class="admin-filter-item"
              ><div class="admin-filter-field">
                <div class="admin-filter-label">关键词</div>
                <div class="admin-filter-control">
                  <bz-input
                    v-model="keywordDraft"
                    placeholder="按用户名或昵称搜索"
                    clearable
                    @keyup.enter="applyFilters"
                  />
                </div></div
            ></bz-form-item>
            <bz-form-item class="admin-filter-item"
              ><div class="admin-filter-field">
                <div class="admin-filter-label">状态</div>
                <div class="admin-filter-control">
                  <bz-select
                    v-model="statusDraft"
                    placeholder="全部状态"
                    clearable
                    ><bz-option
                      label="启用"
                      value="ACTIVE" /><bz-option
                      label="停用"
                      value="DISABLED" /><bz-option
                      label="已注销"
                      value="CANCELLED"
                  /></bz-select>
                </div></div
            ></bz-form-item>
            <div class="admin-filter-actions">
              <bz-button
                class="admin-filter-secondary"
                @click="resetFilters"
                >重置</bz-button
              ><bz-button
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
          <template #header
            ><div class="admin-table-header">
              <div class="admin-table-title">用户</div>
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
                  ></i></button
                ><button
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
              </div></div
          ></template>

          <div class="admin-table-surface">
            <bz-table
              v-loading="loading"
              :data="rows"
              row-key="id"
              empty-text="暂无用户"
              size="small"
            >
              <bz-table-column
                prop="account"
                label="用户名"
                min-width="180"
                show-overflow-tooltip
              />
              <bz-table-column
                prop="nickname"
                label="昵称"
                min-width="160"
                show-overflow-tooltip
                ><template #default="scope">{{
                  scope.row.nickname || "-"
                }}</template></bz-table-column
              >
              <bz-table-column
                label="类型"
                width="110"
                ><template #default="scope"
                  ><bz-tag :type="resolveTagType(userTypeMetaMap, scope.row.userType)">{{
                    resolveLabel(userTypeMetaMap, scope.row.userType)
                  }}</bz-tag></template
                ></bz-table-column
              >
              <bz-table-column
                label="状态"
                width="110"
                ><template #default="scope"
                  ><bz-tag :type="resolveTagType(webUserStatusMetaMap, scope.row.status)">{{
                    resolveLabel(webUserStatusMetaMap, scope.row.status)
                  }}</bz-tag></template
                ></bz-table-column
              >
              <bz-table-column
                label="最近登录"
                width="180"
                ><template #default="scope">{{
                  formatDateTime(scope.row.lastLoginAt)
                }}</template></bz-table-column
              >
              <bz-table-column
                label="创建时间"
                width="180"
                ><template #default="scope">{{
                  formatDateTime(scope.row.createdAt)
                }}</template></bz-table-column
              >
              <bz-table-column
                label="更新时间"
                width="180"
                ><template #default="scope">{{
                  formatDateTime(scope.row.updatedAt)
                }}</template></bz-table-column
              >
              <bz-table-column
                label="操作"
                width="160"
                fixed="right"
                ><template #default="scope"
                  ><AdminActionBar :actions="getRowActions(scope.row)" /></template
              ></bz-table-column>
            </bz-table>
          </div>

          <div
            v-if="page.totalElements > 0"
            class="dict-pagination-bar"
          >
            <div class="dict-pagination-summary">共 {{ page.totalElements }} 条记录</div>
            <div class="dict-pagination-right">
              <label class="dict-page-size"
                ><select
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
                </select></label
              >
              <div class="dict-page-list">
                <button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isFirstPage"
                  @click="goToPage(1)"
                >
                  <span aria-hidden="true">|&lt;</span></button
                ><button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isFirstPage"
                  @click="goToPage(pageNo - 1)"
                >
                  <span aria-hidden="true">&lt;</span></button
                ><template
                  v-for="(token, tokenIndex) in pageTokens"
                  :key="`${String(token)}-${tokenIndex}`"
                  ><button
                    v-if="typeof token === 'number'"
                    class="dict-page-btn"
                    :class="{ 'is-active': token === pageNo }"
                    type="button"
                    @click="goToPage(token)"
                  >
                    {{ token }}</button
                  ><span
                    v-else
                    class="dict-page-ellipsis"
                    >...</span
                  ></template
                ><button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isLastPage"
                  @click="goToPage(pageNo + 1)"
                >
                  <span aria-hidden="true">&gt;</span></button
                ><button
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

        <AdminEntityDrawer
          :open="detailOpen"
          :loading="detailLoading"
          title="用户详情"
          width="860px"
          @close="detailOpen = false"
        >
          <div
            v-if="detail"
            class="detail-grid"
          >
            <div class="detail-field">
              <span class="detail-field__label">用户名</span
              ><span class="detail-field__value">{{ detail.account || "-" }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">昵称</span
              ><span class="detail-field__value">{{ detail.nickname || "-" }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">类型</span
              ><span class="detail-field__value">{{
                resolveLabel(userTypeMetaMap, detail.userType)
              }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">状态</span
              ><span class="detail-field__value">{{
                resolveLabel(webUserStatusMetaMap, detail.status)
              }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">最近登录</span
              ><span class="detail-field__value">{{ formatDateTime(detail.lastLoginAt) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">创建时间</span
              ><span class="detail-field__value">{{ formatDateTime(detail.createdAt) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">更新时间</span
              ><span class="detail-field__value">{{ formatDateTime(detail.updatedAt) }}</span>
            </div>
          </div>
          <template #footer><bz-button @click="detailOpen = false">关闭</bz-button></template>
        </AdminEntityDrawer>

        <AdminEntityDrawer
          :open="featureOpen"
          :loading="featureLoading"
          title="用户功能管理"
          width="1100px"
          @close="featureOpen = false"
        >
          <template #extra
            ><span class="drawer-subtitle"
              >ID：{{ featureUser?.id || "-" }} / 用户名：{{
                featureManagement?.account || "-"
              }}</span
            ></template
          >
          <div
            v-if="featureManagement && featureUser"
            class="feature-manage-layout"
          >
            <section class="feature-manage-section">
              <div class="feature-manage-section__head">
                <div class="feature-manage-section__title">功能分组</div>
              </div>
              <div class="feature-group-list">
                <label
                  v-for="group in featureGroups"
                  :key="group.id"
                  class="feature-group-item"
                  ><input
                    type="checkbox"
                    :checked="selectedGroupIds.includes(group.id)"
                    @change="
                      toggleGroupSelection(group.id, ($event.target as HTMLInputElement).checked)
                    "
                  /><span class="feature-group-item__name">{{ group.name }}</span
                  ><span class="feature-group-item__code">{{ group.code }}</span></label
                >
              </div>
            </section>

            <section class="feature-manage-section">
              <div class="feature-manage-section__head">
                <div class="feature-manage-section__title">功能列表</div>
              </div>
              <div class="feature-manage-toolbar">
                <bz-input
                  v-model="featureKeywordDraft"
                  placeholder="搜索功能名称 / code"
                  clearable
                  @keyup.enter="applyFeatureFilters"
                /><bz-select
                  v-model="featureStatusDraft"
                  placeholder="全部状态"
                  clearable
                  ><bz-option
                    label="全局启用"
                    value="true" /><bz-option
                    label="全局停用"
                    value="false" /></bz-select
                ><bz-select
                  v-model="overrideFilterDraft"
                  placeholder="全部覆盖"
                  clearable
                  ><bz-option
                    label="继承分组"
                    value="NONE" /><bz-option
                    label="强制开启"
                    value="ENABLE" /><bz-option
                    label="强制关闭"
                    value="DISABLE" /></bz-select
                ><bz-button @click="applyFeatureFilters">查询</bz-button
                ><bz-button @click="resetFeatureFilters">重置</bz-button>
              </div>
              <div class="admin-table-surface">
                <bz-table
                  :data="pagedUserFeatures"
                  size="small"
                  empty-text="暂无功能"
                  ><bz-table-column
                    label="序号"
                    width="70"
                    ><template #default="scope">{{
                      scope.$index + 1 + (featurePageNo - 1) * featurePageSize
                    }}</template></bz-table-column
                  ><bz-table-column
                    label="功能"
                    min-width="260"
                    ><template #default="scope"
                      ><div class="feature-name">
                        {{ scope.row.name }} <span class="feature-code">{{ scope.row.code }}</span>
                      </div>
                      <div class="feature-desc">{{ scope.row.description || "-" }}</div>
                      <div class="feature-perms">
                        {{
                          scope.row.permissionCodes.length
                            ? scope.row.permissionCodes.join(", ")
                            : "-"
                        }}
                      </div></template
                    ></bz-table-column
                  ><bz-table-column
                    label="全局状态"
                    width="110"
                    ><template #default="scope"
                      ><bz-tag :type="scope.row.enabled ? 'success' : 'warning'">{{
                        scope.row.enabled ? "全局开启" : "全局关闭"
                      }}</bz-tag></template
                    ></bz-table-column
                  ><bz-table-column
                    label="实际状态"
                    width="110"
                    ><template #default="scope"
                      ><bz-tag :type="scope.row.effectiveEnabled ? 'success' : 'danger'">{{
                        scope.row.effectiveEnabled ? "实际开启" : "实际关闭"
                      }}</bz-tag></template
                    ></bz-table-column
                  ><bz-table-column
                    label="用户级覆盖"
                    width="120"
                    ><template #default="scope"
                      ><bz-tag :type="overrideTagType(scope.row.overrideType)">{{
                        overrideLabel(scope.row.overrideType)
                      }}</bz-tag></template
                    ></bz-table-column
                  ><bz-table-column
                    label="操作"
                    width="170"
                    ><template #default="scope"
                      ><div class="feature-row-actions">
                        <bz-button
                          size="small"
                          :type="scope.row.overrideType === 'NONE' ? 'primary' : 'default'"
                          @click="setFeatureOverride(scope.row.id, 'NONE')"
                          >继承</bz-button
                        ><bz-button
                          size="small"
                          :type="scope.row.overrideType === 'ENABLE' ? 'primary' : 'default'"
                          @click="setFeatureOverride(scope.row.id, 'ENABLE')"
                          >开启</bz-button
                        ><bz-button
                          size="small"
                          :type="scope.row.overrideType === 'DISABLE' ? 'primary' : 'default'"
                          @click="setFeatureOverride(scope.row.id, 'DISABLE')"
                          >关闭</bz-button
                        >
                      </div></template
                    ></bz-table-column
                  ></bz-table
                >
              </div>
              <div
                v-if="filteredUserFeatures.length > 0"
                class="dict-pagination-bar feature-manage-pagination"
              >
                <div class="dict-pagination-summary">共 {{ filteredUserFeatures.length }} 条</div>
                <div class="dict-pagination-right">
                  <label class="dict-page-size"
                    ><select
                      class="dict-page-size__select"
                      :value="featurePageSize"
                      @change="handleFeaturePageSizeSelect"
                    >
                      <option
                        v-for="size in pageSizeOptions"
                        :key="size"
                        :value="size"
                      >
                        {{ size }}条/页
                      </option>
                    </select></label
                  >
                  <div class="dict-page-list">
                    <button
                      class="dict-page-btn dict-page-btn--icon"
                      type="button"
                      :disabled="featureIsFirstPage"
                      @click="goToFeaturePage(1)"
                    >
                      <span aria-hidden="true">|&lt;</span></button
                    ><button
                      class="dict-page-btn dict-page-btn--icon"
                      type="button"
                      :disabled="featureIsFirstPage"
                      @click="goToFeaturePage(featurePageNo - 1)"
                    >
                      <span aria-hidden="true">&lt;</span></button
                    ><template
                      v-for="(token, tokenIndex) in featurePageTokens"
                      :key="`${String(token)}-${tokenIndex}`"
                      ><button
                        v-if="typeof token === 'number'"
                        class="dict-page-btn"
                        :class="{ 'is-active': token === featurePageNo }"
                        type="button"
                        @click="goToFeaturePage(token)"
                      >
                        {{ token }}</button
                      ><span
                        v-else
                        class="dict-page-ellipsis"
                        >...</span
                      ></template
                    ><button
                      class="dict-page-btn dict-page-btn--icon"
                      type="button"
                      :disabled="featureIsLastPage"
                      @click="goToFeaturePage(featurePageNo + 1)"
                    >
                      <span aria-hidden="true">&gt;</span></button
                    ><button
                      class="dict-page-btn dict-page-btn--icon"
                      type="button"
                      :disabled="featureIsLastPage"
                      @click="goToFeaturePage(featureTotalPages)"
                    >
                      <span aria-hidden="true">&gt;|</span>
                    </button>
                  </div>
                </div>
              </div>
            </section>
          </div>
          <template #footer
            ><bz-button @click="featureOpen = false">关闭</bz-button
            ><bz-button
              type="primary"
              @click="saveFeatureManagement"
              >保存配置</bz-button
            ></template
          >
        </AdminEntityDrawer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { batchListDictOptions } from "../api/dicts";
import { getExternalUser, pageExternalUsers, updateExternalUser } from "../api/external-users";
import {
  getNormalUserManagement,
  pageNormalFeatureGroups,
  saveNormalUserManagement,
} from "../api/normal-features";
import AdminActionBar from "../components/admin/AdminActionBar.vue";
import AdminEntityDrawer from "../components/admin/AdminEntityDrawer.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { AdminActionItem } from "../types/admin-action";
import type { DictItem } from "../types/dict-admin";
import type { ExternalUserEntry, ExternalUserStatus } from "../types/external-user-admin";
import type {
  NormalFeatureOverrideType,
  NormalFeatureUserFeatureEntry,
  NormalFeatureUserManagementEntry,
} from "../types/normal-feature";
import type { NormalFeatureGroupEntry } from "../types/normal-feature-group";
import type { PageResult } from "../types/page";
import { bzConfirm } from "../utils/confirm";
import { formatDateTime } from "../utils/formatter";
import { message } from "../utils/message";

type DictMeta = { label: string; tagType?: string | null };

const loading = ref(false);
const rows = ref<ExternalUserEntry[]>([]);
const page = ref<PageResult<ExternalUserEntry>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});
const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref<ExternalUserEntry | null>(null);
const featureOpen = ref(false);
const featureLoading = ref(false);
const featureUser = ref<ExternalUserEntry | null>(null);
const featureManagement = ref<NormalFeatureUserManagementEntry | null>(null);
const featureGroups = ref<NormalFeatureGroupEntry[]>([]);
const selectedGroupIds = ref<string[]>([]);
const userFeatures = ref<NormalFeatureUserFeatureEntry[]>([]);

const queryPanelVisible = ref(false);
const queryCollapsed = ref(true);
const keywordDraft = ref("");
const statusDraft = ref<"" | ExternalUserStatus>("");
const appliedKeyword = ref("");
const appliedStatus = ref<"" | ExternalUserStatus>("");
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;

const featureKeywordDraft = ref("");
const featureStatusDraft = ref<"" | "true" | "false">("");
const overrideFilterDraft = ref<"" | NormalFeatureOverrideType>("");
const appliedFeatureKeyword = ref("");
const appliedFeatureStatus = ref<"" | "true" | "false">("");
const appliedOverrideFilter = ref<"" | NormalFeatureOverrideType>("");
const featurePageNo = ref(1);
const featurePageSize = ref(10);

const userTypeMetaMap = ref<Record<string, DictMeta>>({});
const webUserStatusMetaMap = ref<Record<string, DictMeta>>({});

const canView = computed(() => hasResourceCodeAccess("web-user-manage-view"));
const canEdit = computed(() => hasResourceCodeAccess("web-user-manage-edit"));
const canFeatureManage = computed(
  () =>
    hasResourceCodeAccess("normal-feature-user-view") ||
    hasResourceCodeAccess("normal-feature-user-save"),
);
const canFeatureSave = computed(() => hasResourceCodeAccess("normal-feature-user-save"));
const totalPages = computed(() => Math.max(1, page.value.totalPages || 1));
const isFirstPage = computed(() => pageNo.value <= 1);
const isLastPage = computed(() => pageNo.value >= totalPages.value);
const pageTokens = computed<Array<number | "ellipsis">>(() =>
  buildTokens(pageNo.value, totalPages.value),
);
const filteredUserFeatures = computed(() => {
  const kw = appliedFeatureKeyword.value.trim().toLowerCase();
  return userFeatures.value.filter((item) => {
    if (
      appliedFeatureStatus.value !== "" &&
      item.enabled !== (appliedFeatureStatus.value === "true")
    )
      return false;
    if (appliedOverrideFilter.value !== "" && item.overrideType !== appliedOverrideFilter.value)
      return false;
    if (!kw) return true;
    return (
      item.code.toLowerCase().includes(kw) ||
      item.name.toLowerCase().includes(kw) ||
      (item.description || "").toLowerCase().includes(kw)
    );
  });
});
const featureTotalPages = computed(() =>
  Math.max(1, Math.ceil(filteredUserFeatures.value.length / featurePageSize.value)),
);
const featureIsFirstPage = computed(() => featurePageNo.value <= 1);
const featureIsLastPage = computed(() => featurePageNo.value >= featureTotalPages.value);
const featurePageTokens = computed<Array<number | "ellipsis">>(() =>
  buildTokens(featurePageNo.value, featureTotalPages.value),
);
const pagedUserFeatures = computed(() => {
  const start = (featurePageNo.value - 1) * featurePageSize.value;
  return filteredUserFeatures.value.slice(start, start + featurePageSize.value);
});

onMounted(async () => {
  await Promise.all([loadDictionaries(), reload()]);
});

async function loadDictionaries() {
  try {
    const result = await batchListDictOptions(["USER_TYPE", "WEB_USER_STATUS"]);
    userTypeMetaMap.value = toMetaMap(result.USER_TYPE);
    webUserStatusMetaMap.value = toMetaMap(result.WEB_USER_STATUS);
  } catch {
    userTypeMetaMap.value = { EXTERNAL: { label: "用户", tagType: "info" } };
    webUserStatusMetaMap.value = {
      ACTIVE: { label: "启用", tagType: "success" },
      DISABLED: { label: "停用", tagType: "warning" },
      CANCELLED: { label: "已注销", tagType: "danger" },
    };
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
  if (current >= total - 3)
    return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
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
    let result = await pageExternalUsers({
      keyword: appliedKeyword.value || undefined,
      status: appliedStatus.value,
      pageNo: requestedPageNo,
      pageSize: pageSize.value,
    });
    if (result.totalElements > 0 && requestedPageNo > Math.max(1, result.totalPages)) {
      pageNo.value = Math.max(1, result.totalPages);
      result = await pageExternalUsers({
        keyword: appliedKeyword.value || undefined,
        status: appliedStatus.value,
        pageNo: pageNo.value,
        pageSize: pageSize.value,
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
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === pageSize.value)
    return;
  pageSize.value = nextPageSize;
  pageNo.value = 1;
  void reload();
}

function getRowActions(row: ExternalUserEntry): AdminActionItem[] {
  const actions: AdminActionItem[] = [
    { key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) },
  ];
  if (canFeatureManage.value)
    actions.push({
      key: `feature-${row.id}`,
      label: "功能",
      tone: "detail",
      handler: () => openFeatureManagement(row),
    });
  if (canEdit.value && row.status !== "CANCELLED")
    actions.push({
      key: `toggle-${row.id}`,
      label: row.status === "ACTIVE" ? "停用" : "启用",
      tone: row.status === "ACTIVE" ? "disable" : "enable",
      handler: () => toggleStatus(row),
    });
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
  const confirmed = await bzConfirm({
    title: nextStatus === "ACTIVE" ? "启用用户" : "停用用户",
    message: `确认${nextStatus === "ACTIVE" ? "启用" : "停用"}：${row.account}？`,
    confirmText: nextStatus === "ACTIVE" ? "启用" : "停用",
    cancelText: "取消",
    danger: nextStatus !== "ACTIVE",
  });
  if (!confirmed) return;
  await updateExternalUser(row.id, { status: nextStatus });
  message.success(nextStatus === "ACTIVE" ? "已启用" : "已停用");
  await reload();
  if (detailOpen.value && detail.value?.id === row.id) detail.value = await getExternalUser(row.id);
}

async function openFeatureManagement(user: ExternalUserEntry) {
  featureOpen.value = true;
  featureLoading.value = true;
  featureUser.value = user;
  try {
    const [management, groupPage] = await Promise.all([
      getNormalUserManagement(user.id),
      pageNormalFeatureGroups({ enabled: true, page: { pageNo: 1, pageSize: 200 } }),
    ]);
    featureManagement.value = management;
    featureGroups.value = groupPage.elements;
    selectedGroupIds.value = [...management.groupIds];
    userFeatures.value = management.features.map((item) => ({ ...item }));
    resetFeatureFilters();
  } finally {
    featureLoading.value = false;
  }
}

function applyFeatureFilters() {
  appliedFeatureKeyword.value = featureKeywordDraft.value;
  appliedFeatureStatus.value = featureStatusDraft.value;
  appliedOverrideFilter.value = overrideFilterDraft.value;
  featurePageNo.value = 1;
}
function resetFeatureFilters() {
  featureKeywordDraft.value = "";
  featureStatusDraft.value = "";
  overrideFilterDraft.value = "";
  appliedFeatureKeyword.value = "";
  appliedFeatureStatus.value = "";
  appliedOverrideFilter.value = "";
  featurePageNo.value = 1;
}
function goToFeaturePage(nextPage: number) {
  const target = Math.min(Math.max(nextPage, 1), featureTotalPages.value);
  if (target === featurePageNo.value) return;
  featurePageNo.value = target;
}
function handleFeaturePageSizeSelect(event: Event) {
  const nextPageSize = Number((event.target as HTMLSelectElement).value);
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === featurePageSize.value)
    return;
  featurePageSize.value = nextPageSize;
  featurePageNo.value = 1;
}

function toggleGroupSelection(groupId: string, checked: boolean) {
  const next = new Set(selectedGroupIds.value);
  if (checked) next.add(groupId);
  else next.delete(groupId);
  selectedGroupIds.value = Array.from(next);
  resetFeaturesByGroups();
}

function resetFeaturesByGroups() {
  const featureIds = new Set<string>();
  for (const group of featureGroups.value) {
    if (selectedGroupIds.value.includes(group.id)) {
      for (const featureId of group.featureIds) featureIds.add(featureId);
    }
  }
  userFeatures.value = userFeatures.value.map((feature) => ({
    ...feature,
    groupEnabled: featureIds.has(feature.id) && feature.enabled,
    effectiveEnabled: featureIds.has(feature.id) && feature.enabled,
    overrideType: "NONE",
  }));
}

function overrideLabel(type: NormalFeatureOverrideType): string {
  if (type === "ENABLE") return "强制开启";
  if (type === "DISABLE") return "强制关闭";
  return "继承分组";
}
function overrideTagType(type: NormalFeatureOverrideType): string {
  if (type === "ENABLE") return "success";
  if (type === "DISABLE") return "danger";
  return "info";
}
function setFeatureOverride(featureId: string, type: NormalFeatureOverrideType) {
  userFeatures.value = userFeatures.value.map((feature) =>
    feature.id === featureId
      ? {
          ...feature,
          overrideType: type,
          effectiveEnabled:
            type === "ENABLE" ? true : type === "DISABLE" ? false : feature.groupEnabled,
        }
      : feature,
  );
}

async function saveFeatureManagement() {
  if (!featureUser.value || !canFeatureSave.value) return;
  await saveNormalUserManagement(featureUser.value.id, {
    groupIds: selectedGroupIds.value,
    overrides: userFeatures.value
      .filter((feature) => feature.overrideType !== "NONE")
      .map((feature) => ({ featureId: feature.id, overrideType: feature.overrideType })),
  });
  message.success("功能配置已保存");
  featureOpen.value = false;
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
.drawer-subtitle {
  color: #64748b;
  font-size: 12px;
}
.feature-manage-layout {
  display: grid;
  gap: 18px;
}
.feature-manage-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.feature-manage-card {
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
  display: grid;
  gap: 8px;
}
.feature-manage-card span {
  color: #64748b;
  font-size: 12px;
}
.feature-manage-card strong {
  color: #0f172a;
  font-size: 28px;
  font-weight: 800;
}
.feature-manage-section {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
  padding: 16px;
}
.feature-manage-section__head,
.feature-manage-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.feature-manage-section__title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}
.feature-manage-section__tip {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}
.feature-group-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.feature-group-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #dbe1ea;
  border-radius: 10px;
  background: #f8fafc;
}
.feature-group-item__name {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}
.feature-group-item__code {
  color: #64748b;
  font-size: 12px;
}
.feature-name {
  font-weight: 700;
  color: #0f172a;
}
.feature-code,
.feature-desc,
.feature-perms {
  color: #64748b;
  font-size: 12px;
  margin-top: 4px;
}
.feature-row-actions {
  display: flex;
  gap: 8px;
}
.feature-manage-pagination {
  padding-left: 0;
  padding-right: 0;
}
@media (max-width: 900px) {
  .detail-grid,
  .feature-manage-cards,
  .feature-group-list {
    grid-template-columns: 1fr;
  }
  .feature-manage-section__head,
  .feature-manage-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
