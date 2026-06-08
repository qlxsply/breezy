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
                    placeholder="按编码、名称搜索"
                    clearable
                    @keyup.enter="applyFilters"
                  />
                </div>
              </div>
            </bz-form-item>

            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">状态</div>
                <div class="admin-filter-control">
                  <bz-select
                    v-model="enabledDraft"
                    placeholder="全部状态"
                    clearable
                  >
                    <bz-option
                      label="启用"
                      value="true"
                    />
                    <bz-option
                      label="停用"
                      value="false"
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
              <div class="admin-table-title">角色管理</div>
              <div class="admin-table-tools">
                <bz-button
                  v-if="canCreate"
                  class="admin-toolbar-primary"
                  type="primary"
                  @click="openCreate"
                >
                  新增
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
            <RoleTable
              :rows="pagedRows"
              :loading="loading"
              :can-edit="canEdit"
              :can-delete="canDelete"
              :can-permissions="canGrant"
              @edit="openEdit"
              @remove="onRemove"
              @permissions="openGrants"
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

        <RoleFormDialog
          v-if="dialogOpen"
          :mode="dialogMode"
          :model="dialogModel"
          @close="dialogOpen = false"
          @submit="onSubmit"
        />

        <RolePermissionDialog
          v-if="grantOpen"
          :role-name="grantTarget?.name || ''"
          :resources="grantResources"
          :selection="grantSelection"
          :loading="grantLoading || grantResourcesLoading"
          :can-save="canGrantEdit"
          @close="grantOpen = false"
          @submit="onGrantSubmit"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import {
  createRole,
  deleteRole,
  getRoleGrantSelection,
  listRoleGrantResources,
  listRoles,
  updateRole,
  updateRoleGrantSelection,
} from "../api/roles";
import RoleFormDialog from "../components/roles-admin/RoleFormDialog.vue";
import RolePermissionDialog from "../components/roles-admin/RolePermissionDialog.vue";
import RoleTable from "../components/roles-admin/RoleTable.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { RoleEntry, RoleGrantResourceEntry, RoleGrantSelection } from "../types/role-admin";
import { bzConfirm } from "../utils/confirm";
import { message } from "../utils/message";

const rows = ref<RoleEntry[]>([]);
const loading = ref(false);

const queryPanelVisible = ref(false);
const queryCollapsed = ref(true);
const keywordDraft = ref("");
const enabledDraft = ref<"" | "true" | "false">("");
const appliedKeyword = ref("");
const appliedEnabled = ref<"" | "true" | "false">("");
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;

const dialogOpen = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const dialogModel = ref<RoleEntry | null>(null);

const grantOpen = ref(false);
const grantTarget = ref<RoleEntry | null>(null);
const grantSelection = ref<RoleGrantSelection>({ menuIds: [], functionIds: [] });
const grantLoading = ref(false);
const grantResourcesLoading = ref(false);
const grantResourceRows = ref<RoleGrantResourceEntry[]>([]);

const grantResources = computed(() => grantResourceRows.value);

const canCreate = computed(() => hasResourceCodeAccess("role-manage-create"));
const canEdit = computed(() => hasResourceCodeAccess("role-manage-edit"));
const canDelete = computed(() => hasResourceCodeAccess("role-manage-delete"));
const canGrantView = computed(() => hasResourceCodeAccess("role-manage-permission-view"));
const canGrantEdit = computed(() => hasResourceCodeAccess("role-manage-permission-edit"));
const canGrant = computed(() => canGrantView.value || canGrantEdit.value);

const filteredRows = computed(() => {
  const kw = appliedKeyword.value.trim().toLowerCase();
  const enabledValue = appliedEnabled.value === "" ? "" : appliedEnabled.value === "true";

  return rows.value.filter((role) => {
    if (enabledValue !== "" && role.enabled !== enabledValue) {
      return false;
    }

    if (!kw) {
      return true;
    }

    return role.code.toLowerCase().includes(kw) || role.name.toLowerCase().includes(kw);
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

onMounted(async () => {
  await Promise.all([reloadGrantResources(), reload()]);
});

async function reloadGrantResources() {
  grantResourcesLoading.value = true;

  try {
    grantResourceRows.value = await listRoleGrantResources();
  } catch {
    grantResourceRows.value = [];
  } finally {
    grantResourcesLoading.value = false;
  }
}

async function reload() {
  loading.value = true;

  try {
    rows.value = await listRoles();
    syncPageNoWithinRange();
  } finally {
    loading.value = false;
  }
}

function applyFilters() {
  appliedKeyword.value = keywordDraft.value;
  appliedEnabled.value = enabledDraft.value;
  pageNo.value = 1;
}

function resetFilters() {
  keywordDraft.value = "";
  enabledDraft.value = "";
  applyFilters();
}

function goToPage(nextPage: number) {
  const target = Math.min(Math.max(nextPage, 1), totalPages.value);

  if (target === pageNo.value) {
    return;
  }

  pageNo.value = target;
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

function syncPageNoWithinRange() {
  const maxPageNo = Math.max(1, totalPages.value);

  if (pageNo.value > maxPageNo) {
    pageNo.value = maxPageNo;
  }
}

function openCreate() {
  if (!canCreate.value) {
    return;
  }

  dialogMode.value = "create";
  dialogModel.value = {
    id: "",
    code: "",
    name: "",
    enabled: true,
  };
  dialogOpen.value = true;
}

function openEdit(role: RoleEntry) {
  if (!canEdit.value) {
    return;
  }

  dialogMode.value = "edit";
  dialogModel.value = { ...role };
  dialogOpen.value = true;
}

async function onSubmit(payload: RoleEntry) {
  if (dialogMode.value === "create") {
    await createRole({
      code: payload.code,
      name: payload.name,
      enabled: payload.enabled,
    });
  } else if (dialogModel.value) {
    await updateRole(dialogModel.value.id, {
      code: payload.code,
      name: payload.name,
      enabled: payload.enabled,
    });
  }

  dialogOpen.value = false;
  await reload();
}

async function onRemove(role: RoleEntry) {
  if (!canDelete.value) {
    return;
  }

  const confirmed = await bzConfirm({
    title: "删除角色",
    message: `确认删除：${role.name} (${role.code})？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });

  if (!confirmed) {
    return;
  }

  await deleteRole(role.id);
  message.success("删除成功");
  await reload();
}

async function openGrants(role: RoleEntry) {
  if (!canGrant.value) {
    return;
  }

  grantTarget.value = role;
  grantOpen.value = true;
  grantLoading.value = true;

  try {
    if (!grantResourceRows.value.length) {
      await reloadGrantResources();
    }

    grantSelection.value = await getRoleGrantSelection(role.id);
  } finally {
    grantLoading.value = false;
  }
}

async function onGrantSubmit(selection: RoleGrantSelection) {
  if (!grantTarget.value || !canGrantEdit.value) {
    return;
  }

  await updateRoleGrantSelection(grantTarget.value.id, selection);

  grantSelection.value = {
    menuIds: [...selection.menuIds],
    functionIds: [...selection.functionIds],
  };

  message.success("角色授权已保存，受影响用户需要重新登录");
  grantOpen.value = false;
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
</style>
