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
                    v-model="keywordDraft"
                    placeholder="按账号或昵称搜索"
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
                    v-model="statusDraft"
                    placeholder="全部状态"
                    clearable
                  >
                    <bz-option
                      label="启用"
                      value="ENABLED"
                    />
                    <bz-option
                      label="停用"
                      value="DISABLED"
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
              <div class="admin-table-title">账号</div>
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
            <UserTable
              :rows="rows"
              :loading="loading"
              :can-edit="canEdit"
              :can-toggle="canToggle"
              :can-reset="canReset"
              :can-roles="canRoles"
              :can-delete="canDelete"
              :user-type-meta-map="userTypeMetaMap"
              @edit="openEdit"
              @toggle="onToggle"
              @reset="openReset"
              @roles="openRoles"
              @remove="onRemove"
            />
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
        </bz-card>

        <UserFormDialog
          v-if="dialogOpen"
          :mode="dialogMode"
          :model="dialogModel"
          @close="dialogOpen = false"
          @submit="onSubmit"
        />

        <PasswordResetDialog
          v-if="resetOpen"
          @close="resetOpen = false"
          @submit="onResetSubmit"
        />

        <UserRoleDialog
          v-if="roleOpen"
          :user-name="roleTarget?.username || ''"
          :roles="roleList"
          :selected-ids="roleSelected"
          :loading="roleLoading"
          :can-save="canRoleEdit"
          @close="roleOpen = false"
          @submit="onRoleSubmit"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { batchListDictOptions } from "@admin/api/dicts";
import { listRoles } from "@admin/api/roles";
import {
  createUser,
  deleteUser,
  getUserRoles,
  pageUsers,
  resetUserPassword,
  updateUser,
  updateUserRoles,
} from "@admin/api/users";
import PasswordResetDialog from "@admin/components/users-admin/PasswordResetDialog.vue";
import UserFormDialog from "@admin/components/users-admin/UserFormDialog.vue";
import UserRoleDialog from "@admin/components/users-admin/UserRoleDialog.vue";
import UserTable from "@admin/components/users-admin/UserTable.vue";
import { hasAdminResourceCodeAccess } from "@admin/registry/admin-permissions";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import type { RoleEntry } from "@admin/types/role-admin";
import type { UserEntry, UserStatus } from "@admin/types/user-admin";
import { bzConfirm } from "@shared/utils/confirm";
import { message } from "@shared/utils/message";
import { computed, onMounted, ref } from "vue";

const loading = ref(false);
const rows = ref<UserEntry[]>([]);
const page = ref<PageResult<UserEntry>>({
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
const statusDraft = ref<"" | UserStatus>("");
const appliedKeyword = ref("");
const appliedStatus = ref<"" | UserStatus>("");
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;

const userTypeMetaMap = ref<Record<string, { label: string; tagType?: string | null }>>({});

const dialogOpen = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const dialogModel = ref<UserEntry | null>(null);

const resetOpen = ref(false);
const resetTarget = ref<UserEntry | null>(null);

const roleOpen = ref(false);
const roleTarget = ref<UserEntry | null>(null);
const roleList = ref<RoleEntry[]>([]);
const roleSelected = ref<string[]>([]);
const roleLoading = ref(false);

const canCreate = computed(() => hasAdminResourceCodeAccess("user-manage-create"));
const canEdit = computed(() => hasAdminResourceCodeAccess("user-manage-edit"));
const canToggle = computed(() => hasAdminResourceCodeAccess("user-manage-edit"));
const canReset = computed(() => hasAdminResourceCodeAccess("user-manage-reset-password"));
const canRoleEdit = computed(() => hasAdminResourceCodeAccess("user-manage-role-edit"));
const canRoles = computed(
  () => canRoleEdit.value || hasAdminResourceCodeAccess("user-manage-role-view"),
);
const canDelete = computed(() => hasAdminResourceCodeAccess("user-manage-delete"));

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
    const result = await batchListDictOptions(["USER_TYPE"]);
    userTypeMetaMap.value = toDictMetaMap(result.USER_TYPE);
  } catch {
    userTypeMetaMap.value = {
      SYSTEM: { label: "系统账号", tagType: "warning" },
      INTERNAL: { label: "账号", tagType: "info" },
      EXTERNAL: { label: "用户", tagType: "info" },
      GUEST: { label: "游客", tagType: "danger" },
    };
  }
}

function toDictMetaMap(
  items?: DictItem[],
): Record<string, { label: string; tagType?: string | null }> {
  const map: Record<string, { label: string; tagType?: string | null }> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = {
      label: item.itemLabel || item.itemValue,
      tagType: item.tagType || undefined,
    };
  }
  return map;
}

async function reload() {
  loading.value = true;
  try {
    const requestedPageNo = pageNo.value;
    let result = await pageUsers({
      usernameLike: appliedKeyword.value,
      status: appliedStatus.value,
      page: {
        pageNo: requestedPageNo,
        pageSize: pageSize.value,
      },
    });

    if (result.totalElements > 0 && requestedPageNo > Math.max(1, result.totalPages)) {
      pageNo.value = Math.max(1, result.totalPages);
      result = await pageUsers({
        usernameLike: appliedKeyword.value,
        status: appliedStatus.value,
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

function goToPage(targetPageNo: number) {
  const nextPage = Math.min(Math.max(targetPageNo, 1), totalPages.value);
  if (nextPage === pageNo.value) {
    return;
  }
  pageNo.value = nextPage;
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

function openCreate() {
  if (!canCreate.value) return;
  dialogMode.value = "create";
  dialogModel.value = {
    id: "",
    username: "",
    nickname: "",
    userType: "INTERNAL",
    status: "ENABLED",
  };
  dialogOpen.value = true;
}

function openEdit(user: UserEntry) {
  if (!canEdit.value) return;
  dialogMode.value = "edit";
  dialogModel.value = { ...user };
  dialogOpen.value = true;
}

async function onSubmit(payload: {
  username: string;
  nickname: string;
  password?: string;
  status: UserStatus;
}) {
  if (dialogMode.value === "create") {
    if (!payload.password) return;
    await createUser({
      username: payload.username,
      nickname: payload.nickname,
      password: payload.password,
    });
  } else if (dialogModel.value) {
    await updateUser(dialogModel.value.id, {
      nickname: payload.nickname,
      status: payload.status,
    });
  }
  dialogOpen.value = false;
  await reload();
}

async function onToggle(user: UserEntry) {
  if (!canToggle.value) return;
  const nextStatus: UserStatus = user.status === "ENABLED" ? "DISABLED" : "ENABLED";
  await updateUser(user.id, { nickname: user.nickname, status: nextStatus });
  await reload();
}

function openReset(user: UserEntry) {
  if (!canReset.value) return;
  resetTarget.value = user;
  resetOpen.value = true;
}

async function onResetSubmit() {
  if (!resetTarget.value) return;
  await resetUserPassword(resetTarget.value.id);
  resetOpen.value = false;
}

async function openRoles(user: UserEntry) {
  if (!canRoles.value) return;
  if (user.userType === "EXTERNAL") return;
  roleTarget.value = user;
  roleOpen.value = true;
  roleLoading.value = true;
  try {
    if (roleList.value.length === 0) {
      roleList.value = await listRoles();
    }
    roleSelected.value = await getUserRoles(user.id);
  } finally {
    roleLoading.value = false;
  }
}

async function onRoleSubmit(roleIds: string[]) {
  if (!roleTarget.value || !canRoleEdit.value) return;
  await updateUserRoles(roleTarget.value.id, roleIds);
  roleOpen.value = false;
}

async function onRemove(user: UserEntry) {
  if (!canDelete.value) return;
  const confirmed = await bzConfirm({
    title: "删除用户",
    message: `确认删除：${user.username} ?`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) return;
  await deleteUser(user.id);
  message.success("删除成功");
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

.admin-toolbar-secondary {
  min-height: 32px !important;
  padding: 0 15px !important;
  border-radius: 8px !important;
  font-size: 13px !important;
  font-weight: 600 !important;
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
