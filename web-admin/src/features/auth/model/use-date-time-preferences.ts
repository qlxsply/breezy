"use client";

import { useAuthUser } from "@admin/features/auth/model/auth-store";
import {
  type DateTimePrecision,
  resolveDateTimePrecision,
  resolveUserDateTimeFormatCode,
  resolveUserTimeZoneCode,
} from "@admin/shared/lib/formatter";
import { useMemo } from "react";

export interface DateTimePreferences {
  dateTimePattern: string;
  precision: DateTimePrecision;
  timeZone: string;
}

export function useDateTimePreferences(): DateTimePreferences {
  const configs = useAuthUser()?.configs ?? [];
  return useMemo(() => {
    const configuredPattern = configs
      .find((item) => item.code === "USER_DATE_TIME_FORMAT")
      ?.value.trim();
    const configuredTimeZone = configs.find((item) => item.code === "USER_TIME_ZONE")?.value.trim();
    const dateTimePattern = resolveUserDateTimeFormatCode(configuredPattern);
    return {
      dateTimePattern,
      precision: resolveDateTimePrecision(dateTimePattern),
      timeZone: resolveUserTimeZoneCode(configuredTimeZone),
    };
  }, [configs]);
}
