import { del, get, post, put, type RequestOptions } from "@admin/shared/transport";
import type { PageResult, PageRule, SortRule } from "@admin/shared/types/pagination";

import type {
  AssignableRole,
  UserCreateRequest,
  UserEntry,
  UserStatus,
  UserUpdateRequest,
} from "../model/types";
import type {
  AssignableRolePayload,
  UserCreatePayload,
  UserPayload,
  UserUpdatePayload,
} from "./payload";

const BASE = "/users";
const ROLE_BASE = "/roles";

function toUserEntry(payload: UserPayload): UserEntry {
  return {
    id: payload.id,
    username: payload.username || "",
    account: payload.username || "",
    nickname: payload.nickname || "",
    userType: payload.userType || "ADMIN",
    status: payload.status,
    createdBy: payload.createdBy,
    createdAt: payload.createdAt,
    updatedBy: payload.updatedBy,
    updatedAt: payload.updatedAt,
  };
}

export async function pageUsers(
  params: {
    usernameLike?: string;
    status?: UserStatus | "";
    page?: PageRule;
    sort?: SortRule;
  },
  options?: Pick<RequestOptions, "signal">,
): Promise<PageResult<UserEntry>> {
  const payload = {
    usernameLike: params.usernameLike || undefined,
    status: params.status || undefined,
    page: params.page,
    sort: params.sort,
  };
  const page = await post<PageResult<UserPayload>>(`${BASE}/page`, payload, options);
  return {
    ...page,
    elements: page.elements.map(toUserEntry),
  };
}

export async function pageAssignableRoles(
  params: { keyword?: string; page?: PageRule },
  options?: Pick<RequestOptions, "signal">,
): Promise<PageResult<AssignableRole>> {
  const page = await post<PageResult<AssignableRolePayload>>(
    `${ROLE_BASE}/page`,
    {
      keyword: params.keyword || undefined,
      enabled: true,
      page: params.page,
    },
    options,
  );
  return {
    ...page,
    elements: page.elements.map((role) => ({
      id: String(role.id),
      code: role.code,
      name: role.name,
      enabled: Boolean(role.enabled),
    })),
  };
}

export async function createUser(req: UserCreateRequest): Promise<UserEntry> {
  const payload: UserCreatePayload = {
    username: req.username,
    nickname: req.nickname,
    password: req.password,
    roleIds: req.roleIds
      ?.map((roleId) => Number(roleId))
      .filter((roleId) => Number.isFinite(roleId)),
  };
  const row = await post<UserPayload>(BASE, payload);
  return toUserEntry(row);
}

export async function updateUser(id: string, req: UserUpdateRequest): Promise<UserEntry> {
  const payload: UserUpdatePayload = {
    nickname: req.nickname,
    status: req.status,
  };
  const row = await put<UserPayload>(`${BASE}/${encodeURIComponent(id)}`, payload);
  return toUserEntry(row);
}

export function resetUserPassword(id: string): Promise<boolean> {
  return post<boolean>(`${BASE}/${encodeURIComponent(id)}/reset-password`, {});
}

export function batchUpdateUserStatus(userIds: string[], status: UserStatus): Promise<boolean> {
  return put<boolean>(`${BASE}/batch/status`, {
    userIds: userIds.map((id) => Number(id)).filter((id) => Number.isFinite(id)),
    status,
  });
}

export function batchResetUserPassword(userIds: string[]): Promise<boolean> {
  return post<boolean>(`${BASE}/batch/reset-password`, {
    userIds: userIds.map((id) => Number(id)).filter((id) => Number.isFinite(id)),
  });
}

export function deleteUser(id: string): Promise<boolean> {
  return del<boolean>(`${BASE}/${encodeURIComponent(id)}`);
}

export function batchDeleteUsers(userIds: string[]): Promise<boolean> {
  return post<boolean>(`${BASE}/batch/delete`, {
    userIds: userIds.map((id) => Number(id)).filter((id) => Number.isFinite(id)),
  });
}

export async function getUserRoles(
  id: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<string[]> {
  const roleIds = await get<string[]>(`${BASE}/${encodeURIComponent(id)}/roles`, options);
  return roleIds.map((rid) => String(rid));
}

export function updateUserRoles(id: string, roleIds: string[]): Promise<boolean> {
  const payload = roleIds.map((rid) => Number(rid)).filter((rid) => Number.isFinite(rid));
  return put<boolean>(`${BASE}/${encodeURIComponent(id)}/roles`, { roleIds: payload });
}
