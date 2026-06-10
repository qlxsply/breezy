<!-- /src/components/clinic/PurchaseTable.vue -->
<template>
  <bz-table
    v-loading="loading"
    :data="rows"
    style="width: 100%"
  >
    <bz-table-column
      prop="purchasedAt"
      label="采购日期"
      width="140"
    />
    <bz-table-column label="供应商">
      <template #default="{ row }">
        <span>{{ row.supplierName }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      label="采购总额"
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
      <bz-empty description="暂无采购记录" />
    </template>
  </bz-table>
</template>

<script setup lang="ts">
import type { Purchase } from "../../types/clinic";

defineProps<{
  rows: Purchase[];
  loading: boolean;
  canDelete: boolean;
}>();

defineEmits<{
  (e: "view", item: Purchase): void;
  (e: "remove", item: Purchase): void;
}>();
</script>
