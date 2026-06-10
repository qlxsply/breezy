<!-- /src/pages/DataSourceAdminPage.vue -->
<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section class="list-page-actions">
          <div class="list-page-actions-main">
            <bz-button
              type="primary"
              @click="handleAdd"
              >新增</bz-button
            >
            <bz-button
              :loading="testingAll"
              @click="handleBatchTest"
              >测试全部</bz-button
            >
            <bz-button @click="handleImport">导入</bz-button>
            <bz-button @click="handleExport">导出</bz-button>
            <template v-if="selectedRows.length > 0">
              <span
                class="action-divider"
                aria-hidden="true"
              ></span>
              <bz-button
                :loading="copyingSelected"
                @click="handleBatchCopySelected"
                >批量复制</bz-button
              >
              <bz-button
                :loading="testingSelected"
                @click="handleBatchTestSelected"
                >批量测试</bz-button
              >
              <bz-button
                :loading="deletingSelected"
                type="danger"
                @click="handleBatchDelete"
                >批量删除</bz-button
              >
            </template>
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
                <div class="list-page-filter-label">名称</div>
                <bz-input
                  v-model="filters.nameLike"
                  class="list-page-filter-control"
                  placeholder="搜索数据源名称"
                  clearable
                  @keyup.enter="loadData(1)"
                />
              </div>
            </bz-form-item>

            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">类型</div>
                <bz-select
                  v-model="filters.dbType"
                  class="list-page-filter-control"
                  placeholder="请选择"
                  clearable
                  @change="loadData(1)"
                >
                  <bz-option
                    label="MySQL"
                    value="MYSQL"
                  />
                  <bz-option
                    label="PostgreSQL"
                    value="POSTGRESQL"
                  />
                  <bz-option
                    label="Oracle"
                    value="ORACLE"
                  />
                  <bz-option
                    label="SQL Server"
                    value="SQLSERVER"
                  />
                  <bz-option
                    label="H2"
                    value="H2"
                  />
                </bz-select>
              </div>
            </bz-form-item>

            <bz-form-item class="list-page-filter-item sort-field-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">排序规则</div>
                <div class="list-page-sort-group">
                  <bz-select
                    v-model="sortField"
                    class="sort-field-select"
                  >
                    <bz-option
                      label="名称"
                      value="NAME"
                    />
                    <bz-option
                      label="类型"
                      value="DB_TYPE"
                    />
                    <bz-option
                      label="状态"
                      value="STATUS"
                    />
                    <bz-option
                      label="最近测试时间"
                      value="LAST_TEST_TIME"
                    />
                  </bz-select>
                  <bz-select
                    v-model="sortDirection"
                    class="sort-order-select"
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

            <bz-form-item class="list-page-filter-actions">
              <bz-button
                type="primary"
                @click="loadData(1)"
                >搜索</bz-button
              >
              <bz-button @click="resetFilters">重置</bz-button>
            </bz-form-item>
          </bz-form>
        </bz-card>

        <bz-card class="list-page-result-card">
          <bz-table
            ref="tableRef"
            :data="displayRows"
            :loading="loading"
            row-key="id"
            empty-text="暂无数据"
            size="small"
            @selection-change="handleSelectionChange"
          >
            <bz-table-column
              type="selection"
              width="48"
              :selectable="isRowSelectable"
            />
            <bz-table-column
              field="name"
              title="名称"
              :min-width="120"
            />
            <bz-table-column
              field="dbType"
              title="类型"
              :width="110"
            >
              <template #default="scope">
                {{ formatDbType(scope.row.dbType) }}
              </template>
            </bz-table-column>
            <bz-table-column
              field="jdbcUrl"
              title="JDBC URL"
              :min-width="260"
              ellipsis
            />
            <bz-table-column
              field="username"
              title="用户名"
              :width="120"
            />
            <bz-table-column
              field="remarkCustom"
              title="备注"
              :min-width="160"
              ellipsis
            >
              <template #default="scope">
                <span class="cell-text">{{ scope.row.remarkCustom || "-" }}</span>
              </template>
            </bz-table-column>
            <bz-table-column
              title="状态"
              :width="100"
            >
              <template #default="scope">
                <bz-tag
                  v-if="scope.row.status === 'OK'"
                  type="success"
                  >可用</bz-tag
                >
                <bz-tag
                  v-else-if="scope.row.status === 'FAILED'"
                  type="danger"
                  >失败</bz-tag
                >
                <bz-tag
                  v-else
                  type="info"
                  >未测试</bz-tag
                >
              </template>
            </bz-table-column>
            <bz-table-column
              title="最近测试"
              :width="190"
            >
              <template #default="scope">
                <div>{{ formatDateTime(scope.row.lastTestTime) }}</div>
                <div
                  v-if="scope.row.lastError"
                  class="sub"
                >
                  {{ scope.row.lastError }}
                </div>
              </template>
            </bz-table-column>
            <bz-table-column
              title="操作"
              :width="220"
              fixed="right"
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
                  <bz-dropdown v-if="getExtraActions(scope.row).length">
                    <bz-button size="small">更多</bz-button>
                    <template #dropdown>
                      <bz-dropdown-menu>
                        <bz-dropdown-item
                          v-for="action in getExtraActions(scope.row)"
                          :key="action.key"
                          :disabled="action.disabled"
                          @click="action.handler"
                          >{{ action.label }}</bz-dropdown-item
                        >
                      </bz-dropdown-menu>
                    </template>
                  </bz-dropdown>
                </div>
              </template>
            </bz-table-column>
          </bz-table>

          <div class="list-page-pagination">
            <div class="list-page-pagination-summary">
              <span>总计 {{ data.totalElements }} 项</span>
              <span v-if="selectedRows.length > 0">，已选择 {{ selectedRows.length }} 项目</span>
              <span>，共 {{ totalPages }} 页</span>
            </div>
            <bz-pagination
              v-if="data.totalElements > 0"
              :total="data.totalElements"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="filters.pageSize"
              :current-page="filters.pageNo"
              @current-change="changePage"
              @size-change="changePageSize"
            />
          </div>
        </bz-card>
      </div>

      <ConnectionFormDialog
        v-if="dialog.visible"
        :id="dialog.editId"
        @close="dialog.visible = false"
        @saved="onSaved"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import {
  deleteDatabaseSource,
  getDatabaseSourceConnectionInfo,
  pageDatabaseSources,
  testAllDatabaseSources,
  testDatabaseSource,
} from "../api/database-source";
import ConnectionFormDialog from "../components/datasource-admin/ConnectionFormDialog.vue";
import type {
  DatabaseSource,
  DatabaseSourceConnectionInfo,
  DatabaseSourcePageRequest,
  DatabaseType,
} from "../types/database-source";
import type { PageResult } from "../types/page";
import { bzConfirm } from "../utils/confirm";
import { formatDateTime } from "../utils/formatter";
import { message } from "../utils/message";

type SortField = "NAME" | "DB_TYPE" | "STATUS" | "LAST_TEST_TIME";
type SortDirection = "ASC" | "DESC";

interface DatabaseSourcePageFilterState {
  nameLike: string;
  dbType?: DatabaseType;
  pageNo: number;
  pageSize: number;
}

interface RowAction {
  key: string;
  label: string;
  type?: "default" | "primary" | "success" | "warning" | "danger";
  disabled?: boolean;
  handler: () => void;
}

interface TableRef {
  clearSelection: () => void;
}

const loading = ref(false);
const testingAll = ref(false);
const copyingSelected = ref(false);
const testingSelected = ref(false);
const deletingSelected = ref(false);
const tableRef = ref<TableRef | null>(null);
const selectedRows = ref<DatabaseSource[]>([]);

const data = reactive<PageResult<DatabaseSource>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});

const filters = reactive<DatabaseSourcePageFilterState>({
  nameLike: "",
  dbType: undefined,
  pageNo: 1,
  pageSize: 10,
});

const sortField = ref<SortField>("LAST_TEST_TIME");
const sortDirection = ref<SortDirection>("DESC");

const dialog = reactive({
  visible: false,
  editId: null as string | null,
});

const statusSortOrder: Record<DatabaseSource["status"], number> = {
  NEW: 0,
  FAILED: 1,
  OK: 2,
};

const displayRows = computed(() => {
  const rows = [...data.elements];
  const order = sortDirection.value === "ASC" ? 1 : -1;
  rows.sort((left, right) => compareRows(left, right) * order);
  return rows;
});

const totalPages = computed(() => {
  if (filters.pageSize <= 0) {
    return 0;
  }
  return Math.ceil(data.totalElements / filters.pageSize);
});

async function loadData(pageNo?: number) {
  if (pageNo !== undefined) {
    filters.pageNo = pageNo;
  }
  const request: DatabaseSourcePageRequest = {
    nameLike: filters.nameLike.trim() || undefined,
    dbType: filters.dbType,
    page: {
      pageNo: filters.pageNo,
      pageSize: filters.pageSize,
    },
  };
  loading.value = true;
  try {
    const res = await pageDatabaseSources(request);
    Object.assign(data, res);
    clearSelection();
  } finally {
    loading.value = false;
  }
}

function changePage(pageNo: number) {
  filters.pageNo = pageNo;
  void loadData();
}

function changePageSize(pageSize: number) {
  filters.pageSize = pageSize;
  void loadData(1);
}

function resetFilters() {
  filters.nameLike = "";
  filters.dbType = undefined;
  sortField.value = "LAST_TEST_TIME";
  sortDirection.value = "DESC";
  void loadData(1);
}

function clearSelection() {
  selectedRows.value = [];
  tableRef.value?.clearSelection();
}

function handleSelectionChange(rows: Record<string, unknown>[]) {
  selectedRows.value = rows as unknown as DatabaseSource[];
}

function isRowSelectable(row: Record<string, unknown>): boolean {
  return row.sourceType !== "APP";
}

function compareRows(left: DatabaseSource, right: DatabaseSource): number {
  switch (sortField.value) {
    case "NAME":
      return compareText(left.name, right.name);
    case "DB_TYPE":
      return compareText(formatDbType(left.dbType), formatDbType(right.dbType));
    case "STATUS":
      return statusSortOrder[left.status] - statusSortOrder[right.status];
    case "LAST_TEST_TIME":
      return toTime(left.lastTestTime) - toTime(right.lastTestTime);
    default:
      return 0;
  }
}

function compareText(left: string, right: string): number {
  return left.localeCompare(right, "zh-CN", { sensitivity: "base" });
}

function toTime(value?: string): number {
  if (!value) {
    return 0;
  }
  const time = Date.parse(value);
  return Number.isNaN(time) ? 0 : time;
}

function formatDbType(type: DatabaseType): string {
  return {
    H2: "H2",
    MYSQL: "MySQL",
    POSTGRESQL: "PostgreSQL",
    ORACLE: "Oracle",
    SQLSERVER: "SQL Server",
  }[type];
}

function handleAdd() {
  dialog.editId = null;
  dialog.visible = true;
}

async function handleBatchTest() {
  if (testingAll.value) {
    return;
  }
  testingAll.value = true;
  try {
    await testAllDatabaseSources();
    message.success("测试完成");
    await loadData(filters.pageNo);
  } catch (_error) {
    message.warning("测试失败，请稍后重试");
  } finally {
    testingAll.value = false;
  }
}

async function handleBatchTestSelected() {
  if (testingSelected.value) {
    return;
  }
  if (selectedRows.value.length === 0) {
    message.warning("请先选择需要测试的数据源");
    return;
  }
  testingSelected.value = true;
  try {
    const results = await Promise.allSettled(
      selectedRows.value.map((row) => testDatabaseSource(row.id)),
    );
    const successCount = results.filter((item) => item.status === "fulfilled").length;
    const failedCount = results.length - successCount;
    if (failedCount === 0) {
      message.success(`批量测试完成，共 ${successCount} 项`);
    } else {
      message.warning(`批量测试完成：成功 ${successCount} 项，失败 ${failedCount} 项`);
    }
    await loadData(filters.pageNo);
  } finally {
    testingSelected.value = false;
  }
}

async function handleBatchCopySelected() {
  if (copyingSelected.value) {
    return;
  }
  if (selectedRows.value.length === 0) {
    message.warning("请先选择需要复制的数据源");
    return;
  }
  copyingSelected.value = true;
  try {
    const infos = await Promise.all(
      selectedRows.value.map((row) => getDatabaseSourceConnectionInfo(row.id)),
    );
    const content = infos
      .map((info) => buildDatabaseSourceConnectionText(info))
      .join("\n------------------------------\n");
    await navigator.clipboard.writeText(content);
    message.success(`已复制 ${infos.length} 个数据源连接信息`);
  } catch (_error) {
    message.warning("批量复制失败，请检查权限或稍后重试");
  } finally {
    copyingSelected.value = false;
  }
}

async function handleBatchDelete() {
  if (deletingSelected.value) {
    return;
  }
  if (selectedRows.value.length === 0) {
    message.warning("请先选择需要删除的数据源");
    return;
  }
  const confirmed = await bzConfirm({
    title: "批量删除",
    message: `确定删除已选择的 ${selectedRows.value.length} 个数据源吗？已接管数据库会变为未配置状态。`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  deletingSelected.value = true;
  try {
    const results = await Promise.allSettled(
      selectedRows.value.map((row) => deleteDatabaseSource(row.id)),
    );
    const successCount = results.filter((item) => item.status === "fulfilled").length;
    const failedCount = results.length - successCount;
    if (successCount > 0) {
      message.success(`已删除 ${successCount} 项`);
    }
    if (failedCount > 0) {
      message.warning(`${failedCount} 项删除失败，请稍后重试`);
    }
    await loadData(filters.pageNo);
  } finally {
    deletingSelected.value = false;
  }
}

function handleImport() {
  message.info("功能暂未实现");
}

function handleExport() {
  message.info("功能暂未实现");
}

function handleEdit(row: DatabaseSource) {
  dialog.editId = row.id;
  dialog.visible = true;
}

async function handleDelete(row: DatabaseSource) {
  const confirmed = await bzConfirm({
    title: "删除数据源",
    message: `确定要删除数据源 "${row.name}" 吗？已接管的数据库将变为未配置状态，需要重新绑定。`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  await deleteDatabaseSource(row.id);
  message.success("删除成功");
  await loadData(filters.pageNo);
}

async function handleTest(row: DatabaseSource) {
  try {
    message.info("正在测试连接...");
    await testDatabaseSource(row.id);
    message.success("连接成功");
    await loadData(filters.pageNo);
  } catch (_error) {}
}

function formatAuthMode(mode?: string | null): string {
  if (mode === "PASSWORD") {
    return "用户名 + 密码";
  }
  return mode || "-";
}

function formatConnectMode(mode?: string | null): string {
  if (mode === "HOST_PORT") {
    return "主机 + 端口";
  }
  if (mode === "SERVICE_NAME") {
    return "服务名";
  }
  if (mode === "SID") {
    return "SID";
  }
  if (mode === "TNS") {
    return "TNS";
  }
  return mode || "-";
}

function buildDatabaseSourceConnectionText(info: DatabaseSourceConnectionInfo): string {
  const jdbcUrl = info.jdbcUrl?.trim() || "-";
  const host = info.host?.trim() || "-";
  const port = info.port !== null && info.port !== undefined ? String(info.port) : "-";
  const databaseName = info.databaseName?.trim() || "-";
  const serviceName = info.serviceName?.trim() || "-";
  const sid = info.sid?.trim() || "-";
  const authMode = formatAuthMode(info.authMode);
  const connectMode = formatConnectMode(info.connectMode);
  const driverClassName = info.driverClassName?.trim() || "-";
  const username = info.username?.trim() || "-";
  const password = info.passwordRaw?.trim() || "-";
  const remark = info.remarkCustom?.trim() || "-";
  const dbTypeName = formatDbType(info.dbType) || info.dbType || "-";

  const lines = [
    `数据源：${info.name || "-"}`,
    `类型：${dbTypeName}`,
    `认证方式：${authMode}`,
    `连接类型：${connectMode}`,
    `驱动程序：${driverClassName}`,
    `主机：${host}`,
    `端口：${port}`,
  ];

  if (info.connectMode === "SERVICE_NAME") {
    lines.push(`服务名：${serviceName}`);
  } else if (info.connectMode === "SID") {
    lines.push(`SID：${sid}`);
  } else {
    lines.push(`数据库：${databaseName}`);
  }

  lines.push(`用户名：${username}`, `密码：${password}`, `JDBC URL：${jdbcUrl}`, `备注：${remark}`);

  return lines.join("\n");
}

async function handleCopy(row: DatabaseSource) {
  try {
    const info = await getDatabaseSourceConnectionInfo(row.id);
    const content = buildDatabaseSourceConnectionText(info);
    await navigator.clipboard.writeText(content);
    message.success(`已复制「${info.name || row.name}」连接信息`);
  } catch (_error) {
    message.warning("复制失败，请检查权限或稍后重试");
  }
}

function onSaved() {
  dialog.visible = false;
  void loadData(filters.pageNo);
}

function getRowActions(row: DatabaseSource): RowAction[] {
  const disabled = row.sourceType === "APP";
  return [
    {
      key: "edit",
      label: "编辑",
      handler: () => handleEdit(row),
    },
    {
      key: "copy",
      label: "复制",
      handler: () => handleCopy(row),
    },
    {
      key: "test",
      label: "测试",
      disabled,
      handler: () => handleTest(row),
    },
    {
      key: "delete",
      label: "删除",
      type: "danger",
      disabled,
      handler: () => handleDelete(row),
    },
  ];
}

function getPrimaryActions(row: DatabaseSource): RowAction[] {
  const actions = getRowActions(row);
  if (actions.length <= 3) {
    return actions;
  }
  return actions.slice(0, 2);
}

function getExtraActions(row: DatabaseSource): RowAction[] {
  const actions = getRowActions(row);
  if (actions.length <= 3) {
    return [];
  }
  return actions.slice(2);
}

onMounted(() => {
  void loadData();
});
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

.sort-field-item {
  min-width: 320px;
}

.sort-field-select {
  width: 160px;
}

.sort-order-select {
  width: 120px;
}

.action-divider {
  width: 1px;
  height: 20px;
  background: var(--border-color);
}

.sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
  word-break: break-all;
}

@media (max-width: 768px) {
  .content {
    padding: 12px;
  }

  .sort-field-item,
  .sort-field-select,
  .sort-order-select {
    width: 100%;
    min-width: 0;
  }
}
</style>
