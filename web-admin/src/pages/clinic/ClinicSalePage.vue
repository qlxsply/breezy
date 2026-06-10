<!-- /src/pages/clinic/ClinicSalePage.vue -->
<template>
  <div class="app-shell">
    <div class="content">
      <div class="list-page-stack">
        <section class="list-page-actions">
          <div class="list-page-actions-main">
            <bz-button
              v-if="canManage"
              type="primary"
              @click="openCreate"
              >新增销售单</bz-button
            >
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
                <div class="list-page-filter-label">客户名称</div>
                <bz-input
                  v-model="query.customerNameLike"
                  class="list-page-filter-control"
                  placeholder="按姓名搜索"
                  clearable
                />
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
          <SaleTable
            :rows="rows"
            :loading="loading"
            :can-delete="canManage"
            @view="onView"
            @remove="onRemove"
          />
        </bz-card>

        <SaleFormDialog
          v-if="createOpen"
          :loading="submitting"
          @close="createOpen = false"
          @submit="onSubmit"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

import { createSale, deleteSale, pageSales } from "../../api/clinic";
import SaleFormDialog from "../../components/clinic/SaleFormDialog.vue";
import SaleTable from "../../components/clinic/SaleTable.vue";
import { hasApiPermission } from "../../registry/permissions.registry";
import type { Sale, SaleSubmitPayload } from "../../types/clinic";
import { bzConfirm } from "../../utils/confirm";
import { message } from "../../utils/message";

const rows = ref<Sale[]>([]);
const loading = ref(false);
const submitting = ref(false);
const createOpen = ref(false);

const query = reactive({
  customerNameLike: undefined as string | undefined,
  startAt: undefined as string | undefined,
  endAt: undefined as string | undefined,
  page: {
    pageNo: 1,
    pageSize: 50,
  },
});

const canManage = computed(() => hasApiPermission("cln.sal.edit"));

async function load() {
  loading.value = true;
  try {
    const res = await pageSales(query);
    rows.value = res.elements;
  } finally {
    loading.value = false;
  }
}

function reset() {
  query.customerNameLike = undefined;
  query.startAt = undefined;
  query.endAt = undefined;
  load();
}

function openCreate() {
  if (!canManage.value) return message.error("无权限");
  createOpen.value = true;
}

async function onSubmit(data: SaleSubmitPayload) {
  submitting.value = true;
  try {
    await createSale(data);
    message.success("销售单已保存");
    createOpen.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
}

function onView(item: Sale) {
  message.info(`详情功能开发中 (ID: ${item.id})`);
}

async function onRemove(item: Sale) {
  if (!canManage.value) return message.error("无权限");
  const confirmed = await bzConfirm({
    title: "删除销售单",
    message: "确认删除该销售单及其产生的台账记录吗？",
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }

  try {
    await deleteSale(item.id);
    message.success("已删除");
    await load();
  } catch (_e) {}
}

onMounted(() => {
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
