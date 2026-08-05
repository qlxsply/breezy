export interface UserConfigOptionItem {
  code: string;
  label: string;
  value: string;
}

export const USER_TIME_ZONE_OPTIONS: UserConfigOptionItem[] = [
  { code: "ASIA_SHANGHAI", label: "中国上海 (UTC+08:00)", value: "Asia/Shanghai" },
  { code: "UTC", label: "协调世界时 UTC (UTC+00:00)", value: "UTC" },
  { code: "ASIA_TOKYO", label: "日本东京 (UTC+09:00)", value: "Asia/Tokyo" },
  { code: "EUROPE_BERLIN", label: "德国柏林 (UTC+01:00)", value: "Europe/Berlin" },
  {
    code: "AMERICA_NEW_YORK",
    label: "美国纽约 (UTC-05:00)",
    value: "America/New_York",
  },
];

export const USER_DATE_TIME_FORMAT_OPTIONS: UserConfigOptionItem[] = [
  {code: "YMD_DASH_24H_MINUTE", label: "2026-08-05 14:58（年-月-日 24小时制，精确到分钟）", value: "yyyy-MM-dd HH:mm"},
  {code: "YMD_DASH_24H_SECOND", label: "2026-08-05 14:58:36（年-月-日 24小时制，精确到秒）", value: "yyyy-MM-dd HH:mm:ss"},
  {code: "YMD_CHINESE_24H_MINUTE", label: "2026年8月5日 14:58（中文年月日 24小时制，精确到分钟）", value: "yyyy年M月d日 HH:mm"},
  {code: "YMD_CHINESE_24H_SECOND", label: "2026年8月5日 14:58:36（中文年月日 24小时制，精确到秒）", value: "yyyy年M月d日 HH:mm:ss"},
  {code: "YMD_SLASH_24H_MINUTE", label: "2026/08/05 14:58（年/月/日 24小时制，精确到分钟）", value: "yyyy/MM/dd HH:mm"},
  {code: "YMD_SLASH_24H_SECOND", label: "2026/08/05 14:58:36（年/月/日 24小时制，精确到秒）", value: "yyyy/MM/dd HH:mm:ss"},
  {code: "ISO_8601_OFFSET_MINUTE", label: "2026-08-05T14:58+08:00（ISO 8601，精确到分钟）", value: "yyyy-MM-dd'T'HH:mmXXX"},
  {code: "ISO_8601_OFFSET_SECOND", label: "2026-08-05T14:58:36+08:00（ISO 8601，精确到秒）", value: "yyyy-MM-dd'T'HH:mm:ssXXX"},
  {code: "DMY_SLASH_24H_MINUTE", label: "05/08/2026 14:58（日/月/年 24小时制，精确到分钟）", value: "dd/MM/yyyy HH:mm"},
  {code: "DMY_SLASH_24H_SECOND", label: "05/08/2026 14:58:36（日/月/年 24小时制，精确到秒）", value: "dd/MM/yyyy HH:mm:ss"},
  {code: "MDY_SLASH_12H_MINUTE", label: "08/05/2026 2:58 PM（月/日/年 12小时制，精确到分钟）", value: "MM/dd/yyyy h:mm a"},
  {code: "MDY_SLASH_12H_SECOND", label: "08/05/2026 2:58:36 PM（月/日/年 12小时制，精确到秒）", value: "MM/dd/yyyy h:mm:ss a"},
  {code: "ENGLISH_MONTH_12H_MINUTE", label: "Aug 5, 2026 2:58 PM（英文月份 12小时制，精确到分钟）", value: "MMM d, yyyy h:mm a"},
  {code: "ENGLISH_MONTH_12H_SECOND", label: "Aug 5, 2026 2:58:36 PM（英文月份 12小时制，精确到秒）", value: "MMM d, yyyy h:mm:ss a"},
  {code: "COMPACT_24H_MINUTE", label: "20260805 1458（紧凑格式，精确到分钟）", value: "yyyyMMdd HHmm"},
  {code: "COMPACT_24H_SECOND", label: "20260805 145836（紧凑格式，精确到秒）", value: "yyyyMMdd HHmmss"},
];

export const USER_DATE_FORMAT_OPTIONS: UserConfigOptionItem[] = [
  {code: "YMD_DASH", label: "2026-08-05（年-月-日）", value: "yyyy-MM-dd"},
  {code: "YMD_SLASH", label: "2026/08/05（年/月/日）", value: "yyyy/MM/dd"},
  {code: "YMD_CHINESE", label: "2026年8月5日（中文年月日）", value: "yyyy年M月d日"},
  {code: "DMY_SLASH", label: "05/08/2026（日/月/年）", value: "dd/MM/yyyy"},
  {code: "MDY_SLASH", label: "08/05/2026（月/日/年）", value: "MM/dd/yyyy"},
  {code: "EN_MONTH_SHORT", label: "Aug 5, 2026（英文月份简写）", value: "MMM d, yyyy"},
  {code: "EN_DAY_MONTH_SHORT", label: "5 Aug 2026（日 英文月份 年）", value: "d MMM yyyy"},
  {code: "COMPACT", label: "20260805（紧凑格式）", value: "yyyyMMdd"},
];

export const USER_DECIMAL_FORMAT_OPTIONS: UserConfigOptionItem[] = [
  {code: "COMMA_DOT", label: "1,234.56（千分位逗号、小数点）", value: "COMMA_DOT"},
  {code: "PLAIN_DOT", label: "1234.56（无千分位、小数点）", value: "PLAIN_DOT"},
  {code: "DOT_COMMA", label: "1.234,56（千分位点、逗号小数点）", value: "DOT_COMMA"},
];

function findValue(
  options: UserConfigOptionItem[],
  code: string | null | undefined,
  fallback: string,
): string {
  if (!code) return fallback;
  const matched = options.find((item) => item.code === code.trim() || item.value === code.trim());
  return matched?.value || fallback;
}

function findLabel(
  options: UserConfigOptionItem[],
  code: string | null | undefined,
  fallback: string,
): string {
  if (!code) return fallback;
  const matched = options.find((item) => item.code === code.trim());
  return matched?.label || fallback;
}

export function resolveUserTimeZoneCode(code: string | null | undefined): string {
  const candidate = findValue(USER_TIME_ZONE_OPTIONS, code, code?.trim() || "Asia/Shanghai");
  try {
    new Intl.DateTimeFormat("zh-CN", {timeZone: candidate}).format(new Date());
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

export function resolveUserConfigLabel(
  code: string,
  value: string | null | undefined,
): string | null {
  if (code === "USER_TIME_ZONE") {
    return findLabel(USER_TIME_ZONE_OPTIONS, value, "中国上海 (UTC+08:00)");
  }
  if (code === "USER_DATE_TIME_FORMAT") {
    return findLabel(USER_DATE_TIME_FORMAT_OPTIONS, value, "2026-08-05 14:58:36（年-月-日 24小时制，精确到秒）");
  }
  if (code === "USER_DATE_FORMAT") {
    return findLabel(USER_DATE_FORMAT_OPTIONS, value, "2026-08-05（年-月-日）");
  }
  if (code === "USER_DECIMAL_FORMAT") {
    return findLabel(USER_DECIMAL_FORMAT_OPTIONS, value, "1,234.56 (千分位，两位小数)");
  }
  return null;
}
