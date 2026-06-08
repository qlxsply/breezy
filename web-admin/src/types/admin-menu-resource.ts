export type AdminMenuResourceType = "MENU" | "BUTTON" | "FEATURE" | "DATA";
export type AdminMenuResourceScope = "TOOL" | "SETTING" | "INFO" | "NONE";
export type AdminMenuResourceOpenMode = "NONE" | "PAGE" | "MODAL";

export interface AdminMenuResourceEntry {
  id: string;
  parentId: string | null;
  name: string;
  code: string;
  type: AdminMenuResourceType;
  scope: AdminMenuResourceScope;
  openMode: AdminMenuResourceOpenMode;
  url: string;
  orderNo: number;
  enabled: boolean;
}

export interface AdminMenuTreeNode {
  id: string;
  name: string;
  code: string;
  to?: string;
  children: AdminMenuTreeNode[];
  orderNo: number;
}
