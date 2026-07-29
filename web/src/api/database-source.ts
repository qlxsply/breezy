// /src/api/database-source.ts
import type {
  DatabaseColumn,
  DatabaseSchema,
  DatabaseSchemaBasicInfo,
  DatabaseSource,
  DatabaseSourceConnectionInfo,
  DatabaseSourcePageRequest,
  DatabaseSourceSimple,
  DatabaseTable,
  TestConnectionRes,
  UpsertDatabaseSourceRequest,
} from "../types/database-source";
import type { PageResult, PageRule, SortRule } from "../types/page";
import { del, get, post, put } from "./http";

const BASE = "/database-sources";

// --- 数据源管理 ---

export function pageDatabaseSources(
  req: DatabaseSourcePageRequest,
): Promise<PageResult<DatabaseSource>> {
  return post<PageResult<DatabaseSource>>(`${BASE}/page`, req);
}

export function listSimpleDatabaseSources(): Promise<DatabaseSourceSimple[]> {
  return get<DatabaseSourceSimple[]>(`${BASE}/simple-list`);
}

export function getDatabaseSource(id: string): Promise<DatabaseSource> {
  return get<DatabaseSource>(`${BASE}/${id}`);
}

export function upsertDatabaseSource(req: UpsertDatabaseSourceRequest): Promise<string> {
  return post<string>(`${BASE}`, req);
}

export function deleteDatabaseSource(id: string): Promise<void> {
  return del<void>(`${BASE}/${id}`);
}

export function testDatabaseSource(id: string): Promise<void> {
  return post<void>(`${BASE}/${id}/test`, {});
}

export function testAllDatabaseSources(): Promise<void> {
  return post<void>(`${BASE}/test-all`, {});
}

export function getDatabaseSourceConnectionInfo(id: string): Promise<DatabaseSourceConnectionInfo> {
  return get<DatabaseSourceConnectionInfo>(`${BASE}/${id}/connection-info`);
}

export function testDatabaseSourceConfig(
  req: UpsertDatabaseSourceRequest,
): Promise<TestConnectionRes> {
  return post<TestConnectionRes>(`${BASE}/test-config`, req);
}

// --- 数据库管理 ---

export function listAvailableDatabases(dataSourceId: string): Promise<string[]> {
  return get<string[]>(`${BASE}/${dataSourceId}/available-databases`);
}

export function createDatabaseSchema(req: {
  dataSourceId: string;
  databaseName: string;
  alias: string;
  remarkCustom: string;
}): Promise<string> {
  return post<string>(`${BASE}/database-schemas`, req);
}

export function listDatabaseSchemas(
  dataSourceId?: string,
  options?: {
    unboundOnly?: boolean;
    sortBy?: "DATA_SOURCE" | "CREATED_AT";
    sortDirection?: "ASC" | "DESC";
  },
): Promise<DatabaseSchema[]> {
  return post<DatabaseSchema[]>(`${BASE}/database-schemas/list`, {
    dataSourceId,
    unboundOnly: options?.unboundOnly,
    sortBy: options?.sortBy,
    sortDirection: options?.sortDirection,
  });
}

export function updateDatabaseSchemaInfo(
  id: string,
  data: { alias: string; remarkCustom: string },
): Promise<void> {
  return put<void>(`${BASE}/database-schemas/${id}/info`, data);
}

export function getDatabaseSchema(id: string): Promise<DatabaseSchema> {
  return get<DatabaseSchema>(`${BASE}/database-schemas/${id}`);
}

export function getDatabaseSchemaBasicInfo(id: string): Promise<DatabaseSchemaBasicInfo> {
  return get<DatabaseSchemaBasicInfo>(`${BASE}/database-schemas/${id}/basic-info`);
}

export function deleteDatabaseSchema(id: string): Promise<void> {
  return del<void>(`${BASE}/database-schemas/${id}`);
}

export function refreshDatabaseSchemaMetadata(id: string): Promise<void> {
  return post<void>(`${BASE}/database-schemas/${id}/refresh`, {});
}

export function rebindDatabaseSchema(id: string, dataSourceId: string): Promise<void> {
  return put<void>(`${BASE}/database-schemas/${id}/source`, { dataSourceId });
}

// --- 表与列全量查询（用于前端过滤） ---

export function listAllTables(databaseId: string): Promise<DatabaseTable[]> {
  return get<DatabaseTable[]>(`${BASE}/database-schemas/${databaseId}/tables/all`);
}

export function listAllColumns(tableId: string): Promise<DatabaseColumn[]> {
  return get<DatabaseColumn[]>(`${BASE}/tables/${tableId}/columns/all`);
}

// --- 信息更新 (别名与备注) ---

export function updateTableInfo(
  id: string,
  data: { alias: string; remarkCustom: string },
): Promise<void> {
  return put<void>(`${BASE}/tables/${id}/info`, data);
}

export function updateColumnInfo(
  id: string,
  data: { alias: string; remarkCustom: string },
): Promise<void> {
  return put<void>(`${BASE}/columns/${id}/info`, data);
}

// --- 分页查询（保留备用） ---

export function pageTables(req: {
  databaseId: string;
  nameLike?: string;
  page?: PageRule;
  sort?: SortRule;
}): Promise<PageResult<DatabaseTable>> {
  return post<PageResult<DatabaseTable>>(`${BASE}/tables/page`, req);
}

export function pageColumns(req: {
  tableId: string;
  nameLike?: string;
  page?: PageRule;
  sort?: SortRule;
}): Promise<PageResult<DatabaseColumn>> {
  return post<PageResult<DatabaseColumn>>(`${BASE}/columns/page`, req);
}
