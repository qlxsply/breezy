<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section class="list-page-actions">
          <div class="list-page-actions-main">
            <bz-button
              v-if="canUse"
              type="primary"
              @click="reload"
              >查询</bz-button
            >
            <bz-button
              v-if="canUse"
              @click="reset"
              >重置</bz-button
            >
            <bz-button @click="backBoard">返回看板</bz-button>
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
                  v-model="keyword"
                  class="list-page-filter-control"
                  placeholder="事项内容关键词"
                  clearable
                />
              </div>
            </bz-form-item>
            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">状态</div>
                <bz-select
                  v-model="status"
                  class="list-page-filter-control"
                  clearable
                  placeholder="全部状态"
                >
                  <bz-option
                    v-for="option in statusOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </bz-select>
              </div>
            </bz-form-item>
          </bz-form>
        </bz-card>

        <bz-card class="list-page-result-card">
          <bz-empty
            v-if="!canUse"
            description="无权限访问待办事项"
          />
          <bz-table
            v-else
            v-loading="loading"
            :data="rows"
            empty-text="暂无事项"
            size="small"
            @row-dblclick="handleRowDblClick"
          >
            <bz-table-column
              label="状态"
              width="92"
            >
              <template #default="scope">
                <bz-tag
                  size="small"
                  :type="statusTagType(scope.row.status)"
                  >{{ statusText(scope.row.status) }}</bz-tag
                >
              </template>
            </bz-table-column>
            <bz-table-column
              label="内容"
              min-width="280"
            >
              <template #default="scope">
                <div class="content-cell">{{ scope.row.content || "-" }}</div>
              </template>
            </bz-table-column>
            <bz-table-column
              label="创建时间"
              width="180"
            >
              <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
            </bz-table-column>
            <bz-table-column
              label="截止时间"
              width="180"
            >
              <template #default="scope">{{ formatDateTime(scope.row.dueTime) }}</template>
            </bz-table-column>
            <bz-table-column
              label="完成时间"
              width="180"
            >
              <template #default="scope">{{ formatDateTime(scope.row.completedAt) }}</template>
            </bz-table-column>
          </bz-table>

          <div
            v-if="canUse && total > pageSize"
            class="pagination"
          >
            <bz-pagination
              background
              layout="total, prev, pager, next"
              :total="total"
              :page-size="pageSize"
              :current-page="pageNo"
              @current-change="changePage"
            />
          </div>
        </bz-card>

        <TodoDetailDialog
          v-model="detailVisible"
          :todo="detailTodo"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import { listDictOptions } from "../api/dicts";
import { listTodos } from "../api/todo";
import TodoDetailDialog from "../components/todo/TodoDetailDialog.vue";
import { hasUserPermissionCode } from "../registry/user-tool-permissions.registry";
import type { DictItem } from "../types/dict-admin";
import type { TodoItem, TodoStatus } from "../types/todo";
import { formatDateTime } from "../utils/formatter";
import { message } from "../utils/message";

const router = useRouter();
const canUse = computed(() => hasUserPermissionCode("tdo.use"));

const loading = ref(false);
const rows = ref<TodoItem[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = 20;
const todoStatusItems = ref<DictItem[]>([]);

const keyword = ref("");
const status = ref<TodoStatus | "">("");
const detailVisible = ref(false);
const detailTodo = ref<TodoItem | null>(null);

onMounted(() => {
  void loadStatusDict();
  void reload();
});

const statusOptions = computed(() =>
  todoStatusItems.value.map((item) => ({
    label: item.itemLabel,
    value: item.itemValue,
  })),
);

async function loadStatusDict() {
  try {
    todoStatusItems.value = await listDictOptions("TODO_TASK_STATUS");
  } catch {
    todoStatusItems.value = [];
  }
}

async function reload() {
  if (!canUse.value) {
    rows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const page = await listTodos({
      page: {
        pageNo: pageNo.value,
        pageSize,
      },
      status: status.value || null,
      contentLike: keyword.value.trim() || undefined,
    });
    rows.value = page.elements;
    total.value = page.totalElements;
  } catch (error) {
    if (error instanceof Error && error.message.trim().length > 0) {
      message.error(error.message);
      return;
    }
    message.error("事项加载失败");
  } finally {
    loading.value = false;
  }
}

function reset() {
  keyword.value = "";
  status.value = "";
  pageNo.value = 1;
  void reload();
}

function changePage(next: number) {
  pageNo.value = next;
  void reload();
}

function backBoard() {
  void router.push({ path: "/todo" });
}

function handleRowDblClick(row: unknown) {
  const todo = row as TodoItem;
  detailTodo.value = todo;
  detailVisible.value = true;
}

function statusText(value: TodoStatus): string {
  return todoStatusItems.value.find((item) => item.itemValue === value)?.itemLabel || value;
}

function statusTagType(value: TodoStatus): "info" | "warning" | "success" {
  const tagType = todoStatusItems.value.find((item) => item.itemValue === value)?.tagType || "info";
  if (tagType === "warning" || tagType === "success") {
    return tagType;
  }
  return "info";
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

.content-cell {
  line-height: 1.5;
}
</style>
