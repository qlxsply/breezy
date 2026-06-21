// /src/api/configs.ts
import type {
  ClientIpMode,
  ConfigClientIpPreviewRes,
  ConfigItem,
  ConfigTimeOffsetPreviewRes,
  ConfigType,
} from "../types/config-admin";
import type { PageResult } from "../types/page";
import { get, post, put } from "./http";

const BASE = "/sys/configs";

export function listConfigs(params?: {
  codeLike?: string;
  descriptionLike?: string;
  pageNo?: number;
  pageSize?: number;
}): Promise<PageResult<ConfigItem>> {
  const searchParams = new URLSearchParams();
  if (params?.codeLike?.trim()) {
    searchParams.set("codeLike", params.codeLike.trim());
  }
  if (params?.descriptionLike?.trim()) {
    searchParams.set("descriptionLike", params.descriptionLike.trim());
  }
  if (params?.pageNo) {
    searchParams.set("pageNo", String(params.pageNo));
  }
  if (params?.pageSize) {
    searchParams.set("pageSize", String(params.pageSize));
  }
  const query = searchParams.toString();
  return get<PageResult<ConfigItem>>(query ? `${BASE}?${query}` : BASE);
}

export function updateConfigValue(code: string, value: string): Promise<boolean> {
  return put<boolean>(`${BASE}/${encodeURIComponent(code)}`, { value });
}

export function previewClientIp(mode: ClientIpMode): Promise<ConfigClientIpPreviewRes> {
  return post<ConfigClientIpPreviewRes>(`${BASE}/preview/client-ip`, { mode });
}

export function previewTimeOffset(params: {
  offsetSeconds: number;
  targetEpochMillis?: number;
}): Promise<ConfigTimeOffsetPreviewRes> {
  return post<ConfigTimeOffsetPreviewRes>(`${BASE}/preview/time-offset`, params);
}

export interface UserConfigItem {
  code: string;
  description: string;
  valueType: ConfigType;
  value: string;
}

export function getMyConfigs(): Promise<UserConfigItem[]> {
  return get<UserConfigItem[]>(`${BASE}/my`);
}

export function updateMyConfig(code: string, value: string): Promise<boolean> {
  return put<boolean>(`${BASE}/my/${encodeURIComponent(code)}`, { value });
}
