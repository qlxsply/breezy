export type ManageResourceType = "DIRECTORY" | "MENU" | "FUNCTION" | "BUTTON";

export interface ResourcePermissionOption {
  id: string;
  code: string;
  name: string;
  userScope: "INTERNAL" | "EXTERNAL";
}

export interface ResourceManageEntry {
  id: string;
  parentId?: string | null;
  code: string;
  name: string;
  resourceType: ManageResourceType;
  path?: string | null;
  component?: string | null;
  icon?: string | null;
  sortNo: number;
  visible: boolean;
  enabled: boolean;
  defaultEntry: boolean;
  systemBuiltin: boolean;
  remark?: string | null;
  permissionIds: string[];
  children: ResourceManageEntry[];
}

export interface ResourceManageSaveRequest {
  parentId?: string | null;
  code: string;
  name: string;
  resourceType: ManageResourceType;
  path?: string | null;
  component?: string | null;
  icon?: string | null;
  sortNo: number;
  visible: boolean;
  enabled: boolean;
  defaultEntry: boolean;
  systemBuiltin: boolean;
  remark?: string | null;
}

export interface ResourceManagePermissionSelection {
  permissionIds: string[];
}
