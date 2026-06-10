<!-- /src/components/clinic/SupplierSelect.vue -->
<template>
  <bz-select
    v-model="query"
    filterable
    remote
    reserve-keyword
    allow-create
    default-first-option
    clearable
    :remote-method="onSearch"
    :loading="loading"
    :placeholder="placeholder || '搜索供应商名称...'"
    @change="onChange"
    @visible-change="onVisibleChange"
  >
    <bz-option
      v-for="opt in options"
      :key="opt.id"
      :label="opt.name"
      :value="opt.name"
    />
  </bz-select>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";

import { listSuppliers } from "../../api/clinic";
import type { Supplier } from "../../types/clinic";

const props = defineProps<{
  modelValue?: string;
  placeholder?: string;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void;
  (e: "select", supplier: Supplier): void;
}>();

const query = ref(props.modelValue || "");
const options = ref<Supplier[]>([]);
const loading = ref(false);

watch(
  () => props.modelValue,
  (v) => {
    if ((v || "") !== query.value) {
      query.value = v || "";
    }
  },
);

watch(query, (v) => {
  emit("update:modelValue", v);
});

async function search(keyword: string) {
  loading.value = true;
  try {
    const trimmed = keyword.trim();
    options.value = await listSuppliers(trimmed ? { nameLike: trimmed } : {});
  } finally {
    loading.value = false;
  }
}

function onSearch(keyword: string) {
  search(keyword);
}

function onVisibleChange(visible: boolean) {
  if (visible && options.value.length === 0) {
    search("");
  }
}

function onChange(value: string) {
  const match = options.value.find((opt) => opt.name === value);
  if (match) {
    emit("select", match);
  }
}
</script>

<style scoped>
:deep(.el-select) {
  width: 100%;
}
</style>
