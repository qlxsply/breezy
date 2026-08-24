import type { UserFeatureItemEntry } from "@admin/features/user-feature-applications/public/catalog";
import { del, get, post, put, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type {
  UserFeaturePackageApplicationAccessEntry,
  UserFeaturePackageEntry,
} from "../model/types";
import type {
  SaveUserFeaturePackageRequest,
  UserFeaturePackageAccessPayload,
  UserFeaturePackageItemPayload,
  UserFeaturePackagePageRequest,
  UserFeaturePackagePayload,
} from "./payload";

const BASE = "/user-features/packages";
type Options = Pick<RequestOptions, "signal">;

function toFeature(payload: UserFeaturePackageItemPayload): UserFeatureItemEntry {
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

function toAccess(
  payload: UserFeaturePackageAccessPayload,
): UserFeaturePackageApplicationAccessEntry {
  return {
    applicationId: String(payload.applicationId ?? ""),
    applicationCode: payload.applicationCode || "",
    applicationName: payload.applicationName || "",
    featureAccessScope: payload.featureAccessScope || "PARTIAL",
    featureIds: Array.isArray(payload.featureIds) ? payload.featureIds.map(String) : [],
    features: Array.isArray(payload.features) ? payload.features.map(toFeature) : [],
  };
}

function toPackage(payload: UserFeaturePackagePayload): UserFeaturePackageEntry {
  return {
    id: String(payload.id ?? ""),
    code: payload.code || "",
    name: payload.name || "",
    packageType: payload.packageType || "",
    description: payload.description,
    enabled: Boolean(payload.enabled),
    defaultPackage: Boolean(payload.defaultPackage),
    applicationAccesses: Array.isArray(payload.applicationAccesses)
      ? payload.applicationAccesses.map(toAccess)
      : [],
  };
}

export async function pageUserFeaturePackages(
  request: UserFeaturePackagePageRequest,
  options?: Options,
): Promise<PageResult<UserFeaturePackageEntry>> {
  const page = await post<PageResult<UserFeaturePackagePayload>>(`${BASE}/page`, request, options);
  return { ...page, elements: page.elements.map(toPackage) };
}

export async function getUserFeaturePackage(
  id: string,
  options?: Options,
): Promise<UserFeaturePackageEntry> {
  const payload = await get<UserFeaturePackagePayload>(
    `${BASE}/${encodeURIComponent(id)}`,
    options,
  );
  return toPackage(payload);
}

export async function createUserFeaturePackage(
  payload: SaveUserFeaturePackageRequest,
): Promise<UserFeaturePackageEntry> {
  return toPackage(await post<UserFeaturePackagePayload>(BASE, payload));
}

export async function updateUserFeaturePackage(
  id: string,
  payload: SaveUserFeaturePackageRequest,
): Promise<UserFeaturePackageEntry> {
  return toPackage(
    await put<UserFeaturePackagePayload>(`${BASE}/${encodeURIComponent(id)}`, payload),
  );
}

export function updateUserFeaturePackageStatus(id: string, enabled: boolean): Promise<boolean> {
  return put<boolean>(`${BASE}/${encodeURIComponent(id)}/status`, { enabled });
}

export function deleteUserFeaturePackage(id: string): Promise<boolean> {
  return del<boolean>(`${BASE}/${encodeURIComponent(id)}`);
}
