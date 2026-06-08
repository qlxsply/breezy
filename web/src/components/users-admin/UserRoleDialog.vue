<!-- /src/components/users-admin/UserRoleDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    :title="`角色分配 - ${userName}`"
    width="720px"
    @close="$emit('close')"
  >
    <div class="toolbar">
      <bz-input
        v-model="keyword"
        placeholder="搜索角色编码/名称"
        clearable
      />
      <div class="count">已选 {{ selectedSet.size }} 项</div>
    </div>

    <div
      v-loading="loading"
      class="list"
    >
      <bz-empty
        v-if="!loading && filtered.length === 0"
        description="暂无角色"
      />
      <div v-else>
        <div
          v-for="role in filtered"
          :key="role.id"
          class="row"
        >
          <bz-checkbox
            v-if="canSave !== false"
            :model-value="selectedSet.has(role.id)"
            @change="() => toggle(role.id)"
          />
          <span
            v-else
            class="readonly-mark"
            >{{ selectedSet.has(role.id) ? "已分配" : "-" }}</span
          >
          <div class="meta">
            <div class="name">{{ role.name }}</div>
            <div class="code">{{ role.code }}</div>
          </div>
          <bz-tag :type="role.enabled ? 'success' : 'warning'">
            {{ role.enabled ? "启用" : "停用" }}
          </bz-tag>
        </div>
      </div>
    </div>

    <template #footer>
      <bz-button @click="$emit('close')">取消</bz-button>
      <bz-button
        v-if="canSave !== false"
        type="primary"
        @click="submit"
        >保存</bz-button
      >
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
// <script setup> + TS：顶层即 setup()。
import { computed, ref, watch } from "vue";

import type { RoleEntry } from "../../types/role-admin";

const props = defineProps<{
  userName: string;
  roles: RoleEntry[];
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

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return props.roles;
  return props.roles.filter((role) => {
    const text = `${role.code} ${role.name}`.toLowerCase();
    return text.includes(kw);
  });
});

function toggle(id: string) {
  const next = new Set(selectedSet.value);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  selectedSet.value = next;
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

.count {
  color: var(--text-muted);
  font-size: 12px;
}

.list {
  margin-top: 12px;
  max-height: 420px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 6px;
  border-radius: 10px;
}

.meta {
  flex: 1;
}

.name {
  font-weight: 800;
}

.code {
  font-size: 12px;
  color: var(--text-muted);
}

.readonly-mark {
  min-width: 34px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
