"use client";

import { useEffect, useMemo, useState } from "react";

import { disableApi, listApis, publishApi } from "@admin/api/apis";
import { batchListDictOptions } from "@admin/api/dicts";
import { ApiTable } from "@admin/components/apis-admin/ApiTable";
import { BzButton, BzCard, BzForm, BzFormItem, BzInput, BzOption, BzPagination, BzSelect } from "@admin/components/bz";
import { hasResourceCodeAccess } from "@admin/core/registry/permissions-registry";
import type { ApiEntry } from "@admin/types/api-admin";
import type { DictItem } from "@admin/types/dict-admin";

const API_DICT_CODES = ["API_METHOD", "API_PROTOCOL", "API_ACCESS_TYPE"] as const;
const USER_TYPE_LABELS: Record<string, string> = { SYSTEM: "系统账号", INTERNAL: "账号", EXTERNAL: "用户", GUEST: "游客" };

export function ApisAdminPage() {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<ApiEntry[]>([]);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [queryCollapsed, setQueryCollapsed] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [moduleDraft, setModuleDraft] = useState<string | undefined>();
  const [statusDraft, setStatusDraft] = useState<string | undefined>();
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedModule, setAppliedModule] = useState<string | undefined>();
  const [appliedStatus, setAppliedStatus] = useState<string | undefined>();
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [methodLabelMap, setMethodLabelMap] = useState<Record<string, string>>({});
  const [protocolLabelMap, setProtocolLabelMap] = useState<Record<string, string>>({});
  const [accessTypeLabelMap, setAccessTypeLabelMap] = useState<Record<string, string>>({});

  const canPublish = hasResourceCodeAccess("api-manage-publish");
  const canDisable = hasResourceCodeAccess("api-manage-disable");
  const pageSizeOptions = [10, 20, 30, 50, 100, 200];

  useEffect(() => {
    void Promise.all([loadDictionaries(), reload()]);
  }, []);

  const moduleOptions = useMemo(
    () => Array.from(new Set(rows.map((item) => item.module?.trim()).filter((item): item is string => Boolean(item)))).sort((a, b) => a.localeCompare(b)),
    [rows],
  );

  const enrichedRows = useMemo(
    () =>
      rows.map((api) => ({
        ...api,
        protocolLabel: protocolLabelMap[api.protocol] || api.protocol,
        httpMethodLabel: methodLabelMap[api.httpMethod] || api.httpMethod,
        accessTypeLabel: accessTypeLabelMap[api.accessType] || api.accessType,
        userTypeLabels: resolveUserTypeLabels(api.userTypes),
        auditTooltip: buildAuditTooltip(api),
      })),
    [accessTypeLabelMap, methodLabelMap, protocolLabelMap, rows],
  );

  const filteredRows = useMemo(() => {
    const kw = appliedKeyword.trim().toLowerCase();
    const moduleValue = appliedModule?.trim() || "";
    const statusValue = appliedStatus?.trim() || "";
    return enrichedRows.filter((api) => {
      if (moduleValue && api.module !== moduleValue) return false;
      if (statusValue === "enabled" && !api.enabled) return false;
      if (statusValue === "disabled" && api.enabled) return false;
      if (!kw) return true;
      const text = [api.module, api.protocol, api.protocolLabel, api.httpMethod, api.httpMethodLabel, api.pathPattern, api.handlerClass, api.handlerMethod, api.accessType, api.accessTypeLabel, api.userTypes, ...(api.userTypeLabels || []), api.auditResource, api.auditAction, api.auditDescription]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return text.includes(kw);
    });
  }, [appliedKeyword, appliedModule, appliedStatus, enrichedRows]);

  const pagedRows = useMemo(() => filteredRows.slice((pageNo - 1) * pageSize, (pageNo - 1) * pageSize + pageSize), [filteredRows, pageNo, pageSize]);

  useEffect(() => {
    const totalPages = Math.max(1, Math.ceil(filteredRows.length / pageSize));
    if (pageNo > totalPages) setPageNo(totalPages);
  }, [filteredRows.length, pageNo, pageSize]);

  async function loadDictionaries() {
    try {
      const result = await batchListDictOptions([...API_DICT_CODES]);
      setMethodLabelMap(toLabelMap(result.API_METHOD));
      setProtocolLabelMap(toLabelMap(result.API_PROTOCOL));
      setAccessTypeLabelMap(toLabelMap(result.API_ACCESS_TYPE));
    } catch {
      setMethodLabelMap({});
      setProtocolLabelMap({});
      setAccessTypeLabelMap({});
    }
  }

  async function reload() {
    setLoading(true);
    try {
      setRows(await listApis());
    } finally {
      setLoading(false);
    }
  }

  function applyFilters() {
    setAppliedKeyword(keywordDraft);
    setAppliedModule(moduleDraft);
    setAppliedStatus(statusDraft);
    setPageNo(1);
  }

  function resetFilters() {
    setKeywordDraft("");
    setModuleDraft(undefined);
    setStatusDraft(undefined);
    setAppliedKeyword("");
    setAppliedModule(undefined);
    setAppliedStatus(undefined);
    setPageNo(1);
  }

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          {queryPanelVisible ? (
            <BzCard className="admin-panel admin-filter-card" shadow="never">
              <BzForm className={`admin-filter-form${queryCollapsed ? " is-collapsed" : ""}`} onSubmit={(event) => { event.preventDefault(); applyFilters(); }}>
                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">关键字</div>
                    <div className="admin-filter-control">
                      <BzInput modelValue={keywordDraft} placeholder="搜索模块、路径、处理器、访问类型" clearable onValueChange={setKeywordDraft} onKeyUp={(event) => event.key === "Enter" && applyFilters()} />
                    </div>
                  </div>
                </BzFormItem>
                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">模块</div>
                    <div className="admin-filter-control">
                      <BzSelect modelValue={moduleDraft} placeholder="请选择模块" clearable onValueChange={setModuleDraft}>
                        {moduleOptions.map((option) => (
                          <BzOption key={option} label={option} value={option} />
                        ))}
                      </BzSelect>
                    </div>
                  </div>
                </BzFormItem>
                {!queryCollapsed ? (
                  <BzFormItem className="admin-filter-item">
                    <div className="admin-filter-field">
                      <div className="admin-filter-label">状态</div>
                      <div className="admin-filter-control">
                        <BzSelect modelValue={statusDraft} placeholder="请选择状态" clearable onValueChange={setStatusDraft}>
                          <BzOption label="启用" value="enabled" />
                          <BzOption label="停用" value="disabled" />
                        </BzSelect>
                      </div>
                    </div>
                  </BzFormItem>
                ) : null}
                <div className="admin-filter-actions">
                  <BzButton className="admin-filter-secondary" onClick={resetFilters}>重置</BzButton>
                  <BzButton className="admin-filter-primary" buttonType="primary" nativeType="submit">搜索</BzButton>
                  <button className="admin-filter-toggle" type="button" onClick={() => setQueryCollapsed((value) => !value)}>
                    <span>{queryCollapsed ? "展开" : "收起"}</span>
                    <i className={`admin-filter-toggle__icon ${queryCollapsed ? "is-down" : "is-up"}`} aria-hidden="true" />
                  </button>
                </div>
              </BzForm>
            </BzCard>
          ) : null}

          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={<div className="admin-table-header"><div className="admin-table-title">接口列表</div><div className="admin-table-tools"><BzButton className="admin-toolbar-primary" buttonType="primary" onClick={() => void reload()}><span className="admin-toolbar-primary__content"><i className="admin-toolbar-primary__icon admin-toolbar-primary__icon--reload" aria-hidden="true" /><span>刷新接口</span></span></BzButton><button className={`admin-vben-circle-button${queryPanelVisible ? " is-active" : ""}`} type="button" title={queryPanelVisible ? "关闭搜索框" : "打开搜索框"} onClick={() => setQueryPanelVisible((value) => !value)}><i className="admin-vben-circle-button__icon admin-vben-circle-button__icon--search" aria-hidden="true" /></button><button className="admin-vben-circle-button" type="button" title="刷新列表" onClick={() => void reload()}><i className="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh" aria-hidden="true" /></button></div></div>}
          >
            <div className="admin-table-surface">
              <ApiTable rows={pagedRows} loading={loading} canPublish={canPublish} canDisable={canDisable} onPublish={(api) => void onPublish(api)} onDisable={(api) => void onDisable(api)} />
            </div>
            {filteredRows.length > 0 ? (
              <div className="dict-pagination-bar">
                <div className="dict-pagination-summary">共 {filteredRows.length} 条记录</div>
                <div className="dict-pagination-right">
                  <BzPagination
                    total={filteredRows.length}
                    pageSize={pageSize}
                    currentPage={pageNo}
                    pageSizes={pageSizeOptions}
                    onCurrentChange={setPageNo}
                    onSizeChange={(size) => {
                      if (!Number.isFinite(size) || size <= 0 || size === pageSize) return;
                      setPageSize(size);
                      setPageNo(1);
                    }}
                  />
                </div>
              </div>
            ) : null}
          </BzCard>
        </div>
      </div>
    </div>
  );

  async function onPublish(api: ApiEntry) {
    if (!canPublish) return;
    await publishApi(api.id);
    await reload();
  }

  async function onDisable(api: ApiEntry) {
    if (!canDisable) return;
    await disableApi(api.id);
    await reload();
  }
}

function toLabelMap(items?: DictItem[]): Record<string, string> {
  const map: Record<string, string> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = item.itemLabel || item.itemValue;
  }
  return map;
}

function resolveUserTypeLabels(raw?: string): string[] {
  return parseUserTypes(raw).map((code) => USER_TYPE_LABELS[code] || code);
}

function parseUserTypes(raw?: string): string[] {
  const normalized = raw?.trim();
  if (!normalized) return [];
  try {
    const parsed = JSON.parse(normalized);
    if (Array.isArray(parsed)) return parsed.map((item) => String(item).trim()).filter(Boolean);
  } catch {
    // ignore
  }
  return normalized.replace(/^\[|\]$/g, "").split(",").map((item) => item.replace(/^["'\s]+|["'\s]+$/g, "")).filter(Boolean);
}

function buildAuditTooltip(api: ApiEntry): string {
  if (!api.auditDeclared) return "";
  const lines = [api.auditResource ? `审计资源：${api.auditResource}` : "", api.auditAction ? `审计动作：${api.auditAction}` : "", api.auditDescription ? `审计描述：${api.auditDescription}` : ""].filter(Boolean);
  return lines.join("\n");
}
