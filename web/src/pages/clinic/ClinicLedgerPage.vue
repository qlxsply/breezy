<!-- /src/pages/clinic/ClinicLedgerPage.vue -->
<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section class="list-page-actions">
          <div class="list-page-actions-main">
            <bz-button @click="load">查询</bz-button>
            <bz-button @click="reset">重置</bz-button>
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
                  v-model="query.skuId"
                  class="list-page-filter-control"
                  placeholder="按货物ID筛选"
                  clearable
                  @keyup.enter="load"
                />
              </div>
            </bz-form-item>
            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">业务类型</div>
                <bz-select
                  v-model="query.bizType"
                  class="list-page-filter-control"
                  placeholder="全部"
                  clearable
                >
                  <bz-option
                    label="采购入库"
                    value="PURCHASE"
                  />
                  <bz-option
                    label="销售出库"
                    value="SALE"
                  />
                </bz-select>
              </div>
            </bz-form-item>
            <bz-form-item class="list-page-filter-item">
              <div class="list-page-filter-field">
                <div class="list-page-filter-label">起始日期</div>
                <bz-date-picker
                  v-model="query.startAt"
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
                  v-model="query.endAt"
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

        <bz-card class="list-page-result-card">
          <LedgerTable
            :rows="rows"
            :loading="loading"
          />
        </bz-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";

import { pageLedger } from "../../api/clinic";
import LedgerTable from "../../components/clinic/LedgerTable.vue";
import type { ItemLedger, LedgerBizType } from "../../types/clinic";

const route = useRoute();

const rows = ref<ItemLedger[]>([]);
const loading = ref(false);

const query = reactive({
  skuId: undefined as string | undefined,
  bizType: undefined as LedgerBizType | undefined,
  supplierId: undefined as string | undefined,
  startAt: undefined as string | undefined,
  endAt: undefined as string | undefined,
  page: {
    pageNo: 1,
    pageSize: 100,
  },
});

async function load() {
  loading.value = true;
  try {
    const res = await pageLedger(query);
    rows.value = res.elements;
  } finally {
    loading.value = false;
  }
}

function reset() {
  query.skuId = undefined;
  query.bizType = undefined;
  query.supplierId = undefined;
  query.startAt = undefined;
  query.endAt = undefined;
  load();
}

onMounted(() => {
  const skuId =
    typeof route.query.skuId === "string" && route.query.skuId.trim()
      ? route.query.skuId.trim()
      : undefined;
  if (skuId) query.skuId = skuId;
  load();
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
</style>
