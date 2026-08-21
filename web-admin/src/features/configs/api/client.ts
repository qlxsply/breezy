import { get, post, put, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";
import type { UserConfigItem } from "@admin/shared/types/user-config";

import type { PasswordPolicyConfig } from "../model/password-policy";
import type {
  ConfigActivationPolicy,
  ConfigChangeResult,
  ConfigEditPolicy,
  ConfigEffectiveResult,
  ConfigFieldSpec,
  ConfigFieldType,
  ConfigItem,
  ConfigManagementStatus,
  ConfigValidationResult,
  ConfigValueSource,
  JsonValue,
} from "../model/types";
import type {
  ConfigBatchResetItemPayload,
  ConfigBatchResetRequestPayload,
  ConfigChangePayload,
  ConfigEffectivePayload,
  ConfigFieldSpecPayload,
  ConfigItemPayload,
  ConfigPageRequestPayload,
  ConfigUpdateRequestPayload,
  ConfigValidationPayload,
  ConfigValidationRequestPayload,
  UserConfigUpdateRequestPayload,
} from "./payload";

const BASE = "/sys/configs";

const FIELD_TYPES = [
  "STRING",
  "INTEGER",
  "LONG",
  "DECIMAL",
  "BOOLEAN",
  "ENUM",
  "STRING_LIST",
  "OBJECT",
] as const;
const ACTIVATION_POLICIES = ["DYNAMIC", "RESTART_REQUIRED"] as const;
const EDIT_POLICIES = ["ADMIN_EDITABLE", "READ_ONLY"] as const;
const VALUE_SOURCES = ["CODE_DEFAULT", "DATABASE_OVERRIDE", "INVALID_DATABASE_FALLBACK"] as const;
const MANAGEMENT_STATUSES = [
  "DEFAULT_VALUE",
  "CONFIGURED",
  "INVALID_DATABASE_VALUE",
  "RESTART_REQUIRED",
  "READ_ONLY",
] as const;

export async function listConfigs(
  params?: ConfigPageRequestPayload,
  options?: Pick<RequestOptions, "signal">,
): Promise<PageResult<ConfigItem>> {
  const page = await post<PageResult<ConfigItemPayload>>(
    `${BASE}/page`,
    {
      keyword: params?.keyword?.trim() || undefined,
      module: params?.module?.trim() || undefined,
      group: params?.group?.trim() || undefined,
      page: params?.page,
    },
    options,
  );
  return {
    pageNo: numberOr(page.pageNo, 1),
    pageSize: numberOr(page.pageSize, params?.page?.pageSize ?? 10),
    numberOfElements: numberOr(page.numberOfElements, 0),
    totalPages: numberOr(page.totalPages, 0),
    totalElements: numberOr(page.totalElements, 0),
    elements: arrayOrEmpty(page.elements).map(toConfigItem),
  };
}

export async function getConfig(
  configKey: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<ConfigItem> {
  const payload = await get<ConfigItemPayload>(`${BASE}/${encodeURIComponent(configKey)}`, options);
  return toConfigItem(payload);
}

export async function getEffectiveConfig(configKey: string): Promise<ConfigEffectiveResult> {
  const payload = await get<ConfigEffectivePayload>(
    `${BASE}/effective/${encodeURIComponent(configKey)}`,
  );
  return {
    key: stringOr(payload.key),
    title: stringOr(payload.title),
    description: stringOr(payload.description),
    effectiveValue: jsonOrNull(payload.effectiveValue),
  };
}

export async function validateConfig(
  configKey: string,
  value: JsonValue,
): Promise<ConfigValidationResult> {
  const payload: ConfigValidationRequestPayload = { value };
  const result = await post<ConfigValidationPayload>(
    `${BASE}/${encodeURIComponent(configKey)}/validate`,
    payload,
  );
  return {
    valid: booleanOr(result.valid, false),
    violations: arrayOrEmpty(result.violations).map((violation) => ({
      path: stringOr(violation.path),
      code: stringOr(violation.code),
      message: stringOr(violation.message),
    })),
  };
}

export async function updateConfig(
  configKey: string,
  body: ConfigUpdateRequestPayload,
): Promise<ConfigChangeResult> {
  return toConfigChange(
    await put<ConfigChangePayload>(`${BASE}/${encodeURIComponent(configKey)}`, body),
  );
}

export async function batchResetConfigDefaults(
  items: ConfigBatchResetItemPayload[],
  reason?: string,
): Promise<ConfigChangeResult[]> {
  const payload: ConfigBatchResetRequestPayload = {
    items,
    reason: reason?.trim() || undefined,
  };
  const results = await post<ConfigChangePayload[]>(`${BASE}/batch-reset-default`, payload);
  return arrayOrEmpty(results).map(toConfigChange);
}

export function getMyConfigs(): Promise<UserConfigItem[]> {
  return get<UserConfigItem[]>(`${BASE}/my`);
}

export function updateMyConfig(code: string, value: string): Promise<boolean> {
  const payload: UserConfigUpdateRequestPayload = { value };
  return put<boolean>(`${BASE}/my/${encodeURIComponent(code)}`, payload);
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

function toConfigItem(payload: ConfigItemPayload): ConfigItem {
  return {
    key: stringOr(payload.key),
    module: stringOr(payload.module),
    group: stringOr(payload.group),
    title: stringOr(payload.title),
    description: stringOr(payload.description),
    schemaVersion: numberOr(payload.schemaVersion, 0),
    persistedRevision: numberOr(payload.persistedRevision, 0),
    effectiveRevision: numberOr(payload.effectiveRevision, 0),
    configured: booleanOr(payload.configured, false),
    activationPolicy: enumOr<ConfigActivationPolicy>(
      payload.activationPolicy,
      ACTIVATION_POLICIES,
      "DYNAMIC",
    ),
    editPolicy: enumOr<ConfigEditPolicy>(payload.editPolicy, EDIT_POLICIES, "READ_ONLY"),
    source: enumOr<ConfigValueSource>(payload.source, VALUE_SOURCES, "CODE_DEFAULT"),
    status: enumOr<ConfigManagementStatus>(payload.status, MANAGEMENT_STATUSES, "DEFAULT_VALUE"),
    pendingRestart: booleanOr(payload.pendingRestart, false),
    effectiveValue: jsonOrNull(payload.effectiveValue),
    persistedValue: jsonOrNull(payload.persistedValue),
    defaultValue: jsonOrNull(payload.defaultValue),
    fields: arrayOrEmpty(payload.fields).map(toConfigFieldSpec),
    sensitiveValuePresence: Object.fromEntries(
      Object.entries(payload.sensitiveValuePresence ?? {}).map(([path, present]) => [
        path,
        booleanOr(present, false),
      ]),
    ),
    editorId: stringOr(payload.editorId),
    loadWarning: payload.loadWarning == null ? null : String(payload.loadWarning),
  };
}

function toConfigFieldSpec(payload: ConfigFieldSpecPayload): ConfigFieldSpec {
  return {
    path: stringOr(payload.path),
    title: stringOr(payload.title),
    description: stringOr(payload.description),
    type: enumOr<ConfigFieldType>(payload.type, FIELD_TYPES, "STRING"),
    required: booleanOr(payload.required, false),
    sensitive: booleanOr(payload.sensitive, false),
    readOnly: booleanOr(payload.readOnly, false),
    order: numberOr(payload.order, 0),
    placeholder: stringOr(payload.placeholder),
    min: nullableNumber(payload.min),
    max: nullableNumber(payload.max),
    minLength: nullableNumber(payload.minLength),
    maxLength: nullableNumber(payload.maxLength),
    options: arrayOrEmpty(payload.options).map((option) => ({
      value: stringOr(option.value),
      label: stringOr(option.label),
    })),
  };
}

function toConfigChange(payload: ConfigChangePayload): ConfigChangeResult {
  return {
    key: stringOr(payload.key),
    persistedRevision: numberOr(payload.persistedRevision, 0),
    pendingRestart: booleanOr(payload.pendingRestart, false),
  };
}

function arrayOrEmpty<T>(value: T[] | null | undefined): T[] {
  return Array.isArray(value) ? value : [];
}

function stringOr(value: string | null | undefined, fallback = ""): string {
  return value == null ? fallback : String(value);
}

function numberOr(value: number | string | null | undefined, fallback: number): number {
  if (value == null || value === "") return fallback;
  const normalized = Number(value);
  return Number.isFinite(normalized) ? normalized : fallback;
}

function nullableNumber(value: number | string | null | undefined): number | null {
  if (value == null || value === "") return null;
  const normalized = Number(value);
  return Number.isFinite(normalized) ? normalized : null;
}

function booleanOr(
  value: boolean | string | number | null | undefined,
  fallback: boolean,
): boolean {
  if (typeof value === "boolean") return value;
  if (typeof value === "number") return value !== 0;
  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    if (normalized === "true") return true;
    if (normalized === "false") return false;
  }
  return fallback;
}

function enumOr<T extends string>(
  value: string | null | undefined,
  values: readonly T[],
  fallback: T,
): T {
  return typeof value === "string" && values.includes(value as T) ? (value as T) : fallback;
}

function jsonOrNull(value: ConfigEffectivePayload["effectiveValue"]): JsonValue {
  return value ?? null;
}
