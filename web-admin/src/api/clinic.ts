// web/src/api/clinic.ts

import type {
  ItemCategory,
  ItemInventory,
  ItemLedger,
  ItemSku,
  LedgerBizType,
  Purchase,
  PurchaseDetail,
  PurchaseSubmitPayload,
  Sale,
  SaleDetail,
  SaleSubmitPayload,
  Supplier,
} from "../types/clinic";
import type { PageResult, PageRule, SortRule } from "../types/page";
import { del, get, post } from "./http";

async function collectAllPages<T>(
  fetchPage: (pageNo: number) => Promise<PageResult<T>>,
): Promise<T[]> {
  const all: T[] = [];
  let pageNo = 1;
  let totalPages = 1;

  while (pageNo <= totalPages) {
    const page = await fetchPage(pageNo);
    all.push(...page.elements);
    totalPages = Math.max(page.totalPages || 0, pageNo);
    pageNo += 1;
  }

  return all;
}

// Catalog
export function pageSkus(params: {
  category?: ItemCategory;
  nameLike?: string;
  enabled?: boolean;
  page?: PageRule;
  sort?: SortRule;
}) {
  return post<PageResult<ItemSku>>("/clinic/catalog/skus/page", params);
}

export function pageSuppliers(params: {
  nameLike?: string;
  enabled?: boolean;
  page?: PageRule;
  sort?: SortRule;
}) {
  return post<PageResult<Supplier>>("/clinic/catalog/suppliers/page", params);
}

export function listSkus(
  params: { nameLike?: string; enabled?: boolean; sort?: SortRule; page?: PageRule } = {},
) {
  return collectAllPages<ItemSku>((pageNo) =>
    pageSkus({ ...params, page: { ...(params.page || {}), pageNo } }),
  );
}

export function listSuppliers(
  params: { nameLike?: string; enabled?: boolean; sort?: SortRule; page?: PageRule } = {},
) {
  return collectAllPages<Supplier>((pageNo) =>
    pageSuppliers({ ...params, page: { ...(params.page || {}), pageNo } }),
  );
}

// Purchase
export function createPurchase(data: PurchaseSubmitPayload) {
  return post<string>("/clinic/purchases", data);
}

export function pagePurchases(params: {
  supplierId?: string;
  startAt?: string;
  endAt?: string;
  page?: PageRule;
  sort?: SortRule;
}) {
  return post<PageResult<Purchase>>("/clinic/purchases/page", params);
}

export function getPurchaseDetail(id: string) {
  return get<PurchaseDetail>(`/clinic/purchases/${id}`);
}

export function deletePurchase(id: string) {
  return del<void>(`/clinic/purchases/${id}`);
}

// Sale
export function createSale(data: SaleSubmitPayload) {
  return post<string>("/clinic/sales", data);
}

export function pageSales(params: {
  customerNameLike?: string;
  startAt?: string;
  endAt?: string;
  page?: PageRule;
  sort?: SortRule;
}) {
  return post<PageResult<Sale>>("/clinic/sales/page", params);
}

export function getSaleDetail(id: string) {
  return get<SaleDetail>(`/clinic/sales/${id}`);
}

export function deleteSale(id: string) {
  return del<void>(`/clinic/sales/${id}`);
}

// Inventory & Ledger
export function pageInventory(params: { skuId?: string; page?: PageRule; sort?: SortRule }) {
  return post<PageResult<ItemInventory>>("/clinic/inventory/page", params);
}

export function pageLedger(params: {
  skuId?: string;
  bizType?: LedgerBizType;
  supplierId?: string;
  startAt?: string;
  endAt?: string;
  page?: PageRule;
  sort?: SortRule;
}) {
  return post<PageResult<ItemLedger>>("/clinic/ledger/page", params);
}
