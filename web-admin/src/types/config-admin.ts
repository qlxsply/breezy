export type JsonPrimitive = string | number | boolean | null;
export type JsonValue = JsonPrimitive | JsonObject | JsonValue[];
export interface JsonObject {
  [key: string]: JsonValue;
}

export type ConfigFieldType =
  | "STRING"
  | "INTEGER"
  | "LONG"
  | "DECIMAL"
  | "BOOLEAN"
  | "ENUM"
  | "STRING_LIST"
  | "OBJECT";

export type ConfigActivationPolicy = "DYNAMIC" | "RESTART_REQUIRED";
export type ConfigEditPolicy = "ADMIN_EDITABLE" | "READ_ONLY";
export type ConfigValueSource =
  | "CODE_DEFAULT"
  | "DATABASE_OVERRIDE"
  | "INVALID_DATABASE_FALLBACK";
export type ConfigManagementStatus =
  | "DEFAULT_VALUE"
  | "CONFIGURED"
  | "INVALID_DATABASE_VALUE"
  | "RESTART_REQUIRED"
  | "READ_ONLY";

export interface ConfigOptionItem {
  value: string;
  label: string;
}

export interface ConfigFieldSpec {
  path: string;
  title: string;
  description: string;
  type: ConfigFieldType;
  required: boolean;
  sensitive: boolean;
  readOnly: boolean;
  order: number;
  placeholder: string;
  min: number | null;
  max: number | null;
  minLength: number | null;
  maxLength: number | null;
  options: ConfigOptionItem[];
}

export interface ConfigItem {
  key: string;
  module: string;
  group: string;
  title: string;
  description: string;
  schemaVersion: number;
  persistedRevision: number;
  effectiveRevision: number;
  configured: boolean;
  activationPolicy: ConfigActivationPolicy;
  editPolicy: ConfigEditPolicy;
  source: ConfigValueSource;
  status: ConfigManagementStatus;
  pendingRestart: boolean;
  effectiveValue: JsonValue;
  persistedValue: JsonValue;
  defaultValue: JsonValue;
  fields: ConfigFieldSpec[];
  sensitiveValuePresence: Record<string, boolean>;
  editorId: string;
  loadWarning: string | null;
}

export interface ConfigViolation {
  path: string;
  code: string;
  message: string;
}

export interface ConfigValidationResult {
  valid: boolean;
  violations: ConfigViolation[];
}

export interface ConfigChangeResult {
  key: string;
  persistedRevision: number;
  pendingRestart: boolean;
}

export interface ConfigEffectiveResult {
  key: string;
  title: string;
  description: string;
  effectiveValue: JsonValue;
}

export type ConfigType = "STR" | "INT" | "LONG" | "BOOL" | "DEC" | "STR_LIST" | "STR_SET";
