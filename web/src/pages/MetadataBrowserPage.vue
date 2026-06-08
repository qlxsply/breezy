<!-- /src/pages/MetadataBrowserPage.vue -->
<template>
  <div class="metadata-browser">
    <!-- 顶部导航 -->
    <div class="browser-header">
      <div class="breadcrumb">
        <router-link
          to="/database-schemas"
          class="link"
          >数据库管理</router-link
        >
        <span class="sep">/</span>
        <span class="current">{{ dbInfo?.alias || dbInfo?.databaseName || "加载中..." }}</span>
      </div>
      <div class="db-actions">
        <bz-button
          size="small"
          :disabled="dataSourceMissing"
          @click="handleRefresh"
          >刷新全量元数据</bz-button
        >
      </div>
    </div>

    <div class="browser-main">
      <bz-empty
        v-if="dataSourceMissing"
        description="未配置数据源，请先在数据源管理中重新绑定后再使用。"
      />
      <template v-else>
        <!-- 左侧：表列表 -->
        <div class="side-nav">
          <div class="nav-search">
            <bz-input
              v-model="tableSearch"
              size="small"
              placeholder="搜索表名或别名…"
              clearable
            />
          </div>
          <div
            v-if="!loadingTables"
            class="nav-list"
          >
            <div
              v-for="table in filteredTables"
              :key="table.id"
              :class="['nav-item', { active: selectedTableId === table.id }]"
              @click="selectTable(table)"
            >
              <div class="item-alias">{{ table.alias || table.tableName }}</div>
              <div
                v-if="table.alias"
                class="item-name"
              >
                {{ table.tableName }}
              </div>
            </div>
            <div
              v-if="filteredTables.length === 0"
              class="empty-hint"
            >
              未找到表
            </div>
          </div>
          <div
            v-else
            class="loading-hint"
          >
            正在加载表…
          </div>
        </div>

        <!-- 右侧：列详情 -->
        <div class="content-area">
          <div
            v-if="selectedTable"
            class="table-detail"
          >
            <div class="detail-header">
              <div class="table-info">
                <h2 class="title">
                  {{ selectedTable.alias || selectedTable.tableName }}
                  <span
                    v-if="selectedTable.alias"
                    class="raw-name"
                    >({{ selectedTable.tableName }})</span
                  >
                </h2>
                <bz-button
                  type="primary"
                  link
                  size="small"
                  @click="openTableEdit"
                  >编辑表信息</bz-button
                >
              </div>
              <div class="column-search">
                <bz-input
                  v-model="columnSearch"
                  size="small"
                  placeholder="搜索列名或别名…"
                  clearable
                />
              </div>
            </div>

            <div class="column-grid">
              <div
                v-if="loadingColumns"
                class="loading-hint"
              >
                正在加载列…
              </div>
              <bz-table
                v-else
                :data="filteredColumns"
                size="small"
              >
                <bz-table-column
                  label="列名 / 别名"
                  min-width="220"
                >
                  <template #default="scope">
                    <div class="col-name-wrap">
                      <div
                        class="alias"
                        @click="startEditColumn(scope.row, 'alias')"
                      >
                        <span v-if="editingCol?.id !== scope.row.id || editingField !== 'alias'">{{
                          scope.row.alias || "点击设置别名"
                        }}</span>
                        <bz-input
                          v-else
                          v-model="editValue"
                          size="small"
                          @blur="saveColumnEdit"
                          @keyup.enter="saveColumnEdit"
                        />
                      </div>
                      <div class="raw-name">{{ scope.row.columnName }}</div>
                    </div>
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="类型"
                  width="180"
                >
                  <template #default="scope">
                    <span class="type-tag">{{ scope.row.typeName }}</span>
                    <span
                      v-if="scope.row.columnSize"
                      class="size-info"
                      >({{ scope.row.columnSize
                      }}{{ scope.row.decimalDigits ? "," + scope.row.decimalDigits : "" }})</span
                    >
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="备注"
                  min-width="200"
                >
                  <template #default="scope">
                    <div
                      class="remark-cell"
                      @click="startEditColumn(scope.row, 'remark')"
                    >
                      <span v-if="editingCol?.id !== scope.row.id || editingField !== 'remark'">{{
                        scope.row.remarkCustom || scope.row.remarkDb || "-"
                      }}</span>
                      <bz-input
                        v-else
                        v-model="editValue"
                        size="small"
                        @blur="saveColumnEdit"
                        @keyup.enter="saveColumnEdit"
                      />
                    </div>
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="可为空"
                  width="100"
                >
                  <template #default="scope">{{ scope.row.nullable ? "YES" : "NO" }}</template>
                </bz-table-column>
                <bz-table-column
                  label="默认值"
                  min-width="140"
                >
                  <template #default="scope"
                    ><span class="mono">{{ scope.row.defaultValue || "-" }}</span></template
                  >
                </bz-table-column>
              </bz-table>
            </div>
          </div>
          <bz-empty
            v-else
            description="请在左侧选择一张表以查看详细列信息"
          />
        </div>
      </template>
    </div>

    <!-- 表信息编辑弹窗 -->
    <bz-dialog
      v-model="tableEditVisible"
      title="编辑表信息"
      width="520px"
    >
      <bz-form label-width="70px">
        <bz-form-item label="别名">
          <bz-input v-model="tableEditForm.alias" />
        </bz-form-item>
        <bz-form-item label="备注">
          <bz-input
            v-model="tableEditForm.remark"
            type="textarea"
            :rows="3"
          />
        </bz-form-item>
      </bz-form>
      <template #footer>
        <bz-button @click="tableEditVisible = false">取消</bz-button>
        <bz-button
          type="primary"
          @click="saveTableInfo"
          >保存</bz-button
        >
      </template>
    </bz-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";

import {
  getDatabaseSchema,
  listAllColumns,
  listAllTables,
  refreshDatabaseSchemaMetadata,
  updateColumnInfo,
  updateTableInfo,
} from "../api/database-source";
import type { DatabaseColumn, DatabaseSchema, DatabaseTable } from "../types/database-source";
import { message } from "../utils/message";

const route = useRoute();
const dbId = String(route.params.id || "");

const dbInfo = ref<DatabaseSchema | null>(null);
const dataSourceMissing = ref(false);
const tables = ref<DatabaseTable[]>([]);
const columns = ref<DatabaseColumn[]>([]);

const loadingTables = ref(false);
const loadingColumns = ref(false);
const selectedTableId = ref<string | null>(null);
const selectedTable = ref<DatabaseTable | null>(null);

const tableSearch = ref("");
const columnSearch = ref("");

// 过滤后的表列表
const filteredTables = computed(() => {
  const kw = tableSearch.value.toLowerCase().trim();
  if (!kw) return tables.value;
  return tables.value.filter(
    (t) =>
      t.tableName.toLowerCase().includes(kw) || (t.alias && t.alias.toLowerCase().includes(kw)),
  );
});

// 过滤后的列列表
const filteredColumns = computed(() => {
  const kw = columnSearch.value.toLowerCase().trim();
  if (!kw) return columns.value;
  return columns.value.filter(
    (c) =>
      c.columnName.toLowerCase().includes(kw) || (c.alias && c.alias.toLowerCase().includes(kw)),
  );
});

// 加载初始数据
async function init() {
  try {
    dbInfo.value = await getDatabaseSchema(dbId);
    if (!dbInfo.value.dataSourceId) {
      dataSourceMissing.value = true;
      tables.value = [];
      columns.value = [];
      selectedTableId.value = null;
      selectedTable.value = null;
      message.error("未配置数据源，请先重新绑定后再使用");
      return;
    }
    dataSourceMissing.value = false;
    await loadTables();
  } catch (_e) {}
}

async function loadTables() {
  if (dataSourceMissing.value) return;
  loadingTables.value = true;
  try {
    tables.value = await listAllTables(dbId);
  } finally {
    loadingTables.value = false;
  }
}

async function selectTable(table: DatabaseTable) {
  selectedTableId.value = table.id;
  selectedTable.value = table;
  loadingColumns.value = true;
  columnSearch.value = "";
  try {
    columns.value = await listAllColumns(table.id);
  } finally {
    loadingColumns.value = false;
  }
}

async function handleRefresh() {
  if (dataSourceMissing.value) {
    message.error("未配置数据源，请先重新绑定后再使用");
    return;
  }
  try {
    message.info("正在刷新元数据...");
    await refreshDatabaseSchemaMetadata(dbId);
    message.success("刷新成功");
    await loadTables();
    if (selectedTableId.value) {
      const stillExists = tables.value.find((t) => t.id === selectedTableId.value);
      if (stillExists) selectTable(stillExists);
      else selectedTable.value = null;
    }
  } catch (_e) {}
}

// 表信息编辑
const tableEditVisible = ref(false);
const tableEditForm = reactive({ alias: "", remark: "" });

function openTableEdit() {
  if (!selectedTable.value) return;
  tableEditForm.alias = selectedTable.value.alias || "";
  tableEditForm.remark = selectedTable.value.remarkCustom || "";
  tableEditVisible.value = true;
}

async function saveTableInfo() {
  if (!selectedTable.value) return;
  try {
    await updateTableInfo(selectedTable.value.id, {
      alias: tableEditForm.alias,
      remarkCustom: tableEditForm.remark,
    });
    message.success("更新成功");
    tableEditVisible.value = false;
    // 更新本地数据
    selectedTable.value.alias = tableEditForm.alias;
    selectedTable.value.remarkCustom = tableEditForm.remark;
    const idx = tables.value.findIndex((t) => t.id === selectedTable.value?.id);
    if (idx !== -1) tables.value[idx] = { ...selectedTable.value };
  } catch (_e) {}
}

// 列信息编辑 (行内)
const editingCol = ref<DatabaseColumn | null>(null);
const editingField = ref<"alias" | "remark" | null>(null);
const editValue = ref("");

function startEditColumn(col: DatabaseColumn, field: "alias" | "remark") {
  editingCol.value = col;
  editingField.value = field;
  editValue.value = (field === "alias" ? col.alias : col.remarkCustom) || "";
}

async function saveColumnEdit() {
  if (!editingCol.value || !editingField.value) return;

  const col = editingCol.value;
  const field = editingField.value;
  const val = editValue.value;

  // 如果没变，直接取消
  const oldVal = (field === "alias" ? col.alias : col.remarkCustom) || "";
  if (val === oldVal) {
    cancelColumnEdit();
    return;
  }

  try {
    const updateData = {
      alias: field === "alias" ? val : col.alias || "",
      remarkCustom: field === "remark" ? val : col.remarkCustom || "",
    };
    await updateColumnInfo(col.id, updateData);

    // 更新本地
    if (field === "alias") col.alias = val;
    else col.remarkCustom = val;

    cancelColumnEdit();
  } catch (_e) {}
}

function cancelColumnEdit() {
  editingCol.value = null;
  editingField.value = null;
  editValue.value = "";
}

onMounted(init);
</script>

<style scoped>
.metadata-browser {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

.browser-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.breadcrumb .link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 600;
}

.breadcrumb .sep {
  color: var(--text-muted);
}

.breadcrumb .current {
  font-weight: 800;
  color: var(--text-main);
}

.browser-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧导航 */
.side-nav {
  width: 280px;
  background: #fff;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.nav-search {
  padding: 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: center;
}

.nav-search :deep(.el-input) {
  width: 100%;
}

.nav-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.nav-item {
  padding: 12px 12px;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
}

.nav-item:hover {
  background: #f1f5f9;
}

.nav-item.active {
  background: #eff6ff;
  color: var(--primary-color);
}

.item-alias {
  font-weight: 700;
  font-size: 14px;
}

.item-name {
  font-size: 11px;
  opacity: 0.6;
  margin-top: 2px;
}

/* 右侧内容区 */
.content-area {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.table-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.table-info .title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
}

.table-info .raw-name {
  font-size: 14px;
  font-weight: 400;
  color: var(--text-muted);
  margin-left: 8px;
}

.column-search {
  width: 240px;
}

.column-grid {
  flex: 1;
  overflow: auto;
  padding: 24px;
}

.col-name-wrap .alias {
  font-weight: 800;
  color: var(--text-main);
  cursor: pointer;
  min-height: 20px;
}

.col-name-wrap .raw-name {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.remark-cell {
  cursor: pointer;
  min-height: 20px;
  color: var(--text-muted);
}

.type-tag {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 11px;
  color: var(--text-main);
}

.size-info {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: 4px;
}

.mono {
  font-family: monospace;
  font-size: 12px;
}

.loading-hint,
.empty-hint {
  padding: 20px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
