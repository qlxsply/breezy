<template>
  <bz-dialog
    v-model="visible"
    title="图片预览"
    width="860px"
    @close="resetState"
  >
    <div class="preview-body">
      <div
        v-if="src"
        class="preview-canvas"
      >
        <img
          :src="src"
          :alt="name || '附件图片'"
          class="preview-image"
        />
      </div>
      <div
        v-else
        class="preview-empty"
      >
        暂无可预览图片
      </div>
    </div>
    <template #footer>
      <div class="preview-footer">
        <bz-tooltip
          content="复制"
          placement="top"
        >
          <bz-icon-action-button
            icon="copy"
            :disabled="busy || !canCopy"
            @click="handleCopy"
          />
        </bz-tooltip>
        <bz-tooltip
          content="下载"
          placement="top"
        >
          <bz-icon-action-button
            icon="download"
            tone="primary"
            :disabled="busy || !src"
            @click="handleDownload"
          />
        </bz-tooltip>
      </div>
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";

import { message } from "../../utils/message";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    src?: string;
    name?: string;
  }>(),
  {
    src: "",
    name: "",
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
}>();

const busy = ref(false);

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value),
});

const canCopy = computed(() => {
  if (!props.src) {
    return false;
  }
  if (!window.isSecureContext) {
    return false;
  }
  if (!navigator.clipboard || typeof navigator.clipboard.write !== "function") {
    return false;
  }
  if (typeof ClipboardItem === "undefined") {
    return false;
  }
  if (typeof ClipboardItem.supports === "function") {
    return ClipboardItem.supports("image/png");
  }
  return true;
});

async function handleCopy() {
  if (!props.src) {
    return;
  }
  if (!canCopy.value) {
    return;
  }
  busy.value = true;
  try {
    const blob = await fetchBlob(props.src);
    const clipboardBlob = await toClipboardBlob(blob);
    await navigator.clipboard.write([
      new ClipboardItem({ [clipboardBlob.type || "image/png"]: clipboardBlob }),
    ]);
    message.success("图片已复制到剪切板");
  } catch (error) {
    if (error instanceof Error && error.message.trim().length > 0) {
      message.error(error.message);
      return;
    }
    message.error("复制图片失败");
  } finally {
    busy.value = false;
  }
}

async function handleDownload() {
  if (!props.src) {
    return;
  }
  busy.value = true;
  try {
    const blob = await fetchBlob(props.src);
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = normalizeName(props.name, blob.type);
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  } catch (error) {
    if (error instanceof Error && error.message.trim().length > 0) {
      message.error(error.message);
      return;
    }
    message.error("下载图片失败");
  } finally {
    busy.value = false;
  }
}

function resetState() {
  busy.value = false;
}

async function toClipboardBlob(blob: Blob): Promise<Blob> {
  if (blob.type === "image/png") {
    return blob;
  }
  if (!blob.type.startsWith("image/")) {
    return blob;
  }
  return toPngBlob(blob);
}

async function toPngBlob(blob: Blob): Promise<Blob> {
  const url = URL.createObjectURL(blob);
  try {
    const image = await loadImage(url);
    const canvas = document.createElement("canvas");
    canvas.width = image.naturalWidth || image.width;
    canvas.height = image.naturalHeight || image.height;
    const context = canvas.getContext("2d");
    if (!context) {
      throw new Error("无法创建画布上下文");
    }
    context.drawImage(image, 0, 0);
    const pngBlob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, "image/png"),
    );
    if (!pngBlob) {
      throw new Error("图片转码失败");
    }
    return pngBlob;
  } finally {
    URL.revokeObjectURL(url);
  }
}

function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("图片加载失败"));
    image.src = url;
  });
}

async function fetchBlob(src: string): Promise<Blob> {
  const response = await fetch(src);
  if (!response.ok) {
    throw new Error(`图片读取失败: ${response.status}`);
  }
  return response.blob();
}

function normalizeName(name: string | undefined, contentType: string): string {
  const trimmed = (name || "").trim();
  if (trimmed.length > 0) {
    return trimmed;
  }
  const ext = contentType.includes("png") ? "png" : contentType.includes("jpeg") ? "jpg" : "webp";
  return `todo-image.${ext}`;
}
</script>

<style scoped>
.preview-body {
  min-height: 380px;
}

.preview-canvas {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 12px;
  display: grid;
  place-items: center;
  min-height: 380px;
}

.preview-image {
  max-width: 100%;
  max-height: 72vh;
  object-fit: contain;
}

.preview-empty {
  min-height: 380px;
  display: grid;
  place-items: center;
  color: #64748b;
}

.preview-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
