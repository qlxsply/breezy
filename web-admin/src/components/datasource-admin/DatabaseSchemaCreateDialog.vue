<!-- /src/components/datasource-admin/DatabaseSchemaCreateDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    title="新增数据库"
    width="520px"
    @close="$emit('close')"
  >
    <bz-form label-width="120px">
      <bz-form-item
        label="选择数据源"
        required
      >
        <bz-select
          v-model="form.dataSourceId"
          placeholder="请选择数据源"
          @change="onDataSourceSelected"
        >
          <bz-option
            v-for="ds in dataSourceOptions"
            :key="ds.id"
            :label="`${ds.name} (${ds.dbType}) ${ds.status !== 'OK' && ds.sourceType !== 'APP' ? '[不可用]' : ''}`"
            :value="ds.id"
            :disabled="ds.status !== 'OK' && ds.sourceType !== 'APP'"
          />
        </bz-select>
      </bz-form-item>

      <bz-form-item
        label="数据库/Schema"
        required
      >
        <bz-select
          v-model="form.databaseName"
          :disabled="form.dataSourceId === null || loadingDbs || availableDbs.length === 0"
          :placeholder="databaseSchemaPlaceholderText"
          @change="onDatabaseSelected"
        >
          <bz-option
            v-for="db in availableDbs"
            :key="db"
            :label="db"
            :value="db"
          />
        </bz-select>
      </bz-form-item>

      <bz-form-item
        label="别名"
        required
      >
        <bz-input
          v-model="form.alias"
          placeholder="用于显示的名称"
        />
      </bz-form-item>

      <bz-form-item label="备注">
        <bz-input
          v-model="form.remarkCustom"
          type="textarea"
          :rows="3"
          placeholder="描述 Schema 用途"
        />
      </bz-form-item>
    </bz-form>
    <template #footer>
      <bz-button @click="$emit('close')">取消</bz-button>
      <bz-button
        type="primary"
        :loading="saving"
        :disabled="!canSubmit"
        @click="handleSave"
      >
        {{ saving ? "保存中…" : "确定" }}
      </bz-button>
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";

import { createDatabaseSchema, listAvailableDatabases } from "../../api/database-source";
import type { DatabaseSourceSimple } from "../../types/database-source";
import { message } from "../../utils/message";

const props = defineProps<{
  dataSources: DatabaseSourceSimple[];
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "saved"): void;
}>();

const loadingDbs = ref(false);
const saving = ref(false);
const availableDbs = ref<string[]>([]);
const dataSourceOptions = computed(() => props.dataSources);

const form = reactive({
  dataSourceId: null as string | null,
  databaseName: "",
  alias: "",
  remarkCustom: "",
});

const canSubmit = computed(() => {
  return form.dataSourceId !== null && form.databaseName.trim() !== "" && form.alias.trim() !== "";
});

const databaseSchemaPlaceholderText = computed(() => {
  if (loadingDbs.value) {
    return "正在获取数据库列表…";
  }
  if (form.dataSourceId === null) {
    return "请先选择数据源";
  }
  if (availableDbs.value.length === 0) {
    return "暂无可选数据库";
  }
  return "请选择物理数据库";
});

async function onDataSourceSelected() {
  form.databaseName = "";
  availableDbs.value = [];

  if (form.dataSourceId === null) {
    return;
  }

  loadingDbs.value = true;
  try {
    availableDbs.value = await listAvailableDatabases(form.dataSourceId);
    if (availableDbs.value.length === 0) {
      message.error("未发现可用的数据库/模式");
    }
  } catch (_e) {
    message.error("获取数据库列表失败");
  } finally {
    loadingDbs.value = false;
  }
}

function onDatabaseSelected() {
  if (form.databaseName && !form.alias.trim()) {
    form.alias = form.databaseName;
  }
}

async function handleSave() {
  if (form.dataSourceId === null) {
    message.error("请选择数据源");
    return;
  }
  if (!form.databaseName.trim()) {
    message.error("请选择数据库");
    return;
  }
  if (!form.alias.trim()) {
    message.error("请填写别名");
    return;
  }

  saving.value = true;
  try {
    await createDatabaseSchema({
      dataSourceId: form.dataSourceId,
      databaseName: form.databaseName.trim(),
      alias: form.alias.trim(),
      remarkCustom: form.remarkCustom.trim(),
    });
    message.success("数据库创建成功");
    emit("saved");
  } catch (_e) {
    message.error("创建数据库失败");
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped></style>
