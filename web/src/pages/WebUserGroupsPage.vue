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
                    placeholder="按编码或名称搜索"
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
                    v-model="enabledDraft"
                    placeholder="全部状态"
                    clearable
                    ><bz-option
                      label="启用"
                      value="true" /><bz-option
                      label="停用"
                      value="false"
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
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">用户分组</div>
              <div class="admin-table-tools">
                <bz-button
                  v-if="canEdit"
                  class="admin-toolbar-primary"
                  type="primary"
                  @click="openCreate"
                  >新增</bz-button
                >
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
            <bz-table
              v-loading="loading"
              :data="rows"
              row-key="id"
              empty-text="暂无分组"
              size="small"
            >
              <bz-table-column
                prop="code"
                label="分组编码"
                min-width="180"
                show-overflow-tooltip
              />
              <bz-table-column
                prop="name"
                label="名称"
                min-width="160"
                show-overflow-tooltip
              />
              <bz-table-column
                label="类型"
                width="110"
                ><template #default="scope"
                  ><bz-tag :type="resolveTagType(groupTypeMetaMap, scope.row.groupType)">{{
                    resolveLabel(groupTypeMetaMap, scope.row.groupType)
                  }}</bz-tag></template
                ></bz-table-column
              >
              <bz-table-column
                label="默认分组"
                width="100"
                ><template #default="scope"
                  ><bz-tag :type="scope.row.defaultGroup ? 'success' : 'info'">{{
                    scope.row.defaultGroup ? "是" : "否"
                  }}</bz-tag></template
                ></bz-table-column
              >
              <bz-table-column
                label="状态"
                width="100"
                ><template #default="scope"
                  ><bz-tag :type="scope.row.enabled ? 'success' : 'warning'">{{
                    scope.row.enabled ? "启用" : "停用"
                  }}</bz-tag></template
                ></bz-table-column
              >
              <bz-table-column
                prop="description"
                label="描述"
                min-width="220"
                show-overflow-tooltip
                ><template #default="scope">{{
                  scope.row.description || "-"
                }}</template></bz-table-column
              >
              <bz-table-column
                label="功能数"
                width="90"
                ><template #default="scope">{{
                  scope.row.featureIds.length
                }}</template></bz-table-column
              >
              <bz-table-column
                label="操作"
                width="160"
                fixed="right"
                ><template #default="scope"
                  ><AdminActionBar
                    :actions="getRowActions(scope.row)"
                    :more-actions="getRowMoreActions(scope.row)" /></template
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
          :open="drawerOpen"
          :loading="drawerLoading"
          :title="
            drawerMode === 'create' ? '新增分组' : drawerMode === 'edit' ? '编辑分组' : '分组详情'
          "
          width="980px"
          @close="drawerOpen = false"
        >
          <div
            v-if="drawerMode === 'detail' && currentGroup"
            class="detail-grid"
          >
            <div class="detail-field">
              <span class="detail-field__label">分组编码</span
              ><span class="detail-field__value">{{ currentGroup.code }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">名称</span
              ><span class="detail-field__value">{{ currentGroup.name }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">类型</span
              ><span class="detail-field__value">{{
                resolveLabel(groupTypeMetaMap, currentGroup.groupType)
              }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">默认分组</span
              ><span class="detail-field__value">{{
                currentGroup.defaultGroup ? "是" : "否"
              }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">状态</span
              ><span class="detail-field__value">{{ currentGroup.enabled ? "启用" : "停用" }}</span>
            </div>
            <div class="detail-field detail-field--wide">
              <span class="detail-field__label">描述</span
              ><span class="detail-field__value">{{ currentGroup.description || "-" }}</span>
            </div>
          </div>

          <div
            v-if="drawerMode === 'detail'"
            class="group-feature-panel"
          >
            <div class="group-feature-panel__head">
              <div class="group-feature-panel__title">关联功能</div>
              <div class="group-feature-panel__meta">已选 {{ detailFeatureRows.length }} 项</div>
            </div>
            <div class="admin-table-surface">
              <bz-table
                v-loading="detailFeatureLoading"
                :data="detailFeatureRows"
                row-key="id"
                size="small"
                empty-text="暂无关联功能"
              >
                <bz-table-column
                  prop="code"
                  label="功能编码"
                  min-width="180"
                  show-overflow-tooltip
                />
                <bz-table-column
                  prop="name"
                  label="名称"
                  min-width="160"
                  show-overflow-tooltip
                />
                <bz-table-column
                  label="状态"
                  width="100"
                >
                  <template #default="scope">
                    <bz-tag :type="scope.row.enabled ? 'success' : 'warning'">{{
                      scope.row.enabled ? "启用" : "停用"
                    }}</bz-tag>
                  </template>
                </bz-table-column>
              </bz-table>
            </div>
          </div>

          <template v-if="drawerMode !== 'detail'">
            <bz-form label-position="top">
              <div class="group-form-grid">
                <bz-form-item label="编码"
                  ><bz-input
                    v-model="form.code"
                    :disabled="drawerMode === 'edit'"
                /></bz-form-item>
                <bz-form-item label="名称"><bz-input v-model="form.name" /></bz-form-item>
                <bz-form-item label="类型"
                  ><bz-select v-model="form.groupType"
                    ><bz-option
                      v-for="item in groupTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value" /></bz-select
                ></bz-form-item>
                <bz-form-item label="状态"><bz-switch v-model="form.enabled" /></bz-form-item>
                <bz-form-item label="默认分组"
                  ><bz-switch v-model="form.defaultGroup"
                /></bz-form-item>
                <bz-form-item
                  label="描述"
                  class="group-form-grid__wide"
                  ><bz-text-field
                    v-model="form.description"
                    type="textarea"
                    :rows="3"
                /></bz-form-item>
              </div>
            </bz-form>

            <div class="group-feature-panel">
              <div class="group-feature-panel__head">
                <div class="group-feature-panel__title">关联功能</div>
                <div class="group-feature-panel__meta">已选 {{ selectedFeatureIds.length }} 项</div>
              </div>
              <div class="group-feature-panel__toolbar">
                <bz-input
                  v-model="featureKeywordDraft"
                  placeholder="搜索功能编码或名称"
                  clearable
                  @keyup.enter="reloadFeaturePage"
                /><bz-button
                  class="admin-filter-primary group-feature-search-btn"
                  type="primary"
                  @click="reloadFeaturePage"
                  >查询</bz-button
                >
              </div>
              <div class="admin-table-surface">
                <bz-table
                  v-loading="featureLoading"
                  :data="featureRows"
                  row-key="id"
                  size="small"
                  empty-text="暂无功能"
                  ><bz-table-column
                    label="选择"
                    width="60"
                    ><template #default="scope"
                      ><input
                        class="feature-checkbox"
                        type="checkbox"
                        :checked="selectedFeatureIds.includes(scope.row.id)"
                        @change="
                          toggleFeatureSelection(
                            scope.row.id,
                            ($event.target as HTMLInputElement).checked,
                          )
                        " /></template></bz-table-column
                  ><bz-table-column
                    prop="code"
                    label="功能编码"
                    min-width="180"
                    show-overflow-tooltip
                  /><bz-table-column
                    prop="name"
                    label="名称"
                    min-width="160"
                    show-overflow-tooltip
                  /><bz-table-column
                    label="状态"
                    width="100"
                    ><template #default="scope"
                      ><bz-tag :type="scope.row.enabled ? 'success' : 'warning'">{{
                        scope.row.enabled ? "启用" : "停用"
                      }}</bz-tag></template
                    ></bz-table-column
                  ></bz-table
                >
              </div>
              <div
                v-if="featurePage.totalElements > 0"
                class="dict-pagination-bar group-feature-pagination"
              >
                <div class="dict-pagination-summary">共 {{ featurePage.totalElements }} 条记录</div>
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
                      @click="goToFeaturePage(featureFeatureTotalPages)"
                    >
                      <span aria-hidden="true">&gt;|</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <template #footer
            ><bz-button @click="drawerOpen = false">{{
              drawerMode === "detail" ? "关闭" : "取消"
            }}</bz-button
            ><bz-button
              v-if="drawerMode !== 'detail'"
              type="primary"
              @click="submitGroup"
              >确定</bz-button
            ></template
          >
        </AdminEntityDrawer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { batchListDictOptions } from "../api/dicts";
import {
  createNormalFeatureGroup,
  deleteNormalFeatureGroup,
  getNormalFeatureGroup,
  pageNormalFeatureGroups,
  pageNormalFeatures,
  updateNormalFeatureGroup,
  updateNormalFeatureGroupStatus,
} from "../api/normal-features";
import AdminActionBar from "../components/admin/AdminActionBar.vue";
import AdminEntityDrawer from "../components/admin/AdminEntityDrawer.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { AdminActionItem } from "../types/admin-action";
import type { DictItem } from "../types/dict-admin";
import type { NormalFeatureEntry } from "../types/normal-feature";
import type {
  NormalFeatureGroupFeatureEntry,
  NormalFeatureGroupEntry,
  SaveNormalFeatureGroupRequest,
} from "../types/normal-feature-group";
import type { PageResult } from "../types/page";
import { bzConfirm } from "../utils/confirm";
import { message } from "../utils/message";

type TagType = "info" | "success" | "warning" | "danger";
type DictMeta = { label: string; tagType?: TagType };
type DrawerMode = "detail" | "edit" | "create";

const loading = ref(false);
const rows = ref<NormalFeatureGroupEntry[]>([]);
const page = ref<PageResult<NormalFeatureGroupEntry>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});
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
const currentGroup = ref<NormalFeatureGroupEntry | null>(null);
const form = reactive<SaveNormalFeatureGroupRequest>({
  code: "",
  name: "",
  groupType: "WHITELIST",
  description: "",
  enabled: true,
  defaultGroup: false,
  featureIds: [],
});

const featureLoading = ref(false);
const featureRows = ref<NormalFeatureEntry[]>([]);
const detailFeatureLoading = ref(false);
const detailFeatureRows = ref<NormalFeatureGroupFeatureEntry[]>([]);
const featurePage = ref<PageResult<NormalFeatureEntry>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});
const featureKeywordDraft = ref("");
const selectedFeatureIds = ref<string[]>([]);
const featurePageNo = ref(1);
const featurePageSize = ref(10);

const groupTypeMetaMap = ref<Record<string, DictMeta>>({});
const groupTypeOptions = computed(() =>
  Object.entries(groupTypeMetaMap.value).map(([value, meta]) => ({ value, label: meta.label })),
);
const canView = computed(() => hasResourceCodeAccess("web-user-stats-view"));
const canEdit = computed(() => hasResourceCodeAccess("nfm.user.save"));
const totalPages = computed(() => Math.max(1, page.value.totalPages || 1));
const isFirstPage = computed(() => pageNo.value <= 1);
const isLastPage = computed(() => pageNo.value >= totalPages.value);
const pageTokens = computed<Array<number | "ellipsis">>(() =>
  buildTokens(pageNo.value, totalPages.value),
);
const featureFeatureTotalPages = computed(() => Math.max(1, featurePage.value.totalPages || 1));
const featureIsFirstPage = computed(() => featurePageNo.value <= 1);
const featureIsLastPage = computed(() => featurePageNo.value >= featureFeatureTotalPages.value);
const featurePageTokens = computed<Array<number | "ellipsis">>(() =>
  buildTokens(featurePageNo.value, featureFeatureTotalPages.value),
);

onMounted(async () => {
  await Promise.all([loadDictionaries(), reload()]);
});

async function loadDictionaries() {
  try {
    const result = await batchListDictOptions(["NORMAL_FEATURE_GROUP_TYPE"]);
    groupTypeMetaMap.value = toMetaMap(result.NORMAL_FEATURE_GROUP_TYPE);
  } catch {
    groupTypeMetaMap.value = {};
  }
}
function toMetaMap(items?: DictItem[]): Record<string, DictMeta> {
  const map: Record<string, DictMeta> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = {
      label: item.itemLabel || item.itemValue,
      tagType: normalizeTagType(item.tagType),
    };
  }
  return map;
}
function normalizeTagType(value?: string | null): TagType | undefined {
  if (value === "info" || value === "success" || value === "warning" || value === "danger") {
    return value;
  }
  return undefined;
}
function resolveLabel(metaMap: Record<string, DictMeta>, value?: string | null): string {
  if (!value) return "-";
  return metaMap[value]?.label || value;
}
function resolveTagType(metaMap: Record<string, DictMeta>, value?: string | null): TagType {
  if (!value) return "info";
  return metaMap[value]?.tagType || "info";
}
function buildTokens(currentPageNo: number, totalPageCount: number): Array<number | "ellipsis"> {
  const total = totalPageCount;
  const current = Math.min(Math.max(currentPageNo, 1), total);
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
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
    const result = await pageNormalFeatureGroups({
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
function goToPage(next: number) {
  const t = Math.min(Math.max(next, 1), totalPages.value);
  if (t === pageNo.value) return;
  pageNo.value = t;
  void reload();
}
function handlePageSizeSelect(event: Event) {
  const v = Number((event.target as HTMLSelectElement).value);
  if (!Number.isFinite(v) || v <= 0 || v === pageSize.value) return;
  pageSize.value = v;
  pageNo.value = 1;
  void reload();
}

function getRowActions(row: NormalFeatureGroupEntry): AdminActionItem[] {
  return [
    { key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) },
    { key: `edit-${row.id}`, label: "编辑", tone: "edit", handler: () => openEdit(row.id) },
  ];
}
function getRowMoreActions(row: NormalFeatureGroupEntry): AdminActionItem[] {
  return [
    {
      key: `toggle-${row.id}`,
      label: row.enabled ? "停用" : "启用",
      tone: row.enabled ? "disable" : "enable",
      handler: () => toggleStatus(row),
    },
    { key: `delete-${row.id}`, label: "删除", tone: "delete", handler: () => removeGroup(row) },
  ];
}

async function openDetail(id: string) {
  drawerMode.value = "detail";
  drawerOpen.value = true;
  await loadGroup(id);
}
async function openEdit(id: string) {
  drawerMode.value = "edit";
  drawerOpen.value = true;
  await loadGroup(id);
  await reloadFeaturePage();
}
async function openCreate() {
  drawerMode.value = "create";
  drawerOpen.value = true;
  currentGroup.value = null;
  detailFeatureRows.value = [];
  Object.assign(form, {
    code: "",
    name: "",
    groupType: groupTypeOptions.value[0]?.value || "WHITELIST",
    description: "",
    enabled: true,
    defaultGroup: false,
    featureIds: [],
  });
  selectedFeatureIds.value = [];
  featureKeywordDraft.value = "";
  featurePageNo.value = 1;
  await reloadFeaturePage();
}
async function loadGroup(id: string) {
  drawerLoading.value = true;
  detailFeatureLoading.value = drawerMode.value === "detail";
  detailFeatureRows.value = [];
  try {
    const group = await getNormalFeatureGroup(id);
    currentGroup.value = group;
    Object.assign(form, {
      code: group.code,
      name: group.name,
      groupType: group.groupType,
      description: group.description || "",
      enabled: group.enabled,
      defaultGroup: group.defaultGroup,
      featureIds: [...group.featureIds],
    });
    selectedFeatureIds.value = [...group.featureIds];
    detailFeatureRows.value = Array.isArray(group.features) ? group.features : [];
  } finally {
    detailFeatureLoading.value = false;
    drawerLoading.value = false;
  }
}
async function reloadFeaturePage() {
  featureLoading.value = true;
  try {
    const result = await pageNormalFeatures({
      keyword: featureKeywordDraft.value.trim() || undefined,
      page: { pageNo: featurePageNo.value, pageSize: featurePageSize.value },
    });
    featurePage.value = result;
    featureRows.value = result.elements;
  } finally {
    featureLoading.value = false;
  }
}
function goToFeaturePage(next: number) {
  const t = Math.min(Math.max(next, 1), featureFeatureTotalPages.value);
  if (t === featurePageNo.value) return;
  featurePageNo.value = t;
  void reloadFeaturePage();
}
function handleFeaturePageSizeSelect(event: Event) {
  const v = Number((event.target as HTMLSelectElement).value);
  if (!Number.isFinite(v) || v <= 0 || v === featurePageSize.value) return;
  featurePageSize.value = v;
  featurePageNo.value = 1;
  void reloadFeaturePage();
}

async function submitGroup() {
  const payload: SaveNormalFeatureGroupRequest = {
    ...form,
    code: form.code.trim(),
    name: form.name.trim(),
    description: form.description?.trim() || "",
    featureIds: Array.from(new Set(selectedFeatureIds.value)),
  };
  if (!payload.code || !payload.name) {
    message.error("编码和名称不能为空");
    return;
  }
  if (drawerMode.value === "create") {
    await createNormalFeatureGroup(payload);
    message.success("创建成功");
  } else if (currentGroup.value) {
    await updateNormalFeatureGroup(currentGroup.value.id, payload);
    message.success("保存成功");
  }
  drawerOpen.value = false;
  await reload();
}
async function toggleStatus(row: NormalFeatureGroupEntry) {
  const nextEnabled = !row.enabled;
  const confirmed = await bzConfirm({
    title: nextEnabled ? "启用分组" : "停用分组",
    message: `确认${nextEnabled ? "启用" : "停用"}：${row.name}？`,
    confirmText: nextEnabled ? "启用" : "停用",
    cancelText: "取消",
    danger: !nextEnabled,
  });
  if (!confirmed) return;
  await updateNormalFeatureGroupStatus(row.id, nextEnabled);
  message.success(nextEnabled ? "已启用" : "已停用");
  await reload();
}
async function removeGroup(row: NormalFeatureGroupEntry) {
  const confirmed = await bzConfirm({
    title: "删除分组",
    message: `确认删除：${row.name} (${row.code})？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) return;
  await deleteNormalFeatureGroup(row.id);
  message.success("删除成功");
  await reload();
}
function toggleFeatureSelection(featureId: string, checked: boolean) {
  const next = new Set(selectedFeatureIds.value);
  if (checked) {
    next.add(featureId);
  } else {
    next.delete(featureId);
  }
  selectedFeatureIds.value = Array.from(next);
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
.detail-grid,
.group-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}
.group-form-grid__wide,
.detail-field--wide {
  grid-column: 1 / -1;
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
.group-feature-panel {
  margin-top: 18px;
}
.group-feature-panel__head {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}
.group-feature-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.group-feature-panel__toolbar :deep(.bz-input) {
  flex: 1;
}
.group-feature-search-btn {
  flex-shrink: 0;
  white-space: nowrap;
}
.group-feature-panel__title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}
.group-feature-panel__meta {
  color: #64748b;
  font-size: 12px;
}
.group-feature-pagination {
  padding-left: 0;
  padding-right: 0;
}
.feature-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
}
@media (max-width: 900px) {
  .detail-grid,
  .group-form-grid {
    grid-template-columns: 1fr;
  }
  .group-feature-panel__head,
  .group-feature-panel__toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
