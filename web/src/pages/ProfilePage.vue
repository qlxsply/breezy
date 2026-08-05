<!-- /src/pages/ProfilePage.vue -->
<template>
  <div class="app-shell">
    <div class="content">
      <div class="card">
        <div class="title">用户信息</div>
        <div class="row">
          <span class="label">用户名</span>
          <span class="value">{{ user?.username || "未登录" }}</span>
        </div>
        <div class="row">
          <span class="label">状态</span>
          <span class="value">{{ user ? "已登录" : "游客" }}</span>
        </div>
      </div>

      <div class="card">
        <div class="title">个性化显示设置</div>
        <bz-form
          label-width="120px"
          @submit.prevent
        >
          <bz-form-item label="时区">
            <bz-select
              v-model="form.USER_TIME_ZONE"
              :disabled="submitting || !canSave"
            >
              <bz-option
                v-for="option in timeZoneOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </bz-select>
          </bz-form-item>
          <bz-form-item label="日期时间格式">
            <bz-select
              v-model="form.USER_DATE_TIME_FORMAT"
              :disabled="submitting || !canSave"
            >
              <bz-option
                v-for="option in dateTimeFormatOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </bz-select>
          </bz-form-item>
          <bz-form-item label="日期格式">
            <bz-select
              v-model="form.USER_DATE_FORMAT"
              :disabled="submitting || !canSave"
            >
              <bz-option
                v-for="option in dateFormatOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </bz-select>
          </bz-form-item>
          <bz-form-item label="小数格式">
            <bz-select
              v-model="form.USER_DECIMAL_FORMAT"
              :disabled="submitting || !canSave"
            >
              <bz-option
                v-for="option in decimalFormatOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </bz-select>
          </bz-form-item>
        </bz-form>

        <div class="actions">
          <bz-button
            v-if="canSave"
            :disabled="submitting"
            @click="reload"
            >重载</bz-button
          >
          <bz-button
            v-if="canSave"
            type="primary"
            :loading="submitting"
            @click="save"
            >保存</bz-button
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";

import { updateMyConfig } from "../api/configs";
import { batchListDictOptions } from "../api/dicts";
import { ensureAuthLoaded, useAuthUser, usePersonalizedConfigs } from "../registry/auth.registry";
import { hasUserPermissionCode } from "../registry/user-tool-permissions.registry";
import { message } from "../utils/message";
import {
  USER_DATE_FORMAT_OPTIONS,
  USER_DATE_TIME_FORMAT_OPTIONS,
  USER_DECIMAL_FORMAT_OPTIONS,
  USER_TIME_ZONE_OPTIONS,
} from "../utils/user-config-options";

const user = computed(() => useAuthUser().value);
const configs = usePersonalizedConfigs();
const canSave = computed(() => Boolean(user.value) && hasUserPermissionCode("pro.cfg.edit"));
const submitting = ref(false);

type OptionItem = { label: string; value: string };

const timeZoneOptions = ref<OptionItem[]>([]);
const dateTimeFormatOptions = ref<OptionItem[]>([]);
const dateFormatOptions = ref<OptionItem[]>([]);
const decimalFormatOptions = ref<OptionItem[]>([]);

const form = reactive({
  USER_TIME_ZONE: "Asia/Shanghai",
  USER_DATE_TIME_FORMAT: "yyyy-MM-dd HH:mm:ss",
  USER_DATE_FORMAT: "yyyy-MM-dd",
  USER_DECIMAL_FORMAT: "COMMA_DOT",
});

void Promise.all([loadOptions(), reload()]);

async function loadOptions() {
  try {
    const result = await batchListDictOptions([
      "USER_TIME_ZONE",
      "USER_DATE_TIME_FORMAT",
      "USER_DATE_FORMAT",
      "USER_DECIMAL_FORMAT",
    ]);
    timeZoneOptions.value = toOptions(result.USER_TIME_ZONE || []);
    dateTimeFormatOptions.value = toOptions(result.USER_DATE_TIME_FORMAT || []);
    dateFormatOptions.value = toOptions(result.USER_DATE_FORMAT || []);
    decimalFormatOptions.value = toOptions(result.USER_DECIMAL_FORMAT || []);
    if (timeZoneOptions.value.length === 0)
      timeZoneOptions.value = toStaticOptions(USER_TIME_ZONE_OPTIONS);
    if (dateTimeFormatOptions.value.length === 0) {
      dateTimeFormatOptions.value = toStaticOptions(USER_DATE_TIME_FORMAT_OPTIONS);
    }
    if (dateFormatOptions.value.length === 0) {
      dateFormatOptions.value = toStaticOptions(USER_DATE_FORMAT_OPTIONS);
    }
    if (decimalFormatOptions.value.length === 0) {
      decimalFormatOptions.value = toStaticOptions(USER_DECIMAL_FORMAT_OPTIONS);
    }
  } catch (error) {
    timeZoneOptions.value = toStaticOptions(USER_TIME_ZONE_OPTIONS);
    dateTimeFormatOptions.value = toStaticOptions(USER_DATE_TIME_FORMAT_OPTIONS);
    dateFormatOptions.value = toStaticOptions(USER_DATE_FORMAT_OPTIONS);
    decimalFormatOptions.value = toStaticOptions(USER_DECIMAL_FORMAT_OPTIONS);
    if (error instanceof Error && error.message.trim().length > 0) {
      message.error(error.message);
      return;
    }
    message.error("个性化候选项加载失败");
  }
}

async function reload() {
  try {
    await ensureAuthLoaded(true);
    applyConfigs(configs.value);
  } catch (error) {
    if (error instanceof Error && error.message.trim().length > 0) {
      message.error(error.message);
      return;
    }
    message.error("个性化设置加载失败");
  }
}

async function save() {
  if (!canSave.value) {
    message.warning("请先登录后再保存设置");
    return;
  }
  submitting.value = true;
  try {
    await updateMyConfig("USER_TIME_ZONE", form.USER_TIME_ZONE);
    await updateMyConfig("USER_DATE_TIME_FORMAT", form.USER_DATE_TIME_FORMAT);
    await updateMyConfig("USER_DATE_FORMAT", form.USER_DATE_FORMAT);
    await updateMyConfig("USER_DECIMAL_FORMAT", form.USER_DECIMAL_FORMAT);
    await reload();
    message.success("个性化设置已保存");
  } catch (error) {
    if (error instanceof Error && error.message.trim().length > 0) {
      message.error(error.message);
      return;
    }
    message.error("个性化设置保存失败");
  } finally {
    submitting.value = false;
  }
}

function applyConfigs(configs: Array<{ code: string; value: string }>) {
  configs.forEach((item) => {
    if (item.code === "USER_TIME_ZONE") {
      form.USER_TIME_ZONE = item.value;
    }
    if (item.code === "USER_DATE_TIME_FORMAT") {
      form.USER_DATE_TIME_FORMAT = item.value;
    }
    if (item.code === "USER_DATE_FORMAT") {
      form.USER_DATE_FORMAT = item.value;
    }
    if (item.code === "USER_DECIMAL_FORMAT") {
      form.USER_DECIMAL_FORMAT = item.value;
    }
  });
}

function toOptions(
  items: Array<{ itemCode?: string; itemLabel: string; itemValue: string }>,
): OptionItem[] {
  return items.map((item) => ({ label: item.itemLabel, value: item.itemValue }));
}

function toStaticOptions(items: Array<{ label: string; value: string }>): OptionItem[] {
  return items.map((item) => ({ label: item.label, value: item.value }));
}
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  overflow-y: auto;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
  box-sizing: border-box;
}

.card {
  background: #fff;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  display: grid;
  gap: 16px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.hint {
  font-size: 12px;
  color: var(--text-muted);
}

.title {
  font-size: 16px;
  font-weight: 900;
}

.row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.label {
  color: var(--text-muted);
  font-weight: 700;
}

.value {
  font-weight: 700;
}
</style>
