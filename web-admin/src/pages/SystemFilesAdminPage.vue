<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section
          v-if="canAdmin"
          class="list-page-actions"
        >
          <div class="list-page-actions-main">
            <bz-button @click="goRoot">根目录</bz-button>
            <bz-button
              :disabled="breadcrumbs.length === 0"
              @click="goBack"
              >返回上级</bz-button
            >
            <bz-button @click="reload">刷新</bz-button>
            <span
              class="path"
              :title="currentPath"
              >当前路径：{{ currentPath }}</span
            >
          </div>
        </section>

        <bz-card
          v-if="canAdmin"
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
                  class="list-page-filter-control keyword"
                  placeholder="按名称搜索当前目录及子目录"
                  clearable
                  @keyup.enter="applySearch"
                />
              </div>
            </bz-form-item>
            <bz-form-item class="list-page-filter-actions">
              <bz-button
                type="primary"
                @click="applySearch"
                >搜索</bz-button
              >
              <bz-button
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
                    class="sm"
                    @change="reloadAndRefreshDetail"
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
                    class="sm"
                    @change="reloadAndRefreshDetail"
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
                <bz-button-group class="view-switch">
                  <bz-button
                    size="small"
                    :type="viewMode === 'GRID' ? 'primary' : 'default'"
                    @click="viewMode = 'GRID'"
                    >平铺</bz-button
                  >
                  <bz-button
                    size="small"
                    :type="viewMode === 'LIST' ? 'primary' : 'default'"
                    @click="viewMode = 'LIST'"
                    >详细列表</bz-button
                  >
                </bz-button-group>
              </div>
            </bz-form-item>
          </bz-form>
        </bz-card>

        <bz-card
          v-loading="loading"
          class="list-page-result-card board"
          element-loading-text="加载中..."
        >
          <bz-empty
            v-if="!canAdmin"
            description="无权限查看文件管理页面"
          />
          <div
            v-else-if="errorMessage"
            class="state-block"
          >
            <bz-alert
              :title="errorMessage"
              type="error"
              show-icon
            />
            <bz-button
              size="small"
              type="primary"
              @click="reload"
              >重试</bz-button
            >
          </div>
          <bz-empty
            v-else-if="!loading && items.length === 0"
            :description="activeKeyword ? '没有匹配结果' : '暂无数据'"
          />

          <template v-else>
            <bz-alert
              v-if="activeKeyword"
              class="search-tip"
              type="info"
              :closable="false"
              show-icon
              title="搜索中：仅显示当前目录及子目录名称匹配结果"
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
                    link
                    class="title"
                    @click="enterFolder(item)"
                  >
                    📁 {{ item.name }}
                  </bz-button>
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
                  <span class="meta">Owner：{{ item.ownerType }}/{{ item.ownerId }}</span>
                  <span class="meta"
                    >大小：{{ item.type === "FILE" ? formatSize(item.size) : "-" }}</span
                  >
                  <span class="meta"
                    >更新：{{ formatDateTime(item.updatedAt || item.createdAt || "") }}</span
                  >
                </div>

                <div class="action-buttons">
                  <bz-button
                    v-for="action in getPrimaryActions(item)"
                    :key="action.key"
                    size="small"
                    :type="action.type"
                    :disabled="action.disabled"
                    @click="action.handler"
                    >{{ action.label }}
                  </bz-button>
                  <bz-dropdown
                    v-if="getExtraActions(item).length"
                    trigger="click"
                  >
                    <bz-button size="small">更多</bz-button>
                    <template #dropdown>
                      <bz-dropdown-menu>
                        <bz-dropdown-item
                          v-for="action in getExtraActions(item)"
                          :key="action.key"
                          :disabled="action.disabled"
                          @click="action.handler"
                          >{{ action.label }}
                        </bz-dropdown-item>
                      </bz-dropdown-menu>
                    </template>
                  </bz-dropdown>
                </div>
              </bz-card>
            </div>

            <bz-table
              v-else
              :data="items"
              size="small"
              row-key="id"
            >
              <bz-table-column
                label="名称"
                min-width="260"
              >
                <template #default="scope">
                  <bz-button
                    v-if="scope.row.type === 'FOLDER'"
                    link
                    @click="enterFolder(scope.row)"
                    >📁 {{ scope.row.name }}</bz-button
                  >
                  <span v-else>📄 {{ scope.row.name }}</span>
                  <div class="node-id">{{ scope.row.id }}</div>
                </template>
              </bz-table-column>
              <bz-table-column
                label="Owner"
                width="160"
              >
                <template #default="scope">
                  {{ scope.row.ownerType }}/{{ scope.row.ownerId }}
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
                <template #default="scope">
                  {{ formatDateTime(scope.row.updatedAt || scope.row.createdAt || "") }}
                </template>
              </bz-table-column>
              <bz-table-column
                label="操作"
                min-width="220"
              >
                <template #default="scope">
                  <div class="action-buttons">
                    <bz-button
                      v-for="action in getPrimaryActions(scope.row)"
                      :key="action.key"
                      size="small"
                      :type="action.type"
                      :disabled="action.disabled"
                      @click="action.handler"
                      >{{ action.label }}
                    </bz-button>
                    <bz-dropdown
                      v-if="getExtraActions(scope.row).length"
                      trigger="click"
                    >
                      <bz-button size="small">更多</bz-button>
                      <template #dropdown>
                        <bz-dropdown-menu>
                          <bz-dropdown-item
                            v-for="action in getExtraActions(scope.row)"
                            :key="action.key"
                            :disabled="action.disabled"
                            @click="action.handler"
                            >{{ action.label }}
                          </bz-dropdown-item>
                        </bz-dropdown-menu>
                      </template>
                    </bz-dropdown>
                  </div>
                </template>
              </bz-table-column>
            </bz-table>
          </template>
        </bz-card>

        <bz-card
          v-if="previewName"
          class="preview-panel"
        >
          <div class="preview-header">
            <div>
              <div class="preview-title">预览：{{ previewName }}</div>
              <div class="muted">{{ previewContentType || "unknown" }}</div>
            </div>
            <div class="row-actions">
              <bz-button
                v-if="previewIsText"
                size="small"
                @click="copyPreviewText"
                >复制文本</bz-button
              >
              <bz-button
                size="small"
                @click="clearPreview"
                >关闭</bz-button
              >
            </div>
          </div>
          <div class="preview-body">
            <img
              v-if="previewIsImage"
              :src="previewUrl"
              alt="preview"
              class="preview-image"
            />
            <pre
              v-else-if="previewIsText"
              class="preview-text"
              >{{ previewText }}</pre
            >
            <bz-empty
              v-else
              :image-size="80"
              description="该文件类型不支持内嵌预览，请使用下载。"
            />
          </div>
        </bz-card>

        <bz-card
          v-if="physicalDetail"
          class="physical-panel"
        >
          <div class="physical-header">
            <div>
              <div class="preview-title">物理文件详情：{{ physicalDetail.fileName }}</div>
              <div class="muted">逻辑文件：{{ physicalDetail.logicalFileName }}</div>
            </div>
            <bz-button
              size="small"
              @click="clearPhysicalDetail"
              >关闭</bz-button
            >
          </div>

          <div class="physical-grid">
            <div class="pair">
              <span class="label">逻辑文件ID</span
              ><span class="value mono">{{ physicalDetail.logicalFileId }}</span>
            </div>
            <div class="pair">
              <span class="label">物理文件ID</span
              ><span class="value mono">{{ physicalDetail.physicalFileId }}</span>
            </div>
            <div class="pair">
              <span class="label">Owner</span
              ><span class="value"
                >{{ physicalDetail.logicalOwnerType }}/{{ physicalDetail.logicalOwnerId }}</span
              >
            </div>
            <div class="pair">
              <span class="label">Hash</span
              ><span class="value mono">{{ physicalDetail.hash }}</span>
            </div>
            <div class="pair">
              <span class="label">相对路径</span
              ><span class="value mono">{{ physicalDetail.relativePath }}</span>
            </div>
            <div class="pair">
              <span class="label">绝对路径</span
              ><span class="value mono">{{ physicalDetail.absolutePath }}</span>
            </div>
            <div class="pair">
              <span class="label">大小</span
              ><span class="value">{{ formatSize(physicalDetail.fileSize) }}</span>
            </div>
            <div class="pair">
              <span class="label">内容类型</span
              ><span class="value">{{ physicalDetail.contentType || "-" }}</span>
            </div>
            <div class="pair">
              <span class="label">引用数</span
              ><span class="value">{{ physicalDetail.refCount }}</span>
            </div>
          </div>

          <bz-alert
            v-if="detailErrorMessage"
            :title="detailErrorMessage"
            type="error"
            show-icon
            class="detail-error"
          />

          <div class="refs-head">
            <h4>同物理文件逻辑引用</h4>
            <div class="refs-search">
              <bz-input
                v-model="refsKeywordInput"
                placeholder="按名称筛选引用"
                clearable
                @keyup.enter="applyRefsSearch"
              />
              <bz-button
                v-if="canReverse"
                @click="applyRefsSearch"
                >查询</bz-button
              >
              <bz-button
                :disabled="!refsKeyword"
                @click="clearRefsSearch"
                >清空</bz-button
              >
            </div>
          </div>

          <bz-empty
            v-if="!canReverse"
            description="无权限查看反向引用信息"
          />
          <div
            v-else
            v-loading="refsLoading"
            class="refs-body"
            element-loading-text="引用查询中..."
          >
            <bz-alert
              v-if="refsErrorMessage"
              :title="refsErrorMessage"
              type="error"
              show-icon
            />
            <bz-table
              v-else-if="reverseRefs.length > 0"
              :data="reverseRefs"
              size="small"
              row-key="id"
              class="refs-table"
            >
              <bz-table-column
                label="文件"
                min-width="260"
              >
                <template #default="scope">
                  📄 {{ scope.row.name }}
                  <div class="node-id">{{ scope.row.id }}</div>
                </template>
              </bz-table-column>
              <bz-table-column
                label="Owner"
                width="160"
              >
                <template #default="scope">
                  {{ scope.row.ownerType }}/{{ scope.row.ownerId }}
                </template>
              </bz-table-column>
              <bz-table-column
                label="更新时间"
                width="180"
              >
                <template #default="scope">
                  {{ formatDateTime(scope.row.updatedAt || scope.row.createdAt || "") }}
                </template>
              </bz-table-column>
              <bz-table-column
                label="操作"
                min-width="200"
              >
                <template #default="scope">
                  <div class="row-actions">
                    <bz-button
                      v-if="canView"
                      size="small"
                      @click="previewFile(scope.row)"
                      >预览</bz-button
                    >
                    <bz-button
                      v-if="canDownload"
                      size="small"
                      @click="downloadFile(scope.row)"
                      >下载</bz-button
                    >
                    <bz-button
                      size="small"
                      @click="copyId(scope.row.id)"
                      >复制ID</bz-button
                    >
                  </div>
                </template>
              </bz-table-column>
            </bz-table>
            <bz-empty
              v-else-if="!refsLoading"
              description="未查到引用记录"
            />
          </div>
        </bz-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  downloadSystemFile,
  fetchSystemFileView,
  getLogicalFilePhysicalDetail,
  listPhysicalFileLogicalRefs,
  listSystemNodes,
} from "@admin/api/system-files";
import { hasAdminResourceCodeAccess } from "@admin/registry/admin-permissions";
import type {
  PhysicalFileDetail,
  StorageListQuery,
  StorageSortBy,
  StorageSortOrder,
  StorageViewMode,
  SystemFileItem,
} from "@admin/types/file-storage";
import { formatDateTime } from "@shared/utils/formatter";
import { message } from "@shared/utils/message";
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const canAdmin = computed(() => hasAdminResourceCodeAccess("system-file-admin-view"));
const canPhysical = computed(() => hasAdminResourceCodeAccess("system-file-physical-view"));
const canReverse = computed(() => hasAdminResourceCodeAccess("system-file-reference-view"));
const canView = computed(() => hasAdminResourceCodeAccess("system-file-preview"));
const canDownload = computed(() => hasAdminResourceCodeAccess("system-file-download"));

const loading = ref(false);
const errorMessage = ref("");
const items = ref<SystemFileItem[]>([]);
const currentParentId = ref<string | undefined>(undefined);
const breadcrumbs = ref<Array<{ id: string; name: string }>>([]);

const keywordInput = ref("");
const activeKeyword = ref("");
const sortBy = ref<StorageSortBy>("NAME");
const sortOrder = ref<StorageSortOrder>("ASC");
const viewMode = ref<StorageViewMode>("LIST");

const previewUrl = ref("");
const previewName = ref("");
const previewContentType = ref("");
const previewText = ref("");
const previewResolvedType = ref("");

const physicalDetail = ref<PhysicalFileDetail | null>(null);
const detailErrorMessage = ref("");
const reverseRefs = ref<SystemFileItem[]>([]);
const refsLoading = ref(false);
const refsErrorMessage = ref("");
const refsKeywordInput = ref("");
const refsKeyword = ref("");

const previewIsImage = computed(() => previewResolvedType.value.startsWith("image/"));
const previewIsText = computed(() => isTextType(previewResolvedType.value));

type RowActionType = "primary" | "success" | "warning" | "danger" | "info";

interface RowAction {
  key: string;
  label: string;
  type?: RowActionType;
  disabled?: boolean;
  handler: () => void;
}

const PREVIEW_SIZE_LIMIT = 5 * 1024 * 1024;

const currentPath = computed(() => {
  if (breadcrumbs.value.length === 0) {
    return "/";
  }
  return `/${breadcrumbs.value.map((item) => item.name).join("/")}`;
});

onMounted(() => {
  reload();
});

onBeforeUnmount(() => {
  clearPreview();
});

async function reload() {
  clearPreview();
  if (!canAdmin.value) {
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
    items.value = await listSystemNodes(query);
  } catch (err) {
    errorMessage.value = extractErrorMessage(err, "文件列表加载失败");
  } finally {
    loading.value = false;
  }
}

async function reloadAndRefreshDetail() {
  await reload();
  if (physicalDetail.value && canReverse.value) {
    await loadReverseRefs();
  }
}

function applySearch() {
  activeKeyword.value = keywordInput.value.trim();
  void reload();
}

function clearSearch() {
  keywordInput.value = "";
  activeKeyword.value = "";
  void reload();
}

function enterFolder(folder: SystemFileItem) {
  const fromSearch = Boolean(activeKeyword.value.trim());
  if (fromSearch) {
    keywordInput.value = "";
    activeKeyword.value = "";
    breadcrumbs.value = [{ id: folder.id, name: folder.name }];
  } else {
    breadcrumbs.value.push({ id: folder.id, name: folder.name });
  }
  currentParentId.value = folder.id;
  void reload();
}

function goRoot() {
  breadcrumbs.value = [];
  currentParentId.value = undefined;
  keywordInput.value = "";
  activeKeyword.value = "";
  clearPhysicalDetail();
  void reload();
}

function goBack() {
  if (breadcrumbs.value.length === 0) {
    return;
  }
  breadcrumbs.value.pop();
  currentParentId.value =
    breadcrumbs.value.length > 0 ? breadcrumbs.value[breadcrumbs.value.length - 1].id : undefined;
  keywordInput.value = "";
  activeKeyword.value = "";
  void reload();
}

async function previewFile(row: SystemFileItem) {
  if (!canView.value || row.type !== "FILE") {
    return;
  }
  clearPreview();
  const size = row.size ? Number(row.size) : 0;
  if (Number.isFinite(size) && size > PREVIEW_SIZE_LIMIT) {
    message.warning("文件过大，无法预览");
    return;
  }
  try {
    const blob = await fetchSystemFileView(row.id);
    previewName.value = row.name;
    previewContentType.value = row.contentType || blob.type || "";
    previewResolvedType.value = resolvePreviewType(row.name, previewContentType.value);
    if (previewResolvedType.value) {
      previewContentType.value = previewResolvedType.value;
    }
    if (previewResolvedType.value.startsWith("image/")) {
      previewUrl.value = URL.createObjectURL(blob);
      previewText.value = "";
      return;
    }
    if (isTextType(previewResolvedType.value)) {
      previewText.value = await blob.text();
      previewUrl.value = "";
      return;
    }
    previewUrl.value = "";
    previewText.value = "";
  } catch (err) {
    message.error(extractErrorMessage(err, "文件预览失败"));
  }
}

async function downloadFile(row: SystemFileItem) {
  if (!canDownload.value || row.type !== "FILE") {
    return;
  }
  try {
    const result = await downloadSystemFile(row.id, row.name);
    const url = URL.createObjectURL(result.blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = result.fileName || row.name;
    anchor.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    message.error(extractErrorMessage(err, "文件下载失败"));
  }
}

async function loadPhysicalDetail(item: SystemFileItem) {
  if (!canPhysical.value || item.type !== "FILE") {
    return;
  }
  detailErrorMessage.value = "";
  refsErrorMessage.value = "";
  try {
    physicalDetail.value = await getLogicalFilePhysicalDetail(item.id);
    refsKeywordInput.value = "";
    refsKeyword.value = "";
    reverseRefs.value = [];
    if (canReverse.value) {
      await loadReverseRefs();
    }
  } catch (err) {
    detailErrorMessage.value = extractErrorMessage(err, "物理文件信息加载失败");
    physicalDetail.value = null;
  }
}

function clearPhysicalDetail() {
  physicalDetail.value = null;
  detailErrorMessage.value = "";
  refsErrorMessage.value = "";
  refsKeywordInput.value = "";
  refsKeyword.value = "";
  reverseRefs.value = [];
}

function applyRefsSearch() {
  refsKeyword.value = refsKeywordInput.value.trim();
  void loadReverseRefs();
}

function clearRefsSearch() {
  refsKeywordInput.value = "";
  refsKeyword.value = "";
  void loadReverseRefs();
}

async function loadReverseRefs() {
  if (!canReverse.value || !physicalDetail.value) {
    return;
  }
  refsLoading.value = true;
  refsErrorMessage.value = "";
  try {
    reverseRefs.value = await listPhysicalFileLogicalRefs(
      physicalDetail.value.physicalFileId,
      refsKeyword.value,
      sortBy.value,
      sortOrder.value,
    );
  } catch (err) {
    reverseRefs.value = [];
    refsErrorMessage.value = extractErrorMessage(err, "反向引用查询失败");
  } finally {
    refsLoading.value = false;
  }
}

async function copyId(id: string) {
  try {
    await navigator.clipboard.writeText(id);
    message.success("ID已复制");
  } catch (_err) {
    message.warning("复制失败，请手动复制");
  }
}

function clearPreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
  }
  previewUrl.value = "";
  previewName.value = "";
  previewContentType.value = "";
  previewResolvedType.value = "";
  previewText.value = "";
}

async function copyPreviewText() {
  if (!previewText.value) {
    message.warning("暂无可复制的内容");
    return;
  }
  try {
    await navigator.clipboard.writeText(previewText.value);
    message.success("文本已复制");
  } catch (_err) {
    message.error("复制失败，请手动复制");
  }
}

function resolvePreviewType(fileName: string, contentType: string): string {
  if (contentType) {
    return contentType.toLowerCase();
  }
  const extension = extractExtension(fileName);
  if (!extension) {
    return "";
  }
  if (["png", "jpg", "jpeg", "gif", "webp", "bmp", "svg"].includes(extension)) {
    return `image/${extension === "jpg" ? "jpeg" : extension}`;
  }
  if (["json"].includes(extension)) {
    return "application/json";
  }
  if (["xml"].includes(extension)) {
    return "application/xml";
  }
  if (["yaml", "yml"].includes(extension)) {
    return "application/yaml";
  }
  if (["csv"].includes(extension)) {
    return "text/csv";
  }
  if (["md", "txt", "log", "sql"].includes(extension)) {
    return "text/plain";
  }
  return "";
}

function extractExtension(fileName: string): string {
  if (!fileName) {
    return "";
  }
  const index = fileName.lastIndexOf(".");
  if (index < 0 || index === fileName.length - 1) {
    return "";
  }
  return fileName.substring(index + 1).toLowerCase();
}

function isTextType(contentType: string): boolean {
  if (!contentType) {
    return false;
  }
  const normalized = contentType.toLowerCase().split(";")[0].trim();
  if (normalized.startsWith("text/")) {
    return true;
  }
  return ["application/json", "application/xml", "application/yaml", "application/sql"].includes(
    normalized,
  );
}

function formatSize(size?: string | number | null): string {
  const bytes = typeof size === "string" ? Number(size) : size;
  if (!bytes || !Number.isFinite(bytes) || bytes <= 0) {
    return "0 B";
  }
  const units = ["B", "KB", "MB", "GB", "TB"];
  let value = bytes;
  let index = 0;
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024;
    index += 1;
  }
  return `${value.toFixed(index === 0 ? 0 : 2)} ${units[index]}`;
}

function getItemActions(item: SystemFileItem): RowAction[] {
  const actions: RowAction[] = [];
  if (item.type === "FILE") {
    if (canView.value) {
      actions.push({ key: "preview", label: "预览", handler: () => previewFile(item) });
    }
    if (canDownload.value) {
      actions.push({ key: "download", label: "下载", handler: () => downloadFile(item) });
    }
    if (canPhysical.value) {
      actions.push({ key: "physical", label: "物理信息", handler: () => loadPhysicalDetail(item) });
    }
  }
  actions.push({ key: "copy", label: "复制ID", handler: () => copyId(item.id) });
  return actions;
}

function getPrimaryActions(item: SystemFileItem): RowAction[] {
  const actions = getItemActions(item);
  return actions.length <= 3 ? actions : actions.slice(0, 2);
}

function getExtraActions(item: SystemFileItem): RowAction[] {
  const actions = getItemActions(item);
  return actions.length <= 3 ? [] : actions.slice(2);
}

function extractErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return fallback;
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

.keyword {
  width: 280px;
}

.sm {
  width: 120px;
}

.view-switch {
  display: inline-flex;
  gap: 6px;
}

.board {
  overflow: hidden;
}

.search-tip {
  border-bottom: 1px dashed #e2e8f0;
}

.state-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
}

.detail-error {
  margin: 8px 0 12px;
}

.refs-body {
  margin-top: 8px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  padding: 12px;
}

.node-card {
  border: 1px solid #e2e8f0;
}

.node-card :deep(.bz-card__body) {
  padding: 10px;
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

.meta {
  color: var(--text-muted);
  font-size: 12px;
}

.row-actions,
.action-buttons {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-panel,
.physical-panel {
  margin-top: 0;
}

.preview-header,
.physical-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.preview-title {
  font-weight: 700;
}

.preview-image {
  max-width: 100%;
  max-height: 60vh;
  border: 1px solid var(--border-color);
  border-radius: 10px;
}

.preview-text {
  max-height: 60vh;
  overflow: auto;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.muted {
  color: var(--text-muted);
  font-size: 12px;
}

.physical-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.pair {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.label {
  color: #64748b;
  font-size: 12px;
}

.value {
  color: #0f172a;
  font-size: 13px;
}

.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
  word-break: break-all;
}

.refs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.refs-head h4 {
  margin: 0;
  font-size: 14px;
}

.refs-search {
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}

.refs-table {
  margin-top: 8px;
}

@media (max-width: 768px) {
  .content {
    padding: 12px;
  }

  .keyword {
    min-width: 100%;
  }
}
</style>
