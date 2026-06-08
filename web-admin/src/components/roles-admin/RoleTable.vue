<template>
  <bz-table
    v-loading="loading"
    :data="rows"
    row-key="id"
    empty-text="暂无数据"
    size="small"
  >
    <bz-table-column
      prop="code"
      label="编码"
      width="160"
    >
      <template #default="scope">
        <span class="mono">{{ scope.row.code }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      prop="name"
      label="名称"
      min-width="180"
    >
      <template #default="scope">
        <div class="name">{{ scope.row.name }}</div>
      </template>
    </bz-table-column>
    <bz-table-column
      label="状态"
      width="100"
    >
      <template #default="scope">
        <bz-tag :type="scope.row.enabled ? 'success' : 'warning'">
          {{ scope.row.enabled ? "启用" : "停用" }}
        </bz-tag>
      </template>
    </bz-table-column>
    <bz-table-column
      prop="createdBy"
      label="创建人"
      width="140"
    >
      <template #default="scope">
        {{ scope.row.createdBy || "-" }}
      </template>
    </bz-table-column>
    <bz-table-column
      label="创建时间"
      width="170"
    >
      <template #default="scope">
        {{ formatDateTime(scope.row.createdAt) }}
      </template>
    </bz-table-column>
    <bz-table-column
      prop="updatedBy"
      label="更新人"
      width="140"
    >
      <template #default="scope">
        {{ scope.row.updatedBy || "-" }}
      </template>
    </bz-table-column>
    <bz-table-column
      label="更新时间"
      width="170"
    >
      <template #default="scope">
        {{ formatDateTime(scope.row.updatedAt) }}
      </template>
    </bz-table-column>
    <bz-table-column
      label="操作"
      width="180"
      fixed="right"
    >
      <template #default="scope">
        <AdminActionBar
          :actions="getPrimaryActions(scope.row)"
          :more-actions="getExtraActions(scope.row)"
        />
      </template>
    </bz-table-column>
  </bz-table>
</template>

<script setup lang="ts">
import AdminActionBar from "@admin/components/admin/AdminActionBar.vue";
import type { RoleEntry } from "@admin/types/role-admin";
import { formatDateTime } from "@shared/utils/formatter";

const props = defineProps<{
  rows: RoleEntry[];
  loading: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canPermissions: boolean;
}>();

const emit = defineEmits<{
  (e: "edit", role: RoleEntry): void;
  (e: "permissions", role: RoleEntry): void;
  (e: "remove", role: RoleEntry): void;
}>();

interface RowAction {
  key: string;
  label: string;
  tone?: "edit" | "delete" | "detail" | "neutral";
  handler: () => void;
}

function getRowActions(role: RoleEntry): RowAction[] {
  const actions: RowAction[] = [];
  if (props.canEdit) {
    actions.push({
      key: "edit",
      label: "编辑",
      tone: "edit",
      handler: () => emit("edit", role),
    });
  }
  if (props.canPermissions) {
    actions.push({
      key: "permissions",
      label: "授权",
      tone: "detail",
      handler: () => emit("permissions", role),
    });
  }
  if (props.canDelete) {
    actions.push({
      key: "delete",
      label: "删除",
      tone: "delete",
      handler: () => emit("remove", role),
    });
  }
  return actions;
}

function getPrimaryActions(role: RoleEntry): RowAction[] {
  return getRowActions(role).filter(
    (action) => action.key === "edit" || action.key === "permissions",
  );
}

function getExtraActions(role: RoleEntry): RowAction[] {
  return getRowActions(role).filter(
    (action) => action.key !== "edit" && action.key !== "permissions",
  );
}
</script>

<style scoped>
.name {
  font-weight: 800;
}

.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}
</style>
