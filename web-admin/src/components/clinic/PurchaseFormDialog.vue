<!-- /src/components/clinic/PurchaseFormDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    title="新增采购单"
    width="1000px"
    :close-on-click-modal="false"
    @close="$emit('close')"
  >
    <bz-form label-position="top">
      <div class="form-grid">
        <bz-form-item
          label="供应商"
          class="span-2"
        >
          <SupplierSelect
            v-model="form.supplierName"
            @select="onSupplierSelect"
          />
        </bz-form-item>
        <bz-form-item label="采购日期">
          <bz-date-picker
            v-model="form.purchasedAt"
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
              placeholder="搜索已有货物，或直接输入新货物..."
              :allow-create="true"
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
              :disabled="Boolean(scope.row.skuId)"
              @change="() => onCategoryChange(scope.$index)"
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
            <bz-input v-model="scope.row.manufacturer" />
          </template>
        </bz-table-column>
        <bz-table-column
          label="规格"
          width="120"
        >
          <template #default="scope">
            <bz-input v-model="scope.row.spec" />
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
              @change="calcTotal"
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
          label="总金额"
          width="120"
        >
          <template #default="scope">
            <bz-input-number
              v-model="scope.row.lineTotalAmount"
              :min="0"
              @change="calcTotal"
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
        <bz-tag type="success">总计金额: ￥{{ form.totalAmount.toFixed(2) }}</bz-tag>
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

import type {
  ItemSku,
  PurchaseSubmitLine,
  PurchaseSubmitPayload,
  Supplier,
} from "../../types/clinic";
import { DEFAULT_ITEM_CATEGORY, ITEM_CATEGORY_OPTIONS } from "../../types/clinic";
import { message } from "../../utils/message";
import SkuSelect from "./SkuSelect.vue";
import SupplierSelect from "./SupplierSelect.vue";

defineProps<{
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "submit", data: PurchaseSubmitPayload): void;
}>();

interface PurchaseLineDraft extends PurchaseSubmitLine {
  skuId: string | null;
}

const categoryOptions = ITEM_CATEGORY_OPTIONS;

const form = reactive<PurchaseSubmitPayload & { lines: PurchaseLineDraft[] }>({
  supplierId: null as string | null,
  supplierName: "",
  purchasedAt: new Date().toISOString().split("T")[0],
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
      lineTotalAmount: 0,
    },
  ],
});

const rowKey = (_row: PurchaseLineDraft, index: number) => index;

function onSupplierSelect(s: Supplier) {
  form.supplierId = s.id;
  form.supplierName = s.name;
}

function addLine() {
  form.lines.push({
    skuId: null,
    category: DEFAULT_ITEM_CATEGORY,
    itemName: "",
    manufacturer: "",
    spec: "",
    qty: 1,
    unit: "盒",
    lineTotalAmount: 0,
  });
}

function removeLine(idx: number) {
  form.lines.splice(idx, 1);
  calcTotal();
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
  line.manufacturer = "";
  line.spec = "";
}

function onCategoryChange(idx: number) {
  form.lines[idx].skuId = null;
}

function calcTotal() {
  form.totalAmount = form.lines.reduce((sum, line) => sum + (line.lineTotalAmount || 0), 0);
}

function submit() {
  if (!form.supplierName) return message.error("请选择或输入供应商");
  if (form.lines.length === 0) return message.error("请至少添加一项明细");

  for (const line of form.lines) {
    if (!line.itemName) return message.error("明细项名称不能为空");
    if (line.qty <= 0) return message.error("数量必须大于0");
  }

  calcTotal();
  const payload: PurchaseSubmitPayload = {
    supplierId: form.supplierId,
    supplierName: form.supplierName,
    purchasedAt: form.purchasedAt,
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
}

:deep(.el-input-number) {
  width: 100%;
}
</style>
