import type { PageResult } from "../types/page";
import type {
  CreateSchemaDdlRequest,
  CreateSchemaSnapshotRequest,
  SchemaDdlItem,
  SchemaDdlPageRequest,
  SchemaDiffRequest,
  SchemaDiffResult,
  SchemaSnapshotItem,
  SchemaSnapshotPageRequest,
  SnapshotSelectableObject,
  UpdateSchemaDdlRequest,
  UpdateSchemaSnapshotRequest,
} from "../types/schemaforge";
import { del, get, post, put } from "./http";

const BASE = "/sf";

export function createSchemaSnapshot(req: CreateSchemaSnapshotRequest): Promise<string> {
  return post<string>(`${BASE}/snapshots`, {
    ...req,
    selectedObjectIds: req.selectedObjectIds
      ?.map((item) => Number(item))
      .filter((item) => Number.isFinite(item)),
  });
}

export function listSnapshotSelectableObjects(
  managedDatabaseId: string,
): Promise<SnapshotSelectableObject[]> {
  return get<SnapshotSelectableObject[]>(
    `${BASE}/snapshots/managed-databases/${managedDatabaseId}/objects`,
  );
}

export function pageSchemaSnapshots(
  req: SchemaSnapshotPageRequest,
): Promise<PageResult<SchemaSnapshotItem>> {
  return post<PageResult<SchemaSnapshotItem>>(`${BASE}/snapshots/page`, req);
}

export function listSchemaSnapshotsForDdl(): Promise<SchemaSnapshotItem[]> {
  return get<SchemaSnapshotItem[]>(`${BASE}/snapshots/options`);
}

export function updateSchemaSnapshot(id: string, req: UpdateSchemaSnapshotRequest): Promise<void> {
  return put<void>(`${BASE}/snapshots/${encodeURIComponent(id)}`, req);
}

export function deleteSchemaSnapshot(id: string): Promise<void> {
  return del<void>(`${BASE}/snapshots/${encodeURIComponent(id)}`);
}

export function createSchemaDdl(req: CreateSchemaDdlRequest): Promise<string> {
  return post<string>(`${BASE}/ddls`, req);
}

export function pageSchemaDdls(req: SchemaDdlPageRequest): Promise<PageResult<SchemaDdlItem>> {
  return post<PageResult<SchemaDdlItem>>(`${BASE}/ddls/page`, req);
}

export function updateSchemaDdl(id: string, req: UpdateSchemaDdlRequest): Promise<void> {
  return put<void>(`${BASE}/ddls/${encodeURIComponent(id)}`, req);
}

export function deleteSchemaDdl(id: string): Promise<void> {
  return del<void>(`${BASE}/ddls/${encodeURIComponent(id)}`);
}

export function compareSchemaDiff(req: SchemaDiffRequest): Promise<SchemaDiffResult> {
  const params = new URLSearchParams();
  params.set("refDbId", String(req.refDbId));
  params.set("targetDbId", String(req.targetDbId));
  return get<SchemaDiffResult>(`${BASE}/diff?${params.toString()}`);
}
