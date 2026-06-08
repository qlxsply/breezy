<!-- /src/pages/ResourcesAdminPage.vue -->
<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section class="list-page-actions">
          <div class="list-page-actions-main">
            <bz-button
              v-if="canCreate"
              type="primary"
              @click="openCreate"
              >新增</bz-button
            >
            <bz-button @click="onRefreshPermissions">刷新权限</bz-button>
            <bz-button @click="reload">刷新</bz-button>
            <bz-button @click="expandAllRows">全部展开</bz-button>
            <bz-button @click="collapseAllRows">全部折叠</bz-button>
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
                <div class="list-page-filter-label">关键词</div>
                <bz-input
                  v-model="q"
                  class="list-page-filter-control"
                  placeholder="按名称/编码/URL 搜索"
                  clearable
                  @keyup.enter="applyFilters"
                />
              </div>
            </bz-form-item>

            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">类型</div>
                <bz-select
                  v-model="type"
                  class="list-page-filter-control"
                  placeholder="全部类型"
                  clearable
                >
                  <bz-option
                    label="菜单"
                    value="MENU"
                  />
                  <bz-option
                    label="按钮"
                    value="BUTTON"
                  />
                  <bz-option
                    label="功能"
                    value="FEATURE"
                  />
                  <bz-option
                    label="数据"
                    value="DATA"
                  />
                </bz-select>
              </div>
            </bz-form-item>

            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">入口</div>
                <bz-select
                  v-model="scope"
                  class="list-page-filter-control"
                  placeholder="全部入口"
                  clearable
                >
                  <bz-option
                    label="工具入口"
                    value="TOOL"
                  />
                  <bz-option
                    label="设置入口"
                    value="SETTING"
                  />
                  <bz-option
                    label="信息入口"
                    value="INFO"
                  />
                  <bz-option
                    label="非入口"
                    value="NONE"
                  />
                </bz-select>
              </div>
            </bz-form-item>

            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">级别</div>
                <bz-select
                  v-model="level"
                  class="list-page-filter-control"
                  placeholder="全部级别"
                  clearable
                >
                  <bz-option
                    label="系统"
                    value="SYSTEM"
                  />
                  <bz-option
                    label="自定义"
                    value="CUSTOM"
                  />
                </bz-select>
              </div>
            </bz-form-item>

            <bz-form-item class="list-page-filter-actions">
              <bz-button
                type="primary"
                @click="applyFilters"
                >搜索</bz-button
              >
              <bz-button @click="resetFilters">重置</bz-button>
            </bz-form-item>
          </bz-form>
        </bz-card>

        <bz-card class="list-page-result-card">
          <ResourceTable
            :rows="filtered"
            :loading="loading"
            :expand-all="expandAll"
            :expand-signal="expandSignal"
            :can-edit="canEdit"
            :can-delete="canDelete"
            :can-api="canApi"
            @edit="openEdit"
            @remove="onRemove"
            @api="openApi"
          />
        </bz-card>
      </div>

      <ResourceFormDialog
        v-if="dialogOpen"
        :mode="dialogMode"
        :model="dialogModel"
        :resources="rows"
        @close="dialogOpen = false"
        @submit="onSubmit"
      />

      <ResourceApiDialog
        v-if="apiDialogOpen"
        :resource-name="apiTarget?.name || ''"
        :apis="apiList"
        :selected-ids="selectedApiIds"
        :loading="apiLoading"
        :can-save="canApiEdit"
        @close="apiDialogOpen = false"
        @submit="onApiSubmit"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
// <script setup> + TS：顶层代码即 setup()。
import { computed, onMounted, ref, watch } from "vue";

import { listApis } from "../api/apis";
import {
  createResource,
  deleteResource,
  getResourceApis,
  listResources,
  updateResource,
  updateResourceApis,
} from "../api/resources";
import ResourceApiDialog from "../components/resources-admin/ResourceApiDialog.vue";
import ResourceFormDialog from "../components/resources-admin/ResourceFormDialog.vue";
import ResourceTable from "../components/resources-admin/ResourceTable.vue";
import { hasApiPermission, refreshPermissions } from "../registry/permissions.registry";
import type { ApiEntry } from "../types/api-admin";
import type {
  ResourceEntry,
  ResourceEntryCreate,
  ResourceEntryUpdate,
  ResourceLevel,
  ResourceScope,
  ResourceType,
} from "../types/resource-admin";
import { bzConfirm } from "../utils/confirm";
import { message } from "../utils/message";

/**
 * 资源管理页：CRUD
 * - 列表查询：本地过滤（适合数据量不大）
 * - 后续如数据量大，可改成后端分页查询
 */

// ref<T[]>：泛型指定数组元素类型。
const rows = ref<ResourceEntry[]>([]);
const loading = ref(false);

// 查询条件
const q = ref("");
const type = ref<"" | ResourceType>("");
const scope = ref<"" | ResourceScope>("");
const level = ref<"" | ResourceLevel>("");

// 弹窗状态
const dialogOpen = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const dialogModel = ref<ResourceEntry | null>(null);
const expandAll = ref(true);
const expandSignal = ref(0);

const apiDialogOpen = ref(false);
const apiTarget = ref<ResourceEntry | null>(null);
const apiList = ref<ApiEntry[]>([]);
const selectedApiIds = ref<string[]>([]);
const apiLoading = ref(false);

const canCreate = computed(() => hasApiPermission("res.add"));
const canEdit = computed(() => hasApiPermission("res.edit"));
const canDelete = computed(() => hasApiPermission("res.del"));
const canApiView = computed(() => hasApiPermission("res.api.view"));
const canApiEdit = computed(() => hasApiPermission("res.api.edit"));
const canApi = computed(() => canApiView.value || canApiEdit.value);

onMounted(() => reload());

watch(
  () => [q.value, type.value, scope.value, level.value],
  ([keyword, typeVal, scopeVal, levelVal]) => {
    const hasFilter = Boolean(keyword.trim() || typeVal || scopeVal || levelVal);
    if (hasFilter) expandAllRows();
  },
);

async function reload() {
  loading.value = true;
  try {
    rows.value = await listResources();
  } finally {
    loading.value = false;
  }
}

// computed 的返回值是只读 ref，依赖变化会自动更新。
const filtered = computed(() => {
  const kw = q.value.trim().toLowerCase();
  const hasFilter = Boolean(kw || type.value || scope.value || level.value);
  if (!hasFilter) return rows.value;

  const byId = new Map(rows.value.map((r) => [r.id, r]));
  const matchedIds = new Set<string>();

  const matches = rows.value.filter((r) => {
    if (type.value && r.type !== type.value) return false;
    if (scope.value && r.scope !== scope.value) return false;
    if (level.value && r.level !== level.value) return false;
    if (!kw) return true;
    const name = r.name.toLowerCase();
    const code = r.code.toLowerCase();
    const url = (r.url ?? "").toLowerCase();
    const target = (r.loadTarget ?? "").toLowerCase();
    const desc = (r.description ?? "").toLowerCase();
    return (
      name.includes(kw) ||
      code.includes(kw) ||
      url.includes(kw) ||
      target.includes(kw) ||
      desc.includes(kw)
    );
  });

  const addWithAncestors = (row: ResourceEntry) => {
    let current: ResourceEntry | undefined = row;
    while (current) {
      if (matchedIds.has(current.id)) break;
      matchedIds.add(current.id);
      const pid = current.parentId ?? null;
      if (!pid) break;
      current = byId.get(pid);
    }
  };

  matches.forEach(addWithAncestors);
  return rows.value.filter((r) => matchedIds.has(r.id));
});

function openCreate() {
  if (!canCreate.value) return;
  dialogMode.value = "create";
  dialogModel.value = {
    id: "",
    parentId: "",
    name: "",
    icon: "",
    description: "",
    code: "",
    type: "MENU",
    scope: "SETTING",
    openMode: "PAGE",
    url: "",
    loadTarget: "",
    orderNo: 100,
    level: "CUSTOM",
    enabled: true,
    guestAccess: false,
  };
  dialogOpen.value = true;
}

function openEdit(row: ResourceEntry) {
  if (!canEdit.value) return;
  if (row.level === "SYSTEM") return;
  dialogMode.value = "edit";
  // 展开运算符复制对象，避免直接引用原对象。
  dialogModel.value = { ...row };
  dialogOpen.value = true;
}

async function onRemove(row: ResourceEntry) {
  if (!canDelete.value) return;
  if (row.level === "SYSTEM") return;
  const confirmed = await bzConfirm({
    title: "删除资源",
    message: `确认删除：${row.name} (${row.code}) ?`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  await deleteResource(row.id);
  message.success("删除成功");
  await reload();
}

async function openApi(row: ResourceEntry) {
  if (!canApi.value) return;
  apiTarget.value = row;
  apiDialogOpen.value = true;
  apiLoading.value = true;
  try {
    if (apiList.value.length === 0) {
      apiList.value = await listApis();
    }
    selectedApiIds.value = await getResourceApis(row.id);
  } finally {
    apiLoading.value = false;
  }
}

async function onApiSubmit(ids: string[]) {
  if (!apiTarget.value) return;
  if (!canApiEdit.value) return;
  await updateResourceApis(apiTarget.value.id, ids);
  apiDialogOpen.value = false;
  await reload();
}

async function onSubmit(model: ResourceEntry) {
  if (dialogMode.value === "create") {
    const req: ResourceEntryCreate = { ...model, level: "CUSTOM" };
    await createResource(req);
  } else {
    const req: ResourceEntryUpdate = model;
    await updateResource(model.id, req);
  }

  dialogOpen.value = false;
  await reload();
}

function expandAllRows() {
  expandAll.value = true;
  expandSignal.value += 1;
}

function collapseAllRows() {
  expandAll.value = false;
  expandSignal.value += 1;
}

async function onRefreshPermissions() {
  await refreshPermissions();
}

function applyFilters() {
  expandAllRows();
}

function resetFilters() {
  q.value = "";
  type.value = "";
  scope.value = "";
  level.value = "";
  expandAllRows();
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
</style>
