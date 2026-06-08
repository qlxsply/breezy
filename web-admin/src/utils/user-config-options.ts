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
  {
    code: "YYYY_MM_DD_HH_MM_SS",
    label: "2026-06-04 14:30:45 (年-月-日 24小时)",
    value: "yyyy-MM-dd HH:mm:ss",
  },
  {
    code: "YYYY_SLASH_MM_DD_HH_MM_SS",
    label: "2026/06/04 14:30:45 (年/月/日 24小时)",
    value: "yyyy/MM/dd HH:mm:ss",
  },
  {
    code: "DD_SLASH_MM_YYYY_HH_MM_SS",
    label: "04/06/2026 14:30:45 (日/月/年 24小时)",
    value: "dd/MM/yyyy HH:mm:ss",
  },
  {
    code: "MM_DD_YYYY_HH_MM",
    label: "06-04-2026 14:30 (月-日-年 24小时)",
    value: "MM-dd-yyyy HH:mm",
  },
];

export const USER_DATE_FORMAT_OPTIONS: UserConfigOptionItem[] = [
  { code: "YYYY_MM_DD", label: "2026-06-04 (年-月-日)", value: "yyyy-MM-dd" },
  { code: "YYYY_SLASH_MM_DD", label: "2026/06/04 (年/月/日)", value: "yyyy/MM/dd" },
  { code: "DD_SLASH_MM_YYYY", label: "04/06/2026 (日/月/年)", value: "dd/MM/yyyy" },
  { code: "MM_DD_YYYY", label: "06-04-2026 (月-日-年)", value: "MM-dd-yyyy" },
];

export const USER_DECIMAL_FORMAT_OPTIONS: UserConfigOptionItem[] = [
  { code: "COMMA_2", label: "1,234.56 (千分位，两位小数)", value: "#,##0.00" },
  { code: "COMMA_3", label: "1,234.567 (千分位，三位小数)", value: "#,##0.000" },
  { code: "PLAIN_2", label: "1234.56 (无千分位，两位小数)", value: "0.00" },
  { code: "PLAIN_4", label: "1234.5678 (无千分位，四位小数)", value: "0.0000" },
];

function findValue(
  options: UserConfigOptionItem[],
  code: string | null | undefined,
  fallback: string,
): string {
  if (!code) return fallback;
  const matched = options.find((item) => item.code === code.trim());
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

export function resolveUserConfigLabel(
  code: string,
  value: string | null | undefined,
): string | null {
  if (code === "USER_TIME_ZONE") {
    return findLabel(USER_TIME_ZONE_OPTIONS, value, "中国上海 (UTC+08:00)");
  }
  if (code === "USER_DATE_TIME_FORMAT") {
    return findLabel(USER_DATE_TIME_FORMAT_OPTIONS, value, "2026-06-04 14:30:45 (年-月-日 24小时)");
  }
  if (code === "USER_DATE_FORMAT") {
    return findLabel(USER_DATE_FORMAT_OPTIONS, value, "2026-06-04 (年-月-日)");
  }
  if (code === "USER_DECIMAL_FORMAT") {
    return findLabel(USER_DECIMAL_FORMAT_OPTIONS, value, "1,234.56 (千分位，两位小数)");
  }
  return null;
}
