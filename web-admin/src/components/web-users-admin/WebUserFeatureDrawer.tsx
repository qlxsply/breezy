"use client";

import {
  getUserFeatureUserManagement,
  pageUserFeaturePackages,
  saveUserFeatureUserManagement,
} from "@admin/api/user-features";
import { getExternalUser } from "@admin/api/web-users";
import { batchListDictionaryOptions as batchListDictOptions } from "@admin/features/dicts/public/dictionary-client";
import { message } from "@admin/shared/lib/feedback/message";
import { formatDateTime } from "@admin/shared/lib/formatter";
import { type AdminDetailSection, AdminDetailTable } from "@admin/shared/ui/admin/AdminDetailTable";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import { TableCheckbox, TableSelect } from "@admin/shared/ui/admin/inputs";
import { BzButton, BzEmpty, BzInput, BzTag } from "@admin/shared/ui/bz";
import type { ExternalUserEntry, ExternalUserStatus } from "@admin/types/external-user-admin";
import type {
  UserFeatureAccessScope,
  UserFeatureOverrideType,
  UserFeaturePackageEntry,
  UserFeatureUserApplicationEntry,
  UserFeatureUserManagementEntry,
} from "@admin/types/user-feature";
import { useEffect, useState } from "react";

export type WebUserFeatureDrawerMode = "detail" | "maintain";
type ApplicationAccessMode = "INHERIT" | "FULL" | "PARTIAL" | "DISABLE";

const defaultPackageTypeLabels: Record<string, string> = {
  DEFAULT: "默认包",
  MEMBERSHIP: "会员包",
  OPERATION: "运营包",
  ENTERPRISE: "企业包",
  CUSTOM: "自定义",
};

interface WebUserFeatureDrawerProps {
  open: boolean;
  mode: WebUserFeatureDrawerMode;
  userId: string | null;
  canViewFeatures: boolean;
  canManageFeatures: boolean;
  canManagePackages: boolean;
  onClose: () => void;
}

export function WebUserFeatureDrawer({
  open,
  mode,
  userId,
  canViewFeatures,
  canManageFeatures,
  canManagePackages,
  onClose,
}: WebUserFeatureDrawerProps) {
  const editable = mode === "maintain" && canManageFeatures && canManagePackages;
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [user, setUser] = useState<ExternalUserEntry | null>(null);
  const [management, setManagement] = useState<UserFeatureUserManagementEntry | null>(null);
  const [packages, setPackages] = useState<UserFeaturePackageEntry[]>([]);
  const [selectedPackageIds, setSelectedPackageIds] = useState<string[]>([]);
  const [applications, setApplications] = useState<UserFeatureUserApplicationEntry[]>([]);
  const [packageTypeLabels, setPackageTypeLabels] = useState(defaultPackageTypeLabels);
  const [keyword, setKeyword] = useState("");

  useEffect(() => {
    if (!open || !userId) return;
    setLoading(true);
    setUser(null);
    setManagement(null);
    setPackages([]);
    setSelectedPackageIds([]);
    setApplications([]);
    setKeyword("");
    void (async () => {
      try {
        const [nextUser, nextManagement, packageCatalog, nextPackageTypeLabels] = await Promise.all(
          [
            getExternalUser(userId),
            canViewFeatures ? getUserFeatureUserManagement(userId) : Promise.resolve(null),
            mode === "maintain" && canManagePackages ? loadPackageCatalog() : Promise.resolve([]),
            loadPackageTypeLabels(),
          ],
        );
        setUser(nextUser);
        setPackageTypeLabels(nextPackageTypeLabels);
        if (nextManagement) {
          const nextApplications = cloneApplications(nextManagement.applications);
          setManagement(nextManagement);
          setSelectedPackageIds([...nextManagement.packageIds]);
          setPackages(packageCatalog);
          setApplications(
            packageCatalog.length > 0
              ? deriveEffectiveState(nextApplications, nextManagement.packageIds, packageCatalog)
              : nextApplications,
          );
        }
      } finally {
        setLoading(false);
      }
    })();
  }, [open, userId, mode, canViewFeatures, canManagePackages]);

  const basicSections: AdminDetailSection[] = user
    ? [
        {
          title: "用户信息",
          fields: [
            { label: "账号", value: user.account },
            { label: "昵称", value: user.nickname || "-" },
            {
              label: "状态",
              value: (
                <BzTag type={resolveStatusType(user.status)}>
                  {resolveStatusLabel(user.status)}
                </BzTag>
              ),
            },
            { label: "最近登录", value: formatDateTime(user.lastLoginAt) || "-" },
            { label: "创建时间", value: formatDateTime(user.createdAt) || "-" },
            { label: "更新时间", value: formatDateTime(user.updatedAt) || "-" },
          ],
        },
      ]
    : [];

  const normalizedKeyword = keyword.trim().toLocaleLowerCase();
  const displayedApplications = applications.filter(
    (application) =>
      !normalizedKeyword ||
      application.name.toLocaleLowerCase().includes(normalizedKeyword) ||
      application.code.toLocaleLowerCase().includes(normalizedKeyword),
  );
  const displayedPackages = [
    ...(mode === "maintain" && canManagePackages ? packages : management?.packages || []),
  ].sort(comparePackages);

  function updatePackages(packageId: string, checked: boolean) {
    const nextIds = checked
      ? Array.from(new Set([...selectedPackageIds, packageId]))
      : selectedPackageIds.filter((id) => id !== packageId);
    setSelectedPackageIds(nextIds);
    setApplications((current) =>
      deriveEffectiveState(resetPersonalization(current), nextIds, packages),
    );
  }

  function clearPersonalization() {
    setApplications((current) =>
      deriveEffectiveState(resetPersonalization(current), selectedPackageIds, packages),
    );
  }

  function updateApplicationMode(applicationId: string, accessMode: ApplicationAccessMode) {
    setApplications((current) => {
      const next = current.map((application) =>
        application.id === applicationId
          ? {
              ...application,
              overrideType: resolveApplicationOverrideType(accessMode),
              overrideAccessScope: resolveApplicationOverrideScope(accessMode),
            }
          : application,
      );
      return deriveEffectiveState(next, selectedPackageIds, packages);
    });
  }

  function updateFeatureEnabled(applicationId: string, featureId: string, enabled: boolean) {
    setApplications((current) => {
      const next = current.map((application) => {
        if (application.id !== applicationId) return application;
        let nextApplication = application;
        const accessMode = resolveApplicationAccessMode(application);
        if (enabled && accessMode === "INHERIT" && !application.inheritedVisible) {
          nextApplication = {
            ...application,
            overrideType: "ENABLE",
            overrideAccessScope: "PARTIAL",
          };
        }
        return {
          ...nextApplication,
          features: application.features.map((feature) =>
            feature.id === featureId
              ? {
                  ...feature,
                  overrideType: resolveFeatureOverride(nextApplication, feature, enabled),
                }
              : feature,
          ),
        };
      });
      return deriveEffectiveState(next, selectedPackageIds, packages);
    });
  }

  async function save() {
    if (!userId || !editable || saving) return;
    setSaving(true);
    try {
      await saveUserFeatureUserManagement(userId, {
        packageIds: selectedPackageIds,
        applicationOverrides: applications
          .filter((application) => application.overrideType !== "NONE")
          .map((application) => ({
            applicationId: application.id,
            overrideType: application.overrideType,
            featureAccessScope:
              application.overrideType === "ENABLE"
                ? application.overrideAccessScope || "FULL"
                : undefined,
          })),
        featureOverrides: applications.flatMap((application) =>
          application.features
            .filter((feature) => feature.overrideType !== "NONE")
            .map((feature) => ({
              applicationId: application.id,
              featureId: feature.id,
              overrideType: feature.overrideType,
            })),
        ),
      });
      message.success("用户功能能力已更新");
      onClose();
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminEntityDrawer
      open={open}
      loading={loading}
      title={mode === "detail" ? "用户详情" : "用户维护"}
      width="1180px"
      className="admin-entity-manage-drawer web-user-feature-drawer"
      onClose={onClose}
      footer={
        <>
          <BzButton onClick={onClose}>{mode === "detail" ? "关闭" : "取消"}</BzButton>
          {mode === "maintain" ? (
            <BzButton
              buttonType="primary"
              disabled={!editable || saving}
              onClick={save}
            >
              确定
            </BzButton>
          ) : null}
        </>
      }
    >
      {user ? (
        <div className="admin-entity-shell web-user-feature-shell">
          <section className="admin-entity-section">
            <div className="admin-entity-section__head">
              <div className="admin-entity-section__title">用户信息</div>
            </div>
            <AdminDetailTable
              sections={basicSections}
              variant="plain"
            />
          </section>

          {!canViewFeatures ? (
            <section className="admin-entity-section">
              <BzEmpty description="当前账号无权查看用户功能能力" />
            </section>
          ) : management ? (
            <>
              <section className="admin-entity-section">
                <div className="admin-entity-section__head">
                  <div className="admin-entity-section__title">应用包</div>
                  <div className="admin-entity-section__stat">
                    已分配 {selectedPackageIds.length} 个
                  </div>
                </div>
                <div className="admin-grid-table admin-permission-table web-user-package-table">
                  <div className="admin-grid-table__viewport">
                    <div className="admin-grid-table__row admin-grid-table__row--head web-user-package-table__head">
                      <div className="admin-grid-table__cell admin-grid-table__cell--check" />
                      <div className="admin-grid-table__cell">应用包名称</div>
                      <div className="admin-grid-table__cell">应用包编码</div>
                      <div className="admin-grid-table__cell">类型</div>
                      <div className="admin-grid-table__cell">状态</div>
                      <div className="admin-grid-table__cell">默认包</div>
                    </div>
                    <div className="admin-grid-table__body">
                      {displayedPackages.length > 0 ? (
                        displayedPackages.map((entry) => {
                          const selected = selectedPackageIds.includes(entry.id);
                          return (
                            <div
                              key={entry.id}
                              className={[
                                "admin-grid-table__row",
                                "web-user-package-table__row",
                                selected ? "is-selected" : "",
                              ]
                                .filter(Boolean)
                                .join(" ")}
                            >
                              <div
                                className={`admin-grid-table__cell admin-grid-table__cell--check${editable ? " admin-grid-table__cell--editable" : ""}`}
                              >
                                <TableCheckbox
                                  value={selected}
                                  disabled={!editable || (!entry.enabled && !selected)}
                                  onValueChange={(checked) => updatePackages(entry.id, checked)}
                                />
                              </div>
                              <div
                                className={`admin-grid-table__cell web-user-package-table__name${editable ? " admin-grid-table__cell--readonly" : ""}`}
                              >
                                {entry.name}
                              </div>
                              <div
                                className={`admin-grid-table__cell mono${editable ? " admin-grid-table__cell--readonly" : ""}`}
                              >
                                {entry.code}
                              </div>
                              <div
                                className={`admin-grid-table__cell${editable ? " admin-grid-table__cell--readonly" : ""}`}
                              >
                                {packageTypeLabels[entry.packageType] || entry.packageType}
                              </div>
                              <div
                                className={`admin-grid-table__cell${editable ? " admin-grid-table__cell--readonly" : ""}`}
                              >
                                <BzTag type={entry.enabled ? "success" : "danger"}>
                                  {entry.enabled ? "启用" : "停用"}
                                </BzTag>
                              </div>
                              <div
                                className={`admin-grid-table__cell${editable ? " admin-grid-table__cell--readonly" : ""}`}
                              >
                                <BzTag type={entry.defaultPackage ? "success" : "info"}>
                                  {entry.defaultPackage ? "是" : "否"}
                                </BzTag>
                              </div>
                            </div>
                          );
                        })
                      ) : (
                        <div className="admin-permission-empty-state">暂未分配应用包</div>
                      )}
                    </div>
                  </div>
                </div>
              </section>

              <section className="admin-entity-section">
                <div className="admin-entity-section__head">
                  <div className="admin-entity-section__title">功能能力</div>
                  <div className="admin-entity-section__stat">
                    {applications.filter((application) => application.effectiveVisible).length}{" "}
                    个应用可用
                  </div>
                </div>
                <div className="admin-permission-toolbar web-user-capability-toolbar">
                  <BzInput
                    modelValue={keyword}
                    placeholder="搜索应用名称或编码"
                    clearable
                    className="admin-permission-toolbar__search"
                    onValueChange={setKeyword}
                  />
                  <div className="admin-permission-toolbar__actions">
                    {editable ? (
                      <BzButton
                        className="admin-permission-toolbar-button"
                        onClick={clearPersonalization}
                      >
                        重置
                      </BzButton>
                    ) : null}
                  </div>
                </div>
                <div className="admin-grid-table admin-permission-table web-user-capability-table">
                  <div className="admin-grid-table__viewport">
                    <div className="admin-grid-table__row admin-grid-table__row--head web-user-capability-table__head">
                      <div className="admin-grid-table__cell">应用名称</div>
                      <div className="admin-grid-table__cell">应用编码</div>
                      <div className="admin-grid-table__cell">全局状态</div>
                      <div className="admin-grid-table__cell">应用包能力</div>
                      <div className="admin-grid-table__cell">个性化设置</div>
                      <div className="admin-grid-table__cell">最终状态</div>
                      <div className="admin-grid-table__cell">功能范围</div>
                    </div>
                    <div className="admin-grid-table__body">
                      {displayedApplications.length > 0 ? (
                        displayedApplications.map((application) => (
                          <div
                            key={application.id}
                            className="admin-grid-table__row web-user-capability-table__row"
                          >
                            <div
                              className={`admin-grid-table__cell web-user-capability-table__name${editable ? " admin-grid-table__cell--readonly" : ""}`}
                            >
                              {application.name}
                            </div>
                            <div
                              className={`admin-grid-table__cell mono${editable ? " admin-grid-table__cell--readonly" : ""}`}
                            >
                              {application.code}
                            </div>
                            <div
                              className={`admin-grid-table__cell${editable ? " admin-grid-table__cell--readonly" : ""}`}
                            >
                              <BzTag type={application.enabled ? "success" : "danger"}>
                                {application.enabled ? "启用" : "停用"}
                              </BzTag>
                            </div>
                            <div
                              className={`admin-grid-table__cell${editable ? " admin-grid-table__cell--readonly" : ""}`}
                            >
                              {resolveScopeLabel(application.packageAccessScope)}
                            </div>
                            <div
                              className={`admin-grid-table__cell${editable ? " admin-grid-table__cell--editable" : ""}`}
                            >
                              {editable ? (
                                <TableSelect
                                  value={resolveApplicationAccessMode(application)}
                                  options={[
                                    { label: "继承应用包", value: "INHERIT" },
                                    { label: "开放全部功能", value: "FULL" },
                                    { label: "按功能开放", value: "PARTIAL" },
                                    { label: "禁止访问", value: "DISABLE" },
                                  ]}
                                  allowClear={false}
                                  disabled={!application.enabled}
                                  onValueChange={(value) =>
                                    updateApplicationMode(
                                      application.id,
                                      (Array.isArray(value)
                                        ? value[0]
                                        : value) as ApplicationAccessMode,
                                    )
                                  }
                                />
                              ) : (
                                resolveApplicationModeLabel(application)
                              )}
                            </div>
                            <div
                              className={`admin-grid-table__cell${editable ? " admin-grid-table__cell--readonly" : ""}`}
                            >
                              <BzTag type={application.effectiveVisible ? "success" : "warning"}>
                                {application.effectiveVisible ? "可见" : "不可见"}
                              </BzTag>
                            </div>
                            <div
                              className={`admin-grid-table__cell user-feature-access-table__features web-user-capability-table__features${editable ? " admin-grid-table__cell--editable" : ""}`}
                            >
                              {application.features.length > 0 ? (
                                <>
                                  <TableCheckbox
                                    value={isFullFeatureRange(application)}
                                    disabled
                                  >
                                    全部
                                  </TableCheckbox>
                                  {application.features.map((feature) => (
                                    <TableCheckbox
                                      key={feature.id}
                                      value={
                                        isFullFeatureRange(application) || feature.effectiveEnabled
                                      }
                                      disabled={
                                        !editable ||
                                        isFullFeatureRange(application) ||
                                        !application.enabled ||
                                        !feature.enabled ||
                                        application.overrideType === "DISABLE"
                                      }
                                      onValueChange={(enabled) =>
                                        updateFeatureEnabled(application.id, feature.id, enabled)
                                      }
                                    >
                                      {feature.name}
                                    </TableCheckbox>
                                  ))}
                                </>
                              ) : (
                                <span className="user-feature-access-table__unselected">
                                  暂无功能
                                </span>
                              )}
                            </div>
                          </div>
                        ))
                      ) : (
                        <div className="admin-permission-empty-state">暂无匹配应用或功能</div>
                      )}
                    </div>
                  </div>
                </div>
              </section>
            </>
          ) : (
            <BzEmpty description="暂无用户功能数据" />
          )}
        </div>
      ) : null}
    </AdminEntityDrawer>
  );
}

async function loadPackageCatalog(): Promise<UserFeaturePackageEntry[]> {
  const firstPage = await pageUserFeaturePackages({
    page: { pageNo: 1, pageSize: 100 },
    sort: { orders: [{ field: "packageName", direction: "ASC" }] },
  });
  if (firstPage.totalPages <= 1) return firstPage.elements;
  const remainingPages = await Promise.all(
    Array.from({ length: firstPage.totalPages - 1 }, (_, index) =>
      pageUserFeaturePackages({
        page: { pageNo: index + 2, pageSize: 100 },
        sort: { orders: [{ field: "packageName", direction: "ASC" }] },
      }),
    ),
  );
  return [firstPage, ...remainingPages].flatMap((page) => page.elements);
}

async function loadPackageTypeLabels(): Promise<Record<string, string>> {
  try {
    const result = await batchListDictOptions(["USER_APPLICATION_PACKAGE_TYPE"]);
    return {
      ...defaultPackageTypeLabels,
      ...Object.fromEntries(
        (result.USER_APPLICATION_PACKAGE_TYPE || []).map((item) => [
          item.itemValue,
          item.itemLabel || item.itemValue,
        ]),
      ),
    };
  } catch {
    return defaultPackageTypeLabels;
  }
}

function comparePackages(
  left: { id: string; name: string; code: string },
  right: { id: string; name: string; code: string },
): number {
  return (
    left.name.localeCompare(right.name, "zh-CN", { numeric: true }) ||
    left.code.localeCompare(right.code, "zh-CN", { numeric: true }) ||
    left.id.localeCompare(right.id, undefined, { numeric: true })
  );
}

function cloneApplications(
  applications: UserFeatureUserApplicationEntry[],
): UserFeatureUserApplicationEntry[] {
  return applications.map((application) => ({
    ...application,
    features: application.features.map((feature) => ({ ...feature })),
  }));
}

function resetPersonalization(
  applications: UserFeatureUserApplicationEntry[],
): UserFeatureUserApplicationEntry[] {
  return applications.map((application) => ({
    ...application,
    overrideType: "NONE",
    overrideAccessScope: null,
    features: application.features.map((feature) => ({
      ...feature,
      overrideType: "NONE",
    })),
  }));
}

function deriveEffectiveState(
  applications: UserFeatureUserApplicationEntry[],
  selectedPackageIds: string[],
  packages: UserFeaturePackageEntry[],
): UserFeatureUserApplicationEntry[] {
  const activePackages = packages.filter(
    (entry) => entry.enabled && selectedPackageIds.includes(entry.id),
  );
  return applications.map((application) => {
    const accesses = activePackages.flatMap((entry) =>
      entry.applicationAccesses.filter((access) => access.applicationId === application.id),
    );
    const fullAccess = accesses.some((access) => access.featureAccessScope === "FULL");
    const inheritedFeatureIds = new Set(
      accesses.flatMap((access) =>
        access.featureAccessScope === "PARTIAL" ? access.featureIds : [],
      ),
    );
    const inheritedVisible = accesses.length > 0;
    const packageAccessScope = (
      fullAccess ? "FULL" : inheritedVisible ? "PARTIAL" : "NONE"
    ) as UserFeatureUserApplicationEntry["packageAccessScope"];
    const effectiveVisible =
      application.enabled &&
      (application.overrideType === "DISABLE"
        ? false
        : application.overrideType === "ENABLE"
          ? true
          : inheritedVisible);
    const features = application.features.map((feature) => {
      const inheritedEnabled = fullAccess || inheritedFeatureIds.has(feature.id);
      let effectiveEnabled = inheritedEnabled;
      if (!application.enabled || !feature.enabled || application.overrideType === "DISABLE") {
        effectiveEnabled = false;
      } else if (feature.overrideType === "DISABLE") {
        effectiveEnabled = false;
      } else if (
        application.overrideType === "ENABLE" &&
        application.overrideAccessScope === "FULL"
      ) {
        effectiveEnabled = true;
      } else if (
        application.overrideType === "ENABLE" &&
        application.overrideAccessScope === "PARTIAL"
      ) {
        effectiveEnabled = feature.overrideType === "ENABLE";
      } else if (!inheritedEnabled && feature.overrideType === "ENABLE" && inheritedVisible) {
        effectiveEnabled = true;
      }
      return { ...feature, inheritedEnabled, effectiveEnabled };
    });
    return {
      ...application,
      inheritedVisible,
      packageAccessScope,
      effectiveVisible,
      features,
    };
  });
}

function resolveFeatureOverride(
  application: UserFeatureUserApplicationEntry,
  feature: UserFeatureUserApplicationEntry["features"][number],
  enabled: boolean,
): UserFeatureOverrideType {
  const mode = resolveApplicationAccessMode(application);
  if (enabled) {
    if (mode === "PARTIAL") return "ENABLE";
    return feature.inheritedEnabled || mode === "FULL" ? "NONE" : "ENABLE";
  }
  if (mode === "PARTIAL") return "NONE";
  return feature.inheritedEnabled || mode === "FULL" ? "DISABLE" : "NONE";
}

function resolveApplicationAccessMode(
  application: UserFeatureUserApplicationEntry,
): ApplicationAccessMode {
  if (application.overrideType === "DISABLE") return "DISABLE";
  if (application.overrideType === "ENABLE") {
    return application.overrideAccessScope === "PARTIAL" ? "PARTIAL" : "FULL";
  }
  return "INHERIT";
}

function isFullFeatureRange(application: UserFeatureUserApplicationEntry): boolean {
  const accessMode = resolveApplicationAccessMode(application);
  return (
    accessMode === "FULL" || (accessMode === "INHERIT" && application.packageAccessScope === "FULL")
  );
}

function resolveApplicationOverrideType(mode: ApplicationAccessMode): UserFeatureOverrideType {
  if (mode === "DISABLE") return "DISABLE";
  if (mode === "FULL" || mode === "PARTIAL") return "ENABLE";
  return "NONE";
}

function resolveApplicationOverrideScope(
  mode: ApplicationAccessMode,
): UserFeatureAccessScope | null {
  if (mode === "FULL") return "FULL";
  if (mode === "PARTIAL") return "PARTIAL";
  return null;
}

function resolveApplicationModeLabel(application: UserFeatureUserApplicationEntry): string {
  const mode = resolveApplicationAccessMode(application);
  if (mode === "FULL") return "开放全部功能";
  if (mode === "PARTIAL") return "按功能开放";
  if (mode === "DISABLE") return "禁止访问";
  return "继承应用包";
}

function resolveScopeLabel(scope: UserFeatureUserApplicationEntry["packageAccessScope"]): string {
  if (scope === "FULL") return "全部功能";
  if (scope === "PARTIAL") return "部分功能";
  return "未授权";
}

function resolveStatusLabel(status: ExternalUserStatus): string {
  if (status === "ACTIVE") return "启用";
  if (status === "DISABLED") return "停用";
  return "已注销";
}

function resolveStatusType(status: ExternalUserStatus): "success" | "danger" {
  return status === "ACTIVE" ? "success" : "danger";
}
