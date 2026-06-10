<!-- /src/pages/clinic/ClinicPurchasePage.vue -->
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
              >新增采购单</bz-button
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
          <PurchaseTable
            :rows="rows"
            :loading="loading"
            :can-delete="canManage"
            @view="onView"
            @remove="onRemove"
          />
        </bz-card>

        <PurchaseFormDialog
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

import { createPurchase, deletePurchase, pagePurchases } from "../../api/clinic";
import PurchaseFormDialog from "../../components/clinic/PurchaseFormDialog.vue";
import PurchaseTable from "../../components/clinic/PurchaseTable.vue";
import { hasUserPermissionCode } from "../../registry/user-tool-permissions.registry";
import type { Purchase, PurchaseSubmitPayload } from "../../types/clinic";
import { bzConfirm } from "../../utils/confirm";
import { message } from "../../utils/message";

const rows = ref<Purchase[]>([]);
const loading = ref(false);
const submitting = ref(false);
const createOpen = ref(false);

const query = reactive({
  startAt: undefined as string | undefined,
  endAt: undefined as string | undefined,
  page: {
    pageNo: 1,
    pageSize: 50,
  },
});

const canManage = computed(() => hasUserPermissionCode("cln.pur.edit"));

async function load() {
  loading.value = true;
  try {
    const res = await pagePurchases(query);
    rows.value = res.elements;
  } finally {
    loading.value = false;
  }
}

function reset() {
  query.startAt = undefined;
  query.endAt = undefined;
  load();
}

function openCreate() {
  if (!canManage.value) return message.error("无权限");
  createOpen.value = true;
}

async function onSubmit(data: PurchaseSubmitPayload) {
  submitting.value = true;
  try {
    await createPurchase(data);
    message.success("采购单已保存");
    createOpen.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
}

function onView(item: Purchase) {
  message.info(`详情功能开发中 (ID: ${item.id})`);
}

async function onRemove(item: Purchase) {
  if (!canManage.value) return message.error("无权限");
  const confirmed = await bzConfirm({
    title: "删除采购单",
    message: "确认删除该采购单及其产生的台账记录吗？",
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) {
    return;
  }

  try {
    await deletePurchase(item.id);
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
