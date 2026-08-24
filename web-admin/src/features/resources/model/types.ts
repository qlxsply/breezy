// /src/types/resource-admin.ts

export type ResourceType = "MENU" | "BUTTON" | "FUNCTION" | "FEATURE" | "DATA";
export type ResourceNodeType = "DIRECTORY" | "MENU" | "BUTTON" | "FUNCTION" | "FEATURE" | "DATA";
export type ResourceScope = "TOOL" | "SETTING" | "INFO" | "NONE";
export type ResourceOpenMode = "NONE" | "MODAL" | "PAGE";
export type ResourceLevel = "SYSTEM" | "CUSTOM";

/**
 * 资源定义：用于授权与访问控制的统一抽象
 * - type=MENU 时可通过 scope 决定是否进入搜索入口
 * - level=SYSTEM 表示系统内置资源，不允许编辑/删除
 */
export interface ResourceEntry {
  id: string;
  parentId?: string | null;
  nodeType?: ResourceNodeType;
  name: string;
  icon?: string;
  description?: string;
  code: string; // 唯一编码：菜单/操作 code
  type: ResourceType;
  scope: ResourceScope; // 仅 MENU 时生效
  openMode: ResourceOpenMode;
  url: string; // openMode=PAGE 时使用的路由路径
  orderNo: number;
  level: ResourceLevel;
  enabled: boolean;
  guestAccess: boolean;
  missingApis?: boolean;
}

export type ResourceEntryCreate = Omit<ResourceEntry, "id">;
export type ResourceEntryUpdate = ResourceEntry;
