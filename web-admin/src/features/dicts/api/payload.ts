import type { PageRule, SortRule } from "@admin/shared/types/pagination";

export interface DictTypePageRequest {
  code?: string;
  name?: string;
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
  valueType: "STRING" | "NUMBER" | "BOOLEAN";
  structureType: "FLAT" | "TREE";
  enabled: boolean;
  items: SaveDictTypeItemPayload[];
}

export interface DictTypePayload {
  id: string | number;
  code: string;
  name: string;
  description?: string | null;
  enumClass?: string | null;
  valueType: string;
  structureType: string;
  sourceType: string;
  enabled: boolean;
}

export interface DictItemPayload {
  id: string | number;
  dictTypeId: string | number;
  parentItemId?: string | number | null;
  itemCode: string;
  itemLabel: string;
  itemValue: string;
  sortNo: number | string;
  enabled: boolean;
  defaultItem: boolean;
  tagColor?: string | null;
  tagType?: string | null;
  extraJson?: string | null;
  description?: string | null;
}

export interface CreateDictItemPayload {
  parentItemId: string | null;
  itemCode: string;
  itemLabel: string;
  itemValue: string;
  sortNo: number;
  enabled: boolean;
  defaultItem: boolean;
  tagColor: string | null;
  tagType: string | null;
  extraJson: string | null;
  description: string | null;
}

export type UpdateDictItemPayload = Omit<CreateDictItemPayload, "itemCode">;
