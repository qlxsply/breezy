"use client";

import { getExternalUser, pageExternalUsers, updateExternalUser } from "@admin/api/external-users";
import {
  getUserFeatureUserManagement,
  pageUserFeaturePackages,
  saveUserFeatureUserManagement,
} from "@admin/api/user-features";
import { AdminActionBar } from "@admin/components/admin/AdminActionBar";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import {
  BzButton,
  BzCard,
  BzEmpty,
  BzForm,
  BzFormItem,
  BzInput,
  BzOption,
  BzPagination,
  BzSelect,
  BzTable,
  type BzTableColumn,
  BzTag,
} from "@admin/components/bz";
import { bzConfirm } from "@admin/core/confirm";
import { formatDateTime } from "@admin/core/formatter";
import { message } from "@admin/core/message";
import {
  hasResourceCodeAccess,
  useIsPermissionsLoaded,
} from "@admin/core/registry/permissions-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { ExternalUserEntry, ExternalUserStatus } from "@admin/types/external-user-admin";
import type { PageResult } from "@admin/types/page";
import type {
  UserFeatureAccessScope,
  UserFeatureOverrideType,
  UserFeaturePackageEntry,
  UserFeatureUserApplicationEntry,
  UserFeatureUserManagementEntry,
} from "@admin/types/user-feature";
import { useEffect, useMemo, useRef, useState } from "react";

const pageSizeOptions = [10, 20, 30, 50, 100];

function resolveStatusLabel(status: ExternalUserStatus): string {
  if (status === "ACTIVE") return "启用";
  if (status === "DISABLED") return "停用";
  return "已注销";
}

function resolveStatusType(status: ExternalUserStatus): "success" | "warning" | "danger" {
  if (status === "ACTIVE") return "success";
  if (status === "DISABLED") return "warning";
  return "danger";
}

export function WebUsersAdminPage() {
  const permissionsLoaded = useIsPermissionsLoaded();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<ExternalUserEntry[]>([]);
  const [page, setPage] = useState<PageResult<ExternalUserEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detail, setDetail] = useState<ExternalUserEntry | null>(null);
  const [featureOpen, setFeatureOpen] = useState(false);
  const [featureLoading, setFeatureLoading] = useState(false);
  const [featureUser, setFeatureUser] = useState<ExternalUserEntry | null>(null);
  const [featureManagement, setFeatureManagement] = useState<UserFeatureUserManagementEntry | null>(
    null,
  );
  const [packageEntries, setPackageEntries] = useState<UserFeaturePackageEntry[]>([]);
  const [applicationStates, setApplicationStates] = useState<UserFeatureUserApplicationEntry[]>([]);
  const [selectedPackageIds, setSelectedPackageIds] = useState<string[]>([]);

  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [queryCollapsed] = useState(true);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [statusDraft, setStatusDraft] = useState<"" | ExternalUserStatus>("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedStatus, setAppliedStatus] = useState<"" | ExternalUserStatus>("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [featureKeyword, setFeatureKeyword] = useState("");
  const [featureOverrideFilter, setFeatureOverrideFilter] = useState<"" | UserFeatureOverrideType>(
    "",
  );
  const loadedRef = useRef(false);

  const canView = hasResourceCodeAccess("web-user-manage-view");
  const canEdit = hasResourceCodeAccess("web-user-manage-edit");
  const canFeatureManage =
    hasResourceCodeAccess("user-feature-user-view") ||
    hasResourceCodeAccess("user-feature-user-edit");
  const canFeatureSave = hasResourceCodeAccess("user-feature-user-edit");
  const totalPages = Math.max(1, page.totalPages || 1);

  const flattenedFeatures = useMemo(
    () => applicationStates.flatMap((app) => app.features),
    [applicationStates],
  );

  const filteredFeatures = useMemo(() => {
    const kw = featureKeyword.trim().toLowerCase();
    return flattenedFeatures.filter((item) => {
      if (featureOverrideFilter && item.overrideType !== featureOverrideFilter) return false;
      if (!kw) return true;
      return (
        item.code.toLowerCase().includes(kw) ||
        item.name.toLowerCase().includes(kw) ||
        applicationName(item.applicationId).toLowerCase().includes(kw)
      );
    });
  }, [featureKeyword, featureOverrideFilter, flattenedFeatures, applicationStates]);

  useEffect(() => {
    if (loadedRef.current) return;
    if (!permissionsLoaded) return;
    loadedRef.current = true;
    void reload();
  }, [permissionsLoaded]);

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
      const result = await pageExternalUsers({
        keyword: appliedKeyword || undefined,
        status: appliedStatus || undefined,
        pageNo,
        pageSize,
      });
      setPage(result);
      setPageNo(result.pageNo || 1);
      setPageSize(result.pageSize || pageSize);
      setRows(result.elements);
    } finally {
      setLoading(false);
    }
  }

  function applyFilters() {
    setAppliedKeyword(keywordDraft.trim());
    setAppliedStatus(statusDraft);
    setPageNo(1);
  }
  function resetFilters() {
    setKeywordDraft("");
    setStatusDraft("");
    setAppliedKeyword("");
    setAppliedStatus("");
    setPageNo(1);
  }

  useEffect(() => {
    if (loadedRef.current) void reload();
  }, [pageNo, appliedKeyword, appliedStatus]);

  function getRowActions(row: ExternalUserEntry): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      { key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => openDetail(row.id) },
    ];
    if (canFeatureManage)
      actions.push({
        key: `feature-${row.id}`,
        label: "功能",
        tone: "detail",
        handler: () => openFeatureManagement(row),
      });
    if (canEdit && row.status !== "CANCELLED")
      actions.push({
        key: `toggle-${row.id}`,
        label: row.status === "ACTIVE" ? "停用" : "启用",
        tone: row.status === "ACTIVE" ? "disable" : "enable",
        handler: () => toggleStatus(row),
      });
    return actions;
  }

  async function openDetail(id: string) {
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      setDetail(await getExternalUser(id));
    } finally {
      setDetailLoading(false);
    }
  }

  async function toggleStatus(row: ExternalUserEntry) {
    if (!canEdit || row.status === "CANCELLED") return;
    const nextStatus: ExternalUserStatus = row.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    const confirmed = await bzConfirm({
      title: nextStatus === "ACTIVE" ? "启用用户" : "停用用户",
      content: `确认${nextStatus === "ACTIVE" ? "启用" : "停用"}：${row.account}？`,
      confirmText: nextStatus === "ACTIVE" ? "启用" : "停用",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await updateExternalUser(row.id, { status: nextStatus });
    message.success(nextStatus === "ACTIVE" ? "已启用" : "已停用");
    void reload();
  }

  function renderFeatureFooter() {
    return (
      <>
        <BzButton onClick={() => setFeatureOpen(false)}>取消</BzButton>
        <BzButton
          buttonType="primary"
          disabled={!canFeatureSave}
          onClick={saveFeatureManagement}
        >
          确定
        </BzButton>
      </>
    );
  }

  async function openFeatureManagement(user: ExternalUserEntry) {
    setFeatureOpen(true);
    setFeatureLoading(true);
    setFeatureUser(user);
    try {
      const [management, packagePage] = await Promise.all([
        getUserFeatureUserManagement(user.id),
        pageUserFeaturePackages({ enabled: true, page: { pageNo: 1, pageSize: 200 } }),
      ]);
      setFeatureManagement(management);
      setPackageEntries(packagePage.elements);
      setSelectedPackageIds([...management.packageIds]);
      const states: UserFeatureUserApplicationEntry[] = management.applications.map((app) => ({
        ...app,
        features: app.features.map((f) => ({ ...f })),
      }));
      setApplicationStates(states);
      recomputeEffectiveState(states, [...management.packageIds], packagePage.elements);
    } finally {
      setFeatureLoading(false);
    }
  }

  function packageAccessForApplication(
    applicationId: string,
    packages: UserFeaturePackageEntry[],
    selectedIds: string[],
  ) {
    const accesses = packages
      .filter((pkg) => selectedIds.includes(pkg.id))
      .flatMap((pkg) =>
        pkg.applicationAccesses.filter((access) => access.applicationId === applicationId),
      );
    const fullAccess = accesses.some((access) => access.featureAccessScope === "FULL");
    const featureIds = new Set(
      accesses.flatMap((access) =>
        access.featureAccessScope === "PARTIAL" ? access.featureIds : [],
      ),
    );
    return {
      inheritedVisible: accesses.length > 0,
      packageAccessScope: (fullAccess ? "FULL" : accesses.length > 0 ? "PARTIAL" : "NONE") as
        | "NONE"
        | UserFeatureAccessScope,
      fullAccess,
      featureIds,
    };
  }

  function recomputeEffectiveState(
    states: UserFeatureUserApplicationEntry[],
    selectedIds: string[],
    packages: UserFeaturePackageEntry[],
  ) {
    const next = states.map((app) => {
      const pkgAccess = packageAccessForApplication(app.id, packages, selectedIds);
      const effectiveVisible =
        app.overrideType === "DISABLE"
          ? false
          : app.overrideType === "ENABLE"
            ? true
            : pkgAccess.inheritedVisible;
      const features = app.features.map((feature) => {
        const inheritedEnabled = pkgAccess.fullAccess || pkgAccess.featureIds.has(feature.id);
        let effectiveEnabled = inheritedEnabled;
        if (app.overrideType === "DISABLE") effectiveEnabled = false;
        else if (feature.overrideType === "DISABLE") effectiveEnabled = false;
        else if (app.overrideType === "ENABLE" && app.overrideAccessScope === "FULL")
          effectiveEnabled = true;
        else if (app.overrideType === "ENABLE" && app.overrideAccessScope === "PARTIAL")
          effectiveEnabled = feature.overrideType === "ENABLE";
        else if (
          !inheritedEnabled &&
          feature.overrideType === "ENABLE" &&
          pkgAccess.inheritedVisible
        )
          effectiveEnabled = true;
        return { ...feature, inheritedEnabled, effectiveEnabled };
      });
      return {
        ...app,
        inheritedVisible: pkgAccess.inheritedVisible,
        packageAccessScope: pkgAccess.packageAccessScope,
        effectiveVisible,
        features,
      };
    });
    setApplicationStates(next);
  }

  function togglePackageSelection(packageId: string, checked: boolean) {
    const next = new Set(selectedPackageIds);
    if (checked) next.add(packageId);
    else next.delete(packageId);
    const arr = Array.from(next);
    setSelectedPackageIds(arr);
    recomputeEffectiveState(applicationStates, arr, packageEntries);
  }

  function updateApplicationOverride(applicationId: string, overrideType: UserFeatureOverrideType) {
    const next = applicationStates.map((app) =>
      app.id === applicationId
        ? ({
            ...app,
            overrideType,
            overrideAccessScope:
              overrideType === "ENABLE" ? app.overrideAccessScope || "FULL" : null,
          } as UserFeatureUserApplicationEntry)
        : app,
    );
    setApplicationStates(next);
    recomputeEffectiveState(next, selectedPackageIds, packageEntries);
  }

  function updateApplicationOverrideScope(applicationId: string, scope: UserFeatureAccessScope) {
    const next = applicationStates.map((app) =>
      app.id === applicationId
        ? ({ ...app, overrideAccessScope: scope } as UserFeatureUserApplicationEntry)
        : app,
    );
    setApplicationStates(next);
    recomputeEffectiveState(next, selectedPackageIds, packageEntries);
  }

  function updateFeatureOverride(
    applicationId: string,
    featureId: string,
    overrideType: UserFeatureOverrideType,
  ) {
    const next = applicationStates.map((app) =>
      app.id !== applicationId
        ? app
        : ({
            ...app,
            features: app.features.map((f) => (f.id === featureId ? { ...f, overrideType } : f)),
          } as UserFeatureUserApplicationEntry),
    );
    setApplicationStates(next);
    recomputeEffectiveState(next, selectedPackageIds, packageEntries);
  }

  function applicationName(applicationId: string): string {
    return applicationStates.find((app) => app.id === applicationId)?.name || applicationId;
  }

  async function saveFeatureManagement() {
    if (!featureUser || !canFeatureSave) return;
    await saveUserFeatureUserManagement(featureUser.id, {
      packageIds: selectedPackageIds,
      applicationOverrides: applicationStates
        .filter((app) => app.overrideType !== "NONE")
        .map((app) => ({
          applicationId: app.id,
          overrideType: app.overrideType,
          featureAccessScope:
            app.overrideType === "ENABLE" ? app.overrideAccessScope || "FULL" : undefined,
        })),
      featureOverrides: applicationStates.flatMap((app) =>
        app.features
          .filter((f) => f.overrideType !== "NONE")
          .map((f) => ({ applicationId: app.id, featureId: f.id, overrideType: f.overrideType })),
      ),
    });
    message.success("用户功能配置已保存");
    setFeatureOpen(false);
  }

  const columns = useMemo<Array<BzTableColumn<ExternalUserEntry>>>(
    () => [
      { key: "account", title: "账号", minWidth: 180, render: (row) => <>{row.account}</> },
      {
        key: "nickname",
        title: "昵称",
        minWidth: 160,
        render: (row) => <>{row.nickname || "-"}</>,
      },
      {
        key: "status",
        title: "状态",
        width: 100,
        render: (row) => (
          <BzTag type={resolveStatusType(row.status)}>{resolveStatusLabel(row.status)}</BzTag>
        ),
      },
      {
        key: "lastLoginAt",
        title: "最近登录",
        minWidth: 160,
        render: (row) => <>{formatDateTime(row.lastLoginAt) || "-"}</>,
      },
      {
        key: "createdAt",
        title: "创建时间",
        minWidth: 160,
        render: (row) => <>{formatDateTime(row.createdAt) || "-"}</>,
      },
      {
        key: "actions",
        title: "操作",
        width: 160,
        render: (row) => <AdminActionBar actions={getRowActions(row)} />,
      },
    ],
    [canEdit, canFeatureManage],
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
                className={`admin-filter-form${queryCollapsed ? " is-collapsed" : ""}`}
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
                        placeholder="按账号或昵称搜索"
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
                        modelValue={statusDraft}
                        placeholder="全部状态"
                        clearable
                        onValueChange={(v) => setStatusDraft((v || "") as "" | ExternalUserStatus)}
                      >
                        <BzOption
                          label="启用"
                          value="ACTIVE"
                        />
                        <BzOption
                          label="停用"
                          value="DISABLED"
                        />
                        <BzOption
                          label="已注销"
                          value="CANCELLED"
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
                <div className="admin-table-title">用户管理</div>
                <div className="admin-table-tools">
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
                emptyText="暂无用户"
                size="small"
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

          <AdminEntityDrawer
            open={detailOpen}
            loading={detailLoading}
            title="用户详情"
            width="860px"
            onClose={() => setDetailOpen(false)}
            footer={<BzButton onClick={() => setDetailOpen(false)}>关闭</BzButton>}
          >
            {detail ? (
              <div className="detail-grid">
                <div className="detail-field">
                  <span className="detail-field__label">账号</span>
                  <span className="detail-field__value">{detail.account}</span>
                </div>
                <div className="detail-field">
                  <span className="detail-field__label">昵称</span>
                  <span className="detail-field__value">{detail.nickname || "-"}</span>
                </div>
                <div className="detail-field">
                  <span className="detail-field__label">状态</span>
                  <span className="detail-field__value">{resolveStatusLabel(detail.status)}</span>
                </div>
                <div className="detail-field">
                  <span className="detail-field__label">最近登录</span>
                  <span className="detail-field__value">
                    {formatDateTime(detail.lastLoginAt) || "-"}
                  </span>
                </div>
                <div className="detail-field">
                  <span className="detail-field__label">创建时间</span>
                  <span className="detail-field__value">
                    {formatDateTime(detail.createdAt) || "-"}
                  </span>
                </div>
                <div className="detail-field">
                  <span className="detail-field__label">更新时间</span>
                  <span className="detail-field__value">
                    {formatDateTime(detail.updatedAt) || "-"}
                  </span>
                </div>
              </div>
            ) : null}
          </AdminEntityDrawer>

          <AdminEntityDrawer
            open={featureOpen}
            loading={featureLoading}
            title="用户应用功能配置"
            width="1180px"
            onClose={() => setFeatureOpen(false)}
            footer={renderFeatureFooter()}
          >
            {featureManagement ? (
              <div className="feature-manage-layout">
                <div className="feature-manage-cards">
                  <div className="feature-manage-card">
                    <span>当前用户</span>
                    <strong>{featureManagement.account}</strong>
                  </div>
                  <div className="feature-manage-card">
                    <span>已选应用包</span>
                    <strong>{selectedPackageIds.length}</strong>
                  </div>
                  <div className="feature-manage-card">
                    <span>应用数</span>
                    <strong>{applicationStates.length}</strong>
                  </div>
                  <div className="feature-manage-card">
                    <span>功能项</span>
                    <strong>{flattenedFeatures.length}</strong>
                  </div>
                </div>

                <div className="feature-section">
                  <div className="feature-section__head">
                    <div className="feature-section__title">用户应用包</div>
                    <div className="feature-section__meta">勾选后参与权限继承计算</div>
                  </div>
                  <div className="package-option-grid">
                    {packageEntries.map((pkg) => (
                      <label
                        key={pkg.id}
                        className="package-option-card"
                      >
                        <input
                          type="checkbox"
                          checked={selectedPackageIds.includes(pkg.id)}
                          onChange={(e) => togglePackageSelection(pkg.id, e.target.checked)}
                        />
                        <div className="package-option-card__body">
                          <div className="package-option-card__top">
                            <strong>{pkg.name}</strong>
                            <BzTag type={pkg.defaultPackage ? "success" : "info"}>
                              {pkg.defaultPackage ? "默认包" : "普通包"}
                            </BzTag>
                          </div>
                          <div className="package-option-card__code">{pkg.code}</div>
                          <div className="package-option-card__desc">
                            {pkg.description || "无描述"}
                          </div>
                        </div>
                      </label>
                    ))}
                  </div>
                </div>

                <div className="feature-section">
                  <div className="feature-section__head">
                    <div className="feature-section__title">应用特例</div>
                    <div className="feature-section__meta">优先级高于应用包授权</div>
                  </div>
                  <div className="admin-table-surface">
                    <BzTable
                      columns={[
                        {
                          key: "name",
                          title: "应用",
                          minWidth: 180,
                          render: (row: UserFeatureUserApplicationEntry) => (
                            <div className="table-title-cell">
                              <strong>{row.name}</strong>
                              <small>{row.code}</small>
                            </div>
                          ),
                        },
                        {
                          key: "inheritedVisible",
                          title: "继承可见",
                          width: 100,
                          render: (row) => (
                            <BzTag type={row.inheritedVisible ? "success" : "info"}>
                              {row.inheritedVisible ? "是" : "否"}
                            </BzTag>
                          ),
                        },
                        {
                          key: "packageAccessScope",
                          title: "继承范围",
                          width: 110,
                          render: (row) => <>{row.packageAccessScope}</>,
                        },
                        {
                          key: "effectiveVisible",
                          title: "最终可见",
                          width: 100,
                          render: (row) => (
                            <BzTag type={row.effectiveVisible ? "success" : "warning"}>
                              {row.effectiveVisible ? "是" : "否"}
                            </BzTag>
                          ),
                        },
                        {
                          key: "overrideType",
                          title: "特例类型",
                          width: 140,
                          render: (row) => (
                            <select
                              className="inline-select"
                              value={row.overrideType}
                              onChange={(e) =>
                                updateApplicationOverride(
                                  row.id,
                                  e.target.value as UserFeatureOverrideType,
                                )
                              }
                            >
                              <option value="NONE">继承</option>
                              <option value="ENABLE">单独启用</option>
                              <option value="DISABLE">单独禁用</option>
                            </select>
                          ),
                        },
                        {
                          key: "overrideAccessScope",
                          title: "启用范围",
                          width: 140,
                          render: (row) => (
                            <select
                              className="inline-select"
                              disabled={row.overrideType !== "ENABLE"}
                              value={row.overrideAccessScope || "FULL"}
                              onChange={(e) =>
                                updateApplicationOverrideScope(
                                  row.id,
                                  e.target.value as UserFeatureAccessScope,
                                )
                              }
                            >
                              <option value="FULL">完整功能</option>
                              <option value="PARTIAL">部分功能</option>
                            </select>
                          ),
                        },
                      ]}
                      data={applicationStates}
                      rowKey="id"
                      size="small"
                      emptyText="暂无应用"
                    />
                  </div>
                </div>

                <div className="feature-section">
                  <div className="feature-section__head">
                    <div className="feature-section__title">功能特例</div>
                    <div className="feature-section__meta">用于补充单个功能的启用或禁用</div>
                  </div>
                  <div className="feature-filter-bar">
                    <BzInput
                      modelValue={featureKeyword}
                      placeholder="搜索应用、功能编码或名称"
                      clearable
                      onValueChange={setFeatureKeyword}
                    />
                    <select
                      className="inline-select inline-select--filter"
                      value={featureOverrideFilter}
                      onChange={(e) =>
                        setFeatureOverrideFilter(e.target.value as "" | UserFeatureOverrideType)
                      }
                    >
                      <option value="">全部特例</option>
                      <option value="NONE">继承</option>
                      <option value="ENABLE">单独启用</option>
                      <option value="DISABLE">单独禁用</option>
                    </select>
                  </div>
                  <div className="admin-table-surface">
                    <BzTable
                      columns={[
                        {
                          key: "applicationId",
                          title: "所属应用",
                          minWidth: 160,
                          render: (row) => <>{applicationName(row.applicationId)}</>,
                        },
                        { key: "code", title: "功能编码", minWidth: 170 },
                        { key: "name", title: "名称", minWidth: 160 },
                        {
                          key: "inheritedEnabled",
                          title: "继承可用",
                          width: 100,
                          render: (row) => (
                            <BzTag type={row.inheritedEnabled ? "success" : "info"}>
                              {row.inheritedEnabled ? "是" : "否"}
                            </BzTag>
                          ),
                        },
                        {
                          key: "effectiveEnabled",
                          title: "最终可用",
                          width: 100,
                          render: (row) => (
                            <BzTag type={row.effectiveEnabled ? "success" : "warning"}>
                              {row.effectiveEnabled ? "是" : "否"}
                            </BzTag>
                          ),
                        },
                        {
                          key: "overrideType",
                          title: "特例类型",
                          width: 140,
                          render: (row) => (
                            <select
                              className="inline-select"
                              value={row.overrideType}
                              onChange={(e) =>
                                updateFeatureOverride(
                                  row.applicationId,
                                  row.id,
                                  e.target.value as UserFeatureOverrideType,
                                )
                              }
                            >
                              <option value="NONE">继承</option>
                              <option value="ENABLE">单独启用</option>
                              <option value="DISABLE">单独禁用</option>
                            </select>
                          ),
                        },
                      ]}
                      data={filteredFeatures}
                      rowKey="id"
                      size="small"
                      emptyText="暂无功能项"
                    />
                  </div>
                </div>
              </div>
            ) : (
              <BzEmpty description="暂无数据" />
            )}
          </AdminEntityDrawer>
        </div>
      </div>
    </div>
  );
}
