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
  resourceId: string;
  name: string;
  code: string;
  type: string;
  description?: string;
  enabled: boolean;
  selectable: boolean;
  orderNo: number;
}

export interface RoleGrantSelection {
  resourceIds: string[];
}
