import { del, get, post, put, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type { DictItem, DictTypeItem } from "../model/types";
import type {
  CreateDictItemPayload,
  DictItemPayload,
  DictTypePageRequest,
  DictTypePayload,
  SaveDictTypePayload,
  UpdateDictItemPayload,
} from "./payload";

const TYPE_BASE = "/sys/dicts/types";

function toDictTypeItem(payload: DictTypePayload): DictTypeItem {
  return {
    id: String(payload.id),
    code: payload.code,
    name: payload.name,
    description: payload.description ?? null,
    enumClass: payload.enumClass ?? null,
    valueType: payload.valueType as DictTypeItem["valueType"],
    structureType: payload.structureType as DictTypeItem["structureType"],
    sourceType: payload.sourceType as DictTypeItem["sourceType"],
    enabled: Boolean(payload.enabled),
  };
}

function toDictItem(payload: DictItemPayload): DictItem {
  return {
    id: String(payload.id),
    dictTypeId: String(payload.dictTypeId),
    parentItemId: payload.parentItemId == null ? null : String(payload.parentItemId),
    itemCode: payload.itemCode,
    itemLabel: payload.itemLabel,
    itemValue: payload.itemValue,
    sortNo: normalizeNumber(payload.sortNo),
    enabled: Boolean(payload.enabled),
    defaultItem: Boolean(payload.defaultItem),
    tagColor: payload.tagColor ?? null,
    tagType: payload.tagType ?? null,
    extraJson: payload.extraJson ?? null,
    description: payload.description ?? null,
  };
}

export async function listDictTypes(
  req: DictTypePageRequest,
  options?: Pick<RequestOptions, "signal">,
): Promise<PageResult<DictTypeItem>> {
  const page = await post<PageResult<DictTypePayload>>(`${TYPE_BASE}/page`, req, options);
  const elements = page.elements.map(toDictTypeItem);
  return {
    pageNo: normalizeNumber(page.pageNo, req.page?.pageNo ?? 1),
    pageSize: normalizeNumber(page.pageSize, req.page?.pageSize ?? 10),
    numberOfElements: normalizeNumber(page.numberOfElements, elements.length),
    totalPages: normalizeNumber(page.totalPages),
    totalElements: normalizeNumber(page.totalElements),
    elements,
  };
}

export async function getDictType(
  id: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<DictTypeItem> {
  const payload = await get<DictTypePayload>(`${TYPE_BASE}/${encodeURIComponent(id)}`, options);
  return toDictTypeItem(payload);
}

export async function createDictType(payload: SaveDictTypePayload): Promise<DictTypeItem> {
  const result = await post<DictTypePayload>(TYPE_BASE, payload);
  return toDictTypeItem(result);
}

export async function updateDictType(
  id: string,
  payload: Omit<SaveDictTypePayload, "code">,
): Promise<DictTypeItem> {
  const result = await put<DictTypePayload>(`${TYPE_BASE}/${encodeURIComponent(id)}`, payload);
  return toDictTypeItem(result);
}

export async function updateDictTypeStatus(id: string, enabled: boolean): Promise<boolean> {
  return Boolean(await put<boolean>(`${TYPE_BASE}/${encodeURIComponent(id)}/status`, { enabled }));
}

export async function deleteDictType(id: string): Promise<boolean> {
  return Boolean(await del<boolean>(`${TYPE_BASE}/${encodeURIComponent(id)}`));
}

export async function listDictItems(
  typeId: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<DictItem[]> {
  const payloads = await get<DictItemPayload[]>(
    `${TYPE_BASE}/${encodeURIComponent(typeId)}/items`,
    options,
  );
  return payloads.map(toDictItem);
}

export async function createDictItem(
  typeId: string,
  payload: CreateDictItemPayload,
): Promise<DictItem> {
  const result = await post<DictItemPayload>(
    `${TYPE_BASE}/${encodeURIComponent(typeId)}/items`,
    payload,
  );
  return toDictItem(result);
}

export async function updateDictItem(
  itemId: string,
  payload: UpdateDictItemPayload,
): Promise<DictItem> {
  const result = await put<DictItemPayload>(
    `${TYPE_BASE}/items/${encodeURIComponent(itemId)}`,
    payload,
  );
  return toDictItem(result);
}

export async function updateDictItemStatus(itemId: string, enabled: boolean): Promise<boolean> {
  return Boolean(
    await put<boolean>(`${TYPE_BASE}/items/${encodeURIComponent(itemId)}/status`, { enabled }),
  );
}

export async function sortDictItems(typeId: string, itemIds: string[]): Promise<boolean> {
  return Boolean(
    await put<boolean>(`${TYPE_BASE}/${encodeURIComponent(typeId)}/items/sort`, { itemIds }),
  );
}

function normalizeNumber(value: number | string, fallback = 0): number {
  const normalized = Number(value);
  return Number.isFinite(normalized) ? normalized : fallback;
}
