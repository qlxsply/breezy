import { del, get, post, put, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type {
  MethodStatGlobalSwitch,
  MethodStatMethodSwitch,
  MethodStatStatsItem,
} from "../model/types";
import type { MethodStatStatsPageRequest } from "./payload";

const QUERY_BASE = "/method-stat/query";
const MANAGE_BASE = "/method-stat/manage";

export function pageMethodStatStats(
  req: MethodStatStatsPageRequest,
  options?: Pick<RequestOptions, "signal">,
): Promise<PageResult<MethodStatStatsItem>> {
  return post<PageResult<MethodStatStatsItem>>(`${QUERY_BASE}/stats/page`, req, options);
}

export function getMethodStatStatsDetail(
  key: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<MethodStatStatsItem> {
  const params = new URLSearchParams({ key });
  return get<MethodStatStatsItem>(`${QUERY_BASE}/stats/detail?${params.toString()}`, options);
}

export function getMethodStatGlobalSwitch(
  options?: Pick<RequestOptions, "signal">,
): Promise<MethodStatGlobalSwitch> {
  return get<MethodStatGlobalSwitch>(`${MANAGE_BASE}/global-switch`, options);
}

export function updateMethodStatGlobalSwitch(enabled: boolean): Promise<MethodStatGlobalSwitch> {
  return put<MethodStatGlobalSwitch>(`${MANAGE_BASE}/global-switch`, { enabled });
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
