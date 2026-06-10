<template>
  <div class="permission-selector">
    <div class="toolbar">
      <bz-input
        v-model="keyword"
        placeholder="搜索权限编码、名称、说明"
        clearable
      />
      <div class="toolbar-meta">已选 {{ selectedSet.size }} 项</div>
    </div>

    <div
      v-loading="loading"
      class="list"
    >
      <bz-empty
        v-if="!loading && filtered.length === 0"
        description="暂无权限"
      />
      <div
        v-else
        class="rows"
      >
        <div
          v-for="permission in filtered"
          :key="permission.id"
          class="row"
        >
          <bz-checkbox
            v-if="canEdit"
            :model-value="selectedSet.has(permission.id)"
            @change="() => toggle(permission.id)"
          />
          <span
            v-else
            class="readonly-mark"
          >
            {{ selectedSet.has(permission.id) ? "已分配" : "-" }}
          </span>

          <div class="meta">
            <div class="meta-main">
              <span class="code">{{ permission.code }}</span>
              <span class="name">{{ permission.name }}</span>
              <bz-tag
                size="small"
                :type="scopeTagType(permission.userScope)"
              >
                {{ scopeLabel(permission.userScope) }}
              </bz-tag>
              <bz-tag
                v-if="!permission.enabled"
                size="small"
                type="warning"
              >
                已禁用
              </bz-tag>
            </div>
            <div
              v-if="permission.description"
              class="desc"
            >
              {{ permission.description }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";

import type { PermissionEntry, PermissionUserScope } from "../../types/permission-admin";

const props = defineProps<{
  permissions: PermissionEntry[];
  modelValue: string[];
  loading?: boolean;
  canEdit?: boolean;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: string[]): void;
}>();

const keyword = ref("");
const selectedSet = ref(new Set<string>());

watch(
  () => props.modelValue,
  (next) => {
    selectedSet.value = new Set((next || []).map((item) => String(item)));
  },
  { immediate: true },
);

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  const rows = [...props.permissions].sort((left, right) => {
    if (left.code !== right.code) {
      return left.code.localeCompare(right.code);
    }
    return left.name.localeCompare(right.name);
  });
  if (!kw) return rows;
  return rows.filter((permission) => {
    const text = [permission.code, permission.name, permission.description]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    return text.includes(kw);
  });
});

function toggle(id: string) {
  const next = new Set(selectedSet.value);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  selectedSet.value = next;
  emit("update:modelValue", Array.from(next));
}

function scopeLabel(scope: PermissionUserScope): string {
  switch (scope) {
    case "COMMON":
      return "全部用户";
    case "EXTERNAL":
      return "用户";
    default:
      return "账号";
  }
}

function scopeTagType(scope: PermissionUserScope): "primary" | "success" | "info" {
  switch (scope) {
    case "COMMON":
      return "info";
    case "EXTERNAL":
      return "success";
    default:
      return "primary";
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-meta {
  color: var(--text-muted);
  font-size: 12px;
  white-space: nowrap;
}

.list {
  margin-top: 12px;
  max-height: 480px;
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

.readonly-mark {
  min-width: 34px;
  font-size: 12px;
  color: var(--text-muted);
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

.code {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
  font-size: 13px;
  color: #0f172a;
}

.name {
  font-weight: 700;
  color: #0f172a;
}

.desc {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.6;
}
</style>
