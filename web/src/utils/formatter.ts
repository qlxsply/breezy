// /src/utils/formatter.ts
import { useAuthUser, usePersonalizedConfigs } from "../registry/auth.registry";
import {
  resolveUserDateFormatCode,
  resolveUserDateTimeFormatCode,
  resolveUserDecimalFormatCode,
  resolveUserTimeZoneCode,
} from "./user-config-options";

const DEFAULT_DATE_TIME_CODE = "yyyy-MM-dd HH:mm:ss";
const DEFAULT_DATE_CODE = "yyyy-MM-dd";
const DEFAULT_DECIMAL_CODE = "COMMA_DOT";
const DEFAULT_TIME_ZONE_CODE = "Asia/Shanghai";
const DATE_TIME_INPUT_PATTERN = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/;
const DATE_TIME_WITH_SECONDS_PATTERN = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})$/;
const USER_DATE_TIME_FORMAT = "USER_DATE_TIME_FORMAT";
const USER_DATE_FORMAT = "USER_DATE_FORMAT";
const USER_DECIMAL_FORMAT = "USER_DECIMAL_FORMAT";
const USER_TIME_ZONE = "USER_TIME_ZONE";

function getConfigs() {
  const personalized = usePersonalizedConfigs();
  if (personalized.value.length > 0) {
    return personalized.value;
  }
  const user = useAuthUser();
  return user.value?.configs ?? [];
}

function getConfigValue(code: string): string | undefined {
  const configs = getConfigs();
  const matched = configs.find((item) => item.code === code);
  return matched?.value;
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

export function formatDateTime(value: string | number | Date | null | undefined): string {
  if (!value) return "-";
  const date = toDate(value);
  if (!date) return "-";

  const pattern = resolveUserDateTimeFormatCode(
    getConfigValue(USER_DATE_TIME_FORMAT) || DEFAULT_DATE_TIME_CODE,
  );
  return applyPattern(date, pattern, getUserTimeZone());
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
  const num = typeof value === "number" ? value : parseFloat(value);
  if (isNaN(num)) return "-";

  const format = resolveUserDecimalFormatCode(
    getConfigValue(USER_DECIMAL_FORMAT) || DEFAULT_DECIMAL_CODE,
  );
  return num.toLocaleString(format === "DOT_COMMA" ? "de-DE" : "en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
    useGrouping: format !== "PLAIN_DOT",
  });
}

export function dateTimeInputToEpochMillisString(value: string): string | null {
  if (!value) {
    return null;
  }
  const parsed = parseDateTimeInput(value.trim());
  if (!parsed) {
    return null;
  }
  const timeZone = getUserTimeZone();
  const epochMillis = zonedDateTimeToEpochMillis(parsed, timeZone);
  return Number.isFinite(epochMillis) ? String(epochMillis) : null;
}

export function dateTimeInputToNextMinuteEpochMillisString(value: string): string | null {
  if (!value) {
    return null;
  }
  const parsed = parseDateTimeInput(value.trim());
  if (!parsed) {
    return null;
  }
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
  const timeZone = getUserTimeZone();
  const epochMillis = zonedDateTimeToEpochMillis(
    {
      year: nextMinute.getUTCFullYear(),
      month: nextMinute.getUTCMonth() + 1,
      day: nextMinute.getUTCDate(),
      hour: nextMinute.getUTCHours(),
      minute: nextMinute.getUTCMinutes(),
      second: nextMinute.getUTCSeconds(),
    },
    timeZone,
  );
  return Number.isFinite(epochMillis) ? String(epochMillis) : null;
}

export function epochMillisStringToDateTimeInput(value: string | null | undefined): string {
  if (!value) {
    return "";
  }
  const date = toDate(value);
  if (!date) {
    return "";
  }
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
    // Check if it's ISO or just yyyy-MM-dd
    const d = new Date(value);
    return isNaN(d.getTime()) ? null : d;
  }
  return null;
}

function applyPattern(date: Date, pattern: string, timeZone: string): string {
  const parts = getZonedParts(date, timeZone);
  const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  const hour12 = parts.hour % 12 || 12;
  const map: Record<string, string | number> = {
    yyyy: parts.year,
    MMM: monthNames[parts.month - 1],
    MM: pad(parts.month),
    M: parts.month,
    dd: pad(parts.day),
    d: parts.day,
    HH: pad(parts.hour),
    h: hour12,
    mm: pad(parts.minute),
    ss: pad(parts.second),
    a: parts.hour < 12 ? "AM" : "PM",
    XXX: formatTimeZoneOffset(date, timeZone),
  };
  return pattern
    .replace(/yyyy|MMM|MM|dd|HH|mm|ss|XXX|M|d|h|a/g, (token) => String(map[token]))
    .replaceAll("'", "");
}

function pad(n: number): string {
  return n < 10 ? "0" + n : String(n);
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

interface DateTimeParts {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
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

function formatTimeZoneOffset(date: Date, timeZone: string): string {
  const epochMillis = Math.floor(date.getTime() / 1000) * 1000;
  const totalMinutes = Math.round(timeZoneOffsetMillis(timeZone, epochMillis) / 60_000);
  const sign = totalMinutes < 0 ? "-" : "+";
  const absoluteMinutes = Math.abs(totalMinutes);
  return `${sign}${pad(Math.floor(absoluteMinutes / 60))}:${pad(absoluteMinutes % 60)}`;
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

function timeZoneOffsetMillis(timeZone: string, epochMillis: number): number {
  const parts = getZonedParts(new Date(epochMillis), timeZone);
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
