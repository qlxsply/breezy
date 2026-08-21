import type { PageRule } from "@admin/shared/types/pagination";

export type JsonPayload =
  string | number | boolean | null | JsonPayload[] | { [key: string]: JsonPayload };

export interface ConfigOptionItemPayload {
  value?: string | null;
  label?: string | null;
}

export interface ConfigFieldSpecPayload {
  path?: string | null;
  title?: string | null;
  description?: string | null;
  type?: string | null;
  required?: boolean | string | number | null;
  sensitive?: boolean | string | number | null;
  readOnly?: boolean | string | number | null;
  order?: number | string | null;
  placeholder?: string | null;
  min?: number | string | null;
  max?: number | string | null;
  minLength?: number | string | null;
  maxLength?: number | string | null;
  options?: ConfigOptionItemPayload[] | null;
}

export interface ConfigItemPayload {
  key?: string | null;
  module?: string | null;
  group?: string | null;
  title?: string | null;
  description?: string | null;
  schemaVersion?: number | string | null;
  persistedRevision?: number | string | null;
  effectiveRevision?: number | string | null;
  configured?: boolean | string | number | null;
  activationPolicy?: string | null;
  editPolicy?: string | null;
  source?: string | null;
  status?: string | null;
  pendingRestart?: boolean | string | number | null;
  effectiveValue?: JsonPayload;
  persistedValue?: JsonPayload;
  defaultValue?: JsonPayload;
  fields?: ConfigFieldSpecPayload[] | null;
  sensitiveValuePresence?: Record<string, boolean | string | number | null> | null;
  editorId?: string | null;
  loadWarning?: string | null;
}

export interface ConfigPageRequestPayload {
  keyword?: string;
  module?: string;
  group?: string;
  page?: PageRule;
}

export interface ConfigValidationRequestPayload {
  value: JsonPayload;
}

export interface ConfigViolationPayload {
  path?: string | null;
  code?: string | null;
  message?: string | null;
}

export interface ConfigValidationPayload {
  valid?: boolean | string | number | null;
  violations?: ConfigViolationPayload[] | null;
}

export interface ConfigUpdateRequestPayload {
  expectedRevision: number;
  reason?: string;
  value: JsonPayload;
}

export interface ConfigChangePayload {
  key?: string | null;
  persistedRevision?: number | string | null;
  pendingRestart?: boolean | string | number | null;
}

export interface ConfigBatchResetItemPayload {
  configKey: string;
  expectedRevision: number;
}

export interface ConfigBatchResetRequestPayload {
  items: ConfigBatchResetItemPayload[];
  reason?: string;
}

export interface ConfigEffectivePayload {
  key?: string | null;
  title?: string | null;
  description?: string | null;
  effectiveValue?: JsonPayload;
}

export interface UserConfigUpdateRequestPayload {
  value: string;
}
