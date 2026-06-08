import type {
  NormalFeatureEntry,
  NormalFeatureSelection,
  NormalFeatureUserFeatureEntry,
  NormalFeatureUserManagementEntry,
  SaveNormalFeatureUserManagementRequest,
} from "../types/normal-feature";
import type {
  NormalFeatureGroupEntry,
  NormalFeatureGroupFeatureEntry,
  SaveNormalFeatureGroupRequest,
} from "../types/normal-feature-group";
import type { PageResult, PageRule, SortRule } from "../types/page";
import { del, get, post, put } from "./http";

const BASE = "/normal-features";

interface NormalFeaturePayload extends NormalFeatureEntry {}

interface NormalFeatureUserFeaturePayload extends NormalFeatureEntry {
  groupEnabled: boolean;
  effectiveEnabled: boolean;
  overrideType: NormalFeatureUserFeatureEntry["overrideType"];
}

interface NormalFeatureUserManagementPayload {
  userId: string;
  account: string;
  groupIds: string[];
  features: NormalFeatureUserFeaturePayload[];
}

function toFeatureEntry(payload: NormalFeaturePayload): NormalFeatureEntry {
  return {
    id: String(payload.id),
    code: payload.code,
    name: payload.name,
    description: payload.description,
    enabled: Boolean(payload.enabled),
    permissionCodes: Array.isArray(payload.permissionCodes) ? payload.permissionCodes : [],
  };
}

function toUserFeatureEntry(
  payload: NormalFeatureUserFeaturePayload,
): NormalFeatureUserFeatureEntry {
  return {
    ...toFeatureEntry(payload),
    groupEnabled: Boolean(payload.groupEnabled),
    effectiveEnabled: Boolean(payload.effectiveEnabled),
    overrideType: payload.overrideType || "NONE",
  };
}

function toUserManagementEntry(
  payload: NormalFeatureUserManagementPayload,
): NormalFeatureUserManagementEntry {
  return {
    userId: String(payload.userId),
    account: payload.account || "",
    groupIds: Array.isArray(payload.groupIds) ? payload.groupIds.map(String) : [],
    features: Array.isArray(payload.features) ? payload.features.map(toUserFeatureEntry) : [],
  };
}

function toGroupFeatureEntry(
  payload: NormalFeatureGroupFeatureEntry,
): NormalFeatureGroupFeatureEntry {
  return {
    id: String(payload.id),
    code: payload.code,
    name: payload.name,
    description: payload.description,
    enabled: Boolean(payload.enabled),
  };
}

function toGroupEntry(payload: NormalFeatureGroupEntry): NormalFeatureGroupEntry {
  return {
    ...payload,
    id: String(payload.id),
    featureIds: Array.isArray(payload.featureIds) ? payload.featureIds.map(String) : [],
    features: Array.isArray(payload.features) ? payload.features.map(toGroupFeatureEntry) : [],
  };
}

export function pageNormalFeatures(params: {
  keyword?: string;
  enabled?: boolean | "";
  page?: PageRule;
  sort?: SortRule;
}): Promise<PageResult<NormalFeatureEntry>> {
  return post<PageResult<NormalFeaturePayload>>(`${BASE}/page`, {
    keyword: params.keyword || undefined,
    enabled: params.enabled === "" ? undefined : params.enabled,
    page: params.page,
    sort: params.sort,
  }).then((page) => ({
    ...page,
    elements: page.elements.map(toFeatureEntry),
  }));
}

export function listNormalFeatures(): Promise<NormalFeatureEntry[]> {
  return get<NormalFeaturePayload[]>(`${BASE}/features`).then((rows) => rows.map(toFeatureEntry));
}

export function getNormalFeature(id: string): Promise<NormalFeatureEntry> {
  return get<NormalFeaturePayload>(`${BASE}/features/${encodeURIComponent(id)}`).then(
    toFeatureEntry,
  );
}

export function updateNormalFeatureStatus(id: string, enabled: boolean): Promise<boolean> {
  return put<boolean>(`${BASE}/features/${encodeURIComponent(id)}/status`, { enabled });
}

export function getDefaultNormalFeatures(): Promise<NormalFeatureSelection> {
  return get<NormalFeatureSelection>(`${BASE}/default`);
}

export function updateDefaultNormalFeatures(featureIds: string[]): Promise<boolean> {
  return put<boolean>(`${BASE}/default`, { featureIds });
}

export function getNormalUserFeatures(userId: string): Promise<NormalFeatureSelection> {
  return get<NormalFeatureSelection>(`${BASE}/users/${encodeURIComponent(userId)}`);
}

export function updateNormalUserFeatures(userId: string, featureIds: string[]): Promise<boolean> {
  return put<boolean>(`${BASE}/users/${encodeURIComponent(userId)}`, { featureIds });
}

export function pageNormalFeatureGroups(params: {
  keyword?: string;
  enabled?: boolean | "";
  page?: PageRule;
  sort?: SortRule;
}): Promise<PageResult<NormalFeatureGroupEntry>> {
  return post<PageResult<NormalFeatureGroupEntry>>(`${BASE}/groups/page`, {
    keyword: params.keyword || undefined,
    enabled: params.enabled === "" ? undefined : params.enabled,
    page: params.page,
    sort: params.sort,
  }).then((page) => ({
    ...page,
    elements: page.elements.map(toGroupEntry),
  }));
}

export function getNormalFeatureGroup(id: string): Promise<NormalFeatureGroupEntry> {
  return get<NormalFeatureGroupEntry>(`${BASE}/groups/${encodeURIComponent(id)}`).then(
    toGroupEntry,
  );
}

export function createNormalFeatureGroup(
  payload: SaveNormalFeatureGroupRequest,
): Promise<NormalFeatureGroupEntry> {
  return post<NormalFeatureGroupEntry>(`${BASE}/groups`, payload).then(toGroupEntry);
}

export function updateNormalFeatureGroup(
  id: string,
  payload: SaveNormalFeatureGroupRequest,
): Promise<NormalFeatureGroupEntry> {
  return put<NormalFeatureGroupEntry>(`${BASE}/groups/${encodeURIComponent(id)}`, payload).then(
    toGroupEntry,
  );
}

export function updateNormalFeatureGroupStatus(id: string, enabled: boolean): Promise<boolean> {
  return put<boolean>(`${BASE}/groups/${encodeURIComponent(id)}/status`, { enabled });
}

export function deleteNormalFeatureGroup(id: string): Promise<boolean> {
  return del<boolean>(`${BASE}/groups/${encodeURIComponent(id)}`);
}

export function getNormalUserManagement(userId: string): Promise<NormalFeatureUserManagementEntry> {
  return get<NormalFeatureUserManagementPayload>(
    `${BASE}/users/${encodeURIComponent(userId)}/management`,
  ).then(toUserManagementEntry);
}

export function saveNormalUserManagement(
  userId: string,
  payload: SaveNormalFeatureUserManagementRequest,
): Promise<boolean> {
  return put<boolean>(`${BASE}/users/${encodeURIComponent(userId)}/management`, payload);
}
