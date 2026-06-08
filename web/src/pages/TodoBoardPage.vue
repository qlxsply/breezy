<template>
  <div class="app-shell">
    <div class="content">
      <bz-card
        v-if="canUse"
        class="stats-card"
        shadow="never"
      >
        <div class="stats-head">
          <div class="stats-actions">
            <bz-tooltip
              content="新增"
              placement="top"
            >
              <bz-icon-action-button
                icon="plus"
                tone="primary"
                @click="openCreateDialog"
              />
            </bz-tooltip>
            <bz-tooltip
              content="刷新"
              placement="top"
            >
              <bz-icon-action-button
                icon="refresh"
                :disabled="loading"
                @click="reloadAll"
              />
            </bz-tooltip>
            <bz-tooltip
              content="全部查看"
              placement="top"
            >
              <bz-icon-action-button
                icon="list"
                @click="goAllItems"
              />
            </bz-tooltip>
          </div>

          <div class="stats-strip">
            <div class="stats-pill stats-pill--all">
              <span>总数</span><strong>{{ stats.total }}</strong>
            </div>
            <div class="stats-pill stats-pill--todo">
              <span>待办</span><strong>{{ stats.todo }}</strong>
            </div>
            <div class="stats-pill stats-pill--paused">
              <span>暂停</span><strong>{{ stats.paused }}</strong>
            </div>
            <div class="stats-pill stats-pill--done">
              <span>完成</span><strong>{{ stats.done }}</strong>
            </div>
          </div>
        </div>
      </bz-card>

      <div
        v-loading="loading"
        class="board"
      >
        <bz-empty
          v-if="!canUse"
          description="无权限访问"
        />
        <template v-else>
          <section
            v-for="column in visibleColumns"
            :key="column.status"
            class="board-column"
            :class="`board-column--${column.status.toLowerCase()}`"
            @dragover.prevent
            @drop.prevent="handleColumnDrop(column.status)"
          >
            <div class="board-column__head">
              <div class="board-column__title">{{ column.title }}</div>
              <span class="board-column__count">{{ column.items.length }}</span>
            </div>

            <bz-empty
              v-if="column.items.length === 0"
              description="暂无事项"
            />

            <div
              v-else
              class="board-column__list"
            >
              <article
                v-for="todo in column.items"
                :key="todo.id"
                class="todo-card"
                :class="{
                  'is-selected': selectedTodoId === todo.id,
                  'is-dragging': dragSourceId === todo.id,
                  'is-dragover': dragOverId === todo.id,
                  'has-due': todo.status === 'TODO' && Boolean(todo.dueTime),
                }"
                :draggable="todo.status !== 'DONE'"
                @click="selectTodo(todo.id)"
                @dblclick="openDetail(todo)"
                @dragstart="handleDragStart(todo)"
                @dragend="clearDragState"
                @dragover.prevent="handleCardDragOver(column.status, todo.id)"
                @drop.prevent="handleCardDrop(column.status, todo.id)"
              >
                <div
                  v-if="todo.status === 'TODO' && todo.dueTime"
                  class="todo-card__due"
                  :style="{ color: dueToneColor(todo.dueTime) }"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    aria-hidden="true"
                    class="todo-card__due-icon"
                  >
                    <circle
                      cx="12"
                      cy="13"
                      r="7"
                      stroke="currentColor"
                      stroke-width="1.8"
                    />
                    <path
                      d="M12 13V9"
                      stroke="currentColor"
                      stroke-width="1.8"
                      stroke-linecap="round"
                    />
                    <path
                      d="M12 13L15 15"
                      stroke="currentColor"
                      stroke-width="1.8"
                      stroke-linecap="round"
                    />
                    <path
                      d="M8 4.5L6.5 3"
                      stroke="currentColor"
                      stroke-width="1.8"
                      stroke-linecap="round"
                    />
                    <path
                      d="M16 4.5L17.5 3"
                      stroke="currentColor"
                      stroke-width="1.8"
                      stroke-linecap="round"
                    />
                  </svg>
                  <span class="todo-card__due-text">{{ formatDateTime(todo.dueTime) }}</span>
                </div>

                <div
                  class="todo-card__menu"
                  @click.stop
                >
                  <bz-dropdown trigger="click">
                    <bz-icon-action-button
                      icon="more"
                      :size="26"
                    />
                    <template #dropdown>
                      <bz-dropdown-menu>
                        <bz-dropdown-item @click="openEditDialog(todo)">编辑</bz-dropdown-item>
                        <bz-dropdown-item
                          v-if="todo.status === 'DONE'"
                          :disabled="reopeningTodoId === todo.id"
                          @click="reopenTodo(todo)"
                        >
                          重新打开
                        </bz-dropdown-item>
                        <bz-dropdown-item
                          v-if="todo.status === 'TODO'"
                          @click="pauseItem(todo)"
                          >暂停</bz-dropdown-item
                        >
                        <bz-dropdown-item
                          v-if="todo.status === 'PAUSED'"
                          @click="resumeItem(todo)"
                          >恢复</bz-dropdown-item
                        >
                        <bz-dropdown-item
                          v-if="todo.status !== 'DONE'"
                          @click="quickCompleteItem(todo)"
                          >快速完成</bz-dropdown-item
                        >
                        <bz-dropdown-item
                          v-if="todo.status !== 'DONE'"
                          @click="openCompleteDialog(todo)"
                          >补充完成</bz-dropdown-item
                        >
                        <bz-dropdown-item @click="removeItem(todo)">删除</bz-dropdown-item>
                      </bz-dropdown-menu>
                    </template>
                  </bz-dropdown>
                </div>

                <div
                  v-if="todo.content"
                  class="todo-card__content"
                >
                  {{ todo.content }}
                </div>
                <div
                  v-if="cardSecondaryText(todo)"
                  class="todo-card__note"
                >
                  {{ cardSecondaryText(todo) }}
                </div>

                <div
                  v-if="cardAttachmentThumbs(todo).length > 0"
                  class="thumb-row"
                >
                  <div
                    v-for="item in cardAttachmentThumbs(todo)"
                    :key="item.key"
                    class="thumb-box"
                    :class="{ 'thumb-box--more': item.isMore }"
                  >
                    <img
                      v-if="!item.isMore && item.previewUrl"
                      :src="item.previewUrl"
                      :alt="item.name"
                      class="thumb-box__img"
                      @dblclick.stop="openImagePreview(item.previewUrl, item.name)"
                    />
                    <span
                      v-else
                      class="thumb-box__fallback"
                      >{{ item.isMore ? `+${item.moreCount}` : "图" }}</span
                    >
                  </div>
                </div>
              </article>
            </div>
          </section>
        </template>
      </div>

      <bz-dialog
        v-model="editorVisible"
        :title="editorMode === 'create' ? '新增事项' : '编辑事项'"
        width="760px"
        @close="closeEditorDialog"
      >
        <div class="dialog-body">
          <bz-form @submit.prevent>
            <bz-form-item
              label="事项内容"
              label-width="90px"
            >
              <bz-text-field
                v-model="editorForm.content"
                type="textarea"
                :rows="3"
                placeholder="输入事项内容"
                :maxlength="TODO_TEXT_MAX_LENGTH"
                show-counter
              />
            </bz-form-item>
            <bz-form-item
              label="截止时间"
              label-width="90px"
            >
              <bz-date-picker
                v-model="editorForm.dueTime"
                type="datetime"
                clearable
                value-format="YYYY-MM-DDTHH:mm"
              />
            </bz-form-item>
            <bz-form-item
              label="备注"
              label-width="90px"
            >
              <bz-text-field
                v-model="editorForm.note"
                type="textarea"
                :rows="2"
                placeholder="备注"
                :maxlength="TODO_TEXT_MAX_LENGTH"
                show-counter
              />
            </bz-form-item>
          </bz-form>

          <TodoAttachmentField
            title="附件"
            :items="editorPreviewItems"
            :disabled="editorSubmitting"
            :uploading="editorUploading"
            @files="handleEditorAttachmentFiles"
            @remove="removeEditorAttachment"
          />
        </div>
        <template #footer>
          <div class="dialog-footer-actions">
            <bz-tooltip
              content="取消"
              placement="top"
            >
              <bz-icon-action-button
                icon="close"
                :disabled="editorSubmitting"
                @click="closeEditorDialog"
              />
            </bz-tooltip>
            <bz-tooltip
              content="保存"
              placement="top"
            >
              <bz-icon-action-button
                icon="save"
                tone="primary"
                :disabled="editorSubmitting || editorTextTooLong"
                @click="submitEditorDialog"
              />
            </bz-tooltip>
          </div>
        </template>
      </bz-dialog>

      <bz-dialog
        v-model="completeVisible"
        title="补充完成"
        width="680px"
        @close="closeCompleteDialog"
      >
        <div class="dialog-body">
          <div
            v-if="completingTodo"
            class="complete-summary"
          >
            {{ completingTodo.content || "无内容事项" }}
          </div>
          <bz-form @submit.prevent>
            <bz-form-item
              label="完成补充"
              label-width="90px"
            >
              <bz-text-field
                v-model="completeForm.completionNote"
                type="textarea"
                :rows="3"
                placeholder="补充完成说明"
                :maxlength="TODO_TEXT_MAX_LENGTH"
                show-counter
              />
            </bz-form-item>
          </bz-form>
          <TodoAttachmentField
            title="附件"
            :items="completePreviewItems"
            :disabled="completeSubmitting"
            :uploading="completeUploading"
            @files="handleCompleteFiles"
            @remove="removeCompleteAttachment"
          />
        </div>
        <template #footer>
          <div class="dialog-footer-actions">
            <bz-tooltip
              content="取消"
              placement="top"
            >
              <bz-icon-action-button
                icon="close"
                :disabled="completeSubmitting"
                @click="closeCompleteDialog"
              />
            </bz-tooltip>
            <bz-tooltip
              content="完成"
              placement="top"
            >
              <bz-icon-action-button
                icon="check"
                tone="primary"
                :disabled="completeSubmitting || completionNoteTooLong"
                @click="submitCompleteDialog"
              />
            </bz-tooltip>
          </div>
        </template>
      </bz-dialog>

      <TodoDetailDialog
        v-model="detailVisible"
        :todo="detailTodo"
      />
      <TodoImagePreviewDialog
        v-model="previewVisible"
        :src="previewSrc"
        :name="previewName"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";

import {
  completeTodo,
  createTodo,
  deleteTodo,
  fetchTodoAttachmentView,
  getTodoAttachmentMetadata,
  listTodos,
  pauseTodo,
  quickCompleteTodo,
  reorderTodos,
  resumeTodo,
  updateTodo,
  uploadTodoAttachment,
} from "../api/todo";
import TodoAttachmentField from "../components/todo/TodoAttachmentField.vue";
import TodoDetailDialog from "../components/todo/TodoDetailDialog.vue";
import TodoImagePreviewDialog from "../components/todo/TodoImagePreviewDialog.vue";
import { hasApiPermission } from "../registry/permissions.registry";
import { clearPageShortcuts, setPageShortcuts } from "../registry/shortcuts.registry";
import type {
  TodoAttachmentDraftItem,
  TodoAttachmentMeta,
  TodoAttachmentPreviewItem,
  TodoCompleteReq,
  TodoItem,
  TodoReorderReq,
  TodoSaveReq,
  TodoStatus,
} from "../types/todo";
import { bzConfirm } from "../utils/confirm";
import {
  dateTimeInputToEpochMillisString,
  epochMillisStringToDateTimeInput,
  formatDateTime,
} from "../utils/formatter";
import { message } from "../utils/message";

type EditorMode = "create" | "edit";

interface BoardColumn {
  status: TodoStatus;
  title: string;
  items: TodoItem[];
}

interface CardThumbItem {
  key: string;
  name: string;
  previewUrl?: string;
  isMore: boolean;
  moreCount: number;
}

const TODO_TEXT_MAX_LENGTH = 2000;

const loading = ref(false);
const todos = ref<TodoItem[]>([]);
const selectedTodoId = ref<number | null>(null);

const editorVisible = ref(false);
const editorMode = ref<EditorMode>("create");
const editorSubmitting = ref(false);
const editorUploading = ref(false);
const editingTodoId = ref<number | null>(null);

const completeVisible = ref(false);
const completeSubmitting = ref(false);
const completeUploading = ref(false);
const completingTodoId = ref<number | null>(null);
const reopeningTodoId = ref<number | null>(null);
const detailVisible = ref(false);
const detailTodo = ref<TodoItem | null>(null);
const previewVisible = ref(false);
const previewSrc = ref("");
const previewName = ref("");

const dragSourceId = ref<number | null>(null);
const dragOverId = ref<number | null>(null);

const savedAttachmentMetaMap = reactive<Record<string, TodoAttachmentMeta>>({});
const savedAttachmentUrlMap = reactive<Record<string, string>>({});
const uploadedBlobUrls = new Set<string>();

const editorForm = reactive({
  content: "",
  dueTime: "",
  note: "",
  savedIds: [] as string[],
  drafts: [] as TodoAttachmentDraftItem[],
});

const completeForm = reactive({
  completionNote: "",
  savedIds: [] as string[],
  drafts: [] as TodoAttachmentDraftItem[],
});

const canUse = computed(() => hasApiPermission("tdo.use"));
const router = useRouter();

const groupedTodos = computed<Record<TodoStatus, TodoItem[]>>(() => ({
  TODO: todos.value.filter((item) => item.status === "TODO"),
  PAUSED: todos.value.filter((item) => item.status === "PAUSED"),
  DONE: todos.value.filter((item) => item.status === "DONE"),
}));

const visibleColumns = computed<BoardColumn[]>(() => [
  { status: "TODO", title: "待办", items: groupedTodos.value.TODO },
  { status: "PAUSED", title: "暂停", items: groupedTodos.value.PAUSED },
  { status: "DONE", title: "完成", items: groupedTodos.value.DONE },
]);

const stats = computed(() => ({
  total: todos.value.length,
  todo: groupedTodos.value.TODO.length,
  paused: groupedTodos.value.PAUSED.length,
  done: groupedTodos.value.DONE.length,
}));

const editorPreviewItems = computed(() =>
  buildAttachmentPreviewItems(editorForm.savedIds, editorForm.drafts),
);
const completePreviewItems = computed(() =>
  buildAttachmentPreviewItems(completeForm.savedIds, completeForm.drafts),
);
const completingTodo = computed(
  () => todos.value.find((item) => item.id === completingTodoId.value) ?? null,
);
const editorContentLength = computed(() => editorForm.content.length);
const editorNoteLength = computed(() => editorForm.note.length);
const completionNoteLength = computed(() => completeForm.completionNote.length);
const editorTextTooLong = computed(
  () =>
    editorContentLength.value > TODO_TEXT_MAX_LENGTH ||
    editorNoteLength.value > TODO_TEXT_MAX_LENGTH,
);
const completionNoteTooLong = computed(() => completionNoteLength.value > TODO_TEXT_MAX_LENGTH);

onMounted(() => {
  setPageShortcuts([
    { keys: "Alt + Q", action: "新增事项" },
    { keys: "Alt + W", action: "编辑当前事项" },
    { keys: "Alt + E", action: "快速完成当前事项" },
    { keys: "Alt + R", action: "补充完成" },
    { keys: "Alt + A", action: "暂停/恢复当前事项" },
    { keys: "Alt + S", action: "删除当前事项" },
    { keys: "Alt + D", action: "查看事项详情" },
    { keys: "Esc", action: "关闭弹窗" },
  ]);
  window.addEventListener("keydown", handleKeydown);
  void reloadAll();
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleKeydown);
  clearPageShortcuts();
  clearAttachmentDrafts(editorForm.drafts);
  clearAttachmentDrafts(completeForm.drafts);
  Object.values(savedAttachmentUrlMap).forEach(revokeBlobUrl);
});

async function reloadAll() {
  await loadTodos();
}

async function loadTodos() {
  if (!canUse.value) {
    todos.value = [];
    return;
  }
  loading.value = true;
  try {
    const page = await listTodos({
      page: {
        pageNo: 1,
        pageSize: 500,
      },
    });
    todos.value = page.elements;
    if (!selectedTodoId.value || !todos.value.some((item) => item.id === selectedTodoId.value)) {
      selectedTodoId.value = todos.value[0]?.id ?? null;
    }
    await ensureSavedAttachmentAssets(collectSavedAttachmentIds(todos.value));
  } catch (error) {
    message.error(extractErrorMessage(error, "待处理事项加载失败"));
  } finally {
    loading.value = false;
  }
}

function selectTodo(id: number) {
  selectedTodoId.value = id;
}

function openDetail(todo: TodoItem) {
  detailTodo.value = todo;
  detailVisible.value = true;
}

function openImagePreview(src?: string, name?: string) {
  if (!src) {
    return;
  }
  previewSrc.value = src;
  previewName.value = name || "附件图片";
  previewVisible.value = true;
}

function goAllItems() {
  void router.push({ path: "/todo/all" });
}

function openCreateDialog() {
  if (!canUse.value) {
    return;
  }
  resetEditorForm();
  editorMode.value = "create";
  editingTodoId.value = null;
  editorVisible.value = true;
}

function openEditDialog(todo: TodoItem) {
  if (!canUse.value) {
    return;
  }
  resetEditorForm();
  editorMode.value = "edit";
  editingTodoId.value = todo.id;
  editorForm.content = limitTextLength(todo.content ?? "");
  editorForm.dueTime = normalizeDateTimeInput(todo.dueTime);
  editorForm.note = limitTextLength(todo.note ?? "");
  editorForm.savedIds = mergedAttachmentIds(todo);
  editorVisible.value = true;
  void ensureSavedAttachmentAssets(editorForm.savedIds);
}

function closeEditorDialog() {
  editorVisible.value = false;
  resetEditorForm();
}

async function submitEditorDialog() {
  if (editorTextTooLong.value) {
    message.warning(`事项内容和备注不能超过${TODO_TEXT_MAX_LENGTH}字符`);
    return;
  }
  const trimmedContent = editorForm.content.trim();
  if (!trimmedContent && editorForm.savedIds.length === 0 && editorForm.drafts.length === 0) {
    message.warning("请填写内容或添加附件");
    return;
  }

  editorSubmitting.value = true;
  try {
    const uploadedIds = await uploadDraftAttachments(editorForm.drafts, "editor");
    const payload: TodoSaveReq = {
      content: trimmedContent,
      dueTime: normalizeApiDateTime(editorForm.dueTime),
      note: trimToNull(editorForm.note),
      contentAttachmentFileIds: [...editorForm.savedIds, ...uploadedIds],
      completionAttachmentFileIds: [],
    };

    if (editorMode.value === "create") {
      await createTodo(payload);
      message.success("事项创建成功");
    } else if (editingTodoId.value !== null) {
      await updateTodo(editingTodoId.value, payload);
      message.success("事项已更新");
    }

    closeEditorDialog();
    await reloadAll();
  } catch (error) {
    message.error(
      extractErrorMessage(error, editorMode.value === "create" ? "事项创建失败" : "事项更新失败"),
    );
  } finally {
    editorSubmitting.value = false;
  }
}

function openCompleteDialog(todo: TodoItem) {
  if (todo.status === "DONE") {
    return;
  }
  completingTodoId.value = todo.id;
  completeForm.completionNote = limitTextLength(todo.completionNote ?? "");
  completeForm.savedIds = [...todo.completionAttachmentFileIds];
  clearAttachmentDrafts(completeForm.drafts);
  completeForm.drafts = [];
  completeVisible.value = true;
  void ensureSavedAttachmentAssets(completeForm.savedIds);
}

function closeCompleteDialog() {
  completeVisible.value = false;
  completingTodoId.value = null;
  completeForm.completionNote = "";
  completeForm.savedIds = [];
  clearAttachmentDrafts(completeForm.drafts);
  completeForm.drafts = [];
}

async function submitCompleteDialog() {
  if (completingTodoId.value === null) {
    return;
  }
  if (completionNoteTooLong.value) {
    message.warning(`补充完成说明不能超过${TODO_TEXT_MAX_LENGTH}字符`);
    return;
  }
  completeSubmitting.value = true;
  try {
    const uploadedIds = await uploadDraftAttachments(completeForm.drafts, "complete");
    const payload: TodoCompleteReq = {
      completionNote: trimToNull(completeForm.completionNote),
      completionAttachmentFileIds: [...completeForm.savedIds, ...uploadedIds],
    };
    await completeTodo(completingTodoId.value, payload);
    message.success("事项已完成");
    closeCompleteDialog();
    await reloadAll();
  } catch (error) {
    message.error(extractErrorMessage(error, "事项完成失败"));
  } finally {
    completeSubmitting.value = false;
  }
}

async function pauseItem(todo: TodoItem) {
  try {
    await pauseTodo(todo.id);
    message.success("事项已暂停");
    await loadTodos();
  } catch (error) {
    message.error(extractErrorMessage(error, "事项暂停失败"));
  }
}

async function resumeItem(todo: TodoItem) {
  try {
    await resumeTodo(todo.id);
    message.success("事项已恢复");
    await loadTodos();
  } catch (error) {
    message.error(extractErrorMessage(error, "事项恢复失败"));
  }
}

async function quickCompleteItem(todo: TodoItem) {
  try {
    await quickCompleteTodo(todo.id);
    message.success("事项已快速完成");
    await reloadAll();
  } catch (error) {
    message.error(extractErrorMessage(error, "事项完成失败"));
  }
}

async function reopenTodo(todo: TodoItem) {
  if (reopeningTodoId.value === todo.id) {
    return;
  }

  reopeningTodoId.value = todo.id;
  try {
    const req: TodoSaveReq = {
      content: todo.content ?? "",
      note: todo.note ?? "",
      dueTime: null,
      contentAttachmentFileIds: mergedAttachmentIds(todo),
      completionAttachmentFileIds: [],
    };
    const createdId = await createTodo(req);
    selectedTodoId.value = createdId;
    await reloadAll();
    message.success("已重新打开为新待办");
  } catch (error) {
    message.error(extractErrorMessage(error, "重新打开失败"));
  } finally {
    reopeningTodoId.value = null;
  }
}

async function removeItem(todo: TodoItem) {
  const confirmed = await bzConfirm({
    title: "删除事项",
    message: `确认删除事项“${todo.content || "无内容"}”吗？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }

  try {
    await deleteTodo(todo.id);
    message.success("事项已删除");
    await reloadAll();
  } catch (error) {
    message.error(extractErrorMessage(error, "事项删除失败"));
  }
}

function handleDragStart(todo: TodoItem) {
  if (todo.status === "DONE") {
    return;
  }
  dragSourceId.value = todo.id;
}

function handleCardDragOver(_status: TodoStatus, todoId: number) {
  if (!dragSourceId.value) {
    return;
  }
  dragOverId.value = todoId;
}

async function handleCardDrop(status: TodoStatus, targetId: number) {
  await persistReorder(status, targetId);
}

async function handleColumnDrop(status: TodoStatus) {
  await persistReorder(status, null);
}

async function persistReorder(status: TodoStatus, targetId: number | null) {
  if (!dragSourceId.value) {
    return;
  }
  const source = todos.value.find((item) => item.id === dragSourceId.value);
  if (!source || (source.status === status && targetId === source.id)) {
    clearDragState();
    return;
  }
  const payload = buildReorderPayload(dragSourceId.value, status, targetId);
  clearDragState();
  try {
    await reorderTodos(payload);
    await reloadAll();
  } catch (error) {
    message.error(extractErrorMessage(error, "事项排序更新失败"));
  }
}

function clearDragState() {
  dragSourceId.value = null;
  dragOverId.value = null;
}

function buildReorderPayload(
  sourceId: number,
  targetStatus: TodoStatus,
  targetId: number | null,
): TodoReorderReq {
  const groups: Record<TodoStatus, number[]> = {
    TODO: groupedTodos.value.TODO.map((item) => item.id),
    PAUSED: groupedTodos.value.PAUSED.map((item) => item.id),
    DONE: groupedTodos.value.DONE.map((item) => item.id),
  };

  (Object.keys(groups) as TodoStatus[]).forEach((status) => {
    groups[status] = groups[status].filter((id) => id !== sourceId);
  });

  const targetList = [...groups[targetStatus]];
  if (targetId === null) {
    targetList.push(sourceId);
  } else {
    const targetIndex = targetList.indexOf(targetId);
    if (targetIndex < 0) {
      targetList.push(sourceId);
    } else {
      targetList.splice(targetIndex, 0, sourceId);
    }
  }
  groups[targetStatus] = targetList;

  return {
    groups: (Object.keys(groups) as TodoStatus[])
      .filter((status) => groups[status].length > 0)
      .map((status) => ({ status, orderedIds: groups[status] })),
  };
}

function handleEditorAttachmentFiles(files: File[]) {
  if (files.length === 0) {
    return;
  }
  editorForm.drafts.push(...files.map(createDraftAttachment));
}

function removeEditorAttachment(item: TodoAttachmentPreviewItem) {
  if (item.source === "saved" && item.fileId) {
    editorForm.savedIds = editorForm.savedIds.filter((id) => id !== item.fileId);
    return;
  }
  if (item.localId) {
    removeDraftAttachment(editorForm.drafts, item.localId);
  }
}

function handleCompleteFiles(files: File[]) {
  if (files.length === 0) {
    return;
  }
  completeForm.drafts.push(...files.map(createDraftAttachment));
}

function removeCompleteAttachment(item: TodoAttachmentPreviewItem) {
  if (item.source === "saved" && item.fileId) {
    completeForm.savedIds = completeForm.savedIds.filter((id) => id !== item.fileId);
    return;
  }
  if (item.localId) {
    removeDraftAttachment(completeForm.drafts, item.localId);
  }
}

function mergedAttachmentPreviewItems(todo: TodoItem): TodoAttachmentPreviewItem[] {
  return savedPreviewItems(mergedAttachmentIds(todo));
}

function cardAttachmentThumbs(todo: TodoItem): CardThumbItem[] {
  const items = mergedAttachmentPreviewItems(todo);
  if (items.length <= 3) {
    return items.map((item) => ({
      key: item.key,
      name: item.name,
      previewUrl: item.previewUrl,
      isMore: false,
      moreCount: 0,
    }));
  }

  return [
    {
      key: items[0].key,
      name: items[0].name,
      previewUrl: items[0].previewUrl,
      isMore: false,
      moreCount: 0,
    },
    {
      key: items[1].key,
      name: items[1].name,
      previewUrl: items[1].previewUrl,
      isMore: false,
      moreCount: 0,
    },
    {
      key: `${todo.id}-more`,
      name: "更多附件",
      isMore: true,
      moreCount: items.length - 2,
    },
  ];
}

function savedPreviewItems(ids: string[]): TodoAttachmentPreviewItem[] {
  return ids
    .map((id) => toSavedPreviewItem(id))
    .filter((item): item is TodoAttachmentPreviewItem => Boolean(item));
}

function buildAttachmentPreviewItems(
  savedIds: string[],
  drafts: TodoAttachmentDraftItem[],
): TodoAttachmentPreviewItem[] {
  return [
    ...savedPreviewItems(savedIds),
    ...drafts.map((draft) => ({
      key: draft.localId,
      name: draft.name,
      previewUrl: draft.previewUrl,
      removable: true,
      source: "draft" as const,
      localId: draft.localId,
    })),
  ];
}

function toSavedPreviewItem(fileId: string): TodoAttachmentPreviewItem | null {
  const meta = savedAttachmentMetaMap[fileId];
  return {
    key: fileId,
    name: meta?.name || `附件 ${fileId.slice(0, 6)}`,
    previewUrl: savedAttachmentUrlMap[fileId],
    removable: true,
    source: "saved",
    fileId,
  };
}

async function ensureSavedAttachmentAssets(fileIds: string[]) {
  const ids = Array.from(new Set(fileIds.filter((id) => id.trim().length > 0)));
  if (ids.length === 0) {
    return;
  }

  const missingMetaIds = ids.filter((id) => !savedAttachmentMetaMap[id]);
  if (missingMetaIds.length > 0) {
    try {
      const metadata = await getTodoAttachmentMetadata(missingMetaIds);
      metadata.forEach((item) => {
        savedAttachmentMetaMap[item.id] = item;
      });
    } catch (error) {
      console.warn("[todo] attachment metadata load failed", error);
    }
  }

  await Promise.all(
    ids
      .filter((id) => !savedAttachmentUrlMap[id])
      .map(async (id) => {
        try {
          const blob = await fetchTodoAttachmentView(id);
          const url = URL.createObjectURL(blob);
          uploadedBlobUrls.add(url);
          savedAttachmentUrlMap[id] = url;
        } catch (error) {
          console.warn("[todo] attachment preview load failed", error);
        }
      }),
  );
}

async function uploadDraftAttachments(
  drafts: TodoAttachmentDraftItem[],
  stage: "editor" | "complete",
) {
  if (drafts.length === 0) {
    return [] as string[];
  }
  setUploadingState(stage, true);
  try {
    const uploadedIds = await Promise.all(drafts.map((draft) => uploadTodoAttachment(draft.file)));
    clearAttachmentDrafts(drafts);
    drafts.splice(0, drafts.length);
    await ensureSavedAttachmentAssets(uploadedIds);
    return uploadedIds;
  } finally {
    setUploadingState(stage, false);
  }
}

function setUploadingState(stage: "editor" | "complete", value: boolean) {
  if (stage === "editor") {
    editorUploading.value = value;
    return;
  }
  completeUploading.value = value;
}

function createDraftAttachment(file: File): TodoAttachmentDraftItem {
  const previewUrl = URL.createObjectURL(file);
  uploadedBlobUrls.add(previewUrl);
  return {
    localId: createLocalId(),
    file,
    name: file.name,
    previewUrl,
  };
}

function clearAttachmentDrafts(drafts: TodoAttachmentDraftItem[]) {
  drafts.forEach((draft) => revokeBlobUrl(draft.previewUrl));
}

function removeDraftAttachment(drafts: TodoAttachmentDraftItem[], localId: string) {
  const index = drafts.findIndex((item) => item.localId === localId);
  if (index < 0) {
    return;
  }
  revokeBlobUrl(drafts[index].previewUrl);
  drafts.splice(index, 1);
}

function handleKeydown(event: KeyboardEvent) {
  if (isInteractiveElement(event.target)) {
    return;
  }

  if (previewVisible.value || detailVisible.value || completeVisible.value || editorVisible.value) {
    if (event.key === "Escape") {
      return;
    }
    return;
  }

  if (event.altKey && event.key.toLowerCase() === "q") {
    event.preventDefault();
    openCreateDialog();
    return;
  }
  if (event.altKey && event.key.toLowerCase() === "w") {
    event.preventDefault();
    const todo = currentTodo();
    if (todo) {
      openEditDialog(todo);
    }
    return;
  }
  if (event.altKey && event.key.toLowerCase() === "r") {
    event.preventDefault();
    const todo = currentTodo();
    if (todo && todo.status !== "DONE") {
      openCompleteDialog(todo);
    }
    return;
  }
  if (event.altKey && event.key.toLowerCase() === "a") {
    event.preventDefault();
    const todo = currentTodo();
    if (!todo) {
      return;
    }
    if (todo.status === "TODO") {
      void pauseItem(todo);
    } else if (todo.status === "PAUSED") {
      void resumeItem(todo);
    }
    return;
  }
  if (event.altKey && event.key.toLowerCase() === "e") {
    event.preventDefault();
    const todo = currentTodo();
    if (todo && todo.status !== "DONE") {
      void quickCompleteItem(todo);
    }
    return;
  }
  if (event.altKey && event.key.toLowerCase() === "s") {
    const todo = currentTodo();
    if (todo) {
      event.preventDefault();
      void removeItem(todo);
    }
    return;
  }
  if (event.altKey && event.key.toLowerCase() === "d") {
    event.preventDefault();
    const todo = currentTodo();
    if (todo) {
      openDetail(todo);
    }
    return;
  }
  if (event.key === "Escape") {
    selectedTodoId.value = null;
  }
}

function currentTodo() {
  return todos.value.find((item) => item.id === selectedTodoId.value) ?? null;
}

function cardSecondaryText(todo: TodoItem): string {
  if (todo.status === "DONE") {
    const completion = (todo.completionNote || "").trim();
    if (completion.length > 0) {
      return completion;
    }
  }
  return (todo.note || "").trim();
}

function dueToneColor(dueTime?: string | null): string {
  const dueMillis = toEpochMillis(dueTime);
  if (dueMillis === null) {
    return "#64748b";
  }

  const diff = dueMillis - Date.now();
  if (diff < 0) {
    return "#dc2626";
  }

  const hours = diff / (60 * 60 * 1000);
  if (hours <= 6) {
    return "#ef4444";
  }
  if (hours <= 24) {
    return "#f97316";
  }
  if (hours <= 48) {
    return "#f59e0b";
  }
  return "#0ea5e9";
}

function toEpochMillis(value?: string | null): number | null {
  if (!value) {
    return null;
  }
  const text = value.trim();
  if (text.length === 0) {
    return null;
  }
  if (/^\d+$/.test(text)) {
    const millis = Number(text);
    return Number.isFinite(millis) ? millis : null;
  }
  const parsed = Date.parse(text);
  return Number.isNaN(parsed) ? null : parsed;
}

function resetEditorForm() {
  editorForm.content = "";
  editorForm.dueTime = "";
  editorForm.note = "";
  editorForm.savedIds = [];
  clearAttachmentDrafts(editorForm.drafts);
  editorForm.drafts = [];
}

function collectSavedAttachmentIds(items: TodoItem[]) {
  return items.flatMap((item) => mergedAttachmentIds(item));
}

function mergedAttachmentIds(todo: TodoItem): string[] {
  return Array.from(
    new Set([...todo.contentAttachmentFileIds, ...todo.completionAttachmentFileIds]),
  );
}

function limitTextLength(value: string): string {
  if (value.length <= TODO_TEXT_MAX_LENGTH) {
    return value;
  }
  return value.slice(0, TODO_TEXT_MAX_LENGTH);
}

function normalizeApiDateTime(value: string) {
  return dateTimeInputToEpochMillisString(value);
}

function normalizeDateTimeInput(value?: string | null) {
  return epochMillisStringToDateTimeInput(value);
}

function trimToNull(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function extractErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return fallback;
}

function isInteractiveElement(target: EventTarget | null) {
  const element = target instanceof HTMLElement ? target : null;
  if (!element) {
    return false;
  }
  const tagName = element.tagName.toLowerCase();
  return (
    tagName === "input" ||
    tagName === "textarea" ||
    tagName === "select" ||
    element.isContentEditable
  );
}

function createLocalId() {
  return `draft-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function revokeBlobUrl(url?: string) {
  if (!url || !uploadedBlobUrls.has(url)) {
    return;
  }
  URL.revokeObjectURL(url);
  uploadedBlobUrls.delete(url);
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
  min-height: 0;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  background: linear-gradient(180deg, #f8fbff 0%, #eef4fb 100%);
}

.stats-card {
  border-radius: 12px;
}

.stats-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stats-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.stats-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-left: auto;
  gap: 8px;
}

.stats-pill {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 112px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  background: #fff;
  padding: 6px 9px;
  font-size: 12px;
  color: #334155;
  font-weight: 700;
}

.stats-pill strong {
  color: #0f172a;
  font-size: 14px;
}

.stats-pill--todo {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.stats-pill--paused {
  border-color: #fed7aa;
  background: #fff7ed;
}

.stats-pill--done {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.board {
  flex: 1;
  min-height: 420px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.board-column {
  min-height: 260px;
  border-radius: 14px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.88);
}

.board-column--todo {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(239, 246, 255, 0.92));
}

.board-column--paused {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 247, 237, 0.92));
}

.board-column--done {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(240, 253, 244, 0.92));
}

.board-column__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.board-column__title {
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
}

.board-column__count {
  min-width: 24px;
  height: 24px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.16);
  color: #1d4ed8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
}

.board-column__list {
  display: grid;
  gap: 8px;
}

.todo-card {
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.18);
  padding: 8px 38px 8px 9px;
  display: grid;
  gap: 6px;
  cursor: pointer;
  position: relative;
}

.todo-card.has-due {
  padding-top: 28px;
}

.todo-card.is-selected {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

.todo-card.is-dragging {
  opacity: 0.52;
}

.todo-card.is-dragover {
  border-color: #0f766e;
}

.todo-card__menu {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 2;
}

.todo-card__due {
  position: absolute;
  left: 9px;
  top: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: calc(100% - 52px);
  font-size: 11px;
  font-weight: 700;
}

.todo-card__due-icon {
  width: 13px;
  height: 13px;
  flex: 0 0 auto;
}

.todo-card__due-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-card__note {
  color: #64748b;
  font-size: 11px;
}

.todo-card__content {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.todo-card__note {
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.thumb-row {
  display: flex;
  gap: 6px;
  flex-wrap: nowrap;
}

.thumb-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  overflow: hidden;
  background: #eff6ff;
  display: grid;
  place-items: center;
}

.thumb-box--more {
  background: #dbeafe;
}

.thumb-box__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-box__fallback {
  color: #475569;
  font-size: 10px;
  font-weight: 700;
}

.dialog-body {
  display: grid;
  gap: 12px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.complete-summary {
  padding: 10px 12px;
  border-radius: 10px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.dialog-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 1200px) {
  .board,
  .dialog-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .content {
    padding: 10px;
  }

  .stats-head {
    flex-direction: column;
    align-items: stretch;
  }

  .stats-actions {
    justify-content: flex-start;
  }

  .stats-strip {
    margin-left: 0;
    justify-content: flex-start;
  }

  .todo-card__menu {
    top: 5px;
    right: 5px;
  }

  .todo-card__due {
    max-width: calc(100% - 48px);
  }
}
</style>
