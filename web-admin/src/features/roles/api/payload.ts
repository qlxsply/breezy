export interface RolePayload {
  id: string;
  code: string;
  name: string;
  enabled: boolean;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface RoleGrantResourcePayload {
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

export interface RoleGrantSelectionPayload {
  resourceIds?: Array<string | number>;
}
