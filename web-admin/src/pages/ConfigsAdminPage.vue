<!-- /src/pages/ConfigsAdminPage.vue -->
<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card
          v-if="queryPanelVisible"
          class="admin-panel admin-filter-card"
          shadow="never"
        >
          <bz-form
            class="admin-filter-form"
            :class="{ 'is-collapsed': queryCollapsed }"
            @submit.prevent="applyFilters"
          >
            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">关键词</div>
                <div class="admin-filter-control">
                  <bz-input
                    v-model="keywordDraft"
                    placeholder="搜索配置项"
                    clearable
                    @keyup.enter="applyFilters"
                  />
                </div>
              </div>
            </bz-form-item>

            <div class="admin-filter-actions">
              <bz-button
                class="admin-filter-secondary"
                @click="resetFilters"
              >
                重置
              </bz-button>
              <bz-button
                class="admin-filter-primary"
                type="primary"
                native-type="submit"
              >
                搜索
              </bz-button>
              <div
                class="admin-filter-toggle-placeholder"
                aria-hidden="true"
              ></div>
            </div>
          </bz-form>
        </bz-card>

        <bz-card
          class="admin-panel admin-table-card"
          shadow="never"
        >
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">系统配置</div>
              <div class="admin-table-tools">
                <bz-button
                  class="admin-toolbar-primary"
                  type="primary"
                  @click="reload"
                >
                  <span class="admin-toolbar-primary__content">
                    <i
                      class="admin-toolbar-primary__icon admin-toolbar-primary__icon--reload"
                      aria-hidden="true"
                    ></i>
                    <span>刷新配置</span>
                  </span>
                </bz-button>
                <button
                  class="admin-vben-circle-button"
                  :class="{ 'is-active': queryPanelVisible }"
                  type="button"
                  :title="queryPanelVisible ? '关闭搜索框' : '打开搜索框'"
                  @click="queryPanelVisible = !queryPanelVisible"
                >
                  <i
                    class="admin-vben-circle-button__icon admin-vben-circle-button__icon--search"
                    aria-hidden="true"
                  ></i>
                </button>
                <button
                  class="admin-vben-circle-button"
                  type="button"
                  title="刷新列表"
                  @click="reload"
                >
                  <i
                    class="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh"
                    aria-hidden="true"
                  ></i>
                </button>
              </div>
            </div>
          </template>

          <div class="admin-table-surface">
            <bz-table
              v-loading="loading"
              :data="rows"
              empty-text="暂无配置"
              size="small"
            >
              <bz-table-column
                label="配置键"
                width="280"
              >
                <template #default="scope">
                  <div class="cell-title">
                    <span class="mono">{{ scope.row.code }}</span>
                    <bz-tag
                      v-if="scope.row.personalized"
                      size="small"
                      type="info"
                      >个性化</bz-tag
                    >
                  </div>
                </template>
              </bz-table-column>
              <bz-table-column
                prop="description"
                label="说明"
                width="240"
              />
              <bz-table-column
                label="当前值"
                min-width="320"
              >
                <template #default="scope">
                  <div
                    class="value-cell"
                    :title="renderValue(scope.row)"
                  >
                    {{ renderValue(scope.row) }}
                  </div>
                </template>
              </bz-table-column>
              <bz-table-column
                label="类型"
                width="110"
              >
                <template #default="scope">
                  <bz-tag size="small">{{
                    configValueTypeLabelMap[scope.row.valueType] || scope.row.valueType
                  }}</bz-tag>
                </template>
              </bz-table-column>
              <bz-table-column
                label="级别"
                width="110"
              >
                <template #default="scope">
                  <bz-tag
                    size="small"
                    :type="scope.row.level === 'USER' ? 'warning' : 'info'"
                  >
                    {{ configLevelLabelMap[scope.row.level] || scope.row.level }}
                  </bz-tag>
                </template>
              </bz-table-column>
              <bz-table-column
                label="操作"
                width="88"
                fixed="right"
              >
                <template #default="scope">
                  <AdminActionBar :actions="getRowActions(scope.row)" />
                </template>
              </bz-table-column>
            </bz-table>
          </div>

          <div
            v-if="page.totalElements > 0"
            class="dict-pagination-bar"
          >
            <div class="dict-pagination-summary">共 {{ page.totalElements }} 条记录</div>
            <div class="dict-pagination-right">
              <label class="dict-page-size">
                <select
                  class="dict-page-size__select"
                  :value="pageSize"
                  @change="handlePageSizeSelect"
                >
                  <option
                    v-for="size in pageSizeOptions"
                    :key="size"
                    :value="size"
                  >
                    {{ size }}条/页
                  </option>
                </select>
              </label>
              <div class="dict-page-list">
                <button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isFirstPage"
                  @click="goToPage(1)"
                >
                  <span aria-hidden="true">|&lt;</span>
                </button>
                <button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isFirstPage"
                  @click="goToPage(pageNo - 1)"
                >
                  <span aria-hidden="true">&lt;</span>
                </button>
                <template
                  v-for="(token, tokenIndex) in pageTokens"
                  :key="`${String(token)}-${tokenIndex}`"
                >
                  <button
                    v-if="typeof token === 'number'"
                    class="dict-page-btn"
                    :class="{ 'is-active': token === pageNo }"
                    type="button"
                    @click="goToPage(token)"
                  >
                    {{ token }}
                  </button>
                  <span
                    v-else
                    class="dict-page-ellipsis"
                  >
                    ...
                  </span>
                </template>
                <button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isLastPage"
                  @click="goToPage(pageNo + 1)"
                >
                  <span aria-hidden="true">&gt;</span>
                </button>
                <button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isLastPage"
                  @click="goToPage(totalPages)"
                >
                  <span aria-hidden="true">&gt;|</span>
                </button>
              </div>
            </div>
          </div>
        </bz-card>
      </div>
    </div>

    <bz-dialog
      v-if="editor.visible"
      :model-value="editor.visible"
      :title="editorTitle"
      :width="editorDialogWidth"
      :max-width="editorDialogMaxWidth"
      :close-on-click-modal="false"
      @close="closeEditor"
    >
      <div class="editor-meta">
        <div class="meta-item">
          <div class="meta-label">配置键</div>
          <div class="meta-value mono">{{ editor.item?.code }}</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">说明</div>
          <div class="meta-value">{{ editor.item?.description }}</div>
        </div>
      </div>

      <bz-form label-position="top">
        <template v-if="isClientIpEditor">
          <bz-form-item label="IP 获取方式">
            <bz-select v-model="editor.ipMode">
              <bz-option
                v-for="item in clientIpModeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </bz-select>
          </bz-form-item>

          <div class="editor-actions-row">
            <bz-button
              :disabled="editor.previewLoading"
              @click="runClientIpPreview"
            >
              {{ editor.previewLoading ? "测试中..." : "测试当前请求" }}
            </bz-button>
          </div>

          <div
            v-loading="editor.previewLoading"
            class="preview-panel"
          >
            <div class="preview-title">效果预览</div>
            <div
              v-if="editor.ipPreview"
              class="preview-grid"
            >
              <div class="preview-label">解析后客户端 IP</div>
              <div class="preview-value strong">{{ showText(editor.ipPreview.resolvedIp) }}</div>

              <div class="preview-label">REMOTE_ADDR</div>
              <div class="preview-value">{{ showText(editor.ipPreview.remoteAddr) }}</div>

              <div class="preview-label">X-Real-IP</div>
              <div class="preview-value">{{ showText(editor.ipPreview.xRealIp) }}</div>

              <div class="preview-label">X-Forwarded-For</div>
              <div class="preview-value">{{ showText(editor.ipPreview.xForwardedFor) }}</div>

              <div class="preview-label">CF-Connecting-IP</div>
              <div class="preview-value">{{ showText(editor.ipPreview.cfConnectingIp) }}</div>

              <div class="preview-label">True-Client-IP</div>
              <div class="preview-value">{{ showText(editor.ipPreview.trueClientIp) }}</div>
            </div>
            <div
              v-else
              class="preview-empty"
            >
              点击“测试当前请求”查看当前请求下的解析结果。
            </div>
          </div>
        </template>

        <template v-else-if="isTimeOffsetEditor">
          <bz-form-item label="偏移秒数（最终保存值）">
            <bz-input
              v-model="editor.offsetSecondsInput"
              type="number"
              placeholder="例如：0、60、-300"
            />
          </bz-form-item>

          <div class="editor-actions-row">
            <bz-button
              :disabled="editor.previewLoading"
              @click="refreshTimeOffsetPreview"
            >
              {{ editor.previewLoading ? "刷新中..." : "刷新预览" }}
            </bz-button>
            <bz-button @click="resetOffsetSeconds">重置为 0</bz-button>
          </div>

          <div class="assist-panel">
            <div class="assist-title">辅助计算（不会自动保存）</div>
            <div class="assist-row">
              <bz-date-picker
                v-model="editor.targetDateTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm"
                placeholder="选择目标时间"
              />
            </div>
            <div class="assist-row">
              <bz-button
                :disabled="editor.previewLoading"
                @click="calculateOffsetByTarget"
              >
                计算秒差
              </bz-button>
              <bz-button
                :disabled="editor.calculatedOffsetSeconds === null"
                @click="applyCalculatedOffset"
              >
                回填秒数
              </bz-button>
              <div
                v-if="editor.calculatedOffsetSeconds !== null"
                class="assist-result"
              >
                建议秒差：<span class="strong">{{ editor.calculatedOffsetSeconds }}</span>
              </div>
            </div>
          </div>

          <div
            v-loading="editor.previewLoading"
            class="preview-panel"
          >
            <div class="preview-title">效果预览（基于服务器时间）</div>
            <div
              v-if="editor.timePreview"
              class="preview-grid"
            >
              <div class="preview-label">服务器当前时间</div>
              <div class="preview-value">
                {{ formatEpoch(editor.timePreview.serverNowEpochMillis) }}
              </div>

              <div class="preview-label">按当前秒数偏移后</div>
              <div class="preview-value strong">
                {{ formatEpoch(editor.timePreview.mockedEpochMillis) }}
              </div>

              <div class="preview-label">当前秒数</div>
              <div class="preview-value">{{ editor.timePreview.offsetSeconds }}</div>

              <template v-if="editor.timePreview.calculatedOffsetSeconds !== null">
                <div class="preview-label">目标时间</div>
                <div class="preview-value">
                  {{ formatEpoch(editor.timePreview.targetEpochMillis) }}
                </div>

                <div class="preview-label">计算出的秒差</div>
                <div class="preview-value strong">
                  {{ editor.timePreview.calculatedOffsetSeconds }}
                </div>
              </template>
            </div>
            <div
              v-else
              class="preview-empty"
            >
              输入偏移秒数后可点击“刷新预览”查看效果。
            </div>
          </div>
        </template>

        <template v-else-if="isUserTimeZoneEditor">
          <bz-form-item label="时区">
            <bz-select v-model="editor.rawValue">
              <bz-option
                v-for="option in currentUserFormatOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </bz-select>
          </bz-form-item>
        </template>

        <template v-else-if="isDateFormatEditor || isDateTimeFormatEditor">
          <bz-form-item :label="isDateTimeFormatEditor ? '日期时间格式' : '日期格式'">
            <bz-select v-model="editor.rawValue">
              <bz-option
                v-for="option in currentUserFormatOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </bz-select>
          </bz-form-item>

          <div class="preview-panel">
            <div class="preview-title">格式样例</div>
            <div class="preview-grid">
              <div class="preview-label">示例时间</div>
              <div class="preview-value">{{ formatEpoch(datePreviewBase.getTime()) }}</div>

              <div class="preview-label">格式化结果</div>
              <div class="preview-value strong">{{ dateFormatPreviewValue }}</div>
            </div>
            <div class="preview-tip">按统一编码存储，前端按选项语义渲染。</div>
          </div>
        </template>

        <template v-else-if="isDecimalFormatEditor">
          <bz-form-item label="小数格式">
            <bz-select v-model="editor.rawValue">
              <bz-option
                v-for="option in currentUserFormatOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </bz-select>
          </bz-form-item>

          <div class="preview-panel">
            <div class="preview-title">格式样例</div>
            <div
              v-for="sample in decimalPreviewRows"
              :key="sample.source"
              class="preview-grid"
            >
              <div class="preview-label">原始值</div>
              <div class="preview-value">{{ sample.source }}</div>

              <div class="preview-label">格式化结果</div>
              <div class="preview-value strong">{{ sample.formatted }}</div>
            </div>
          </div>
        </template>

        <template v-else-if="isBoolEditor">
          <bz-form-item label="配置值">
            <bz-select v-model="editor.boolValue">
              <bz-option
                label="true"
                value="true"
              />
              <bz-option
                label="false"
                value="false"
              />
            </bz-select>
          </bz-form-item>
        </template>

        <template v-else-if="isMsgTypeConfigsEditor">
          <div class="msg-config-editor">
            <div class="msg-config-head">
              <div class="msg-col-type">消息类型</div>
              <div class="msg-col-route">目标路由</div>
              <div class="msg-col-priority">提醒优先级</div>
              <div class="msg-col-switch">SSE</div>
              <div class="msg-col-switch">WebPush</div>
              <div class="msg-col-switch">弹层</div>
              <div class="msg-col-switch">系统通知</div>
              <div class="msg-col-action"></div>
            </div>
            <div class="msg-config-list">
              <div
                v-for="(config, index) in editor.msgTypeConfigs"
                :key="index"
                class="msg-config-item"
              >
                <bz-select
                  v-model="config.msgType"
                  class="msg-col-type"
                  placeholder="选择类型"
                >
                  <bz-option
                    v-for="opt in getMsgTypeOptionsFor(index)"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                    :disabled="opt.disabled"
                  />
                </bz-select>
                <bz-input
                  v-model="config.route"
                  class="msg-col-route"
                  placeholder="例如 /todo/all"
                />
                <bz-select
                  v-model="config.priority"
                  class="msg-col-priority"
                  placeholder="选择优先级"
                >
                  <bz-option
                    v-for="opt in msgPriorityOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </bz-select>
                <div class="msg-col-switch">
                  <bz-switch v-model="config.sseEnabled" />
                </div>
                <div class="msg-col-switch">
                  <bz-switch v-model="config.webPushEnabled" />
                </div>
                <div class="msg-col-switch">
                  <bz-switch v-model="config.panelAutoOpen" />
                </div>
                <div class="msg-col-switch">
                  <bz-switch v-model="config.osNotificationEnabled" />
                </div>
                <div class="msg-col-action">
                  <bz-icon-action-button
                    icon="minus"
                    tone="danger"
                    title="删除"
                    @click="removeMsgConfigItem(index)"
                  />
                </div>
              </div>
            </div>
            <div class="msg-config-foot">
              <div class="msg-config-foot-left">
                <bz-button
                  v-if="canUpdate"
                  size="small"
                  @click="addMsgConfigItem"
                >
                  <span class="bz-icon bz-icon-plus" />
                  添加类型配置
                </bz-button>
              </div>
              <div class="msg-config-foot-right">
                <bz-select
                  v-if="canPreviewPush"
                  v-model="previewMsgType"
                  class="msg-preview-type"
                  placeholder="选择消息类型"
                  :disabled="previewSendOptions.length === 0"
                >
                  <bz-option
                    v-for="opt in previewSendOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </bz-select>
                <bz-button
                  v-if="canPreviewPush"
                  size="small"
                  type="primary"
                  :disabled="!canPreviewSend"
                  :loading="previewSending"
                  @click="previewSendCurrentRule"
                >
                  预览发送
                </bz-button>
              </div>
            </div>
            <div class="msg-config-preview">
              <div class="preview-title">规则预览</div>
              <div
                v-if="msgConfigPreviewRows.length === 0"
                class="preview-empty"
              >
                暂无规则，未配置类型将按默认规则处理。
              </div>
              <div
                v-for="row in msgConfigPreviewRows"
                :key="row.msgType"
                class="preview-row"
              >
                <div class="preview-row-title">{{ row.typeLabel }}</div>
                <div class="preview-row-meta">
                  路由：{{ row.routeLabel }} ｜ 优先级：{{ row.priorityLabel }} ｜ 渠道：{{
                    row.channelLabel
                  }}
                </div>
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="isAuthWhitelistEditor">
          <div class="whitelist-editor">
            <div class="whitelist-head">
              <div class="whitelist-col-type">匹配类型</div>
              <div class="whitelist-col-pattern">路径规则</div>
              <div class="whitelist-col-action"></div>
            </div>
            <div class="whitelist-list">
              <div
                v-for="(rule, index) in editor.authWhitelistRules"
                :key="index"
                class="whitelist-item"
              >
                <bz-select
                  v-model="rule.type"
                  class="whitelist-col-type"
                >
                  <bz-option
                    v-for="opt in authWhitelistMatchTypeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </bz-select>
                <bz-input
                  v-model="rule.pattern"
                  class="whitelist-col-pattern"
                  placeholder="例如 /api/auth/login、/api/public/**"
                />
                <div class="whitelist-col-action">
                  <bz-icon-action-button
                    icon="minus"
                    tone="danger"
                    title="删除"
                    @click="removeAuthWhitelistRule(index)"
                  />
                </div>
              </div>
            </div>
            <div class="whitelist-foot">
              <bz-button
                v-if="canUpdate"
                size="small"
                @click="addAuthWhitelistRule"
              >
                <span class="bz-icon bz-icon-plus" />
                添加白名单规则
              </bz-button>
            </div>
            <div class="whitelist-preview">
              <div class="preview-title">规则预览</div>
              <div
                v-if="authWhitelistPreviewRows.length === 0"
                class="preview-empty"
              >
                暂无规则。
              </div>
              <div
                v-for="(row, index) in authWhitelistPreviewRows"
                :key="`${row.type}:${row.pattern}:${index}`"
                class="preview-row"
              >
                <div class="preview-row-title">{{ row.typeLabel }}</div>
                <div class="preview-row-meta">{{ row.pattern }}</div>
              </div>
            </div>
          </div>
        </template>

        <template v-else-if="isListEditor">
          <bz-form-item label="配置值（列表）">
            <div class="list-editor">
              <div
                v-for="(_, index) in editor.listValue"
                :key="index"
                class="list-item"
              >
                <bz-input
                  v-model="editor.listValue[index]"
                  placeholder="输入项..."
                />
                <bz-icon-action-button
                  icon="minus"
                  tone="danger"
                  title="删除"
                  aria-label="删除"
                  @click="removeListItem(index)"
                />
              </div>
              <bz-icon-action-button
                icon="plus"
                tone="primary"
                title="新增"
                aria-label="新增"
                @click="addListItem"
              />
            </div>
          </bz-form-item>
        </template>

        <template v-else>
          <bz-form-item label="配置值">
            <bz-input
              v-model="editor.rawValue"
              :type="isNumberType(editor.item) ? 'number' : 'text'"
              clearable
            />
          </bz-form-item>
        </template>
      </bz-form>

      <div
        v-if="editorValidationError"
        class="form-error"
      >
        {{ editorValidationError }}
      </div>

      <template #footer>
        <bz-button @click="closeEditor">取消</bz-button>
        <bz-button
          v-if="canSaveEditor"
          type="primary"
          :loading="saving"
          @click="saveCurrentConfig"
        >
          保存
        </bz-button>
      </template>
    </bz-dialog>
  </div>
</template>

<script setup lang="ts">
// <script setup> + TS：顶层即 setup()。
import { computed, onMounted, reactive, ref, watch } from "vue";

import { listConfigs, previewClientIp, previewTimeOffset, updateConfigValue } from "../api/configs";
import { batchListDictOptions, listDictOptions } from "../api/dicts";
import { previewMsgPush } from "../api/sse";
import AdminActionBar from "../components/admin/AdminActionBar.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { AdminActionItem } from "../types/admin-action";
import type {
  ClientIpMode,
  ConfigClientIpPreviewRes,
  ConfigItem,
  ConfigTimeOffsetPreviewRes,
} from "../types/config-admin";
import type { DictItem, DictOption } from "../types/dict-admin";
import type { PageResult } from "../types/page";
import { message } from "../utils/message";
import {
  resolveUserConfigLabel,
  resolveUserDateFormatCode,
  resolveUserDateTimeFormatCode,
  resolveUserDecimalFormatCode,
  USER_DATE_FORMAT_OPTIONS,
  USER_DATE_TIME_FORMAT_OPTIONS,
  USER_DECIMAL_FORMAT_OPTIONS,
  USER_TIME_ZONE_OPTIONS,
} from "../utils/user-config-options";

interface MsgTypeConfigModel {
  msgType: string;
  route: string;
  priority: string;
  sseEnabled: boolean;
  webPushEnabled: boolean;
  panelAutoOpen: boolean;
  osNotificationEnabled: boolean;
}

interface AuthWhitelistRuleModel {
  type: string;
  pattern: string;
}

const clientIpModeOptions = ref<Array<{ value: ClientIpMode; label: string }>>([]);
const msgTypeOptions = ref<DictOption[]>([]);
const msgPriorityOptions = ref<DictOption[]>([]);
const defaultWhitelistTypeOptions: DictOption[] = [
  { label: "精确匹配", value: "exact" },
  { label: "Ant 路径匹配", value: "ant" },
  { label: "PathPattern 匹配", value: "pathPattern" },
];
const authWhitelistMatchTypeOptions = ref<DictOption[]>([...defaultWhitelistTypeOptions]);
const configValueTypeLabelMap = ref<Record<string, string>>({});
const configLevelLabelMap = ref<Record<string, string>>({});
const userTimeZoneOptions = ref<DictOption[]>([]);
const userDateTimeFormatOptions = ref<DictOption[]>([]);
const userDateFormatOptions = ref<DictOption[]>([]);
const userDecimalFormatOptions = ref<DictOption[]>([]);

const loading = ref(false);
const saving = ref(false);
const queryPanelVisible = ref(false);
const queryCollapsed = ref(true);
const keywordDraft = ref("");
const appliedKeyword = ref("");
const rows = ref<ConfigItem[]>([]);
const pageNo = ref(1);
const pageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;
const page = ref<PageResult<ConfigItem>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});

const datePreviewBase = new Date(2026, 2, 11, 15, 42, 9);
const decimalPreviewNumbers = [1234567.891, -9876.5, 0.126];

interface ConfigEditorState {
  visible: boolean;
  item: ConfigItem | null;
  rawValue: string;
  boolValue: "true" | "false";
  listValue: string[];
  msgTypeConfigs: MsgTypeConfigModel[];
  authWhitelistRules: AuthWhitelistRuleModel[];
  ipMode: ClientIpMode;
  ipPreview: ConfigClientIpPreviewRes | null;
  timePreview: ConfigTimeOffsetPreviewRes | null;
  offsetSecondsInput: string;
  targetDateTime: string;
  calculatedOffsetSeconds: number | null;
  previewLoading: boolean;
}

const editor = reactive<ConfigEditorState>({
  visible: false,
  item: null,
  rawValue: "",
  boolValue: "true",
  listValue: [],
  msgTypeConfigs: [],
  authWhitelistRules: [],
  ipMode: "REMOTE_ADDR",
  ipPreview: null,
  timePreview: null,
  offsetSecondsInput: "0",
  targetDateTime: "",
  calculatedOffsetSeconds: null,
  previewLoading: false,
});

const canUpdate = computed(() => hasResourceCodeAccess("config-system-edit"));
const canPreviewPush = computed(() => hasResourceCodeAccess("config-system-preview-push"));
const editorValidationError = computed(() => validateCurrentEditor());
const canSaveEditor = computed(
  () => canUpdate.value && !saving.value && !editorValidationError.value,
);

function getRowActions(row: ConfigItem): AdminActionItem[] {
  if (!canUpdate.value) {
    return [];
  }
  return [
    {
      key: `edit-${row.code}`,
      label: "编辑",
      tone: "edit",
      handler: () => openEditor(row),
    },
  ];
}

const totalPages = computed(() => Math.max(1, page.value.totalPages || 1));
const isFirstPage = computed(() => pageNo.value <= 1);
const isLastPage = computed(() => pageNo.value >= totalPages.value);
const pageTokens = computed<Array<number | "ellipsis">>(() => {
  const total = totalPages.value;
  const current = Math.min(Math.max(pageNo.value, 1), total);
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1);
  }
  if (current <= 4) {
    return [1, 2, 3, 4, 5, "ellipsis", total];
  }
  if (current >= total - 3) {
    return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  }
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
});

async function applyFilters() {
  appliedKeyword.value = keywordDraft.value;
  pageNo.value = 1;
  await reload();
}

async function resetFilters() {
  keywordDraft.value = "";
  await applyFilters();
}

function goToPage(targetPageNo: number) {
  const nextPage = Math.min(Math.max(targetPageNo, 1), totalPages.value);
  if (nextPage === pageNo.value) {
    return;
  }
  pageNo.value = nextPage;
  void reload();
}

function handlePageSizeSelect(event: Event) {
  const value = Number((event.target as HTMLSelectElement).value);
  if (!Number.isFinite(value) || value <= 0 || value === pageSize.value) {
    return;
  }
  pageSize.value = value;
  pageNo.value = 1;
  void reload();
}

const editorTitle = computed(() => {
  if (!editor.item) return "编辑配置";
  return `编辑配置 - ${editor.item.code}`;
});

const editorDialogWidth = computed(() => {
  if (isMsgTypeConfigsEditor.value) {
    return "1180px";
  }
  if (isAuthWhitelistEditor.value) {
    return "960px";
  }
  return "760px";
});

const editorDialogMaxWidth = computed(() => {
  if (isMsgTypeConfigsEditor.value) {
    return "96vw";
  }
  if (isAuthWhitelistEditor.value) {
    return "96vw";
  }
  return "min(92vw, 760px)";
});

const isClientIpEditor = computed(() => editor.item?.code === "CLIENT_IP_MODE");
const isUserTimeZoneEditor = computed(() => editor.item?.code === "USER_TIME_ZONE");
const isTimeOffsetEditor = computed(() => editor.item?.code === "TIME_OFFSET");
const isMsgTypeConfigsEditor = computed(() => editor.item?.code === "MSG_TYPE_CONFIGS");
const isAuthWhitelistEditor = computed(() => editor.item?.code === "AUTH_WHITELIST");
const isDateFormatEditor = computed(() => editor.item?.code === "USER_DATE_FORMAT");
const isDateTimeFormatEditor = computed(() => editor.item?.code === "USER_DATE_TIME_FORMAT");
const isDecimalFormatEditor = computed(() => editor.item?.code === "USER_DECIMAL_FORMAT");
const currentUserFormatOptions = computed(() => {
  if (isUserTimeZoneEditor.value) return userTimeZoneOptions.value;
  if (isDateTimeFormatEditor.value) return userDateTimeFormatOptions.value;
  if (isDateFormatEditor.value) return userDateFormatOptions.value;
  if (isDecimalFormatEditor.value) return userDecimalFormatOptions.value;
  return [] as DictOption[];
});
const isBoolEditor = computed(() => editor.item?.valueType === "BOOL");
const isListEditor = computed(() => {
  const type = editor.item?.valueType;
  if (isMsgTypeConfigsEditor.value || isAuthWhitelistEditor.value) return false;
  return type === "STR_LIST" || type === "STR_SET";
});

const dateFormatPreviewValue = computed(() => {
  if (isDateFormatEditor.value || isDateTimeFormatEditor.value) {
    const err = validateCurrentEditor();
    if (err) {
      return "-";
    }
  }
  const pattern = isDateTimeFormatEditor.value
    ? resolveUserDateTimeFormatCode(editor.rawValue.trim())
    : resolveUserDateFormatCode(editor.rawValue.trim());
  if (!pattern) return "-";
  return applyDatePattern(datePreviewBase, pattern);
});

const decimalPreviewRows = computed(() => {
  const pattern = resolveUserDecimalFormatCode(editor.rawValue.trim());
  const error =
    isDecimalFormatEditor.value &&
    !currentUserFormatOptions.value.some((item) => item.value === editor.rawValue.trim())
      ? "小数格式选项无效"
      : null;
  return decimalPreviewNumbers.map((num) => ({
    source: num,
    formatted: error ? "-" : formatDecimalByPattern(num, pattern),
  }));
});

const msgConfigPreviewRows = computed(() => {
  return editor.msgTypeConfigs
    .filter((item) => normalizeMsgTypeValue(item.msgType).length > 0)
    .map((item) => {
      const msgType = normalizeMsgTypeValue(item.msgType);
      return {
        msgType,
        typeLabel: resolveMsgTypeLabel(msgType),
        routeLabel: item.route.trim() || "默认",
        priorityLabel: item.priority.trim() || "MEDIUM",
        channelLabel: resolveChannelLabel(item),
      };
    });
});

const authWhitelistPreviewRows = computed(() => {
  return editor.authWhitelistRules
    .map((item) => ({
      type: normalizeWhitelistMatchType(item.type),
      pattern: item.pattern.trim(),
    }))
    .filter((item) => item.pattern.length > 0)
    .map((item) => ({
      type: item.type,
      typeLabel: resolveWhitelistMatchTypeLabel(item.type),
      pattern: item.pattern,
    }));
});

const previewSending = ref(false);
const previewMsgType = ref("");

const previewSendOptions = computed(() => {
  return editor.msgTypeConfigs
    .map((item) => normalizeMsgTypeValue(item.msgType))
    .filter((item, index, arr) => item.length > 0 && arr.indexOf(item) === index)
    .map((msgType) => ({
      label: resolveMsgTypeLabel(msgType),
      value: msgType,
    }));
});

const canPreviewSend = computed(() => {
  if (!canUpdate.value || !isMsgTypeConfigsEditor.value || previewSending.value) {
    return false;
  }
  if (!canPreviewPush.value) {
    return false;
  }
  if (!previewMsgType.value) {
    return false;
  }
  return editor.msgTypeConfigs.some(
    (item) => normalizeMsgTypeValue(item.msgType) === previewMsgType.value,
  );
});

watch(
  previewSendOptions,
  (options) => {
    if (options.length === 0) {
      previewMsgType.value = "";
      return;
    }
    const current = previewMsgType.value;
    if (!options.some((item) => item.value === current)) {
      previewMsgType.value = options[0].value;
    }
  },
  { immediate: true },
);

onMounted(() => {
  void Promise.all([
    reload(),
    loadClientIpModeOptions(),
    loadAuthWhitelistMatchTypeOptions(),
    loadConfigDictionaries(),
  ]);
});

async function loadConfigDictionaries() {
  try {
    const result = await batchListDictOptions([
      "CONFIG_VALUE_TYPE",
      "CONFIG_LEVEL",
      "USER_TIME_ZONE",
      "USER_DATE_TIME_FORMAT",
      "USER_DATE_FORMAT",
      "USER_DECIMAL_FORMAT",
    ]);
    configValueTypeLabelMap.value = toDictLabelMap(result.CONFIG_VALUE_TYPE);
    configLevelLabelMap.value = toDictLabelMap(result.CONFIG_LEVEL);
    userTimeZoneOptions.value = toDictOptions(result.USER_TIME_ZONE);
    userDateTimeFormatOptions.value = toDictOptions(result.USER_DATE_TIME_FORMAT);
    userDateFormatOptions.value = toDictOptions(result.USER_DATE_FORMAT);
    userDecimalFormatOptions.value = toDictOptions(result.USER_DECIMAL_FORMAT);
  } catch {
    configValueTypeLabelMap.value = {};
    configLevelLabelMap.value = {};
    userTimeZoneOptions.value = toStaticDictOptions(USER_TIME_ZONE_OPTIONS);
    userDateTimeFormatOptions.value = toStaticDictOptions(USER_DATE_TIME_FORMAT_OPTIONS);
    userDateFormatOptions.value = toStaticDictOptions(USER_DATE_FORMAT_OPTIONS);
    userDecimalFormatOptions.value = toStaticDictOptions(USER_DECIMAL_FORMAT_OPTIONS);
  }
}

function toDictLabelMap(items?: DictItem[]): Record<string, string> {
  const map: Record<string, string> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = item.itemLabel || item.itemValue;
  }
  return map;
}

function toDictOptions(items?: DictItem[]): DictOption[] {
  return (items || []).map((item) => ({
    label: item.itemLabel,
    value: item.itemCode || item.itemValue,
  }));
}

function toStaticDictOptions(items: Array<{ label: string; code: string }>): DictOption[] {
  return items.map((item) => ({ label: item.label, value: item.code }));
}

async function loadClientIpModeOptions() {
  try {
    const items = await listDictOptions("CLIENT_IP_MODE");
    clientIpModeOptions.value = items.map((item) => ({
      value: item.itemValue as ClientIpMode,
      label: item.itemLabel,
    }));
  } catch (error) {
    if (error instanceof Error && error.message.trim().length > 0) {
      message.error(error.message);
      return;
    }
    message.error("客户端 IP 模式候选项加载失败");
  }
}

async function loadMsgConfigOptions() {
  try {
    const types = await listDictOptions("MSG_TYPE");
    const priorities = await listDictOptions("MSG_PRIORITY");
    msgTypeOptions.value = types.map((t) => ({ label: t.itemLabel, value: t.itemValue }));
    msgPriorityOptions.value = priorities.map((p) => ({ label: p.itemLabel, value: p.itemValue }));
  } catch (error) {
    message.error("消息配置字典项加载失败");
  }
}

function defaultAuthWhitelistMatchTypeOptions(): DictOption[] {
  return defaultWhitelistTypeOptions.map((item) => ({ ...item }));
}

async function loadAuthWhitelistMatchTypeOptions() {
  try {
    const items = await listDictOptions("AUTH_WHITELIST_MATCH_TYPE");
    authWhitelistMatchTypeOptions.value = items.map((item) => ({
      label: item.itemLabel,
      value: item.itemValue,
    }));
    if (authWhitelistMatchTypeOptions.value.length === 0) {
      authWhitelistMatchTypeOptions.value = defaultAuthWhitelistMatchTypeOptions();
    }
  } catch (_error) {
    authWhitelistMatchTypeOptions.value = defaultAuthWhitelistMatchTypeOptions();
    message.error("白名单匹配类型候选项加载失败，已使用默认选项");
  }
}

async function reload() {
  loading.value = true;
  try {
    const requestedPageNo = pageNo.value;
    const p1 = listConfigs({
      keyword: appliedKeyword.value,
      pageNo: requestedPageNo,
      pageSize: pageSize.value,
    });
    if (clientIpModeOptions.value.length === 0) {
      void loadClientIpModeOptions();
    }
    if (msgTypeOptions.value.length === 0) {
      void loadMsgConfigOptions();
    }
    if (authWhitelistMatchTypeOptions.value.length === 0) {
      void loadAuthWhitelistMatchTypeOptions();
    }
    page.value = await p1;
    pageSize.value = page.value.pageSize || pageSize.value;

    if (page.value.totalElements > 0 && requestedPageNo > Math.max(1, page.value.totalPages)) {
      const fallbackPageNo = Math.max(1, page.value.totalPages);
      pageNo.value = fallbackPageNo;
      page.value = await listConfigs({
        keyword: appliedKeyword.value,
        pageNo: fallbackPageNo,
        pageSize: pageSize.value,
      });
    }

    pageNo.value = page.value.pageNo || 1;
    rows.value = page.value.elements;
  } finally {
    loading.value = false;
  }
}

function renderValue(item: ConfigItem): string {
  if (item.code === "MSG_TYPE_CONFIGS") {
    const list = parseMsgTypeConfigs(item.value);
    if (list.length === 0) return "[]";
    const preview = list
      .map((c) => c.msgType)
      .slice(0, 3)
      .join("，");
    return list.length > 3
      ? `${preview} ...（共 ${list.length} 类）`
      : `${preview}（共 ${list.length} 类）`;
  }
  if (item.code === "AUTH_WHITELIST") {
    const rules = parseAuthWhitelistRules(item.value);
    if (rules.length === 0) return "[]";
    const preview = rules
      .slice(0, 2)
      .map((rule) => `${rule.type}:${rule.pattern}`)
      .join("，");
    return rules.length > 2
      ? `${preview} ...（共 ${rules.length} 条）`
      : `${preview}（共 ${rules.length} 条）`;
  }
  if (item.valueType === "STR_LIST" || item.valueType === "STR_SET") {
    const list = parseStringList(item.value);
    if (list.length === 0) return "[]";
    const preview = list.slice(0, 3).join("，");
    return list.length > 3
      ? `${preview} ...（共 ${list.length} 项）`
      : `${preview}（共 ${list.length} 项）`;
  }
  if (item.code === "TIME_OFFSET") {
    const raw = (item.value || "").trim();
    return raw ? `${raw} 秒` : "0 秒";
  }
  const value = String(item.value ?? "").trim();
  const configLabel = resolveUserConfigLabel(item.code, value);
  if (configLabel) {
    return configLabel;
  }
  return value || "-";
}

function openEditor(item: ConfigItem) {
  editor.visible = true;
  editor.item = item;
  editor.rawValue = item.value ?? "";
  editor.boolValue = normalizeBool(item.value);
  editor.listValue = parseStringList(item.value);
  editor.msgTypeConfigs = parseMsgTypeConfigs(item.value);
  editor.authWhitelistRules = parseAuthWhitelistRules(item.value);
  editor.ipMode = normalizeClientIpMode(item.value);
  editor.ipPreview = null;
  editor.timePreview = null;
  editor.offsetSecondsInput = normalizeOffsetSeconds(item.value);
  editor.targetDateTime = "";
  editor.calculatedOffsetSeconds = null;
  editor.previewLoading = false;

  if (item.code === "CLIENT_IP_MODE") {
    void runClientIpPreview();
  }
  if (item.code === "TIME_OFFSET") {
    void refreshTimeOffsetPreview();
  }
}

function closeEditor() {
  if (saving.value) return;
  editor.visible = false;
  editor.item = null;
  previewMsgType.value = "";
}

async function saveCurrentConfig() {
  if (!canUpdate.value || !editor.item) return;

  const validationError = editorValidationError.value;
  if (validationError) {
    message.error(validationError);
    return;
  }

  const saveValue = buildSaveValue(editor.item);
  if (saveValue === null) {
    return;
  }

  let saved = false;
  saving.value = true;
  try {
    await updateConfigValue(editor.item.code, saveValue);
    editor.item.value = saveValue;
    message.success("保存成功");
    saved = true;
  } finally {
    saving.value = false;
    if (saved) {
      closeEditor();
    }
  }
}

function buildSaveValue(item: ConfigItem): string | null {
  if (item.code === "CLIENT_IP_MODE") {
    return editor.ipMode;
  }

  if (item.code === "TIME_OFFSET") {
    const offsetSeconds = parseOffsetSecondsInput();
    if (offsetSeconds === null) {
      message.error("偏移秒数必须是整数");
      return null;
    }
    return String(offsetSeconds);
  }

  if (item.code === "MSG_TYPE_CONFIGS") {
    const validConfigs = editor.msgTypeConfigs.filter(
      (v) => normalizeMsgTypeValue(v.msgType).length > 0,
    );
    return JSON.stringify(
      validConfigs.map((c) => ({
        msgType: normalizeMsgTypeValue(c.msgType),
        route: c.route.trim(),
        priority: c.priority.trim() || "MEDIUM",
        sseEnabled: c.sseEnabled,
        webPushEnabled: c.webPushEnabled,
        panelAutoOpen: c.panelAutoOpen,
        osNotificationEnabled: c.osNotificationEnabled,
      })),
    );
  }

  if (item.code === "AUTH_WHITELIST") {
    return JSON.stringify(
      editor.authWhitelistRules.map((rule) => ({
        type: normalizeWhitelistMatchType(rule.type),
        pattern: rule.pattern.trim(),
      })),
    );
  }

  if (item.valueType === "BOOL") {
    return editor.boolValue;
  }

  if (item.valueType === "STR_LIST" || item.valueType === "STR_SET") {
    const list = editor.listValue.map((v) => v.trim()).filter((v) => v.length > 0);
    return JSON.stringify(list);
  }

  return editor.rawValue;
}

async function runClientIpPreview() {
  if (!isClientIpEditor.value || !editor.visible) return;

  editor.previewLoading = true;
  try {
    editor.ipPreview = await previewClientIp(editor.ipMode);
  } finally {
    editor.previewLoading = false;
  }
}

async function refreshTimeOffsetPreview() {
  if (!isTimeOffsetEditor.value || !editor.visible) return;

  const offsetSeconds = parseOffsetSecondsInput();
  if (offsetSeconds === null) {
    message.error("偏移秒数必须是整数");
    return;
  }

  editor.previewLoading = true;
  try {
    editor.timePreview = await previewTimeOffset({ offsetSeconds });
  } finally {
    editor.previewLoading = false;
  }
}

async function calculateOffsetByTarget() {
  if (!isTimeOffsetEditor.value || !editor.visible) return;

  const offsetSeconds = parseOffsetSecondsInput();
  if (offsetSeconds === null) {
    message.error("偏移秒数必须是整数");
    return;
  }

  const targetEpochMillis = parseTargetDateTime(editor.targetDateTime);
  if (targetEpochMillis === null) {
    message.error("请先选择有效的目标时间");
    return;
  }

  editor.previewLoading = true;
  try {
    const preview = await previewTimeOffset({ offsetSeconds, targetEpochMillis });
    editor.timePreview = preview;
    editor.calculatedOffsetSeconds = preview.calculatedOffsetSeconds;
  } finally {
    editor.previewLoading = false;
  }
}

async function applyCalculatedOffset() {
  if (editor.calculatedOffsetSeconds === null) return;

  editor.offsetSecondsInput = String(editor.calculatedOffsetSeconds);
  await refreshTimeOffsetPreview();
}

function resetOffsetSeconds() {
  editor.offsetSecondsInput = "0";
  void refreshTimeOffsetPreview();
}

function addMsgConfigItem() {
  editor.msgTypeConfigs.push({
    msgType: "",
    route: "",
    priority: "MEDIUM",
    sseEnabled: true,
    webPushEnabled: false,
    panelAutoOpen: true,
    osNotificationEnabled: false,
  });
}

function removeMsgConfigItem(index: number) {
  editor.msgTypeConfigs.splice(index, 1);
}

function addAuthWhitelistRule() {
  editor.authWhitelistRules.push({
    type: defaultWhitelistMatchType(),
    pattern: "",
  });
}

function removeAuthWhitelistRule(index: number) {
  editor.authWhitelistRules.splice(index, 1);
}

function getMsgTypeOptionsFor(
  index: number,
): Array<{ label: string; value: string; disabled: boolean }> {
  const currentValue = editor.msgTypeConfigs[index]?.msgType?.trim() ?? "";
  const usedByOthers = new Set(
    editor.msgTypeConfigs
      .map((item, itemIndex) => (itemIndex === index ? "" : normalizeMsgTypeValue(item.msgType)))
      .filter((item) => item.length > 0),
  );

  const options = msgTypeOptions.value.map((opt) => ({
    label: opt.label,
    value: opt.value,
    disabled: usedByOthers.has(opt.value) && opt.value !== currentValue,
  }));

  if (currentValue && !options.some((opt) => opt.value === currentValue)) {
    options.unshift({
      label: resolveMsgTypeLabel(currentValue),
      value: currentValue,
      disabled: false,
    });
  }
  return options;
}

async function previewSendCurrentRule() {
  if (!canPreviewSend.value) {
    return;
  }

  const target = editor.msgTypeConfigs.find(
    (item) => normalizeMsgTypeValue(item.msgType) === previewMsgType.value,
  );
  if (!target) {
    message.error("未找到可预览发送的消息类型规则");
    return;
  }

  const itemValidation = validateMsgTypeConfigItem(target);
  if (itemValidation) {
    message.error(itemValidation);
    return;
  }

  previewSending.value = true;
  try {
    const result = await previewMsgPush({
      msgType: normalizeMsgTypeValue(target.msgType),
      route: target.route.trim(),
      priority: target.priority.trim().toUpperCase() || "MEDIUM",
      sseEnabled: target.sseEnabled,
      webPushEnabled: target.webPushEnabled,
      panelAutoOpen: target.panelAutoOpen,
      osNotificationEnabled: target.osNotificationEnabled,
    });
    message.success(result || "预览发送成功");
  } finally {
    previewSending.value = false;
  }
}

function addListItem() {
  editor.listValue.push("");
}

function removeListItem(index: number) {
  editor.listValue.splice(index, 1);
}

function parseOffsetSecondsInput(): number | null {
  const raw = editor.offsetSecondsInput.trim();
  if (!/^-?\d+$/.test(raw)) {
    return null;
  }
  const num = Number(raw);
  if (!Number.isSafeInteger(num)) {
    return null;
  }
  return num;
}

function parseTargetDateTime(value: string): number | null {
  const input = value.trim();
  const match = /^(\d{4})-(\d{2})-(\d{2})\s(\d{2}):(\d{2})$/.exec(input);
  if (!match) {
    return null;
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const hour = Number(match[4]);
  const minute = Number(match[5]);

  const date = new Date(year, month - 1, day, hour, minute, 0, 0);
  const valid =
    date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day &&
    date.getHours() === hour &&
    date.getMinutes() === minute;
  if (!valid) {
    return null;
  }
  return date.getTime();
}

function parseMsgTypeConfigs(raw: string): MsgTypeConfigModel[] {
  try {
    const parsed: unknown = JSON.parse(raw || "[]");
    if (!Array.isArray(parsed)) return [];
    return parsed.map((item) => normalizeMsgTypeConfig(item));
  } catch (_e) {
    return [];
  }
}

function normalizeMsgTypeConfig(item: unknown): MsgTypeConfigModel {
  const raw = item && typeof item === "object" ? (item as Record<string, unknown>) : {};
  const priority =
    String(raw.priority ?? "MEDIUM")
      .trim()
      .toUpperCase() || "MEDIUM";
  const isHigh = priority === "HIGH";
  const isLow = priority === "LOW";
  return {
    msgType: String(raw.msgType ?? ""),
    route: String(raw.route ?? ""),
    priority,
    sseEnabled: raw.sseEnabled === undefined ? true : Boolean(raw.sseEnabled),
    webPushEnabled: raw.webPushEnabled === undefined ? isHigh : Boolean(raw.webPushEnabled),
    panelAutoOpen: raw.panelAutoOpen === undefined ? !isLow : Boolean(raw.panelAutoOpen),
    osNotificationEnabled:
      raw.osNotificationEnabled === undefined ? isHigh : Boolean(raw.osNotificationEnabled),
  };
}

function parseAuthWhitelistRules(raw: string): AuthWhitelistRuleModel[] {
  try {
    const parsed: unknown = JSON.parse(raw || "[]");
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed.map((item) => normalizeAuthWhitelistRule(item));
  } catch (_e) {
    return [];
  }
}

function normalizeAuthWhitelistRule(item: unknown): AuthWhitelistRuleModel {
  const raw = item && typeof item === "object" ? (item as Record<string, unknown>) : {};
  return {
    type: normalizeWhitelistMatchType(raw.type),
    pattern: String(raw.pattern ?? "").trim(),
  };
}

function normalizeWhitelistMatchType(raw: unknown): string {
  const value = String(raw ?? "").trim();
  if (!value) {
    return defaultWhitelistMatchType();
  }

  const matched = authWhitelistMatchTypeOptions.value.find(
    (item) => item.value.toLowerCase() === value.toLowerCase(),
  );
  if (matched) {
    return matched.value;
  }

  if (value.toUpperCase() === "EXACT") return "exact";
  if (value.toUpperCase() === "ANT") return "ant";
  if (value.toUpperCase() === "PATH_PATTERN") return "pathPattern";
  return defaultWhitelistMatchType();
}

function defaultWhitelistMatchType(): string {
  return authWhitelistMatchTypeOptions.value[0]?.value || "exact";
}

function resolveWhitelistMatchTypeLabel(type: string): string {
  const matched = authWhitelistMatchTypeOptions.value.find((item) => item.value === type);
  if (matched) {
    return matched.label;
  }
  return type;
}

function parseStringList(raw: string): string[] {
  try {
    const parsed = JSON.parse(raw || "[]");
    if (!Array.isArray(parsed)) return [];
    return parsed.map((item) => String(item ?? "")).filter((item) => item.length > 0);
  } catch (_e) {
    return [];
  }
}

function normalizeBool(raw: string): "true" | "false" {
  const value = String(raw ?? "")
    .trim()
    .toLowerCase();
  return value === "false" ? "false" : "true";
}

function normalizeOffsetSeconds(raw: string): string {
  const text = String(raw ?? "").trim();
  if (/^-?\d+$/.test(text)) {
    return text;
  }
  return "0";
}

function normalizeClientIpMode(raw: string): ClientIpMode {
  const value = String(raw ?? "").trim();
  const matched = clientIpModeOptions.value.find((item) => item.value === value);
  return matched?.value || "REMOTE_ADDR";
}

function normalizeMsgTypeValue(raw: unknown): string {
  return String(raw ?? "").trim();
}

function isNumberType(item: ConfigItem | null): boolean {
  if (!item) return false;
  return item.valueType === "INT" || item.valueType === "LONG" || item.valueType === "DEC";
}

function applyDatePattern(date: Date, pattern: string): string {
  const map: Record<string, string> = {
    yyyy: String(date.getFullYear()),
    MM: pad(date.getMonth() + 1),
    dd: pad(date.getDate()),
    HH: pad(date.getHours()),
    mm: pad(date.getMinutes()),
    ss: pad(date.getSeconds()),
  };

  let result = pattern;
  Object.keys(map).forEach((token) => {
    result = result.split(token).join(map[token]);
  });
  return result;
}

function formatDecimalByPattern(value: number, pattern: string): string {
  const safePattern = pattern || "#,##0.00";
  const parts = safePattern.split(".");
  const fractionDigits = parts.length > 1 ? parts[1].length : 0;
  const useGrouping = safePattern.includes(",");

  try {
    const digits = Math.min(fractionDigits, 20);
    return value.toLocaleString(undefined, {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
      useGrouping,
    });
  } catch (_e) {
    return String(value);
  }
}

function validateCurrentEditor(): string | null {
  if (!editor.visible || !editor.item) {
    return null;
  }

  if (isMsgTypeConfigsEditor.value) {
    return validateMsgTypeConfigs();
  }

  if (isAuthWhitelistEditor.value) {
    return validateAuthWhitelistRules();
  }

  if (isTimeOffsetEditor.value) {
    const offsetSeconds = parseOffsetSecondsInput();
    if (offsetSeconds === null) {
      return "偏移秒数必须是整数";
    }
    return null;
  }

  if (isDateFormatEditor.value) {
    return currentUserFormatOptions.value.some((item) => item.value === editor.rawValue.trim())
      ? null
      : "日期格式选项无效";
  }

  if (isUserTimeZoneEditor.value) {
    return currentUserFormatOptions.value.some((item) => item.value === editor.rawValue.trim())
      ? null
      : "时区选项无效";
  }

  if (isDateTimeFormatEditor.value) {
    return currentUserFormatOptions.value.some((item) => item.value === editor.rawValue.trim())
      ? null
      : "日期时间格式选项无效";
  }

  if (isDecimalFormatEditor.value) {
    return currentUserFormatOptions.value.some((item) => item.value === editor.rawValue.trim())
      ? null
      : "小数格式选项无效";
  }

  return null;
}

function validateMsgTypeConfigs(): string | null {
  const used = new Set<string>();
  for (const item of editor.msgTypeConfigs) {
    const msgType = normalizeMsgTypeValue(item.msgType);
    if (!msgType) {
      continue;
    }
    if (used.has(msgType)) {
      return `消息类型 ${resolveMsgTypeLabel(msgType)} 重复，请删除重复配置`;
    }
    used.add(msgType);

    const rowError = validateMsgTypeConfigItem(item);
    if (rowError) {
      return rowError;
    }
  }
  return null;
}

function validateAuthWhitelistRules(): string | null {
  const used = new Set<string>();
  for (let index = 0; index < editor.authWhitelistRules.length; index += 1) {
    const item = editor.authWhitelistRules[index];
    const type = normalizeWhitelistMatchType(item.type);
    const pattern = item.pattern.trim();
    if (!pattern) {
      return `第 ${index + 1} 条规则的路径不能为空`;
    }

    const exists = authWhitelistMatchTypeOptions.value.some((opt) => opt.value === type);
    if (!exists) {
      return `第 ${index + 1} 条规则的匹配类型无效`;
    }

    const key = `${type}::${pattern}`;
    if (used.has(key)) {
      return `第 ${index + 1} 条规则与其他规则重复`;
    }
    used.add(key);
  }
  return null;
}

function validateMsgTypeConfigItem(item: MsgTypeConfigModel): string | null {
  const msgType = normalizeMsgTypeValue(item.msgType);
  if (!msgType) {
    return null;
  }
  const route = item.route.trim();
  if (route && !route.startsWith("/")) {
    return `消息类型 ${resolveMsgTypeLabel(msgType)} 的路由必须以 / 开头`;
  }

  const priority = item.priority.trim().toUpperCase();
  if (priority !== "LOW" && priority !== "MEDIUM" && priority !== "HIGH") {
    return `消息类型 ${resolveMsgTypeLabel(msgType)} 的优先级无效`;
  }
  return null;
}

function resolveMsgTypeLabel(msgType: string): string {
  const found = msgTypeOptions.value.find((item) => item.value === msgType);
  if (found) {
    return found.label;
  }
  return msgType;
}

function resolveChannelLabel(config: MsgTypeConfigModel): string {
  const channels: string[] = [];
  if (config.sseEnabled) {
    channels.push("SSE");
  }
  if (config.webPushEnabled) {
    channels.push("WebPush");
  }
  if (config.panelAutoOpen) {
    channels.push("弹层");
  }
  if (config.osNotificationEnabled) {
    channels.push("系统通知");
  }
  if (channels.length === 0) {
    return "无";
  }
  return channels.join(" + ");
}

function showText(value: string | null): string {
  if (!value) return "-";
  const text = value.trim();
  return text || "-";
}

function formatEpoch(epochMillis: number | null | undefined): string {
  if (epochMillis === null || epochMillis === undefined) {
    return "-";
  }
  if (!Number.isFinite(epochMillis) || epochMillis <= 0) {
    return "-";
  }
  const date = new Date(epochMillis);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function pad(value: number): string {
  return String(value).padStart(2, "0");
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

.cell-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
  color: var(--text-muted);
}

.value-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dict-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 14px 20px 18px;
  color: #475569;
}

.dict-pagination-summary {
  font-size: 13px;
  color: #64748b;
}

.dict-pagination-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.dict-page-size__select {
  min-width: 92px;
  height: 32px;
  padding: 0 12px;
  color: #0f172a;
  background: #ffffff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  font-size: 13px;
}

.dict-page-list {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.dict-page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 10px;
  color: #334155;
  background: #ffffff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease;
}

.dict-page-btn:hover:not(:disabled) {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.dict-page-btn.is-active {
  color: #ffffff;
  background: #1677ff;
  border-color: #1677ff;
}

.dict-page-btn:disabled {
  opacity: 0.52;
  cursor: not-allowed;
}

.dict-page-ellipsis {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  color: #94a3b8;
}

.editor-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.meta-item {
  min-width: 0;
}

.meta-label {
  font-size: 12px;
  color: var(--text-muted);
}

.meta-value {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-main);
  word-break: break-all;
}

.editor-actions-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.assist-panel {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fbff;
  padding: 10px;
}

.assist-title {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.assist-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.assist-result {
  font-size: 12px;
  color: var(--text-muted);
}

.strong {
  color: #0f172a;
  font-weight: 700;
}

.preview-panel {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  padding: 10px 12px;
}

.preview-title {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 8px;
}

.preview-grid {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 8px 10px;
  margin-bottom: 8px;
}

.preview-label {
  font-size: 12px;
  color: var(--text-muted);
}

.preview-value {
  font-size: 13px;
  color: var(--text-main);
  overflow-wrap: anywhere;
}

.preview-empty {
  font-size: 12px;
  color: var(--text-muted);
}

.preview-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.list-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.list-item {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.list-item :deep(.bz-input) {
  flex: 1;
  min-width: 0;
}

.list-item :deep(.bz-icon-action-button) {
  flex: 0 0 auto;
}

.msg-config-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  overflow-x: auto;
}

.msg-config-head {
  display: flex;
  gap: 8px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
}

.msg-col-type {
  width: 160px;
  flex: 0 0 auto;
}

.msg-col-route {
  flex: 1 1 0;
  min-width: 240px;
}

.msg-col-priority {
  width: 120px;
  flex: 0 0 auto;
}

.msg-col-switch {
  width: 70px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.msg-col-action {
  width: 32px;
  flex: 0 0 auto;
  display: flex;
  justify-content: center;
}

.msg-config-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.msg-config-item {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
  min-width: 980px;
}

.msg-config-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.msg-config-foot-left,
.msg-config-foot-right {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.msg-preview-type {
  width: 220px;
}

.msg-config-preview {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  padding: 10px;
  display: grid;
  gap: 8px;
}

.preview-row {
  border-top: 1px dashed #dbe3ef;
  padding-top: 8px;
}

.preview-row:first-of-type {
  border-top: none;
  padding-top: 0;
}

.preview-row-title {
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
}

.preview-row-meta {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-muted);
}

.whitelist-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.whitelist-head {
  display: flex;
  gap: 8px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
}

.whitelist-col-type {
  width: 180px;
  flex: 0 0 auto;
}

.whitelist-col-pattern {
  flex: 1 1 0;
  min-width: 260px;
}

.whitelist-col-action {
  width: 32px;
  flex: 0 0 auto;
  display: flex;
  justify-content: center;
}

.whitelist-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.whitelist-item {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.whitelist-foot {
  display: flex;
  justify-content: flex-start;
}

.whitelist-preview {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  padding: 10px;
  display: grid;
  gap: 8px;
}

.form-error {
  margin-top: 8px;
  font-size: 12px;
  color: #dc2626;
}

@media (max-width: 900px) {
  .editor-meta {
    grid-template-columns: 1fr;
  }

  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
