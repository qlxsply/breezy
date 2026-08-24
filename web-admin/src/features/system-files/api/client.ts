import { get, getBlob, getResponse, post, type RequestOptions } from "@admin/shared/transport";

import type {
  PhysicalFileDetail,
  StorageSortBy,
  StorageSortOrder,
  SystemFileItem,
} from "../model/types";
import type { StorageListQuery } from "./payload";

const BASE = "/sys/files";

type SystemFileRequestOptions = Pick<RequestOptions, "signal">;

export function listSystemNodes(
  query: StorageListQuery = {},
  options?: SystemFileRequestOptions,
): Promise<SystemFileItem[]> {
  return post<SystemFileItem[]>(`${BASE}/admin/nodes`, query, options);
}

export function getLogicalFilePhysicalDetail(
  logicalFileId: string,
  options?: SystemFileRequestOptions,
): Promise<PhysicalFileDetail> {
  return get<PhysicalFileDetail>(
    `${BASE}/admin/logical-files/${encodeURIComponent(logicalFileId)}/physical`,
    options,
  );
}

export function listPhysicalFileLogicalRefs(
  physicalFileId: string,
  keyword?: string,
  sortBy: StorageSortBy = "NAME",
  sortOrder: StorageSortOrder = "ASC",
  options?: SystemFileRequestOptions,
): Promise<SystemFileItem[]> {
  return post<SystemFileItem[]>(
    `${BASE}/admin/physical/${encodeURIComponent(physicalFileId)}/logical-refs`,
    { keyword: keyword?.trim() || undefined, sortBy, sortOrder },
    options,
  );
}

export async function fetchSystemFileView(
  fileId: string,
  options?: SystemFileRequestOptions,
): Promise<Blob> {
  return getBlob(`${BASE}/view/${encodeURIComponent(fileId)}`, options);
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
  options?: SystemFileRequestOptions,
): Promise<{ blob: Blob; fileName: string }> {
  const resp = await getResponse(`${BASE}/download/${encodeURIComponent(fileId)}`, options);

  const contentDisposition = resp.headers.get("content-disposition") || "";
  const fileName =
    parseFileNameFromContentDisposition(contentDisposition) || fallbackName || `${fileId}.bin`;

  return {
    blob: await resp.blob(),
    fileName,
  };
}
