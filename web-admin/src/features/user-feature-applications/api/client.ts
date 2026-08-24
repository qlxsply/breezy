import { get, post, put, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type { UserFeatureApplicationEntry, UserFeatureItemEntry } from "../model/types";
import type {
  UserFeatureApplicationPageRequest,
  UserFeatureApplicationPayload,
  UserFeatureItemPayload,
} from "./payload";

const BASE = "/user-features/applications";
type Options = Pick<RequestOptions, "signal">;

function toFeature(payload: UserFeatureItemPayload): UserFeatureItemEntry {
  return {
    id: String(payload.id ?? ""),
    applicationId: String(payload.applicationId ?? ""),
    code: payload.code || "",
    name: payload.name || "",
    description: payload.description,
    enabled: Boolean(payload.enabled),
    permissionCodes: Array.isArray(payload.permissionCodes) ? payload.permissionCodes : [],
  };
}

export function toApplication(payload: UserFeatureApplicationPayload): UserFeatureApplicationEntry {
  return {
    id: String(payload.id ?? ""),
    code: payload.code || "",
    name: payload.name || "",
    description: payload.description,
    icon: payload.icon,
    routePath: payload.routePath,
    componentPath: payload.componentPath,
    enabled: Boolean(payload.enabled),
    featureCount: Number(payload.featureCount || 0),
    permissionBindingCount: Number(payload.permissionBindingCount || 0),
    features: Array.isArray(payload.features) ? payload.features.map(toFeature) : [],
  };
}

export async function pageUserFeatureApplications(
  request: UserFeatureApplicationPageRequest,
  options?: Options,
): Promise<PageResult<UserFeatureApplicationEntry>> {
  const page = await post<PageResult<UserFeatureApplicationPayload>>(
    `${BASE}/page`,
    request,
    options,
  );
  return { ...page, elements: page.elements.map(toApplication) };
}

export async function listUserFeatureApplications(
  options?: Options,
): Promise<UserFeatureApplicationEntry[]> {
  const rows = await get<UserFeatureApplicationPayload[]>(`${BASE}/catalog`, options);
  return rows.map(toApplication);
}

export async function getUserFeatureApplication(
  id: string,
  options?: Options,
): Promise<UserFeatureApplicationEntry> {
  const payload = await get<UserFeatureApplicationPayload>(
    `${BASE}/${encodeURIComponent(id)}`,
    options,
  );
  return toApplication(payload);
}

export function updateUserFeatureApplicationStatus(
  id: string,
  enabled: boolean,
  options?: Options,
): Promise<boolean> {
  return put<boolean>(`${BASE}/${encodeURIComponent(id)}/status`, { enabled }, options);
}
