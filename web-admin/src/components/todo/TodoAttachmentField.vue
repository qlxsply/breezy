<template>
  <div class="attachment-field">
    <div class="attachment-field__header">
      <div class="attachment-field__title">{{ title }}</div>
      <bz-tooltip
        :content="uploading ? '上传中' : '添加图片'"
        placement="top"
      >
        <bz-icon-action-button
          icon="upload"
          tone="primary"
          :disabled="disabled || uploading"
          @click="triggerPick"
        />
      </bz-tooltip>
    </div>

    <input
      ref="fileInputRef"
      class="attachment-field__input"
      type="file"
      accept="image/*"
      multiple
      @change="onPick"
    />

    <div
      class="attachment-field__dropzone"
      :class="{ 'is-disabled': disabled, 'is-active': dropActive }"
      tabindex="0"
      @dragover.prevent
      @dragenter.prevent="dropActive = !disabled"
      @dragleave.prevent="dropActive = false"
      @drop.prevent="onDrop"
      @paste="onPaste"
    >
      <div
        v-if="items.length === 0"
        class="attachment-field__empty"
      >
        <div class="attachment-field__empty-title">拖拽或粘贴图片</div>
      </div>

      <div
        v-else
        class="attachment-field__grid"
      >
        <div
          v-for="item in items"
          :key="item.key"
          class="attachment-field__item"
        >
          <img
            v-if="item.previewUrl"
            :src="item.previewUrl"
            :alt="item.name"
            class="attachment-field__thumb"
            @dblclick.stop="openPreview(item)"
          />
          <div
            v-else
            class="attachment-field__thumb attachment-field__thumb--fallback"
          >
            图片
          </div>
          <div class="attachment-field__meta">
            <span
              class="attachment-field__name"
              :title="item.name"
              >{{ item.name }}</span
            >
          </div>
          <bz-tooltip
            v-if="item.removable && !disabled"
            content="移除"
            placement="top"
          >
            <bz-icon-action-button
              icon="delete"
              tone="danger"
              :disabled="disabled"
              @click="$emit('remove', item)"
            />
          </bz-tooltip>
        </div>
      </div>
    </div>

    <TodoImagePreviewDialog
      v-model="previewVisible"
      :src="previewSrc"
      :name="previewName"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

import type { TodoAttachmentPreviewItem } from "../../types/todo";
import TodoImagePreviewDialog from "./TodoImagePreviewDialog.vue";

const props = withDefaults(
  defineProps<{
    title: string;
    items: TodoAttachmentPreviewItem[];
    disabled?: boolean;
    uploading?: boolean;
  }>(),
  {
    disabled: false,
    uploading: false,
  },
);

const emit = defineEmits<{
  (e: "files", files: File[]): void;
  (e: "remove", item: TodoAttachmentPreviewItem): void;
}>();

const fileInputRef = ref<HTMLInputElement | null>(null);
const dropActive = ref(false);
const previewVisible = ref(false);
const previewSrc = ref("");
const previewName = ref("");

function triggerPick() {
  if (props.disabled) {
    return;
  }
  fileInputRef.value?.click();
}

function onPick(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = normalizeFiles(input.files);
  if (files.length > 0) {
    emit("files", files);
  }
  input.value = "";
}

function onDrop(event: DragEvent) {
  dropActive.value = false;
  if (props.disabled) {
    return;
  }
  const files = normalizeFiles(event.dataTransfer?.files ?? null);
  if (files.length > 0) {
    emit("files", files);
  }
}

function onPaste(event: ClipboardEvent) {
  if (props.disabled) {
    return;
  }
  const files = normalizeClipboardFiles(event.clipboardData);
  if (files.length > 0) {
    event.preventDefault();
    emit("files", files);
  }
}

function normalizeFiles(fileList: FileList | null): File[] {
  if (!fileList) {
    return [];
  }
  return Array.from(fileList).filter((file) => file.type.startsWith("image/"));
}

function normalizeClipboardFiles(data: DataTransfer | null): File[] {
  if (!data) {
    return [];
  }
  const files = Array.from(data.items)
    .filter((item) => item.kind === "file")
    .map((item) => item.getAsFile())
    .filter((file): file is File => Boolean(file))
    .filter((file) => file.type.startsWith("image/"));
  return files;
}

function openPreview(item: TodoAttachmentPreviewItem) {
  if (!item.previewUrl) {
    return;
  }
  previewSrc.value = item.previewUrl;
  previewName.value = item.name;
  previewVisible.value = true;
}
</script>

<style scoped>
.attachment-field {
  display: grid;
  gap: 8px;
}

.attachment-field__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.attachment-field__title {
  font-weight: 700;
  font-size: 12px;
  color: var(--text-main);
}

.attachment-field__input {
  display: none;
}

.attachment-field__dropzone {
  border: 1px dashed #bfdbfe;
  border-radius: 12px;
  background: linear-gradient(180deg, #f8fbff 0%, #f1f7ff 100%);
  padding: 10px;
  min-height: 88px;
  outline: none;
  transition:
    border-color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.attachment-field__dropzone.is-active {
  border-color: #2563eb;
  background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
}

.attachment-field__dropzone.is-disabled {
  opacity: 0.65;
}

.attachment-field__empty {
  min-height: 66px;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--text-muted);
}

.attachment-field__empty-title {
  font-weight: 700;
  font-size: 12px;
  color: #1d4ed8;
}

.attachment-field__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
  gap: 8px;
}

.attachment-field__item {
  display: grid;
  gap: 6px;
  padding: 6px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.attachment-field__thumb {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 8px;
  background: #e2e8f0;
  cursor: zoom-in;
}

.attachment-field__thumb--fallback {
  display: grid;
  place-items: center;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.attachment-field__meta {
  display: grid;
  gap: 2px;
}

.attachment-field__name {
  font-size: 11px;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 640px) {
  .attachment-field__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
