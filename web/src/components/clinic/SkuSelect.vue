<!-- /src/components/clinic/SkuSelect.vue -->
<template>
  <bz-select
    v-model="query"
    filterable
    remote
    reserve-keyword
    default-first-option
    clearable
    :allow-create="allowCreate"
    :remote-method="onSearch"
    :loading="loading"
    :placeholder="placeholder || '搜索货物名称...'"
    @change="onChange"
    @visible-change="onVisibleChange"
  >
    <bz-option
      v-for="opt in options"
      :key="opt.id"
      :label="opt.displayName"
      :value="opt.displayName"
    >
      <div class="opt-name">{{ opt.displayName }}</div>
      <div class="opt-detail">{{ opt.manufacturer || "-" }} | {{ opt.spec || "-" }}</div>
    </bz-option>
  </bz-select>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";

import { listSkus } from "../../api/clinic";
import type { ItemSku } from "../../types/clinic";

const props = defineProps<{
  modelValue?: string;
  placeholder?: string;
  allowCreate?: boolean;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void;
  (e: "select", sku: ItemSku): void;
  (e: "typing"): void;
}>();

const query = ref(props.modelValue || "");
const options = ref<ItemSku[]>([]);
const loading = ref(false);
const allowCreate = computed(() => props.allowCreate !== false);

watch(
  () => props.modelValue,
  (v) => {
    if ((v || "") !== query.value) {
      query.value = v || "";
    }
  },
);

async function search(keyword: string) {
  loading.value = true;
  try {
    const trimmed = keyword.trim();
    options.value = await listSkus(trimmed ? { nameLike: trimmed } : {});
  } finally {
    loading.value = false;
  }
}

function onSearch(keyword: string) {
  emit("typing");
  search(keyword);
}

function onVisibleChange(visible: boolean) {
  if (visible && options.value.length === 0) {
    search("");
  }
}

function onChange(value: string) {
  emit("update:modelValue", value);
  const match = options.value.find((opt) => opt.displayName === value);
  if (match) {
    emit("select", match);
  }
}
</script>

<style scoped>
:deep(.el-select) {
  width: 100%;
}

.opt-name {
  font-weight: 600;
  font-size: 14px;
}

.opt-detail {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}
</style>
