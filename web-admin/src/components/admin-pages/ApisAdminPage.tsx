"use client";

import { disableApi, getApi, pageApis, publishApi, updateApiSortOptions } from "@admin/api/apis";
import { batchListDictOptions } from "@admin/api/dicts";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { AdminListPageTemplate } from "@admin/components/admin/AdminListPageTemplate";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import { ApiTable } from "@admin/components/apis-admin/ApiTable";
import {
  BzButton,
  BzFormItem,
  BzIconActionButton,
  BzInput,
  BzOption,
  BzPagination,
  BzSelect,
  BzSwitch,
  BzTag,
} from "@admin/components/bz";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { ApiEntry } from "@admin/types/api-admin";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import { type CSSProperties, type DragEvent, type ReactNode, useEffect, useMemo, useState } from "react";

const API_DICT_CODES = [
  "API_METHOD",
  "API_PROTOCOL",
  "API_ACCESS_TYPE",
  "USER_TYPE",
  "API_STATUS",
  "API_PERMISSION_DECLARED",
  "API_AUDIT_DECLARED",
] as const;

interface ApiSortAllowedField {
  field: string;
  column: string;
}

interface ApiDefaultSort {
  field: string;
  direction: "ASC" | "DESC";
}

interface ApiSortOptions {
  enabled: boolean;
  allowed: ApiSortAllowedField[];
  defaults: ApiDefaultSort[];
}

interface ApiSortAllowedDraft extends ApiSortAllowedField {
  uid: string;
}

interface ApiDefaultSortDraft extends ApiDefaultSort {
  uid: string;
}

type SortDraftType = "allowed" | "defaults";

let sortUidSeed = 0;

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
  const [permissionDeclaredLabelMap, setPermissionDeclaredLabelMap] = useState<
    Record<string, string>
  >({});
  const [auditDeclaredLabelMap, setAuditDeclaredLabelMap] = useState<Record<string, string>>({});
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailItem, setDetailItem] = useState<ApiEntry | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [maintainOpen, setMaintainOpen] = useState(false);
  const [maintainItem, setMaintainItem] = useState<ApiEntry | null>(null);
  const [maintainLoading, setMaintainLoading] = useState(false);
  const [sortSaving, setSortSaving] = useState(false);
  const [sortEnabled, setSortEnabled] = useState(false);
  const [allowedSortDrafts, setAllowedSortDrafts] = useState<ApiSortAllowedDraft[]>([]);
  const [defaultSortDrafts, setDefaultSortDrafts] = useState<ApiDefaultSortDraft[]>([]);
  const [disabledSortCache, setDisabledSortCache] = useState<{
    allowed: ApiSortAllowedDraft[];
    defaults: ApiDefaultSortDraft[];
  } | null>(null);
  const [dragState, setDragState] = useState<{ type: SortDraftType; index: number } | null>(null);

  const canPublish = hasResourceCodeAccess("api-manage-publish");
  const canDisable = hasResourceCodeAccess("api-manage-disable");
  const canMaintain = hasResourceCodeAccess("api-manage-edit");
  const pageSizeOptions = [10, 20, 30, 50, 100, 200];
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

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

  const enrichedRows = useMemo(
    () =>
      page.elements.map((api) => ({
        ...api,
        protocolLabel: protocolLabelMap[api.protocol] || api.protocol,
        httpMethodLabel: methodLabelMap[api.httpMethod] || api.httpMethod,
        accessTypeLabel: accessTypeLabelMap[api.accessType] || api.accessType,
        userTypeLabel: resolveUserTypeLabel(api.userType, userTypeLabelMap),
        auditTooltip: buildAuditTooltip(api),
      })),
    [accessTypeLabelMap, methodLabelMap, page.elements, protocolLabelMap, userTypeLabelMap],
  );

  const accessTypeOptions = useMemo(() => toOptions(accessTypeLabelMap), [accessTypeLabelMap]);
  const userTypeOptions = useMemo(() => toOptions(userTypeLabelMap), [userTypeLabelMap]);
  const permissionDeclaredOptions = useMemo(
    () => toOptions(permissionDeclaredLabelMap),
    [permissionDeclaredLabelMap],
  );
  const auditDeclaredOptions = useMemo(
    () => toOptions(auditDeclaredLabelMap),
    [auditDeclaredLabelMap],
  );
  const statusOptions = useMemo(
    () =>
      toOptions(statusLabelMap).filter(
        (item) => item.value === "ACTIVE" || item.value === "DISABLED",
      ),
    [statusLabelMap],
  );

  const detailSortOptions = useMemo(
    () => parseSortOptions(detailItem?.sortOptionsJson),
    [detailItem?.sortOptionsJson],
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
        ADMIN: "账号",
        USER: "用户",
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

  function renderBasicSection(api: ApiEntry, ariaLabel: string) {
    return (
      <section className="role-manage-section">
        <div className="role-manage-section__head">
          <div className="role-manage-section__title">基础信息</div>
        </div>
        <div className="role-info-table-wrap">
          <table
            className="role-info-table"
            aria-label={ariaLabel}
          >
            <tbody>
              <tr>
                <th>模块</th>
                <td>{api.module || "-"}</td>
                <th>协议</th>
                <td>{api.protocolLabel || api.protocol || "-"}</td>
                <th>方法</th>
                <td>{api.httpMethodLabel || api.httpMethod || "-"}</td>
              </tr>
              <tr>
                <th>访问类型</th>
                <td>{api.accessTypeLabel || api.accessType || "-"}</td>
                <th>用户类型</th>
                <td>{api.userTypeLabel || api.userType || "-"}</td>
                <th>接口状态</th>
                <td>{api.enabled ? "启用" : "停用"}</td>
              </tr>
              <tr>
                <th>路径</th>
                <td colSpan={5}>{api.pathPattern || "-"}</td>
              </tr>
              <tr>
                <th>处理类</th>
                <td colSpan={5}>{api.handlerClass || "-"}</td>
              </tr>
              <tr>
                <th>处理方法</th>
                <td colSpan={5}>{api.handlerMethod || "-"}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    );
  }

  function renderAuditSection(api: ApiEntry, ariaLabel: string) {
    return (
      <section className="role-manage-section">
        <div className="role-manage-section__head">
          <div className="role-manage-section__title">权限与审计</div>
        </div>
        <div className="role-info-table-wrap">
          <table
            className="role-info-table"
            aria-label={ariaLabel}
          >
            <tbody>
              <tr>
                <th>权限声明</th>
                <td>{api.permissionDeclared ? "已声明" : "未声明"}</td>
                <th>审计状态</th>
                <td>{api.auditDeclared ? "已开启" : "未开启"}</td>
                <th>审计资源</th>
                <td>{api.auditResource || "-"}</td>
              </tr>
              <tr>
                <th>审计动作</th>
                <td colSpan={5}>{api.auditAction || "-"}</td>
              </tr>
              <tr>
                <th>审计说明</th>
                <td colSpan={5}>{api.auditDescription || "-"}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    );
  }

  function renderSortDetailSection(options: ApiSortOptions) {
    return (
      <section className="role-manage-section">
        <div className="role-manage-section__head">
          <div className="role-manage-section__title">排序规则</div>
          <BzTag
            size="small"
            type={options.enabled ? "success" : "info"}
          >
            {options.enabled ? "已开启" : "未开启"}
          </BzTag>
        </div>
        {options.enabled ? (
          <>
            {renderAllowedSortTable(options.allowed)}
            {renderDefaultSortTable(options.defaults)}
          </>
        ) : null}
      </section>
    );
  }

  function renderSortEditorSection() {
    return (
      <section className="role-manage-section">
        <div className="role-manage-section__head">
          <div className="role-manage-section__title">排序规则</div>
          <div className="api-sort-switch-line">
            <BzSwitch
              modelValue={sortEnabled}
              onValueChange={toggleSortEnabled}
            />
            <span className="api-sort-switch-label">{sortEnabled ? "已开启" : "未开启"}</span>
          </div>
        </div>
        {sortEnabled ? (
          <>
            {renderAllowedEditor()}
            {renderDefaultEditor()}
          </>
        ) : null}
      </section>
    );
  }

  function renderAllowedEditor() {
    return (
      <SortRuleEditTable
        title="候选排序字段"
        addTitle="添加字段"
        emptyText="暂无候选字段"
        rowCount={allowedSortDrafts.length}
        dragType="allowed"
        headers={["请求字段 field", "数据库列 column"]}
        getRowKey={(index) => allowedSortDrafts[index].uid}
        onAdd={addAllowedSortField}
        onRemove={removeAllowedSortField}
        onDragStart={(index) => setDragState({ type: "allowed", index })}
        onDragEnd={() => setDragState(null)}
        onDragOver={handleSortDragOver}
        renderRow={(index) => {
          const item = allowedSortDrafts[index];
          return [
            <input
              className="api-sort-input"
              key="field"
              value={item.field}
              placeholder="createdAt"
              onChange={(event) => updateAllowedField(index, event.target.value)}
            />,
            <input
              className="api-sort-input is-code"
              key="column"
              value={item.column}
              placeholder="created_at"
              onChange={(event) => updateAllowedColumn(index, event.target.value)}
            />,
          ];
        }}
      />
    );
  }

  function renderDefaultEditor() {
    return (
      <SortRuleEditTable
        title="默认排序规则"
        addTitle="添加规则"
        emptyText="暂无默认排序规则"
        rowCount={defaultSortDrafts.length}
        dragType="defaults"
        headers={["请求字段 field", "排序方向"]}
        getRowKey={(index) => defaultSortDrafts[index].uid}
        addDisabled={!canAddDefaultSort()}
        onAdd={addDefaultSortRule}
        onRemove={removeDefaultSortRule}
        onDragStart={(index) => setDragState({ type: "defaults", index })}
        onDragEnd={() => setDragState(null)}
        onDragOver={handleSortDragOver}
        renderRow={(index) => {
          const item = defaultSortDrafts[index];
          return [
            <select
              className="api-sort-input is-code"
              key="field"
              value={item.field}
              onChange={(event) => updateDefaultSortField(index, event.target.value)}
            >
              {allowedSortDrafts
                .filter((field) => field.field.trim())
                .map((field) => (
                  <option
                    key={field.uid}
                    value={field.field}
                  >
                    {field.field}
                  </option>
                ))}
            </select>,
            <div
              className="api-sort-direction-group"
              key="direction"
            >
              <button
                className={`api-sort-direction-btn${item.direction === "ASC" ? " is-active" : ""}`}
                type="button"
                aria-pressed={item.direction === "ASC"}
                onClick={() => updateDefaultSortDirection(index, "ASC")}
              >
                ↑
              </button>
              <button
                className={`api-sort-direction-btn${item.direction === "DESC" ? " is-active" : ""}`}
                type="button"
                aria-pressed={item.direction === "DESC"}
                onClick={() => updateDefaultSortDirection(index, "DESC")}
              >
                ↓
              </button>
            </div>,
          ];
        }}
      />
    );
  }

  function toggleSortEnabled(nextEnabled: boolean) {
    setSortEnabled(nextEnabled);
    if (!nextEnabled) {
      setDisabledSortCache({
        allowed: allowedSortDrafts,
        defaults: defaultSortDrafts,
      });
      setAllowedSortDrafts([]);
      setDefaultSortDrafts([]);
      return;
    }
    if (disabledSortCache) {
      setAllowedSortDrafts(disabledSortCache.allowed);
      setDefaultSortDrafts(disabledSortCache.defaults);
      setDisabledSortCache(null);
    }
  }

  function addAllowedSortField() {
    setAllowedSortDrafts((items) => [...items, { uid: nextSortUid(), field: "", column: "" }]);
  }

  function updateAllowedField(index: number, value: string) {
    setAllowedSortDrafts((items) => {
      const oldField = items[index]?.field;
      const nextField = value.trim();
      const next = items.map((item, itemIndex) =>
        itemIndex === index ? { ...item, field: nextField } : item,
      );
      if (oldField !== undefined) {
        setDefaultSortDrafts((rules) =>
          rules.map((rule) => (rule.field === oldField ? { ...rule, field: nextField } : rule)),
        );
      }
      return next;
    });
  }

  function updateAllowedColumn(index: number, value: string) {
    setAllowedSortDrafts((items) =>
      items.map((item, itemIndex) =>
        itemIndex === index ? { ...item, column: value.trim() } : item,
      ),
    );
  }

  function removeAllowedSortField(index: number) {
    const removedField = allowedSortDrafts[index]?.field;
    setAllowedSortDrafts((items) => items.filter((_, itemIndex) => itemIndex !== index));
    setDefaultSortDrafts((items) => items.filter((item) => item.field !== removedField));
  }

  function canAddDefaultSort() {
    const used = new Set(defaultSortDrafts.map((item) => item.field));
    return allowedSortDrafts.some((item) => item.field.trim() && !used.has(item.field));
  }

  function addDefaultSortRule() {
    const used = new Set(defaultSortDrafts.map((item) => item.field));
    const candidate = allowedSortDrafts.find((item) => item.field.trim() && !used.has(item.field));
    if (!candidate) return;
    setDefaultSortDrafts((items) => [
      ...items,
      { uid: nextSortUid(), field: candidate.field, direction: "ASC" },
    ]);
  }

  function updateDefaultSortField(index: number, field: string) {
    setDefaultSortDrafts((items) =>
      items.map((item, itemIndex) => (itemIndex === index ? { ...item, field } : item)),
    );
  }

  function updateDefaultSortDirection(index: number, direction: "ASC" | "DESC") {
    setDefaultSortDrafts((items) =>
      items.map((item, itemIndex) => (itemIndex === index ? { ...item, direction } : item)),
    );
  }

  function removeDefaultSortRule(index: number) {
    setDefaultSortDrafts((items) => items.filter((_, itemIndex) => itemIndex !== index));
  }

  function handleSortDragOver(
    event: DragEvent<HTMLDivElement>,
    type: SortDraftType,
    targetIndex: number,
  ) {
    if (!dragState || dragState.type !== type || dragState.index === targetIndex) return;
    event.preventDefault();
    if (type === "allowed") {
      setAllowedSortDrafts((items) => moveSortItem(items, dragState.index, targetIndex));
    } else {
      setDefaultSortDrafts((items) => moveSortItem(items, dragState.index, targetIndex));
    }
    setDragState({ type, index: targetIndex });
  }

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
        }
        queryTools={
          <AdminTableTools
            queryPanelVisible={queryPanelVisible}
            onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
            onRefresh={() => void reload()}
          />
        }
        table={
          <ApiTable
            rows={enrichedRows}
            loading={loading}
            canDetail
            canMaintain={canMaintain}
            canPublish={canPublish}
            canDisable={canDisable}
            onDetail={(api) => void openDetail(api)}
            onMaintain={(api) => void openMaintain(api)}
            onPublish={(api) => void onPublish(api)}
            onDisable={(api) => void onDisable(api)}
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
          ) : null
        }
        overlays={
          <>
            <AdminEntityDrawer
            open={detailOpen}
            title="接口详情"
            width="1180px"
            loading={detailLoading}
            className="role-manage-drawer"
            onClose={() => {
              setDetailOpen(false);
              setDetailItem(null);
            }}
            footer={
              <BzButton
                onClick={() => {
                  setDetailOpen(false);
                  setDetailItem(null);
                }}
              >
                关闭
              </BzButton>
            }
          >
            {detailItem ? (
              <div className="role-manage-shell">
                {renderBasicSection(detailItem, "接口基础信息")}
                {renderAuditSection(detailItem, "接口权限与审计")}
                {renderSortDetailSection(detailSortOptions)}
              </div>
            ) : null}
            </AdminEntityDrawer>
            <AdminEntityDrawer
            open={maintainOpen}
            title="接口维护"
            width="980px"
            loading={maintainLoading}
            className="role-manage-drawer"
            onClose={() => {
              if (sortSaving) return;
              closeMaintain();
            }}
            footer={
              <>
                <BzButton
                  disabled={sortSaving}
                  onClick={closeMaintain}
                >
                  取消
                </BzButton>
                <BzButton
                  buttonType="primary"
                  loading={sortSaving}
                  onClick={() => void saveSortOptions()}
                >
                  保存
                </BzButton>
              </>
            }
          >
            {maintainItem ? (
              <div className="role-manage-shell">
                {renderBasicSection(maintainItem, "维护排序规则基础信息")}
                {renderAuditSection(maintainItem, "维护排序规则权限与审计")}
                {renderSortEditorSection()}
              </div>
            ) : null}
            </AdminEntityDrawer>
          </>
        }
      />
    </>
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

  function enrichApi(api: ApiEntry): ApiEntry {
    return {
      ...api,
      protocolLabel: protocolLabelMap[api.protocol] || api.protocol,
      httpMethodLabel: methodLabelMap[api.httpMethod] || api.httpMethod,
      accessTypeLabel: accessTypeLabelMap[api.accessType] || api.accessType,
      userTypeLabel: resolveUserTypeLabel(api.userType, userTypeLabelMap),
      auditTooltip: buildAuditTooltip(api),
    };
  }

  async function openDetail(api: ApiEntry) {
    setDetailOpen(true);
    setDetailItem(api);
    setDetailLoading(true);
    try {
      setDetailItem(enrichApi(await getApi(api.id)));
    } finally {
      setDetailLoading(false);
    }
  }

  async function openMaintain(api: ApiEntry) {
    if (!canMaintain) return;
    setMaintainOpen(true);
    setMaintainItem(api);
    setMaintainLoading(true);
    try {
      const detail = enrichApi(await getApi(api.id));
      setMaintainItem(detail);
      fillSortForm(detail.sortOptionsJson);
    } finally {
      setMaintainLoading(false);
    }
  }

  function fillSortForm(sortOptionsJson?: string) {
    const options = parseSortOptions(sortOptionsJson);
    setSortEnabled(options.enabled);
    setAllowedSortDrafts(toAllowedDrafts(options.allowed));
    setDefaultSortDrafts(toDefaultDrafts(options.defaults));
    setDisabledSortCache(null);
  }

  function closeMaintain() {
    setMaintainOpen(false);
    setMaintainItem(null);
    setMaintainLoading(false);
    setSortEnabled(false);
    setAllowedSortDrafts([]);
    setDefaultSortDrafts([]);
    setDisabledSortCache(null);
    setDragState(null);
  }

  async function saveSortOptions() {
    if (!maintainItem) return;
    let sortOptionsJson: string;
    try {
      sortOptionsJson = buildSortOptionsJson(sortEnabled, allowedSortDrafts, defaultSortDrafts);
    } catch (error) {
      message.error(error instanceof Error ? error.message : "排序规则格式错误");
      return;
    }

    setSortSaving(true);
    try {
      const updated = enrichApi(await updateApiSortOptions(maintainItem.id, sortOptionsJson));
      setMaintainItem(updated);
      setDetailItem((current) => (current?.id === updated.id ? updated : current));
      message.success("保存成功");
      closeMaintain();
      await reload();
    } finally {
      setSortSaving(false);
    }
  }
}

interface SortRuleEditTableProps {
  title: string;
  addTitle: string;
  emptyText: string;
  rowCount: number;
  dragType: SortDraftType;
  headers: string[];
  columnWidths?: Array<number | string>;
  addDisabled?: boolean;
  getRowKey: (index: number) => string;
  renderRow: (index: number) => ReactNode[];
  onAdd: () => void;
  onRemove: (index: number) => void;
  onDragStart: (index: number) => void;
  onDragEnd: () => void;
  onDragOver: (event: DragEvent<HTMLDivElement>, type: SortDraftType, targetIndex: number) => void;
}

interface SortRuleViewTableProps {
  title: string;
  headers: string[];
  emptyText: string;
  rows: Array<{
    key: string;
    cells: ReactNode[];
  }>;
}

function SortRuleViewTable({ title, headers, emptyText, rows }: SortRuleViewTableProps) {
  return (
    <div className="api-sort-table-panel">
      <div className="api-sort-table-panel__title-row">
        <div className="api-sort-table-panel__title">{title}</div>
      </div>
      <table className="api-sort-data-table">
        <thead>
          <tr>
            {headers.map((header) => (
              <th key={header}>{header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td
                className="api-sort-empty-row"
                colSpan={headers.length}
              >
                {emptyText}
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={row.key}>
                {row.cells.map((cell, index) => (
                  <td
                    className={typeof cell === "string" ? "is-code" : undefined}
                    key={index}
                  >
                    {cell}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

function SortRuleEditTable({
  title,
  addTitle,
  emptyText,
  rowCount,
  dragType,
  headers,
  columnWidths,
  addDisabled = false,
  getRowKey,
  renderRow,
  onAdd,
  onRemove,
  onDragStart,
  onDragEnd,
  onDragOver,
}: SortRuleEditTableProps) {
  const contentColumns = resolveSortTableColumns(headers.length, columnWidths);
  const rowStyle = {
    "--api-sort-columns": contentColumns,
  } as CSSProperties;

  return (
    <div className="api-sort-table-panel api-sort-edit-table">
      <div className="api-sort-table-panel__title-row">
        <div className="api-sort-table-panel__title">{title}</div>
      </div>
      <div className="api-sort-edit-table__grid">
        <div
          className="api-sort-edit-table__head"
          style={rowStyle}
        >
          <div />
          {headers.map((header) => (
            <div key={header}>{header}</div>
          ))}
          <div className="api-sort-table-panel__add-action">
            <BzIconActionButton
              icon="plus"
              tone="primary"
              size={28}
              disabled={addDisabled}
              title={addTitle}
              ariaLabel={addTitle}
              onClick={onAdd}
            />
          </div>
        </div>
        {rowCount === 0 ? (
          <div className="api-sort-empty-row">{emptyText}</div>
        ) : (
          Array.from({ length: rowCount }).map((_, index) => (
            <div
              className="api-sort-edit-table__row"
              key={getRowKey(index)}
              style={rowStyle}
              onDragOver={(event) => onDragOver(event, dragType, index)}
              onDrop={(event) => event.preventDefault()}
            >
              <div className="api-sort-drag-cell">
                <button
                  className="api-sort-drag-handle"
                  type="button"
                  draggable
                  aria-label="拖动排序"
                  onDragStart={() => onDragStart(index)}
                  onDragEnd={onDragEnd}
                >
                  ⋮⋮
                </button>
              </div>
              {renderRow(index).map((cell, cellIndex) => (
                <div
                  className="api-sort-edit-table__cell"
                  key={cellIndex}
                >
                  {cell}
                </div>
              ))}
              <div className="api-sort-row-actions">
                <BzIconActionButton
                  icon="minus"
                  tone="danger"
                  size={28}
                  title="删除"
                  ariaLabel="删除"
                  onClick={() => onRemove(index)}
                />
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

function resolveSortTableColumns(count: number, widths?: Array<number | string>): string {
  if (!widths || widths.length !== count) {
    return `repeat(${count}, minmax(0, 1fr))`;
  }
  return widths
    .map((width) => (typeof width === "number" ? `${width}px` : width.trim() || "minmax(0, 1fr)"))
    .join(" ");
}

function toLabelMap(items?: DictItem[]): Record<string, string> {
  const map: Record<string, string> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = item.itemLabel || item.itemValue;
  }
  return map;
}

function resolveUserTypeLabel(raw: string | undefined, labelMap: Record<string, string>): string {
  const normalized = raw?.trim();
  if (!normalized) return "";
  return labelMap[normalized] || normalized;
}

function toOptions(labelMap: Record<string, string>) {
  return Object.entries(labelMap).map(([value, label]) => ({ value, label }));
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

function parseSortOptions(json?: string): ApiSortOptions {
  const fallback: ApiSortOptions = { enabled: false, allowed: [], defaults: [] };
  const normalized = json?.trim();
  if (!normalized) return fallback;

  try {
    const payload = JSON.parse(normalized) as Partial<ApiSortOptions>;
    return {
      enabled: payload.enabled !== false,
      allowed: normalizeAllowedFields(payload.allowed),
      defaults: normalizeDefaultSorts(payload.defaults),
    };
  } catch {
    return fallback;
  }
}

function normalizeAllowedFields(value: unknown): ApiSortAllowedField[] {
  if (!Array.isArray(value)) return [];
  return value
    .map((item) => {
      if (!isRecord(item)) return null;
      const field = String(item.field ?? "").trim();
      const column = String(item.column ?? "").trim();
      return field && column ? { field, column } : null;
    })
    .filter((item): item is ApiSortAllowedField => Boolean(item));
}

function normalizeDefaultSorts(value: unknown): ApiDefaultSort[] {
  if (!Array.isArray(value)) return [];
  return value
    .map((item) => {
      if (!isRecord(item)) return null;
      const field = String(item.field ?? "").trim();
      const direction = String(item.direction ?? "ASC").trim().toUpperCase();
      if (!field || (direction !== "ASC" && direction !== "DESC")) return null;
      return { field, direction };
    })
    .filter((item): item is ApiDefaultSort => Boolean(item));
}

function nextSortUid(): string {
  sortUidSeed += 1;
  return `sort-${Date.now()}-${sortUidSeed}`;
}

function toAllowedDrafts(fields: ApiSortAllowedField[]): ApiSortAllowedDraft[] {
  return fields.map((item) => ({ ...item, uid: nextSortUid() }));
}

function toDefaultDrafts(sorts: ApiDefaultSort[]): ApiDefaultSortDraft[] {
  return sorts.map((item) => ({ ...item, uid: nextSortUid() }));
}

function renderAllowedSortTable(fields: ApiSortAllowedField[]) {
  return (
    <SortRuleViewTable
      title="候选排序字段"
      headers={["请求字段 field", "数据库列 column"]}
      emptyText="暂无候选字段"
      rows={fields.map((item) => ({
        key: `${item.field}:${item.column}`,
        cells: [item.field, item.column],
      }))}
    />
  );
}

function renderDefaultSortTable(sorts: ApiDefaultSort[]) {
  return (
    <SortRuleViewTable
      title="默认排序规则"
      headers={["请求字段 field", "排序方向"]}
      emptyText="暂无默认排序规则"
      rows={sorts.map((item, index) => ({
        key: `${item.field}:${item.direction}:${index}`,
        cells: [
          item.field,
          <span
            className="api-sort-direction-value"
            key="direction"
          >
            <span>{item.direction === "ASC" ? "↑" : "↓"}</span>
            <span className="is-code">{item.direction}</span>
          </span>,
        ],
      }))}
    />
  );
}

function buildSortOptionsJson(
  enabled: boolean,
  allowedDrafts: ApiSortAllowedDraft[],
  defaultDrafts: ApiDefaultSortDraft[],
): string {
  if (!enabled) {
    return JSON.stringify({ enabled: false, allowed: [], defaults: [] });
  }

  const allowed = allowedDrafts.map(({ field, column }) => ({
    field: field.trim(),
    column: column.trim(),
  }));
  const defaults = defaultDrafts.map(({ field, direction }) => ({ field: field.trim(), direction }));
  if (allowed.length === 0) {
    throw new Error("开启排序规则时必须配置候选排序字段");
  }

  const identifierPattern = /^[A-Za-z_][A-Za-z0-9_]*$/;
  const columnPattern = /^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)?$/;
  const allowedFieldSet = new Set<string>();
  for (const item of allowed) {
    if (!item.field || !identifierPattern.test(item.field)) {
      throw new Error(`候选排序字段无效：${item.field || "空"}`);
    }
    if (!item.column || !columnPattern.test(item.column)) {
      throw new Error(`候选排序列无效：${item.column || "空"}`);
    }
    if (allowedFieldSet.has(item.field)) {
      throw new Error(`候选排序字段重复：${item.field}`);
    }
    allowedFieldSet.add(item.field);
  }

  const defaultFieldSet = new Set<string>();
  for (const item of defaults) {
    if (!allowedFieldSet.has(item.field)) {
      throw new Error(`默认排序字段未包含在候选排序字段中：${item.field || "空"}`);
    }
    if (defaultFieldSet.has(item.field)) {
      throw new Error(`默认排序字段重复：${item.field}`);
    }
    defaultFieldSet.add(item.field);
  }

  return JSON.stringify({ enabled: true, allowed, defaults });
}

function moveSortItem<T>(items: T[], fromIndex: number, toIndex: number): T[] {
  if (fromIndex < 0 || toIndex < 0 || fromIndex >= items.length || toIndex >= items.length) {
    return items;
  }
  const next = [...items];
  const [moved] = next.splice(fromIndex, 1);
  next.splice(toIndex, 0, moved);
  return next;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
