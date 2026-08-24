"use client";

import {
  disableApi,
  getApi,
  pageApis,
  publishApi,
  updateApiSortOptions,
} from "@admin/features/apis/api/client";
import type { ApiEntry } from "@admin/features/apis/model/types";
import { API_PERMISSIONS } from "@admin/features/apis/permissions";
import { ApiTable } from "@admin/features/apis/ui/ApiTable";
import type { ApiTableRow } from "@admin/features/apis/ui/types";
import { batchListDictionaryOptions as batchListDictOptions } from "@admin/features/dicts/public/dictionary-client";
import type { DictionaryItem as DictItem } from "@admin/features/dicts/public/types";
import { usePermission } from "@admin/features/resources/permissions";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { message } from "@admin/shared/lib/feedback/message";
import entityStyles from "@admin/shared/ui/admin/AdminEntity.module.css";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import { AdminInfoCell } from "@admin/shared/ui/admin/AdminInfoCell";
import { AdminListPageTemplate } from "@admin/shared/ui/admin/AdminListPageTemplate";
import { AdminSearchField, AdminSearchForm } from "@admin/shared/ui/admin/AdminSearchForm";
import { AdminTablePagination } from "@admin/shared/ui/admin/AdminTablePagination";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import { TableInput, TableSelect } from "@admin/shared/ui/admin/inputs";
import {
  BzAlert,
  BzButton,
  BzIconActionButton,
  BzInput,
  BzOption,
  BzSelect,
  BzSwitch,
  BzTag,
} from "@admin/shared/ui/bz";
import {
  type CSSProperties,
  type DragEvent,
  type ReactNode,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import styles from "./ApisPage.module.css";

const API_DICT_CODES = [
  "API_METHOD",
  "API_PROTOCOL",
  "API_ACCESS_TYPE",
  "USER_TYPE",
  "API_STATUS",
  "API_PERMISSION_DECLARED",
  "API_AUDIT_DECLARED",
] as const;

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100, 200] as const;
const INITIAL_FILTERS = {
  module: "",
  pathPattern: "",
  handlerClass: "",
  handlerMethod: "",
  permissionDeclared: "",
  accessType: "",
  userType: "",
  auditDeclared: "",
  status: "",
};

type ApiFilters = typeof INITIAL_FILTERS;

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

export function ApisPage() {
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
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
  const detailRequestRef = useRef<AbortController | null>(null);
  const maintainRequestRef = useRef<AbortController | null>(null);

  const canView = usePermission(API_PERMISSIONS.view);
  const canPublish = usePermission(API_PERMISSIONS.publish);
  const canDisable = usePermission(API_PERMISSIONS.disable);
  const canMaintain = usePermission(API_PERMISSIONS.edit);
  const {
    page,
    draftFilters,
    setDraftFilters,
    pageNo,
    pageSize,
    loading,
    error,
    submit,
    reset,
    refresh,
    setPageNo,
    setPageSize,
  } = useAdminPagedQuery<ApiEntry, ApiFilters>({
    initialFilters: INITIAL_FILTERS,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canView,
    normalizeFilters: (filters) => ({
      ...filters,
      module: filters.module.trim(),
      pathPattern: filters.pathPattern.trim(),
      handlerClass: filters.handlerClass.trim(),
      handlerMethod: filters.handlerMethod.trim(),
    }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) =>
      pageApis({ ...filters, pageNo: targetPage, pageSize: targetSize }, { signal }),
  });

  useEffect(() => {
    const controller = new AbortController();
    void loadDictionaries(controller.signal);
    return () => controller.abort();
  }, []);

  useEffect(() => {
    return () => {
      detailRequestRef.current?.abort();
      maintainRequestRef.current?.abort();
    };
  }, []);

  const enrichedRows = useMemo<ApiTableRow[]>(
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

  async function loadDictionaries(signal: AbortSignal) {
    try {
      const result = await batchListDictOptions([...API_DICT_CODES], { signal });
      setMethodLabelMap(toLabelMap(result.API_METHOD));
      setProtocolLabelMap(toLabelMap(result.API_PROTOCOL));
      setAccessTypeLabelMap(toLabelMap(result.API_ACCESS_TYPE));
      setUserTypeLabelMap(toLabelMap(result.USER_TYPE));
      setStatusLabelMap(toLabelMap(result.API_STATUS));
      setPermissionDeclaredLabelMap(toLabelMap(result.API_PERMISSION_DECLARED));
      setAuditDeclaredLabelMap(toLabelMap(result.API_AUDIT_DECLARED));
    } catch (cause) {
      if (isAbortError(cause)) return;
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

  function renderInfoCell(value: ReactNode, readonly: boolean, colSpan?: number) {
    return (
      <AdminInfoCell
        state={readonly ? "readonly" : "display"}
        colSpan={colSpan}
      >
        {value || "-"}
      </AdminInfoCell>
    );
  }

  function renderBasicSection(api: ApiEntry, ariaLabel: string, readonly = false) {
    return (
      <section className={entityStyles.section}>
        <div className={entityStyles.sectionHead}>
          <div className={entityStyles.sectionTitle}>基础信息</div>
        </div>
        <div className={entityStyles.infoTableWrap}>
          <table
            className={entityStyles.infoTable}
            aria-label={ariaLabel}
          >
            <tbody>
              <tr>
                <th>模块</th>
                {renderInfoCell(api.module, readonly)}
                <th>协议</th>
                {renderInfoCell(protocolLabelMap[api.protocol] || api.protocol, readonly)}
                <th>方法</th>
                {renderInfoCell(methodLabelMap[api.httpMethod] || api.httpMethod, readonly)}
              </tr>
              <tr>
                <th>访问类型</th>
                {renderInfoCell(accessTypeLabelMap[api.accessType] || api.accessType, readonly)}
                <th>用户类型</th>
                {renderInfoCell(resolveUserTypeLabel(api.userType, userTypeLabelMap), readonly)}
                <th>接口状态</th>
                {renderInfoCell(api.enabled ? "启用" : "停用", readonly)}
              </tr>
              <tr>
                <th>路径</th>
                {renderInfoCell(api.pathPattern, readonly, 5)}
              </tr>
              <tr>
                <th>处理类</th>
                {renderInfoCell(api.handlerClass, readonly, 5)}
              </tr>
              <tr>
                <th>处理方法</th>
                {renderInfoCell(api.handlerMethod, readonly, 5)}
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    );
  }

  function renderAuditSection(api: ApiEntry, ariaLabel: string, readonly = false) {
    return (
      <section className={entityStyles.section}>
        <div className={entityStyles.sectionHead}>
          <div className={entityStyles.sectionTitle}>权限与审计</div>
        </div>
        <div className={entityStyles.infoTableWrap}>
          <table
            className={entityStyles.infoTable}
            aria-label={ariaLabel}
          >
            <tbody>
              <tr>
                <th>权限声明</th>
                {renderInfoCell(api.permissionDeclared ? "已声明" : "未声明", readonly)}
                <th>审计状态</th>
                {renderInfoCell(api.auditDeclared ? "已开启" : "未开启", readonly)}
                <th>审计资源</th>
                {renderInfoCell(api.auditResource, readonly)}
              </tr>
              <tr>
                <th>审计动作</th>
                {renderInfoCell(api.auditAction, readonly, 5)}
              </tr>
              <tr>
                <th>审计说明</th>
                {renderInfoCell(api.auditDescription, readonly, 5)}
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    );
  }

  function renderSortDetailSection(options: ApiSortOptions) {
    return (
      <section className={entityStyles.section}>
        <div className={entityStyles.sectionHead}>
          <div className={entityStyles.sectionTitle}>排序规则</div>
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
      <section className={entityStyles.section}>
        <div className={entityStyles.sectionHead}>
          <div className={entityStyles.sectionTitle}>排序规则</div>
          <div className={styles.sortSwitchLine}>
            <BzSwitch
              modelValue={sortEnabled}
              onValueChange={toggleSortEnabled}
            />
            <span className={styles.sortSwitchLabel}>{sortEnabled ? "已开启" : "未开启"}</span>
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
            <TableInput
              key="field"
              value={item.field}
              placeholder="createdAt"
              onValueChange={(value) => updateAllowedField(index, value)}
            />,
            <TableInput
              className={styles.code}
              key="column"
              value={item.column}
              placeholder="created_at"
              onValueChange={(value) => updateAllowedColumn(index, value)}
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
            <TableSelect
              className={styles.code}
              key="field"
              value={item.field}
              options={allowedSortDrafts
                .filter((field) => field.field.trim())
                .map((field) => ({ label: field.field, value: field.field }))}
              allowClear={false}
              onValueChange={(value) =>
                updateDefaultSortField(index, Array.isArray(value) ? "" : String(value))
              }
            />,
            <TableSelect
              key="direction"
              value={item.direction}
              options={[
                { label: "升序（ASC）", value: "ASC" },
                { label: "降序（DESC）", value: "DESC" },
              ]}
              allowClear={false}
              onValueChange={(value) =>
                updateDefaultSortDirection(
                  index,
                  (Array.isArray(value) ? value[0] : value) as "ASC" | "DESC",
                )
              }
            />,
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
        className={styles.page}
        queryPanelVisible={queryPanelVisible}
        queryPanel={
          <AdminSearchForm
            visible={queryPanelVisible}
            onSubmit={submit}
            onReset={reset}
          >
            <AdminSearchField label="模块">
              <BzInput
                modelValue={draftFilters.module}
                placeholder="请输入模块"
                clearable
                onValueChange={(module) => setDraftFilters((filters) => ({ ...filters, module }))}
              />
            </AdminSearchField>
            <AdminSearchField label="路径">
              <BzInput
                modelValue={draftFilters.pathPattern}
                placeholder="请输入路径"
                clearable
                onValueChange={(pathPattern) =>
                  setDraftFilters((filters) => ({ ...filters, pathPattern }))
                }
              />
            </AdminSearchField>
            <AdminSearchField label="处理类">
              <BzInput
                modelValue={draftFilters.handlerClass}
                placeholder="请输入处理类"
                clearable
                onValueChange={(handlerClass) =>
                  setDraftFilters((filters) => ({ ...filters, handlerClass }))
                }
              />
            </AdminSearchField>
            <AdminSearchField label="处理方法">
              <BzInput
                modelValue={draftFilters.handlerMethod}
                placeholder="请输入处理方法"
                clearable
                onValueChange={(handlerMethod) =>
                  setDraftFilters((filters) => ({ ...filters, handlerMethod }))
                }
              />
            </AdminSearchField>
            {renderSearchSelect(
              "权限声明",
              "请选择权限声明",
              draftFilters.permissionDeclared,
              permissionDeclaredOptions,
              (permissionDeclared) =>
                setDraftFilters((filters) => ({ ...filters, permissionDeclared })),
            )}
            {renderSearchSelect(
              "访问类型",
              "请选择访问类型",
              draftFilters.accessType,
              accessTypeOptions,
              (accessType) => setDraftFilters((filters) => ({ ...filters, accessType })),
            )}
            {renderSearchSelect(
              "用户类型",
              "请选择用户类型",
              draftFilters.userType,
              userTypeOptions,
              (userType) => setDraftFilters((filters) => ({ ...filters, userType })),
            )}
            {renderSearchSelect(
              "审计",
              "请选择审计状态",
              draftFilters.auditDeclared,
              auditDeclaredOptions,
              (auditDeclared) => setDraftFilters((filters) => ({ ...filters, auditDeclared })),
            )}
            {renderSearchSelect(
              "状态",
              "请选择状态",
              draftFilters.status,
              statusOptions,
              (status) => setDraftFilters((filters) => ({ ...filters, status })),
            )}
          </AdminSearchForm>
        }
        queryTools={
          <AdminTableTools
            queryPanelVisible={queryPanelVisible}
            onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
            onRefresh={() => void refresh()}
          />
        }
        table={
          <>
            {error ? (
              <BzAlert
                key={error.message}
                title={error.message}
                type="error"
                closable={false}
              />
            ) : null}
            <ApiTable
              rows={enrichedRows}
              loading={loading}
              canDetail={canView}
              canMaintain={canMaintain}
              canPublish={canPublish}
              canDisable={canDisable}
              onDetail={(api) => void openDetail(api)}
              onMaintain={(api) => void openMaintain(api)}
              onPublish={(api) => void onPublish(api)}
              onDisable={(api) => void onDisable(api)}
            />
          </>
        }
        footer={
          <AdminTablePagination
            total={page.totalElements}
            pageNo={pageNo}
            pageSize={pageSize}
            pageSizes={PAGE_SIZE_OPTIONS}
            onPageChange={setPageNo}
            onPageSizeChange={setPageSize}
          />
        }
        overlays={
          <>
            <AdminEntityDrawer
              open={detailOpen}
              title="接口详情"
              width="1180px"
              loading={detailLoading}
              className={entityStyles.manageDrawer}
              onClose={() => {
                detailRequestRef.current?.abort();
                detailRequestRef.current = null;
                setDetailOpen(false);
                setDetailItem(null);
                setDetailLoading(false);
              }}
              footer={
                <BzButton
                  onClick={() => {
                    detailRequestRef.current?.abort();
                    detailRequestRef.current = null;
                    setDetailOpen(false);
                    setDetailItem(null);
                    setDetailLoading(false);
                  }}
                >
                  关闭
                </BzButton>
              }
            >
              {detailItem ? (
                <div className={entityStyles.shell}>
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
              className={entityStyles.manageDrawer}
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
                <div className={entityStyles.shell}>
                  {renderBasicSection(maintainItem, "维护排序规则基础信息", true)}
                  {renderAuditSection(maintainItem, "维护排序规则权限与审计", true)}
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
    await refresh();
  }

  async function onDisable(api: ApiEntry) {
    if (!canDisable) return;
    await disableApi(api.id);
    await refresh();
  }

  async function openDetail(api: ApiEntry) {
    if (!canView) return;
    detailRequestRef.current?.abort();
    const controller = new AbortController();
    detailRequestRef.current = controller;
    setDetailOpen(true);
    setDetailItem(api);
    setDetailLoading(true);
    try {
      const detail = await getApi(api.id, { signal: controller.signal });
      if (detailRequestRef.current === controller && !controller.signal.aborted) {
        setDetailItem(detail);
      }
    } catch (cause) {
      if (!isAbortError(cause) && detailRequestRef.current === controller) {
        message.error(cause instanceof Error ? cause.message : "接口详情加载失败");
        setDetailOpen(false);
        setDetailItem(null);
      }
    } finally {
      if (detailRequestRef.current === controller) {
        detailRequestRef.current = null;
        setDetailLoading(false);
      }
    }
  }

  async function openMaintain(api: ApiEntry) {
    if (!canMaintain) return;
    maintainRequestRef.current?.abort();
    const controller = new AbortController();
    maintainRequestRef.current = controller;
    setMaintainOpen(true);
    setMaintainItem(api);
    setMaintainLoading(true);
    try {
      const detail = await getApi(api.id, { signal: controller.signal });
      if (maintainRequestRef.current === controller && !controller.signal.aborted) {
        setMaintainItem(detail);
        fillSortForm(detail.sortOptionsJson);
      }
    } catch (cause) {
      if (!isAbortError(cause) && maintainRequestRef.current === controller) {
        message.error(cause instanceof Error ? cause.message : "接口维护数据加载失败");
        setMaintainOpen(false);
        setMaintainItem(null);
      }
    } finally {
      if (maintainRequestRef.current === controller) {
        maintainRequestRef.current = null;
        setMaintainLoading(false);
      }
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
    maintainRequestRef.current?.abort();
    maintainRequestRef.current = null;
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
      const updated = await updateApiSortOptions(maintainItem.id, sortOptionsJson);
      setMaintainItem(updated);
      setDetailItem((current) => (current?.id === updated.id ? updated : current));
      message.success("保存成功");
      closeMaintain();
      await refresh();
    } finally {
      setSortSaving(false);
    }
  }
}

function renderSearchSelect(
  label: string,
  placeholder: string,
  value: string,
  options: Array<{ value: string; label: string }>,
  onValueChange: (value: string) => void,
) {
  return (
    <AdminSearchField label={label}>
      <BzSelect
        modelValue={value || undefined}
        placeholder={placeholder}
        clearable
        onValueChange={(nextValue) => onValueChange(nextValue || "")}
      >
        {options.map((option) => (
          <BzOption
            key={option.value}
            label={option.label}
            value={option.value}
          />
        ))}
      </BzSelect>
    </AdminSearchField>
  );
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
    <div className={styles.sortTablePanel}>
      <div className={styles.sortTableTitleRow}>
        <div className={styles.sortTableTitle}>{title}</div>
      </div>
      <table className={styles.sortDataTable}>
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
                className={styles.sortEmptyRow}
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
                    className={typeof cell === "string" ? styles.code : undefined}
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
    <div className={`${styles.sortTablePanel} ${styles.sortEditTable}`}>
      <div className={styles.sortTableTitleRow}>
        <div className={styles.sortTableTitle}>{title}</div>
      </div>
      <div className={styles.sortEditGrid}>
        <div
          className={styles.sortEditHead}
          style={rowStyle}
        >
          <div />
          {headers.map((header) => (
            <div key={header}>{header}</div>
          ))}
          <div className={styles.sortTableAddAction}>
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
          <div className={styles.sortEmptyRow}>{emptyText}</div>
        ) : (
          Array.from({ length: rowCount }).map((_, index) => (
            <div
              className={styles.sortEditRow}
              key={getRowKey(index)}
              style={rowStyle}
              onDragOver={(event) => onDragOver(event, dragType, index)}
              onDrop={(event) => event.preventDefault()}
            >
              <div className={styles.sortDragCell}>
                <button
                  className={styles.sortDragHandle}
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
                  className={`${styles.sortEditCell} ${entityStyles.infoCellEditable}`}
                  key={cellIndex}
                >
                  {cell}
                </div>
              ))}
              <div className={styles.sortRowActions}>
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
      const direction = String(item.direction ?? "ASC")
        .trim()
        .toUpperCase();
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
            className={styles.sortDirectionValue}
            key="direction"
          >
            <span>{item.direction === "ASC" ? "↑" : "↓"}</span>
            <span className={styles.code}>{item.direction}</span>
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
  const defaults = defaultDrafts.map(({ field, direction }) => ({
    field: field.trim(),
    direction,
  }));
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

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}
