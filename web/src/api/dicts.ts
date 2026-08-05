import type { DictDisableValidation, DictItem, DictTypeItem } from "../types/dict-admin";
import type { PageResult, PageRule, SortRule } from "../types/page";
import { del, get, post, put } from "./http";

const TYPE_BASE = "/sys/dicts/types";
const QUERY_BASE = "/sys/dicts";
const PUBLIC_QUERY_BASE = "/public/dicts";

export interface PublicDictItem {
  itemCode: string;
  itemLabel: string;
  itemValue: string;
  tagColor: string | null;
  tagType: string | null;
}

export interface DictTypePageRequest {
  keyword?: string;
  page?: PageRule;
  sort?: SortRule;
}

export interface SaveDictTypeItemPayload {
  id?: string;
  clientKey: string;
  parentClientKey?: string | null;
  itemCode: string;
  itemLabel: string;
  itemValue: string;
  sortNo: number;
  enabled: boolean;
  defaultItem: boolean;
  tagColor?: string | null;
  tagType?: string | null;
  extraJson?: string | null;
  description?: string | null;
}

export interface SaveDictTypePayload {
  code: string;
  name: string;
  description?: string | null;
  enumClass?: string | null;
  valueType: DictTypeItem["valueType"];
  structureType: DictTypeItem["structureType"];
  enabled: boolean;
  items: SaveDictTypeItemPayload[];
}

export function listDictTypes(req: DictTypePageRequest): Promise<PageResult<DictTypeItem>> {
  return post<PageResult<DictTypeItem>>(`${TYPE_BASE}/page`, req);
}

export function getDictType(id: string): Promise<DictTypeItem> {
  return get<DictTypeItem>(`${TYPE_BASE}/${encodeURIComponent(id)}`);
}

export function createDictType(payload: SaveDictTypePayload): Promise<DictTypeItem> {
  return post<DictTypeItem>(TYPE_BASE, payload);
}

export function updateDictType(
  id: string,
  payload: Omit<SaveDictTypePayload, "code">,
): Promise<DictTypeItem> {
  return put<DictTypeItem>(`${TYPE_BASE}/${encodeURIComponent(id)}`, payload);
}

export function updateDictTypeStatus(id: string, enabled: boolean): Promise<boolean> {
  return put<boolean>(`${TYPE_BASE}/${encodeURIComponent(id)}/status`, { enabled });
}

export function deleteDictType(id: string): Promise<boolean> {
  return del<boolean>(`${TYPE_BASE}/${encodeURIComponent(id)}`);
}

export function listDictItems(typeId: string): Promise<DictItem[]> {
  return get<DictItem[]>(`${TYPE_BASE}/${encodeURIComponent(typeId)}/items`);
}

export function createDictItem(
  typeId: string,
  payload: Omit<DictItem, "id" | "dictTypeId">,
): Promise<DictItem> {
  return post<DictItem>(`${TYPE_BASE}/${encodeURIComponent(typeId)}/items`, payload);
}

export function updateDictItem(
  itemId: string,
  payload: Omit<DictItem, "id" | "dictTypeId" | "itemCode">,
): Promise<DictItem> {
  return put<DictItem>(`${TYPE_BASE}/items/${encodeURIComponent(itemId)}`, payload);
}

export function updateDictItemStatus(itemId: string, enabled: boolean): Promise<boolean> {
  return put<boolean>(`${TYPE_BASE}/items/${encodeURIComponent(itemId)}/status`, { enabled });
}

export function deleteDictItem(itemId: string): Promise<boolean> {
  return del<boolean>(`${TYPE_BASE}/items/${encodeURIComponent(itemId)}`);
}

export function sortDictItems(typeId: string, itemIds: string[]): Promise<boolean> {
  return put<boolean>(`${TYPE_BASE}/${encodeURIComponent(typeId)}/items/sort`, { itemIds });
}

export function listDictOptions(code: string): Promise<DictItem[]> {
  return get<DictItem[]>(`${QUERY_BASE}/${encodeURIComponent(code)}/items`);
}

export function listPublicDictOptions(code: string): Promise<PublicDictItem[]> {
  return get<PublicDictItem[]>(`${PUBLIC_QUERY_BASE}/${encodeURIComponent(code)}/items`);
}

export function batchListDictOptions(codes: string[]): Promise<Record<string, DictItem[]>> {
  return post<Record<string, DictItem[]>>(`${QUERY_BASE}/batch-items`, { codes });
}

export function resolveDictLabel(code: string, value: string): Promise<string | null> {
  return get<string | null>(
    `${QUERY_BASE}/${encodeURIComponent(code)}/label?value=${encodeURIComponent(value)}`,
  );
}

export function validateDisableDict(code: string): Promise<DictDisableValidation> {
  return post<DictDisableValidation>(
    `${QUERY_BASE}/${encodeURIComponent(code)}/validate-disable`,
    {},
  );
}

export function validateDisableDictItem(
  code: string,
  itemId: string,
): Promise<DictDisableValidation> {
  return post<DictDisableValidation>(
    `${QUERY_BASE}/${encodeURIComponent(code)}/items/${encodeURIComponent(itemId)}/validate-disable`,
    {},
  );
}
