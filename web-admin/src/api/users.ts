import { del, get, post, put } from "@admin/api/http";
import type { PageResult, PageRule, SortRule } from "@admin/types/page";
import type {
  UserCreateRequest,
  UserEntry,
  UserStatus,
  UserType,
  UserUpdateRequest,
} from "@admin/types/user-admin";

const BASE = "/users";

interface UserPayload {
  id: string;
  username: string;
  nickname?: string;
  userType?: UserType;
  status: UserEntry["status"];
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

interface UserCreatePayload {
  username: string;
  nickname: string;
  password: string;
}

interface UserUpdatePayload {
  nickname: string;
  status: UserEntry["status"];
}

function toUserEntry(payload: UserPayload): UserEntry {
  return {
    id: payload.id,
    username: payload.username || "",
    account: payload.username || "",
    nickname: payload.nickname || "",
    userType: payload.userType || "INTERNAL",
    status: payload.status,
    createdBy: payload.createdBy,
    createdAt: payload.createdAt,
    updatedBy: payload.updatedBy,
    updatedAt: payload.updatedAt,
  };
}

export async function listUsers(): Promise<UserEntry[]> {
  const rows = await get<UserPayload[]>(BASE);
  return rows.map(toUserEntry);
}

export async function pageUsers(params: {
  usernameLike?: string;
  status?: UserStatus | "";
  page?: PageRule;
  sort?: SortRule;
}): Promise<PageResult<UserEntry>> {
  const payload = {
    usernameLike: params.usernameLike || undefined,
    status: params.status || undefined,
    page: params.page,
    sort: params.sort,
  };
  const page = await post<PageResult<UserPayload>>(`${BASE}/page`, payload);
  return {
    ...page,
    elements: page.elements.map(toUserEntry),
  };
}

export async function createUser(req: UserCreateRequest): Promise<UserEntry> {
  const payload: UserCreatePayload = {
    username: req.username,
    nickname: req.nickname,
    password: req.password,
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

export function deleteUser(id: string): Promise<boolean> {
  return del<boolean>(`${BASE}/${encodeURIComponent(id)}`);
}

export async function getUserRoles(id: string): Promise<string[]> {
  const roleIds = await get<string[]>(`${BASE}/${encodeURIComponent(id)}/roles`);
  return roleIds.map((rid) => String(rid));
}

export function updateUserRoles(id: string, roleIds: string[]): Promise<boolean> {
  const payload = roleIds.map((rid) => Number(rid)).filter((rid) => Number.isFinite(rid));
  return put<boolean>(`${BASE}/${encodeURIComponent(id)}/roles`, { roleIds: payload });
}
