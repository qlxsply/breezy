import type { StorageSortBy, StorageSortOrder } from "../model/types";

export interface StorageListQuery {
  parentId?: string;
  keyword?: string;
  recursive?: boolean;
  sortBy?: StorageSortBy;
  sortOrder?: StorageSortOrder;
}
