<!-- /src/components/clinic/LedgerTable.vue -->
<template>
  <bz-table
    v-loading="loading"
    :data="rows"
    style="width: 100%"
  >
    <bz-table-column
      prop="occurredAt"
      label="发生日期"
      width="120"
    />
    <bz-table-column
      label="类型"
      width="100"
    >
      <template #default="{ row }">
        <bz-tag
          size="small"
          :type="row.bizType === 'PURCHASE' ? 'success' : 'danger'"
        >
          {{ row.bizType === "PURCHASE" ? "采购入库" : "销售出库" }}
        </bz-tag>
      </template>
    </bz-table-column>
    <bz-table-column label="货物">
      <template #default="{ row }">
        <span>{{ row.skuDisplayName }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      label="变动数量"
      width="110"
      align="right"
    >
      <template #default="{ row }">
        <span>{{ row.bizType === "PURCHASE" ? "+" : "-" }}{{ row.qty }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      prop="unit"
      label="单位"
      width="70"
    />
    <bz-table-column
      label="金额"
      width="120"
      align="right"
    >
      <template #default="{ row }">
        <span>{{ row.amountTotal.toFixed(2) }}</span>
      </template>
    </bz-table-column>
    <bz-table-column label="供应商/相关方">
      <template #default="{ row }">
        <span>{{ row.supplierName || "-" }}</span>
      </template>
    </bz-table-column>
    <bz-table-column label="备注">
      <template #default="{ row }">
        <span>{{ row.remark || "-" }}</span>
      </template>
    </bz-table-column>
    <template #empty>
      <bz-empty description="暂无台账记录" />
    </template>
  </bz-table>
</template>

<script setup lang="ts">
import type { ItemLedger } from "../../types/clinic";

defineProps<{
  rows: ItemLedger[];
  loading: boolean;
}>();
</script>
