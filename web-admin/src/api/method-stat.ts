import { del, get, post, put } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type {
  MethodStatGlobalSwitch,
  MethodStatMethodSwitch,
  MethodStatStatsItem,
  MethodStatStatsPageRequest,
} from "../types/method-stat";

const QUERY_BASE = "/method-stat/query";
const MANAGE_BASE = "/method-stat/manage";

export function pageMethodStatStats(
  req: MethodStatStatsPageRequest,
): Promise<PageResult<MethodStatStatsItem>> {
  return post<PageResult<MethodStatStatsItem>>(`${QUERY_BASE}/stats/page`, req);
}

export function getMethodStatStatsDetail(key: string): Promise<MethodStatStatsItem> {
  const params = new URLSearchParams({ key });
  return get<MethodStatStatsItem>(`${QUERY_BASE}/stats/detail?${params.toString()}`);
}

export function getMethodStatGlobalSwitch(): Promise<MethodStatGlobalSwitch> {
  return get<MethodStatGlobalSwitch>(`${MANAGE_BASE}/global-switch`);
}

export function updateMethodStatGlobalSwitch(enabled: boolean): Promise<MethodStatGlobalSwitch> {
  return put<MethodStatGlobalSwitch>(`${MANAGE_BASE}/global-switch`, { enabled });
}

export function getMethodStatMethodSwitch(key: string): Promise<MethodStatMethodSwitch> {
  const params = new URLSearchParams({ key });
  return get<MethodStatMethodSwitch>(`${MANAGE_BASE}/method-switch?${params.toString()}`);
}

export function updateMethodStatMethodSwitch(
  key: string,
  enabled: boolean,
): Promise<MethodStatMethodSwitch> {
  return put<MethodStatMethodSwitch>(`${MANAGE_BASE}/method-switch`, { key, enabled });
}

export function updateAllMethodStatMethodSwitch(enabled: boolean): Promise<void> {
  return put<void>(`${MANAGE_BASE}/method-switch/all`, { enabled });
}

export function clearMethodStat(key: string): Promise<void> {
  const params = new URLSearchParams({ key });
  return del<void>(`${MANAGE_BASE}/stats?${params.toString()}`);
}

export function clearAllMethodStat(): Promise<void> {
  return del<void>(`${MANAGE_BASE}/stats/all`);
}
