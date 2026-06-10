<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section
          v-if="canView"
          class="list-page-actions"
        >
          <div class="list-page-actions-main">
            <bz-button
              size="small"
              @click="goRoot"
              >根目录</bz-button
            >
            <bz-button
              size="small"
              :disabled="breadcrumbs.length === 0"
              @click="goBack"
              >返回上级</bz-button
            >
            <bz-button
              v-if="canCreateFolder"
              size="small"
              type="primary"
              @click="createFolder"
              >新建目录</bz-button
            >
            <bz-button
              v-if="canUpload"
              size="small"
              type="primary"
              @click="triggerUpload"
              >上传文件</bz-button
            >
            <bz-button
              size="small"
              @click="reload"
              >刷新</bz-button
            >
            <span
              class="path"
              :title="currentPath"
              >当前路径：{{ currentPath }}</span
            >
            <input
              ref="fileInputRef"
              class="hidden"
              type="file"
              @change="onFilePicked"
            />
          </div>
        </section>

        <bz-card
          v-if="canView"
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
                <div class="list-page-filter-label">搜索</div>
                <bz-input
                  v-model="keywordInput"
                  class="list-page-filter-control"
                  placeholder="按名称搜索当前目录及子目录"
                  size="small"
                  clearable
                  @keyup.enter="applySearch"
                />
              </div>
            </bz-form-item>
            <bz-form-item class="list-page-filter-actions">
              <bz-button
                size="small"
                type="primary"
                @click="applySearch"
                >搜索</bz-button
              >
              <bz-button
                size="small"
                :disabled="!activeKeyword"
                @click="clearSearch"
                >清空</bz-button
              >
            </bz-form-item>
            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">排序</div>
                <div class="list-page-sort-group">
                  <bz-select
                    v-model="sortBy"
                    class="sort-select"
                    size="small"
                    @change="reload"
                  >
                    <bz-option
                      label="名称"
                      value="NAME"
                    />
                    <bz-option
                      label="大小"
                      value="SIZE"
                    />
                    <bz-option
                      label="类型"
                      value="TYPE"
                    />
                    <bz-option
                      label="最新修改"
                      value="UPDATED_AT"
                    />
                  </bz-select>
                  <bz-select
                    v-model="sortOrder"
                    class="sort-select sort-order"
                    size="small"
                    @change="reload"
                  >
                    <bz-option
                      label="升序"
                      value="ASC"
                    />
                    <bz-option
                      label="降序"
                      value="DESC"
                    />
                  </bz-select>
                </div>
              </div>
            </bz-form-item>
            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">视图</div>
                <bz-radio-group
                  v-model="viewMode"
                  size="small"
                >
                  <bz-radio-button label="GRID">平铺</bz-radio-button>
                  <bz-radio-button label="LIST">详细列表</bz-radio-button>
                </bz-radio-group>
              </div>
            </bz-form-item>
          </bz-form>
        </bz-card>

        <bz-card
          v-loading="loading"
          class="list-page-result-card board"
        >
          <bz-empty
            v-if="!canView"
            description="无权限查看个人存储"
          />
          <bz-alert
            v-else-if="errorMessage"
            type="error"
            :title="errorMessage"
            show-icon
          >
            <template #default>
              <bz-button
                size="small"
                type="primary"
                @click="reload"
                >重试</bz-button
              >
            </template>
          </bz-alert>
          <bz-empty
            v-else-if="items.length === 0"
            :description="activeKeyword ? '没有匹配结果' : '当前目录为空'"
          />

          <template v-else>
            <bz-alert
              v-if="activeKeyword"
              type="info"
              show-icon
              :title="'搜索中：仅显示当前目录及子目录名称匹配结果'"
              class="search-tip"
            />

            <div
              v-if="viewMode === 'GRID'"
              class="grid"
            >
              <bz-card
                v-for="item in items"
                :key="item.id"
                class="node-card"
                shadow="never"
              >
                <div class="node-header">
                  <bz-button
                    v-if="item.type === 'FOLDER'"
                    type="primary"
                    link
                    @click="enterFolder(item)"
                    >📁 {{ item.name }}</bz-button
                  >
                  <div
                    v-else
                    class="title"
                  >
                    📄 {{ item.name }}
                  </div>
                  <div
                    class="node-id"
                    :title="item.id"
                  >
                    {{ item.id }}
                  </div>
                </div>

                <div class="node-meta">
                  <bz-tag size="small">{{ item.type === "FOLDER" ? "目录" : "文件" }}</bz-tag>
                  <span class="meta"
                    >大小：{{ item.type === "FILE" ? formatSize(item.size) : "-" }}</span
                  >
                  <span class="meta"
                    >更新：{{ formatDateTime(item.updatedAt || item.createdAt || "") }}</span
                  >
                </div>

                <div class="row-actions">
                  <bz-button
                    v-if="item.type === 'FOLDER' && canRenameFolder"
                    size="small"
                    @click="renameFolder(item)"
                    >重命名</bz-button
                  >
                  <bz-button
                    v-if="item.type === 'FILE' && canRenameFile"
                    size="small"
                    @click="renameFile(item)"
                    >重命名</bz-button
                  >
                  <bz-button
                    v-if="canMoveNode(item)"
                    size="small"
                    @click="startMove(item)"
                    >移动</bz-button
                  >
                  <bz-button
                    v-if="item.type === 'FOLDER' && canDeleteFolder"
                    size="small"
                    type="danger"
                    @click="removeFolder(item)"
                    >删除目录</bz-button
                  >
                  <bz-button
                    v-if="item.type === 'FILE' && canDeleteFile"
                    size="small"
                    type="danger"
                    @click="removeFile(item)"
                    >删除文件</bz-button
                  >
                </div>
              </bz-card>
            </div>

            <bz-table
              v-else
              :data="items"
              size="small"
              class="table"
            >
              <bz-table-column
                label="名称"
                min-width="320"
              >
                <template #default="scope">
                  <bz-button
                    v-if="scope.row.type === 'FOLDER'"
                    type="primary"
                    link
                    @click="enterFolder(scope.row)"
                    >📁 {{ scope.row.name }}</bz-button
                  >
                  <span v-else>📄 {{ scope.row.name }}</span>
                  <div class="node-id">{{ scope.row.id }}</div>
                </template>
              </bz-table-column>
              <bz-table-column
                label="类型"
                width="120"
              >
                <template #default="scope">
                  <bz-tag size="small">{{ scope.row.type === "FOLDER" ? "目录" : "文件" }}</bz-tag>
                </template>
              </bz-table-column>
              <bz-table-column
                label="大小"
                width="120"
              >
                <template #default="scope">
                  {{ scope.row.type === "FILE" ? formatSize(scope.row.size) : "-" }}
                </template>
              </bz-table-column>
              <bz-table-column
                label="更新时间"
                width="180"
              >
                <template #default="scope">{{
                  formatDateTime(scope.row.updatedAt || scope.row.createdAt || "")
                }}</template>
              </bz-table-column>
              <bz-table-column
                label="操作"
                min-width="260"
              >
                <template #default="scope">
                  <div class="row-actions">
                    <bz-button
                      v-if="scope.row.type === 'FOLDER' && canRenameFolder"
                      size="small"
                      @click="renameFolder(scope.row)"
                      >重命名</bz-button
                    >
                    <bz-button
                      v-if="scope.row.type === 'FILE' && canRenameFile"
                      size="small"
                      @click="renameFile(scope.row)"
                      >重命名</bz-button
                    >
                    <bz-button
                      v-if="canMoveNode(scope.row)"
                      size="small"
                      @click="startMove(scope.row)"
                      >移动</bz-button
                    >
                    <bz-button
                      v-if="scope.row.type === 'FOLDER' && canDeleteFolder"
                      size="small"
                      type="danger"
                      @click="removeFolder(scope.row)"
                      >删除目录</bz-button
                    >
                    <bz-button
                      v-if="scope.row.type === 'FILE' && canDeleteFile"
                      size="small"
                      type="danger"
                      @click="removeFile(scope.row)"
                      >删除文件</bz-button
                    >
                  </div>
                </template>
              </bz-table-column>
            </bz-table>
          </template>
        </bz-card>
      </div>
    </div>

    <FolderTargetPickerDialog
      v-model="moveTargetValue"
      :visible="moveDialogVisible"
      title="选择目标目录"
      :sub-title="
        movingItem ? `${movingItem.type === 'FOLDER' ? '目录' : '文件'}：${movingItem.name}` : ''
      "
      :loading="moveLoading"
      :submitting="moveSubmitting"
      :error-message="moveErrorMessage"
      :targets="moveTargets"
      confirm-text="确认移动"
      @close="closeMoveDialog"
      @confirm="confirmMove"
    />

    <bz-dialog
      v-model="nameDialog.visible"
      :title="nameDialog.title"
      width="480px"
      @close="closeNameDialog"
    >
      <bz-form @submit.prevent>
        <bz-form-item
          label="名称"
          label-width="70px"
        >
          <bz-input
            v-model="nameDialog.value"
            :placeholder="nameDialog.placeholder"
            @keyup="onNameDialogKeyup"
          />
        </bz-form-item>
      </bz-form>
      <template #footer>
        <bz-button @click="closeNameDialog">取消</bz-button>
        <bz-button
          type="primary"
          :loading="nameDialog.submitting"
          @click="submitNameDialog"
          >确定</bz-button
        >
      </template>
    </bz-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import {
  createStorageFolder,
  deleteStorageFile,
  deleteStorageFolder,
  listStorageItems,
  moveStorageFile,
  moveStorageFolder,
  renameStorageFile,
  renameStorageFolder,
  uploadStorageFile,
} from "../api/storage";
import FolderTargetPickerDialog, {
  type FolderTargetOption,
} from "../components/common/FolderTargetPickerDialog.vue";
import { hasUserPermissionCode } from "../registry/user-tool-permissions.registry";
import type {
  StorageItem,
  StorageListQuery,
  StorageSortBy,
  StorageSortOrder,
  StorageViewMode,
} from "../types/file-storage";
import { bzConfirm } from "../utils/confirm";
import { formatDateTime } from "../utils/formatter";
import { message } from "../utils/message";

type FolderOption = {
  id: string;
  name: string;
  parentId?: string;
  depth: number;
};

type NameDialogMode = "CREATE_FOLDER" | "RENAME_FOLDER" | "RENAME_FILE";

const ROOT_TARGET_VALUE = "__ROOT__";

const loading = ref(false);
const errorMessage = ref("");
const items = ref<StorageItem[]>([]);
const currentParentId = ref<string | undefined>(undefined);
const breadcrumbs = ref<Array<{ id: string; name: string }>>([]);
const fileInputRef = ref<HTMLInputElement | null>(null);

const keywordInput = ref("");
const activeKeyword = ref("");
const sortBy = ref<StorageSortBy>("NAME");
const sortOrder = ref<StorageSortOrder>("ASC");
const viewMode = ref<StorageViewMode>("LIST");

const moveDialogVisible = ref(false);
const movingItem = ref<StorageItem | null>(null);
const moveLoading = ref(false);
const moveSubmitting = ref(false);
const moveErrorMessage = ref("");
const moveTargetValue = ref(ROOT_TARGET_VALUE);
const folderOptions = ref<FolderOption[]>([]);
const forbiddenTargetIds = ref<Set<string>>(new Set());

const nameDialog = reactive({
  visible: false,
  mode: "CREATE_FOLDER" as NameDialogMode,
  title: "",
  placeholder: "",
  targetId: "",
  value: "",
  submitting: false,
});

const canView = computed(() => hasUserPermissionCode("stg.view"));
const canCreateFolder = computed(() => hasUserPermissionCode("stg.dir.add"));
const canRenameFolder = computed(() => hasUserPermissionCode("stg.dir.rename"));
const canDeleteFolder = computed(() => hasUserPermissionCode("stg.dir.del"));
const canUpload = computed(() => hasUserPermissionCode("stg.file.upload"));
const canRenameFile = computed(() => hasUserPermissionCode("stg.file.rename"));
const canDeleteFile = computed(() => hasUserPermissionCode("stg.file.del"));
const canMoveFolder = computed(() => hasUserPermissionCode("stg.dir.move"));
const canMoveFile = computed(() => hasUserPermissionCode("stg.file.move"));

const currentPath = computed(() => {
  if (breadcrumbs.value.length === 0) return "/";
  return `/${breadcrumbs.value.map((b) => b.name).join("/")}`;
});

const moveTargets = computed<FolderTargetOption[]>(() => {
  const targets: FolderTargetOption[] = [
    {
      value: ROOT_TARGET_VALUE,
      label: "/",
      depth: 0,
      disabled: false,
    },
  ];

  for (const option of folderOptions.value) {
    targets.push({
      value: option.id,
      label: option.name,
      depth: option.depth + 1,
      disabled: forbiddenTargetIds.value.has(option.id),
    });
  }
  return targets;
});

onMounted(() => {
  reload();
});

async function reload() {
  if (!canView.value) {
    items.value = [];
    return;
  }
  loading.value = true;
  errorMessage.value = "";
  try {
    const query: StorageListQuery = {
      parentId: currentParentId.value,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value,
    };
    if (activeKeyword.value.trim()) {
      query.keyword = activeKeyword.value.trim();
      query.recursive = true;
    }
    items.value = await listStorageItems(query);
  } catch (err) {
    errorMessage.value = extractErrorMessage(err, "加载目录失败");
  } finally {
    loading.value = false;
  }
}

function applySearch() {
  activeKeyword.value = keywordInput.value.trim();
  reload();
}

function clearSearch() {
  keywordInput.value = "";
  activeKeyword.value = "";
  reload();
}

function enterFolder(folder: StorageItem) {
  const fromSearch = !!activeKeyword.value.trim();
  if (fromSearch) {
    keywordInput.value = "";
    activeKeyword.value = "";
    breadcrumbs.value = [{ id: folder.id, name: folder.name }];
  } else {
    breadcrumbs.value.push({ id: folder.id, name: folder.name });
  }
  currentParentId.value = folder.id;
  reload();
}

function goRoot() {
  breadcrumbs.value = [];
  currentParentId.value = undefined;
  keywordInput.value = "";
  activeKeyword.value = "";
  reload();
}

function goBack() {
  if (breadcrumbs.value.length === 0) return;
  breadcrumbs.value.pop();
  currentParentId.value =
    breadcrumbs.value.length > 0 ? breadcrumbs.value[breadcrumbs.value.length - 1].id : undefined;
  keywordInput.value = "";
  activeKeyword.value = "";
  reload();
}

function canMoveNode(item: StorageItem): boolean {
  return item.type === "FOLDER" ? canMoveFolder.value : canMoveFile.value;
}

async function startMove(item: StorageItem) {
  if (!canMoveNode(item)) return;
  movingItem.value = item;
  moveDialogVisible.value = true;
  moveTargetValue.value = currentParentId.value || ROOT_TARGET_VALUE;
  await loadMoveTargets(item);
}

function closeMoveDialog() {
  moveDialogVisible.value = false;
  moveSubmitting.value = false;
  moveLoading.value = false;
  movingItem.value = null;
  moveErrorMessage.value = "";
  folderOptions.value = [];
  forbiddenTargetIds.value = new Set();
  moveTargetValue.value = ROOT_TARGET_VALUE;
}

async function loadMoveTargets(item: StorageItem) {
  moveLoading.value = true;
  moveErrorMessage.value = "";
  try {
    const allNodes = await listStorageItems({ recursive: true, sortBy: "NAME", sortOrder: "ASC" });
    const allFolders = allNodes.filter((node) => node.type === "FOLDER");
    folderOptions.value = flattenFolderOptions(allFolders);
    if (item.type === "FOLDER") {
      forbiddenTargetIds.value = collectForbiddenFolderIds(item.id, allFolders);
    } else {
      forbiddenTargetIds.value = new Set();
    }
  } catch (err) {
    moveErrorMessage.value = extractErrorMessage(err, "加载目录树失败");
  } finally {
    moveLoading.value = false;
  }
}

async function confirmMove() {
  if (!movingItem.value) return;
  const targetId = moveTargetValue.value === ROOT_TARGET_VALUE ? undefined : moveTargetValue.value;
  if (targetId && forbiddenTargetIds.value.has(targetId)) {
    message.warning("目标目录不可选");
    return;
  }

  moveSubmitting.value = true;
  try {
    if (movingItem.value.type === "FOLDER") {
      await moveStorageFolder(movingItem.value.id, targetId);
    } else {
      await moveStorageFile(movingItem.value.id, targetId);
    }
    message.success("移动成功");
    closeMoveDialog();
    await reload();
  } catch (err) {
    message.error(extractErrorMessage(err, "移动失败"));
  } finally {
    moveSubmitting.value = false;
  }
}

async function createFolder() {
  if (!canCreateFolder.value) return;
  openNameDialog("CREATE_FOLDER", "新建目录", "请输入目录名称", "");
}

function renameFolder(folder: StorageItem) {
  if (!canRenameFolder.value) return;
  openNameDialog("RENAME_FOLDER", "目录重命名", "请输入新目录名称", folder.name, folder.id);
}

function renameFile(file: StorageItem) {
  if (!canRenameFile.value) return;
  openNameDialog("RENAME_FILE", "文件重命名", "请输入新文件名称", file.name, file.id);
}

function openNameDialog(
  mode: NameDialogMode,
  title: string,
  placeholder: string,
  value: string,
  targetId = "",
) {
  nameDialog.visible = true;
  nameDialog.mode = mode;
  nameDialog.title = title;
  nameDialog.placeholder = placeholder;
  nameDialog.targetId = targetId;
  nameDialog.value = value;
  nameDialog.submitting = false;
}

function closeNameDialog() {
  nameDialog.visible = false;
  nameDialog.mode = "CREATE_FOLDER";
  nameDialog.title = "";
  nameDialog.placeholder = "";
  nameDialog.targetId = "";
  nameDialog.value = "";
  nameDialog.submitting = false;
}

async function submitNameDialog() {
  if (nameDialog.submitting) {
    return;
  }
  const nextValue = nameDialog.value.trim();
  if (!nextValue) {
    message.warning("名称不能为空");
    return;
  }

  nameDialog.submitting = true;
  try {
    if (nameDialog.mode === "CREATE_FOLDER") {
      await createStorageFolder({
        parentId: currentParentId.value,
        name: nextValue,
      });
      message.success("目录创建成功");
    } else if (nameDialog.mode === "RENAME_FOLDER") {
      await renameStorageFolder(nameDialog.targetId, { newName: nextValue });
      message.success("目录重命名成功");
    } else {
      await renameStorageFile(nameDialog.targetId, { newName: nextValue });
      message.success("文件重命名成功");
    }
    closeNameDialog();
    await reload();
  } catch (err) {
    if (nameDialog.mode === "CREATE_FOLDER") {
      message.error(extractErrorMessage(err, "目录创建失败"));
    } else if (nameDialog.mode === "RENAME_FOLDER") {
      message.error(extractErrorMessage(err, "目录重命名失败"));
    } else {
      message.error(extractErrorMessage(err, "文件重命名失败"));
    }
    nameDialog.submitting = false;
  }
}

function onNameDialogKeyup(event: KeyboardEvent) {
  if (event.key === "Enter") {
    void submitNameDialog();
  }
}

async function removeFolder(folder: StorageItem) {
  if (!canDeleteFolder.value) return;
  const confirmed = await bzConfirm({
    title: "删除目录",
    message: `确认删除目录 "${folder.name}" 吗？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }

  try {
    await deleteStorageFolder(folder.id, false);
    message.success("目录已删除");
  } catch (_err) {
    const recursiveConfirmed = await bzConfirm({
      title: "递归删除",
      message: "目录可能非空，是否递归删除目录及其全部内容？",
      confirmText: "递归删除",
      cancelText: "取消",
      danger: true,
    });
    if (!recursiveConfirmed) {
      return;
    }
    await deleteStorageFolder(folder.id, true);
    message.success("目录已递归删除");
  }
  await reload();
}

async function removeFile(file: StorageItem) {
  if (!canDeleteFile.value) return;
  const confirmed = await bzConfirm({
    title: "删除文件",
    message: `确认删除文件 "${file.name}" 吗？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  try {
    await deleteStorageFile(file.id);
    message.success("文件已删除");
    await reload();
  } catch (err) {
    message.error(extractErrorMessage(err, "文件删除失败"));
  }
}

function triggerUpload() {
  if (!canUpload.value) return;
  fileInputRef.value?.click();
}

async function onFilePicked(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  try {
    await uploadStorageFile(file, currentParentId.value);
    message.success("文件上传成功");
    await reload();
  } catch (err) {
    message.error(extractErrorMessage(err, "文件上传失败"));
  } finally {
    input.value = "";
  }
}

function flattenFolderOptions(folders: StorageItem[]): FolderOption[] {
  const folderMap = new Map<string, StorageItem>();
  const childrenMap = new Map<string | undefined, StorageItem[]>();

  for (const folder of folders) {
    folderMap.set(folder.id, folder);
  }

  for (const folder of folders) {
    let parentId = normalizeParentId(folder.parentId);
    if (parentId && !folderMap.has(parentId)) {
      parentId = undefined;
    }
    const list = childrenMap.get(parentId) || [];
    list.push(folder);
    childrenMap.set(parentId, list);
  }

  for (const [, list] of childrenMap) {
    list.sort((a, b) => a.name.localeCompare(b.name, "zh-CN"));
  }

  const result: FolderOption[] = [];
  const walk = (parentId: string | undefined, depth: number) => {
    const children = childrenMap.get(parentId) || [];
    for (const child of children) {
      result.push({
        id: child.id,
        name: child.name,
        parentId: normalizeParentId(child.parentId),
        depth,
      });
      walk(child.id, depth + 1);
    }
  };
  walk(undefined, 0);
  return result;
}

function collectForbiddenFolderIds(folderId: string, folders: StorageItem[]): Set<string> {
  const childrenMap = new Map<string, string[]>();
  for (const folder of folders) {
    const parentId = normalizeParentId(folder.parentId);
    if (!parentId) continue;
    const list = childrenMap.get(parentId) || [];
    list.push(folder.id);
    childrenMap.set(parentId, list);
  }

  const forbidden = new Set<string>([folderId]);
  const queue: string[] = [folderId];
  while (queue.length > 0) {
    const current = queue.shift();
    if (!current) continue;
    const children = childrenMap.get(current) || [];
    for (const childId of children) {
      if (forbidden.has(childId)) continue;
      forbidden.add(childId);
      queue.push(childId);
    }
  }
  return forbidden;
}

function normalizeParentId(parentId?: string | null): string | undefined {
  return parentId || undefined;
}

function extractErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return fallback;
}

function formatSize(size?: string | number | null): string {
  const bytes = typeof size === "string" ? Number(size) : size;
  if (!bytes || !Number.isFinite(bytes) || bytes <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  let value = bytes;
  let index = 0;
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024;
    index += 1;
  }
  return `${value.toFixed(index === 0 ? 0 : 2)} ${units[index]}`;
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
  width: 100%;
  padding: 16px 24px;
  box-sizing: border-box;
}

.path {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
  color: var(--text-muted);
  font-size: 13px;
  margin-left: 4px;
  max-width: min(560px, 100%);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sort-select {
  width: 130px;
}

.sort-order {
  width: 100px;
}

.board {
  overflow: hidden;
}

.search-tip {
  border-bottom: 1px dashed #e2e8f0;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
  padding: 12px;
}

.node-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px;
  background: #fff;
}

.node-header {
  margin-bottom: 8px;
}

.title {
  font-weight: 700;
  line-height: 1.4;
}

.node-id {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
  word-break: break-all;
}

.node-meta {
  display: grid;
  gap: 4px;
  margin-bottom: 8px;
}

.node-meta .tag {
  justify-self: start;
}

.meta {
  color: var(--text-muted);
  font-size: 12px;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hidden {
  display: none;
}

@media (max-width: 768px) {
  .content {
    padding: 12px;
  }
}
</style>
