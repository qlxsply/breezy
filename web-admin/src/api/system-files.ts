import { get, getBlob, getResponse, post } from "@admin/shared/transport";

import type {
  PhysicalFileDetail,
  StorageListQuery,
  StorageSortBy,
  StorageSortOrder,
  SystemFileItem,
} from "../types/file-storage";

const BASE = "/sys/files";

export function listSystemNodes(query?: StorageListQuery): Promise<SystemFileItem[]> {
  return post<SystemFileItem[]>(`${BASE}/admin/nodes`, query);
}

export function getLogicalFilePhysicalDetail(logicalFileId: string): Promise<PhysicalFileDetail> {
  return get<PhysicalFileDetail>(
    `${BASE}/admin/logical-files/${encodeURIComponent(logicalFileId)}/physical`,
  );
}

export function getSystemFileMeta(fileId: string): Promise<SystemFileItem> {
  return get<SystemFileItem>(`${BASE}/meta/${encodeURIComponent(fileId)}`);
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
  return getBlob(`${BASE}/view/${encodeURIComponent(fileId)}`);
}

function parseFileNameFromContentDisposition(contentDisposition: string): string | undefined {
  if (!contentDisposition) return undefined;

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
  const resp = await getResponse(`${BASE}/download/${encodeURIComponent(fileId)}`);

  const contentDisposition = resp.headers.get("content-disposition") || "";
  const fileName =
    parseFileNameFromContentDisposition(contentDisposition) || fallbackName || `${fileId}.bin`;

  return {
    blob: await resp.blob(),
    fileName,
  };
}
