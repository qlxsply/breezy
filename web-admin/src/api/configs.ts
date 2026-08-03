import type {
  ConfigChangeResult,
  ConfigEffectiveResult,
  ConfigItem,
  ConfigType,
  ConfigValidationResult,
  JsonValue,
} from "../types/config-admin";
import type { PageResult, PageRule } from "../types/page";
import { get, post, put } from "./http";

const BASE = "/sys/configs";

export function listConfigs(params?: {
  keyword?: string;
  module?: string;
  group?: string;
  page?: PageRule;
}): Promise<PageResult<ConfigItem>> {
  return post<PageResult<ConfigItem>>(`${BASE}/page`, {
    keyword: params?.keyword?.trim() || undefined,
    module: params?.module?.trim() || undefined,
    group: params?.group?.trim() || undefined,
    page: params?.page,
  });
}

export function getConfig(configKey: string): Promise<ConfigItem> {
  return get<ConfigItem>(`${BASE}/${encodeURIComponent(configKey)}`);
}

export function getEffectiveConfig(configKey: string): Promise<ConfigEffectiveResult> {
  return get<ConfigEffectiveResult>(`${BASE}/effective/${encodeURIComponent(configKey)}`);
}

export function validateConfig(
  configKey: string,
  value: JsonValue,
): Promise<ConfigValidationResult> {
  return post<ConfigValidationResult>(`${BASE}/${encodeURIComponent(configKey)}/validate`, { value });
}

export function updateConfig(
  configKey: string,
  body: { expectedRevision: number; reason?: string; value: JsonValue },
): Promise<ConfigChangeResult> {
  return put<ConfigChangeResult>(`${BASE}/${encodeURIComponent(configKey)}`, body);
}

export function resetConfigDefault(
  configKey: string,
  body: { expectedRevision: number; reason?: string },
): Promise<ConfigChangeResult> {
  return post<ConfigChangeResult>(`${BASE}/${encodeURIComponent(configKey)}/reset-default`, body);
}

export function batchResetConfigDefaults(
  items: Array<{ configKey: string; expectedRevision: number }>,
  reason?: string,
): Promise<ConfigChangeResult[]> {
  return post<ConfigChangeResult[]>(`${BASE}/batch-reset-default`, {
    items,
    reason: reason?.trim() || undefined,
  });
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

export async function getPasswordPolicyConfig(): Promise<PasswordPolicyConfig> {
  const result = await getEffectiveConfig("system.security.password-policy");
  const value = result.effectiveValue as Record<string, JsonValue>;
  return {
    passwordMinLength: Number(value.minLength),
    passwordRequireDigit: Boolean(value.requireDigit),
    passwordRequireLetter: Boolean(value.requireLetter),
    passwordRequireUpper: Boolean(value.requireUpper),
    passwordRequireLower: Boolean(value.requireLower),
    passwordRequireSpecial: Boolean(value.requireSpecial),
    passwordForceChangeOnFirstLogin: Boolean(value.forceChangeOnFirstLogin),
    passwordForceChangeOnReset: Boolean(value.forceChangeOnReset),
  };
}
