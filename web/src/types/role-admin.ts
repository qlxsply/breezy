export interface RoleEntry {
  id: string;
  code: string;
  name: string;
  enabled: boolean;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface RoleCreateRequest {
  code: string;
  name: string;
  enabled: boolean;
}

export interface RoleUpdateRequest {
  code: string;
  name: string;
  enabled: boolean;
}

export interface RoleGrantResourceEntry {
  id: string;
  parentId?: string | null;
  menuId?: string | null;
  functionId?: string | null;
  name: string;
  code: string;
  type: string;
  description?: string;
  enabled: boolean;
  selectable: boolean;
  orderNo: number;
  permissionCodes: string[];
}

export interface RoleGrantSelection {
  menuIds: string[];
  functionIds: string[];
}
