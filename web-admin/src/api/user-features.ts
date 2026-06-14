import type { PageResult, PageRule, SortRule } from "../types/page";
import type {
  SaveUserFeaturePackageRequest,
  SaveUserFeatureUserManagementRequest,
  UserFeatureApplicationEntry,
  UserFeatureItemEntry,
  UserFeaturePackageApplicationAccessEntry,
  UserFeaturePackageEntry,
  UserFeatureUserApplicationEntry,
  UserFeatureUserFeatureEntry,
  UserFeatureUserManagementEntry,
} from "../types/user-feature";
import { del, get, post, put } from "./http";

const BASE = "/user-features";

function toFeatureItemEntry(payload: UserFeatureItemEntry): UserFeatureItemEntry {
  return {
    id: String(payload.id),
    applicationId: String(payload.applicationId),
    code: payload.code,
    name: payload.name,
    description: payload.description,
    enabled: Boolean(payload.enabled),
    permissionCodes: Array.isArray(payload.permissionCodes) ? payload.permissionCodes : [],
  };
}

function toApplicationEntry(payload: UserFeatureApplicationEntry): UserFeatureApplicationEntry {
  return {
    id: String(payload.id),
    code: payload.code,
    name: payload.name,
    description: payload.description,
    icon: payload.icon,
    routePath: payload.routePath,
    componentPath: payload.componentPath,
    enabled: Boolean(payload.enabled),
    featureCount: Number(payload.featureCount || 0),
    permissionBindingCount: Number(payload.permissionBindingCount || 0),
    features: Array.isArray(payload.features) ? payload.features.map(toFeatureItemEntry) : [],
  };
}

function toPackageApplicationAccessEntry(
  payload: UserFeaturePackageApplicationAccessEntry,
): UserFeaturePackageApplicationAccessEntry {
  return {
    applicationId: String(payload.applicationId),
    applicationCode: payload.applicationCode,
    applicationName: payload.applicationName,
    featureAccessScope: payload.featureAccessScope,
    featureIds: Array.isArray(payload.featureIds) ? payload.featureIds.map(String) : [],
    features: Array.isArray(payload.features) ? payload.features.map(toFeatureItemEntry) : [],
  };
}

function toPackageEntry(payload: UserFeaturePackageEntry): UserFeaturePackageEntry {
  return {
    id: String(payload.id),
    code: payload.code,
    name: payload.name,
    packageType: payload.packageType,
    description: payload.description,
    enabled: Boolean(payload.enabled),
    defaultPackage: Boolean(payload.defaultPackage),
    applicationAccesses: Array.isArray(payload.applicationAccesses)
      ? payload.applicationAccesses.map(toPackageApplicationAccessEntry)
      : [],
  };
}

function toUserFeatureEntry(payload: UserFeatureUserFeatureEntry): UserFeatureUserFeatureEntry {
  return {
    id: String(payload.id),
    applicationId: String(payload.applicationId),
    applicationCode: payload.applicationCode,
    code: payload.code,
    name: payload.name,
    description: payload.description,
    enabled: Boolean(payload.enabled),
    permissionCodes: Array.isArray(payload.permissionCodes) ? payload.permissionCodes : [],
    inheritedEnabled: Boolean(payload.inheritedEnabled),
    effectiveEnabled: Boolean(payload.effectiveEnabled),
    overrideType: payload.overrideType || "NONE",
  };
}

function toUserApplicationEntry(
  payload: UserFeatureUserApplicationEntry,
): UserFeatureUserApplicationEntry {
  return {
    id: String(payload.id),
    code: payload.code,
    name: payload.name,
    description: payload.description,
    icon: payload.icon,
    routePath: payload.routePath,
    componentPath: payload.componentPath,
    enabled: Boolean(payload.enabled),
    inheritedVisible: Boolean(payload.inheritedVisible),
    effectiveVisible: Boolean(payload.effectiveVisible),
    packageAccessScope: payload.packageAccessScope || "NONE",
    overrideType: payload.overrideType || "NONE",
    overrideAccessScope: payload.overrideAccessScope,
    features: Array.isArray(payload.features) ? payload.features.map(toUserFeatureEntry) : [],
  };
}

function toUserManagementEntry(payload: UserFeatureUserManagementEntry): UserFeatureUserManagementEntry {
  return {
    userId: String(payload.userId),
    account: payload.account || "",
    packageIds: Array.isArray(payload.packageIds) ? payload.packageIds.map(String) : [],
    packages: Array.isArray(payload.packages)
      ? payload.packages.map((item) => ({
          ...item,
          id: String(item.id),
          enabled: Boolean(item.enabled),
          defaultPackage: Boolean(item.defaultPackage),
        }))
      : [],
    applications: Array.isArray(payload.applications)
      ? payload.applications.map(toUserApplicationEntry)
      : [],
  };
}

export function pageUserFeatureApplications(params: {
  keyword?: string;
  enabled?: boolean | "";
  page?: PageRule;
  sort?: SortRule;
}): Promise<PageResult<UserFeatureApplicationEntry>> {
  return post<PageResult<UserFeatureApplicationEntry>>(`${BASE}/applications/page`, {
    keyword: params.keyword || undefined,
    enabled: params.enabled === "" ? undefined : params.enabled,
    page: params.page,
    sort: params.sort,
  }).then((page) => ({
    ...page,
    elements: page.elements.map(toApplicationEntry),
  }));
}

export function listUserFeatureApplications(): Promise<UserFeatureApplicationEntry[]> {
  return get<UserFeatureApplicationEntry[]>(`${BASE}/applications/catalog`).then((rows) =>
    rows.map(toApplicationEntry),
  );
}

export function getUserFeatureApplication(id: string): Promise<UserFeatureApplicationEntry> {
  return get<UserFeatureApplicationEntry>(`${BASE}/applications/${encodeURIComponent(id)}`).then(
    toApplicationEntry,
  );
}

export function updateUserFeatureApplicationStatus(id: string, enabled: boolean): Promise<boolean> {
  return put<boolean>(`${BASE}/applications/${encodeURIComponent(id)}/status`, { enabled });
}

export function pageUserFeaturePackages(params: {
  keyword?: string;
  enabled?: boolean | "";
  page?: PageRule;
  sort?: SortRule;
}): Promise<PageResult<UserFeaturePackageEntry>> {
  return post<PageResult<UserFeaturePackageEntry>>(`${BASE}/packages/page`, {
    keyword: params.keyword || undefined,
    enabled: params.enabled === "" ? undefined : params.enabled,
    page: params.page,
    sort: params.sort,
  }).then((page) => ({
    ...page,
    elements: page.elements.map(toPackageEntry),
  }));
}

export function getUserFeaturePackage(id: string): Promise<UserFeaturePackageEntry> {
  return get<UserFeaturePackageEntry>(`${BASE}/packages/${encodeURIComponent(id)}`).then(toPackageEntry);
}

export function createUserFeaturePackage(
  payload: SaveUserFeaturePackageRequest,
): Promise<UserFeaturePackageEntry> {
  return post<UserFeaturePackageEntry>(`${BASE}/packages`, payload).then(toPackageEntry);
}

export function updateUserFeaturePackage(
  id: string,
  payload: SaveUserFeaturePackageRequest,
): Promise<UserFeaturePackageEntry> {
  return put<UserFeaturePackageEntry>(`${BASE}/packages/${encodeURIComponent(id)}`, payload).then(
    toPackageEntry,
  );
}

export function updateUserFeaturePackageStatus(id: string, enabled: boolean): Promise<boolean> {
  return put<boolean>(`${BASE}/packages/${encodeURIComponent(id)}/status`, { enabled });
}

export function deleteUserFeaturePackage(id: string): Promise<boolean> {
  return del<boolean>(`${BASE}/packages/${encodeURIComponent(id)}`);
}

export function getUserFeatureUserManagement(userId: string): Promise<UserFeatureUserManagementEntry> {
  return get<UserFeatureUserManagementEntry>(`${BASE}/users/${encodeURIComponent(userId)}/management`).then(
    toUserManagementEntry,
  );
}

export function saveUserFeatureUserManagement(
  userId: string,
  payload: SaveUserFeatureUserManagementRequest,
): Promise<boolean> {
  return put<boolean>(`${BASE}/users/${encodeURIComponent(userId)}/management`, payload);
}
