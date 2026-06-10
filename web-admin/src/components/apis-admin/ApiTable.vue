<template>
  <bz-table
    v-loading="loading"
    :data="rows"
    row-key="id"
    empty-text="暂无数据"
    size="small"
  >
    <bz-table-column
      label="模块"
      width="120"
      show-overflow-tooltip
    >
      <template #default="scope">
        <span class="mono">{{ scope.row.module || "-" }}</span>
      </template>
    </bz-table-column>

    <bz-table-column
      label="协议"
      width="110"
    >
      <template #default="scope">
        <bz-tag size="small">{{ scope.row.protocolLabel || scope.row.protocol }}</bz-tag>
      </template>
    </bz-table-column>

    <bz-table-column
      label="方法"
      width="110"
    >
      <template #default="scope">
        <bz-tag
          size="small"
          :type="methodTagType(scope.row.httpMethod)"
        >
          {{ scope.row.httpMethodLabel || scope.row.httpMethod }}
        </bz-tag>
      </template>
    </bz-table-column>

    <bz-table-column
      label="路径"
      min-width="420"
      show-overflow-tooltip
    >
      <template #default="scope">
        <span class="mono">{{ scope.row.pathPattern }}</span>
      </template>
    </bz-table-column>

    <bz-table-column
      label="处理器"
      min-width="380"
      show-overflow-tooltip
    >
      <template #default="scope">
        <span class="mono subdued">{{
          formatHandler(scope.row.handlerClass, scope.row.handlerMethod)
        }}</span>
      </template>
    </bz-table-column>

    <bz-table-column
      label="权限声明"
      width="110"
    >
      <template #default="scope">
        <bz-tag
          size="small"
          :type="scope.row.permissionDeclared ? 'success' : 'warning'"
        >
          {{ scope.row.permissionDeclared ? "已声明" : "未声明" }}
        </bz-tag>
      </template>
    </bz-table-column>

    <bz-table-column
      label="访问类型"
      width="130"
    >
      <template #default="scope">
        <bz-tag
          size="small"
          :type="accessTagType(scope.row.accessType)"
        >
          {{ scope.row.accessTypeLabel || scope.row.accessType }}
        </bz-tag>
      </template>
    </bz-table-column>

    <bz-table-column
      label="用户类型"
      min-width="140"
    >
      <template #default="scope">
        <div
          v-if="scope.row.userTypeLabels?.length"
          class="tag-stack"
        >
          <bz-tag
            v-for="label in scope.row.userTypeLabels"
            :key="label"
            size="small"
            type="info"
          >
            {{ label }}
          </bz-tag>
        </div>
        <span v-else>-</span>
      </template>
    </bz-table-column>

    <bz-table-column
      label="审计"
      width="110"
    >
      <template #default="scope">
        <bz-tooltip :content="scope.row.auditTooltip || ''">
          <bz-tag
            size="small"
            :type="scope.row.auditDeclared ? 'success' : 'info'"
          >
            {{ scope.row.auditDeclared ? "已开启" : "未开启" }}
          </bz-tag>
        </bz-tooltip>
      </template>
    </bz-table-column>

    <bz-table-column
      label="状态"
      width="100"
    >
      <template #default="scope">
        <bz-tag
          size="small"
          :type="scope.row.enabled ? 'success' : 'danger'"
        >
          {{ scope.row.enabled ? "启用" : "停用" }}
        </bz-tag>
      </template>
    </bz-table-column>

    <bz-table-column
      label="操作"
      width="88"
      fixed="right"
    >
      <template #default="scope">
        <AdminActionBar :actions="getRowActions(scope.row)" />
      </template>
    </bz-table-column>
  </bz-table>
</template>

<script setup lang="ts">
import type { AdminActionItem } from "../../types/admin-action";
import type { ApiEntry } from "../../types/api-admin";
import AdminActionBar from "../admin/AdminActionBar.vue";

const props = defineProps<{
  rows: ApiEntry[];
  loading: boolean;
  canPublish?: boolean;
  canDisable?: boolean;
}>();

const emit = defineEmits<{
  (e: "publish", api: ApiEntry): void;
  (e: "disable", api: ApiEntry): void;
}>();

function methodTagType(method?: string) {
  switch ((method || "").toUpperCase()) {
    case "POST":
      return "success";
    case "PUT":
      return "warning";
    case "DELETE":
      return "danger";
    case "PATCH":
      return "warning";
    default:
      return "info";
  }
}

function accessTagType(accessType?: string) {
  switch ((accessType || "").toUpperCase()) {
    case "AUTHORIZED":
      return "warning";
    case "AUTHENTICATED":
      return "primary";
    default:
      return "info";
  }
}

function formatHandler(handlerClass?: string, handlerMethod?: string) {
  if (handlerClass && handlerMethod) {
    return `${handlerClass}#${handlerMethod}`;
  }
  return handlerClass || handlerMethod || "-";
}

function getRowActions(row: ApiEntry): AdminActionItem[] {
  const actions: AdminActionItem[] = [];
  if (props.canPublish && !row.enabled) {
    actions.push({
      key: "enable",
      label: "启用",
      tone: "enable",
      handler: () => emitPublish(row),
    });
  }
  if (props.canDisable && row.enabled) {
    actions.push({
      key: "disable",
      label: "停用",
      tone: "disable",
      handler: () => emitDisable(row),
    });
  }
  return actions;
}

function emitPublish(row: ApiEntry) {
  emit("publish", row);
}

function emitDisable(row: ApiEntry) {
  emit("disable", row);
}
</script>

<style scoped>
.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.subdued {
  color: var(--text-muted);
}

.tag-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
