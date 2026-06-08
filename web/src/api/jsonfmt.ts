import type {
  JsonFmtRecordBatchDeleteRequest,
  JsonFmtRecordDetail,
  JsonFmtRecordListItem,
  JsonFmtRecordRenameRequest,
  JsonFmtRecordReorderRequest,
  JsonFmtRecordSaveRequest,
} from "../types/jsonfmt";
import { del, get, post, put } from "./http";

const BASE = "/jsonfmt/records";

export function listJsonFmtRecords(keyword?: string): Promise<JsonFmtRecordListItem[]> {
  const params = new URLSearchParams();
  if (keyword && keyword.trim()) {
    params.set("keyword", keyword.trim());
  }
  const query = params.toString();
  return get<JsonFmtRecordListItem[]>(query ? `${BASE}?${query}` : BASE);
}

export function getJsonFmtRecord(recordId: string): Promise<JsonFmtRecordDetail> {
  return get<JsonFmtRecordDetail>(`${BASE}/${encodeURIComponent(recordId)}`);
}

export function saveJsonFmtRecord(req: JsonFmtRecordSaveRequest): Promise<JsonFmtRecordDetail> {
  return post<JsonFmtRecordDetail>(BASE, req);
}

export function renameJsonFmtRecord(
  recordId: string,
  req: JsonFmtRecordRenameRequest,
): Promise<boolean> {
  return put<boolean>(`${BASE}/${encodeURIComponent(recordId)}/name`, req);
}

export function reorderJsonFmtRecords(req: JsonFmtRecordReorderRequest): Promise<boolean> {
  return put<boolean>(`${BASE}/order`, req);
}

export function deleteJsonFmtRecord(recordId: string): Promise<boolean> {
  return del<boolean>(`${BASE}/${encodeURIComponent(recordId)}`);
}

export function batchDeleteJsonFmtRecords(req: JsonFmtRecordBatchDeleteRequest): Promise<boolean> {
  return post<boolean>(`${BASE}/batch-delete`, req);
}
