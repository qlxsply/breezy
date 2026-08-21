export interface ResourceManagePayload {
  id: string | number;
  parentId?: string | number | null;
  code: string;
  name: string;
  resourceType: string;
  path?: string | null;
  component?: string | null;
  icon?: string | null;
  sortNo?: number;
  visible?: boolean;
  enabled?: boolean;
  defaultEntry?: boolean;
  systemBuiltin?: boolean;
  remark?: string | null;
  permissionIds?: Array<string | number>;
  children?: ResourceManagePayload[];
}

export interface PermissionPayload {
  id: string | number;
  code: string;
  name: string;
  userScope: "ADMIN" | "USER";
}

export interface PermissionSelectionPayload {
  permissionIds?: Array<string | number>;
}
