export type DictValueType = "STRING" | "NUMBER" | "BOOLEAN";
export type DictStructureType = "FLAT" | "TREE";
export type DictSourceType = "BUILTIN" | "CUSTOM";
export type DictUsageType = "PAGE" | "FIELD" | "CONFIG" | "API" | "MODULE";
export type DictUsageSourceType = "BOOTSTRAP" | "MANUAL" | "CODE_REGISTER";

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

export interface DictUsage {
  id: string;
  dictCode: string;
  usageType: DictUsageType;
  usageKey: string;
  usageName: string;
  moduleCode: string | null;
  pagePath: string | null;
  ownerClass: string | null;
  ownerMember: string | null;
  remark: string | null;
  active: boolean;
  sourceType: DictUsageSourceType;
}

export interface DictImpact {
  dictCode: string;
  itemValue: string | null;
  moduleCode: string | null;
  impactType: string;
  hitCount: number;
  summary: string;
}

export interface DictDisableValidation {
  allowed: boolean;
  activeUsages: DictUsage[];
  impacts: DictImpact[];
}

export interface DictOption {
  label: string;
  value: string;
}
