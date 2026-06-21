"use client";

import { batchListDictOptions } from "@admin/api/dicts";
import {
  createUserFeaturePackage,
  deleteUserFeaturePackage,
  getUserFeaturePackage,
  listUserFeatureApplications,
  pageUserFeaturePackages,
  updateUserFeaturePackage,
  updateUserFeaturePackageStatus,
} from "@admin/api/user-features";
import { AdminActionBar } from "@admin/components/admin/AdminActionBar";
import {
  BzButton,
  BzCard,
  BzDialog,
  BzForm,
  BzFormItem,
  BzInput,
  BzLoading,
  BzOption,
  BzSelect,
  BzSwitch,
  BzTable,
  BzTag,
  BzTextField,
} from "@admin/components/bz";
import type { BzTableColumn } from "@admin/components/bz/BzTable";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import {
  hasResourceCodeAccess,
  useIsPermissionsLoaded,
} from "@admin/core/registry/permissions-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import type {
  SaveUserFeaturePackageRequest,
  UserFeatureAccessScope,
  UserFeatureApplicationEntry,
  UserFeaturePackageEntry,
} from "@admin/types/user-feature";
import { useEffect, useMemo, useRef, useState } from "react";

const pageSizeOptions = [10, 20, 30, 50, 100] as const;
type TagType = "info" | "success" | "warning" | "danger";
type DictMeta = { label: string; tagType?: TagType };
type DrawerMode = "detail" | "edit" | "create";

function buildTokens(current: number, total: number): Array<number | "ellipsis"> {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", total];
  if (current >= total - 3)
    return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
}

function toMetaMap(items?: DictItem[]): Record<string, DictMeta> {
  const map: Record<string, DictMeta> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = {
      label: item.itemLabel || item.itemValue,
      tagType: (item.tagType as TagType | null) || undefined,
    };
  }
  return map;
}

function resolveLabel(metaMap: Record<string, DictMeta>, value?: string | null): string {
  if (!value) return "-";
  return metaMap[value]?.label || value;
}

function resolveTagType(metaMap: Record<string, DictMeta>, value?: string | null): string {
  if (!value) return "info";
  return metaMap[value]?.tagType || "info";
}

function defaultForm(): SaveUserFeaturePackageRequest {
  return {
    code: "",
    name: "",
    packageType: "CUSTOM",
    description: "",
    enabled: true,
    defaultPackage: false,
    applicationAccesses: [],
  };
}

export function UserFeaturePackagesPage() {
  const permissionsLoaded = useIsPermissionsLoaded();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<UserFeaturePackageEntry[]>([]);
  const [applications, setApplications] = useState<UserFeatureApplicationEntry[]>([]);
  const [page, setPage] = useState<PageResult<UserFeaturePackageEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [enabledDraft, setEnabledDraft] = useState<"" | "true" | "false">("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedEnabled, setAppliedEnabled] = useState<"" | "true" | "false">("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [drawerMode, setDrawerMode] = useState<DrawerMode>("detail");
  const [currentPackage, setCurrentPackage] = useState<UserFeaturePackageEntry | null>(null);
  const [selectedApplications, setSelectedApplications] = useState<
    Record<string, { featureAccessScope: UserFeatureAccessScope; featureIds: string[] }>
  >({});
  const [form, setForm] = useState<SaveUserFeaturePackageRequest>(defaultForm());
  const [packageTypeMetaMap, setPackageTypeMetaMap] = useState<Record<string, DictMeta>>({});
  const [dictsLoading, setDictsLoading] = useState(true);

  const loadedRef = useRef(false);

  const canView = hasResourceCodeAccess("user-feature-package-view");
  const canEdit = hasResourceCodeAccess("user-feature-package-edit");
  const totalPages = Math.max(1, page.totalPages || 1);
  const isFirstPage = pageNo <= 1;
  const isLastPage = pageNo >= totalPages;
  const pageTokens = buildTokens(pageNo, totalPages);
  const selectedApplicationCount = Object.keys(selectedApplications).length;

  const packageTypeOptions = useMemo(
    () => Object.entries(packageTypeMetaMap).map(([value, meta]) => ({ value, label: meta.label })),
    [packageTypeMetaMap],
  );

  async function reload() {
    if (!canView) {
      setRows([]);
      setPage({
        pageNo: 1,
        pageSize,
        numberOfElements: 0,
        totalPages: 0,
        totalElements: 0,
        elements: [],
      });
      return;
    }
    setLoading(true);
    try {
      const result = await pageUserFeaturePackages({
        keyword: appliedKeyword || undefined,
        enabled: appliedEnabled === "" ? undefined : appliedEnabled === "true",
        page: { pageNo, pageSize },
      });
      setPage(result);
      setPageNo(result.pageNo || 1);
      setPageSize(result.pageSize || pageSize);
      setRows(result.elements);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!permissionsLoaded || dictsLoading) return;
    void (async () => {
      try {
        const [dictResult, appResult] = await Promise.all([
          batchListDictOptions(["USER_APPLICATION_PACKAGE_TYPE"]),
          listUserFeatureApplications(),
        ]);
        setPackageTypeMetaMap(toMetaMap(dictResult.USER_APPLICATION_PACKAGE_TYPE));
        setApplications(appResult);
      } catch {
        setPackageTypeMetaMap({
          DEFAULT: { label: "默认包", tagType: "info" },
          MEMBERSHIP: { label: "会员包", tagType: "success" },
          OPERATION: { label: "运营包", tagType: "warning" },
          ENTERPRISE: { label: "企业包", tagType: "danger" },
          CUSTOM: { label: "自定义", tagType: "info" },
        });
      } finally {
        setDictsLoading(false);
      }
    })();
  }, [permissionsLoaded]);

  useEffect(() => {
    if (loadedRef.current) return;
    if (!permissionsLoaded || dictsLoading) return;
    loadedRef.current = true;
    void reload();
  }, [permissionsLoaded, dictsLoading]);

  useEffect(() => {
    if (loadedRef.current) void reload();
  }, [pageNo, appliedKeyword, appliedEnabled]);

  function applyFilters() {
    setAppliedKeyword(keywordDraft.trim());
    setAppliedEnabled(enabledDraft);
    setPageNo(1);
  }

  function resetFilters() {
    setKeywordDraft("");
    setEnabledDraft("");
    setAppliedKeyword("");
    setAppliedEnabled("");
    setPageNo(1);
  }

  function goToPage(nextPage: number) {
    const t = Math.min(Math.max(nextPage, 1), totalPages);
    if (t === pageNo) return;
    setPageNo(t);
  }

  function handlePageSizeSelect(e: React.ChangeEvent<HTMLSelectElement>) {
    const v = Number(e.target.value);
    if (!Number.isFinite(v) || v <= 0 || v === pageSize) return;
    setPageSize(v);
    setPageNo(1);
  }

  function getRowActions(row: UserFeaturePackageEntry): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      { key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) },
    ];
    if (canEdit) {
      actions.push({
        key: `edit-${row.id}`,
        label: "编辑",
        tone: "edit",
        handler: () => openEdit(row.id),
      });
    }
    return actions;
  }

  function getRowMoreActions(row: UserFeaturePackageEntry): AdminActionItem[] {
    if (!canEdit) return [];
    return [
      {
        key: `toggle-${row.id}`,
        label: row.enabled ? "停用" : "启用",
        tone: row.enabled ? "disable" : "enable",
        handler: () => toggleStatus(row),
      },
      {
        key: `delete-${row.id}`,
        label: "删除",
        tone: "delete",
        handler: () => removePackage(row),
      },
    ];
  }

  function hydrateSelections(applicationAccesses: UserFeaturePackageEntry["applicationAccesses"]) {
    const next: Record<
      string,
      { featureAccessScope: UserFeatureAccessScope; featureIds: string[] }
    > = {};
    for (const access of applicationAccesses) {
      next[access.applicationId] = {
        featureAccessScope: access.featureAccessScope,
        featureIds: [...access.featureIds],
      };
    }
    setSelectedApplications(next);
  }

  function isApplicationSelected(applicationId: string): boolean {
    return Boolean(selectedApplications[applicationId]);
  }

  function applicationScopeOf(applicationId: string): UserFeatureAccessScope {
    return selectedApplications[applicationId]?.featureAccessScope || "FULL";
  }

  function isFeatureSelected(applicationId: string, featureId: string): boolean {
    return selectedApplications[applicationId]?.featureIds.includes(featureId) || false;
  }

  function toggleApplicationSelection(applicationId: string, checked: boolean) {
    setSelectedApplications((prev) => {
      const next = { ...prev };
      if (checked) {
        next[applicationId] = next[applicationId] || { featureAccessScope: "FULL", featureIds: [] };
      } else {
        delete next[applicationId];
      }
      return next;
    });
  }

  function updateApplicationScope(applicationId: string, scope: UserFeatureAccessScope) {
    setSelectedApplications((prev) => {
      const current = prev[applicationId] || {
        featureAccessScope: "FULL" as UserFeatureAccessScope,
        featureIds: [],
      };
      return {
        ...prev,
        [applicationId]: {
          featureAccessScope: scope,
          featureIds: scope === "FULL" ? [] : current.featureIds,
        },
      };
    });
  }

  function toggleFeatureSelection(applicationId: string, featureId: string, checked: boolean) {
    setSelectedApplications((prev) => {
      const current = prev[applicationId];
      if (!current) return prev;
      const nextFeatureIds = new Set(current.featureIds);
      if (checked) nextFeatureIds.add(featureId);
      else nextFeatureIds.delete(featureId);
      return {
        ...prev,
        [applicationId]: {
          ...current,
          featureIds: Array.from(nextFeatureIds),
        },
      };
    });
  }

  async function openCreate() {
    setForm(defaultForm());
    setSelectedApplications({});
    setDrawerMode("create");
    setCurrentPackage(null);
    setDrawerOpen(true);
  }

  async function openEdit(id: string) {
    setDrawerMode("edit");
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      const pkg = await getUserFeaturePackage(id);
      setCurrentPackage(pkg);
      setForm({
        code: pkg.code,
        name: pkg.name,
        packageType: pkg.packageType,
        description: pkg.description || "",
        enabled: pkg.enabled,
        defaultPackage: pkg.defaultPackage,
        applicationAccesses: [],
      });
      hydrateSelections(pkg.applicationAccesses);
    } finally {
      setDrawerLoading(false);
    }
  }

  async function openDetail(id: string) {
    setDrawerMode("detail");
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      setCurrentPackage(await getUserFeaturePackage(id));
    } finally {
      setDrawerLoading(false);
    }
  }

  function buildPayload(): SaveUserFeaturePackageRequest {
    return {
      code: form.code.trim(),
      name: form.name.trim(),
      packageType: form.packageType,
      description: form.description?.trim() || null,
      enabled: form.enabled,
      defaultPackage: form.defaultPackage,
      applicationAccesses: Object.entries(selectedApplications).map(([applicationId, config]) => ({
        applicationId,
        featureAccessScope: config.featureAccessScope,
        featureIds: config.featureAccessScope === "FULL" ? [] : config.featureIds,
      })),
    };
  }

  async function submitPackage() {
    const payload = buildPayload();
    if (!payload.code || !payload.name) {
      message.warning("编码和名称不能为空");
      return;
    }
    if (drawerMode === "create") {
      await createUserFeaturePackage(payload);
      message.success("应用包已创建");
    } else if (currentPackage) {
      await updateUserFeaturePackage(currentPackage.id, payload);
      message.success("应用包已更新");
    }
    setDrawerOpen(false);
    await reload();
  }

  async function toggleStatus(row: UserFeaturePackageEntry) {
    const nextEnabled = !row.enabled;
    const confirmed = await bzConfirm({
      title: nextEnabled ? "启用应用包" : "停用应用包",
      content: `确认${nextEnabled ? "启用" : "停用"}：${row.name}？`,
      confirmText: nextEnabled ? "启用" : "停用",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await updateUserFeaturePackageStatus(row.id, nextEnabled);
    message.success(nextEnabled ? "已启用" : "已停用");
    await reload();
  }

  async function removePackage(row: UserFeaturePackageEntry) {
    const confirmed = await bzConfirm({
      title: "删除应用包",
      content: `确认删除：${row.name}？`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteUserFeaturePackage(row.id);
    message.success("应用包已删除");
    await reload();
  }

  const columns = useMemo<Array<BzTableColumn<UserFeaturePackageEntry>>>(
    () => [
      { key: "code", title: "编码", minWidth: 180, render: (row) => <>{row.code}</> },
      { key: "name", title: "名称", minWidth: 160, render: (row) => <>{row.name}</> },
      {
        key: "packageType",
        title: "类型",
        width: 120,
        render: (row) => (
          <BzTag type={resolveTagType(packageTypeMetaMap, row.packageType) as TagType}>
            {resolveLabel(packageTypeMetaMap, row.packageType)}
          </BzTag>
        ),
      },
      {
        key: "defaultPackage",
        title: "默认包",
        width: 100,
        render: (row) => (
          <BzTag type={row.defaultPackage ? "success" : "info"}>
            {row.defaultPackage ? "是" : "否"}
          </BzTag>
        ),
      },
      {
        key: "enabled",
        title: "状态",
        width: 100,
        render: (row) => (
          <BzTag type={row.enabled ? "success" : "warning"}>{row.enabled ? "启用" : "停用"}</BzTag>
        ),
      },
      {
        key: "description",
        title: "描述",
        minWidth: 220,
        render: (row) => <>{row.description || "-"}</>,
      },
      {
        key: "applicationAccesses",
        title: "应用数",
        width: 90,
        render: (row) => <>{row.applicationAccesses.length}</>,
      },
      {
        key: "actions",
        title: "操作",
        width: 160,
        render: (row) => (
          <AdminActionBar actions={[...getRowActions(row), ...getRowMoreActions(row)]} />
        ),
      },
    ],
    [packageTypeMetaMap, canEdit],
  );

  const drawerTitle =
    drawerMode === "create" ? "新增应用包" : drawerMode === "edit" ? "编辑应用包" : "应用包详情";

  const drawerFooter = (
    <>
      <BzButton onClick={() => setDrawerOpen(false)}>
        {drawerMode === "detail" ? "关闭" : "取消"}
      </BzButton>
      {drawerMode !== "detail" ? (
        <BzButton
          buttonType="primary"
          onClick={submitPackage}
        >
          确定
        </BzButton>
      ) : null}
    </>
  );

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          {queryPanelVisible ? (
            <BzCard
              className="admin-panel admin-filter-card"
              shadow="never"
            >
              <BzForm
                className="admin-filter-form"
                onSubmit={(e) => {
                  e.preventDefault();
                  applyFilters();
                }}
              >
                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">关键词</div>
                    <div className="admin-filter-control">
                      <BzInput
                        modelValue={keywordDraft}
                        placeholder="按编码或名称搜索"
                        clearable
                        onValueChange={setKeywordDraft}
                        onKeyUp={(e) => e.key === "Enter" && applyFilters()}
                      />
                    </div>
                  </div>
                </BzFormItem>
                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">状态</div>
                    <div className="admin-filter-control">
                      <BzSelect
                        modelValue={enabledDraft}
                        placeholder="全部状态"
                        clearable
                        onValueChange={(v) => setEnabledDraft((v || "") as "" | "true" | "false")}
                      >
                        <BzOption
                          label="启用"
                          value="true"
                        />
                        <BzOption
                          label="停用"
                          value="false"
                        />
                      </BzSelect>
                    </div>
                  </div>
                </BzFormItem>
                <div className="admin-filter-actions">
                  <BzButton
                    className="admin-filter-secondary"
                    onClick={resetFilters}
                  >
                    重置
                  </BzButton>
                  <BzButton
                    className="admin-filter-primary"
                    buttonType="primary"
                    nativeType="submit"
                  >
                    搜索
                  </BzButton>
                  <div
                    className="admin-filter-toggle-placeholder"
                    aria-hidden="true"
                  />
                </div>
              </BzForm>
            </BzCard>
          ) : null}

          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">应用包管理</div>
                <div className="admin-table-tools">
                  {canEdit ? (
                    <BzButton
                      className="admin-toolbar-primary"
                      buttonType="primary"
                      onClick={openCreate}
                    >
                      新增
                    </BzButton>
                  ) : null}
                  <button
                    className={`admin-vben-circle-button${queryPanelVisible ? " is-active" : ""}`}
                    type="button"
                    title={queryPanelVisible ? "关闭搜索框" : "打开搜索框"}
                    onClick={() => setQueryPanelVisible((v) => !v)}
                  >
                    <i
                      className="admin-vben-circle-button__icon admin-vben-circle-button__icon--search"
                      aria-hidden="true"
                    />
                  </button>
                  <button
                    className="admin-vben-circle-button"
                    type="button"
                    title="刷新列表"
                    onClick={() => void reload()}
                  >
                    <i
                      className="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh"
                      aria-hidden="true"
                    />
                  </button>
                </div>
              </div>
            }
          >
            <div className="admin-table-surface">
              <BzTable
                columns={columns}
                data={rows}
                loading={loading}
                rowKey="id"
                emptyText="暂无应用包"
                size="small"
              />
            </div>
            {page.totalElements > 0 ? (
              <div className="dict-pagination-bar">
                <div className="dict-pagination-summary">共 {page.totalElements} 条记录</div>
                <div className="dict-pagination-right">
                  <label className="dict-page-size">
                    <select
                      className="dict-page-size__select"
                      value={pageSize}
                      onChange={handlePageSizeSelect}
                    >
                      {pageSizeOptions.map((size) => (
                        <option
                          key={size}
                          value={size}
                        >
                          {size}条/页
                        </option>
                      ))}
                    </select>
                  </label>
                  <div className="dict-page-list">
                    <button
                      className="dict-page-btn dict-page-btn--icon"
                      type="button"
                      disabled={isFirstPage}
                      onClick={() => goToPage(1)}
                    >
                      <span aria-hidden="true">|&lt;</span>
                    </button>
                    <button
                      className="dict-page-btn dict-page-btn--icon"
                      type="button"
                      disabled={isFirstPage}
                      onClick={() => goToPage(pageNo - 1)}
                    >
                      <span aria-hidden="true">&lt;</span>
                    </button>
                    {pageTokens.map((token, i) =>
                      typeof token === "number" ? (
                        <button
                          key={`${token}-${i}`}
                          className={`dict-page-btn${token === pageNo ? " is-active" : ""}`}
                          type="button"
                          onClick={() => goToPage(token)}
                        >
                          {token}
                        </button>
                      ) : (
                        <span
                          key={`e-${i}`}
                          className="dict-page-ellipsis"
                        >
                          ...
                        </span>
                      ),
                    )}
                    <button
                      className="dict-page-btn dict-page-btn--icon"
                      type="button"
                      disabled={isLastPage}
                      onClick={() => goToPage(pageNo + 1)}
                    >
                      <span aria-hidden="true">&gt;</span>
                    </button>
                    <button
                      className="dict-page-btn dict-page-btn--icon"
                      type="button"
                      disabled={isLastPage}
                      onClick={() => goToPage(totalPages)}
                    >
                      <span aria-hidden="true">&gt;|</span>
                    </button>
                  </div>
                </div>
              </div>
            ) : null}
          </BzCard>

          <BzDialog
            modelValue={drawerOpen}
            title={drawerTitle}
            width="1080px"
            maxWidth="1080px"
            closeOnOverlay={false}
            showClose={true}
            onUpdateModelValue={(v) => setDrawerOpen(v)}
            onClose={() => setDrawerOpen(false)}
            footer={drawerFooter}
          >
            <BzLoading loading={drawerLoading}>
              {drawerMode === "detail" && currentPackage ? (
                <>
                  <div className="detail-grid">
                    <div className="detail-field">
                      <span className="detail-field__label">编码</span>
                      <span className="detail-field__value">{currentPackage.code}</span>
                    </div>
                    <div className="detail-field">
                      <span className="detail-field__label">名称</span>
                      <span className="detail-field__value">{currentPackage.name}</span>
                    </div>
                    <div className="detail-field">
                      <span className="detail-field__label">类型</span>
                      <span className="detail-field__value">
                        {resolveLabel(packageTypeMetaMap, currentPackage.packageType)}
                      </span>
                    </div>
                    <div className="detail-field">
                      <span className="detail-field__label">默认包</span>
                      <span className="detail-field__value">
                        {currentPackage.defaultPackage ? "是" : "否"}
                      </span>
                    </div>
                    <div className="detail-field">
                      <span className="detail-field__label">状态</span>
                      <span className="detail-field__value">
                        {currentPackage.enabled ? "启用" : "停用"}
                      </span>
                    </div>
                    <div className="detail-field detail-field--wide">
                      <span className="detail-field__label">描述</span>
                      <span className="detail-field__value">
                        {currentPackage.description || "-"}
                      </span>
                    </div>
                  </div>
                  <div className="package-access-list">
                    {currentPackage.applicationAccesses.map((access) => (
                      <div
                        key={access.applicationId}
                        className="package-access-card"
                      >
                        <div className="package-access-card__head">
                          <strong>{access.applicationName}</strong>
                          <BzTag
                            type={access.featureAccessScope === "FULL" ? "success" : "warning"}
                          >
                            {access.featureAccessScope === "FULL" ? "完整功能" : "部分功能"}
                          </BzTag>
                        </div>
                        <div className="package-access-card__meta">{access.applicationCode}</div>
                        {access.features.length > 0 ? (
                          <div className="package-access-card__feature-list">
                            {access.features.map((feature) => (
                              <span
                                key={feature.id}
                                className="package-feature-chip"
                              >
                                {feature.name}
                              </span>
                            ))}
                          </div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                </>
              ) : null}

              {drawerMode !== "detail" ? (
                <>
                  <BzForm>
                    <div className="group-form-grid">
                      <BzFormItem label="编码">
                        <BzInput
                          modelValue={form.code}
                          disabled={drawerMode === "edit"}
                          onValueChange={(v) => setForm((prev) => ({ ...prev, code: v }))}
                        />
                      </BzFormItem>
                      <BzFormItem label="名称">
                        <BzInput
                          modelValue={form.name}
                          onValueChange={(v) => setForm((prev) => ({ ...prev, name: v }))}
                        />
                      </BzFormItem>
                      <BzFormItem label="类型">
                        <BzSelect
                          modelValue={form.packageType}
                          onValueChange={(v) =>
                            setForm((prev) => ({ ...prev, packageType: v || "CUSTOM" }))
                          }
                        >
                          {packageTypeOptions.map((opt) => (
                            <BzOption
                              key={opt.value}
                              label={opt.label}
                              value={opt.value}
                            />
                          ))}
                        </BzSelect>
                      </BzFormItem>
                      <BzFormItem label="状态">
                        <BzSwitch
                          modelValue={form.enabled}
                          onValueChange={(v) => setForm((prev) => ({ ...prev, enabled: v }))}
                        />
                      </BzFormItem>
                      <BzFormItem label="默认包">
                        <BzSwitch
                          modelValue={form.defaultPackage}
                          onValueChange={(v) => setForm((prev) => ({ ...prev, defaultPackage: v }))}
                        />
                      </BzFormItem>
                      <BzFormItem
                        label="描述"
                        className="group-form-grid__wide"
                      >
                        <BzTextField
                          modelValue={form.description || ""}
                          type="textarea"
                          rows={3}
                          onValueChange={(v) => setForm((prev) => ({ ...prev, description: v }))}
                        />
                      </BzFormItem>
                    </div>
                  </BzForm>

                  <div className="package-config-panel">
                    <div className="package-config-panel__head">
                      <div className="package-config-panel__title">应用授权</div>
                      <div className="package-config-panel__meta">
                        已选 {selectedApplicationCount} 个应用
                      </div>
                    </div>
                    <div className="package-config-panel__body">
                      {applications.map((application) => {
                        const selected = isApplicationSelected(application.id);
                        const scope = applicationScopeOf(application.id);
                        return (
                          <div
                            key={application.id}
                            className="package-config-card"
                          >
                            <div className="package-config-card__top">
                              <label className="package-config-card__select">
                                <input
                                  type="checkbox"
                                  checked={selected}
                                  onChange={(e) =>
                                    toggleApplicationSelection(application.id, e.target.checked)
                                  }
                                />
                                <div>
                                  <div className="package-config-card__name">
                                    {application.name}
                                  </div>
                                  <div className="package-config-card__code">
                                    {application.code}
                                  </div>
                                </div>
                              </label>
                              <BzTag type={application.enabled ? "success" : "warning"}>
                                {application.enabled ? "启用" : "停用"}
                              </BzTag>
                            </div>
                            <div className="package-config-card__path">
                              {application.routePath || "-"}
                            </div>
                            {selected ? (
                              <div className="package-config-card__scope">
                                <label>
                                  <input
                                    type="radio"
                                    name={`scope-${application.id}`}
                                    value="FULL"
                                    checked={scope === "FULL"}
                                    onChange={() => updateApplicationScope(application.id, "FULL")}
                                  />{" "}
                                  完整功能
                                </label>
                                <label>
                                  <input
                                    type="radio"
                                    name={`scope-${application.id}`}
                                    value="PARTIAL"
                                    checked={scope === "PARTIAL"}
                                    onChange={() =>
                                      updateApplicationScope(application.id, "PARTIAL")
                                    }
                                  />{" "}
                                  部分功能
                                </label>
                              </div>
                            ) : null}
                            {selected && scope === "PARTIAL" ? (
                              <div className="package-config-card__features">
                                {application.features.map((feature) => (
                                  <label
                                    key={feature.id}
                                    className={`package-feature-option${!feature.enabled ? " disabled" : ""}`}
                                  >
                                    <input
                                      type="checkbox"
                                      checked={isFeatureSelected(application.id, feature.id)}
                                      disabled={!feature.enabled}
                                      onChange={(e) =>
                                        toggleFeatureSelection(
                                          application.id,
                                          feature.id,
                                          e.target.checked,
                                        )
                                      }
                                    />
                                    <span>{feature.name}</span>
                                    <small>{feature.code}</small>
                                  </label>
                                ))}
                              </div>
                            ) : null}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </>
              ) : null}
            </BzLoading>
          </BzDialog>
        </div>
      </div>
    </div>
  );
}
