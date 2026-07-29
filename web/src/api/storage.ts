import { getValidAuthToken, handleUnauthorizedResponse } from "../registry/auth-token.registry";
import type {
  StorageFolderCreateReq,
  StorageFolderRenameReq,
  StorageItem,
  StorageListQuery,
} from "../types/file-storage";
import { API_BASE_URL, del, post, put } from "./http";

interface ApiResponse<T> {
  success: boolean;
  code: string;
  msg: string;
  data: T;
}

const BASE = "/sys/storage";

export function listStorageItems(query?: StorageListQuery): Promise<StorageItem[]> {
  return post<StorageItem[]>(`${BASE}/list`, query ?? {});
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
  const token = await getValidAuthToken();
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
    if (resp.status === 401) {
      handleUnauthorizedResponse();
    }
    throw new Error(`HTTP ${resp.status} ${resp.statusText}`);
  }

  const json = (await resp.json()) as ApiResponse<string>;
  if (!json.success) {
    throw new Error(json.msg || `API Error: ${json.code}`);
  }
  return json.data;
}
