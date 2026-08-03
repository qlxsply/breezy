"use client";

import {
  getConfig,
  listConfigs,
  resetConfigDefault,
  updateConfig,
  validateConfig,
} from "@admin/api/configs";
import { createAdminActionsColumn } from "@admin/components/admin/admin-actions-column";
import {
  type AdminDetailSection,
  AdminDetailTable,
} from "@admin/components/admin/AdminDetailTable";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { AdminListPageTemplate } from "@admin/components/admin/AdminListPageTemplate";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import { BzAlert } from "@admin/components/bz/BzAlert";
import { BzButton } from "@admin/components/bz/BzButton";
import { BzForm } from "@admin/components/bz/BzForm";
import { BzFormItem } from "@admin/components/bz/BzFormItem";
import { BzInput } from "@admin/components/bz/BzInput";
import { BzOption } from "@admin/components/bz/BzOption";
import { BzPagination } from "@admin/components/bz/BzPagination";
import { BzSelect } from "@admin/components/bz/BzSelect";
import { BzSwitch } from "@admin/components/bz/BzSwitch";
import { BzTable, type BzTableColumn } from "@admin/components/bz/BzTable";
import { BzTag } from "@admin/components/bz/BzTag";
import { BzTextField } from "@admin/components/bz/BzTextField";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type {
  ConfigFieldSpec,
  ConfigItem,
  ConfigManagementStatus,
  ConfigViolation,
  JsonObject,
  JsonValue,
} from "@admin/types/config-admin";
import type { PageResult } from "@admin/types/page";
import { useCallback, useEffect, useRef, useState } from "react";

type DrawerMode = "detail" | "edit";
type FieldInputValue = string | boolean;

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const EMPTY_PAGE: PageResult<ConfigItem> = {
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
};

const STATUS_META: Record<
  ConfigManagementStatus,
  { label: string; type: "info" | "warning" | "danger" | "success" }
> = {
  DEFAULT_VALUE: { label: "使用默认值", type: "info" },
  CONFIGURED: { label: "已配置", type: "success" },
  INVALID_DATABASE_VALUE: { label: "配置异常", type: "danger" },
  RESTART_REQUIRED: { label: "等待重启", type: "warning" },
  READ_ONLY: { label: "只读", type: "info" },
};

function isJsonObject(value: JsonValue): value is JsonObject {
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
    if (!current || typeof current !== "object" || Array.isArray(current)) return undefined;
    current = current[part];
  }
  return current;
}

function setPathValue(target: JsonObject, path: string, value: JsonValue): void {
  const parts = pathParts(path);
  if (!parts.length) return;
  let current = target;
  for (const part of parts.slice(0, -1)) {
    const child = current[part];
    if (!isJsonObject(child)) current[part] = {};
    current = current[part] as JsonObject;
  }
  current[parts[parts.length - 1]] = value;
}

function deletePathValue(target: JsonObject, path: string): void {
  const parts = pathParts(path);
  if (!parts.length) return;
  let current = target;
  for (const part of parts.slice(0, -1)) {
    const child = current[part];
    if (!isJsonObject(child)) return;
    current = child;
  }
  delete current[parts[parts.length - 1]];
}

function formatJson(value: JsonValue): string {
  return JSON.stringify(value, null, 2);
}

function fieldInputValue(item: ConfigItem, field: ConfigFieldSpec): FieldInputValue {
  if (field.sensitive) return "";
  const value = getPathValue(item.persistedValue, field.path);
  if (field.type === "BOOLEAN") return Boolean(value);
  if (field.type === "STRING_LIST") return Array.isArray(value) ? value.join("\n") : "";
  if (field.type === "OBJECT") return value === undefined ? "" : formatJson(value);
  return value === null || value === undefined ? "" : String(value);
}

function initialFieldInputs(item: ConfigItem): Record<string, FieldInputValue> {
  return Object.fromEntries(item.fields.map((field) => [field.path, fieldInputValue(item, field)]));
}

function buildConfigValue(
  item: ConfigItem,
  inputs: Record<string, FieldInputValue>,
  touchedSensitiveFields: string[],
): { value?: JsonObject; violations: ConfigViolation[] } {
  const value = cloneObject(item.persistedValue);
  const violations: ConfigViolation[] = [];

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
    if (field.type === "INTEGER" || field.type === "LONG") {
      const parsed = Number(text);
      if (text.trim() === "" || !Number.isFinite(parsed) || !Number.isInteger(parsed)) {
        violations.push({ path: field.path, code: "INVALID_NUMBER", message: "请输入有效整数" });
      } else {
        setPathValue(value, field.path, parsed);
      }
      continue;
    }
    if (field.type === "DECIMAL") {
      const parsed = Number(text);
      if (text.trim() === "" || !Number.isFinite(parsed)) {
        violations.push({ path: field.path, code: "INVALID_NUMBER", message: "请输入有效数字" });
      } else {
        setPathValue(value, field.path, parsed);
      }
      continue;
    }
    if (field.type === "STRING_LIST") {
      setPathValue(
        value,
        field.path,
        text
          .split(/\r?\n/)
          .map((entry) => entry.trim())
          .filter(Boolean),
      );
      continue;
    }
    if (field.type === "OBJECT") {
      try {
        setPathValue(value, field.path, JSON.parse(text) as JsonValue);
      } catch {
        violations.push({ path: field.path, code: "INVALID_JSON", message: "请输入有效 JSON" });
      }
      continue;
    }
    setPathValue(value, field.path, text);
  }

  return { value: violations.length ? undefined : value, violations };
}

function policyLabel(item: ConfigItem): string {
  return item.activationPolicy === "DYNAMIC" ? "动态生效" : "重启后生效";
}

function sourceLabel(item: ConfigItem): string {
  if (item.source === "DATABASE_OVERRIDE") return "数据库配置";
  if (item.source === "INVALID_DATABASE_FALLBACK") return "异常回退";
  return "代码默认值";
}

export function ConfigsAdminPage() {
  const canUpdate = hasResourceCodeAccess("config-system-edit");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [moduleDraft, setModuleDraft] = useState("");
  const [groupDraft, setGroupDraft] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedModule, setAppliedModule] = useState("");
  const [appliedGroup, setAppliedGroup] = useState("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [page, setPage] = useState<PageResult<ConfigItem>>(EMPTY_PAGE);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<DrawerMode>("detail");
  const [current, setCurrent] = useState<ConfigItem | null>(null);
  const [fieldInputs, setFieldInputs] = useState<Record<string, FieldInputValue>>({});
  const [touchedSensitiveFields, setTouchedSensitiveFields] = useState<string[]>([]);
  const [violations, setViolations] = useState<ConfigViolation[]>([]);
  const [reason, setReason] = useState("");
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const pageNoRef = useRef(pageNo);
  const pageSizeRef = useRef(pageSize);
  const keywordRef = useRef(appliedKeyword);
  const moduleRef = useRef(appliedModule);
  const groupRef = useRef(appliedGroup);
  pageNoRef.current = pageNo;
  pageSizeRef.current = pageSize;
  keywordRef.current = appliedKeyword;
  moduleRef.current = appliedModule;
  groupRef.current = appliedGroup;

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listConfigs({
        keyword: keywordRef.current,
        module: moduleRef.current,
        group: groupRef.current,
        pageNo: pageNoRef.current,
        pageSize: pageSizeRef.current,
      });
      setPage(result);
      setPageNo(result.pageNo || 1);
      setPageSize(result.pageSize || pageSizeRef.current);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [appliedGroup, appliedKeyword, appliedModule, pageNo, pageSize, reload]);

  function applyFilters() {
    setAppliedKeyword(keywordDraft.trim());
    setAppliedModule(moduleDraft.trim());
    setAppliedGroup(groupDraft.trim());
    setPageNo(1);
  }

  function resetFilters() {
    setKeywordDraft("");
    setModuleDraft("");
    setGroupDraft("");
    setAppliedKeyword("");
    setAppliedModule("");
    setAppliedGroup("");
    setPageNo(1);
  }

  async function openDrawer(item: ConfigItem, mode: DrawerMode) {
    setDrawerMode(mode);
    setCurrent(item);
    setDrawerOpen(true);
    setDrawerLoading(true);
    setViolations([]);
    setReason("");
    try {
      const detail = await getConfig(item.key);
      setCurrent(detail);
      setFieldInputs(initialFieldInputs(detail));
      setTouchedSensitiveFields([]);
    } finally {
      setDrawerLoading(false);
    }
  }

  function switchToEdit() {
    if (!current || !canUpdate || current.editPolicy !== "ADMIN_EDITABLE") return;
    setDrawerMode("edit");
    setFieldInputs(initialFieldInputs(current));
    setTouchedSensitiveFields([]);
    setViolations([]);
    setReason("");
  }

  function changeField(field: ConfigFieldSpec, value: FieldInputValue) {
    setFieldInputs((previous) => ({ ...previous, [field.path]: value }));
    if (field.sensitive) {
      setTouchedSensitiveFields((previous) =>
        previous.includes(field.path) ? previous : [...previous, field.path],
      );
    }
    setViolations((previous) => previous.filter((violation) => violation.path !== field.path));
  }

  async function save() {
    if (!current) return;
    const built = buildConfigValue(current, fieldInputs, touchedSensitiveFields);
    if (!built.value) {
      setViolations(built.violations);
      message.warning("请修正表单中的错误");
      return;
    }

    setSaving(true);
    try {
      const validation = await validateConfig(current.key, built.value);
      if (!validation.valid) {
        setViolations(validation.violations);
        message.warning("配置校验未通过");
        return;
      }
      setViolations([]);
      const confirmed = await bzConfirm({
        title: "保存配置",
        content:
          current.activationPolicy === "RESTART_REQUIRED"
            ? "该配置保存后需要重启服务才会生效，确认保存吗？"
            : "确认保存当前配置吗？",
        confirmText: "保存",
        cancelText: "取消",
      });
      if (!confirmed) return;
      const result = await updateConfig(current.key, {
        expectedRevision: current.persistedRevision,
        reason: reason.trim() || undefined,
        value: built.value,
      });
      message.success(result.pendingRestart ? "保存成功，配置将在重启后生效" : "保存成功");
      setDrawerOpen(false);
      await reload();
    } finally {
      setSaving(false);
    }
  }

  async function resetDefault(item: ConfigItem) {
    if (!canUpdate || item.editPolicy !== "ADMIN_EDITABLE" || !item.configured) return;
    const confirmed = await bzConfirm({
      title: "恢复默认配置",
      content: `确认将“${item.title}”恢复为代码默认值吗？`,
      confirmText: "恢复默认",
      cancelText: "取消",
    });
    if (!confirmed) return;
    setSaving(true);
    try {
      const result = await resetConfigDefault(item.key, {
        expectedRevision: item.persistedRevision,
        reason: reason.trim() || undefined,
      });
      message.success(result.pendingRestart ? "已恢复默认，重启后生效" : "已恢复默认配置");
      setDrawerOpen(false);
      await reload();
    } finally {
      setSaving(false);
    }
  }

  function getRowActions(row: ConfigItem): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      { key: "detail", label: "详情", tone: "detail", handler: () => void openDrawer(row, "detail") },
    ];
    if (canUpdate && row.editPolicy === "ADMIN_EDITABLE") {
      actions.push({ key: "edit", label: "编辑", tone: "edit", handler: () => void openDrawer(row, "edit") });
    }
    return actions;
  }

  const columns: BzTableColumn<ConfigItem>[] = [
    {
      key: "title",
      title: "配置项",
      minWidth: 260,
      render: (row) => (
        <div className="configs-title-cell">
          <strong>{row.title}</strong>
          <span className="configs-code-text">{row.key}</span>
        </div>
      ),
    },
    { key: "module", title: "模块", width: 120, render: (row) => row.module },
    { key: "group", title: "分组", width: 130, render: (row) => row.group },
    {
      key: "status",
      title: "状态",
      width: 130,
      render: (row) => <BzTag type={STATUS_META[row.status].type}>{STATUS_META[row.status].label}</BzTag>,
    },
    {
      key: "activationPolicy",
      title: "生效方式",
      width: 130,
      render: (row) => (
        <BzTag type={row.activationPolicy === "DYNAMIC" ? "success" : "warning"}>
          {policyLabel(row)}
        </BzTag>
      ),
    },
    { key: "revision", title: "版本", width: 90, render: (row) => row.persistedRevision },
  ];
  const actionsColumn = createAdminActionsColumn({ rows: page.elements, getActions: getRowActions });
  if (actionsColumn) columns.push(actionsColumn);

  const detailSections: AdminDetailSection[] = current
    ? [
        {
          title: "基本信息",
          fields: [
            { label: "配置名称", value: current.title },
            { label: "配置键", value: <code>{current.key}</code>, span: 2 },
            { label: "模块", value: current.module },
            { label: "分组", value: current.group },
            { label: "状态", value: STATUS_META[current.status].label },
            { label: "生效方式", value: policyLabel(current) },
            { label: "编辑策略", value: current.editPolicy === "READ_ONLY" ? "只读" : "管理员可编辑" },
            { label: "值来源", value: sourceLabel(current) },
            { label: "持久化版本", value: current.persistedRevision },
            { label: "生效版本", value: current.effectiveRevision },
            { label: "Schema 版本", value: current.schemaVersion },
            { label: "编辑器", value: current.editorId || "default" },
            { label: "说明", value: current.description || "-", span: "full", multiline: true },
          ],
        },
      ]
    : [];

  return (
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
            onSubmit={(event) => {
              event.preventDefault();
              applyFilters();
            }}
          >
            <QueryField label="关键词">
              <BzInput
                modelValue={keywordDraft}
                placeholder="配置键、名称或说明"
                clearable
                onValueChange={setKeywordDraft}
              />
            </QueryField>
            <QueryField label="模块">
              <BzInput modelValue={moduleDraft} placeholder="例如 system" clearable onValueChange={setModuleDraft} />
            </QueryField>
            <QueryField label="分组">
              <BzInput modelValue={groupDraft} placeholder="例如 security" clearable onValueChange={setGroupDraft} />
            </QueryField>
            <div className="admin-query-actions">
              <BzButton className="admin-filter-secondary" onClick={resetFilters}>重置</BzButton>
              <BzButton className="admin-filter-primary" buttonType="primary" onClick={applyFilters}>搜索</BzButton>
              {!querySingleRow ? (
                <button
                  className="admin-filter-toggle"
                  type="button"
                  aria-expanded={queryExpanded}
                  onClick={() => setQueryExpanded((value) => !value)}
                >
                  <span>{queryExpanded ? "收起" : "展开"}</span>
                  <i className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`} />
                </button>
              ) : null}
            </div>
          </form>
        </div>
      }
      queryTools={
        <AdminTableTools
          queryPanelVisible={queryPanelVisible}
          onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
          onRefresh={() => void reload()}
        />
      }
      table={
        <BzTable
          data={page.elements}
          columns={columns}
          rowKey="key"
          loading={loading}
          onRowDoubleClick={(row) => void openDrawer(row, "detail")}
        />
      }
      footer={
        page.totalElements > 0 ? (
          <div className="dict-pagination-bar admin-list-table-footer">
            <div className="dict-pagination-summary">共 {page.totalElements} 条记录</div>
            <div className="dict-pagination-right">
              <BzPagination
                total={page.totalElements}
                pageSize={pageSize}
                currentPage={pageNo}
                pageSizes={PAGE_SIZE_OPTIONS}
                onCurrentChange={setPageNo}
                onSizeChange={(size) => {
                  setPageSize(size);
                  setPageNo(1);
                }}
              />
            </div>
          </div>
        ) : null
      }
      overlays={
        <AdminEntityDrawer
          open={drawerOpen}
          title={`${drawerMode === "edit" ? "编辑" : "配置详情"}${current ? ` - ${current.title}` : ""}`}
          width="900px"
          loading={drawerLoading}
          className="configs-drawer"
          onClose={() => setDrawerOpen(false)}
          footer={
            <>
              {drawerMode === "detail" && current && canUpdate && current.editPolicy === "ADMIN_EDITABLE" ? (
                <BzButton buttonType="primary" onClick={switchToEdit}>编辑</BzButton>
              ) : null}
              {drawerMode === "edit" ? (
                <BzButton buttonType="primary" loading={saving} onClick={() => void save()}>保存</BzButton>
              ) : null}
              {current && canUpdate && current.editPolicy === "ADMIN_EDITABLE" && current.configured ? (
                <BzButton buttonType="warning" disabled={saving} onClick={() => void resetDefault(current)}>
                  恢复默认
                </BzButton>
              ) : null}
              <BzButton disabled={saving} onClick={() => setDrawerOpen(false)}>关闭</BzButton>
            </>
          }
        >
          {current ? (
            drawerMode === "detail" ? (
              <ConfigDetail item={current} sections={detailSections} />
            ) : (
              <ConfigEditor
                item={current}
                inputs={fieldInputs}
                violations={violations}
                reason={reason}
                onFieldChange={changeField}
                onReasonChange={setReason}
              />
            )
          ) : null}
        </AdminEntityDrawer>
      }
    />
  );
}

function QueryField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <BzFormItem className="admin-query-field">
      <div className="admin-query-field__label">{label}</div>
      <div className="admin-query-field__control">{children}</div>
    </BzFormItem>
  );
}

function ConfigDetail({ item, sections }: { item: ConfigItem; sections: AdminDetailSection[] }) {
  return (
    <div className="configs-drawer-content">
      {item.loadWarning ? <BzAlert title={item.loadWarning} type="warning" showIcon closable={false} /> : null}
      {item.pendingRestart ? (
        <BzAlert title="持久化配置尚未生效，需要重启服务。" type="warning" showIcon closable={false} />
      ) : null}
      <AdminDetailTable sections={sections} />
      <div className="configs-json-grid">
        <JsonPanel title="当前生效值" value={item.effectiveValue} />
        <JsonPanel title="持久化值" value={item.persistedValue} />
        <JsonPanel title="代码默认值" value={item.defaultValue} />
      </div>
    </div>
  );
}

function JsonPanel({ title, value }: { title: string; value: JsonValue }) {
  return (
    <section className="configs-json-panel">
      <div className="configs-json-panel__title">{title}</div>
      <pre>{formatJson(value)}</pre>
    </section>
  );
}

function ConfigEditor({
  item,
  inputs,
  violations,
  reason,
  onFieldChange,
  onReasonChange,
}: {
  item: ConfigItem;
  inputs: Record<string, FieldInputValue>;
  violations: ConfigViolation[];
  reason: string;
  onFieldChange: (field: ConfigFieldSpec, value: FieldInputValue) => void;
  onReasonChange: (value: string) => void;
}) {
  const sortedFields = [...item.fields].sort((left, right) => left.order - right.order);
  return (
    <div className="configs-drawer-content">
      {item.activationPolicy === "RESTART_REQUIRED" ? (
        <BzAlert title="该配置保存后需要重启服务才会生效。" type="warning" showIcon closable={false} />
      ) : null}
      {item.loadWarning ? <BzAlert title={item.loadWarning} type="error" showIcon closable={false} /> : null}
      <BzForm className="configs-editor-form">
        {sortedFields.map((field) => (
          <ConfigEditorField
            key={field.path}
            field={field}
            value={inputs[field.path] ?? (field.type === "BOOLEAN" ? false : "")}
            violations={violations.filter((violation) => violation.path === field.path)}
            sensitivePresent={Boolean(item.sensitiveValuePresence[field.path])}
            onChange={(value) => onFieldChange(field, value)}
          />
        ))}
        <BzFormItem label="变更原因" meta="选填，最多 500 个字符">
          <BzTextField
            type="textarea"
            rows={3}
            maxlength={500}
            showCounter
            modelValue={reason}
            placeholder="说明本次配置变更原因"
            onValueChange={onReasonChange}
          />
        </BzFormItem>
      </BzForm>
      {violations.some((violation) => !sortedFields.some((field) => field.path === violation.path)) ? (
        <div className="configs-global-errors">
          {violations
            .filter((violation) => !sortedFields.some((field) => field.path === violation.path))
            .map((violation, index) => <div key={`${violation.code}-${index}`}>{violation.message}</div>)}
        </div>
      ) : null}
    </div>
  );
}

function ConfigEditorField({
  field,
  value,
  violations,
  sensitivePresent,
  onChange,
}: {
  field: ConfigFieldSpec;
  value: FieldInputValue;
  violations: ConfigViolation[];
  sensitivePresent: boolean;
  onChange: (value: FieldInputValue) => void;
}) {
  const meta = [
    field.description,
    field.sensitive && sensitivePresent ? "已存在敏感值，留空表示保持不变" : "",
    violations.map((violation) => violation.message).join("；"),
  ]
    .filter(Boolean)
    .join(" · ");
  const disabled = field.readOnly;
  const textValue = typeof value === "string" ? value : String(value);

  return (
    <BzFormItem
      className={violations.length ? "configs-editor-field is-error" : "configs-editor-field"}
      label={
        <span>
          {field.title}
          {field.required ? <i className="configs-required">*</i> : null}
          <code>{field.path}</code>
        </span>
      }
      meta={meta || undefined}
    >
      {field.type === "BOOLEAN" ? (
        <BzSwitch
          modelValue={Boolean(value)}
          disabled={disabled}
          activeText="启用"
          inactiveText="停用"
          onValueChange={onChange}
        />
      ) : field.type === "ENUM" ? (
        <BzSelect
          modelValue={textValue}
          disabled={disabled}
          placeholder={field.placeholder || "请选择"}
          onValueChange={(next) => onChange(next ?? "")}
        >
          {field.options.map((option) => (
            <BzOption key={option.value} value={option.value} label={option.label} />
          ))}
        </BzSelect>
      ) : field.type === "STRING_LIST" || field.type === "OBJECT" ? (
        <BzTextField
          type="textarea"
          rows={field.type === "OBJECT" ? 10 : 5}
          modelValue={textValue}
          disabled={disabled}
          placeholder={field.type === "OBJECT" ? "请输入 JSON" : "每行输入一项"}
          onValueChange={onChange}
        />
      ) : (
        <BzInput
          modelValue={textValue}
          type={field.sensitive ? "password" : field.type === "STRING" ? "text" : "number"}
          disabled={disabled}
          placeholder={field.placeholder || (field.sensitive && sensitivePresent ? "留空保持原值" : "请输入")}
          onValueChange={onChange}
        />
      )}
    </BzFormItem>
  );
}
