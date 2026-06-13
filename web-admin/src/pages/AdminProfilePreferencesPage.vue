<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card
          class="admin-panel admin-table-card"
          shadow="never"
        >
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">偏好设置</div>
            </div>
          </template>

          <div class="preferences-layout">
            <div class="preferences-head">
              <div class="preferences-title">个性化显示设置</div>
              <div class="preferences-actions">
                <bz-button
                  v-if="editing"
                  :disabled="saving"
                  @click="cancelEdit"
                  >取消</bz-button
                >
                <bz-button
                  :type="editing ? 'primary' : 'default'"
                  :loading="saving"
                  @click="editing ? submit() : startEdit()"
                >
                  {{ editing ? "确认" : "编辑" }}
                </bz-button>
              </div>
            </div>

            <bz-form
              label-position="top"
              class="preferences-grid"
            >
              <bz-form-item label="时区">
                <bz-select
                  v-if="editing"
                  v-model="form.USER_TIME_ZONE"
                >
                  <bz-option
                    v-for="option in timeZoneOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </bz-select>
                <div
                  v-else
                  class="preferences-value"
                >
                  {{ currentLabels.timeZone }}
                </div>
              </bz-form-item>
              <bz-form-item label="日期时间格式">
                <bz-select
                  v-if="editing"
                  v-model="form.USER_DATE_TIME_FORMAT"
                >
                  <bz-option
                    v-for="option in dateTimeFormatOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </bz-select>
                <div
                  v-else
                  class="preferences-value"
                >
                  {{ currentLabels.dateTime }}
                </div>
              </bz-form-item>
              <bz-form-item label="日期格式">
                <bz-select
                  v-if="editing"
                  v-model="form.USER_DATE_FORMAT"
                >
                  <bz-option
                    v-for="option in dateFormatOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </bz-select>
                <div
                  v-else
                  class="preferences-value"
                >
                  {{ currentLabels.date }}
                </div>
              </bz-form-item>
              <bz-form-item label="小数格式">
                <bz-select
                  v-if="editing"
                  v-model="form.USER_DECIMAL_FORMAT"
                >
                  <bz-option
                    v-for="option in decimalFormatOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </bz-select>
                <div
                  v-else
                  class="preferences-value"
                >
                  {{ currentLabels.decimal }}
                </div>
              </bz-form-item>
            </bz-form>
          </div>
        </bz-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";

import { updateMyConfig } from "../api/configs";
import { batchListDictOptions } from "../api/dicts";
import { ensureAuthLoaded, usePersonalizedConfigs } from "../registry/auth.registry";
import { message } from "../utils/message";
import {
  resolveUserDateFormatCode,
  resolveUserDateTimeFormatCode,
  resolveUserDecimalFormatCode,
  USER_DATE_FORMAT_OPTIONS,
  USER_DATE_TIME_FORMAT_OPTIONS,
  USER_DECIMAL_FORMAT_OPTIONS,
  USER_TIME_ZONE_OPTIONS,
} from "../utils/user-config-options";

type OptionItem = { label: string; value: string };

const configs = usePersonalizedConfigs();
const editing = ref(false);
const saving = ref(false);
const form = reactive({
  USER_TIME_ZONE: "ASIA_SHANGHAI",
  USER_DATE_TIME_FORMAT: "YYYY_MM_DD_HH_MM_SS",
  USER_DATE_FORMAT: "YYYY_MM_DD",
  USER_DECIMAL_FORMAT: "COMMA_2",
});

const timeZoneOptions = ref<OptionItem[]>([]);
const dateTimeFormatOptions = ref<OptionItem[]>([]);
const dateFormatOptions = ref<OptionItem[]>([]);
const decimalFormatOptions = ref<OptionItem[]>([]);

void Promise.all([loadOptions(), reload()]);

const currentLabels = computed(() => ({
  timeZone:
    timeZoneOptions.value.find((item) => item.value === form.USER_TIME_ZONE)?.label ||
    form.USER_TIME_ZONE,
  dateTime:
    dateTimeFormatOptions.value.find((item) => item.value === form.USER_DATE_TIME_FORMAT)?.label ||
    form.USER_DATE_TIME_FORMAT,
  date:
    dateFormatOptions.value.find((item) => item.value === form.USER_DATE_FORMAT)?.label ||
    form.USER_DATE_FORMAT,
  decimal:
    decimalFormatOptions.value.find((item) => item.value === form.USER_DECIMAL_FORMAT)?.label ||
    form.USER_DECIMAL_FORMAT,
}));

async function loadOptions() {
  try {
    const result = await batchListDictOptions([
      "USER_TIME_ZONE",
      "USER_DATE_TIME_FORMAT",
      "USER_DATE_FORMAT",
      "USER_DECIMAL_FORMAT",
    ]);
    timeZoneOptions.value = buildTimeZoneOptions(result.USER_TIME_ZONE || []);
    dateTimeFormatOptions.value = buildDateTimeOptions(result.USER_DATE_TIME_FORMAT || []);
    dateFormatOptions.value = buildDateOptions(result.USER_DATE_FORMAT || []);
    decimalFormatOptions.value = buildDecimalOptions(result.USER_DECIMAL_FORMAT || []);
  } catch {
    timeZoneOptions.value = USER_TIME_ZONE_OPTIONS.map((item) => ({
      label: item.label,
      value: item.code,
    }));
    dateTimeFormatOptions.value = USER_DATE_TIME_FORMAT_OPTIONS.map((item) => ({
      label: item.label,
      value: item.code,
    }));
    dateFormatOptions.value = USER_DATE_FORMAT_OPTIONS.map((item) => ({
      label: item.label,
      value: item.code,
    }));
    decimalFormatOptions.value = USER_DECIMAL_FORMAT_OPTIONS.map((item) => ({
      label: item.label,
      value: item.code,
    }));
  }
}

async function reload() {
  await ensureAuthLoaded(true);
  applyConfigs(configs.value);
}

function startEdit() {
  editing.value = true;
}

function cancelEdit() {
  editing.value = false;
  applyConfigs(configs.value);
}

async function submit() {
  saving.value = true;
  try {
    await updateMyConfig("USER_TIME_ZONE", form.USER_TIME_ZONE);
    await updateMyConfig("USER_DATE_TIME_FORMAT", form.USER_DATE_TIME_FORMAT);
    await updateMyConfig("USER_DATE_FORMAT", form.USER_DATE_FORMAT);
    await updateMyConfig("USER_DECIMAL_FORMAT", form.USER_DECIMAL_FORMAT);
    await reload();
    editing.value = false;
    message.success("已确认");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "偏好设置保存失败");
  } finally {
    saving.value = false;
  }
}

function applyConfigs(items: Array<{ code: string; value: string }>) {
  items.forEach((item) => {
    if (item.code === "USER_TIME_ZONE") form.USER_TIME_ZONE = item.value;
    if (item.code === "USER_DATE_TIME_FORMAT") form.USER_DATE_TIME_FORMAT = item.value;
    if (item.code === "USER_DATE_FORMAT") form.USER_DATE_FORMAT = item.value;
    if (item.code === "USER_DECIMAL_FORMAT") form.USER_DECIMAL_FORMAT = item.value;
  });
}

function buildTimeZoneOptions(
  items: Array<{ itemCode?: string; itemLabel: string; itemValue: string }>,
): OptionItem[] {
  return items.map((item) => ({
    label: resolveStaticLabel(item.itemCode, item.itemLabel, USER_TIME_ZONE_OPTIONS),
    value: item.itemCode || item.itemValue,
  }));
}

function buildDateTimeOptions(
  items: Array<{ itemCode?: string; itemLabel: string; itemValue: string }>,
): OptionItem[] {
  return items.map((item) => ({
    label: buildDateTimeSampleLabel(item.itemCode, item.itemLabel),
    value: item.itemCode || item.itemValue,
  }));
}

function buildDateOptions(
  items: Array<{ itemCode?: string; itemLabel: string; itemValue: string }>,
): OptionItem[] {
  return items.map((item) => ({
    label: buildDateSampleLabel(item.itemCode, item.itemLabel),
    value: item.itemCode || item.itemValue,
  }));
}

function buildDecimalOptions(
  items: Array<{ itemCode?: string; itemLabel: string; itemValue: string }>,
): OptionItem[] {
  return items.map((item) => ({
    label: buildDecimalSampleLabel(item.itemCode, item.itemLabel),
    value: item.itemCode || item.itemValue,
  }));
}

function resolveStaticLabel(
  code: string | undefined,
  fallbackLabel: string,
  items: Array<{ code: string; label: string }>,
): string {
  const matched = items.find((item) => item.code === (code || "").trim());
  return matched?.label || fallbackLabel;
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
</script>

<style scoped>
.content {
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}
.preferences-layout {
  display: grid;
  gap: 16px;
}
.preferences-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.preferences-title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}
.preferences-actions {
  display: flex;
  gap: 8px;
}
.preferences-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}
.preferences-value {
  min-height: 32px;
  padding: 6px 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}
@media (max-width: 900px) {
  .preferences-head {
    flex-direction: column;
    align-items: stretch;
  }
  .preferences-grid {
    grid-template-columns: 1fr;
  }
}
</style>
