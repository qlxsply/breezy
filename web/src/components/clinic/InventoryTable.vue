<!-- /src/components/clinic/InventoryTable.vue -->
<template>
  <bz-table
    v-loading="loading"
    :data="rows"
    style="width: 100%"
  >
    <bz-table-column label="货物名称">
      <template #default="{ row }">
        <span>{{ row.skuDisplayName }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      prop="manufacturer"
      label="生产厂家"
    >
      <template #default="{ row }">
        <span>{{ row.manufacturer || "-" }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      prop="spec"
      label="规格"
    >
      <template #default="{ row }">
        <span>{{ row.spec || "-" }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      label="当前库存"
      width="120"
      align="right"
    >
      <template #default="{ row }">
        <span>{{ row.inventoryQty }}</span>
      </template>
    </bz-table-column>
    <bz-table-column
      label="操作"
      width="120"
      align="center"
    >
      <template #default="{ row }">
        <bz-button
          size="small"
          text
          @click="$emit('viewLedger', row)"
          >查看台账</bz-button
        >
      </template>
    </bz-table-column>
    <template #empty>
      <bz-empty description="暂无库存数据" />
    </template>
  </bz-table>
</template>

<script setup lang="ts">
import type { ItemInventory } from "../../types/clinic";

defineProps<{
  rows: ItemInventory[];
  loading: boolean;
}>();

defineEmits<{
  (e: "viewLedger", item: ItemInventory): void;
}>();
</script>
