<template>
  <bz-dialog
    v-model="visible"
    title="事项详情"
    width="760px"
    @close="handleClose"
  >
    <div
      v-if="todo"
      class="detail-body"
    >
      <div class="detail-row">
        <span class="detail-label">状态</span
        ><span class="detail-value">{{ statusText(todo.status) }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">创建时间</span
        ><span class="detail-value">{{ formatDateTime(todo.createdAt) }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">截止时间</span
        ><span class="detail-value">{{ formatDateTime(todo.dueTime) }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">完成时间</span
        ><span class="detail-value">{{ formatDateTime(todo.completedAt) }}</span>
      </div>

      <div class="detail-row detail-row--top">
        <span class="detail-label">事项内容</span>
        <span class="detail-value detail-value--multiline">{{ todo.content || "-" }}</span>
      </div>

      <div
        v-if="secondaryText"
        class="detail-row detail-row--top"
      >
        <span class="detail-label">{{ secondaryLabel }}</span>
        <span class="detail-value detail-value--multiline">{{ secondaryText }}</span>
      </div>

      <div class="detail-row detail-row--top">
        <span class="detail-label">附件</span>
        <div class="detail-value detail-value--multiline">
          <div
            v-if="detailLoading"
            class="detail-hint"
          >
            附件加载中...
          </div>
          <div
            v-else-if="detailAttachments.length === 0"
            class="detail-hint"
          >
            暂无附件
          </div>
          <div
            v-else
            class="attachment-list"
          >
            <div
              v-for="item in detailAttachments"
              :key="`${item.source}-${item.id}`"
              class="attachment-item"
            >
              <img
                v-if="item.previewUrl"
                :src="item.previewUrl"
                :alt="item.name"
                class="attachment-thumb"
                @dblclick.stop="openPreview(item.previewUrl, item.name)"
              />
              <div
                v-else
                class="attachment-fallback"
              >
                附件
              </div>
              <div class="attachment-meta">
                <div class="attachment-name">{{ item.name }}</div>
                <div class="attachment-sub">
                  {{ item.source === "completion" ? "完成附件" : "内容附件" }} ·
                  {{ formatFileSize(item.size) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detail-footer">
        <bz-tooltip
          content="取消"
          placement="top"
        >
          <bz-icon-action-button
            icon="close"
            @click="handleClose"
          />
        </bz-tooltip>
        <bz-tooltip
          content="确认"
          placement="top"
        >
          <bz-icon-action-button
            icon="check"
            tone="primary"
            @click="handleClose"
          />
        </bz-tooltip>
      </div>
    </template>

    <TodoImagePreviewDialog
      v-model="previewVisible"
      :src="previewSrc"
      :name="previewName"
    />
  </bz-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";

import {listPublicDictOptions, type PublicDictItem} from "../../api/dicts";
import { fetchTodoAttachmentView, getTodoAttachmentMetadata } from "../../api/todo";
import type { TodoAttachmentMeta, TodoItem, TodoStatus } from "../../types/todo";
import { formatDateTime } from "../../utils/formatter";
import { message } from "../../utils/message";
import TodoImagePreviewDialog from "./TodoImagePreviewDialog.vue";

const props = defineProps<{
  modelValue: boolean;
  todo: TodoItem | null;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
}>();

interface DetailAttachmentItem {
  id: string;
  name: string;
  size?: number | null;
  previewUrl?: string;
  source: "content" | "completion";
}

const detailLoading = ref(false);
const detailAttachments = ref<DetailAttachmentItem[]>([]);
const detailBlobUrls = new Set<string>();
const todoStatusItems = ref<PublicDictItem[]>([]);

const previewVisible = ref(false);
const previewSrc = ref("");
const previewName = ref("");

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const secondaryText = computed(() => {
  if (!props.todo) {
    return "";
  }
  if (props.todo.status === "DONE" && (props.todo.completionNote || "").trim().length > 0) {
    return props.todo.completionNote || "";
  }
  return props.todo.note || "";
});

const secondaryLabel = computed(() =>
  props.todo?.status === "DONE" && (props.todo.completionNote || "").trim().length > 0
    ? "补充完成"
    : "备注",
);

watch(
  () => [props.modelValue, props.todo?.id] as const,
  ([open]) => {
    if (!open || !props.todo) {
      clearDetailAttachments();
      return;
    }
    void loadDetailAttachments(props.todo);
  },
  { immediate: true },
);

void loadStatusDict();

onBeforeUnmount(() => {
  clearDetailAttachments();
});

function handleClose() {
  previewVisible.value = false;
  emit("update:modelValue", false);
}

async function loadDetailAttachments(todo: TodoItem) {
  detailLoading.value = true;
  clearDetailAttachments();
  try {
    const ordered = [
      ...todo.contentAttachmentFileIds.map((id) => ({ id, source: "content" as const })),
      ...todo.completionAttachmentFileIds.map((id) => ({ id, source: "completion" as const })),
    ];
    if (ordered.length === 0) {
      return;
    }

    const metas = await getTodoAttachmentMetadata(
      Array.from(new Set(ordered.map((item) => item.id))),
    );
    const metaMap = new Map<string, TodoAttachmentMeta>();
    metas.forEach((meta) => metaMap.set(meta.id, meta));

    const items: DetailAttachmentItem[] = [];
    for (const entry of ordered) {
      const meta = metaMap.get(entry.id);
      const item: DetailAttachmentItem = {
        id: entry.id,
        name: meta?.name || entry.id,
        size: meta?.size,
        source: entry.source,
      };
      if ((meta?.contentType || "").startsWith("image/")) {
        try {
          const blob = await fetchTodoAttachmentView(entry.id);
          const blobUrl = URL.createObjectURL(blob);
          detailBlobUrls.add(blobUrl);
          item.previewUrl = blobUrl;
        } catch {
          item.previewUrl = undefined;
        }
      }
      items.push(item);
    }
    detailAttachments.value = items;
  } catch (error) {
    if (error instanceof Error && error.message.trim().length > 0) {
      message.error(error.message);
      return;
    }
    message.error("附件加载失败");
  } finally {
    detailLoading.value = false;
  }
}

function clearDetailAttachments() {
  detailAttachments.value = [];
  detailBlobUrls.forEach((url) => URL.revokeObjectURL(url));
  detailBlobUrls.clear();
}

function openPreview(src: string, name: string) {
  previewSrc.value = src;
  previewName.value = name;
  previewVisible.value = true;
}

function formatFileSize(size?: number | null): string {
  if (size == null || Number.isNaN(size)) {
    return "大小未知";
  }
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function statusText(value: TodoStatus): string {
  return todoStatusItems.value.find((item) => item.itemValue === value)?.itemLabel || value;
}

async function loadStatusDict() {
  try {
    todoStatusItems.value = await listPublicDictOptions("TODO_TASK_STATUS");
  } catch {
    todoStatusItems.value = [];
  }
}
</script>

<style scoped>
.detail-body {
  display: grid;
  gap: 10px;
}

.detail-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.detail-row--top {
  align-items: start;
}

.detail-label {
  font-size: 12px;
  color: #64748b;
  padding-top: 2px;
}

.detail-value {
  font-size: 13px;
  color: #0f172a;
  text-align: left;
}

.detail-value--multiline {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-hint {
  color: #64748b;
  font-size: 12px;
}

.attachment-list {
  display: grid;
  gap: 8px;
}

.attachment-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.attachment-thumb,
.attachment-fallback {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #f1f5f9;
  display: grid;
  place-items: center;
  object-fit: cover;
  font-size: 11px;
  color: #64748b;
}

.attachment-thumb {
  cursor: zoom-in;
}

.attachment-meta {
  min-width: 0;
}

.attachment-name {
  font-size: 13px;
  color: #0f172a;
  font-weight: 700;
}

.attachment-sub {
  font-size: 12px;
  color: #64748b;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 720px) {
  .detail-row {
    grid-template-columns: 80px minmax(0, 1fr);
  }
}
</style>
