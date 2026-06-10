<template>
  <div class="app-shell">
    <div class="content">
      <bz-tabs
        v-model="activeTab"
        class="tabs"
      >
        <bz-tab-pane
          label="快照管理"
          name="SNAPSHOT"
        />
        <bz-tab-pane
          label="DDL 管理"
          name="DDL"
        />
        <bz-tab-pane
          label="结构对比"
          name="DIFF"
        />
      </bz-tabs>

      <template v-if="activeTab === 'SNAPSHOT'">
        <div class="list-page-stack">
          <section class="list-page-actions">
            <div class="list-page-actions-main">
              <bz-button
                v-if="canSnapshotCreate"
                type="primary"
                @click="openCreateDialog('SNAPSHOT')"
                >新增</bz-button
              >
              <bz-button
                v-if="canSnapshotView"
                @click="loadSnapshotPage(1)"
                >刷新</bz-button
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
                  <div class="list-page-filter-label">数据库</div>
                  <bz-select
                    v-model="snapshotQuery.managedDatabaseId"
                    class="list-page-filter-control"
                    placeholder="全部数据库"
                    clearable
                  >
                    <bz-option
                      v-for="db in managedDatabases"
                      :key="db.id"
                      :label="getManagedDatabaseLabel(db.id)"
                      :value="db.id"
                    />
                  </bz-select>
                </div>
              </bz-form-item>
              <bz-form-item class="list-page-filter-item">
                <div class="list-page-filter-field">
                  <div class="list-page-filter-label">快照名称</div>
                  <bz-input
                    v-model="snapshotQuery.nameLike"
                    class="list-page-filter-control"
                    placeholder="搜索快照名称"
                    clearable
                    @keyup.enter="loadSnapshotPage(1)"
                  />
                </div>
              </bz-form-item>
              <bz-form-item class="list-page-filter-actions">
                <bz-button
                  type="primary"
                  @click="loadSnapshotPage(1)"
                  >搜索</bz-button
                >
                <bz-button @click="resetSnapshotFilters">重置</bz-button>
              </bz-form-item>
            </bz-form>
          </bz-card>

          <bz-card class="list-page-result-card board">
            <bz-empty
              v-if="!canSnapshotView"
              description="无权限查看快照管理"
            />
            <bz-table
              v-else
              v-loading="snapshotLoading"
              :data="snapshotPage.elements"
              empty-text="暂无快照记录"
              size="small"
            >
              <bz-table-column
                label="名称"
                min-width="220"
              >
                <template #default="scope">
                  <div class="primary-text">{{ scope.row.name }}</div>
                  <div class="minor-text">{{ scope.row.schemaName || "-" }}</div>
                </template>
              </bz-table-column>
              <bz-table-column
                label="数据库"
                min-width="180"
              >
                <template #default="scope">
                  {{ getManagedDatabaseLabel(scope.row.managedDatabaseId) }}
                </template>
              </bz-table-column>
              <bz-table-column
                prop="dbType"
                label="类型"
                width="120"
              />
              <bz-table-column
                label="创建时间"
                width="180"
              >
                <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
              </bz-table-column>
              <bz-table-column
                label="备注"
                min-width="180"
              >
                <template #default="scope">
                  <bz-tooltip
                    v-if="scope.row.remark"
                    :content="scope.row.remark"
                    placement="top"
                  >
                    <span class="truncate">{{ scope.row.remark }}</span>
                  </bz-tooltip>
                  <span v-else>-</span>
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
                      v-for="action in getPrimarySnapshotActions(scope.row)"
                      :key="action.key"
                      size="small"
                      :type="action.type"
                      :disabled="action.disabled"
                      @click="action.handler"
                      >{{ action.label }}
                    </bz-button>
                    <bz-dropdown
                      v-if="getExtraSnapshotActions(scope.row).length"
                      trigger="click"
                    >
                      <bz-button size="small">更多</bz-button>
                      <template #dropdown>
                        <bz-dropdown-menu>
                          <bz-dropdown-item
                            v-for="action in getExtraSnapshotActions(scope.row)"
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

            <div
              v-if="snapshotPage.totalElements > 0"
              class="list-page-pagination"
            >
              <bz-pagination
                background
                layout="total, prev, pager, next"
                :total="snapshotPage.totalElements"
                :page-size="snapshotQuery.pageSize"
                :current-page="snapshotQuery.pageNo"
                @current-change="loadSnapshotPage"
              />
            </div>
          </bz-card>
        </div>
      </template>

      <template v-if="activeTab === 'DDL'">
        <div class="list-page-stack">
          <section class="list-page-actions">
            <div class="list-page-actions-main">
              <bz-button
                v-if="canDdlCreate"
                type="primary"
                @click="openCreateDialog('DDL')"
                >新增</bz-button
              >
              <bz-button
                v-if="canDdlView"
                @click="loadDdlPage(1)"
                >刷新</bz-button
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
                  <div class="list-page-filter-label">数据库</div>
                  <bz-select
                    v-model="ddlQuery.managedDatabaseId"
                    class="list-page-filter-control"
                    placeholder="全部数据库"
                    clearable
                  >
                    <bz-option
                      v-for="db in managedDatabases"
                      :key="db.id"
                      :label="getManagedDatabaseLabel(db.id)"
                      :value="db.id"
                    />
                  </bz-select>
                </div>
              </bz-form-item>
              <bz-form-item class="list-page-filter-item">
                <div class="list-page-filter-field">
                  <div class="list-page-filter-label">DDL 名称</div>
                  <bz-input
                    v-model="ddlQuery.nameLike"
                    class="list-page-filter-control"
                    placeholder="搜索DDL名称"
                    clearable
                    @keyup.enter="loadDdlPage(1)"
                  />
                </div>
              </bz-form-item>
              <bz-form-item class="list-page-filter-actions">
                <bz-button
                  type="primary"
                  @click="loadDdlPage(1)"
                  >搜索</bz-button
                >
                <bz-button @click="resetDdlFilters">重置</bz-button>
              </bz-form-item>
            </bz-form>
          </bz-card>

          <bz-card class="list-page-result-card board">
            <bz-empty
              v-if="!canDdlView"
              description="无权限查看DDL管理"
            />
            <bz-table
              v-else
              v-loading="ddlLoading"
              :data="ddlPage.elements"
              empty-text="暂无DDL记录"
              size="small"
            >
              <bz-table-column
                label="名称"
                min-width="220"
              >
                <template #default="scope">
                  <div class="primary-text">{{ scope.row.name }}</div>
                  <div class="minor-text">{{ scope.row.schemaName || "-" }}</div>
                </template>
              </bz-table-column>
              <bz-table-column
                label="数据库"
                min-width="180"
              >
                <template #default="scope">
                  {{ getManagedDatabaseLabel(scope.row.managedDatabaseId) }}
                </template>
              </bz-table-column>
              <bz-table-column
                prop="dbType"
                label="类型"
                width="120"
              />
              <bz-table-column
                label="创建时间"
                width="180"
              >
                <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
              </bz-table-column>
              <bz-table-column
                label="备注"
                min-width="180"
              >
                <template #default="scope">
                  <bz-tooltip
                    v-if="scope.row.remark"
                    :content="scope.row.remark"
                    placement="top"
                  >
                    <span class="truncate">{{ scope.row.remark }}</span>
                  </bz-tooltip>
                  <span v-else>-</span>
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
                      v-for="action in getPrimaryDdlActions(scope.row)"
                      :key="action.key"
                      size="small"
                      :type="action.type"
                      :disabled="action.disabled"
                      @click="action.handler"
                      >{{ action.label }}
                    </bz-button>
                    <bz-dropdown
                      v-if="getExtraDdlActions(scope.row).length"
                      trigger="click"
                    >
                      <bz-button size="small">更多</bz-button>
                      <template #dropdown>
                        <bz-dropdown-menu>
                          <bz-dropdown-item
                            v-for="action in getExtraDdlActions(scope.row)"
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

            <div
              v-if="ddlPage.totalElements > 0"
              class="list-page-pagination"
            >
              <bz-pagination
                background
                layout="total, prev, pager, next"
                :total="ddlPage.totalElements"
                :page-size="ddlQuery.pageSize"
                :current-page="ddlQuery.pageNo"
                @current-change="loadDdlPage"
              />
            </div>
          </bz-card>
        </div>
      </template>

      <template v-if="activeTab === 'DIFF'">
        <div class="list-page-stack">
          <section class="list-page-actions">
            <div class="list-page-actions-main">
              <bz-button
                v-if="canDiffView"
                type="primary"
                :loading="diffLoading"
                :disabled="!canRunDiffCompare"
                @click="runDiffCompare"
              >
                {{ diffLoading ? "对比中..." : "开始对比" }}
              </bz-button>
              <bz-button
                v-if="canDiffView"
                :disabled="comparableManagedDatabases.length < 2"
                @click="swapDiffDatabases"
                >交换数据库</bz-button
              >
              <bz-button
                v-if="canDiffView"
                @click="resetDiffFilters"
                >重置</bz-button
              >
            </div>
          </section>

          <bz-card
            class="list-page-query-card"
            shadow="never"
          >
            <bz-empty
              v-if="!canDiffView"
              description="无权限查看结构对比"
            />
            <template v-else>
              <bz-form
                class="list-page-filter-form"
                :inline="true"
                @submit.prevent
              >
                <bz-form-item class="list-page-filter-item">
                  <div class="list-page-filter-field">
                    <div class="list-page-filter-label">起始数据库</div>
                    <bz-select
                      v-model="diffForm.refDbId"
                      class="list-page-filter-control"
                      placeholder="请选择"
                    >
                      <bz-option
                        v-for="db in comparableManagedDatabases"
                        :key="`ref-${db.id}`"
                        :label="getManagedDatabaseOptionLabel(db)"
                        :value="db.id"
                      />
                    </bz-select>
                  </div>
                </bz-form-item>

                <bz-form-item class="list-page-filter-item">
                  <div class="list-page-filter-field">
                    <div class="list-page-filter-label">目标数据库</div>
                    <bz-select
                      v-model="diffForm.targetDbId"
                      class="list-page-filter-control"
                      placeholder="请选择"
                    >
                      <bz-option
                        v-for="db in comparableManagedDatabases"
                        :key="`target-${db.id}`"
                        :label="getManagedDatabaseOptionLabel(db)"
                        :value="db.id"
                      />
                    </bz-select>
                  </div>
                </bz-form-item>
              </bz-form>
              <div class="minor-text">生成的 SQL 用于将起始数据库结构变更为目标数据库结构。</div>
              <div
                v-if="comparableManagedDatabases.length === 0"
                class="minor-text"
              >
                暂无可用于对比的数据库，请先在数据库管理中绑定数据源。
              </div>
            </template>
          </bz-card>

          <DiffResultPanel
            v-if="canDiffView"
            :loading="diffLoading"
            :result="diffResult"
            :reference-label="diffReferenceLabel"
            :target-label="diffTargetLabel"
          />
        </div>
      </template>

      <bz-card
        v-if="preview.visible"
        class="preview"
      >
        <div class="preview-head">
          <div>
            <div class="preview-title">预览：{{ preview.title }}</div>
            <div
              v-if="preview.truncated"
              class="minor-text"
            >
              内容过长，仅展示前 200000 个字符
            </div>
          </div>
          <div class="preview-actions">
            <bz-button
              v-if="canCopyPreview"
              size="small"
              @click="copyPreviewContent"
              >复制全部</bz-button
            >
            <bz-button
              size="small"
              @click="closePreview"
              >关闭</bz-button
            >
          </div>
        </div>
        <pre class="preview-content">{{ preview.content }}</pre>
      </bz-card>

      <bz-dialog
        v-model="createDialog.visible"
        :title="createDialog.kind === 'SNAPSHOT' ? '新建快照' : '新建DDL'"
        width="760px"
        @close="closeCreateDialog"
      >
        <bz-form label-width="110px">
          <bz-form-item
            v-if="createDialog.kind === 'SNAPSHOT'"
            label="数据库"
          >
            <bz-select
              v-model="createDialog.managedDatabaseId"
              placeholder="请选择"
              @change="handleSnapshotDatabaseChange"
            >
              <bz-option
                v-for="db in creatableManagedDatabases"
                :key="db.id"
                :label="getManagedDatabaseOptionLabel(db)"
                :value="db.id"
              />
            </bz-select>
          </bz-form-item>
          <bz-form-item
            v-if="createDialog.kind === 'SNAPSHOT'"
            label="备份对象"
          >
            <div class="object-panel">
              <div class="object-toolbar">
                <bz-input
                  v-model="snapshotObjectKeyword"
                  size="small"
                  maxlength="128"
                  placeholder="按名称或 schema 搜索"
                />
                <bz-button
                  size="small"
                  :disabled="snapshotObjectsLoading || snapshotObjects.length === 0"
                  @click="selectAllSnapshotObjects"
                  >全选</bz-button
                >
                <bz-button
                  size="small"
                  :disabled="snapshotObjectsLoading || snapshotObjects.length === 0"
                  @click="clearSnapshotObjects"
                  >清空</bz-button
                >
              </div>
              <div
                v-if="snapshotObjectsLoading"
                class="minor-text"
              >
                正在加载对象列表...
              </div>
              <div
                v-else-if="filteredSnapshotObjects.length === 0"
                class="minor-text"
              >
                暂无可选对象
              </div>
              <bz-checkbox-group
                v-else
                v-model="createDialog.selectedObjectIds"
                class="object-list"
              >
                <bz-checkbox
                  v-for="item in filteredSnapshotObjects"
                  :key="item.id"
                  :label="item.id"
                >
                  <span
                    class="object-type-tag"
                    :class="resolveObjectTypeClass(item.tableType)"
                    >{{ resolveObjectTypeTag(item.tableType) }}</span
                  >
                  <span class="object-name">{{ getSnapshotObjectDisplayName(item) }}</span>
                </bz-checkbox>
              </bz-checkbox-group>
              <div class="minor-text">
                已选择 {{ createDialog.selectedObjectIds.length }} / {{ snapshotObjects.length }}
              </div>
            </div>
          </bz-form-item>
          <bz-form-item
            v-else
            label="起始快照"
          >
            <bz-select
              v-model="createDialog.sourceSnapshotId"
              placeholder="空快照（生成全量DDL）"
            >
              <bz-option
                :label="'空快照（生成全量DDL）'"
                :value="undefined"
              />
              <bz-option
                v-for="snapshot in ddlSnapshotOptions"
                :key="`source-${snapshot.id}`"
                :label="getSnapshotOptionLabel(snapshot)"
                :value="snapshot.id"
              />
            </bz-select>
          </bz-form-item>
          <bz-form-item
            v-if="createDialog.kind === 'DDL'"
            label="目标快照"
          >
            <bz-select
              v-model="createDialog.targetSnapshotId"
              placeholder="请选择"
            >
              <bz-option
                v-for="snapshot in ddlSnapshotOptions"
                :key="`target-${snapshot.id}`"
                :label="getSnapshotOptionLabel(snapshot)"
                :value="snapshot.id"
              />
            </bz-select>
          </bz-form-item>
          <bz-form-item label="名称">
            <bz-input
              v-model="createDialog.name"
              maxlength="128"
              :placeholder="
                createDialog.kind === 'SNAPSHOT' ? '例如：xx库快照' : '例如：v1到v2结构迁移DDL'
              "
            />
          </bz-form-item>
          <bz-form-item label="备注">
            <bz-input
              v-model="createDialog.remark"
              type="textarea"
              :rows="3"
              maxlength="512"
            />
          </bz-form-item>
        </bz-form>
        <template #footer>
          <bz-button @click="closeCreateDialog">取消</bz-button>
          <bz-button
            type="primary"
            :loading="createDialog.saving"
            :disabled="createDialog.kind === 'SNAPSHOT' && snapshotObjectsLoading"
            @click="submitCreate"
          >
            {{ createDialog.saving ? "生成中..." : "确认生成" }}
          </bz-button>
        </template>
      </bz-dialog>

      <bz-dialog
        v-model="editDialog.visible"
        :title="editDialog.kind === 'SNAPSHOT' ? '编辑快照' : '编辑DDL'"
        width="520px"
        @close="closeEditDialog"
      >
        <bz-form label-width="70px">
          <bz-form-item label="名称">
            <bz-input
              v-model="editDialog.name"
              maxlength="128"
            />
          </bz-form-item>
          <bz-form-item label="备注">
            <bz-input
              v-model="editDialog.remark"
              type="textarea"
              :rows="3"
              maxlength="512"
            />
          </bz-form-item>
        </bz-form>
        <template #footer>
          <bz-button @click="closeEditDialog">取消</bz-button>
          <bz-button
            type="primary"
            :loading="editDialog.saving"
            @click="submitEdit"
          >
            {{ editDialog.saving ? "保存中..." : "保存" }}
          </bz-button>
        </template>
      </bz-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import { listDatabaseSchemas } from "../api/database-source";
import * as schemaforgeApi from "../api/schemaforge";
import { downloadSystemFile, fetchSystemFileView, getSystemFileMeta } from "../api/system-files";
import DiffResultPanel from "../components/schemaforge/DiffResultPanel.vue";
import { hasApiPermission } from "../registry/permissions.registry";
import type { DatabaseSchema } from "../types/database-source";
import type { SystemFileItem } from "../types/file-storage";
import type { PageResult } from "../types/page";
import type {
  SchemaDdlItem,
  SchemaDiffResult,
  SchemaSnapshotItem,
  SnapshotSelectableObject,
} from "../types/schemaforge";
import { bzConfirm } from "../utils/confirm";
import { formatDateTime } from "../utils/formatter";
import { message } from "../utils/message";

type ActiveTab = "SNAPSHOT" | "DDL" | "DIFF";
type EditKind = "SNAPSHOT" | "DDL";
type RowActionType = "primary" | "success" | "warning" | "danger" | "info";

interface RowAction {
  key: string;
  label: string;
  type?: RowActionType;
  disabled?: boolean;
  handler: () => void;
}

const route = useRoute();
const router = useRouter();

function resolveTabFromQuery(value: unknown): ActiveTab {
  const raw = Array.isArray(value) ? value[0] : value;
  if (raw === "DDL" || raw === "DIFF" || raw === "SNAPSHOT") {
    return raw;
  }
  return "SNAPSHOT";
}

const activeTab = ref<ActiveTab>(resolveTabFromQuery(route.query.tab));
const managedDatabases = ref<DatabaseSchema[]>([]);

const canSnapshotView = computed(() => hasApiPermission("sfg.snap.view"));
const canSnapshotCreate = computed(() => hasApiPermission("sfg.snap.add"));
const canSnapshotEdit = computed(() => hasApiPermission("sfg.snap.edit"));
const canSnapshotDelete = computed(() => hasApiPermission("sfg.snap.del"));

const canDdlView = computed(() => hasApiPermission("sfg.ddl.view"));
const canDdlCreate = computed(() => hasApiPermission("sfg.ddl.add"));
const canDdlEdit = computed(() => hasApiPermission("sfg.ddl.edit"));
const canDdlDelete = computed(() => hasApiPermission("sfg.ddl.del"));

const canDiffView = computed(() => hasApiPermission("sfg.diff.view"));
const canFileView = computed(() => hasApiPermission("sfl.preview"));
const canFileDownload = computed(() => hasApiPermission("sfl.download"));

const snapshotQuery = reactive({
  managedDatabaseId: undefined as string | undefined,
  nameLike: "",
  pageNo: 1,
  pageSize: 10,
});

const ddlQuery = reactive({
  managedDatabaseId: undefined as string | undefined,
  nameLike: "",
  pageNo: 1,
  pageSize: 10,
});

const snapshotLoading = ref(false);
const ddlLoading = ref(false);

const snapshotPage = reactive<PageResult<SchemaSnapshotItem>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});

const ddlPage = reactive<PageResult<SchemaDdlItem>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});

const preview = reactive({
  visible: false,
  title: "",
  content: "",
  truncated: false,
  fullContent: "",
  fileMeta: null as SystemFileItem | null,
});

const canCopyPreview = computed(() => {
  return preview.visible && preview.fileMeta?.type === "FILE" && !!preview.fullContent;
});

const editDialog = reactive({
  visible: false,
  kind: "SNAPSHOT" as EditKind,
  id: "",
  name: "",
  remark: "",
  saving: false,
});

const managedDatabaseMap = computed(() => {
  return new Map(managedDatabases.value.map((db) => [db.id, db]));
});

const creatableManagedDatabases = computed(() => {
  return managedDatabases.value.filter((db) => db.dataSourceId !== null);
});

const comparableManagedDatabases = computed(() => {
  return managedDatabases.value.filter((db) => db.dataSourceId !== null);
});

const diffReferenceLabel = computed(() => {
  if (!diffCompared.refDbId) {
    return "-";
  }
  return getManagedDatabaseLabel(diffCompared.refDbId);
});

const diffTargetLabel = computed(() => {
  if (!diffCompared.targetDbId) {
    return "-";
  }
  return getManagedDatabaseLabel(diffCompared.targetDbId);
});

const filteredSnapshotObjects = computed(() => {
  const keyword = snapshotObjectKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return snapshotObjects.value;
  }
  return snapshotObjects.value.filter((item) => {
    const tableName = (item.tableName || "").toLowerCase();
    const tableSchema = (item.tableSchema || "").toLowerCase();
    const alias = (item.alias || "").toLowerCase();
    return tableName.includes(keyword) || tableSchema.includes(keyword) || alias.includes(keyword);
  });
});

const createDialog = reactive({
  visible: false,
  kind: "SNAPSHOT" as EditKind,
  managedDatabaseId: undefined as string | undefined,
  selectedObjectIds: [] as string[],
  sourceSnapshotId: undefined as string | undefined,
  targetSnapshotId: undefined as string | undefined,
  name: "",
  remark: "",
  saving: false,
});

const ddlSnapshotOptions = ref<SchemaSnapshotItem[]>([]);
const snapshotObjects = ref<SnapshotSelectableObject[]>([]);
const snapshotObjectsLoading = ref(false);
const snapshotObjectKeyword = ref("");

const diffForm = reactive({
  refDbId: undefined as string | undefined,
  targetDbId: undefined as string | undefined,
});

const diffCompared = reactive({
  refDbId: undefined as string | undefined,
  targetDbId: undefined as string | undefined,
});

const diffLoading = ref(false);
const diffResult = ref<SchemaDiffResult | null>(null);

const canRunDiffCompare = computed(() => {
  return Boolean(
    diffForm.refDbId && diffForm.targetDbId && diffForm.refDbId !== diffForm.targetDbId,
  );
});

function getManagedDatabaseLabel(managedDatabaseId: string): string {
  const item = managedDatabaseMap.value.get(managedDatabaseId);
  if (!item) {
    return `#${managedDatabaseId}`;
  }
  return item.alias || item.databaseName;
}

function getManagedDatabaseOptionLabel(item: DatabaseSchema): string {
  const base = item.alias || item.databaseName;
  if (!item.alias || item.alias === item.databaseName) {
    return base;
  }
  return `${base} (${item.databaseName})`;
}

function getSnapshotOptionLabel(item: SchemaSnapshotItem): string {
  const dbLabel = getManagedDatabaseLabel(item.managedDatabaseId);
  return `${item.name} · ${dbLabel} · ${formatDateTime(item.createdAt)}`;
}

function resolveObjectTypeTag(type?: string): string {
  if ((type || "").toUpperCase() === "VIEW") {
    return "V";
  }
  return "T";
}

function resolveObjectTypeClass(type?: string): string {
  if ((type || "").toUpperCase() === "VIEW") {
    return "view";
  }
  return "table";
}

function getSnapshotActions(row: SchemaSnapshotItem): RowAction[] {
  const actions: RowAction[] = [];
  if (canFileView.value) {
    actions.push({
      key: "preview",
      label: "预览",
      handler: () => previewLogicalFile(resolveSnapshotPreviewFileId(row), row.name),
    });
  }
  if (canFileDownload.value) {
    actions.push({
      key: "download",
      label: "下载",
      handler: () => downloadLogicalFile(resolveSnapshotPreviewFileId(row), `${row.name}.sql`),
    });
  }
  if (canSnapshotEdit.value) {
    actions.push({
      key: "edit",
      label: "编辑",
      handler: () => openEditDialog("SNAPSHOT", row.id, row.name, row.remark),
    });
  }
  if (canSnapshotDelete.value) {
    actions.push({
      key: "delete",
      label: "删除",
      type: "danger",
      handler: () => removeSnapshot(row.id, row.name),
    });
  }
  return actions;
}

function getPrimarySnapshotActions(row: SchemaSnapshotItem): RowAction[] {
  const actions = getSnapshotActions(row);
  if (actions.length <= 3) return actions;
  return actions.slice(0, 2);
}

function getExtraSnapshotActions(row: SchemaSnapshotItem): RowAction[] {
  const actions = getSnapshotActions(row);
  if (actions.length <= 3) return [];
  return actions.slice(2);
}

function getDdlActions(row: SchemaDdlItem): RowAction[] {
  const actions: RowAction[] = [];
  if (canFileView.value) {
    actions.push({
      key: "preview",
      label: "预览",
      handler: () => previewLogicalFile(row.logicalFileId, row.name),
    });
  }
  if (canFileDownload.value) {
    actions.push({
      key: "download",
      label: "下载",
      handler: () => downloadLogicalFile(row.logicalFileId, `${row.name}.sql`),
    });
  }
  if (canDdlEdit.value) {
    actions.push({
      key: "edit",
      label: "编辑",
      handler: () => openEditDialog("DDL", row.id, row.name, row.remark),
    });
  }
  if (canDdlDelete.value) {
    actions.push({
      key: "delete",
      label: "删除",
      type: "danger",
      handler: () => removeDdl(row.id, row.name),
    });
  }
  return actions;
}

function getPrimaryDdlActions(row: SchemaDdlItem): RowAction[] {
  const actions = getDdlActions(row);
  if (actions.length <= 3) return actions;
  return actions.slice(0, 2);
}

function getExtraDdlActions(row: SchemaDdlItem): RowAction[] {
  const actions = getDdlActions(row);
  if (actions.length <= 3) return [];
  return actions.slice(2);
}

function getSnapshotObjectDisplayName(item: SnapshotSelectableObject): string {
  const tableName = (item.tableName || "").trim();
  const alias = (item.alias || "").trim();
  if (!alias || alias === tableName) {
    return tableName;
  }
  return `${tableName}（${alias}）`;
}

function resolveSnapshotPreviewFileId(item: SchemaSnapshotItem): string {
  return item.sqlLogicalFileId || item.logicalFileId;
}

async function loadManagedDatabaseOptions(initializeDiffDefaults = true): Promise<boolean> {
  try {
    managedDatabases.value = await listDatabaseSchemas();
    if (initializeDiffDefaults && comparableManagedDatabases.value.length > 0) {
      diffForm.refDbId = comparableManagedDatabases.value[0].id;
      diffForm.targetDbId =
        comparableManagedDatabases.value.length > 1
          ? comparableManagedDatabases.value[1].id
          : comparableManagedDatabases.value[0].id;
    }

    if (initializeDiffDefaults && comparableManagedDatabases.value.length === 0) {
      diffForm.refDbId = undefined;
      diffForm.targetDbId = undefined;
    }
    return true;
  } catch (_err) {
    message.warning("数据库列表加载失败，筛选项暂不可用");
    managedDatabases.value = [];
    if (initializeDiffDefaults) {
      diffForm.refDbId = undefined;
      diffForm.targetDbId = undefined;
    }
    return false;
  }
}

async function loadDdlSnapshotOptions(): Promise<boolean> {
  try {
    ddlSnapshotOptions.value = await schemaforgeApi.listSchemaSnapshotsForDdl();
    return true;
  } catch (_err) {
    ddlSnapshotOptions.value = [];
    message.warning("快照列表加载失败，暂时无法新建DDL");
    return false;
  }
}

async function loadSnapshotSelectableObjects(managedDatabaseId: string): Promise<boolean> {
  snapshotObjectsLoading.value = true;
  try {
    const loader = schemaforgeApi.listSnapshotSelectableObjects;
    if (typeof loader !== "function") {
      message.warning("功能模块已更新，请刷新页面后重试");
      return false;
    }
    const objects = await loader(managedDatabaseId);
    snapshotObjects.value = objects;
    createDialog.selectedObjectIds = objects.map((item) => item.id);
    return true;
  } catch (_err) {
    snapshotObjects.value = [];
    createDialog.selectedObjectIds = [];
    message.warning("对象列表加载失败，请先刷新元数据后重试");
    return false;
  } finally {
    snapshotObjectsLoading.value = false;
  }
}

async function handleSnapshotDatabaseChange() {
  if (!createDialog.visible || createDialog.kind !== "SNAPSHOT") {
    return;
  }
  if (!createDialog.managedDatabaseId) {
    snapshotObjects.value = [];
    createDialog.selectedObjectIds = [];
    return;
  }
  await loadSnapshotSelectableObjects(createDialog.managedDatabaseId);
}

function selectAllSnapshotObjects() {
  createDialog.selectedObjectIds = snapshotObjects.value.map((item) => item.id);
}

function clearSnapshotObjects() {
  createDialog.selectedObjectIds = [];
}

async function loadSnapshotPage(pageNo?: number) {
  closePreview();
  if (!canSnapshotView.value) {
    snapshotPage.elements = [];
    snapshotPage.totalElements = 0;
    snapshotPage.totalPages = 0;
    snapshotPage.numberOfElements = 0;
    return;
  }
  if (pageNo) {
    snapshotQuery.pageNo = pageNo;
  }
  snapshotLoading.value = true;
  try {
    const result = await schemaforgeApi.pageSchemaSnapshots({
      managedDatabaseId: snapshotQuery.managedDatabaseId,
      nameLike: snapshotQuery.nameLike,
      page: {
        pageNo: snapshotQuery.pageNo,
        pageSize: snapshotQuery.pageSize,
      },
    });
    Object.assign(snapshotPage, result);
  } finally {
    snapshotLoading.value = false;
  }
}

async function loadDdlPage(pageNo?: number) {
  closePreview();
  if (!canDdlView.value) {
    ddlPage.elements = [];
    ddlPage.totalElements = 0;
    ddlPage.totalPages = 0;
    ddlPage.numberOfElements = 0;
    return;
  }
  if (pageNo) {
    ddlQuery.pageNo = pageNo;
  }
  ddlLoading.value = true;
  try {
    const result = await schemaforgeApi.pageSchemaDdls({
      managedDatabaseId: ddlQuery.managedDatabaseId,
      nameLike: ddlQuery.nameLike,
      page: {
        pageNo: ddlQuery.pageNo,
        pageSize: ddlQuery.pageSize,
      },
    });
    Object.assign(ddlPage, result);
  } finally {
    ddlLoading.value = false;
  }
}

function resetSnapshotFilters() {
  snapshotQuery.managedDatabaseId = undefined;
  snapshotQuery.nameLike = "";
  snapshotQuery.pageNo = 1;
  loadSnapshotPage(1);
}

function resetDdlFilters() {
  ddlQuery.managedDatabaseId = undefined;
  ddlQuery.nameLike = "";
  ddlQuery.pageNo = 1;
  loadDdlPage(1);
}

async function openCreateDialog(kind: EditKind) {
  if (kind === "SNAPSHOT") {
    const loaded = await loadManagedDatabaseOptions(false);
    if (!loaded) {
      return;
    }

    if (creatableManagedDatabases.value.length === 0) {
      message.warning("暂无可用数据库，请先在数据库管理中绑定数据源");
      return;
    }
  } else {
    const loaded = await loadDdlSnapshotOptions();
    if (!loaded) {
      return;
    }
    if (ddlSnapshotOptions.value.length === 0) {
      message.warning("暂无快照记录，请先生成目标快照");
      return;
    }
  }

  createDialog.visible = true;
  createDialog.kind = kind;
  createDialog.managedDatabaseId =
    kind === "SNAPSHOT" ? creatableManagedDatabases.value[0].id : undefined;
  createDialog.selectedObjectIds = [];
  createDialog.sourceSnapshotId = undefined;
  createDialog.targetSnapshotId = kind === "DDL" ? ddlSnapshotOptions.value[0]?.id : undefined;
  createDialog.name = "";
  createDialog.remark = "";
  createDialog.saving = false;
  snapshotObjectKeyword.value = "";
  snapshotObjects.value = [];
  if (kind === "SNAPSHOT" && createDialog.managedDatabaseId) {
    const loaded = await loadSnapshotSelectableObjects(createDialog.managedDatabaseId);
    if (!loaded) {
      closeCreateDialog();
      return;
    }
    if (snapshotObjects.value.length === 0) {
      closeCreateDialog();
      message.warning("当前数据库暂无可选表或视图，请先刷新元数据");
      return;
    }
  }
}

function closeCreateDialog() {
  createDialog.visible = false;
  createDialog.managedDatabaseId = undefined;
  createDialog.selectedObjectIds = [];
  createDialog.sourceSnapshotId = undefined;
  createDialog.targetSnapshotId = undefined;
  createDialog.name = "";
  createDialog.remark = "";
  createDialog.saving = false;
  snapshotObjectKeyword.value = "";
  snapshotObjects.value = [];
  snapshotObjectsLoading.value = false;
}

async function submitCreate() {
  if (createDialog.kind === "SNAPSHOT" && !createDialog.managedDatabaseId) {
    message.warning("请选择数据库");
    return;
  }
  if (createDialog.kind === "SNAPSHOT" && createDialog.selectedObjectIds.length === 0) {
    message.warning("请至少选择一个表或视图");
    return;
  }
  if (createDialog.kind === "DDL" && !createDialog.targetSnapshotId) {
    message.warning("请选择目标快照");
    return;
  }
  if (
    createDialog.kind === "DDL" &&
    createDialog.sourceSnapshotId === createDialog.targetSnapshotId
  ) {
    message.warning("起始快照和目标快照不能相同");
    return;
  }

  const trimmedName = createDialog.name.trim();
  if (!trimmedName) {
    message.warning("名称不能为空");
    return;
  }

  createDialog.saving = true;
  try {
    if (createDialog.kind === "SNAPSHOT") {
      await schemaforgeApi.createSchemaSnapshot({
        managedDatabaseId: createDialog.managedDatabaseId!,
        name: trimmedName,
        remark: createDialog.remark.trim() || undefined,
        selectedObjectIds: createDialog.selectedObjectIds,
      });
      message.success("快照创建成功");
      await loadSnapshotPage(1);
    } else {
      await schemaforgeApi.createSchemaDdl({
        sourceSnapshotId: createDialog.sourceSnapshotId,
        targetSnapshotId: createDialog.targetSnapshotId!,
        name: trimmedName,
        remark: createDialog.remark.trim() || undefined,
      });
      message.success("DDL创建成功");
      await loadDdlPage(1);
    }
    closeCreateDialog();
  } catch (err) {
    createDialog.saving = false;
    message.error(extractErrorMessage(err, "创建失败"));
  }
}

function swapDiffDatabases() {
  const currentRef = diffForm.refDbId;
  diffForm.refDbId = diffForm.targetDbId;
  diffForm.targetDbId = currentRef;
}

function resetDiffFilters() {
  diffResult.value = null;
  diffCompared.refDbId = undefined;
  diffCompared.targetDbId = undefined;
  if (comparableManagedDatabases.value.length === 0) {
    diffForm.refDbId = undefined;
    diffForm.targetDbId = undefined;
    return;
  }
  diffForm.refDbId = comparableManagedDatabases.value[0].id;
  diffForm.targetDbId =
    comparableManagedDatabases.value.length > 1
      ? comparableManagedDatabases.value[1].id
      : comparableManagedDatabases.value[0].id;
}

async function runDiffCompare() {
  if (!canDiffView.value) {
    return;
  }
  if (!diffForm.refDbId || !diffForm.targetDbId) {
    message.warning("请选择起始数据库和目标数据库");
    return;
  }
  if (diffForm.refDbId === diffForm.targetDbId) {
    message.warning("起始数据库与目标数据库不能相同");
    return;
  }

  diffLoading.value = true;
  try {
    diffResult.value = await schemaforgeApi.compareSchemaDiff({
      refDbId: diffForm.refDbId,
      targetDbId: diffForm.targetDbId,
    });
    diffCompared.refDbId = diffForm.refDbId;
    diffCompared.targetDbId = diffForm.targetDbId;
    message.success("结构对比完成");
  } catch (err) {
    message.error(extractErrorMessage(err, "结构对比失败"));
  } finally {
    diffLoading.value = false;
  }
}

async function previewLogicalFile(fileId: string, title: string) {
  if (!fileId) {
    message.warning("文件不存在或尚未生成");
    return;
  }
  if (!canFileView.value) {
    message.warning("无文件预览权限");
    return;
  }
  closePreview();
  try {
    const [fileMeta, blob] = await Promise.all([
      getSystemFileMeta(fileId),
      fetchSystemFileView(fileId),
    ]);
    const text = await blob.text();
    const MAX_PREVIEW_LENGTH = 200000;
    preview.visible = true;
    preview.title = title;
    preview.fileMeta = fileMeta;
    preview.truncated = text.length > MAX_PREVIEW_LENGTH;
    preview.fullContent = text;
    preview.content = preview.truncated ? text.slice(0, MAX_PREVIEW_LENGTH) : text;
  } catch (err) {
    message.error(extractErrorMessage(err, "预览文件失败"));
  }
}

async function copyPreviewContent() {
  if (!canCopyPreview.value) {
    return;
  }
  try {
    await navigator.clipboard.writeText(preview.fullContent);
    message.success("已复制全部文本内容");
  } catch (_err) {
    message.warning("复制失败，请手动复制");
  }
}

function closePreview() {
  preview.visible = false;
  preview.title = "";
  preview.content = "";
  preview.truncated = false;
  preview.fullContent = "";
  preview.fileMeta = null;
}

async function downloadLogicalFile(fileId: string, fallbackName: string) {
  if (!fileId) {
    message.warning("文件不存在或尚未生成");
    return;
  }
  if (!canFileDownload.value) {
    message.warning("无文件下载权限");
    return;
  }
  try {
    const result = await downloadSystemFile(fileId, fallbackName);
    const url = URL.createObjectURL(result.blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = result.fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    message.error(extractErrorMessage(err, "下载文件失败"));
  }
}

function openEditDialog(kind: EditKind, id: string, name: string, remark?: string) {
  editDialog.kind = kind;
  editDialog.id = id;
  editDialog.name = name;
  editDialog.remark = remark || "";
  editDialog.saving = false;
  editDialog.visible = true;
}

function closeEditDialog() {
  editDialog.visible = false;
  editDialog.id = "";
  editDialog.name = "";
  editDialog.remark = "";
  editDialog.saving = false;
}

async function submitEdit() {
  const trimmedName = editDialog.name.trim();
  if (!trimmedName) {
    message.warning("名称不能为空");
    return;
  }

  editDialog.saving = true;
  try {
    if (editDialog.kind === "SNAPSHOT") {
      await schemaforgeApi.updateSchemaSnapshot(editDialog.id, {
        name: trimmedName,
        remark: editDialog.remark.trim() || undefined,
      });
      message.success("快照更新成功");
      await loadSnapshotPage();
    } else {
      await schemaforgeApi.updateSchemaDdl(editDialog.id, {
        name: trimmedName,
        remark: editDialog.remark.trim() || undefined,
      });
      message.success("DDL更新成功");
      await loadDdlPage();
    }
    closeEditDialog();
  } catch (err) {
    editDialog.saving = false;
    message.error(extractErrorMessage(err, "保存失败"));
  }
}

async function removeSnapshot(id: string, name: string) {
  const confirmed = await bzConfirm({
    title: "删除快照",
    message: `确定删除快照“${name}”吗？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  try {
    await schemaforgeApi.deleteSchemaSnapshot(id);
    message.success("删除成功");
    await loadSnapshotPage();
  } catch (err) {
    message.error(extractErrorMessage(err, "删除失败"));
  }
}

async function removeDdl(id: string, name: string) {
  const confirmed = await bzConfirm({
    title: "删除DDL",
    message: `确定删除DDL“${name}”吗？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  try {
    await schemaforgeApi.deleteSchemaDdl(id);
    message.success("删除成功");
    await loadDdlPage();
  } catch (err) {
    message.error(extractErrorMessage(err, "删除失败"));
  }
}

function extractErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return fallback;
}

onMounted(async () => {
  await loadManagedDatabaseOptions();
  await Promise.all([loadSnapshotPage(), loadDdlPage()]);
});

watch(
  () => route.query.tab,
  (tab) => {
    const nextTab = resolveTabFromQuery(tab);
    if (nextTab !== activeTab.value) {
      activeTab.value = nextTab;
    }
  },
);

watch(
  activeTab,
  (tab) => {
    const raw = Array.isArray(route.query.tab) ? route.query.tab[0] : route.query.tab;
    if (raw !== tab) {
      const query = {
        ...route.query,
        tab,
      };
      void router.replace({ query });
    }
    closePreview();
  },
  { immediate: true },
);
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

.tabs {
  margin-bottom: 12px;
}

.truncate {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.board {
  overflow: hidden;
}

.primary-text {
  font-weight: 700;
}

.minor-text {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

.preview {
  margin-top: 12px;
}

.preview :deep(.bz-card__body) {
  padding: 12px;
}

.preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.preview-title {
  font-weight: 700;
}

.preview-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.preview-content {
  margin: 0;
  padding: 12px;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  overflow: auto;
  max-height: 55vh;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.5;
}

.object-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.object-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  width: 100%;
}

.object-toolbar :deep(.bz-input) {
  flex: 1;
  min-width: 240px;
}

.object-list {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  max-height: 240px;
  overflow-y: auto;
  padding: 8px;
  background: #fafcff;
  width: 100%;
}

.object-list :deep(.bz-checkbox-group) {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
}

.object-list :deep(.bz-checkbox) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 4px;
  width: 100%;
}

.object-type-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-size: 11px;
  font-weight: 700;
  border-radius: 6px;
  border: 1px solid transparent;
  flex: 0 0 auto;
}

.object-type-tag.table {
  color: #1e40af;
  border-color: #93c5fd;
  background: #eff6ff;
}

.object-type-tag.view {
  color: #065f46;
  border-color: #6ee7b7;
  background: #ecfdf5;
}

.object-name {
  font-size: 13px;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .content {
    padding: 12px;
  }

  .tabs {
    overflow-x: auto;
    white-space: nowrap;
  }

  .object-name {
    white-space: normal;
    word-break: break-all;
  }
}
</style>
