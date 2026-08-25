import { get, post, put, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type {
  WebUserEntry,
  UserFeaturePackageOptionEntry,
  UserFeatureUserApplicationEntry,
  UserFeatureUserFeatureEntry,
  UserFeatureUserManagementEntry,
} from "../model/types";
import type {
  WebUserPageRequest,
  WebUserPayload,
  SaveUserFeatureUserManagementRequest,
  UserFeaturePackageOptionPayload,
  UserFeatureUserApplicationPayload,
  UserFeatureUserFeaturePayload,
  UserFeatureUserManagementPayload,
} from "./payload";

const USER_BASE = "/web-users";
const FEATURE_BASE = "/user-features/users";
type Options = Pick<RequestOptions, "signal">;

function toWebUser(payload: WebUserPayload): WebUserEntry {
  const account = payload.account || "";
  return {
    id: String(payload.id ?? ""),
    account,
    username: account,
    nickname: payload.nickname || "",
    userType: "USER",
    status: payload.status || "DISABLED",
    lastLoginAt: payload.lastLoginAt,
    createdAt: payload.createdAt,
    updatedAt: payload.updatedAt,
  };
}

function toPackageOption(payload: UserFeaturePackageOptionPayload): UserFeaturePackageOptionEntry {
  return {
    id: String(payload.id ?? ""),
    code: payload.code || "",
    name: payload.name || "",
    packageType: payload.packageType || "",
    description: payload.description,
    enabled: Boolean(payload.enabled),
    defaultPackage: Boolean(payload.defaultPackage),
  };
}

function toUserFeature(payload: UserFeatureUserFeaturePayload): UserFeatureUserFeatureEntry {
  return {
    id: String(payload.id ?? ""),
    applicationId: String(payload.applicationId ?? ""),
    applicationCode: payload.applicationCode || "",
    code: payload.code || "",
    name: payload.name || "",
    description: payload.description,
    enabled: Boolean(payload.enabled),
    permissionCodes: Array.isArray(payload.permissionCodes) ? payload.permissionCodes : [],
    inheritedEnabled: Boolean(payload.inheritedEnabled),
    effectiveEnabled: Boolean(payload.effectiveEnabled),
    overrideType: payload.overrideType || "NONE",
  };
}

function toUserApplication(
  payload: UserFeatureUserApplicationPayload,
): UserFeatureUserApplicationEntry {
  return {
    id: String(payload.id ?? ""),
    code: payload.code || "",
    name: payload.name || "",
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
    features: Array.isArray(payload.features) ? payload.features.map(toUserFeature) : [],
  };
}

export async function pageWebUsers(
  request: WebUserPageRequest,
  options?: Options,
): Promise<PageResult<WebUserEntry>> {
  const page = await post<PageResult<WebUserPayload>>(`${USER_BASE}/page`, request, options);
  return { ...page, elements: page.elements.map(toWebUser) };
}

export async function getWebUser(id: string, options?: Options): Promise<WebUserEntry> {
  return toWebUser(
    await get<WebUserPayload>(`${USER_BASE}/${encodeURIComponent(id)}`, options),
  );
}

export async function updateWebUser(
  id: string,
  status: WebUserEntry["status"],
): Promise<WebUserEntry> {
  return toWebUser(
    await put<WebUserPayload>(`${USER_BASE}/${encodeURIComponent(id)}`, { status }),
  );
}

export async function getUserFeatureUserManagement(
  userId: string,
  options?: Options,
): Promise<UserFeatureUserManagementEntry> {
  const payload = await get<UserFeatureUserManagementPayload>(
    `${FEATURE_BASE}/${encodeURIComponent(userId)}/management`,
    options,
  );
  return {
    userId: String(payload.userId ?? ""),
    account: payload.account || "",
    packageIds: Array.isArray(payload.packageIds) ? payload.packageIds.map(String) : [],
    packages: Array.isArray(payload.packages) ? payload.packages.map(toPackageOption) : [],
    applications: Array.isArray(payload.applications)
      ? payload.applications.map(toUserApplication)
      : [],
  };
}

export function saveUserFeatureUserManagement(
  userId: string,
  payload: SaveUserFeatureUserManagementRequest,
): Promise<boolean> {
  return put<boolean>(`${FEATURE_BASE}/${encodeURIComponent(userId)}/management`, payload);
}
