import type { PageQuery } from "./page";

export interface SchemaSnapshotItem {
  id: string;
  managedDatabaseId: string;
  name: string;
  remark?: string;
  dbType?: string;
  dbVersion?: string;
  schemaName?: string;
  logicalFileId: string;
  sqlLogicalFileId?: string;
  contentHash: string;
  createdAt: string;
}

export interface SchemaDdlItem {
  id: string;
  managedDatabaseId: string;
  sourceSnapshotId?: string;
  targetSnapshotId: string;
  name: string;
  remark?: string;
  dbType?: string;
  dbVersion?: string;
  schemaName?: string;
  logicalFileId: string;
  contentHash: string;
  createdAt: string;
}

export interface CreateSchemaSnapshotRequest {
  managedDatabaseId: string;
  name: string;
  remark?: string;
  selectedObjectIds?: string[];
}

export interface SnapshotSelectableObject {
  id: string;
  tableName: string;
  tableSchema?: string;
  tableType?: string;
  alias?: string;
}

export interface UpdateSchemaSnapshotRequest {
  name: string;
  remark?: string;
}

export interface SchemaSnapshotPageRequest extends PageQuery {
  managedDatabaseId?: string;
  nameLike?: string;
}

export interface CreateSchemaDdlRequest {
  sourceSnapshotId?: string;
  targetSnapshotId: string;
  name: string;
  remark?: string;
}

export interface UpdateSchemaDdlRequest {
  name: string;
  remark?: string;
}

export interface SchemaDdlPageRequest extends PageQuery {
  managedDatabaseId?: string;
  nameLike?: string;
}

export interface SchemaDiffRequest {
  refDbId: string;
  targetDbId: string;
}

export interface SchemaDiffResult {
  addedCount: number;
  removedCount: number;
  changedCount: number;
  changeLogXml: string;
  changeSql: string;
}
