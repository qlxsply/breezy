"use client";

import {getMyConfigs, updateMyConfig} from "@admin/api/configs";
import {listPublicDictOptions, type PublicDictItem} from "@admin/api/dicts";
import {
  type AdminDetailSection,
  AdminDetailTable,
  AdminEditableSection,
} from "@admin/components/admin";
import { BzButton, BzOption, BzSelect } from "@admin/components/bz";
import {
  formatDateByPattern,
  resolveUserTimeZoneCode,
} from "@admin/core/formatter";
import { message } from "@admin/core/message";
import {applyPersonalizedConfigs, usePersonalizedConfigs} from "@admin/core/registry/auth-registry";
import { useEffect, useMemo, useRef, useState } from "react";

interface OptionItem {
  label: string;
  value: string;
}

function formatDecimalByPattern(value: number, format: string): string {
  return value.toLocaleString(format === "DOT_COMMA" ? "de-DE" : "en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
    useGrouping: format !== "PLAIN_DOT",
  });
}

const PREFERENCE_DICT_CODES = [
  "USER_TIME_ZONE",
  "USER_DATE_TIME_FORMAT",
  "USER_DATE_FORMAT",
  "USER_DECIMAL_FORMAT",
] as const;

export function AdminProfilePreferencesPage() {
  const configs = usePersonalizedConfigs();
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    USER_TIME_ZONE: "Asia/Shanghai",
    USER_DATE_TIME_FORMAT: "yyyy-MM-dd HH:mm:ss",
    USER_DATE_FORMAT: "yyyy-MM-dd",
    USER_DECIMAL_FORMAT: "COMMA_DOT",
  });

  const [dictItems, setDictItems] = useState<Record<string, PublicDictItem[]>>({});
  const loadedRef = useRef(false);

  useEffect(() => {
    if (loadedRef.current) return;
    loadedRef.current = true;
    void Promise.all([loadOptions(), reload()]);
  }, []);

  const preferenceOptions = useMemo(() => {
    const now = new Date();
    const timeZone = resolveUserTimeZoneCode(form.USER_TIME_ZONE);
    return {
      timeZone: (dictItems.USER_TIME_ZONE || []).map((item) => ({
        label: item.itemLabel,
        value: item.itemValue,
      })),
      dateTime: (dictItems.USER_DATE_TIME_FORMAT || []).map((item) => ({
        label: formatDateByPattern(now, item.itemValue, timeZone),
        value: item.itemValue,
      })),
      date: (dictItems.USER_DATE_FORMAT || []).map((item) => ({
        label: formatDateByPattern(now, item.itemValue, timeZone),
        value: item.itemValue,
      })),
      decimal: (dictItems.USER_DECIMAL_FORMAT || []).map((item) => ({
        label: formatDecimalByPattern(1234567.8912, item.itemValue),
        value: item.itemValue,
      })),
    } satisfies Record<string, OptionItem[]>;
  }, [dictItems, form.USER_TIME_ZONE]);

  const timeZoneOptions = preferenceOptions.timeZone;
  const dateTimeFormatOptions = preferenceOptions.dateTime;
  const dateFormatOptions = preferenceOptions.date;
  const decimalFormatOptions = preferenceOptions.decimal;

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
                  setForm((prev) => ({ ...prev, USER_TIME_ZONE: value || "Asia/Shanghai" }))
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
                    USER_DATE_TIME_FORMAT: value || "yyyy-MM-dd HH:mm:ss",
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
                  setForm((prev) => ({ ...prev, USER_DATE_FORMAT: value || "yyyy-MM-dd" }))
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
                  setForm((prev) => ({ ...prev, USER_DECIMAL_FORMAT: value || "COMMA_DOT" }))
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
      const entries = await Promise.all(PREFERENCE_DICT_CODES.map(async (code) => [
        code,
        await listPublicDictOptions(code),
      ] as const));
      setDictItems(Object.fromEntries(entries));
    } catch (error) {
      message.error(error instanceof Error ? error.message : "个性化配置候选项加载失败");
    }
  }

  async function reload() {
    const latestConfigs = await getMyConfigs();
    applyPersonalizedConfigs(latestConfigs);
    applyConfigs(latestConfigs);
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
