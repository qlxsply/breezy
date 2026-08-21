export type DictValueType = "STRING" | "NUMBER" | "BOOLEAN";
export type DictStructureType = "FLAT" | "TREE";
export type DictSourceType = "BUILTIN" | "CUSTOM";

export interface DictTypeItem {
  id: string;
  code: string;
  name: string;
  description: string | null;
  enumClass: string | null;
  valueType: DictValueType;
  structureType: DictStructureType;
  sourceType: DictSourceType;
  enabled: boolean;
}

export interface DictItem {
  id: string;
  dictTypeId: string;
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
