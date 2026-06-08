<!-- /src/pages/DatabaseSchemaPage.vue -->
<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section class="list-page-actions">
          <div class="list-page-actions-main">
            <bz-button
              type="primary"
              @click="dialog.visible = true"
              >新增</bz-button
            >
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
                <div class="list-page-filter-label">所属数据源</div>
                <bz-select
                  v-model="selectedDataSourceId"
                  class="list-page-filter-control"
                  placeholder="全部"
                  @change="loadDatabaseSchemas"
                >
                  <bz-option
                    label="全部"
                    value="ALL"
                  />
                  <bz-option
                    label="未绑定"
                    value="UNBOUND"
                  />
                  <bz-option
                    v-for="ds in dataSources"
                    :key="ds.id"
                    :label="`${ds.name} (${ds.dbType})`"
                    :value="ds.id"
                  />
                </bz-select>
              </div>
            </bz-form-item>
            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">排序方式</div>
                <div class="list-page-sort-group">
                  <bz-button
                    :type="sortBy === 'DATA_SOURCE' ? 'primary' : 'default'"
                    @click="toggleSort('DATA_SOURCE')"
                  >
                    数据源
                    {{ sortBy === "DATA_SOURCE" ? (sortDirection === "ASC" ? "↑" : "↓") : "" }}
                  </bz-button>
                  <bz-button
                    :type="sortBy === 'CREATED_AT' ? 'primary' : 'default'"
                    @click="toggleSort('CREATED_AT')"
                  >
                    创建时间
                    {{ sortBy === "CREATED_AT" ? (sortDirection === "ASC" ? "↑" : "↓") : "" }}
                  </bz-button>
                </div>
              </div>
            </bz-form-item>
          </bz-form>
        </bz-card>

        <bz-card class="list-page-result-card">
          <bz-table
            v-loading="loading"
            :data="databaseSchemas"
            :tooltip-options="tableTooltipOptions"
            row-key="id"
            :empty-text="emptyText"
            size="small"
          >
            <bz-table-column
              label="别名"
              min-width="140"
              show-overflow-tooltip
            >
              <template #default="scope">
                <span class="cell-text">{{ scope.row.alias || "-" }}</span>
              </template>
            </bz-table-column>
            <bz-table-column
              prop="databaseName"
              label="数据库"
              min-width="160"
              show-overflow-tooltip
            >
              <template #default="scope">
                <span class="cell-text">{{ scope.row.databaseName }}</span>
              </template>
            </bz-table-column>
            <bz-table-column
              label="所属数据源"
              min-width="180"
              show-overflow-tooltip
            >
              <template #default="scope">
                <span class="cell-text">{{ getDataSourceName(scope.row.dataSourceId) }}</span>
              </template>
            </bz-table-column>
            <bz-table-column
              label="类型"
              width="120"
            >
              <template #default="scope">
                {{ formatDbType(getDataSourceType(scope.row.dataSourceId)) }}
              </template>
            </bz-table-column>
            <bz-table-column
              label="最近刷新"
              width="180"
            >
              <template #default="scope">
                {{ formatDateTime(scope.row.fetchedAt) }}
              </template>
            </bz-table-column>
            <bz-table-column
              label="备注"
              min-width="200"
              show-overflow-tooltip
            >
              <template #default="scope">
                <span class="cell-text">{{ scope.row.remarkCustom || "-" }}</span>
              </template>
            </bz-table-column>
            <bz-table-column
              label="操作"
              width="240"
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
                    :loading="action.loading"
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
        </bz-card>

        <DatabaseSchemaCreateDialog
          v-if="dialog.visible"
          :data-sources="dataSources"
          @close="dialog.visible = false"
          @saved="onSaved"
        />

        <bz-dialog
          v-model="rebindDialog.visible"
          title="绑定数据源"
          width="520px"
          @close="closeRebindDialog"
        >
          <bz-form label-width="90px">
            <bz-form-item label="选择数据源">
              <bz-select
                v-model="rebindDialog.dataSourceId"
                placeholder="请选择数据源"
              >
                <bz-option
                  v-for="ds in dataSources"
                  :key="ds.id"
                  :label="`${ds.name} (${ds.dbType})`"
                  :value="ds.id"
                />
              </bz-select>
            </bz-form-item>
          </bz-form>
          <template #footer>
            <bz-button @click="closeRebindDialog">取消</bz-button>
            <bz-button
              type="primary"
              :loading="rebindDialog.saving"
              :disabled="rebindDialog.dataSourceId === null"
              @click="handleRebind"
            >
              {{ rebindDialog.saving ? "绑定中…" : "确定绑定" }}
            </bz-button>
          </template>
        </bz-dialog>

        <bz-dialog
          v-model="editDialog.visible"
          title="编辑数据库信息"
          width="520px"
          @close="closeEditDialog"
        >
          <bz-form label-width="70px">
            <bz-form-item label="别名">
              <bz-input
                v-model="editDialog.alias"
                placeholder="输入别名"
              />
            </bz-form-item>
            <bz-form-item label="备注">
              <bz-input
                v-model="editDialog.remarkCustom"
                type="textarea"
                :rows="4"
                placeholder="输入备注"
              />
            </bz-form-item>
          </bz-form>
          <template #footer>
            <bz-button @click="closeEditDialog">取消</bz-button>
            <bz-button
              type="primary"
              :loading="editDialog.saving"
              @click="handleSaveInfo"
            >
              {{ editDialog.saving ? "保存中…" : "保存" }}
            </bz-button>
          </template>
        </bz-dialog>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";

import * as databaseSourceApi from "../api/database-source";
import DatabaseSchemaCreateDialog from "../components/datasource-admin/DatabaseSchemaCreateDialog.vue";
import type {
  DatabaseSchema,
  DatabaseSchemaBasicInfo,
  DatabaseSourceSimple,
  DatabaseType,
} from "../types/database-source";
import { bzConfirm } from "../utils/confirm";
import { formatDateTime } from "../utils/formatter";
import { message } from "../utils/message";

const router = useRouter();
const loading = ref(false);
const selectedDataSourceId = ref<string | "UNBOUND" | "ALL">("ALL");
const dataSources = ref<DatabaseSourceSimple[]>([]);
const databaseSchemas = ref<DatabaseSchema[]>([]);
const sortBy = ref<"DATA_SOURCE" | "CREATED_AT">("DATA_SOURCE");
const sortDirection = ref<"ASC" | "DESC">("ASC");
const copyingDbId = ref<string | null>(null);
const emptyText = computed(() =>
  selectedDataSourceId.value === "UNBOUND"
    ? "暂无未绑定的数据库记录。"
    : "暂无数据库记录，请点击右上方按钮添加。",
);

const dialog = reactive({
  visible: false,
});

const tableTooltipOptions = {
  placement: "bottom-start",
};

async function loadDataSources() {
  try {
    dataSources.value = await databaseSourceApi.listSimpleDatabaseSources();
    selectedDataSourceId.value = "ALL";
  } catch (_e) {}
}

async function loadDatabaseSchemas() {
  loading.value = true;
  try {
    const options = {
      sortBy: sortBy.value,
      sortDirection: sortDirection.value,
    } as const;

    if (selectedDataSourceId.value === "UNBOUND") {
      databaseSchemas.value = await databaseSourceApi.listDatabaseSchemas(undefined, {
        ...options,
        unboundOnly: true,
      });
    } else if (selectedDataSourceId.value === "ALL") {
      databaseSchemas.value = await databaseSourceApi.listDatabaseSchemas(undefined, options);
    } else {
      databaseSchemas.value = await databaseSourceApi.listDatabaseSchemas(
        selectedDataSourceId.value,
        options,
      );
    }
  } finally {
    loading.value = false;
  }
}

function toggleSort(target: "DATA_SOURCE" | "CREATED_AT") {
  if (sortBy.value === target) {
    sortDirection.value = sortDirection.value === "ASC" ? "DESC" : "ASC";
  } else {
    sortBy.value = target;
    sortDirection.value = "ASC";
  }
  void loadDatabaseSchemas();
}

async function handleRefresh(db: DatabaseSchema) {
  if (!db.dataSourceId) {
    message.error("未配置数据源，请先绑定后再操作");
    return;
  }
  try {
    message.info(`正在刷新 "${db.alias || db.databaseName}" 的元数据…`);
    await databaseSourceApi.refreshDatabaseSchemaMetadata(db.id);
    message.success("同步完成");
    void loadDatabaseSchemas();
  } catch (_e) {}
}

async function handleDelete(db: DatabaseSchema) {
  const confirmed = await bzConfirm({
    title: "移除数据库",
    message: `确定要移除数据库记录 "${db.alias || db.databaseName}" 吗？表和列元数据将被删除。`,
    confirmText: "移除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  try {
    await databaseSourceApi.deleteDatabaseSchema(db.id);
    message.success("移除成功");
    void loadDatabaseSchemas();
  } catch (_e) {}
}

function goToBrowser(db: DatabaseSchema) {
  if (!db.dataSourceId) {
    message.error("未配置数据源，请先绑定后再操作");
    return;
  }
  router.push(`/database-schemas/${db.id}/metadata`);
}

const rebindDialog = reactive({
  visible: false,
  databaseSchemaId: null as string | null,
  dataSourceId: null as string | null,
  saving: false,
});

const editDialog = reactive({
  visible: false,
  databaseSchemaId: null as string | null,
  alias: "",
  remarkCustom: "",
  saving: false,
});

function openEditDialog(db: DatabaseSchema) {
  editDialog.visible = true;
  editDialog.databaseSchemaId = db.id;
  editDialog.alias = db.alias ?? "";
  editDialog.remarkCustom = db.remarkCustom ?? "";
}

function closeEditDialog() {
  editDialog.visible = false;
  editDialog.databaseSchemaId = null;
  editDialog.alias = "";
  editDialog.remarkCustom = "";
  editDialog.saving = false;
}

async function handleSaveInfo() {
  if (editDialog.databaseSchemaId === null) {
    return;
  }
  editDialog.saving = true;
  try {
    if (typeof databaseSourceApi.updateDatabaseSchemaInfo !== "function") {
      message.error("前端模块未更新，请刷新页面重试");
      editDialog.saving = false;
      return;
    }
    await databaseSourceApi.updateDatabaseSchemaInfo(editDialog.databaseSchemaId, {
      alias: editDialog.alias.trim(),
      remarkCustom: editDialog.remarkCustom.trim(),
    });
    message.success("数据库信息已更新");
    closeEditDialog();
    await loadDatabaseSchemas();
  } catch (_e) {
    editDialog.saving = false;
  }
}

const dataSourceMap = computed(() => {
  const map = new Map<string, DatabaseSourceSimple>();
  dataSources.value.forEach((item) => {
    map.set(item.id, item);
  });
  return map;
});

function getDataSourceName(dataSourceId: string | null): string {
  if (dataSourceId === null) {
    return "未绑定数据源";
  }
  return dataSourceMap.value.get(dataSourceId)?.name ?? "未知数据源";
}

function getDataSourceType(dataSourceId: string | null): DatabaseType | undefined {
  if (dataSourceId === null) {
    return undefined;
  }
  return dataSourceMap.value.get(dataSourceId)?.dbType;
}

function formatDbType(dbType?: DatabaseType): string {
  if (!dbType) {
    return "-";
  }
  return {
    H2: "H2",
    MYSQL: "MySQL",
    POSTGRESQL: "PostgreSQL",
    ORACLE: "Oracle",
    SQLSERVER: "SQL Server",
  }[dbType];
}

function getDefaultPort(dbType?: DatabaseType): string {
  if (!dbType) {
    return "-";
  }
  return {
    H2: "-",
    MYSQL: "3306",
    POSTGRESQL: "5432",
    ORACLE: "1521",
    SQLSERVER: "1433",
  }[dbType];
}

function extractJdbcHostPort(
  jdbcUrl: string,
  dbType?: DatabaseType,
): { host: string; port: string } {
  if (!jdbcUrl || jdbcUrl === "-") {
    return { host: "-", port: "-" };
  }

  const normalized = jdbcUrl.startsWith("jdbc:") ? jdbcUrl.slice(5) : jdbcUrl;
  try {
    const parsed = new URL(normalized);
    return {
      host: parsed.hostname || "-",
      port: parsed.port || getDefaultPort(dbType),
    };
  } catch (_err) {
    const matched = normalized.match(/\/\/([^/:;?]+)(?::(\d+))?/);
    return {
      host: matched?.[1] || "-",
      port: matched?.[2] || getDefaultPort(dbType),
    };
  }
}

function buildDatabaseSchemaBasicInfoText(
  db: DatabaseSchema,
  basicInfo: DatabaseSchemaBasicInfo | null,
): string {
  const title = (db.alias || `${getDataSourceName(db.dataSourceId)}-${db.databaseName}`).trim();
  const jdbcUrl = basicInfo?.jdbcUrl?.trim() || "-";
  const { host, port } = extractJdbcHostPort(jdbcUrl, basicInfo?.dbType);
  const productName = db.productName?.trim() || formatDbType(basicInfo?.dbType);
  const productVersion = db.productVersion?.trim() || "-";
  const username = basicInfo?.username?.trim() || "-";
  const passwordRaw = basicInfo?.passwordRaw?.trim() || "-";
  const remark = db.remarkCustom?.trim() || "-";

  return [
    title || db.databaseName,
    `数据库产品：${productName}`,
    `数据库版本：${productVersion}`,
    `主机：${host}`,
    `端口：${port}`,
    `用户：${username}`,
    `密码：${passwordRaw}`,
    `数据库：${db.databaseName}`,
    `url：${jdbcUrl}`,
    `备注：${remark}`,
  ].join("\n");
}

async function copyDatabaseSchemaBasicInfo(db: DatabaseSchema) {
  copyingDbId.value = db.id;
  try {
    const basicInfo = db.dataSourceId
      ? await databaseSourceApi.getDatabaseSchemaBasicInfo(db.id)
      : null;
    const content = buildDatabaseSchemaBasicInfoText(db, basicInfo);
    await navigator.clipboard.writeText(content);
    message.success(`已复制「${db.alias || db.databaseName}」基础信息`);
  } catch (_err) {
    message.warning("复制失败，请检查权限或稍后重试");
  } finally {
    copyingDbId.value = null;
  }
}

type RowActionType = "primary" | "success" | "warning" | "danger" | "info";

interface RowAction {
  key: string;
  label: string;
  type?: RowActionType;
  disabled?: boolean;
  loading?: boolean;
  handler: () => void;
}

function getRowActions(row: DatabaseSchema): RowAction[] {
  const actions: RowAction[] = [];
  if (row.dataSourceId) {
    actions.push({
      key: "browse",
      label: "浏览表列",
      type: "primary",
      handler: () => goToBrowser(row),
    });
  } else {
    actions.push({
      key: "bind",
      label: "绑定数据源",
      type: "primary",
      handler: () => openRebindDialog(row),
    });
  }
  actions.push(
    {
      key: "edit",
      label: "编辑",
      handler: () => openEditDialog(row),
    },
    {
      key: "copy",
      label: "复制",
      loading: copyingDbId.value === row.id,
      disabled: copyingDbId.value === row.id,
      handler: () => copyDatabaseSchemaBasicInfo(row),
    },
  );
  if (row.dataSourceId) {
    actions.push({
      key: "refresh",
      label: "刷新元数据",
      handler: () => handleRefresh(row),
    });
  }
  actions.push({
    key: "delete",
    label: "删除",
    type: "danger",
    handler: () => handleDelete(row),
  });
  return actions;
}

function getPrimaryActions(row: DatabaseSchema): RowAction[] {
  const actions = getRowActions(row);
  if (actions.length <= 3) return actions;
  return actions.slice(0, 2);
}

function getExtraActions(row: DatabaseSchema): RowAction[] {
  const actions = getRowActions(row);
  if (actions.length <= 3) return [];
  return actions.slice(2);
}

function openRebindDialog(db: DatabaseSchema) {
  rebindDialog.visible = true;
  rebindDialog.databaseSchemaId = db.id;
  rebindDialog.dataSourceId = null;
}

function closeRebindDialog() {
  rebindDialog.visible = false;
  rebindDialog.databaseSchemaId = null;
  rebindDialog.dataSourceId = null;
  rebindDialog.saving = false;
}

async function handleRebind() {
  if (rebindDialog.databaseSchemaId === null || rebindDialog.dataSourceId === null) return;
  rebindDialog.saving = true;
  try {
    await databaseSourceApi.rebindDatabaseSchema(
      rebindDialog.databaseSchemaId,
      rebindDialog.dataSourceId,
    );
    message.success("绑定成功");
    closeRebindDialog();
    await loadDatabaseSchemas();
  } catch (_e) {
    rebindDialog.saving = false;
  }
}

async function onSaved() {
  dialog.visible = false;
  await loadDatabaseSchemas();
}

onMounted(async () => {
  await loadDataSources();
  await loadDatabaseSchemas();
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

.list-page-sort-group {
  justify-content: flex-start;
}
</style>
