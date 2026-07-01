import type { UserConfigItem } from "./types";

const DEFAULT_DATE_TIME_CODE = "YYYY_MM_DD_HH_MM_SS";
const DEFAULT_DATE_CODE = "YYYY_MM_DD";
const DEFAULT_DECIMAL_CODE = "COMMA_2";
const DEFAULT_TIME_ZONE_CODE = "ASIA_SHANGHAI";
const DATE_TIME_INPUT_PATTERN = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/;
const DATE_TIME_WITH_SECONDS_PATTERN = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})$/;

const USER_DATE_TIME_FORMAT = "USER_DATE_TIME_FORMAT";
const USER_DATE_FORMAT = "USER_DATE_FORMAT";
const USER_DECIMAL_FORMAT = "USER_DECIMAL_FORMAT";
const USER_TIME_ZONE = "USER_TIME_ZONE";

export interface UserConfigOptionItem {
  code: string;
  value: string;
}

export type DateTimePrecision = "minute" | "second";

export const USER_TIME_ZONE_OPTIONS: UserConfigOptionItem[] = [
  { code: "ASIA_SHANGHAI", value: "Asia/Shanghai" },
  { code: "UTC", value: "UTC" },
  { code: "ASIA_TOKYO", value: "Asia/Tokyo" },
  { code: "EUROPE_BERLIN", value: "Europe/Berlin" },
  { code: "AMERICA_NEW_YORK", value: "America/New_York" },
];

export const USER_DATE_TIME_FORMAT_OPTIONS: UserConfigOptionItem[] = [
  { code: "YYYY_MM_DD_HH_MM_SS", value: "yyyy-MM-dd HH:mm:ss" },
  { code: "YYYY_SLASH_MM_DD_HH_MM_SS", value: "yyyy/MM/dd HH:mm:ss" },
  { code: "DD_SLASH_MM_YYYY_HH_MM_SS", value: "dd/MM/yyyy HH:mm:ss" },
  { code: "MM_DD_YYYY_HH_MM", value: "MM-dd-yyyy HH:mm" },
];

export const USER_DATE_FORMAT_OPTIONS: UserConfigOptionItem[] = [
  { code: "YYYY_MM_DD", value: "yyyy-MM-dd" },
  { code: "YYYY_SLASH_MM_DD", value: "yyyy/MM/dd" },
  { code: "DD_SLASH_MM_YYYY", value: "dd/MM/yyyy" },
  { code: "MM_DD_YYYY", value: "MM-dd-yyyy" },
];

export const USER_DECIMAL_FORMAT_OPTIONS: UserConfigOptionItem[] = [
  { code: "COMMA_2", value: "#,##0.00" },
  { code: "COMMA_3", value: "#,##0.000" },
  { code: "PLAIN_2", value: "0.00" },
  { code: "PLAIN_4", value: "0.0000" },
];

const formatterState: { configs: UserConfigItem[] } = {
  configs: [],
};

interface DateTimeParts {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
}

function findValue(
  options: UserConfigOptionItem[],
  code: string | null | undefined,
  fallback: string,
): string {
  if (!code) {
    return fallback;
  }

  const matched = options.find((item) => item.code === code.trim());
  return matched?.value || fallback;
}

export function resolveUserTimeZoneCode(code: string | null | undefined): string {
  return findValue(USER_TIME_ZONE_OPTIONS, code, "Asia/Shanghai");
}

export function resolveUserDateTimeFormatCode(code: string | null | undefined): string {
  return findValue(USER_DATE_TIME_FORMAT_OPTIONS, code, "yyyy-MM-dd HH:mm:ss");
}

export function resolveUserDateFormatCode(code: string | null | undefined): string {
  return findValue(USER_DATE_FORMAT_OPTIONS, code, "yyyy-MM-dd");
}

export function resolveUserDecimalFormatCode(code: string | null | undefined): string {
  return findValue(USER_DECIMAL_FORMAT_OPTIONS, code, "#,##0.00");
}

function getConfigValue(code: string): string | undefined {
  return formatterState.configs.find((item) => item.code === code)?.value;
}

export function setFormatterConfigs(configs: UserConfigItem[]): void {
  formatterState.configs = [...configs];
}

export function clearFormatterConfigs(): void {
  formatterState.configs = [];
}

export function getUserTimeZone(): string {
  const value = getConfigValue(USER_TIME_ZONE);
  const timeZone = resolveUserTimeZoneCode(value || DEFAULT_TIME_ZONE_CODE);
  try {
    new Intl.DateTimeFormat("zh-CN", { timeZone }).format(new Date());
    return timeZone;
  } catch {
    return resolveUserTimeZoneCode(DEFAULT_TIME_ZONE_CODE);
  }
}

export function getUserDateTimeFormatPattern(): string {
  return resolveUserDateTimeFormatCode(
    getConfigValue(USER_DATE_TIME_FORMAT) || DEFAULT_DATE_TIME_CODE,
  );
}

export function resolveDateTimePrecision(pattern: string | null | undefined): DateTimePrecision {
  return String(pattern || "").includes("ss") ? "second" : "minute";
}

export function getUserDateTimePrecision(): DateTimePrecision {
  return resolveDateTimePrecision(getUserDateTimeFormatPattern());
}

export function formatDateTime(value: string | number | Date | null | undefined): string {
  if (!value) return "-";
  const date = toDate(value);
  if (!date) return "-";

  const pattern = getUserDateTimeFormatPattern();
  return applyPattern(date, pattern, getUserTimeZone());
}

export function formatDateTimeInputValue(
  value: string | null | undefined,
  pattern = getUserDateTimeFormatPattern(),
): string {
  if (!value) return "";
  const parts = parseDateTimeInput(String(value).trim());
  if (!parts) return "";
  return applyPatternToParts(parts, pattern);
}

export function formatDate(value: string | number | Date | null | undefined): string {
  if (!value) return "-";
  const date = toDate(value);
  if (!date) return "-";

  const pattern = resolveUserDateFormatCode(getConfigValue(USER_DATE_FORMAT) || DEFAULT_DATE_CODE);
  return applyPattern(date, pattern, getUserTimeZone());
}

export function formatDecimal(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === "") return "-";
  const num = typeof value === "number" ? value : Number.parseFloat(value);
  if (Number.isNaN(num)) return "-";

  const pattern = resolveUserDecimalFormatCode(
    getConfigValue(USER_DECIMAL_FORMAT) || DEFAULT_DECIMAL_CODE,
  );
  const parts = pattern.split(".");
  const fractionDigits = parts.length > 1 ? parts[1].length : 0;
  const useGrouping = pattern.includes(",");

  return num.toLocaleString(undefined, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
    useGrouping,
  });
}

export function dateTimeInputToEpochMillisString(value: string): string | null {
  if (!value) return null;
  const parsed = parseDateTimeInput(value.trim());
  if (!parsed) return null;
  const epochMillis = zonedDateTimeToEpochMillis(parsed, getUserTimeZone());
  return Number.isFinite(epochMillis) ? String(epochMillis) : null;
}

export function dateTimeInputToEndExclusiveEpochMillisString(value: string): string | null {
  if (!value) return null;
  const parsed = parseDateTimeInput(value.trim());
  if (!parsed) return null;

  const precision = getUserDateTimePrecision();
  const next = new Date(
    Date.UTC(
      parsed.year,
      parsed.month - 1,
      parsed.day,
      parsed.hour,
      parsed.minute,
      parsed.second,
      0,
    ),
  );
  if (precision === "minute") {
    next.setUTCMinutes(next.getUTCMinutes() + 1);
  } else {
    next.setUTCSeconds(next.getUTCSeconds() + 1);
  }

  const epochMillis = zonedDateTimeToEpochMillis(
    {
      year: next.getUTCFullYear(),
      month: next.getUTCMonth() + 1,
      day: next.getUTCDate(),
      hour: next.getUTCHours(),
      minute: next.getUTCMinutes(),
      second: next.getUTCSeconds(),
    },
    getUserTimeZone(),
  );
  return Number.isFinite(epochMillis) ? String(epochMillis) : null;
}

export function buildDateTimeRangeSubmitValue(
  start: string | null | undefined,
  endInclusive: string | null | undefined,
): {
  startTimestamp: string | null;
  endTimestamp: string | null;
} {
  return {
    startTimestamp: start ? dateTimeInputToEpochMillisString(start) : null,
    endTimestamp: endInclusive ? dateTimeInputToEndExclusiveEpochMillisString(endInclusive) : null,
  };
}

export function dateTimeInputToNextMinuteEpochMillisString(value: string): string | null {
  if (!value) return null;
  const parsed = parseDateTimeInput(value.trim());
  if (!parsed) return null;

  const nextMinute = new Date(
    Date.UTC(
      parsed.year,
      parsed.month - 1,
      parsed.day,
      parsed.hour,
      parsed.minute,
      parsed.second,
      0,
    ),
  );
  nextMinute.setUTCMinutes(nextMinute.getUTCMinutes() + 1);

  const epochMillis = zonedDateTimeToEpochMillis(
    {
      year: nextMinute.getUTCFullYear(),
      month: nextMinute.getUTCMonth() + 1,
      day: nextMinute.getUTCDate(),
      hour: nextMinute.getUTCHours(),
      minute: nextMinute.getUTCMinutes(),
      second: nextMinute.getUTCSeconds(),
    },
    getUserTimeZone(),
  );

  return Number.isFinite(epochMillis) ? String(epochMillis) : null;
}

export function epochMillisStringToDateTimeInput(value: string | null | undefined): string {
  if (!value) return "";
  const date = toDate(value);
  if (!date) return "";
  return applyPattern(date, "yyyy-MM-ddTHH:mm", getUserTimeZone());
}

function toDate(value: unknown): Date | null {
  if (value instanceof Date) return value;
  if (typeof value === "number") return new Date(value);
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (/^\d+$/.test(trimmed)) {
      const millis = Number(trimmed);
      if (Number.isFinite(millis)) {
        return new Date(millis);
      }
    }
    const date = new Date(trimmed);
    return Number.isNaN(date.getTime()) ? null : date;
  }
  return null;
}

function applyPattern(date: Date, pattern: string, timeZone: string): string {
  const parts = getZonedParts(date, timeZone);
  const map: Record<string, string | number> = {
    yyyy: parts.year,
    MM: pad(parts.month),
    dd: pad(parts.day),
    HH: pad(parts.hour),
    mm: pad(parts.minute),
    ss: pad(parts.second),
  };

  let result = pattern;
  for (const key of Object.keys(map)) {
    result = result.replace(key, String(map[key]));
  }
  return result;
}

function pad(value: number): string {
  return value < 10 ? `0${value}` : String(value);
}

function parseDateTimeInput(value: string): DateTimeParts | null {
  const matched =
    value.match(DATE_TIME_INPUT_PATTERN) ?? value.match(DATE_TIME_WITH_SECONDS_PATTERN);
  if (!matched) {
    return null;
  }

  const [, y, m, d, h, min, sec] = matched;
  return {
    year: Number(y),
    month: Number(m),
    day: Number(d),
    hour: Number(h),
    minute: Number(min),
    second: Number(sec ?? "0"),
  };
}

function applyPatternToParts(parts: DateTimeParts, pattern: string): string {
  const map: Record<string, string | number> = {
    yyyy: parts.year,
    MM: pad(parts.month),
    dd: pad(parts.day),
    HH: pad(parts.hour),
    mm: pad(parts.minute),
    ss: pad(parts.second),
  };

  let result = pattern;
  for (const key of Object.keys(map)) {
    result = result.replace(key, String(map[key]));
  }
  return result;
}

function getZonedParts(date: Date, timeZone: string): DateTimeParts {
  const formatter = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  });

  const chunks = formatter.formatToParts(date);
  const values: Record<string, string> = {};
  chunks.forEach((chunk) => {
    if (chunk.type !== "literal") {
      values[chunk.type] = chunk.value;
    }
  });

  return {
    year: Number(values.year ?? "0"),
    month: Number(values.month ?? "1"),
    day: Number(values.day ?? "1"),
    hour: Number(values.hour ?? "0"),
    minute: Number(values.minute ?? "0"),
    second: Number(values.second ?? "0"),
  };
}

function zonedDateTimeToEpochMillis(parts: DateTimeParts, timeZone: string): number {
  const utcGuess = Date.UTC(
    parts.year,
    parts.month - 1,
    parts.day,
    parts.hour,
    parts.minute,
    parts.second,
    0,
  );
  const offset0 = timeZoneOffsetMillis(timeZone, utcGuess);
  let epoch = utcGuess - offset0;
  const offset1 = timeZoneOffsetMillis(timeZone, epoch);
  if (offset1 !== offset0) {
    epoch = utcGuess - offset1;
  }
  return epoch;
}

export function resolveUserConfigLabel(code: string, value: string): string {
  const trimmed = (value || "").trim();
  if (code === "USER_TIME_ZONE") return findValue(USER_TIME_ZONE_OPTIONS, trimmed, trimmed);
  if (code === "USER_DATE_TIME_FORMAT")
    return findValue(USER_DATE_TIME_FORMAT_OPTIONS, trimmed, trimmed);
  if (code === "USER_DATE_FORMAT") return findValue(USER_DATE_FORMAT_OPTIONS, trimmed, trimmed);
  if (code === "USER_DECIMAL_FORMAT")
    return findValue(USER_DECIMAL_FORMAT_OPTIONS, trimmed, trimmed);
  return value || "-";
}

function timeZoneOffsetMillis(timeZone: string, epochMillis: number): number {
  const date = new Date(epochMillis);
  const parts = getZonedParts(date, timeZone);
  const asUtc = Date.UTC(
    parts.year,
    parts.month - 1,
    parts.day,
    parts.hour,
    parts.minute,
    parts.second,
    0,
  );
  return asUtc - epochMillis;
}
