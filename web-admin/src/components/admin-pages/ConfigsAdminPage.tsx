"use client";

import {
  listConfigs,
  previewClientIp,
  previewTimeOffset,
  updateConfigValue,
} from "@admin/api/configs";
import { AdminActionBar } from "@admin/components/admin/AdminActionBar";
import { AdminDetailTable, type AdminDetailSection } from "@admin/components/admin/AdminDetailTable";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { AdminListPageTemplate } from "@admin/components/admin/AdminListPageTemplate";
import { batchListDictOptions, listDictOptions } from "@admin/api/dicts";
import { previewMsgPush } from "@admin/api/sse";
import {
  resolveUserConfigLabel,
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
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type {
  ClientIpMode,
  ConfigClientIpPreviewRes,
  ConfigItem,
  ConfigTimeOffsetPreviewRes,
} from "@admin/types/config-admin";
import type { DictItem, DictOption } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { AdminTableTools } from "../admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "../admin/useAdminQueryPanelLayout";
import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzDatePicker } from "../bz/BzDatePicker";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzIconActionButton } from "../bz/BzIconActionButton";
import { BzInput } from "../bz/BzInput";
import { BzLoading } from "../bz/BzLoading";
import { BzOption } from "../bz/BzOption";
import { BzPagination } from "../bz/BzPagination";
import { BzSelect } from "../bz/BzSelect";
import { BzSwitch } from "../bz/BzSwitch";
import { BzTable, type BzTableColumn } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

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

function pad(value: number): string {
  return String(value).padStart(2, "0");
}

function formatEpoch(epochMillis: number | null | undefined): string {
  if (epochMillis === null || epochMillis === undefined) return "-";
  if (!Number.isFinite(epochMillis) || epochMillis <= 0) return "-";
  const date = new Date(epochMillis);
  if (Number.isNaN(date.getTime())) return "-";
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function showText(value: string | null): string {
  if (!value) return "-";
  const text = value.trim();
  return text || "-";
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
  for (const token of Object.keys(map)) {
    result = result.split(token).join(map[token]);
  }
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
  } catch {
    return String(value);
  }
}

const DEFAULT_WHITELIST_TYPE_OPTIONS: DictOption[] = [
  { label: "精确匹配", value: "exact" },
  { label: "Ant 路径匹配", value: "ant" },
  { label: "PathPattern 匹配", value: "pathPattern" },
];

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

function toStaticDictOptions(items: UserConfigOptionItem[]): DictOption[] {
  return items.map((item) => ({ label: item.value, value: item.code }));
}

const DATE_PREVIEW_BASE = new Date(2026, 2, 11, 15, 42, 9);
const DECIMAL_PREVIEW_NUMBERS = [1234567.891, -9876.5, 0.126];

export function ConfigsAdminPage() {
  const canUpdate = hasResourceCodeAccess("config-system-edit");
  const canPreviewPush = hasResourceCodeAccess("config-system-preview-push");

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [codeLikeDraft, setCodeLikeDraft] = useState("");
  const [descriptionLikeDraft, setDescriptionLikeDraft] = useState("");
  const [appliedCodeLike, setAppliedCodeLike] = useState("");
  const [appliedDescriptionLike, setAppliedDescriptionLike] = useState("");
  const [rows, setRows] = useState<ConfigItem[]>([]);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const pageSizeOptions = [10, 20, 30, 50, 100];
  const [page, setPage] = useState<PageResult<ConfigItem>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });

  const [configValueTypeLabelMap, setConfigValueTypeLabelMap] = useState<Record<string, string>>(
    {},
  );
  const [configLevelLabelMap, setConfigLevelLabelMap] = useState<Record<string, string>>({});
  const [clientIpModeOptions, setClientIpModeOptions] = useState<
    Array<{ value: ClientIpMode; label: string }>
  >([]);
  const [msgTypeOptions, setMsgTypeOptions] = useState<DictOption[]>([]);
  const [msgPriorityOptions, setMsgPriorityOptions] = useState<DictOption[]>([]);
  const [authWhitelistMatchTypeOptions, setAuthWhitelistMatchTypeOptions] = useState<DictOption[]>([
    ...DEFAULT_WHITELIST_TYPE_OPTIONS,
  ]);
  const [userTimeZoneOptions, setUserTimeZoneOptions] = useState<DictOption[]>([]);
  const [userDateTimeFormatOptions, setUserDateTimeFormatOptions] = useState<DictOption[]>([]);
  const [userDateFormatOptions, setUserDateFormatOptions] = useState<DictOption[]>([]);
  const [userDecimalFormatOptions, setUserDecimalFormatOptions] = useState<DictOption[]>([]);

  const [editorOpen, setEditorOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailItem, setDetailItem] = useState<ConfigItem | null>(null);
  const [editorItem, setEditorItem] = useState<ConfigItem | null>(null);
  const [editorRawValue, setEditorRawValue] = useState("");
  const [editorBoolValue, setEditorBoolValue] = useState<"true" | "false">("true");
  const [editorListValue, setEditorListValue] = useState<string[]>([]);
  const [editorMsgTypeConfigs, setEditorMsgTypeConfigs] = useState<MsgTypeConfigModel[]>([]);
  const [editorAuthWhitelistRules, setEditorAuthWhitelistRules] = useState<
    AuthWhitelistRuleModel[]
  >([]);
  const [editorIpMode, setEditorIpMode] = useState<ClientIpMode>("REMOTE_ADDR");
  const [editorIpPreview, setEditorIpPreview] = useState<ConfigClientIpPreviewRes | null>(null);
  const [editorTimePreview, setEditorTimePreview] = useState<ConfigTimeOffsetPreviewRes | null>(
    null,
  );
  const [editorOffsetSecondsInput, setEditorOffsetSecondsInput] = useState("0");
  const [editorTargetDateTime, setEditorTargetDateTime] = useState("");
  const [editorCalculatedOffsetSeconds, setEditorCalculatedOffsetSeconds] = useState<number | null>(
    null,
  );
  const [editorPreviewLoading, setEditorPreviewLoading] = useState(false);
  const [previewSending, setPreviewSending] = useState(false);
  const [previewMsgType, setPreviewMsgType] = useState("");
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const editorTitle = useMemo(() => {
    if (!editorItem) return "编辑配置";
    return `编辑配置 - ${editorItem.code}`;
  }, [editorItem]);

  const isClientIpEditor = editorItem?.code === "CLIENT_IP_MODE";
  const isTimeOffsetEditor = editorItem?.code === "TIME_OFFSET";
  const isMsgTypeConfigsEditor = editorItem?.code === "MSG_TYPE_CONFIGS";
  const isAuthWhitelistEditor = editorItem?.code === "AUTH_WHITELIST";
  const isUserTimeZoneEditor = editorItem?.code === "USER_TIME_ZONE";
  const isDateFormatEditor = editorItem?.code === "USER_DATE_FORMAT";
  const isDateTimeFormatEditor = editorItem?.code === "USER_DATE_TIME_FORMAT";
  const isDecimalFormatEditor = editorItem?.code === "USER_DECIMAL_FORMAT";
  const isBoolEditor = editorItem?.valueType === "BOOL";
  const isListEditor =
    !isMsgTypeConfigsEditor &&
    !isAuthWhitelistEditor &&
    (editorItem?.valueType === "STR_LIST" || editorItem?.valueType === "STR_SET");

  const isNumberType = (item: ConfigItem | null): boolean => {
    if (!item) return false;
    return item.valueType === "INT" || item.valueType === "LONG" || item.valueType === "DEC";
  };

  const currentUserFormatOptions = useMemo(() => {
    if (isUserTimeZoneEditor) return userTimeZoneOptions;
    if (isDateTimeFormatEditor) return userDateTimeFormatOptions;
    if (isDateFormatEditor) return userDateFormatOptions;
    if (isDecimalFormatEditor) return userDecimalFormatOptions;
    return [] as DictOption[];
  }, [
    isUserTimeZoneEditor,
    isDateTimeFormatEditor,
    isDateFormatEditor,
    isDecimalFormatEditor,
    userTimeZoneOptions,
    userDateTimeFormatOptions,
    userDateFormatOptions,
    userDecimalFormatOptions,
  ]);

  const editorDialogWidth = useMemo(() => {
    if (isMsgTypeConfigsEditor) return "1180px";
    if (isAuthWhitelistEditor) return "960px";
    return "760px";
  }, [isMsgTypeConfigsEditor, isAuthWhitelistEditor]);

  const editorDialogMaxWidth = useMemo(() => {
    if (isMsgTypeConfigsEditor) return "96vw";
    if (isAuthWhitelistEditor) return "96vw";
    return "min(92vw, 760px)";
  }, [isMsgTypeConfigsEditor, isAuthWhitelistEditor]);

  const totalPages = useMemo(() => Math.max(1, page.totalPages || 1), [page.totalPages]);

  const previewSendOptions = useMemo(() => {
    return editorMsgTypeConfigs
      .map((item) => normalizeMsgTypeValue(item.msgType))
      .filter((item, index, arr) => item.length > 0 && arr.indexOf(item) === index)
      .map((msgType) => ({
        label: resolveMsgTypeLabel(msgType),
        value: msgType,
      }));
  }, [editorMsgTypeConfigs]);

  const canPreviewSend = useMemo(() => {
    if (!canUpdate || !isMsgTypeConfigsEditor || previewSending) return false;
    if (!canPreviewPush) return false;
    if (!previewMsgType) return false;
    return editorMsgTypeConfigs.some(
      (item) => normalizeMsgTypeValue(item.msgType) === previewMsgType,
    );
  }, [
    canUpdate,
    isMsgTypeConfigsEditor,
    previewSending,
    canPreviewPush,
    previewMsgType,
    editorMsgTypeConfigs,
  ]);

  const msgConfigPreviewRows = useMemo(() => {
    return editorMsgTypeConfigs
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
  }, [editorMsgTypeConfigs]);

  const authWhitelistPreviewRows = useMemo(() => {
    return editorAuthWhitelistRules
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
  }, [editorAuthWhitelistRules]);

  const dateFormatPreviewValue = useMemo(() => {
    if (isDateFormatEditor || isDateTimeFormatEditor) {
      const err = validateCurrentEditor();
      if (err) return "-";
    }
    const pattern = isDateTimeFormatEditor
      ? resolveUserDateTimeFormatCode(editorRawValue.trim())
      : resolveUserDateFormatCode(editorRawValue.trim());
    if (!pattern) return "-";
    return applyDatePattern(DATE_PREVIEW_BASE, pattern);
  }, [isDateFormatEditor, isDateTimeFormatEditor, editorRawValue]);

  const decimalPreviewRows = useMemo(() => {
    const pattern = resolveUserDecimalFormatCode(editorRawValue.trim());
    const error =
      isDecimalFormatEditor &&
      !currentUserFormatOptions.some((item) => item.value === editorRawValue.trim())
        ? "小数格式选项无效"
        : null;
    return DECIMAL_PREVIEW_NUMBERS.map((num) => ({
      source: num,
      formatted: error ? "-" : formatDecimalByPattern(num, pattern),
    }));
  }, [isDecimalFormatEditor, editorRawValue, currentUserFormatOptions]);

  useEffect(() => {
    if (previewSendOptions.length === 0) {
      setPreviewMsgType("");
      return;
    }
    if (!previewSendOptions.some((item) => item.value === previewMsgType)) {
      setPreviewMsgType(previewSendOptions[0].value);
    }
  }, [previewSendOptions]);

  function normalizeMsgTypeValue(raw: unknown): string {
    return String(raw ?? "").trim();
  }

  function resolveMsgTypeLabel(msgType: string): string {
    const found = msgTypeOptions.find((item) => item.value === msgType);
    if (found) return found.label;
    return msgType;
  }

  function resolveChannelLabel(config: MsgTypeConfigModel): string {
    const channels: string[] = [];
    if (config.sseEnabled) channels.push("SSE");
    if (config.webPushEnabled) channels.push("WebPush");
    if (config.panelAutoOpen) channels.push("弹层");
    if (config.osNotificationEnabled) channels.push("系统通知");
    if (channels.length === 0) return "无";
    return channels.join(" + ");
  }

  function normalizeBool(raw: string): "true" | "false" {
    const value = String(raw ?? "")
      .trim()
      .toLowerCase();
    return value === "false" ? "false" : "true";
  }

  function parseStringList(raw: string): string[] {
    try {
      const parsed = JSON.parse(raw || "[]");
      if (!Array.isArray(parsed)) return [];
      return parsed.map((item) => String(item ?? "")).filter((item) => item.length > 0);
    } catch {
      return [];
    }
  }

  function parseMsgTypeConfigs(raw: string): MsgTypeConfigModel[] {
    try {
      const parsed: unknown = JSON.parse(raw || "[]");
      if (!Array.isArray(parsed)) return [];
      return parsed.map((item) => normalizeMsgTypeConfig(item));
    } catch {
      return [];
    }
  }

  function normalizeMsgTypeConfig(item: unknown): MsgTypeConfigModel {
    const raw = item && typeof item === "object" ? (item as Record<string, unknown>) : {};
    const priority = String(raw.priority ?? "MEDIUM")
      .trim()
      .toUpperCase();
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
      if (!Array.isArray(parsed)) return [];
      return parsed.map((item) => normalizeAuthWhitelistRule(item));
    } catch {
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
    if (!value) return defaultWhitelistMatchType();
    const matched = authWhitelistMatchTypeOptions.find(
      (item) => item.value.toLowerCase() === value.toLowerCase(),
    );
    if (matched) return matched.value;
    if (value.toUpperCase() === "EXACT") return "exact";
    if (value.toUpperCase() === "ANT") return "ant";
    if (value.toUpperCase() === "PATH_PATTERN") return "pathPattern";
    return defaultWhitelistMatchType();
  }

  function defaultWhitelistMatchType(): string {
    return authWhitelistMatchTypeOptions[0]?.value || "exact";
  }

  function resolveWhitelistMatchTypeLabel(type: string): string {
    const matched = authWhitelistMatchTypeOptions.find((item) => item.value === type);
    if (matched) return matched.label;
    return type;
  }

  function normalizeOffsetSeconds(raw: string): string {
    const text = String(raw ?? "").trim();
    if (/^-?\d+$/.test(text)) return text;
    return "0";
  }

  function normalizeClientIpMode(raw: string): ClientIpMode {
    const value = String(raw ?? "").trim();
    const matched = clientIpModeOptions.find((item) => item.value === value);
    return matched?.value || "REMOTE_ADDR";
  }

  function parseOffsetSecondsInput(): number | null {
    const raw = editorOffsetSecondsInput.trim();
    if (!/^-?\d+$/.test(raw)) return null;
    const num = Number(raw);
    if (!Number.isSafeInteger(num)) return null;
    return num;
  }

  function parseTargetDateTime(value: string): number | null {
    const input = value.trim();
    const match = /^(\d{4})-(\d{2})-(\d{2})\s(\d{2}):(\d{2})$/.exec(input);
    if (!match) return null;
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
    if (!valid) return null;
    return date.getTime();
  }

  function validateCurrentEditor(): string | null {
    if (!editorOpen || !editorItem) return null;

    if (isMsgTypeConfigsEditor) return validateMsgTypeConfigs();
    if (isAuthWhitelistEditor) return validateAuthWhitelistRules();
    if (isTimeOffsetEditor) {
      if (parseOffsetSecondsInput() === null) return "偏移秒数必须是整数";
      return null;
    }
    if (isDateFormatEditor)
      return currentUserFormatOptions.some((item) => item.value === editorRawValue.trim())
        ? null
        : "日期格式选项无效";
    if (isUserTimeZoneEditor)
      return currentUserFormatOptions.some((item) => item.value === editorRawValue.trim())
        ? null
        : "时区选项无效";
    if (isDateTimeFormatEditor)
      return currentUserFormatOptions.some((item) => item.value === editorRawValue.trim())
        ? null
        : "日期时间格式选项无效";
    if (isDecimalFormatEditor)
      return currentUserFormatOptions.some((item) => item.value === editorRawValue.trim())
        ? null
        : "小数格式选项无效";
    return null;
  }

  function validateMsgTypeConfigs(): string | null {
    const used = new Set<string>();
    for (const item of editorMsgTypeConfigs) {
      const msgType = normalizeMsgTypeValue(item.msgType);
      if (!msgType) continue;
      if (used.has(msgType)) return `消息类型 ${resolveMsgTypeLabel(msgType)} 重复，请删除重复配置`;
      used.add(msgType);
      const rowError = validateMsgTypeConfigItem(item);
      if (rowError) return rowError;
    }
    return null;
  }

  function validateAuthWhitelistRules(): string | null {
    const used = new Set<string>();
    for (let index = 0; index < editorAuthWhitelistRules.length; index += 1) {
      const item = editorAuthWhitelistRules[index];
      const type = normalizeWhitelistMatchType(item.type);
      const pattern = item.pattern.trim();
      if (!pattern) return `第 ${index + 1} 条规则的路径不能为空`;
      const exists = authWhitelistMatchTypeOptions.some((opt) => opt.value === type);
      if (!exists) return `第 ${index + 1} 条规则的匹配类型无效`;
      const key = `${type}::${pattern}`;
      if (used.has(key)) return `第 ${index + 1} 条规则与其他规则重复`;
      used.add(key);
    }
    return null;
  }

  function validateMsgTypeConfigItem(item: MsgTypeConfigModel): string | null {
    const msgType = normalizeMsgTypeValue(item.msgType);
    if (!msgType) return null;
    const route = item.route.trim();
    if (route && !route.startsWith("/"))
      return `消息类型 ${resolveMsgTypeLabel(msgType)} 的路由必须以 / 开头`;
    const priority = item.priority.trim().toUpperCase();
    if (priority !== "LOW" && priority !== "MEDIUM" && priority !== "HIGH")
      return `消息类型 ${resolveMsgTypeLabel(msgType)} 的优先级无效`;
    return null;
  }

  const editorValidationErrorComputed = validateCurrentEditor();
  const canSaveEditor = canUpdate && !saving && !editorValidationErrorComputed;

  const loadConfigDictionaries = useCallback(async () => {
    try {
      const result = await batchListDictOptions([
        "CONFIG_VALUE_TYPE",
        "CONFIG_LEVEL",
        "USER_TIME_ZONE",
        "USER_DATE_TIME_FORMAT",
        "USER_DATE_FORMAT",
        "USER_DECIMAL_FORMAT",
      ]);
      setConfigValueTypeLabelMap(toDictLabelMap(result.CONFIG_VALUE_TYPE));
      setConfigLevelLabelMap(toDictLabelMap(result.CONFIG_LEVEL));
      setUserTimeZoneOptions(toDictOptions(result.USER_TIME_ZONE));
      setUserDateTimeFormatOptions(toDictOptions(result.USER_DATE_TIME_FORMAT));
      setUserDateFormatOptions(toDictOptions(result.USER_DATE_FORMAT));
      setUserDecimalFormatOptions(toDictOptions(result.USER_DECIMAL_FORMAT));
    } catch {
      setConfigValueTypeLabelMap({});
      setConfigLevelLabelMap({});
      setUserTimeZoneOptions(toStaticDictOptions(USER_TIME_ZONE_OPTIONS));
      setUserDateTimeFormatOptions(toStaticDictOptions(USER_DATE_TIME_FORMAT_OPTIONS));
      setUserDateFormatOptions(toStaticDictOptions(USER_DATE_FORMAT_OPTIONS));
      setUserDecimalFormatOptions(toStaticDictOptions(USER_DECIMAL_FORMAT_OPTIONS));
    }
  }, []);

  const loadClientIpModeOptions = useCallback(async () => {
    try {
      const items = await listDictOptions("CLIENT_IP_MODE");
      setClientIpModeOptions(
        items.map((item) => ({
          value: item.itemValue as ClientIpMode,
          label: item.itemLabel,
        })),
      );
    } catch (error) {
      if (error instanceof Error && error.message.trim().length > 0) {
        message.error(error.message);
        return;
      }
      message.error("客户端 IP 模式候选项加载失败");
    }
  }, []);

  const loadMsgConfigOptions = useCallback(async () => {
    try {
      const types = await listDictOptions("MSG_TYPE");
      const priorities = await listDictOptions("MSG_PRIORITY");
      setMsgTypeOptions(types.map((t) => ({ label: t.itemLabel, value: t.itemValue })));
      setMsgPriorityOptions(priorities.map((p) => ({ label: p.itemLabel, value: p.itemValue })));
    } catch {
      message.error("消息配置字典项加载失败");
    }
  }, []);

  const loadAuthWhitelistMatchTypeOptions = useCallback(async () => {
    try {
      const items = await listDictOptions("AUTH_WHITELIST_MATCH_TYPE");
      const opts = items.map((item) => ({
        label: item.itemLabel,
        value: item.itemValue,
      }));
      if (opts.length === 0) {
        setAuthWhitelistMatchTypeOptions(DEFAULT_WHITELIST_TYPE_OPTIONS.map((o) => ({ ...o })));
      } else {
        setAuthWhitelistMatchTypeOptions(opts);
      }
    } catch {
      setAuthWhitelistMatchTypeOptions(DEFAULT_WHITELIST_TYPE_OPTIONS.map((o) => ({ ...o })));
      message.error("白名单匹配类型候选项加载失败，已使用默认选项");
    }
  }, []);

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      const requestedPageNo = pageNo;
      const p = await listConfigs({
        codeLike: appliedCodeLike,
        descriptionLike: appliedDescriptionLike,
        pageNo: requestedPageNo,
        pageSize,
      });
      if (clientIpModeOptions.length === 0) {
        loadClientIpModeOptions();
      }
      if (msgTypeOptions.length === 0) {
        loadMsgConfigOptions();
      }
      if (authWhitelistMatchTypeOptions.length === 0) {
        loadAuthWhitelistMatchTypeOptions();
      }
      setPage(p);
      const newPageSize = p.pageSize || pageSize;
      setPageSize(newPageSize);

      if (p.totalElements > 0 && requestedPageNo > Math.max(1, p.totalPages)) {
        const fallbackPageNo = Math.max(1, p.totalPages);
        setPageNo(fallbackPageNo);
        const fallback = await listConfigs({
          codeLike: appliedCodeLike,
          descriptionLike: appliedDescriptionLike,
          pageNo: fallbackPageNo,
          pageSize: newPageSize,
        });
        setPage(fallback);
        setPageNo(fallback.pageNo || 1);
        setRows(fallback.elements);
      } else {
        setPageNo(p.pageNo || 1);
        setRows(p.elements);
      }
    } finally {
      setLoading(false);
    }
  }, [
    pageNo,
    pageSize,
    appliedCodeLike,
    appliedDescriptionLike,
    clientIpModeOptions.length,
    msgTypeOptions.length,
    authWhitelistMatchTypeOptions.length,
    loadClientIpModeOptions,
    loadMsgConfigOptions,
    loadAuthWhitelistMatchTypeOptions,
  ]);

  useEffect(() => {
    loadConfigDictionaries();
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  function applyFilters() {
    setAppliedCodeLike(codeLikeDraft.trim());
    setAppliedDescriptionLike(descriptionLikeDraft.trim());
    setPageNo(1);
  }

  function resetFilters() {
    setCodeLikeDraft("");
    setDescriptionLikeDraft("");
    setAppliedCodeLike("");
    setAppliedDescriptionLike("");
    setPageNo(1);
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
    if (configLabel) return configLabel;
    return value || "-";
  }

  async function runClientIpPreview() {
    if (!isClientIpEditor || !editorOpen) return;
    setEditorPreviewLoading(true);
    try {
      setEditorIpPreview(await previewClientIp(editorIpMode));
    } finally {
      setEditorPreviewLoading(false);
    }
  }

  async function refreshTimeOffsetPreview() {
    if (!isTimeOffsetEditor || !editorOpen) return;
    const offsetSeconds = parseOffsetSecondsInput();
    if (offsetSeconds === null) {
      message.error("偏移秒数必须是整数");
      return;
    }
    setEditorPreviewLoading(true);
    try {
      setEditorTimePreview(await previewTimeOffset({ offsetSeconds }));
    } finally {
      setEditorPreviewLoading(false);
    }
  }

  async function calculateOffsetByTarget() {
    if (!isTimeOffsetEditor || !editorOpen) return;
    const offsetSeconds = parseOffsetSecondsInput();
    if (offsetSeconds === null) {
      message.error("偏移秒数必须是整数");
      return;
    }
    const targetEpochMillis = parseTargetDateTime(editorTargetDateTime);
    if (targetEpochMillis === null) {
      message.error("请先选择有效的目标时间");
      return;
    }
    setEditorPreviewLoading(true);
    try {
      const preview = await previewTimeOffset({
        offsetSeconds,
        targetEpochMillis,
      });
      setEditorTimePreview(preview);
      setEditorCalculatedOffsetSeconds(preview.calculatedOffsetSeconds);
    } finally {
      setEditorPreviewLoading(false);
    }
  }

  async function applyCalculatedOffset() {
    if (editorCalculatedOffsetSeconds === null) return;
    setEditorOffsetSecondsInput(String(editorCalculatedOffsetSeconds));
    setTimeout(() => refreshTimeOffsetPreview(), 0);
  }

  function resetOffsetSeconds() {
    setEditorOffsetSecondsInput("0");
    setTimeout(() => refreshTimeOffsetPreview(), 0);
  }

  function addMsgConfigItem() {
    setEditorMsgTypeConfigs((prev) => [
      ...prev,
      {
        msgType: "",
        route: "",
        priority: "MEDIUM",
        sseEnabled: true,
        webPushEnabled: false,
        panelAutoOpen: true,
        osNotificationEnabled: false,
      },
    ]);
  }

  function removeMsgConfigItem(index: number) {
    setEditorMsgTypeConfigs((prev) => prev.filter((_, i) => i !== index));
  }

  function addAuthWhitelistRule() {
    setEditorAuthWhitelistRules((prev) => [
      ...prev,
      { type: defaultWhitelistMatchType(), pattern: "" },
    ]);
  }

  function removeAuthWhitelistRule(index: number) {
    setEditorAuthWhitelistRules((prev) => prev.filter((_, i) => i !== index));
  }

  function addListItem() {
    setEditorListValue((prev) => [...prev, ""]);
  }

  function removeListItem(index: number) {
    setEditorListValue((prev) => prev.filter((_, i) => i !== index));
  }

  function getMsgTypeOptionsFor(
    index: number,
  ): Array<{ label: string; value: string; disabled: boolean }> {
    const currentValue = editorMsgTypeConfigs[index]?.msgType?.trim() ?? "";
    const usedByOthers = new Set(
      editorMsgTypeConfigs
        .map((item, itemIndex) => (itemIndex === index ? "" : normalizeMsgTypeValue(item.msgType)))
        .filter((item) => item.length > 0),
    );
    const options = msgTypeOptions.map((opt) => ({
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
    if (!canPreviewSend) return;
    const target = editorMsgTypeConfigs.find(
      (item) => normalizeMsgTypeValue(item.msgType) === previewMsgType,
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
    setPreviewSending(true);
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
      setPreviewSending(false);
    }
  }

  function openEditor(item: ConfigItem) {
    setEditorOpen(true);
    setEditorItem(item);
    setEditorRawValue(item.value ?? "");
    setEditorBoolValue(normalizeBool(item.value));
    setEditorListValue(parseStringList(item.value));
    setEditorMsgTypeConfigs(parseMsgTypeConfigs(item.value));
    setEditorAuthWhitelistRules(parseAuthWhitelistRules(item.value));
    setEditorIpMode(normalizeClientIpMode(item.value));
    setEditorIpPreview(null);
    setEditorTimePreview(null);
    setEditorOffsetSecondsInput(normalizeOffsetSeconds(item.value));
    setEditorTargetDateTime("");
    setEditorCalculatedOffsetSeconds(null);
    setEditorPreviewLoading(false);

    if (item.code === "CLIENT_IP_MODE") {
      setTimeout(() => runClientIpPreview(), 0);
    }
    if (item.code === "TIME_OFFSET") {
      setTimeout(() => refreshTimeOffsetPreview(), 0);
    }
  }

  function closeEditor() {
    if (saving) return;
    setEditorOpen(false);
    setEditorItem(null);
    setPreviewMsgType("");
  }

  function buildSaveValue(item: ConfigItem): string | null {
    if (item.code === "CLIENT_IP_MODE") return editorIpMode;

    if (item.code === "TIME_OFFSET") {
      const offsetSeconds = parseOffsetSecondsInput();
      if (offsetSeconds === null) {
        message.error("偏移秒数必须是整数");
        return null;
      }
      return String(offsetSeconds);
    }

    if (item.code === "MSG_TYPE_CONFIGS") {
      const validConfigs = editorMsgTypeConfigs.filter(
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
        editorAuthWhitelistRules.map((rule) => ({
          type: normalizeWhitelistMatchType(rule.type),
          pattern: rule.pattern.trim(),
        })),
      );
    }

    if (item.valueType === "BOOL") return editorBoolValue;

    if (item.valueType === "STR_LIST" || item.valueType === "STR_SET") {
      const list = editorListValue.map((v) => v.trim()).filter((v) => v.length > 0);
      return JSON.stringify(list);
    }

    return editorRawValue;
  }

  async function saveCurrentConfig() {
    if (!canUpdate || !editorItem) return;
    const validationError = editorValidationErrorComputed;
    if (validationError) {
      message.error(validationError);
      return;
    }
    const saveValue = buildSaveValue(editorItem);
    if (saveValue === null) return;

    let saved = false;
    setSaving(true);
    try {
      await updateConfigValue(editorItem.code, saveValue);
      message.success("保存成功");
      saved = true;
    } finally {
      setSaving(false);
      if (saved) closeEditor();
    }
  }

  const columns = useMemo<BzTableColumn<ConfigItem>[]>(
    () => [
      {
        key: "code",
        title: "配置键",
        width: 280,
        className: "admin-freeze-col--config-code is-sticky-left",
        headerClassName: "admin-freeze-col--config-code is-sticky-left",
        render: (row) => (
          <div className="configs-code-cell">
            <span className="configs-code-text">
              {row.code}
            </span>
            {row.personalized ? (
              <BzTag
                size="small"
                type="info"
              >
                个性化
              </BzTag>
            ) : null}
          </div>
        ),
      },
      {
        key: "description",
        title: "说明",
        width: 240,
        render: (row) => row.description || "-",
      },
      {
        key: "value",
        title: "当前值",
        minWidth: 320,
        render: (row) => (
          <div
            title={renderValue(row)}
            className="configs-value-text"
          >
            {renderValue(row)}
          </div>
        ),
      },
      {
        key: "valueType",
        title: "类型",
        width: 110,
        render: (row) => (
          <BzTag size="small">{configValueTypeLabelMap[row.valueType] || row.valueType}</BzTag>
        ),
      },
      {
        key: "level",
        title: "级别",
        width: 110,
        render: (row) => (
          <BzTag
            size="small"
            type={row.level === "USER" ? "warning" : "info"}
          >
            {configLevelLabelMap[row.level] || row.level}
          </BzTag>
        ),
      },
      {
        key: "actions",
        title: "操作",
        width: 120,
        className: "is-fixed-right",
        headerClassName: "is-fixed-right",
        render: (row) => <AdminActionBar actions={getRowActions(row)} />,
      },
    ],
    [canUpdate, configValueTypeLabelMap, configLevelLabelMap],
  );

  const detailSections = useMemo<AdminDetailSection[]>(() => {
    if (!detailItem) return [];
    return [
      {
        title: "配置详情",
        fields: [
          { label: "配置键", value: detailItem.code },
          { label: "作用域", value: detailItem.scope || "-" },
          {
            label: "值类型",
            value: configValueTypeLabelMap[detailItem.valueType] || detailItem.valueType,
          },
          {
            label: "级别",
            value: configLevelLabelMap[detailItem.level] || detailItem.level,
          },
          {
            label: "个性化",
            value: detailItem.personalized ? "是" : "否",
          },
          { label: "说明", value: detailItem.description || "-", span: "full", multiline: true },
          { label: "当前值", value: renderValue(detailItem), span: "full", multiline: true },
          { label: "原始值", value: detailItem.value || "-", span: "full", multiline: true },
        ],
      },
    ];
  }, [configLevelLabelMap, configValueTypeLabelMap, detailItem]);

  const editorMetaSections = useMemo<AdminDetailSection[]>(() => {
    if (!editorItem) return [];
    return [
      {
        title: "基础信息",
        fields: [
          { label: "配置键", value: <span className="configs-meta-code">{editorItem.code}</span> },
          { label: "值类型", value: configValueTypeLabelMap[editorItem.valueType] || editorItem.valueType },
          { label: "级别", value: configLevelLabelMap[editorItem.level] || editorItem.level },
          { label: "个性化", value: editorItem.personalized ? "是" : "否" },
          { label: "说明", value: editorItem.description || "-", span: "full", multiline: true },
        ],
      },
    ];
  }, [configLevelLabelMap, configValueTypeLabelMap, editorItem]);

  return (
    <>
      <AdminListPageTemplate
        queryPanelVisible={queryPanelVisible}
        queryPanel={
          <div
            ref={queryCardRef}
            className={[
              "admin-query-layout",
              querySingleRow ? "is-single-row" : queryExpanded ? "is-expanded" : "is-collapsed",
            ].join(" ")}
          >
          <form
            ref={queryGridRef}
            className="bz-form admin-query-grid"
            onSubmit={(e) => {
              e.preventDefault();
              applyFilters();
            }}
          >
            <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">配置键</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={codeLikeDraft}
                        placeholder="请输入配置键"
                        clearable
                        onValueChange={setCodeLikeDraft}
                        onKeyUp={(e) => {
                          if (e.key === "Enter") applyFilters();
                        }}
                      />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">说明</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={descriptionLikeDraft}
                        placeholder="请输入说明"
                        clearable
                        onValueChange={setDescriptionLikeDraft}
                        onKeyUp={(e) => {
                          if (e.key === "Enter") applyFilters();
                        }}
                      />
                    </div>
                  </BzFormItem>
                  <div className="admin-query-actions">
                    <BzButton
                      className="admin-filter-secondary"
                      nativeType="button"
                      onClick={resetFilters}
                    >
                      重置
                    </BzButton>
                    <BzButton
                      className="admin-filter-primary"
                      buttonType="primary"
                      nativeType="button"
                      onClick={applyFilters}
                    >
                      搜索
                    </BzButton>
                    {!querySingleRow ? (
                      <button
                        className="admin-filter-toggle"
                        type="button"
                        aria-expanded={queryExpanded}
                        onClick={() => setQueryExpanded((value) => !value)}
                      >
                        <span>{queryExpanded ? "收起" : "展开"}</span>
                        <i
                          className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`}
                          aria-hidden="true"
                        />
                      </button>
                    ) : null}
                  </div>
            </form>
          </div>
        }
        queryTools={<AdminTableTools queryPanelVisible={queryPanelVisible} onToggleQueryPanel={() => setQueryPanelVisible((v) => !v)} onRefresh={() => reload()} />}
        table={<BzTable data={rows} columns={columns} rowKey="code" loading={loading} emptyText="暂无配置" size="small" />}
        footer={page.totalElements > 0 ? <div className="dict-pagination-bar admin-list-table-footer"><div className="dict-pagination-summary">共 {page.totalElements} 条记录</div><div className="dict-pagination-right"><BzPagination total={page.totalElements} pageSize={pageSize} currentPage={pageNo} pageSizes={pageSizeOptions} onCurrentChange={setPageNo} onSizeChange={(size) => { if (!Number.isFinite(size) || size <= 0 || size === pageSize) return; setPageSize(size); setPageNo(1); }} /></div></div> : null}
      />

      <AdminEntityDrawer
        open={detailOpen}
        title="配置详情"
        width="960px"
        onClose={() => {
          setDetailOpen(false);
          setDetailItem(null);
        }}
        footer={<BzButton onClick={() => {
          setDetailOpen(false);
          setDetailItem(null);
        }}>关闭</BzButton>}
      >
        {detailItem ? <AdminDetailTable sections={detailSections} /> : null}
      </AdminEntityDrawer>

      {editorOpen ? (
        <AdminEntityDrawer
          open={editorOpen}
          title={editorTitle}
          width={editorDialogWidth}
          loading={saving}
          onClose={closeEditor}
          footer={
            <>
              <BzButton onClick={closeEditor}>取消</BzButton>
              {canSaveEditor ? (
                <BzButton buttonType="primary" loading={saving} onClick={saveCurrentConfig}>
                  保存
                </BzButton>
              ) : null}
            </>
          }
        >
          <div className="admin-page-stack">
            <AdminDetailTable sections={editorMetaSections} />
            <section className="admin-selection-section configs-editor-section">
              <div className="admin-selection-section__header">
                <div className="admin-selection-section__title">编辑内容</div>
              </div>
              <BzForm>
            {isClientIpEditor ? (
              <>
                <BzFormItem label="IP 获取方式">
                  <BzSelect
                    modelValue={editorIpMode}
                    onValueChange={(v) => setEditorIpMode((v as ClientIpMode) || "REMOTE_ADDR")}
                  >
                    {clientIpModeOptions.map((opt) => (
                      <BzOption
                        key={opt.value}
                        label={opt.label}
                        value={opt.value}
                      />
                    ))}
                  </BzSelect>
                </BzFormItem>

                <div className="editor-actions-row">
                  <BzButton
                    disabled={editorPreviewLoading}
                    onClick={runClientIpPreview}
                  >
                    {editorPreviewLoading ? "测试中..." : "测试当前请求"}
                  </BzButton>
                </div>

                <BzLoading loading={editorPreviewLoading}>
                  <div className="preview-panel">
                    <div className="preview-title">效果预览</div>
                    {editorIpPreview ? (
                      <div className="preview-grid">
                        <div className="preview-label">解析后客户端 IP</div>
                        <div className="preview-value strong">
                          {showText(editorIpPreview.resolvedIp)}
                        </div>
                        <div className="preview-label">REMOTE_ADDR</div>
                        <div className="preview-value">{showText(editorIpPreview.remoteAddr)}</div>
                        <div className="preview-label">X-Real-IP</div>
                        <div className="preview-value">{showText(editorIpPreview.xRealIp)}</div>
                        <div className="preview-label">X-Forwarded-For</div>
                        <div className="preview-value">
                          {showText(editorIpPreview.xForwardedFor)}
                        </div>
                        <div className="preview-label">CF-Connecting-IP</div>
                        <div className="preview-value">
                          {showText(editorIpPreview.cfConnectingIp)}
                        </div>
                        <div className="preview-label">True-Client-IP</div>
                        <div className="preview-value">
                          {showText(editorIpPreview.trueClientIp)}
                        </div>
                      </div>
                    ) : (
                      <div className="preview-empty">
                        点击"测试当前请求"查看当前请求下的解析结果。
                      </div>
                    )}
                  </div>
                </BzLoading>
              </>
            ) : isTimeOffsetEditor ? (
              <>
                <BzFormItem label="偏移秒数（最终保存值）">
                  <BzInput
                    modelValue={editorOffsetSecondsInput}
                    type="number"
                    placeholder="例如：0、60、-300"
                    onValueChange={setEditorOffsetSecondsInput}
                  />
                </BzFormItem>

                <div className="editor-actions-row">
                  <BzButton
                    disabled={editorPreviewLoading}
                    onClick={refreshTimeOffsetPreview}
                  >
                    {editorPreviewLoading ? "刷新中..." : "刷新预览"}
                  </BzButton>
                  <BzButton onClick={resetOffsetSeconds}>重置为 0</BzButton>
                </div>

                <div className="assist-panel">
                  <div className="assist-title">辅助计算（不会自动保存）</div>
                  <div className="assist-row">
                    <BzDatePicker
                      modelValue={editorTargetDateTime}
                      type="datetime"
                      placeholder="选择目标时间"
                      onValueChange={setEditorTargetDateTime}
                    />
                  </div>
                  <div className="assist-row">
                    <BzButton
                      disabled={editorPreviewLoading}
                      onClick={calculateOffsetByTarget}
                    >
                      计算秒差
                    </BzButton>
                    <BzButton
                      disabled={editorCalculatedOffsetSeconds === null}
                      onClick={applyCalculatedOffset}
                    >
                      回填秒数
                    </BzButton>
                    {editorCalculatedOffsetSeconds !== null ? (
                      <div className="assist-result">
                        建议秒差：
                        <span className="strong">{editorCalculatedOffsetSeconds}</span>
                      </div>
                    ) : null}
                  </div>
                </div>

                <BzLoading loading={editorPreviewLoading}>
                  <div className="preview-panel">
                    <div className="preview-title">效果预览（基于服务器时间）</div>
                    {editorTimePreview ? (
                      <div className="preview-grid">
                        <div className="preview-label">服务器当前时间</div>
                        <div className="preview-value">
                          {formatEpoch(editorTimePreview.serverNowEpochMillis)}
                        </div>
                        <div className="preview-label">按当前秒数偏移后</div>
                        <div className="preview-value strong">
                          {formatEpoch(editorTimePreview.mockedEpochMillis)}
                        </div>
                        <div className="preview-label">当前秒数</div>
                        <div className="preview-value">{editorTimePreview.offsetSeconds}</div>
                        {editorTimePreview.calculatedOffsetSeconds !== null ? (
                          <>
                            <div className="preview-label">目标时间</div>
                            <div className="preview-value">
                              {formatEpoch(editorTimePreview.targetEpochMillis)}
                            </div>
                            <div className="preview-label">计算出的秒差</div>
                            <div className="preview-value strong">
                              {editorTimePreview.calculatedOffsetSeconds}
                            </div>
                          </>
                        ) : null}
                      </div>
                    ) : (
                      <div className="preview-empty">输入偏移秒数后可点击"刷新预览"查看效果。</div>
                    )}
                  </div>
                </BzLoading>
              </>
            ) : isUserTimeZoneEditor ? (
              <BzFormItem label="时区">
                <BzSelect
                  modelValue={editorRawValue}
                  onValueChange={(v) => setEditorRawValue(v ?? "")}
                >
                  {currentUserFormatOptions.map((opt) => (
                    <BzOption
                      key={opt.value}
                      label={opt.label}
                      value={opt.value}
                    />
                  ))}
                </BzSelect>
              </BzFormItem>
            ) : isDateFormatEditor || isDateTimeFormatEditor ? (
              <>
                <BzFormItem label={isDateTimeFormatEditor ? "日期时间格式" : "日期格式"}>
                  <BzSelect
                    modelValue={editorRawValue}
                    onValueChange={(v) => setEditorRawValue(v ?? "")}
                  >
                    {currentUserFormatOptions.map((opt) => (
                      <BzOption
                        key={opt.value}
                        label={opt.label}
                        value={opt.value}
                      />
                    ))}
                  </BzSelect>
                </BzFormItem>
                <div className="preview-panel">
                  <div className="preview-title">格式样例</div>
                  <div className="preview-grid">
                    <div className="preview-label">示例时间</div>
                    <div className="preview-value">{formatEpoch(DATE_PREVIEW_BASE.getTime())}</div>
                    <div className="preview-label">格式化结果</div>
                    <div className="preview-value strong">{dateFormatPreviewValue}</div>
                  </div>
                  <div className="preview-tip">按统一编码存储，前端按选项语义渲染。</div>
                </div>
              </>
            ) : isDecimalFormatEditor ? (
              <>
                <BzFormItem label="小数格式">
                  <BzSelect
                    modelValue={editorRawValue}
                    onValueChange={(v) => setEditorRawValue(v ?? "")}
                  >
                    {currentUserFormatOptions.map((opt) => (
                      <BzOption
                        key={opt.value}
                        label={opt.label}
                        value={opt.value}
                      />
                    ))}
                  </BzSelect>
                </BzFormItem>
                <div className="preview-panel">
                  <div className="preview-title">格式样例</div>
                  {decimalPreviewRows.map((sample, i) => (
                    <div
                      key={i}
                      className="preview-grid"
                    >
                      <div className="preview-label">原始值</div>
                      <div className="preview-value">{sample.source}</div>
                      <div className="preview-label">格式化结果</div>
                      <div className="preview-value strong">{sample.formatted}</div>
                    </div>
                  ))}
                </div>
              </>
            ) : isBoolEditor ? (
              <BzFormItem label="配置值">
                <BzSelect
                  modelValue={editorBoolValue}
                  onValueChange={(v) => setEditorBoolValue((v as "true" | "false") || "true")}
                >
                  <BzOption
                    label="true"
                    value="true"
                  />
                  <BzOption
                    label="false"
                    value="false"
                  />
                </BzSelect>
              </BzFormItem>
            ) : isMsgTypeConfigsEditor ? (
              <div className="msg-config-editor">
                <div className="msg-config-head">
                  <div className="msg-col-type">消息类型</div>
                  <div className="msg-col-route">目标路由</div>
                  <div className="msg-col-priority">提醒优先级</div>
                  <div className="msg-col-switch">SSE</div>
                  <div className="msg-col-switch">WebPush</div>
                  <div className="msg-col-switch">弹层</div>
                  <div className="msg-col-switch">系统通知</div>
                  <div className="msg-col-action"></div>
                </div>
                <div className="msg-config-list">
                  {editorMsgTypeConfigs.map((config, index) => (
                    <div
                      key={index}
                      className="msg-config-item"
                    >
                      <BzSelect
                        modelValue={config.msgType}
                        placeholder="选择类型"
                        className="msg-col-type"
                        onValueChange={(v) =>
                          setEditorMsgTypeConfigs((prev) =>
                            prev.map((item, i) =>
                              i === index ? { ...item, msgType: v ?? "" } : item,
                            ),
                          )
                        }
                      >
                        {getMsgTypeOptionsFor(index).map((opt) => (
                          <BzOption
                            key={opt.value}
                            label={opt.label}
                            value={opt.value}
                            disabled={opt.disabled}
                          />
                        ))}
                      </BzSelect>
                      <BzInput
                        modelValue={config.route}
                        placeholder="例如 /todo/all"
                        className="msg-col-route"
                        onValueChange={(v) =>
                          setEditorMsgTypeConfigs((prev) =>
                            prev.map((item, i) => (i === index ? { ...item, route: v } : item)),
                          )
                        }
                      />
                      <BzSelect
                        modelValue={config.priority}
                        placeholder="选择优先级"
                        className="msg-col-priority"
                        onValueChange={(v) =>
                          setEditorMsgTypeConfigs((prev) =>
                            prev.map((item, i) =>
                              i === index ? { ...item, priority: v ?? "MEDIUM" } : item,
                            ),
                          )
                        }
                      >
                        {msgPriorityOptions.map((opt) => (
                          <BzOption
                            key={opt.value}
                            label={opt.label}
                            value={opt.value}
                          />
                        ))}
                      </BzSelect>
                      <div className="msg-col-switch">
                        <BzSwitch
                          modelValue={config.sseEnabled}
                          onValueChange={(v) =>
                            setEditorMsgTypeConfigs((prev) =>
                              prev.map((item, i) =>
                                i === index ? { ...item, sseEnabled: v } : item,
                              ),
                            )
                          }
                        />
                      </div>
                      <div className="msg-col-switch">
                        <BzSwitch
                          modelValue={config.webPushEnabled}
                          onValueChange={(v) =>
                            setEditorMsgTypeConfigs((prev) =>
                              prev.map((item, i) =>
                                i === index ? { ...item, webPushEnabled: v } : item,
                              ),
                            )
                          }
                        />
                      </div>
                      <div className="msg-col-switch">
                        <BzSwitch
                          modelValue={config.panelAutoOpen}
                          onValueChange={(v) =>
                            setEditorMsgTypeConfigs((prev) =>
                              prev.map((item, i) =>
                                i === index ? { ...item, panelAutoOpen: v } : item,
                              ),
                            )
                          }
                        />
                      </div>
                      <div className="msg-col-switch">
                        <BzSwitch
                          modelValue={config.osNotificationEnabled}
                          onValueChange={(v) =>
                            setEditorMsgTypeConfigs((prev) =>
                              prev.map((item, i) =>
                                i === index
                                  ? {
                                      ...item,
                                      osNotificationEnabled: v,
                                    }
                                  : item,
                              ),
                            )
                          }
                        />
                      </div>
                      <div className="msg-col-action">
                        <BzIconActionButton
                          icon="minus"
                          tone="danger"
                          title="删除"
                          onClick={() => removeMsgConfigItem(index)}
                        />
                      </div>
                    </div>
                  ))}
                </div>
                <div className="msg-config-foot">
                  <div className="msg-config-foot-left">
                    {canUpdate ? (
                      <BzButton
                        size="small"
                        onClick={addMsgConfigItem}
                      >
                        <span className="bz-icon bz-icon-plus" />
                        添加类型配置
                      </BzButton>
                    ) : null}
                  </div>
                  <div className="msg-config-foot-right">
                    {canPreviewPush ? (
                      <BzSelect
                        modelValue={previewMsgType}
                        className="msg-preview-type"
                        placeholder="选择消息类型"
                        disabled={previewSendOptions.length === 0}
                        onValueChange={(v) => setPreviewMsgType(v ?? "")}
                      >
                        {previewSendOptions.map((opt) => (
                          <BzOption
                            key={opt.value}
                            label={opt.label}
                            value={opt.value}
                          />
                        ))}
                      </BzSelect>
                    ) : null}
                    {canPreviewPush ? (
                      <BzButton
                        size="small"
                        buttonType="primary"
                        disabled={!canPreviewSend}
                        loading={previewSending}
                        onClick={previewSendCurrentRule}
                      >
                        预览发送
                      </BzButton>
                    ) : null}
                  </div>
                </div>
                <div className="msg-config-preview">
                  <div className="preview-title">规则预览</div>
                  {msgConfigPreviewRows.length === 0 ? (
                    <div className="preview-empty">暂无规则，未配置类型将按默认规则处理。</div>
                  ) : (
                    msgConfigPreviewRows.map((row) => (
                      <div
                        key={row.msgType}
                        className="preview-row"
                      >
                        <div className="preview-row-title">{row.typeLabel}</div>
                        <div className="preview-row-meta">
                          路由：{row.routeLabel} ｜ 优先级：
                          {row.priorityLabel} ｜ 渠道：
                          {row.channelLabel}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            ) : isAuthWhitelistEditor ? (
              <div className="whitelist-editor">
                <div className="whitelist-head">
                  <div className="whitelist-col-type">匹配类型</div>
                  <div className="whitelist-col-pattern">路径规则</div>
                  <div className="whitelist-col-action"></div>
                </div>
                <div className="whitelist-list">
                  {editorAuthWhitelistRules.map((rule, index) => (
                    <div
                      key={index}
                      className="whitelist-item"
                    >
                      <BzSelect
                        modelValue={rule.type}
                        className="whitelist-col-type"
                        onValueChange={(v) =>
                          setEditorAuthWhitelistRules((prev) =>
                            prev.map((item, i) =>
                              i === index ? { ...item, type: v ?? "" } : item,
                            ),
                          )
                        }
                      >
                        {authWhitelistMatchTypeOptions.map((opt) => (
                          <BzOption
                            key={opt.value}
                            label={opt.label}
                            value={opt.value}
                          />
                        ))}
                      </BzSelect>
                      <BzInput
                        modelValue={rule.pattern}
                        className="whitelist-col-pattern"
                        placeholder="例如 /api/auth/login、/api/public/**"
                        onValueChange={(v) =>
                          setEditorAuthWhitelistRules((prev) =>
                            prev.map((item, i) => (i === index ? { ...item, pattern: v } : item)),
                          )
                        }
                      />
                      <div className="whitelist-col-action">
                        <BzIconActionButton
                          icon="minus"
                          tone="danger"
                          title="删除"
                          onClick={() => removeAuthWhitelistRule(index)}
                        />
                      </div>
                    </div>
                  ))}
                </div>
                <div className="whitelist-foot">
                  {canUpdate ? (
                    <BzButton
                      size="small"
                      onClick={addAuthWhitelistRule}
                    >
                      <span className="bz-icon bz-icon-plus" />
                      添加白名单规则
                    </BzButton>
                  ) : null}
                </div>
                <div className="whitelist-preview">
                  <div className="preview-title">规则预览</div>
                  {authWhitelistPreviewRows.length === 0 ? (
                    <div className="preview-empty">暂无规则。</div>
                  ) : (
                    authWhitelistPreviewRows.map((row, index) => (
                      <div
                        key={`${row.type}:${row.pattern}:${index}`}
                        className="preview-row"
                      >
                        <div className="preview-row-title">{row.typeLabel}</div>
                        <div className="preview-row-meta">{row.pattern}</div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            ) : isListEditor ? (
              <BzFormItem label="配置值（列表）">
                <div className="list-editor">
                  {editorListValue.map((_, index) => (
                    <div
                      key={index}
                      className="list-item"
                    >
                      <BzInput
                        modelValue={editorListValue[index]}
                        placeholder="输入项..."
                        onValueChange={(v) =>
                          setEditorListValue((prev) =>
                            prev.map((item, i) => (i === index ? v : item)),
                          )
                        }
                      />
                      <BzIconActionButton
                        icon="minus"
                        tone="danger"
                        title="删除"
                        ariaLabel="删除"
                        onClick={() => removeListItem(index)}
                      />
                    </div>
                  ))}
                  <BzIconActionButton
                    icon="plus"
                    tone="primary"
                    title="新增"
                    ariaLabel="新增"
                    onClick={addListItem}
                  />
                </div>
              </BzFormItem>
            ) : (
              <BzFormItem label="配置值">
                <BzInput
                  modelValue={editorRawValue}
                  type={isNumberType(editorItem) ? "number" : "text"}
                  clearable
                  onValueChange={setEditorRawValue}
                />
              </BzFormItem>
            )}
              </BzForm>

              {editorValidationErrorComputed ? <div className="form-error">{editorValidationErrorComputed}</div> : null}
            </section>
          </div>
        </AdminEntityDrawer>
      ) : null}
    </>
  );

  function getRowActions(row: ConfigItem): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      {
        key: `detail-${row.code}`,
        label: "详情",
        tone: "detail",
        handler: () => {
          setDetailItem(row);
          setDetailOpen(true);
        },
      },
    ];
    if (canUpdate) {
      actions.push({
        key: `edit-${row.code}`,
        label: "编辑",
        tone: "edit",
        handler: () => openEditor(row),
      });
    }
    return actions;
  }
}
