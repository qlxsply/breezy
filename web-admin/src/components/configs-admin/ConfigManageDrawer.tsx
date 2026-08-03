"use client";

import {AdminEntityDrawer} from "@admin/components/admin/AdminEntityDrawer";
import {BzAlert} from "@admin/components/bz/BzAlert";
import {BzButton} from "@admin/components/bz/BzButton";
import {BzInput} from "@admin/components/bz/BzInput";
import {BzOption} from "@admin/components/bz/BzOption";
import {BzSelect} from "@admin/components/bz/BzSelect";
import {BzSwitch} from "@admin/components/bz/BzSwitch";
import {BzTag} from "@admin/components/bz/BzTag";
import {BzTextField} from "@admin/components/bz/BzTextField";
import {
  formatDateTime,
  resolveUserDateFormatCode,
  resolveUserDateTimeFormatCode,
  resolveUserTimeZoneCode,
} from "@admin/core/formatter";
import {usePersonalizedConfigs} from "@admin/core/registry/auth-registry";
import type {ConfigFieldSpec, ConfigItem, ConfigViolation, JsonObject, JsonValue,} from "@admin/types/config-admin";
import {useEffect, useState} from "react";

export type ConfigManageMode = "detail" | "edit";
type FieldInputValue = string | boolean;

interface ConfigManageDrawerProps {
  open: boolean;
  mode: ConfigManageMode;
  item: ConfigItem | null;
  loading?: boolean;
  saving?: boolean;
  canEdit?: boolean;
  onClose: () => void;
  onSubmit: (value: JsonObject, reason: string) => Promise<ConfigViolation[]>;
}

interface WhitelistRule {
  type: string;
  pattern: string;
}

interface MessageTypeRule {
  msgType: string;
  route: string;
  priority: string;
  sseEnabled: boolean;
  webPushEnabled: boolean;
  panelAutoOpen: boolean;
  osNotificationEnabled: boolean;
}

const WHITELIST_TYPE_OPTIONS = [
  {value: "EXACT", label: "精确匹配"},
  {value: "ANT", label: "Ant 路径匹配"},
  {value: "PATH_PATTERN", label: "PathPattern 匹配"},
];

const MESSAGE_TYPE_OPTIONS = [
  {value: "TODO_REMINDER", label: "待办提醒"},
  {value: "SYSTEM_EVENT", label: "系统事件"},
  {value: "BUSINESS_EVENT", label: "业务事件"},
];

const MESSAGE_PRIORITY_OPTIONS = [
  {value: "LOW", label: "低"},
  {value: "MEDIUM", label: "中"},
  {value: "HIGH", label: "高"},
];

const ROUNDING_MODE_OPTIONS = [
  {value: "HALF_UP", label: "四舍五入", description: "大于等于 5 时向远离零方向进位"},
  {value: "HALF_DOWN", label: "五舍六入", description: "大于 5 时进位，正好为 5 时舍去"},
  {value: "HALF_EVEN", label: "银行家舍入", description: "正好为 5 时向最近的偶数舍入"},
  {value: "UP", label: "远离零舍入", description: "存在舍弃位时始终增大绝对值"},
  {value: "DOWN", label: "趋向零舍入", description: "直接舍弃超出位数的部分"},
  {value: "CEILING", label: "向上取整", description: "向正无穷方向舍入"},
  {value: "FLOOR", label: "向下取整", description: "向负无穷方向舍入"},
];

const USER_PREFERENCE_OPTIONS: Record<string, Array<{ value: string; label: string }>> = {
  timeZone: [
    {value: "ASIA_SHANGHAI", label: "Asia/Shanghai (UTC+08:00)"},
    {value: "UTC", label: "UTC"},
    {value: "ASIA_TOKYO", label: "Asia/Tokyo (UTC+09:00)"},
    {value: "EUROPE_BERLIN", label: "Europe/Berlin"},
    {value: "AMERICA_NEW_YORK", label: "America/New_York"},
  ],
  dateTimeFormat: [
    {value: "YYYY_MM_DD_HH_MM_SS", label: "yyyy-MM-dd HH:mm:ss"},
    {value: "YYYY_SLASH_MM_DD_HH_MM_SS", label: "yyyy/MM/dd HH:mm:ss"},
    {value: "DD_SLASH_MM_YYYY_HH_MM_SS", label: "dd/MM/yyyy HH:mm:ss"},
    {value: "MM_DD_YYYY_HH_MM", label: "MM-dd-yyyy HH:mm"},
  ],
  dateFormat: [
    {value: "YYYY_MM_DD", label: "yyyy-MM-dd"},
    {value: "YYYY_SLASH_MM_DD", label: "yyyy/MM/dd"},
    {value: "DD_SLASH_MM_YYYY", label: "dd/MM/yyyy"},
    {value: "MM_DD_YYYY", label: "MM-dd-yyyy"},
  ],
  decimalFormat: [
    {value: "COMMA_DOT", label: "千分位逗号、小数点"},
    {value: "PLAIN_DOT", label: "无千分位、小数点"},
    {value: "DOT_COMMA", label: "千分位点、逗号小数点"},
  ],
};

function roundDecimalText(input: string, scale: number, mode: string): string {
  const normalizedScale = Math.max(0, Math.min(20, Math.trunc(scale)));
  const normalized = input.trim();
  const negative = normalized.startsWith("-");
  const unsigned = normalized.replace(/^[+-]/, "");
  const [wholeRaw = "0", fractionRaw = ""] = unsigned.split(".");
  const whole = wholeRaw.replace(/^0+(?=\d)/, "") || "0";
  const keptFraction = fractionRaw.padEnd(normalizedScale, "0").slice(0, normalizedScale);
  const discarded = fractionRaw.slice(normalizedScale);
  const keptDigits = `${whole}${keptFraction}`.replace(/^0+(?=\d)/, "") || "0";
  let units = BigInt(keptDigits);
  const hasDiscardedValue = /[1-9]/.test(discarded);
  const firstDiscarded = Number(discarded.charAt(0) || "0");
  const remainingDiscarded = discarded.slice(1);
  const greaterThanHalf = firstDiscarded > 5 || (firstDiscarded === 5 && /[1-9]/.test(remainingDiscarded));
  const exactlyHalf = firstDiscarded === 5 && !/[1-9]/.test(remainingDiscarded);
  let increment = false;
  if (mode === "UP") increment = hasDiscardedValue;
  if (mode === "CEILING") increment = !negative && hasDiscardedValue;
  if (mode === "FLOOR") increment = negative && hasDiscardedValue;
  if (mode === "HALF_UP") increment = greaterThanHalf || exactlyHalf;
  if (mode === "HALF_DOWN") increment = greaterThanHalf;
  if (mode === "HALF_EVEN") increment = greaterThanHalf || (exactlyHalf && units % 2n !== 0n);
  if (increment) units += 1n;
  const digits = units.toString().padStart(normalizedScale + 1, "0");
  const integer = normalizedScale ? digits.slice(0, -normalizedScale) : digits;
  const fraction = normalizedScale ? digits.slice(-normalizedScale) : "";
  const sign = negative && units !== 0n ? "-" : "";
  return `${sign}${integer}${normalizedScale ? `.${fraction}` : ""}`;
}

function tieSample(scale: number, negative = false): string {
  return `${negative ? "-" : ""}2.${"0".repeat(Math.max(0, scale))}5`;
}

function oddTieSample(scale: number): string {
  if (scale <= 0) return "3.5";
  return `2.${"0".repeat(scale - 1)}15`;
}

function formatPatternDate(date: Date, timeZoneCode: string, patternCode: string, dateOnly: boolean): string {
  const timeZone = resolveUserTimeZoneCode(timeZoneCode);
  const pattern = dateOnly
    ? resolveUserDateFormatCode(patternCode)
    : resolveUserDateTimeFormatCode(patternCode);
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).formatToParts(date);
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return pattern
    .replaceAll("yyyy", values.year || "")
    .replaceAll("MM", values.month || "")
    .replaceAll("dd", values.day || "")
    .replaceAll("HH", values.hour || "")
    .replaceAll("mm", values.minute || "")
    .replaceAll("ss", values.second || "");
}

function formatPreferenceDecimal(value: string, formatCode: string): string {
  const rounded = roundDecimalText(value, 2, "HALF_UP");
  const negative = rounded.startsWith("-");
  const [integerRaw, fraction = ""] = rounded.replace("-", "").split(".");
  const grouping = formatCode !== "PLAIN_DOT";
  const groupingSeparator = formatCode === "DOT_COMMA" ? "." : ",";
  const decimalSeparator = formatCode === "DOT_COMMA" ? "," : ".";
  const integer = grouping ? integerRaw.replace(/\B(?=(\d{3})+(?!\d))/g, groupingSeparator) : integerRaw;
  return `${negative ? "-" : ""}${integer}${fraction ? `${decimalSeparator}${fraction}` : ""}`;
}

function isJsonObject(value: unknown): value is JsonObject {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function cloneObject(value: JsonValue): JsonObject {
  if (!isJsonObject(value)) return {};
  return JSON.parse(JSON.stringify(value)) as JsonObject;
}

function pathParts(path: string): string[] {
  return path.split(".").filter(Boolean);
}

function getPathValue(value: JsonValue, path: string): JsonValue | undefined {
  let current: JsonValue | undefined = value;
  for (const part of pathParts(path)) {
    if (!isJsonObject(current)) return undefined;
    current = current[part];
  }
  return current;
}

function setPathValue(target: JsonObject, path: string, value: JsonValue): void {
  const parts = pathParts(path);
  if (!parts.length) return;
  let current = target;
  for (const part of parts.slice(0, -1)) {
    if (!isJsonObject(current[part])) current[part] = {};
    current = current[part] as JsonObject;
  }
  current[parts[parts.length - 1]] = value;
}

function deletePathValue(target: JsonObject, path: string): void {
  const parts = pathParts(path);
  if (!parts.length) return;
  let current = target;
  for (const part of parts.slice(0, -1)) {
    if (!isJsonObject(current[part])) return;
    current = current[part] as JsonObject;
  }
  delete current[parts[parts.length - 1]];
}

function inputValue(item: ConfigItem, field: ConfigFieldSpec): FieldInputValue {
  if (field.sensitive) return "";
  const value = getPathValue(item.effectiveValue, field.path);
  if (field.type === "BOOLEAN") return Boolean(value);
  if (field.type === "STRING_LIST") return Array.isArray(value) ? value.join("\n") : "";
  if (field.type === "OBJECT") return value === undefined ? "" : JSON.stringify(value, null, 2);
  return value === null || value === undefined ? "" : String(value);
}

function initialInputs(item: ConfigItem): Record<string, FieldInputValue> {
  return Object.fromEntries(item.fields.map((field) => [field.path, inputValue(item, field)]));
}

function readWhitelistRules(value: JsonValue): WhitelistRule[] {
  const rules = isJsonObject(value) && Array.isArray(value.rules) ? value.rules : [];
  return rules.filter(isJsonObject).map((rule) => ({
    type: typeof rule.type === "string" ? rule.type : "EXACT",
    pattern: typeof rule.pattern === "string" ? rule.pattern : "",
  }));
}

function readMessageRules(value: JsonValue): MessageTypeRule[] {
  const source = isJsonObject(value) && Array.isArray(value.items) ? value.items : [];
  return MESSAGE_TYPE_OPTIONS.map((option) => {
    const matched = source.find(
      (entry) => isJsonObject(entry) && entry.msgType === option.value,
    );
    return {
      msgType: option.value,
      route: isJsonObject(matched) && typeof matched.route === "string" ? matched.route : "",
      priority:
        isJsonObject(matched) && typeof matched.priority === "string" ? matched.priority : "MEDIUM",
      sseEnabled: isJsonObject(matched) && Boolean(matched.sseEnabled),
      webPushEnabled: isJsonObject(matched) && Boolean(matched.webPushEnabled),
      panelAutoOpen: isJsonObject(matched) && Boolean(matched.panelAutoOpen),
      osNotificationEnabled: isJsonObject(matched) && Boolean(matched.osNotificationEnabled),
    };
  });
}

function statusMeta(item: ConfigItem): { label: string; type: "info" | "warning" | "danger" | "success" } {
  if (item.status === "CONFIGURED") return {label: "已配置", type: "success"};
  if (item.status === "INVALID_DATABASE_VALUE") return {label: "配置异常", type: "danger"};
  if (item.status === "RESTART_REQUIRED") return {label: "等待重启", type: "warning"};
  if (item.status === "READ_ONLY") return {label: "只读", type: "info"};
  return {label: "使用默认值", type: "info"};
}

function displayValue(item: ConfigItem, field: ConfigFieldSpec): string {
  if (field.sensitive) {
    return item.sensitiveValuePresence[field.path] ? "******" : "-";
  }
  const value = getPathValue(item.effectiveValue, field.path);
  if (value === null || value === undefined || value === "") return "-";
  if (Array.isArray(value)) return value.join("、") || "-";
  if (typeof value === "object") return JSON.stringify(value);
  if (typeof value === "boolean") return value ? "是" : "否";
  return String(value);
}

function base64UrlByteLength(value: string): number | null {
  try {
    const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    return atob(padded).length;
  } catch {
    return null;
  }
}

export function ConfigManageDrawer({
                                     open,
                                     mode,
                                     item,
                                     loading = false,
                                     saving = false,
                                     canEdit = false,
                                     onClose,
                                     onSubmit,
                                   }: ConfigManageDrawerProps) {
  const [inputs, setInputs] = useState<Record<string, FieldInputValue>>({});
  const [touchedSensitiveFields, setTouchedSensitiveFields] = useState<string[]>([]);
  const [whitelistRules, setWhitelistRules] = useState<WhitelistRule[]>([]);
  const [messageRules, setMessageRules] = useState<MessageTypeRule[]>([]);
  const [violations, setViolations] = useState<ConfigViolation[]>([]);
  const [reason, setReason] = useState("");
  const editable = mode === "edit" && canEdit && item?.editPolicy === "ADMIN_EDITABLE";

  useEffect(() => {
    if (!open || !item) return;
    setInputs(initialInputs(item));
    setTouchedSensitiveFields([]);
    setWhitelistRules(readWhitelistRules(item.effectiveValue));
    setMessageRules(readMessageRules(item.effectiveValue));
    setViolations([]);
    setReason("");
  }, [item, mode, open]);

  function changeField(field: ConfigFieldSpec, value: FieldInputValue) {
    setInputs((previous) => ({...previous, [field.path]: value}));
    if (field.sensitive) {
      setTouchedSensitiveFields((previous) =>
        previous.includes(field.path) ? previous : [...previous, field.path],
      );
    }
    setViolations((previous) => previous.filter((violation) => violation.path !== field.path));
  }

  function buildDefaultValue(): { value?: JsonObject; violations: ConfigViolation[] } {
    if (!item) return {violations: []};
    const value = cloneObject(item.effectiveValue);
    const nextViolations: ConfigViolation[] = [];
    for (const field of [...item.fields].sort((left, right) => left.order - right.order)) {
      if (field.readOnly) continue;
      if (field.sensitive && !touchedSensitiveFields.includes(field.path)) {
        deletePathValue(value, field.path);
        continue;
      }
      const input = inputs[field.path];
      if (field.type === "BOOLEAN") {
        setPathValue(value, field.path, Boolean(input));
        continue;
      }
      const text = typeof input === "string" ? input : String(input ?? "");
      if (field.required && text.trim() === "") {
        nextViolations.push({path: field.path, code: "REQUIRED", message: `${field.title}不能为空`});
        continue;
      }
      if (field.type === "INTEGER" || field.type === "LONG" || field.type === "DECIMAL") {
        const number = Number(text);
        const integerRequired = field.type !== "DECIMAL";
        if (!Number.isFinite(number) || (integerRequired && !Number.isInteger(number))) {
          nextViolations.push({path: field.path, code: "INVALID_NUMBER", message: "请输入有效数字"});
          continue;
        }
        if (field.min !== null && number < field.min) {
          nextViolations.push({path: field.path, code: "MIN", message: `不能小于 ${field.min}`});
        }
        if (field.max !== null && number > field.max) {
          nextViolations.push({path: field.path, code: "MAX", message: `不能大于 ${field.max}`});
        }
        setPathValue(value, field.path, number);
        continue;
      }
      if (field.type === "STRING_LIST") {
        const list = text.split(/\r?\n/).map((entry) => entry.trim()).filter(Boolean);
        if (field.required && !list.length) {
          nextViolations.push({path: field.path, code: "REQUIRED", message: `${field.title}不能为空`});
        }
        setPathValue(value, field.path, list);
        continue;
      }
      if (field.type === "OBJECT") {
        try {
          setPathValue(value, field.path, JSON.parse(text) as JsonValue);
        } catch {
          nextViolations.push({path: field.path, code: "INVALID_JSON", message: "请输入有效 JSON"});
        }
        continue;
      }
      if (field.type === "ENUM" && !field.options.some((option) => option.value === text)) {
        nextViolations.push({path: field.path, code: "INVALID_OPTION", message: "请选择有效选项"});
      }
      if (field.minLength !== null && text.length < field.minLength) {
        nextViolations.push({path: field.path, code: "MIN_LENGTH", message: `长度不能少于 ${field.minLength}`});
      }
      if (field.maxLength !== null && text.length > field.maxLength) {
        nextViolations.push({path: field.path, code: "MAX_LENGTH", message: `长度不能超过 ${field.maxLength}`});
      }
      setPathValue(value, field.path, text);
    }
    return {value: nextViolations.length ? undefined : value, violations: nextViolations};
  }

  function buildValue(): { value?: JsonObject; violations: ConfigViolation[] } {
    if (!item) return {violations: []};
    if (item.editorId === "auth-whitelist") {
      const nextViolations = whitelistRules.flatMap((rule, index) => {
        const errors: ConfigViolation[] = [];
        if (!WHITELIST_TYPE_OPTIONS.some((option) => option.value === rule.type)) {
          errors.push({path: `rules[${index}].type`, code: "INVALID_TYPE", message: "请选择匹配类型"});
        }
        if (!rule.pattern.trim() || !rule.pattern.trim().startsWith("/")) {
          errors.push({path: `rules[${index}].pattern`, code: "INVALID_PATH", message: "路径必须以 / 开头"});
        }
        return errors;
      });
      const value = cloneObject(item.effectiveValue);
      value.rules = whitelistRules.map((rule) => ({type: rule.type, pattern: rule.pattern.trim()}));
      return {value: nextViolations.length ? undefined : value, violations: nextViolations};
    }
    if (item.editorId === "message-type-config") {
      const nextViolations = messageRules.flatMap((rule, index) => {
        if (rule.route.trim() && !rule.route.trim().startsWith("/")) {
          return [{path: `items[${index}].route`, code: "INVALID_ROUTE", message: "消息路由必须以 / 开头"}];
        }
        return [];
      });
      const value = cloneObject(item.effectiveValue);
      value.items = messageRules.map((rule) => ({...rule, route: rule.route.trim()}));
      return {value: nextViolations.length ? undefined : value, violations: nextViolations};
    }
    const built = buildDefaultValue();
    if (item.editorId !== "web-push-vapid" || !built.value) return built;
    const nextViolations = [...built.violations];
    const publicKey = String(built.value.publicKey ?? "").trim();
    const privateKey = String(inputs.privateKey ?? "").trim();
    const hasPrivateKey = touchedSensitiveFields.includes("privateKey")
      ? Boolean(privateKey)
      : Boolean(item.sensitiveValuePresence.privateKey);
    const subject = String(built.value.subject ?? "").trim();
    if (Boolean(publicKey) !== hasPrivateKey) {
      nextViolations.push({path: "publicKey", code: "KEY_PAIR_REQUIRED", message: "公钥和私钥必须同时配置"});
    }
    if (publicKey && base64UrlByteLength(publicKey) !== 65) {
      nextViolations.push({path: "publicKey", code: "INVALID_KEY", message: "VAPID 公钥格式无效"});
    }
    if (privateKey && base64UrlByteLength(privateKey) !== 32) {
      nextViolations.push({path: "privateKey", code: "INVALID_KEY", message: "VAPID 私钥格式无效"});
    }
    if (!subject || (!subject.startsWith("mailto:") && !subject.startsWith("https://"))) {
      nextViolations.push({path: "subject", code: "INVALID_URI", message: "联系主体必须是 mailto 或 https 地址"});
    }
    return {value: nextViolations.length ? undefined : built.value, violations: nextViolations};
  }

  async function submit() {
    const built = buildValue();
    if (!built.value) {
      setViolations(built.violations);
      return;
    }
    setViolations([]);
    setViolations(await onSubmit(built.value, reason.trim()));
  }

  const footer = (
    <div className="permission-dialog-footer">
      <div className="permission-dialog-footer__summary"/>
      <div className="permission-dialog-footer__actions">
        <BzButton disabled={saving} onClick={onClose}>{editable ? "取消" : "关闭"}</BzButton>
        {editable ? (
          <BzButton buttonType="primary" loading={saving} onClick={() => void submit()}>保存</BzButton>
        ) : null}
      </div>
    </div>
  );

  return (
    <AdminEntityDrawer
      open={open}
      title={mode === "edit" ? "编辑配置" : "配置详情"}
      width="1180px"
      loading={loading}
      className="role-manage-drawer config-manage-drawer"
      onClose={onClose}
      footer={footer}
    >
      {item ? (
        <div className="role-manage-shell">
          {item.loadWarning ? <BzAlert title={item.loadWarning} type="warning" showIcon closable={false}/> : null}
          {item.pendingRestart ?
            <BzAlert title="当前配置需要重启服务后生效。" type="warning" showIcon closable={false}/> : null}
          <ConfigBasicInfo item={item}/>
          <section className="role-manage-section">
            <div className="role-manage-section__head">
              <div className="role-manage-section__title">当前生效值</div>
            </div>
            {item.editorId === "decimal-policy" ? (
              <DecimalPolicyEditor
                item={item}
                editable={editable}
                inputs={inputs}
                violations={violations}
                onChange={changeField}
              />
            ) : item.editorId === "time-offset" ? (
              <TimeOffsetEditor
                item={item}
                editable={editable}
                inputs={inputs}
                violations={violations}
                onChange={changeField}
              />
            ) : item.editorId === "user-preference-defaults" ? (
              <UserPreferenceDefaultsEditor
                item={item}
                editable={editable}
                inputs={inputs}
                violations={violations}
                onChange={changeField}
              />
            ) : item.editorId === "auth-whitelist" ? (
              <AuthWhitelistEditor
                editable={editable}
                rules={whitelistRules}
                violations={violations}
                onChange={setWhitelistRules}
              />
            ) : item.editorId === "message-type-config" ? (
              <MessageTypeEditor
                editable={editable}
                rules={messageRules}
                violations={violations}
                onChange={setMessageRules}
              />
            ) : item.editorId === "web-push-vapid" ? (
              <VapidEditor
                item={item}
                editable={editable}
                inputs={inputs}
                violations={violations}
                onChange={changeField}
              />
            ) : (
              <DefaultConfigEditor
                item={item}
                editable={editable}
                inputs={inputs}
                violations={violations}
                onChange={changeField}
              />
            )}
          </section>
          {editable ? (
            <section className="role-manage-section">
              <div className="role-manage-section__head">
                <div className="role-manage-section__title">变更说明</div>
              </div>
              <div className="config-value-table-wrap">
                <table className="config-value-table config-change-table">
                  <tbody>
                  <tr>
                    <th>变更原因</th>
                    <td>
                      <BzTextField
                        type="textarea"
                        rows={3}
                        maxlength={500}
                        showCounter
                        modelValue={reason}
                        placeholder="选填，说明本次配置变更原因"
                        onValueChange={setReason}
                      />
                    </td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}
        </div>
      ) : null}
    </AdminEntityDrawer>
  );
}

function ConfigBasicInfo({item}: { item: ConfigItem }) {
  const status = statusMeta(item);
  return (
    <section className="role-manage-section">
      <div className="role-manage-section__head">
        <div className="role-manage-section__title">基本信息</div>
      </div>
      <div className="role-info-table-wrap">
        <table className="role-info-table" aria-label="配置基本信息">
          <tbody>
          <tr>
            <th>配置键</th>
            <td className="role-info-cell mono" colSpan={5}>{item.key}</td>
          </tr>
          <tr>
            <th>配置名称</th>
            <td className="role-info-cell">{item.title}</td>
            <th>状态</th>
            <td className="role-info-cell"><BzTag type={status.type}>{status.label}</BzTag></td>
            <th>编辑器</th>
            <td className="role-info-cell mono">{item.editorId}</td>
          </tr>
          <tr>
            <th>模块</th>
            <td className="role-info-cell">{item.module}</td>
            <th>分组</th>
            <td className="role-info-cell">{item.group}</td>
            <th>生效方式</th>
            <td className="role-info-cell">{item.activationPolicy === "DYNAMIC" ? "动态生效" : "重启后生效"}</td>
          </tr>
          <tr>
            <th>值来源</th>
            <td
              className="role-info-cell">{item.source === "DATABASE_OVERRIDE" ? "数据库配置" : item.source === "INVALID_DATABASE_FALLBACK" ? "异常回退" : "代码默认值"}</td>
            <th>配置版本</th>
            <td className="role-info-cell mono">{item.persistedRevision}</td>
            <th>Schema 版本</th>
            <td className="role-info-cell mono">{item.schemaVersion}</td>
          </tr>
          <tr>
            <th>说明</th>
            <td className="role-info-cell" colSpan={5}>{item.description || "-"}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>
  );
}

function DefaultConfigEditor({item, editable, inputs, violations, onChange}: {
  item: ConfigItem;
  editable: boolean;
  inputs: Record<string, FieldInputValue>;
  violations: ConfigViolation[];
  onChange: (field: ConfigFieldSpec, value: FieldInputValue) => void;
}) {
  return (
    <div className="config-value-table-wrap">
      <table className="config-value-table">
        <thead>
        <tr>
          <th>Path</th>
          <th>名称</th>
          <th>类型</th>
          <th>值</th>
        </tr>
        </thead>
        <tbody>
        {[...item.fields].sort((left, right) => left.order - right.order).map((field) => {
          const fieldViolations = violations.filter((violation) => violation.path === field.path);
          return (
            <tr key={field.path} className={fieldViolations.length ? "is-error" : undefined}>
              <td className="mono">{field.path}</td>
              <td>{field.title}</td>
              <td><BzTag size="small">{field.type}</BzTag></td>
              <td>
                {editable ? renderFieldInput(item, field, inputs[field.path], onChange) : displayValue(item, field)}
                {fieldViolations.map((violation, index) => <div className="config-field-error"
                                                                key={`${violation.code}-${index}`}>{violation.message}</div>)}
              </td>
            </tr>
          );
        })}
        </tbody>
      </table>
    </div>
  );
}

function DecimalPolicyEditor({item, editable, inputs, violations, onChange}: {
  item: ConfigItem;
  editable: boolean;
  inputs: Record<string, FieldInputValue>;
  violations: ConfigViolation[];
  onChange: (field: ConfigFieldSpec, value: FieldInputValue) => void;
}) {
  const scaleField = item.fields.find((field) => field.path === "scale");
  const roundingField = item.fields.find((field) => field.path === "roundingMode");
  const rawScale = Number(inputs.scale);
  const scale = Number.isInteger(rawScale) && rawScale >= 0 && rawScale <= 20 ? rawScale : 2;
  const roundingMode = typeof inputs.roundingMode === "string" ? inputs.roundingMode : "HALF_UP";
  const positiveTie = tieSample(scale);
  const positiveOddTie = oddTieSample(scale);
  const negativeTie = tieSample(scale, true);
  const selectedSamples = [
    {scene: "正数临界值（保留位为偶数）", source: positiveTie},
    {scene: "正数临界值（保留位为奇数）", source: positiveOddTie},
    {scene: "负数临界值", source: negativeTie},
    {scene: "普通正数", source: "1234.567890123"},
    {scene: "普通负数", source: "-9876.543210987"},
  ];
  return (
    <div className="config-custom-editor">
      <div className="config-value-table-wrap">
        <table className="config-value-table config-setting-table">
          <thead>
          <tr>
            <th>配置项</th>
            <th>说明</th>
            <th>值</th>
          </tr>
          </thead>
          <tbody>
          <tr className={violations.some((violation) => violation.path === "scale") ? "is-error" : undefined}>
            <td>小数位数
              <div className="mono">scale</div>
            </td>
            <td>最终结果保留的小数位数，范围 0 至 20</td>
            <td>{editable && scaleField ? <BzInput modelValue={inputs.scale as string} type="number"
                                                   onValueChange={(value) => onChange(scaleField, value)}/> : String(inputs.scale)}</td>
          </tr>
          <tr className={violations.some((violation) => violation.path === "roundingMode") ? "is-error" : undefined}>
            <td>舍入模式
              <div className="mono">roundingMode</div>
            </td>
            <td>{ROUNDING_MODE_OPTIONS.find((option) => option.value === roundingMode)?.description || "决定超出小数位数时的处理方式"}</td>
            <td>{editable && roundingField ? (
              <BzSelect modelValue={roundingMode}
                        onValueChange={(value) => onChange(roundingField, value ?? "HALF_UP")}>
                {ROUNDING_MODE_OPTIONS.map((option) => <BzOption key={option.value} value={option.value}
                                                                 label={`${option.label} (${option.value})`}/>)}
              </BzSelect>
            ) : `${ROUNDING_MODE_OPTIONS.find((option) => option.value === roundingMode)?.label || roundingMode} (${roundingMode})`}</td>
          </tr>
          </tbody>
        </table>
        {violations.map((violation, index) => <div className="config-field-error"
                                                   key={`${violation.path}-${index}`}>{violation.message}</div>)}
      </div>

      <ConfigPreviewTitle title="当前设置效果" description={`按 ${scale} 位小数和当前舍入模式计算`}/>
      <div className="config-value-table-wrap">
        <table className="config-value-table config-preview-table">
          <thead>
          <tr>
            <th>场景</th>
            <th>原始值</th>
            <th>舍入结果</th>
          </tr>
          </thead>
          <tbody>
          {selectedSamples.map((sample) => <tr key={sample.scene}>
            <td>{sample.scene}</td>
            <td className="mono">{sample.source}</td>
            <td className="mono config-preview-result">{roundDecimalText(sample.source, scale, roundingMode)}</td>
          </tr>)}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function TimeOffsetEditor({item, editable, inputs, violations, onChange}: {
  item: ConfigItem;
  editable: boolean;
  inputs: Record<string, FieldInputValue>;
  violations: ConfigViolation[];
  onChange: (field: ConfigFieldSpec, value: FieldInputValue) => void;
}) {
  const personalizedConfigs = usePersonalizedConfigs();
  const [now, setNow] = useState(() => Date.now());
  const field = item.fields.find((candidate) => candidate.path === "offsetSeconds");
  const parsedOffset = Number(inputs.offsetSeconds);
  const offsetSeconds = Number.isFinite(parsedOffset) ? parsedOffset : 0;
  const formatterVersion = personalizedConfigs.map((config) => `${config.code}:${config.value}`).join("|");
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  return (
    <div className="config-custom-editor" key={formatterVersion}>
      <div className="config-value-table-wrap">
        <table className="config-value-table config-setting-table">
          <thead>
          <tr>
            <th>配置项</th>
            <th>说明</th>
            <th>值</th>
          </tr>
          </thead>
          <tbody>
          <tr className={violations.length ? "is-error" : undefined}>
            <td>时间偏移秒数
              <div className="mono">offsetSeconds</div>
            </td>
            <td>正数表示向未来偏移，负数表示向过去偏移，0 表示使用真实时间</td>
            <td>{editable && field ? <BzInput modelValue={inputs.offsetSeconds as string} type="number"
                                              onValueChange={(value) => onChange(field, value)}/> : String(inputs.offsetSeconds)}</td>
          </tr>
          </tbody>
        </table>
        {violations.map((violation, index) => <div className="config-field-error"
                                                   key={`${violation.path}-${index}`}>{violation.message}</div>)}
      </div>
      <ConfigPreviewTitle title="时间偏移效果" description="时间格式和时区取自当前登录用户的个性化配置"/>
      <div className="config-value-table-wrap">
        <table className="config-value-table config-time-preview-table">
          <thead>
          <tr>
            <th>时间类型</th>
            <th>格式化结果</th>
            <th>Epoch 毫秒</th>
          </tr>
          </thead>
          <tbody>
          <tr>
            <td>当前真实时间</td>
            <td className="mono">{formatDateTime(now)}</td>
            <td className="mono">{now}</td>
          </tr>
          <tr>
            <td>偏移后时间</td>
            <td className="mono config-preview-result">{formatDateTime(now + offsetSeconds * 1000)}</td>
            <td className="mono">{now + offsetSeconds * 1000}</td>
          </tr>
          <tr>
            <td>偏移量</td>
            <td colSpan={2}>{offsetSeconds === 0 ? "无偏移" : `${offsetSeconds > 0 ? "+" : ""}${offsetSeconds} 秒`}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}

function UserPreferenceDefaultsEditor({item, editable, inputs, violations, onChange}: {
  item: ConfigItem;
  editable: boolean;
  inputs: Record<string, FieldInputValue>;
  violations: ConfigViolation[];
  onChange: (field: ConfigFieldSpec, value: FieldInputValue) => void;
}) {
  const now = new Date();
  const timeZone = String(inputs.timeZone || "ASIA_SHANGHAI");
  const dateTimeFormat = String(inputs.dateTimeFormat || "YYYY_MM_DD_HH_MM_SS");
  const dateFormat = String(inputs.dateFormat || "YYYY_MM_DD");
  const decimalFormat = String(inputs.decimalFormat || "COMMA_DOT");
  const decimalSamples = ["12.3", "1234.56", "1234567.891", "9876543210.12", "-12345678.9", "0.0045"];
  return (
    <div className="config-custom-editor">
      <div className="config-value-table-wrap">
        <table className="config-value-table config-setting-table">
          <thead>
          <tr>
            <th>配置项</th>
            <th>说明</th>
            <th>值</th>
          </tr>
          </thead>
          <tbody>
          {[...item.fields].sort((left, right) => left.order - right.order).map((field) => {
            const options = USER_PREFERENCE_OPTIONS[field.path] || field.options;
            const value = String(inputs[field.path] || "");
            return (
              <tr key={field.path}
                  className={violations.some((violation) => violation.path === field.path) ? "is-error" : undefined}>
                <td>{field.title}
                  <div className="mono">{field.path}</div>
                </td>
                <td>{field.path === "timeZone" ? "日期和时间展示使用的时区" : field.path === "decimalFormat" ? "千分位和小数点符号组合" : "日期展示格式"}</td>
                <td>{editable ? (
                  <BzSelect modelValue={value} onValueChange={(next) => onChange(field, next ?? "")}>
                    {options.map((option) => <BzOption key={option.value} value={option.value} label={option.label}/>)}
                  </BzSelect>
                ) : options.find((option) => option.value === value)?.label || value}</td>
              </tr>
            );
          })}
          </tbody>
        </table>
        {violations.map((violation, index) => <div className="config-field-error"
                                                   key={`${violation.path}-${index}`}>{violation.message}</div>)}
      </div>
      <ConfigPreviewTitle title="日期与时间效果" description="以下结果随时区和格式选项实时变化"/>
      <div className="config-value-table-wrap">
        <table className="config-value-table config-preference-date-table">
          <thead>
          <tr>
            <th>预览类型</th>
            <th>预览结果</th>
            <th>使用配置</th>
          </tr>
          </thead>
          <tbody>
          <tr>
            <td>日期时间</td>
            <td className="mono config-preview-result">{formatPatternDate(now, timeZone, dateTimeFormat, false)}</td>
            <td>{timeZone} / {dateTimeFormat}</td>
          </tr>
          <tr>
            <td>仅日期</td>
            <td className="mono config-preview-result">{formatPatternDate(now, timeZone, dateFormat, true)}</td>
            <td>{timeZone} / {dateFormat}</td>
          </tr>
          </tbody>
        </table>
      </div>
      <ConfigPreviewTitle title="数字格式效果" description="使用多种位数和正负数展示千分位、小数点效果"/>
      <div className="config-value-table-wrap">
        <table className="config-value-table config-preference-decimal-table">
          <thead>
          <tr>
            <th>数字规模</th>
            <th>原始值</th>
            <th>格式化结果</th>
          </tr>
          </thead>
          <tbody>
          {decimalSamples.map((sample) => (
            <tr key={sample}>
              <td>{sample.replace("-", "").split(".")[0].length >= 7 ? "长数字" : sample.startsWith("-") ? "负数" : Number(sample) < 1 ? "小数" : "普通数字"}</td>
              <td className="mono">{sample}</td>
              <td className="mono config-preview-result">{formatPreferenceDecimal(sample, decimalFormat)}</td>
            </tr>
          ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ConfigPreviewTitle({title, description}: { title: string; description: string }) {
  return (
    <div className="config-preview-title">
      <strong>{title}</strong>
      <span>{description}</span>
    </div>
  );
}

function renderFieldInput(item: ConfigItem, field: ConfigFieldSpec, value: FieldInputValue | undefined,
                          onChange: (field: ConfigFieldSpec, value: FieldInputValue) => void) {
  const text = typeof value === "string" ? value : String(value ?? "");
  if (field.type === "BOOLEAN") {
    return <BzSwitch modelValue={Boolean(value)} activeText="启用" inactiveText="停用"
                     onValueChange={(next) => onChange(field, next)}/>;
  }
  if (field.type === "ENUM") {
    return (
      <BzSelect modelValue={text} onValueChange={(next) => onChange(field, next ?? "")}>
        {field.options.map((option) => <BzOption key={option.value} value={option.value} label={option.label}/>)}
      </BzSelect>
    );
  }
  if (field.type === "STRING_LIST" || field.type === "OBJECT") {
    return <BzTextField type="textarea" rows={field.type === "OBJECT" ? 8 : 4} modelValue={text}
                        placeholder={field.type === "OBJECT" ? "请输入 JSON" : "每行一项"}
                        onValueChange={(next) => onChange(field, next)}/>;
  }
  return (
    <BzInput
      modelValue={text}
      type={field.sensitive ? "password" : field.type === "STRING" ? "text" : "number"}
      placeholder={field.sensitive && item.sensitiveValuePresence[field.path] ? "******（留空保持原值）" : field.placeholder || "请输入"}
      onValueChange={(next) => onChange(field, next)}
    />
  );
}

function AuthWhitelistEditor({editable, rules, violations, onChange}: {
  editable: boolean;
  rules: WhitelistRule[];
  violations: ConfigViolation[];
  onChange: (rules: WhitelistRule[]) => void;
}) {
  return (
    <div className="config-value-table-wrap">
      <table className="config-value-table">
        <thead>
        <tr>
          <th>匹配类型</th>
          <th>路径规则</th>
          {editable ? <th className="config-action-column">操作</th> : null}</tr>
        </thead>
        <tbody>
        {rules.map((rule, index) => (
          <tr key={`${index}-${rule.type}`}>
            <td>{editable ? <BzSelect modelValue={rule.type}
                                      onValueChange={(value) => onChange(rules.map((item, itemIndex) => itemIndex === index ? {
                                        ...item,
                                        type: value ?? "EXACT"
                                      } : item))}>{WHITELIST_TYPE_OPTIONS.map((option) => <BzOption
              key={option.value} {...option} />)}</BzSelect> : WHITELIST_TYPE_OPTIONS.find((option) => option.value === rule.type)?.label || rule.type}</td>
            <td>{editable ? <BzInput modelValue={rule.pattern}
                                     onValueChange={(value) => onChange(rules.map((item, itemIndex) => itemIndex === index ? {
                                       ...item,
                                       pattern: value
                                     } : item))}/> : rule.pattern}</td>
            {editable ? <td><BzButton size="small" buttonType="danger"
                                      onClick={() => onChange(rules.filter((_, itemIndex) => itemIndex !== index))}>删除</BzButton>
            </td> : null}
          </tr>
        ))}
        </tbody>
      </table>
      {violations.map((violation, index) => <div className="config-field-error"
                                                 key={`${violation.path}-${index}`}>{violation.message}</div>)}
      {editable ? <BzButton size="small" onClick={() => onChange([...rules, {
        type: "EXACT",
        pattern: "/"
      }])}>新增规则</BzButton> : null}
    </div>
  );
}

function VapidEditor({item, editable, inputs, violations, onChange}: {
  item: ConfigItem;
  editable: boolean;
  inputs: Record<string, FieldInputValue>;
  violations: ConfigViolation[];
  onChange: (field: ConfigFieldSpec, value: FieldInputValue) => void;
}) {
  const fields = [...item.fields].sort((left, right) => left.order - right.order);
  const descriptions: Record<string, string> = {
    publicKey: "Base64URL 编码的 P-256 公钥",
    privateKey: "Base64URL 编码的 P-256 私钥，留空保持现值",
    subject: "接收推送服务联系主体，支持 mailto 或 https 地址",
  };
  return (
    <div className="config-value-table-wrap">
      <table className="config-value-table config-vapid-table">
        <thead>
        <tr>
          <th>配置项</th>
          <th>说明</th>
          <th>值</th>
        </tr>
        </thead>
        <tbody>
        {fields.map((field) => {
          const fieldViolations = violations.filter((violation) => violation.path === field.path);
          const text = typeof inputs[field.path] === "string" ? inputs[field.path] as string : "";
          return (
            <tr key={field.path} className={fieldViolations.length ? "is-error" : undefined}>
              <td>{field.title}
                <div className="mono">{field.path}</div>
              </td>
              <td>{descriptions[field.path] || field.description || "-"}</td>
              <td>
                {editable ? (
                  field.path === "publicKey" ? (
                    <BzTextField type="textarea" rows={3} modelValue={text} placeholder="请输入 VAPID 公钥"
                                 onValueChange={(value) => onChange(field, value)}/>
                  ) : (
                    <BzInput
                      modelValue={text}
                      type={field.sensitive ? "password" : "text"}
                      placeholder={field.sensitive && item.sensitiveValuePresence[field.path] ? "******（留空保持原值）" : "请输入"}
                      onValueChange={(value) => onChange(field, value)}
                    />
                  )
                ) : displayValue(item, field)}
                {fieldViolations.map((violation, index) => <div className="config-field-error"
                                                                key={`${violation.code}-${index}`}>{violation.message}</div>)}
              </td>
            </tr>
          );
        })}
        </tbody>
      </table>
    </div>
  );
}

function MessageTypeEditor({editable, rules, violations, onChange}: {
  editable: boolean;
  rules: MessageTypeRule[];
  violations: ConfigViolation[];
  onChange: (rules: MessageTypeRule[]) => void;
}) {
  function update(index: number, patch: Partial<MessageTypeRule>) {
    onChange(rules.map((rule, ruleIndex) => ruleIndex === index ? {...rule, ...patch} : rule));
  }

  return (
    <div className="config-value-table-wrap">
      <table className="config-value-table config-message-table">
        <thead>
        <tr>
          <th>消息类型</th>
          <th>路由</th>
          <th>优先级</th>
          <th>SSE</th>
          <th>Web Push</th>
          <th>自动弹层</th>
          <th>系统通知</th>
        </tr>
        </thead>
        <tbody>
        {rules.map((rule, index) => (
          <tr key={rule.msgType}>
            <td>{MESSAGE_TYPE_OPTIONS.find((option) => option.value === rule.msgType)?.label || rule.msgType}</td>
            <td>{editable ? <BzInput modelValue={rule.route} placeholder="例如 /todo/all"
                                     onValueChange={(route) => update(index, {route})}/> : rule.route || "-"}</td>
            <td>{editable ? <BzSelect modelValue={rule.priority}
                                      onValueChange={(priority) => update(index, {priority: priority ?? "MEDIUM"})}>{MESSAGE_PRIORITY_OPTIONS.map((option) =>
              <BzOption
                key={option.value} {...option} />)}</BzSelect> : MESSAGE_PRIORITY_OPTIONS.find((option) => option.value === rule.priority)?.label || rule.priority}</td>
            {(["sseEnabled", "webPushEnabled", "panelAutoOpen", "osNotificationEnabled"] as const).map((key) => <td
              key={key}>{editable ? <BzSwitch modelValue={rule[key]}
                                              onValueChange={(value) => update(index, {[key]: value})}/> : rule[key] ? "是" : "否"}</td>)}
          </tr>
        ))}
        </tbody>
      </table>
      {violations.map((violation, index) => <div className="config-field-error"
                                                 key={`${violation.path}-${index}`}>{violation.message}</div>)}
    </div>
  );
}
