// /src/types/config-admin.ts

export type ConfigType = "STR" | "INT" | "LONG" | "BOOL" | "DEC" | "STR_LIST" | "STR_SET";
export type ConfigScope = "FRAMEWORK" | "SYSTEM" | "BUSINESS";
export type ConfigLevel = "SYSTEM" | "USER";

export type ClientIpMode =
  | "REMOTE_ADDR"
  | "X_REAL_IP"
  | "X_FORWARDED_FOR_FIRST"
  | "X_FORWARDED_FOR_LAST"
  | "CF_Connecting_IP"
  | "True_Client_IP";

export interface ConfigItem {
  code: string;
  scope: ConfigScope;
  description: string;
  valueType: ConfigType;
  value: string;
  level: ConfigLevel;
  personalized: boolean;
}

export interface ConfigClientIpPreviewRes {
  mode: ClientIpMode;
  resolvedIp: string | null;
  remoteAddr: string | null;
  xRealIp: string | null;
  xForwardedFor: string | null;
  cfConnectingIp: string | null;
  trueClientIp: string | null;
}

export interface ConfigTimeOffsetPreviewRes {
  serverNowEpochMillis: number;
  targetEpochMillis: number;
  calculatedOffsetSeconds: number | null;
  offsetSeconds: number;
  mockedEpochMillis: number;
}
