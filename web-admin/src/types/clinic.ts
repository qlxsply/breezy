// web/src/types/clinic.ts

export type ItemCategory = "DRUG" | "CONSUMABLE" | "EQUIPMENT";

export const DEFAULT_ITEM_CATEGORY: ItemCategory = "DRUG";

export const ITEM_CATEGORY_OPTIONS: Array<{ value: ItemCategory; label: string }> = [
  { value: "DRUG", label: "药品" },
  { value: "CONSUMABLE", label: "耗材" },
  { value: "EQUIPMENT", label: "设备" },
];

export interface ItemSku {
  id: string;
  spuId: string;
  category: ItemCategory;
  displayName: string;
  manufacturer?: string;
  spec?: string;
  enabled: boolean;
}

export interface Supplier {
  id: string;
  name: string;
  enabled: boolean;
  remark?: string;
}

export type LedgerBizType = "PURCHASE" | "SALE";

export interface ItemLedger {
  id: string;
  skuId: string;
  skuDisplayName: string;
  bizType: LedgerBizType;
  bizId: string;
  bizLineId: string;
  occurredAt: string;
  qty: number;
  unit: string;
  amountTotal: number;
  supplierId?: string;
  supplierName?: string;
  remark?: string;
}

export interface ItemInventory {
  skuId: string;
  skuDisplayName: string;
  manufacturer?: string;
  spec?: string;
  inventoryQty: number;
}

export interface PurchaseLine {
  id?: string;
  skuId?: string;
  skuDisplayName?: string;
  manufacturer?: string;
  spec?: string;
  qty: number;
  unit: string;
  lineTotalAmount: number;
  unitPrice: number;
  remark?: string;
}

export interface PurchaseSubmitLine {
  skuId?: string | null;
  category: ItemCategory;
  itemName: string;
  manufacturer?: string;
  spec?: string;
  qty: number;
  unit: string;
  lineTotalAmount: number;
  remark?: string;
}

export interface PurchaseSubmitPayload {
  supplierId?: string | null;
  supplierName?: string;
  purchasedAt: string;
  totalAmount: number;
  remark?: string;
  lines: PurchaseSubmitLine[];
}

export interface Purchase {
  id: string;
  supplierId: string;
  supplierName: string;
  purchasedAt: string;
  totalAmount: number;
  remark?: string;
}

export interface PurchaseDetail extends Purchase {
  lines: PurchaseLine[];
}

export interface SaleLine {
  id?: string;
  skuId?: string;
  skuDisplayName?: string;
  manufacturer?: string;
  spec?: string;
  qty: number;
  unit: string;
  lineTotalAmount: number;
  unitPrice: number;
  remark?: string;
}

export interface SaleSubmitLine {
  skuId?: string | null;
  category: ItemCategory;
  itemName: string;
  manufacturer?: string;
  spec?: string;
  qty: number;
  unit: string;
  lineTotalAmount: number;
  remark?: string;
}

export interface SaleSubmitPayload {
  soldAt: string;
  totalAmount: number;
  customerName?: string;
  remark?: string;
  lines: SaleSubmitLine[];
}

export interface Sale {
  id: string;
  soldAt: string;
  totalAmount: number;
  customerName?: string;
  remark?: string;
}

export interface SaleDetail extends Sale {
  linesTotalAmount: number;
  diffAmount: number;
  lines: SaleLine[];
}
