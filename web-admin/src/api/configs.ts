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
  return post<PageResult<ConfigItem>>(`${BASE}/page`, {
    codeLike: params?.codeLike?.trim() || undefined,
    descriptionLike: params?.descriptionLike?.trim() || undefined,
    page: {
      pageNo: params?.pageNo,
      pageSize: params?.pageSize,
    },
  });
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

export interface PasswordPolicyConfig {
  passwordMinLength: number;
  passwordRequireDigit: boolean;
  passwordRequireLetter: boolean;
  passwordRequireUpper: boolean;
  passwordRequireLower: boolean;
  passwordRequireSpecial: boolean;
  passwordForceChangeOnFirstLogin: boolean;
  passwordForceChangeOnReset: boolean;
}

export function getPasswordPolicyConfig(): Promise<PasswordPolicyConfig> {
  return get<PasswordPolicyConfig>(`${BASE}/password-policy`);
}
