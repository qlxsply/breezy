import type { PageQuery } from "@admin/shared/types/pagination";

export type MethodStatSortDirection = "ASC" | "DESC";

export type MethodStatSortBy =
  | "TOTAL_CALLS"
  | "TOTAL_SUCCESS"
  | "TOTAL_FAILURE"
  | "RECENT_1M_CALLS"
  | "RECENT_1H_CALLS"
  | "RECENT_1D_CALLS"
  | "RECENT_1M_SUCCESS"
  | "RECENT_1M_FAILURE"
  | "RECENT_1H_SUCCESS"
  | "RECENT_1H_FAILURE"
  | "RECENT_1D_SUCCESS"
  | "RECENT_1D_FAILURE"
  | "DURATION_MAX"
  | "DURATION_MIN"
  | "DURATION_AVG"
  | "DURATION_P50"
  | "DURATION_P90"
  | "DURATION_P95"
  | "DURATION_P99"
  | "METHOD_NAME"
  | "KEY";

export interface MethodStatStatsItem {
  key: string;
  packageName: string;
  className: string;
  methodName: string;
  methodSignature: string;
  methodSwitchEnabled: boolean;
  globalSwitchEnabled: boolean;
  collectEnabled: boolean;
  totalCalls: number;
  totalSuccess: number;
  totalFailure: number;
  recent1MinuteCalls: number;
  recent1HourCalls: number;
  recent1DayCalls: number;
  recent1MinuteSuccess: number;
  recent1MinuteFailure: number;
  recent1HourSuccess: number;
  recent1HourFailure: number;
  recent1DaySuccess: number;
  recent1DayFailure: number;
  durationSampleSize: number;
  durationMax: number;
  durationMin: number;
  durationAvg: number;
  durationP50: number;
  durationP90: number;
  durationP95: number;
  durationP99: number;
}

export interface MethodStatGlobalSwitch {
  enabled: boolean;
}

export interface MethodStatMethodSwitch {
  key: string;
  enabled: boolean;
}

export interface MethodStatStatsPageRequest extends PageQuery {
  methodName?: string;
  collectEnabled?: boolean;
}
