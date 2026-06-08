import { API_BASE_URL, get } from "@admin/api/http";
import type {
  PhysicalFileDetail,
  StorageListQuery,
  StorageSortBy,
  StorageSortOrder,
  SystemFileItem,
} from "@admin/types/file-storage";
import { getAuthToken } from "@admin/utils/authStorage";

const BASE = "/sys/files";

function buildNodesUrl(query?: StorageListQuery): string {
  if (!query) {
    return `${BASE}/admin/nodes`;
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
  return queryString ? `${BASE}/admin/nodes?${queryString}` : `${BASE}/admin/nodes`;
}

export function listSystemNodes(query?: StorageListQuery): Promise<SystemFileItem[]> {
  return get<SystemFileItem[]>(buildNodesUrl(query));
}

export function getLogicalFilePhysicalDetail(logicalFileId: string): Promise<PhysicalFileDetail> {
  return get<PhysicalFileDetail>(
    `${BASE}/admin/logical-files/${encodeURIComponent(logicalFileId)}/physical`,
  );
}

export function listPhysicalFileLogicalRefs(
  physicalFileId: string,
  keyword?: string,
  sortBy: StorageSortBy = "NAME",
  sortOrder: StorageSortOrder = "ASC",
): Promise<SystemFileItem[]> {
  const params = new URLSearchParams();
  if (keyword && keyword.trim()) {
    params.set("keyword", keyword.trim());
  }
  params.set("sortBy", sortBy);
  params.set("sortOrder", sortOrder);
  return get<SystemFileItem[]>(
    `${BASE}/admin/physical/${encodeURIComponent(physicalFileId)}/logical-refs?${params.toString()}`,
  );
}

export async function fetchSystemFileView(fileId: string): Promise<Blob> {
  const token = getAuthToken();
  const resp = await fetch(`${API_BASE_URL}${BASE}/view/${encodeURIComponent(fileId)}`, {
    method: "GET",
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!resp.ok) {
    throw new Error(`HTTP ${resp.status} ${resp.statusText}`);
  }
  return resp.blob();
}

function parseFileNameFromContentDisposition(contentDisposition: string): string | undefined {
  if (!contentDisposition) {
    return undefined;
  }

  const filenameStar = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (filenameStar?.[1]) {
    try {
      return decodeURIComponent(filenameStar[1]);
    } catch (_err) {
      return filenameStar[1];
    }
  }

  const filenameQuoted = contentDisposition.match(/filename="([^"]+)"/i);
  if (filenameQuoted?.[1]) {
    try {
      return decodeURIComponent(filenameQuoted[1]);
    } catch (_err) {
      return filenameQuoted[1];
    }
  }

  const filenamePlain = contentDisposition.match(/filename=([^;]+)/i);
  if (filenamePlain?.[1]) {
    return filenamePlain[1].trim();
  }
  return undefined;
}

export async function downloadSystemFile(
  fileId: string,
  fallbackName?: string,
): Promise<{ blob: Blob; fileName: string }> {
  const token = getAuthToken();
  const resp = await fetch(`${API_BASE_URL}${BASE}/download/${encodeURIComponent(fileId)}`, {
    method: "GET",
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!resp.ok) {
    throw new Error(`HTTP ${resp.status} ${resp.statusText}`);
  }

  const contentDisposition = resp.headers.get("content-disposition") || "";
  const fileName =
    parseFileNameFromContentDisposition(contentDisposition) || fallbackName || `${fileId}.bin`;

  return {
    blob: await resp.blob(),
    fileName,
  };
}
