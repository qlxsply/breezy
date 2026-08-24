"use client";

import {
  applyPersonalizedConfigs,
  usePersonalizedConfigs,
} from "@admin/features/auth/public/session";
import { listPublicDictionaryOptions as listPublicDictOptions } from "@admin/features/dicts/public/dictionary-client";
import type { PublicDictionaryItem as PublicDictItem } from "@admin/features/dicts/public/types";
import { message } from "@admin/shared/lib/feedback/message";
import { formatDateByPattern, resolveUserTimeZoneCode } from "@admin/shared/lib/formatter";
import {
  type AdminDetailSection,
  AdminDetailTable,
  AdminEditableSection,
} from "@admin/shared/ui/admin";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { BzButton, BzOption, BzSelect, BzSwitch } from "@admin/shared/ui/bz";
import { useEffect, useMemo, useRef, useState } from "react";

import { getMyConfigs, updateMyConfig } from "./api/preferences-client";
import styles from "./Profile.module.css";

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
    USER_ADMIN_TAB_KEEP_ALIVE: true,
  });

  const [dictItems, setDictItems] = useState<Record<string, PublicDictItem[]>>({});
  const loadControllerRef = useRef<AbortController | null>(null);
  const saveLockRef = useRef(false);

  useEffect(() => {
    const controller = new AbortController();
    loadControllerRef.current = controller;
    void (async () => {
      try {
        await Promise.all([loadOptions(controller.signal), reload(controller.signal)]);
      } catch (error) {
        if (!controller.signal.aborted) {
          message.error(error instanceof Error ? error.message : "偏好设置加载失败");
        }
      }
    })();
    return () => controller.abort();
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
              <span className={styles.preferenceValue}>{currentLabels.timeZone}</span>
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
              <span className={styles.preferenceValue}>{currentLabels.dateTime}</span>
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
              <span className={styles.preferenceValue}>{currentLabels.date}</span>
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
              <span className={styles.preferenceValue}>{currentLabels.decimal}</span>
            ),
          },
          {
            label: "记忆后台标签页状态",
            value: editing ? (
              <BzSwitch
                modelValue={form.USER_ADMIN_TAB_KEEP_ALIVE}
                activeText="已开启"
                inactiveText="已关闭"
                onValueChange={(value) =>
                  setForm((prev) => ({ ...prev, USER_ADMIN_TAB_KEEP_ALIVE: value }))
                }
              />
            ) : (
              <span className={styles.preferenceValue}>
                {form.USER_ADMIN_TAB_KEEP_ALIVE ? "已开启" : "已关闭"}
              </span>
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
      form.USER_ADMIN_TAB_KEEP_ALIVE,
      form.USER_TIME_ZONE,
      timeZoneOptions,
    ],
  );

  async function loadOptions(signal: AbortSignal) {
    const entries = await Promise.all(
      PREFERENCE_DICT_CODES.map(
        async (code) => [code, await listPublicDictOptions(code, { signal })] as const,
      ),
    );
    if (!signal.aborted) setDictItems(Object.fromEntries(entries));
  }

  async function reload(signal?: AbortSignal) {
    const latestConfigs = await getMyConfigs({ signal });
    if (signal?.aborted) return;
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
      if (item.code === "USER_ADMIN_TAB_KEEP_ALIVE") {
        update.USER_ADMIN_TAB_KEEP_ALIVE = item.value.toLowerCase() === "true";
      }
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
    if (saveLockRef.current) return;
    saveLockRef.current = true;
    setSaving(true);
    try {
      const current = Object.fromEntries(configs.map((item) => [item.code, item.value]));
      const allUpdates: Array<[string, string]> = [
        ["USER_TIME_ZONE", form.USER_TIME_ZONE],
        ["USER_DATE_TIME_FORMAT", form.USER_DATE_TIME_FORMAT],
        ["USER_DATE_FORMAT", form.USER_DATE_FORMAT],
        ["USER_DECIMAL_FORMAT", form.USER_DECIMAL_FORMAT],
        ["USER_ADMIN_TAB_KEEP_ALIVE", String(form.USER_ADMIN_TAB_KEEP_ALIVE)],
      ];
      const updates = allUpdates.filter(([code, value]) => current[code] !== value);
      await Promise.all(updates.map(([code, value]) => updateMyConfig(code, value)));
      await reload();
      setEditing(false);
      message.success("已确认");
    } catch (error) {
      message.error(error instanceof Error ? error.message : "偏好设置保存失败");
      try {
        await reload();
      } catch {
        // Keep the original mutation error as the only user-facing feedback.
      }
    } finally {
      saveLockRef.current = false;
      setSaving(false);
    }
  }

  useEffect(() => {
    if (!editing) applyConfigs(configs);
  }, [configs, editing]);

  return (
    <div className={layoutStyles.page}>
      <div className={layoutStyles.content}>
        <div className={layoutStyles.pageStack}>
          <div className={styles.pagePanel}>
            <AdminEditableSection
              title="显示与格式"
              actions={
                <>
                  {editing ? (
                    <BzButton
                      disabled={saving}
                      onClick={cancelEdit}
                    >
                      取消
                    </BzButton>
                  ) : null}
                  <BzButton
                    buttonType={editing ? "primary" : undefined}
                    loading={saving}
                    onClick={editing ? submit : startEdit}
                  >
                    {editing ? "保存" : "编辑"}
                  </BzButton>
                </>
              }
            >
              <div className={`${styles.preferencesTable} ${styles.detailTable}`}>
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
