export type UserToolEntryType = "MENU" | "DATA";
export type UserToolEntryScope = "TOOL" | "NONE";
export type UserToolEntryOpenMode = "NONE" | "MODAL" | "PAGE";
export type UserToolEntryLevel = "SYSTEM" | "CUSTOM";

export interface UserToolEntry {
  id: string;
  parentId?: string | null;
  name: string;
  icon?: string;
  description?: string;
  code: string;
  type: UserToolEntryType;
  scope: UserToolEntryScope;
  openMode: UserToolEntryOpenMode;
  url: string;
  loadTarget: string;
  orderNo: number;
  level: UserToolEntryLevel;
  enabled: boolean;
  guestAccess: boolean;
}

export type ToolPageEntry = UserToolEntry & {
  type: "MENU";
};

export type SearchModeType = "setting" | "info" | "tool";

export interface SearchBadge {
  label: string;
  type: SearchModeType;
  className: string;
}

export interface ConfigActionContext {
  toggleSearchMode: () => void;
  showAbout: () => void;
}

export type ResultItem = { kind: string; resource: ToolPageEntry; score: number };

export interface ResultGroup {
  title: string;
  items: ResultItem[];
}
