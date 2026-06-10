<!-- /src/pages/clinic/ClinicInventoryPage.vue -->
<template>
  <div class="app-shell">
    <div class="content">
      <bz-tabs v-model="activeTab">
        <bz-tab-pane
          label="当前库存"
          name="inventory"
        >
          <div class="list-page-stack">
            <section class="list-page-actions">
              <div class="list-page-actions-main">
                <bz-button @click="loadInventory">刷新</bz-button>
              </div>
            </section>

            <bz-card
              class="list-page-query-card"
              shadow="never"
            >
              <bz-form
                class="list-page-filter-form"
                :inline="true"
                @submit.prevent
              >
                <bz-form-item class="list-page-filter-item">
                  <div class="list-page-filter-field">
                    <div class="list-page-filter-label">货物ID</div>
                    <bz-input
                      v-model="inventoryQuery.skuId"
                      class="list-page-filter-control"
                      placeholder="按货物ID搜索 (开发中...)"
                      clearable
                      @keyup.enter="loadInventory"
                    />
                  </div>
                </bz-form-item>
              </bz-form>
            </bz-card>

            <bz-card class="list-page-result-card">
              <InventoryTable
                :rows="inventoryRows"
                :loading="loadingInventory"
                @view-ledger="onViewLedger"
              />
            </bz-card>
          </div>
        </bz-tab-pane>

        <bz-tab-pane
          label="明细台账"
          name="ledger"
        >
          <div class="list-page-stack">
            <section class="list-page-actions">
              <div class="list-page-actions-main">
                <bz-button @click="loadLedger">查询</bz-button>
                <bz-button @click="resetLedger">重置</bz-button>
              </div>
            </section>

            <bz-card
              class="list-page-query-card"
              shadow="never"
            >
              <bz-form
                class="list-page-filter-form"
                :inline="true"
                @submit.prevent
              >
                <bz-form-item class="list-page-filter-item">
                  <div class="list-page-filter-field">
                    <div class="list-page-filter-label">起始日期</div>
                    <bz-date-picker
                      v-model="ledgerQuery.startAt"
                      class="list-page-filter-control"
                      type="date"
                      value-format="YYYY-MM-DD"
                      placeholder="选择日期"
                      clearable
                    />
                  </div>
                </bz-form-item>
                <bz-form-item class="list-page-filter-item">
                  <div class="list-page-filter-field">
                    <div class="list-page-filter-label">截止日期</div>
                    <bz-date-picker
                      v-model="ledgerQuery.endAt"
                      class="list-page-filter-control"
                      type="date"
                      value-format="YYYY-MM-DD"
                      placeholder="截止日期"
                      clearable
                    />
                  </div>
                </bz-form-item>
              </bz-form>
            </bz-card>

            <bz-card
              v-if="selectedSku"
              class="selection-info"
              shadow="never"
            >
              <div class="selection-text">
                正在查看 <strong>{{ selectedSku.skuDisplayName }}</strong> 的台账记录
              </div>
              <bz-button
                size="small"
                @click="clearLedgerFilter"
                >清除筛选</bz-button
              >
            </bz-card>

            <bz-card class="list-page-result-card">
              <LedgerTable
                :rows="ledgerRows"
                :loading="loadingLedger"
              />
            </bz-card>
          </div>
        </bz-tab-pane>
      </bz-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";

import { pageInventory, pageLedger } from "../../api/clinic";
import InventoryTable from "../../components/clinic/InventoryTable.vue";
import LedgerTable from "../../components/clinic/LedgerTable.vue";
import type { ItemInventory, ItemLedger } from "../../types/clinic";

const activeTab = ref<"inventory" | "ledger">("inventory");

const inventoryRows = ref<ItemInventory[]>([]);
const loadingInventory = ref(false);
const inventoryQuery = reactive({
  skuId: undefined as string | undefined,
  page: {
    pageNo: 1,
    pageSize: 50,
  },
});

const ledgerRows = ref<ItemLedger[]>([]);
const loadingLedger = ref(false);
const selectedSku = ref<ItemInventory | null>(null);
const ledgerQuery = reactive({
  skuId: undefined as string | undefined,
  startAt: undefined as string | undefined,
  endAt: undefined as string | undefined,
  page: {
    pageNo: 1,
    pageSize: 100,
  },
});

async function loadInventory() {
  loadingInventory.value = true;
  try {
    const res = await pageInventory(inventoryQuery);
    inventoryRows.value = res.elements;
  } finally {
    loadingInventory.value = false;
  }
}

async function loadLedger() {
  loadingLedger.value = true;
  try {
    const res = await pageLedger(ledgerQuery);
    ledgerRows.value = res.elements;
  } finally {
    loadingLedger.value = false;
  }
}

function resetLedger() {
  ledgerQuery.startAt = undefined;
  ledgerQuery.endAt = undefined;
  ledgerQuery.skuId = undefined;
  selectedSku.value = null;
  loadLedger();
}

function onViewLedger(item: ItemInventory) {
  selectedSku.value = item;
  ledgerQuery.skuId = item.skuId;
  activeTab.value = "ledger";
  loadLedger();
}

function clearLedgerFilter() {
  selectedSku.value = null;
  ledgerQuery.skuId = undefined;
  loadLedger();
}

onMounted(() => {
  loadInventory();
  loadLedger();
});
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.selection-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.selection-text {
  font-size: 13px;
  color: var(--text-main);
}
</style>
