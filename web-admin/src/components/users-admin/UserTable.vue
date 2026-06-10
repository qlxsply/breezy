<!-- /src/components/users-admin/UserTable.vue -->
<template>
  <bz-table
    v-loading="loading"
    :data="rows"
    row-key="id"
    empty-text="暂无数据"
    size="small"
  >
    <bz-table-column
      prop="username"
      label="账号"
      min-width="180"
    >
      <template #default="scope">
        <div class="name">{{ scope.row.username }}</div>
      </template>
    </bz-table-column>
    <bz-table-column
      prop="nickname"
      label="昵称"
      min-width="160"
    >
      <template #default="scope">
        {{ scope.row.nickname || "-" }}
      </template>
    </bz-table-column>
    <bz-table-column
      label="类型"
      width="120"
    >
      <template #default="scope">
        <bz-tag :type="resolveUserTypeTagType(scope.row.userType)">
          {{ resolveUserTypeLabel(scope.row.userType) }}
        </bz-tag>
      </template>
    </bz-table-column>
    <bz-table-column
      label="状态"
      width="110"
    >
      <template #default="scope">
        <bz-tag
          :type="
            scope.row.status === 'ENABLED' || scope.row.status === 'ACTIVE' ? 'success' : 'warning'
          "
        >
          {{
            scope.row.status === "ENABLED" || scope.row.status === "ACTIVE"
              ? "启用"
              : scope.row.status === "CANCELLED"
                ? "已注销"
                : "停用"
          }}
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
import type { UserEntry } from "../../types/user-admin";
import { formatDateTime } from "../../utils/formatter";
import AdminActionBar from "../admin/AdminActionBar.vue";

interface UserTypeMeta {
  label: string;
  tagType?: string | null;
}

const props = defineProps<{
  rows: UserEntry[];
  loading: boolean;
  canEdit: boolean;
  canToggle: boolean;
  canReset: boolean;
  canRoles: boolean;
  canDelete: boolean;
  userTypeMetaMap?: Record<string, UserTypeMeta>;
}>();

const emit = defineEmits<{
  (e: "edit", user: UserEntry): void;
  (e: "toggle", user: UserEntry): void;
  (e: "reset", user: UserEntry): void;
  (e: "roles", user: UserEntry): void;
  (e: "remove", user: UserEntry): void;
}>();

interface RowAction {
  key: string;
  label: string;
  tone?: "edit" | "enable" | "disable" | "delete" | "detail" | "neutral";
  disabled?: boolean;
  handler: () => void;
}

function resolveUserTypeLabel(userType: UserEntry["userType"]): string {
  return props.userTypeMetaMap?.[userType]?.label || userType;
}

function resolveUserTypeTagType(userType: UserEntry["userType"]): string {
  return props.userTypeMetaMap?.[userType]?.tagType || "info";
}

function getRowActions(user: UserEntry): RowAction[] {
  const actions: RowAction[] = [];
  if (props.canEdit) {
    actions.push({
      key: "edit",
      label: "编辑",
      tone: "edit",
      handler: () => emit("edit", user),
    });
  }
  if (props.canToggle) {
    actions.push({
      key: "toggle",
      label: user.status === "ENABLED" ? "停用" : "启用",
      tone: user.status === "ENABLED" ? "disable" : "enable",
      handler: () => emit("toggle", user),
    });
  }
  if (props.canReset) {
    actions.push({
      key: "reset",
      label: "重置密码",
      handler: () => emit("reset", user),
    });
  }
  if (props.canRoles && user.userType !== "EXTERNAL") {
    actions.push({
      key: "roles",
      label: "角色",
      handler: () => emit("roles", user),
    });
  }
  if (props.canDelete) {
    actions.push({
      key: "delete",
      label: "删除",
      tone: "delete",
      handler: () => emit("remove", user),
    });
  }
  return actions;
}

function getPrimaryActions(user: UserEntry): RowAction[] {
  const actions = getRowActions(user);
  return actions.filter((action) => action.key === "edit" || action.key === "roles");
}

function getExtraActions(user: UserEntry): RowAction[] {
  const actions = getRowActions(user);
  return actions.filter((action) => action.key !== "edit" && action.key !== "roles");
}
</script>

<style scoped>
.name {
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
