<!-- /src/pages/clinic/ClinicCatalogPage.vue -->
<template>
  <div class="app-shell">
    <div class="content">
      <bz-tabs v-model="activeTab">
        <bz-tab-pane
          label="货物目录"
          name="sku"
        >
          <div class="list-page-stack">
            <section class="list-page-actions">
              <div class="list-page-actions-main">
                <bz-button @click="loadSkus">刷新</bz-button>
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
                    <div class="list-page-filter-label">名称</div>
                    <bz-input
                      v-model="skuQuery.nameLike"
                      class="list-page-filter-control"
                      placeholder="按名称搜索"
                      clearable
                      @keyup.enter="loadSkus"
                    />
                  </div>
                </bz-form-item>
              </bz-form>
            </bz-card>

            <bz-card class="list-page-result-card">
              <bz-table
                v-loading="loadingSkus"
                :data="skuRows"
                empty-text="暂无货物"
                size="small"
              >
                <bz-table-column
                  prop="displayName"
                  label="名称"
                  min-width="200"
                />
                <bz-table-column
                  prop="manufacturer"
                  label="厂家"
                  min-width="160"
                >
                  <template #default="scope">
                    {{ scope.row.manufacturer || "-" }}
                  </template>
                </bz-table-column>
                <bz-table-column
                  prop="spec"
                  label="规格"
                  min-width="140"
                >
                  <template #default="scope">
                    {{ scope.row.spec || "-" }}
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="状态"
                  width="120"
                >
                  <template #default="scope">
                    <bz-tag
                      size="small"
                      :type="scope.row.enabled ? 'success' : 'info'"
                    >
                      {{ scope.row.enabled ? "启用" : "禁用" }}
                    </bz-tag>
                  </template>
                </bz-table-column>
              </bz-table>
            </bz-card>
          </div>
        </bz-tab-pane>

        <bz-tab-pane
          label="供应商管理"
          name="supplier"
        >
          <div class="list-page-stack">
            <section class="list-page-actions">
              <div class="list-page-actions-main">
                <bz-button @click="loadSuppliers">刷新</bz-button>
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
                    <div class="list-page-filter-label">名称</div>
                    <bz-input
                      v-model="supplierQuery.nameLike"
                      class="list-page-filter-control"
                      placeholder="按名称搜索"
                      clearable
                      @keyup.enter="loadSuppliers"
                    />
                  </div>
                </bz-form-item>
              </bz-form>
            </bz-card>

            <bz-card class="list-page-result-card">
              <bz-table
                v-loading="loadingSuppliers"
                :data="supplierRows"
                empty-text="暂无供应商"
                size="small"
              >
                <bz-table-column
                  prop="name"
                  label="供应商名称"
                  min-width="200"
                />
                <bz-table-column
                  prop="remark"
                  label="备注"
                  min-width="200"
                >
                  <template #default="scope">
                    {{ scope.row.remark || "-" }}
                  </template>
                </bz-table-column>
                <bz-table-column
                  label="状态"
                  width="120"
                >
                  <template #default="scope">
                    <bz-tag
                      size="small"
                      :type="scope.row.enabled ? 'success' : 'info'"
                    >
                      {{ scope.row.enabled ? "启用" : "禁用" }}
                    </bz-tag>
                  </template>
                </bz-table-column>
              </bz-table>
            </bz-card>
          </div>
        </bz-tab-pane>
      </bz-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";

import { pageSkus, pageSuppliers } from "../../api/clinic";
import type { ItemSku, Supplier } from "../../types/clinic";

const activeTab = ref<"sku" | "supplier">("sku");

const skuRows = ref<ItemSku[]>([]);
const skuQuery = reactive({
  nameLike: "",
  page: {
    pageNo: 1,
    pageSize: 50,
  },
});
const loadingSkus = ref(false);

const supplierRows = ref<Supplier[]>([]);
const supplierQuery = reactive({
  nameLike: "",
  page: {
    pageNo: 1,
    pageSize: 50,
  },
});
const loadingSuppliers = ref(false);

async function loadSkus() {
  loadingSkus.value = true;
  try {
    const res = await pageSkus(skuQuery);
    skuRows.value = res.elements;
  } finally {
    loadingSkus.value = false;
  }
}

async function loadSuppliers() {
  loadingSuppliers.value = true;
  try {
    const res = await pageSuppliers(supplierQuery);
    supplierRows.value = res.elements;
  } finally {
    loadingSuppliers.value = false;
  }
}

onMounted(() => {
  loadSkus();
  loadSuppliers();
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
