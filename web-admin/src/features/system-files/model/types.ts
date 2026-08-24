export type StorageNodeType = "FOLDER" | "FILE";
export type OwnerType = "USER" | "APPLICATION";
export type StorageSortBy = "NAME" | "SIZE" | "TYPE" | "UPDATED_AT";
export type StorageSortOrder = "ASC" | "DESC";
export interface StorageItem {
  id: string;
  type: StorageNodeType;
  name: string;
  parentId?: string | null;
  size?: number | null;
  contentType?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface SystemFileItem extends StorageItem {
  ownerType: OwnerType;
  ownerId: string;
}

export interface PhysicalFileDetail {
  logicalFileId: string;
  logicalFileName: string;
  logicalOwnerType: OwnerType;
  logicalOwnerId: string;
  logicalParentId?: string | null;
  physicalFileId: string;
  hash: string;
  relativePath: string;
  absolutePath: string;
  fileName: string;
  fileSize: number;
  contentType?: string | null;
  refCount: number;
  physicalCreatedAt?: string | null;
}
