"use client";

import { updateMyConfig } from "@admin/api/configs";
import { batchListDictOptions } from "@admin/api/dicts";
import {
  type AdminDetailSection,
  AdminDetailTable,
  AdminEditableSection,
} from "@admin/components/admin";
import { BzButton, BzOption, BzSelect } from "@admin/components/bz";
import {
  resolveUserDateFormatCode,
  resolveUserDateTimeFormatCode,
  resolveUserDecimalFormatCode,
  USER_DATE_FORMAT_OPTIONS,
  USER_DATE_TIME_FORMAT_OPTIONS,
  USER_DECIMAL_FORMAT_OPTIONS,
  USER_TIME_ZONE_OPTIONS,
  type UserConfigOptionItem,
} from "@admin/core/formatter";
import { message } from "@admin/core/message";
import { ensureAuthLoaded, usePersonalizedConfigs } from "@admin/core/registry/auth-registry";
import { useEffect, useMemo, useRef, useState } from "react";

interface OptionItem {
  label: string;
  value: string;
}

function applyPattern(date: Date, pattern: string): string {
  const map: Record<string, string> = {
    yyyy: String(date.getFullYear()),
    MM: String(date.getMonth() + 1).padStart(2, "0"),
    dd: String(date.getDate()).padStart(2, "0"),
    HH: String(date.getHours()).padStart(2, "0"),
    mm: String(date.getMinutes()).padStart(2, "0"),
    ss: String(date.getSeconds()).padStart(2, "0"),
  };
  let result = pattern;
  for (const key of Object.keys(map)) {
    result = result.replace(key, map[key]);
  }
  return result;
}

function formatDecimalByPattern(value: number, pattern: string): string {
  const parts = pattern.split(".");
  const fractionDigits = parts.length > 1 ? parts[1].length : 0;
  return value.toLocaleString(undefined, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
    useGrouping: pattern.includes(","),
  });
}

function resolveStaticLabel(
  code: string | undefined,
  fallbackLabel: string,
  items: UserConfigOptionItem[],
): string {
  const matched = items.find((item) => item.code === (code || "").trim());
  return matched?.value || fallbackLabel;
}

function buildDateTimeSampleLabel(code: string | undefined, fallbackLabel: string): string {
  if (!code) return fallbackLabel;
  return applyPattern(new Date(), resolveUserDateTimeFormatCode(code)) || fallbackLabel;
}

function buildDateSampleLabel(code: string | undefined, fallbackLabel: string): string {
  if (!code) return fallbackLabel;
  return applyPattern(new Date(), resolveUserDateFormatCode(code)) || fallbackLabel;
}

function buildDecimalSampleLabel(code: string | undefined, fallbackLabel: string): string {
  if (!code) return fallbackLabel;
  return formatDecimalByPattern(1234567.8912, resolveUserDecimalFormatCode(code)) || fallbackLabel;
}

function buildOptions(
  items: Array<{ itemCode?: string; itemLabel: string; itemValue: string }>,
  labelFn: (code: string | undefined, fallback: string) => string,
): OptionItem[] {
  return items.map((item) => ({
    label: labelFn(item.itemCode, item.itemLabel),
    value: item.itemCode || item.itemValue,
  }));
}

export function AdminProfilePreferencesPage() {
  const configs = usePersonalizedConfigs();
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    USER_TIME_ZONE: "ASIA_SHANGHAI",
    USER_DATE_TIME_FORMAT: "YYYY_MM_DD_HH_MM_SS",
    USER_DATE_FORMAT: "YYYY_MM_DD",
    USER_DECIMAL_FORMAT: "COMMA_2",
  });

  const [timeZoneOptions, setTimeZoneOptions] = useState<OptionItem[]>([]);
  const [dateTimeFormatOptions, setDateTimeFormatOptions] = useState<OptionItem[]>([]);
  const [dateFormatOptions, setDateFormatOptions] = useState<OptionItem[]>([]);
  const [decimalFormatOptions, setDecimalFormatOptions] = useState<OptionItem[]>([]);
  const loadedRef = useRef(false);

  useEffect(() => {
    if (loadedRef.current) return;
    loadedRef.current = true;
    void Promise.all([loadOptions(), reload()]);
  }, []);

  const currentLabels = useMemo(
    () => ({
      timeZone:
        timeZoneOptions.find((item) => item.value === form.USER_TIME_ZONE)?.label ||
        form.USER_TIME_ZONE,
      dateTime:
        dateTimeFormatOptions.find((item) => item.value === form.USER_DATE_TIME_FORMAT)?.label ||
        form.USER_DATE_TIME_FORMAT,
      date:
        dateFormatOptions.find((item) => item.value === form.USER_DATE_FORMAT)?.label ||
        form.USER_DATE_FORMAT,
      decimal:
        decimalFormatOptions.find((item) => item.value === form.USER_DECIMAL_FORMAT)?.label ||
        form.USER_DECIMAL_FORMAT,
    }),
    [timeZoneOptions, dateTimeFormatOptions, dateFormatOptions, decimalFormatOptions, form],
  );

  const detailSections = useMemo<AdminDetailSection[]>(
    () => [
      {
        title: "显示与格式",
        fields: [
          {
            label: "时区",
            value: editing ? (
              <BzSelect
                modelValue={form.USER_TIME_ZONE}
                onValueChange={(value) =>
                  setForm((prev) => ({ ...prev, USER_TIME_ZONE: value || "ASIA_SHANGHAI" }))
                }
              >
                {timeZoneOptions.map((option) => (
                  <BzOption
                    key={option.value}
                    label={option.label}
                    value={option.value}
                  />
                ))}
              </BzSelect>
            ) : (
              <span className="preferences-value">{currentLabels.timeZone}</span>
            ),
          },
          {
            label: "日期时间格式",
            value: editing ? (
              <BzSelect
                modelValue={form.USER_DATE_TIME_FORMAT}
                onValueChange={(value) =>
                  setForm((prev) => ({
                    ...prev,
                    USER_DATE_TIME_FORMAT: value || "YYYY_MM_DD_HH_MM_SS",
                  }))
                }
              >
                {dateTimeFormatOptions.map((option) => (
                  <BzOption
                    key={option.value}
                    label={option.label}
                    value={option.value}
                  />
                ))}
              </BzSelect>
            ) : (
              <span className="preferences-value">{currentLabels.dateTime}</span>
            ),
          },
          {
            label: "日期格式",
            value: editing ? (
              <BzSelect
                modelValue={form.USER_DATE_FORMAT}
                onValueChange={(value) =>
                  setForm((prev) => ({ ...prev, USER_DATE_FORMAT: value || "YYYY_MM_DD" }))
                }
              >
                {dateFormatOptions.map((option) => (
                  <BzOption
                    key={option.value}
                    label={option.label}
                    value={option.value}
                  />
                ))}
              </BzSelect>
            ) : (
              <span className="preferences-value">{currentLabels.date}</span>
            ),
          },
          {
            label: "小数格式",
            value: editing ? (
              <BzSelect
                modelValue={form.USER_DECIMAL_FORMAT}
                onValueChange={(value) =>
                  setForm((prev) => ({ ...prev, USER_DECIMAL_FORMAT: value || "COMMA_2" }))
                }
              >
                {decimalFormatOptions.map((option) => (
                  <BzOption
                    key={option.value}
                    label={option.label}
                    value={option.value}
                  />
                ))}
              </BzSelect>
            ) : (
              <span className="preferences-value">{currentLabels.decimal}</span>
            ),
          },
        ],
      },
    ],
    [
      currentLabels.date,
      currentLabels.dateTime,
      currentLabels.decimal,
      currentLabels.timeZone,
      dateFormatOptions,
      dateTimeFormatOptions,
      decimalFormatOptions,
      editing,
      form.USER_DATE_FORMAT,
      form.USER_DATE_TIME_FORMAT,
      form.USER_DECIMAL_FORMAT,
      form.USER_TIME_ZONE,
      timeZoneOptions,
    ],
  );

  async function loadOptions() {
    try {
      const result = await batchListDictOptions([
        "USER_TIME_ZONE",
        "USER_DATE_TIME_FORMAT",
        "USER_DATE_FORMAT",
        "USER_DECIMAL_FORMAT",
      ]);
      setTimeZoneOptions(
        buildOptions(result.USER_TIME_ZONE || [], (code, fallback) =>
          resolveStaticLabel(code, fallback, USER_TIME_ZONE_OPTIONS),
        ),
      );
      setDateTimeFormatOptions(
        buildOptions(result.USER_DATE_TIME_FORMAT || [], buildDateTimeSampleLabel),
      );
      setDateFormatOptions(buildOptions(result.USER_DATE_FORMAT || [], buildDateSampleLabel));
      setDecimalFormatOptions(
        buildOptions(result.USER_DECIMAL_FORMAT || [], buildDecimalSampleLabel),
      );
    } catch {
      setTimeZoneOptions(
        USER_TIME_ZONE_OPTIONS.map((item) => ({ label: item.value, value: item.code })),
      );
      setDateTimeFormatOptions(
        USER_DATE_TIME_FORMAT_OPTIONS.map((item) => ({ label: item.value, value: item.code })),
      );
      setDateFormatOptions(
        USER_DATE_FORMAT_OPTIONS.map((item) => ({ label: item.value, value: item.code })),
      );
      setDecimalFormatOptions(
        USER_DECIMAL_FORMAT_OPTIONS.map((item) => ({ label: item.value, value: item.code })),
      );
    }
  }

  async function reload() {
    await ensureAuthLoaded(true);
    applyConfigs(configs);
  }

  function applyConfigs(items: Array<{ code: string; value: string }>) {
    const update: Partial<typeof form> = {};
    items.forEach((item) => {
      if (item.code === "USER_TIME_ZONE") update.USER_TIME_ZONE = item.value;
      if (item.code === "USER_DATE_TIME_FORMAT") update.USER_DATE_TIME_FORMAT = item.value;
      if (item.code === "USER_DATE_FORMAT") update.USER_DATE_FORMAT = item.value;
      if (item.code === "USER_DECIMAL_FORMAT") update.USER_DECIMAL_FORMAT = item.value;
    });
    setForm((prev) => ({ ...prev, ...update }));
  }

  function startEdit() {
    setEditing(true);
  }

  function cancelEdit() {
    setEditing(false);
    applyConfigs(configs);
  }

  async function submit() {
    setSaving(true);
    try {
      await updateMyConfig("USER_TIME_ZONE", form.USER_TIME_ZONE);
      await updateMyConfig("USER_DATE_TIME_FORMAT", form.USER_DATE_TIME_FORMAT);
      await updateMyConfig("USER_DATE_FORMAT", form.USER_DATE_FORMAT);
      await updateMyConfig("USER_DECIMAL_FORMAT", form.USER_DECIMAL_FORMAT);
      await reload();
      setEditing(false);
      message.success("已确认");
    } catch (error) {
      message.error(error instanceof Error ? error.message : "偏好设置保存失败");
    } finally {
      setSaving(false);
    }
  }

  useEffect(() => {
    if (loadedRef.current) {
      applyConfigs(configs);
    }
  }, [configs]);

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          <div className="profile-page-panel">
            <AdminEditableSection
              title="显示与格式"
              actions={
                <>
                  {editing ? <BzButton disabled={saving} onClick={cancelEdit}>取消</BzButton> : null}
                  <BzButton buttonType={editing ? "primary" : undefined} loading={saving} onClick={editing ? submit : startEdit}>
                    {editing ? "保存" : "编辑"}
                  </BzButton>
                </>
              }
            >
              <div className="preferences-detail-table profile-detail-table">
              <AdminDetailTable
                sections={detailSections}
                variant="plain"
              />
              </div>
            </AdminEditableSection>
          </div>
        </div>
      </div>
    </div>
  );
}
