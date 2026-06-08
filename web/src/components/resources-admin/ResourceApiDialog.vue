<!-- /src/components/resources-admin/ResourceApiDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    :title="`API 绑定 - ${resourceName}`"
    width="900px"
    @close="$emit('close')"
  >
    <div class="toolbar">
      <bz-input
        v-model="keyword"
        class="keyword-input"
        placeholder="搜索路径、应用、处理器"
        clearable
      />
      <div class="count">已选 {{ selectedSet.size }} 项</div>
    </div>

    <div
      v-loading="loading"
      class="list"
    >
      <bz-empty
        v-if="!loading && filteredApis.length === 0"
        description="暂无 API"
      />
      <div
        v-else
        class="rows"
      >
        <div
          v-for="api in filteredApis"
          :key="api.id"
          class="row"
        >
          <bz-checkbox
            v-if="canSave !== false"
            class="api-checkbox"
            :model-value="selectedSet.has(api.id)"
            @change="onApiCheckChange(api.id, $event)"
          />
          <span
            v-else
            class="readonly-mark"
          >
            {{ selectedSet.has(api.id) ? "已绑定" : "-" }}
          </span>
          <div class="meta">
            <div class="meta-main">
              <bz-tag
                size="small"
                type="info"
                class="method-tag"
              >
                {{ api.httpMethodLabel || api.httpMethod }}
              </bz-tag>
              <span class="mono">{{ api.pathPattern }}</span>
              <bz-tag size="small">{{ api.module }}</bz-tag>
              <bz-tag
                size="small"
                :type="api.enabled ? 'success' : 'warning'"
              >
                {{ api.enabled ? "启用" : "停用" }}
              </bz-tag>
            </div>
            <div class="sub">
              {{ api.handlerClass || "-" }}
              <span v-if="api.handlerMethod">#{{ api.handlerMethod }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <bz-button @click="$emit('close')">取消</bz-button>
      <bz-button
        v-if="canSave !== false"
        type="primary"
        @click="submit"
      >
        保存
      </bz-button>
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";

import type { ApiEntry } from "../../types/api-admin";

const props = defineProps<{
  resourceName: string;
  apis: ApiEntry[];
  selectedIds: string[];
  loading?: boolean;
  canSave?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "submit", ids: string[]): void;
}>();

const keyword = ref("");
const selectedSet = ref(new Set<string>());

watch(
  () => props.selectedIds,
  (next) => {
    selectedSet.value = new Set(next || []);
  },
  { immediate: true },
);

const filteredApis = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  const rows = [...props.apis].sort((left, right) => {
    if (left.module !== right.module) {
      return left.module.localeCompare(right.module);
    }
    if (left.pathPattern !== right.pathPattern) {
      return left.pathPattern.localeCompare(right.pathPattern);
    }
    return left.httpMethod.localeCompare(right.httpMethod);
  });
  if (!kw) return rows;
  return rows.filter((api) => {
    const text = [api.pathPattern, api.module, api.handlerClass, api.handlerMethod]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    return text.includes(kw);
  });
});

function toggleApi(id: string, checked: boolean) {
  const next = new Set(selectedSet.value);
  if (checked) next.add(id);
  else next.delete(id);
  selectedSet.value = next;
}

function onApiCheckChange(id: string, checked: unknown) {
  toggleApi(id, checked === true);
}

function submit() {
  if (props.canSave === false) return;
  emit("submit", Array.from(selectedSet.value));
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
}

.keyword-input {
  max-width: 420px;
  flex: 1;
}

.count {
  color: var(--text-muted);
  font-size: 12px;
}

.list {
  margin-top: 12px;
  max-height: 520px;
  overflow: auto;
}

.rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
}

.meta {
  flex: 1;
  min-width: 0;
}

.meta-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.api-checkbox {
  margin-right: 4px;
}

.readonly-mark {
  min-width: 34px;
  font-size: 12px;
  color: var(--text-muted);
}

.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.method-tag {
  text-transform: uppercase;
}
</style>
