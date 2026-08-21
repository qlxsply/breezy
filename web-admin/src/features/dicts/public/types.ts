export interface DictionaryItem {
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

export interface PublicDictionaryItem {
  itemCode: string;
  itemLabel: string;
  itemValue: string;
  tagColor: string | null;
  tagType: string | null;
}
