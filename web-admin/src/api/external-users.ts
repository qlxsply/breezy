import type { ExternalUserEntry } from "../types/external-user-admin";
import type { PageResult } from "../types/page";
import { get, post, put } from "./http";

const BASE = "/external-users";

interface ExternalUserPayload {
  id: string;
  account: string;
  nickname?: string;
  userType?: "USER";
  status: ExternalUserEntry["status"];
  lastLoginAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

function toEntry(payload: ExternalUserPayload): ExternalUserEntry {
  return {
    id: payload.id,
    account: payload.account || "",
    username: payload.account || "",
    nickname: payload.nickname || "",
    userType: "USER",
    status: payload.status,
    lastLoginAt: payload.lastLoginAt,
    createdAt: payload.createdAt,
    updatedAt: payload.updatedAt,
  };
}

export async function listExternalUsers(): Promise<ExternalUserEntry[]> {
  const page = await post<PageResult<ExternalUserPayload>>(`${BASE}/page`, {
    page: { pageNo: 1, pageSize: 200 },
  });
  return page.elements.map(toEntry);
}

export async function pageExternalUsers(params: {
  keyword?: string;
  status?: ExternalUserEntry["status"] | "";
  pageNo?: number;
  pageSize?: number;
}): Promise<PageResult<ExternalUserEntry>> {
  const page = await post<PageResult<ExternalUserPayload>>(`${BASE}/page`, {
    keyword: params.keyword || undefined,
    status: params.status || undefined,
    page: {
      pageNo: params.pageNo,
      pageSize: params.pageSize,
    },
  });
  return {
    ...page,
    elements: page.elements.map(toEntry),
  };
}

export async function getExternalUser(id: string): Promise<ExternalUserEntry> {
  const row = await get<ExternalUserPayload>(`${BASE}/${encodeURIComponent(id)}`);
  return toEntry(row);
}

export async function updateExternalUser(
  id: string,
  payload: { status: ExternalUserEntry["status"] },
): Promise<ExternalUserEntry> {
  const row = await put<ExternalUserPayload>(`${BASE}/${encodeURIComponent(id)}`, payload);
  return toEntry(row);
}
