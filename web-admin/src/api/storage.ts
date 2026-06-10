import type {
  StorageFolderCreateReq,
  StorageFolderRenameReq,
  StorageItem,
  StorageListQuery,
} from "../types/file-storage";
import { getAuthToken } from "../utils/authStorage";
import { API_BASE_URL, del, get, post, put } from "./http";

interface ApiResponse<T> {
  success: boolean;
  code: string;
  msg: string;
  data: T;
}

const BASE = "/sys/storage";

function buildListUrl(query?: StorageListQuery): string {
  if (!query) {
    return `${BASE}/list`;
  }
  const params = new URLSearchParams();
  if (query.parentId) {
    params.set("parentId", query.parentId);
  }
  if (query.keyword && query.keyword.trim()) {
    params.set("keyword", query.keyword.trim());
  }
  if (typeof query.recursive === "boolean") {
    params.set("recursive", String(query.recursive));
  }
  if (query.sortBy) {
    params.set("sortBy", query.sortBy);
  }
  if (query.sortOrder) {
    params.set("sortOrder", query.sortOrder);
  }
  const queryString = params.toString();
  return queryString ? `${BASE}/list?${queryString}` : `${BASE}/list`;
}

export function listStorageItems(query?: StorageListQuery): Promise<StorageItem[]> {
  return get<StorageItem[]>(buildListUrl(query));
}

export function createStorageFolder(req: StorageFolderCreateReq): Promise<string> {
  return post<string>(`${BASE}/folders`, req);
}

export function renameStorageFolder(folderId: string, req: StorageFolderRenameReq): Promise<void> {
  return put<void>(`${BASE}/folders/${encodeURIComponent(folderId)}`, req);
}

export function renameStorageFile(fileId: string, req: StorageFolderRenameReq): Promise<void> {
  return put<void>(`${BASE}/files/${encodeURIComponent(fileId)}`, req);
}

export function moveStorageFolder(folderId: string, targetParentId?: string): Promise<void> {
  return put<void>(`${BASE}/folders/${encodeURIComponent(folderId)}/move`, {
    targetParentId: targetParentId || null,
  });
}

export function moveStorageFile(fileId: string, targetParentId?: string): Promise<void> {
  return put<void>(`${BASE}/files/${encodeURIComponent(fileId)}/move`, {
    targetParentId: targetParentId || null,
  });
}

export function deleteStorageFolder(folderId: string, recursive = false): Promise<void> {
  return del<void>(`${BASE}/folders/${encodeURIComponent(folderId)}?recursive=${recursive}`);
}

export function deleteStorageFile(fileId: string): Promise<boolean> {
  return del<boolean>(`${BASE}/files/${encodeURIComponent(fileId)}`);
}

export async function uploadStorageFile(file: File, parentId?: string): Promise<string> {
  const token = getAuthToken();
  const form = new FormData();
  form.append("file", file);
  if (parentId) {
    form.append("parentId", parentId);
  }

  const resp = await fetch(`${API_BASE_URL}${BASE}/upload`, {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    body: form,
  });

  if (!resp.ok) {
    throw new Error(`HTTP ${resp.status} ${resp.statusText}`);
  }

  const json = (await resp.json()) as ApiResponse<string>;
  if (!json.success) {
    throw new Error(json.msg || `API Error: ${json.code}`);
  }
  return json.data;
}
