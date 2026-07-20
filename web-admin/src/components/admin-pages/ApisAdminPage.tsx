"use client";

import { disableApi, pageApis, publishApi } from "@admin/api/apis";
import { batchListDictOptions } from "@admin/api/dicts";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { AdminListPageTemplate } from "@admin/components/admin/AdminListPageTemplate";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import { ApiTable } from "@admin/components/apis-admin/ApiTable";
import {
  BzButton,
  BzFormItem,
  BzInput,
  BzOption,
  BzPagination,
  BzSelect,
} from "@admin/components/bz";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { ApiEntry } from "@admin/types/api-admin";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import { useEffect, useMemo, useState } from "react";

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

  const canPublish = hasResourceCodeAccess("api-manage-publish");
  const canDisable = hasResourceCodeAccess("api-manage-disable");
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
            canPublish={canPublish}
            canDisable={canDisable}
            onDetail={(api) => {
              setDetailItem(api);
              setDetailOpen(true);
            }}
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
          <AdminEntityDrawer
            open={detailOpen}
            title="接口详情"
            width="1180px"
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
                <section className="role-manage-section">
                  <div className="role-manage-section__head">
                    <div className="role-manage-section__title">基础信息</div>
                  </div>
                  <div className="role-info-table-wrap">
                    <table
                      className="role-info-table"
                      aria-label="接口基础信息"
                    >
                      <tbody>
                        <tr>
                          <th>模块</th>
                          <td>{detailItem.module || "-"}</td>
                          <th>协议</th>
                          <td>{detailItem.protocolLabel || detailItem.protocol || "-"}</td>
                          <th>方法</th>
                          <td>{detailItem.httpMethodLabel || detailItem.httpMethod || "-"}</td>
                        </tr>
                        <tr>
                          <th>访问类型</th>
                          <td>{detailItem.accessTypeLabel || detailItem.accessType || "-"}</td>
                          <th>用户类型</th>
                          <td>{detailItem.userTypeLabel || detailItem.userType || "-"}</td>
                          <th>接口状态</th>
                          <td>{detailItem.enabled ? "启用" : "停用"}</td>
                        </tr>
                        <tr>
                          <th>路径</th>
                          <td colSpan={5}>{detailItem.pathPattern || "-"}</td>
                        </tr>
                        <tr>
                          <th>处理类</th>
                          <td colSpan={5}>{detailItem.handlerClass || "-"}</td>
                        </tr>
                        <tr>
                          <th>处理方法</th>
                          <td colSpan={5}>{detailItem.handlerMethod || "-"}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </section>

                <section className="role-manage-section">
                  <div className="role-manage-section__head">
                    <div className="role-manage-section__title">权限与审计</div>
                  </div>
                  <div className="role-info-table-wrap">
                    <table
                      className="role-info-table"
                      aria-label="接口权限与审计"
                    >
                      <tbody>
                        <tr>
                          <th>权限声明</th>
                          <td>{detailItem.permissionDeclared ? "已声明" : "未声明"}</td>
                          <th>审计状态</th>
                          <td>{detailItem.auditDeclared ? "已开启" : "未开启"}</td>
                          <th>审计资源</th>
                          <td>{detailItem.auditResource || "-"}</td>
                        </tr>
                        <tr>
                          <th>审计动作</th>
                          <td colSpan={5}>{detailItem.auditAction || "-"}</td>
                        </tr>
                        <tr>
                          <th>审计说明</th>
                          <td colSpan={5}>{detailItem.auditDescription || "-"}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </section>
              </div>
            ) : null}
          </AdminEntityDrawer>
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
