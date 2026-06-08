<!-- /src/components/clinic/SaleFormDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    title="新增销售单"
    width="1100px"
    :close-on-click-modal="false"
    @close="$emit('close')"
  >
    <bz-form label-position="top">
      <div class="form-grid">
        <bz-form-item label="客户名称">
          <bz-input
            v-model="form.customerName"
            placeholder="输入客户姓名 (选填)"
          />
        </bz-form-item>
        <bz-form-item label="销售日期">
          <bz-date-picker
            v-model="form.soldAt"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </bz-form-item>
        <bz-form-item
          label="备注"
          class="span-2"
        >
          <bz-input
            v-model="form.remark"
            placeholder="可选备注信息"
          />
        </bz-form-item>
      </div>
    </bz-form>

    <div class="line-editor">
      <div class="line-header">
        <div class="line-title">项目明细</div>
        <bz-button
          size="small"
          type="primary"
          @click="addLine"
          >添加项目</bz-button
        >
      </div>

      <bz-table
        :data="form.lines"
        size="small"
        :row-key="rowKey"
      >
        <bz-table-column
          label="货物名称"
          min-width="220"
        >
          <template #default="scope">
            <SkuSelect
              v-model="scope.row.itemName"
              placeholder="搜索并选择已有货物..."
              :allow-create="false"
              @select="(sku) => onSkuSelect(scope.$index, sku)"
              @typing="onSkuTyping(scope.$index)"
            />
          </template>
        </bz-table-column>
        <bz-table-column
          label="类型"
          width="120"
        >
          <template #default="scope">
            <bz-select
              v-model="scope.row.category"
              disabled
            >
              <bz-option
                v-for="opt in categoryOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </bz-select>
          </template>
        </bz-table-column>
        <bz-table-column
          label="厂家"
          width="140"
        >
          <template #default="scope">
            <bz-input
              v-model="scope.row.manufacturer"
              disabled
            />
          </template>
        </bz-table-column>
        <bz-table-column
          label="规格"
          width="120"
        >
          <template #default="scope">
            <bz-input
              v-model="scope.row.spec"
              disabled
            />
          </template>
        </bz-table-column>
        <bz-table-column
          label="数量"
          width="90"
        >
          <template #default="scope">
            <bz-input-number
              v-model="scope.row.qty"
              :min="1"
              @change="() => onQtyPriceChange(scope.$index)"
            />
          </template>
        </bz-table-column>
        <bz-table-column
          label="单位"
          width="80"
        >
          <template #default="scope">
            <bz-input v-model="scope.row.unit" />
          </template>
        </bz-table-column>
        <bz-table-column
          label="单价"
          width="120"
        >
          <template #default="scope">
            <bz-input-number
              v-model="scope.row.unitPrice"
              :min="0"
              @change="() => onQtyPriceChange(scope.$index)"
            />
          </template>
        </bz-table-column>
        <bz-table-column
          label="总金额"
          width="120"
        >
          <template #default="scope">
            <bz-input-number
              v-model="scope.row.lineTotalAmount"
              :min="0"
              @change="() => onLineTotalChange(scope.$index)"
            />
          </template>
        </bz-table-column>
        <bz-table-column
          label="操作"
          width="80"
        >
          <template #default="scope">
            <bz-button
              type="danger"
              text
              @click="removeLine(scope.$index)"
              >删除</bz-button
            >
          </template>
        </bz-table-column>
      </bz-table>

      <div class="total-bar">
        <bz-text type="info">项目数: {{ form.lines.length }}</bz-text>
        <div class="total-inputs">
          <bz-text type="info">实收总金额: ￥</bz-text>
          <bz-input-number
            v-model="form.totalAmount"
            :min="0"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <bz-button @click="$emit('close')">取消</bz-button>
      <bz-button
        type="primary"
        :loading="loading"
        @click="submit"
        >提交</bz-button
      >
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
import { reactive } from "vue";

import type { ItemSku, SaleSubmitLine, SaleSubmitPayload } from "../../types/clinic";
import { DEFAULT_ITEM_CATEGORY, ITEM_CATEGORY_OPTIONS } from "../../types/clinic";
import { message } from "../../utils/message";
import SkuSelect from "./SkuSelect.vue";

defineProps<{
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "submit", data: SaleSubmitPayload): void;
}>();

interface SaleLineDraft extends SaleSubmitLine {
  skuId: string | null;
  unitPrice: number;
}

const categoryOptions = ITEM_CATEGORY_OPTIONS;

const form = reactive<SaleSubmitPayload & { lines: SaleLineDraft[] }>({
  customerName: "",
  soldAt: new Date().toISOString().split("T")[0],
  remark: "",
  totalAmount: 0,
  lines: [
    {
      skuId: null,
      category: DEFAULT_ITEM_CATEGORY,
      itemName: "",
      manufacturer: "",
      spec: "",
      qty: 1,
      unit: "盒",
      unitPrice: 0,
      lineTotalAmount: 0,
    },
  ],
});

const rowKey = (_row: SaleLineDraft, index: number) => index;

function addLine() {
  form.lines.push({
    skuId: null,
    category: DEFAULT_ITEM_CATEGORY,
    itemName: "",
    manufacturer: "",
    spec: "",
    qty: 1,
    unit: "盒",
    unitPrice: 0,
    lineTotalAmount: 0,
  });
}

function removeLine(idx: number) {
  form.lines.splice(idx, 1);
  syncGlobalTotal();
}

function onSkuSelect(idx: number, sku: ItemSku) {
  const line = form.lines[idx];
  line.skuId = sku.id;
  line.itemName = sku.displayName;
  line.manufacturer = sku.manufacturer || "";
  line.spec = sku.spec || "";
  line.category = sku.category;
}

function onSkuTyping(idx: number) {
  const line = form.lines[idx];
  line.skuId = null;
  line.category = DEFAULT_ITEM_CATEGORY;
  line.manufacturer = "";
  line.spec = "";
}

function onQtyPriceChange(idx: number) {
  const line = form.lines[idx];
  line.lineTotalAmount = Number((line.qty * line.unitPrice).toFixed(2));
  syncGlobalTotal();
}

function onLineTotalChange(idx: number) {
  const line = form.lines[idx];
  if (line.qty > 0) {
    line.unitPrice = Number((line.lineTotalAmount / line.qty).toFixed(4));
  }
  syncGlobalTotal();
}

function syncGlobalTotal() {
  form.totalAmount = form.lines.reduce((sum, line) => sum + (line.lineTotalAmount || 0), 0);
}

function submit() {
  if (form.lines.length === 0) return message.error("请至少添加一项明细");

  for (const line of form.lines) {
    if (!line.itemName) return message.error("明细项名称不能为空");
    if (!line.skuId) return message.error("销售明细必须选择已有货物");
    if (line.qty <= 0) return message.error("数量必须大于0");
  }

  syncGlobalTotal();
  const payload: SaleSubmitPayload = {
    customerName: form.customerName,
    soldAt: form.soldAt,
    remark: form.remark,
    totalAmount: form.totalAmount,
    lines: form.lines.map((line) => ({
      skuId: line.skuId,
      category: line.category,
      itemName: line.itemName,
      manufacturer: line.manufacturer,
      spec: line.spec,
      qty: line.qty,
      unit: line.unit,
      lineTotalAmount: line.lineTotalAmount,
      remark: line.remark,
    })),
  };

  emit("submit", payload);
}
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.span-2 {
  grid-column: span 2;
}

.line-editor {
  margin-top: 12px;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 12px;
  background: #fbfdff;
}

.line-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.line-title {
  font-size: 14px;
  font-weight: 700;
}

.total-bar {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.total-inputs {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>
