<!-- /src/components/clinic/SaleTable.vue -->
<template>
  <bz-table
    v-loading="loading"
    :data="rows"
    style="width: 100%"
  >
    <bz-table-column
      prop="soldAt"
      label="销售日期"
      width="140"
    />
    <bz-table-column label="客户名称">
      <template #default="{ row }">
        <span>{{ row.customerName || "散客" }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      label="销售总额"
      width="140"
      align="right"
    >
      <template #default="{ row }">
        <span>￥{{ row.totalAmount.toFixed(2) }}</span>
      </template>
    </bz-table-column>
    <bz-table-column label="备注">
      <template #default="{ row }">
        <span>{{ row.remark || "-" }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      label="操作"
      width="180"
      align="center"
    >
      <template #default="{ row }">
        <bz-button
          size="small"
          text
          @click="$emit('view', row)"
          >详情</bz-button
        >
        <bz-button
          v-if="canDelete"
          size="small"
          type="danger"
          text
          @click="$emit('remove', row)"
        >
          删除
        </bz-button>
      </template>
    </bz-table-column>
    <template #empty>
      <bz-empty description="暂无销售记录" />
    </template>
  </bz-table>
</template>

<script setup lang="ts">
import type { Sale } from "../../types/clinic";

defineProps<{
  rows: Sale[];
  loading: boolean;
  canDelete: boolean;
}>();

defineEmits<{
  (e: "view", item: Sale): void;
  (e: "remove", item: Sale): void;
}>();
</script>
