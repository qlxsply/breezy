import type { UserConfigItem } from "@admin/shared/types/user-config";

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

export interface UserConfigOptionItem {
  code: string;
  value: string;
}

export interface DateTimeFormatOption {
  value: string;
  label: string;
  minutePattern: string;
  secondPattern: string;
  minuteExample: string;
  secondExample: string;
  locale?: string;
  includesTimeZone?: boolean;
}

export interface DateTimeFormatCandidate {
  value: string;
  label: string;
  pattern: string;
  example: string;
  precision: DateTimePrecision;
}

export interface DateFormatOption {
  value: string;
  label: string;
  pattern: string;
  example: string;
  locale?: string;
}

export type DateTimePrecision = "minute" | "second";

export const USER_TIME_ZONE_OPTIONS: UserConfigOptionItem[] = [
  { code: "ASIA_SHANGHAI", value: "Asia/Shanghai" },
  { code: "UTC", value: "UTC" },
  { code: "ASIA_TOKYO", value: "Asia/Tokyo" },
  { code: "EUROPE_BERLIN", value: "Europe/Berlin" },
  { code: "AMERICA_NEW_YORK", value: "America/New_York" },
];

export const DATE_TIME_FORMAT_OPTIONS: DateTimeFormatOption[] = [
  {
    value: "YMD_DASH_24H",
    label: "年-月-日 24小时制",
    locale: "zh-CN",
    minutePattern: "yyyy-MM-dd HH:mm",
    secondPattern: "yyyy-MM-dd HH:mm:ss",
    minuteExample: "2026-08-05 14:58",
    secondExample: "2026-08-05 14:58:36",
  },
  {
    value: "YMD_CHINESE_24H",
    label: "中文年月日 24小时制",
    locale: "zh-CN",
    minutePattern: "yyyy年M月d日 HH:mm",
    secondPattern: "yyyy年M月d日 HH:mm:ss",
    minuteExample: "2026年8月5日 14:58",
    secondExample: "2026年8月5日 14:58:36",
  },
  {
    value: "YMD_SLASH_24H",
    label: "年/月/日 24小时制",
    locale: "zh-CN",
    minutePattern: "yyyy/MM/dd HH:mm",
    secondPattern: "yyyy/MM/dd HH:mm:ss",
    minuteExample: "2026/08/05 14:58",
    secondExample: "2026/08/05 14:58:36",
  },
  {
    value: "ISO_8601_OFFSET",
    label: "ISO 8601（包含时区）",
    minutePattern: "yyyy-MM-dd'T'HH:mmXXX",
    secondPattern: "yyyy-MM-dd'T'HH:mm:ssXXX",
    minuteExample: "2026-08-05T14:58+08:00",
    secondExample: "2026-08-05T14:58:36+08:00",
    includesTimeZone: true,
  },
  {
    value: "DMY_SLASH_24H",
    label: "日/月/年 24小时制",
    locale: "en-GB",
    minutePattern: "dd/MM/yyyy HH:mm",
    secondPattern: "dd/MM/yyyy HH:mm:ss",
    minuteExample: "05/08/2026 14:58",
    secondExample: "05/08/2026 14:58:36",
  },
  {
    value: "MDY_SLASH_12H",
    label: "月/日/年 12小时制",
    locale: "en-US",
    minutePattern: "MM/dd/yyyy h:mm a",
    secondPattern: "MM/dd/yyyy h:mm:ss a",
    minuteExample: "08/05/2026 2:58 PM",
    secondExample: "08/05/2026 2:58:36 PM",
  },
  {
    value: "ENGLISH_MONTH_12H",
    label: "英文月份 12小时制",
    locale: "en-US",
    minutePattern: "MMM d, yyyy h:mm a",
    secondPattern: "MMM d, yyyy h:mm:ss a",
    minuteExample: "Aug 5, 2026 2:58 PM",
    secondExample: "Aug 5, 2026 2:58:36 PM",
  },
  {
    value: "COMPACT_24H",
    label: "紧凑格式",
    minutePattern: "yyyyMMdd HHmm",
    secondPattern: "yyyyMMdd HHmmss",
    minuteExample: "20260805 1458",
    secondExample: "20260805 145836",
  },
];

export const DATE_TIME_FORMAT_CANDIDATES: DateTimeFormatCandidate[] =
  DATE_TIME_FORMAT_OPTIONS.flatMap((option) => [
    {
      value: `${option.value}_MINUTE`,
      label: `${option.label}（精确到分钟）`,
      pattern: option.minutePattern,
      example: option.minuteExample,
      precision: "minute",
    },
    {
      value: `${option.value}_SECOND`,
      label: `${option.label}（精确到秒）`,
      pattern: option.secondPattern,
      example: option.secondExample,
      precision: "second",
    },
  ]);

export const DATE_FORMAT_OPTIONS: DateFormatOption[] = [
  { value: "YMD_DASH", label: "年-月-日", pattern: "yyyy-MM-dd", example: "2026-08-05" },
  { value: "YMD_SLASH", label: "年/月/日", pattern: "yyyy/MM/dd", example: "2026/08/05" },
  {
    value: "YMD_CHINESE",
    label: "中文年月日",
    pattern: "yyyy年M月d日",
    example: "2026年8月5日",
    locale: "zh-CN",
  },
  {
    value: "DMY_SLASH",
    label: "日/月/年",
    pattern: "dd/MM/yyyy",
    example: "05/08/2026",
    locale: "en-GB",
  },
  {
    value: "MDY_SLASH",
    label: "月/日/年",
    pattern: "MM/dd/yyyy",
    example: "08/05/2026",
    locale: "en-US",
  },
  {
    value: "EN_MONTH_SHORT",
    label: "英文月份简写",
    pattern: "MMM d, yyyy",
    example: "Aug 5, 2026",
    locale: "en-US",
  },
  {
    value: "EN_DAY_MONTH_SHORT",
    label: "日 英文月份 年",
    pattern: "d MMM yyyy",
    example: "5 Aug 2026",
    locale: "en-GB",
  },
  { value: "COMPACT", label: "紧凑格式", pattern: "yyyyMMdd", example: "20260805" },
];

export const USER_DATE_TIME_FORMAT_OPTIONS: UserConfigOptionItem[] =
  DATE_TIME_FORMAT_CANDIDATES.map((option) => ({
    code: option.value,
    value: option.pattern,
  }));

export const USER_DATE_FORMAT_OPTIONS: UserConfigOptionItem[] = DATE_FORMAT_OPTIONS.map(
  (option) => ({
    code: option.value,
    value: option.pattern,
  }),
);

export const USER_DECIMAL_FORMAT_OPTIONS: UserConfigOptionItem[] = [
  { code: "COMMA_DOT", value: "COMMA_DOT" },
  { code: "PLAIN_DOT", value: "PLAIN_DOT" },
  { code: "DOT_COMMA", value: "DOT_COMMA" },
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

  const matched = options.find((item) => item.code === code.trim() || item.value === code.trim());
  return matched?.value || fallback;
}

export function resolveUserTimeZoneCode(code: string | null | undefined): string {
  const candidate = findValue(USER_TIME_ZONE_OPTIONS, code, code?.trim() || "Asia/Shanghai");
  try {
    new Intl.DateTimeFormat("zh-CN", { timeZone: candidate }).format(new Date());
    return candidate;
  } catch {
    return "Asia/Shanghai";
  }
}

export function resolveUserDateTimeFormatCode(code: string | null | undefined): string {
  return findValue(USER_DATE_TIME_FORMAT_OPTIONS, code, "yyyy-MM-dd HH:mm:ss");
}

export function resolveUserDateFormatCode(code: string | null | undefined): string {
  return findValue(USER_DATE_FORMAT_OPTIONS, code, "yyyy-MM-dd");
}

export function resolveUserDecimalFormatCode(code: string | null | undefined): string {
  return findValue(USER_DECIMAL_FORMAT_OPTIONS, code, "COMMA_DOT");
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
  timeZone = getUserTimeZone(),
): string {
  if (!value) return "";
  const parts = parseDateTimeInput(String(value).trim());
  if (!parts) return "";
  return applyPattern(new Date(zonedDateTimeToEpochMillis(parts, timeZone)), pattern, timeZone);
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

export function formatDateByPattern(date: Date, pattern: string, timeZone: string): string {
  return applyPattern(date, pattern, timeZone);
}

export function formatUserPreferenceDate(
  date: Date,
  timeZoneCode: string,
  patternCode: string,
  dateOnly: boolean,
): string {
  const timeZone = resolveUserTimeZoneCode(timeZoneCode);
  const pattern = dateOnly
    ? resolveUserDateFormatCode(patternCode)
    : resolveUserDateTimeFormatCode(patternCode);
  return applyPattern(date, pattern, timeZone);
}

function applyPattern(date: Date, pattern: string, timeZone: string): string {
  const parts = getZonedParts(date, timeZone);
  const monthNames = [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
  ];
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
