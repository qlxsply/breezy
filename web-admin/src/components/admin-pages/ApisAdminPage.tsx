"use client";

import { disableApi, pageApis, publishApi } from "@admin/api/apis";
import { batchListDictOptions } from "@admin/api/dicts";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { ApiTable } from "@admin/components/apis-admin/ApiTable";
import { BzButton, BzCard, BzFormItem, BzInput, BzOption, BzPagination, BzSelect } from "@admin/components/bz";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { ApiEntry } from "@admin/types/api-admin";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import { useEffect, useMemo, useRef, useState } from "react";

const API_DICT_CODES = [
  "API_METHOD",
  "API_PROTOCOL",
  "API_ACCESS_TYPE",
  "USER_TYPE",
  "API_STATUS",
  "API_PERMISSION_DECLARED",
  "API_AUDIT_DECLARED",
] as const;

export function ApisAdminPage() {
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState<PageResult<ApiEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [queryExpanded, setQueryExpanded] = useState(false);
  const [querySingleRow, setQuerySingleRow] = useState(true);
  const [moduleDraft, setModuleDraft] = useState("");
  const [pathPatternDraft, setPathPatternDraft] = useState("");
  const [handlerClassDraft, setHandlerClassDraft] = useState("");
  const [handlerMethodDraft, setHandlerMethodDraft] = useState("");
  const [permissionDeclaredDraft, setPermissionDeclaredDraft] = useState("");
  const [accessTypeDraft, setAccessTypeDraft] = useState("");
  const [userTypeDraft, setUserTypeDraft] = useState("");
  const [auditDeclaredDraft, setAuditDeclaredDraft] = useState("");
  const [statusDraft, setStatusDraft] = useState("");
  const [appliedModule, setAppliedModule] = useState("");
  const [appliedPathPattern, setAppliedPathPattern] = useState("");
  const [appliedHandlerClass, setAppliedHandlerClass] = useState("");
  const [appliedHandlerMethod, setAppliedHandlerMethod] = useState("");
  const [appliedPermissionDeclared, setAppliedPermissionDeclared] = useState("");
  const [appliedAccessType, setAppliedAccessType] = useState("");
  const [appliedUserType, setAppliedUserType] = useState("");
  const [appliedAuditDeclared, setAppliedAuditDeclared] = useState("");
  const [appliedStatus, setAppliedStatus] = useState("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [methodLabelMap, setMethodLabelMap] = useState<Record<string, string>>({});
  const [protocolLabelMap, setProtocolLabelMap] = useState<Record<string, string>>({});
  const [accessTypeLabelMap, setAccessTypeLabelMap] = useState<Record<string, string>>({});
  const [userTypeLabelMap, setUserTypeLabelMap] = useState<Record<string, string>>({});
  const [statusLabelMap, setStatusLabelMap] = useState<Record<string, string>>({});
  const [permissionDeclaredLabelMap, setPermissionDeclaredLabelMap] = useState<Record<string, string>>({});
  const [auditDeclaredLabelMap, setAuditDeclaredLabelMap] = useState<Record<string, string>>({});

  const canPublish = hasResourceCodeAccess("api-manage-publish");
  const canDisable = hasResourceCodeAccess("api-manage-disable");
  const pageSizeOptions = [10, 20, 30, 50, 100, 200];
  const queryCardRef = useRef<HTMLDivElement | null>(null);
  const queryGridRef = useRef<HTMLFormElement | null>(null);

  useEffect(() => {
    void loadDictionaries();
  }, []);

  useEffect(() => {
    void reload();
  }, [
    appliedModule,
    appliedPathPattern,
    appliedHandlerClass,
    appliedHandlerMethod,
    appliedPermissionDeclared,
    appliedAccessType,
    appliedUserType,
    appliedAuditDeclared,
    appliedStatus,
    pageNo,
    pageSize,
  ]);

  useEffect(() => {
    if (!queryPanelVisible) {
      return;
    }

    const card = queryCardRef.current;
    const grid = queryGridRef.current;
    if (!card || !grid) {
      return;
    }

    let frame = 0;

    const refreshCollapseState = () => {
      cancelAnimationFrame(frame);

      frame = window.requestAnimationFrame(() => {
        const fields = Array.from(grid.querySelectorAll<HTMLElement>(".admin-query-field"));
        const actions = grid.querySelector<HTMLElement>(".admin-query-actions");
        const items = actions ? [...fields, actions] : fields;

        if (items.length === 0) {
          card.style.removeProperty("--admin-query-collapsed-height");
          card.style.removeProperty("--admin-query-expanded-height");
          setQuerySingleRow(true);
          return;
        }

        const previousMaxHeight = grid.style.maxHeight;
        const previousActionGridRow = actions?.style.gridRow || "";
        const previousActionGridColumn = actions?.style.gridColumn || "";

        grid.style.maxHeight = "none";

        // Measure natural flow first so the action cell sits after all fields.
        if (actions) {
          actions.style.gridRow = "auto";
          actions.style.gridColumn = "auto";
        }

        const rowTops = [...new Set(items.map((item) => Math.round(item.offsetTop)))].sort((left, right) => left - right);
        const firstRowTop = rowTops[0] || 0;
        const firstRowItems = items.filter((item) => Math.round(item.offsetTop) === firstRowTop);
        const firstRowBottom = Math.max(...firstRowItems.map((item) => item.offsetTop + item.offsetHeight), 0);

        const collapsedHeight = Math.max(firstRowBottom - firstRowTop, 0);
        const expandedHeight = grid.scrollHeight;

        grid.style.maxHeight = previousMaxHeight;

        if (actions) {
          actions.style.gridRow = previousActionGridRow;
          actions.style.gridColumn = previousActionGridColumn;
        }

        card.style.setProperty("--admin-query-collapsed-height", `${collapsedHeight}px`);
        card.style.setProperty("--admin-query-expanded-height", `${expandedHeight}px`);

        const nextSingleRow = rowTops.length <= 1;
        setQuerySingleRow(nextSingleRow);

        if (nextSingleRow) {
          setQueryExpanded(false);
        }
      });
    };

    refreshCollapseState();

    const observer = new ResizeObserver(() => {
      refreshCollapseState();
    });

    observer.observe(grid);
    window.addEventListener("resize", refreshCollapseState);

    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
      window.removeEventListener("resize", refreshCollapseState);
    };
  }, [queryPanelVisible]);

  const enrichedRows = useMemo(
    () =>
      page.elements.map((api) => ({
        ...api,
        protocolLabel: protocolLabelMap[api.protocol] || api.protocol,
        httpMethodLabel: methodLabelMap[api.httpMethod] || api.httpMethod,
        accessTypeLabel: accessTypeLabelMap[api.accessType] || api.accessType,
        userTypeLabels: resolveUserTypeLabels(api.userTypes, userTypeLabelMap),
        auditTooltip: buildAuditTooltip(api),
      })),
    [accessTypeLabelMap, methodLabelMap, page.elements, protocolLabelMap, userTypeLabelMap],
  );

  const accessTypeOptions = useMemo(() => toOptions(accessTypeLabelMap), [accessTypeLabelMap]);
  const userTypeOptions = useMemo(() => toOptions(userTypeLabelMap), [userTypeLabelMap]);
  const permissionDeclaredOptions = useMemo(() => toOptions(permissionDeclaredLabelMap), [permissionDeclaredLabelMap]);
  const auditDeclaredOptions = useMemo(() => toOptions(auditDeclaredLabelMap), [auditDeclaredLabelMap]);
  const statusOptions = useMemo(
    () => toOptions(statusLabelMap).filter((item) => item.value === "ACTIVE" || item.value === "DISABLED"),
    [statusLabelMap],
  );

  async function loadDictionaries() {
    try {
      const result = await batchListDictOptions([...API_DICT_CODES]);
      setMethodLabelMap(toLabelMap(result.API_METHOD));
      setProtocolLabelMap(toLabelMap(result.API_PROTOCOL));
      setAccessTypeLabelMap(toLabelMap(result.API_ACCESS_TYPE));
      setUserTypeLabelMap(toLabelMap(result.USER_TYPE));
      setStatusLabelMap(toLabelMap(result.API_STATUS));
      setPermissionDeclaredLabelMap(toLabelMap(result.API_PERMISSION_DECLARED));
      setAuditDeclaredLabelMap(toLabelMap(result.API_AUDIT_DECLARED));
    } catch {
      setMethodLabelMap({});
      setProtocolLabelMap({});
      setAccessTypeLabelMap({});
      setUserTypeLabelMap({
        SYSTEM: "系统账号",
        INTERNAL: "账号",
        EXTERNAL: "用户",
        GUEST: "游客",
      });
      setStatusLabelMap({ ACTIVE: "启用", DISABLED: "停用" });
      setPermissionDeclaredLabelMap({ DECLARED: "已声明", UNDECLARED: "未声明" });
      setAuditDeclaredLabelMap({ ENABLED: "已开启", DISABLED: "未开启" });
    }
  }

  async function reload() {
    setLoading(true);
    try {
      const nextPage = await pageApis({
        module: appliedModule,
        pathPattern: appliedPathPattern,
        handlerClass: appliedHandlerClass,
        handlerMethod: appliedHandlerMethod,
        permissionDeclared: appliedPermissionDeclared,
        accessType: appliedAccessType,
        userType: appliedUserType,
        auditDeclared: appliedAuditDeclared,
        status: appliedStatus,
        pageNo,
        pageSize,
      });
      setPage(nextPage);
    } finally {
      setLoading(false);
    }
  }

  function applyFilters() {
    setAppliedModule(moduleDraft.trim());
    setAppliedPathPattern(pathPatternDraft.trim());
    setAppliedHandlerClass(handlerClassDraft.trim());
    setAppliedHandlerMethod(handlerMethodDraft.trim());
    setAppliedPermissionDeclared(permissionDeclaredDraft);
    setAppliedAccessType(accessTypeDraft);
    setAppliedUserType(userTypeDraft);
    setAppliedAuditDeclared(auditDeclaredDraft);
    setAppliedStatus(statusDraft);
    setPageNo(1);
  }

  function resetFilters() {
    setModuleDraft("");
    setPathPatternDraft("");
    setHandlerClassDraft("");
    setHandlerMethodDraft("");
    setPermissionDeclaredDraft("");
    setAccessTypeDraft("");
    setUserTypeDraft("");
    setAuditDeclaredDraft("");
    setStatusDraft("");
    setAppliedModule("");
    setAppliedPathPattern("");
    setAppliedHandlerClass("");
    setAppliedHandlerMethod("");
    setAppliedPermissionDeclared("");
    setAppliedAccessType("");
    setAppliedUserType("");
    setAppliedAuditDeclared("");
    setAppliedStatus("");
    setPageNo(1);
  }

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          {queryPanelVisible ? (
            <BzCard
              className="admin-panel admin-filter-card"
              shadow="never"
            >
              <div
                ref={queryCardRef}
                className={[
                  "admin-query-layout",
                  querySingleRow ? "is-single-row" : queryExpanded ? "is-expanded" : "is-collapsed",
                ].join(" ")}
              >
                <div className="admin-query-header">
                  <div className="admin-query-title">筛选条件</div>
                </div>
                <form
                  ref={queryGridRef}
                  className="bz-form admin-query-grid"
                  onSubmit={(event) => {
                    event.preventDefault();
                    applyFilters();
                  }}
                >
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">模块</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={moduleDraft}
                        placeholder="请输入模块"
                        clearable
                        onValueChange={setModuleDraft}
                        onKeyUp={(event) => event.key === "Enter" && applyFilters()}
                      />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">路径</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={pathPatternDraft}
                        placeholder="请输入路径"
                        clearable
                        onValueChange={setPathPatternDraft}
                        onKeyUp={(event) => event.key === "Enter" && applyFilters()}
                      />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">处理类</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={handlerClassDraft}
                        placeholder="请输入处理类"
                        clearable
                        onValueChange={setHandlerClassDraft}
                        onKeyUp={(event) => event.key === "Enter" && applyFilters()}
                      />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">处理方法</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={handlerMethodDraft}
                        placeholder="请输入处理方法"
                        clearable
                        onValueChange={setHandlerMethodDraft}
                        onKeyUp={(event) => event.key === "Enter" && applyFilters()}
                      />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">权限声明</div>
                    <div className="admin-query-field__control">
                      <BzSelect
                        modelValue={permissionDeclaredDraft || undefined}
                        placeholder="请选择权限声明"
                        clearable
                        onValueChange={(value) => setPermissionDeclaredDraft(value || "")}
                      >
                        {permissionDeclaredOptions.map((option) => (
                          <BzOption
                            key={option.value}
                            label={option.label}
                            value={option.value}
                          />
                        ))}
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">访问类型</div>
                    <div className="admin-query-field__control">
                      <BzSelect
                        modelValue={accessTypeDraft || undefined}
                        placeholder="请选择访问类型"
                        clearable
                        onValueChange={(value) => setAccessTypeDraft(value || "")}
                      >
                        {accessTypeOptions.map((option) => (
                          <BzOption
                            key={option.value}
                            label={option.label}
                            value={option.value}
                          />
                        ))}
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">用户类型</div>
                    <div className="admin-query-field__control">
                      <BzSelect
                        modelValue={userTypeDraft || undefined}
                        placeholder="请选择用户类型"
                        clearable
                        onValueChange={(value) => setUserTypeDraft(value || "")}
                      >
                        {userTypeOptions.map((option) => (
                          <BzOption
                            key={option.value}
                            label={option.label}
                            value={option.value}
                          />
                        ))}
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">审计</div>
                    <div className="admin-query-field__control">
                      <BzSelect
                        modelValue={auditDeclaredDraft || undefined}
                        placeholder="请选择审计状态"
                        clearable
                        onValueChange={(value) => setAuditDeclaredDraft(value || "")}
                      >
                        {auditDeclaredOptions.map((option) => (
                          <BzOption
                            key={option.value}
                            label={option.label}
                            value={option.value}
                          />
                        ))}
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">状态</div>
                    <div className="admin-query-field__control">
                      <BzSelect
                        modelValue={statusDraft || undefined}
                        placeholder="请选择状态"
                        clearable
                        onValueChange={(value) => setStatusDraft(value || "")}
                      >
                        {statusOptions.map((option) => (
                          <BzOption
                            key={option.value}
                            label={option.label}
                            value={option.value}
                          />
                        ))}
                      </BzSelect>
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
            </BzCard>
          ) : null}

          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">接口列表</div>
                <div className="admin-table-tools">
                  <AdminTableTools
                    queryPanelVisible={queryPanelVisible}
                    onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
                    onRefresh={() => void reload()}
                  />
                </div>
              </div>
            }
          >
            <div className="admin-table-surface">
              <ApiTable
                rows={enrichedRows}
                loading={loading}
                canPublish={canPublish}
                canDisable={canDisable}
                onPublish={(api) => void onPublish(api)}
                onDisable={(api) => void onDisable(api)}
              />
            </div>
            {page.totalElements > 0 ? (
              <div className="dict-pagination-bar">
                <div className="dict-pagination-summary">共 {page.totalElements} 条记录</div>
                <div className="dict-pagination-right">
                  <BzPagination
                    total={page.totalElements}
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

function resolveUserTypeLabels(raw: string | undefined, labelMap: Record<string, string>): string[] {
  return parseUserTypes(raw).map((code) => labelMap[code] || code);
}

function toOptions(labelMap: Record<string, string>) {
  return Object.entries(labelMap).map(([value, label]) => ({ value, label }));
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
  return normalized
    .replace(/^\[|\]$/g, "")
    .split(",")
    .map((item) => item.replace(/^["'\s]+|["'\s]+$/g, ""))
    .filter(Boolean);
}

function buildAuditTooltip(api: ApiEntry): string {
  if (!api.auditDeclared) return "";
  const lines = [
    api.auditResource ? `审计资源：${api.auditResource}` : "",
    api.auditAction ? `审计动作：${api.auditAction}` : "",
    api.auditDescription ? `审计描述：${api.auditDescription}` : "",
  ].filter(Boolean);
  return lines.join("\n");
}
