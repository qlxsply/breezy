"use client";

import { batchListDictionaryOptions as batchListDictOptions } from "@admin/features/dicts/public/dictionary-client";
import {
  listUserFeaturePackageCatalog,
  type UserFeaturePackageEntry,
} from "@admin/features/user-feature-packages/public/catalog";
import { message } from "@admin/shared/lib/feedback/message";
import { formatDateTime } from "@admin/shared/lib/formatter";
import { type AdminDetailSection, AdminDetailTable } from "@admin/shared/ui/admin/AdminDetailTable";
import entityStyles from "@admin/shared/ui/admin/AdminEntity.module.css";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { TableCheckbox, TableSelect } from "@admin/shared/ui/admin/inputs";
import { BzButton, BzEmpty, BzInput, BzTag } from "@admin/shared/ui/bz";
import { useEffect, useRef, useState } from "react";

import {
  getUserFeatureUserManagement,
  getWebUser,
  saveUserFeatureUserManagement,
} from "../api/client";
import type {
  UserFeatureAccessScope,
  UserFeatureOverrideType,
  UserFeatureUserApplicationEntry,
  UserFeatureUserManagementEntry,
  WebUserEntry,
  WebUserStatus,
} from "../model/types";
import styles from "./WebUserFeatureDrawer.module.css";

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
  const [user, setUser] = useState<WebUserEntry | null>(null);
  const [management, setManagement] = useState<UserFeatureUserManagementEntry | null>(null);
  const [packages, setPackages] = useState<UserFeaturePackageEntry[]>([]);
  const [selectedPackageIds, setSelectedPackageIds] = useState<string[]>([]);
  const [applications, setApplications] = useState<UserFeatureUserApplicationEntry[]>([]);
  const [packageTypeLabels, setPackageTypeLabels] = useState(defaultPackageTypeLabels);
  const [keyword, setKeyword] = useState("");
  const loadControllerRef = useRef<AbortController | null>(null);
  const saveLockRef = useRef(false);

  useEffect(() => {
    loadControllerRef.current?.abort();
    if (!open || !userId) return;
    const controller = new AbortController();
    loadControllerRef.current = controller;
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
            getWebUser(userId, { signal: controller.signal }),
            canViewFeatures
              ? getUserFeatureUserManagement(userId, { signal: controller.signal })
              : Promise.resolve(null),
            mode === "maintain" && canManagePackages
              ? loadPackageCatalog(controller.signal)
              : Promise.resolve([]),
            loadPackageTypeLabels(controller.signal),
          ],
        );
        if (loadControllerRef.current !== controller || controller.signal.aborted) return;
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
      } catch (cause) {
        if (!controller.signal.aborted) {
          message.error(cause instanceof Error ? cause.message : "用户功能数据加载失败");
        }
      } finally {
        if (loadControllerRef.current === controller) {
          loadControllerRef.current = null;
          setLoading(false);
        }
      }
    })();
    return () => controller.abort();
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
    if (!userId || !editable || saveLockRef.current) return;
    saveLockRef.current = true;
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
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : "用户功能能力保存失败");
    } finally {
      saveLockRef.current = false;
      setSaving(false);
    }
  }

  return (
    <AdminEntityDrawer
      open={open}
      loading={loading}
      title={mode === "detail" ? "用户详情" : "用户维护"}
      width="1180px"
      className={`${entityStyles.manageDrawer} ${styles.drawer}`}
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
        <div className={`${entityStyles.shell} ${styles.featureShell}`}>
          <section className={entityStyles.section}>
            <div className={entityStyles.sectionHead}>
              <div className={entityStyles.sectionTitle}>用户信息</div>
            </div>
            <AdminDetailTable
              sections={basicSections}
              variant="plain"
            />
          </section>

          {!canViewFeatures ? (
            <section className={entityStyles.section}>
              <BzEmpty description="当前账号无权查看用户功能能力" />
            </section>
          ) : management ? (
            <>
              <section className={entityStyles.section}>
                <div className={entityStyles.sectionHead}>
                  <div className={entityStyles.sectionTitle}>应用包</div>
                  <div className={entityStyles.sectionStat}>
                    已分配 {selectedPackageIds.length} 个
                  </div>
                </div>
                <div className={layoutStyles.gridTable}>
                  <div className={`${layoutStyles.gridViewport} ${styles.packageViewport}`}>
                    <div
                      className={`${layoutStyles.gridRow} ${layoutStyles.gridHead} ${styles.packageHead}`}
                    >
                      <div className={`${layoutStyles.gridCell} ${layoutStyles.gridCheck}`} />
                      <div className={layoutStyles.gridCell}>应用包名称</div>
                      <div className={layoutStyles.gridCell}>应用包编码</div>
                      <div className={layoutStyles.gridCell}>类型</div>
                      <div className={layoutStyles.gridCell}>状态</div>
                      <div className={layoutStyles.gridCell}>默认包</div>
                    </div>
                    <div className={`${layoutStyles.gridBody} ${styles.packageBody}`}>
                      {displayedPackages.length > 0 ? (
                        displayedPackages.map((entry) => {
                          const selected = selectedPackageIds.includes(entry.id);
                          return (
                            <div
                              key={entry.id}
                              className={[
                                layoutStyles.gridRow,
                                styles.packageRow,
                                selected ? layoutStyles.selected : "",
                              ]
                                .filter(Boolean)
                                .join(" ")}
                            >
                              <div
                                className={`${layoutStyles.gridCell} ${layoutStyles.gridCheck}${editable ? ` ${layoutStyles.editable}` : ""}`}
                              >
                                <TableCheckbox
                                  value={selected}
                                  disabled={!editable || (!entry.enabled && !selected)}
                                  onValueChange={(checked) => updatePackages(entry.id, checked)}
                                />
                              </div>
                              <div
                                className={`${layoutStyles.gridCell} ${styles.name}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                              >
                                {entry.name}
                              </div>
                              <div
                                className={`${layoutStyles.gridCell} ${entityStyles.mono}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                              >
                                {entry.code}
                              </div>
                              <div
                                className={`${layoutStyles.gridCell}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                              >
                                {packageTypeLabels[entry.packageType] || entry.packageType}
                              </div>
                              <div
                                className={`${layoutStyles.gridCell}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                              >
                                <BzTag type={entry.enabled ? "success" : "danger"}>
                                  {entry.enabled ? "启用" : "停用"}
                                </BzTag>
                              </div>
                              <div
                                className={`${layoutStyles.gridCell}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                              >
                                <BzTag type={entry.defaultPackage ? "success" : "info"}>
                                  {entry.defaultPackage ? "是" : "否"}
                                </BzTag>
                              </div>
                            </div>
                          );
                        })
                      ) : (
                        <div className={entityStyles.permissionEmptyState}>暂未分配应用包</div>
                      )}
                    </div>
                  </div>
                </div>
              </section>

              <section className={entityStyles.section}>
                <div className={entityStyles.sectionHead}>
                  <div className={entityStyles.sectionTitle}>功能能力</div>
                  <div className={entityStyles.sectionStat}>
                    {applications.filter((application) => application.effectiveVisible).length}{" "}
                    个应用可用
                  </div>
                </div>
                <div className={`${entityStyles.permissionToolbar} ${styles.capabilityToolbar}`}>
                  <BzInput
                    modelValue={keyword}
                    placeholder="搜索应用名称或编码"
                    clearable
                    className={`${entityStyles.permissionToolbarSearch} ${styles.search}`}
                    onValueChange={setKeyword}
                  />
                  <div className={`${entityStyles.permissionToolbarActions} ${styles.actions}`}>
                    {editable ? (
                      <BzButton
                        className={entityStyles.permissionToolbarButton}
                        onClick={clearPersonalization}
                      >
                        重置
                      </BzButton>
                    ) : null}
                  </div>
                </div>
                <div className={layoutStyles.gridTable}>
                  <div className={`${layoutStyles.gridViewport} ${styles.capabilityViewport}`}>
                    <div
                      className={`${layoutStyles.gridRow} ${layoutStyles.gridHead} ${styles.capabilityHead}`}
                    >
                      <div className={layoutStyles.gridCell}>应用名称</div>
                      <div className={layoutStyles.gridCell}>应用编码</div>
                      <div className={layoutStyles.gridCell}>全局状态</div>
                      <div className={layoutStyles.gridCell}>应用包能力</div>
                      <div className={layoutStyles.gridCell}>个性化设置</div>
                      <div className={layoutStyles.gridCell}>最终状态</div>
                      <div className={layoutStyles.gridCell}>功能范围</div>
                    </div>
                    <div className={`${layoutStyles.gridBody} ${styles.capabilityBody}`}>
                      {displayedApplications.length > 0 ? (
                        displayedApplications.map((application) => (
                          <div
                            key={application.id}
                            className={`${layoutStyles.gridRow} ${styles.capabilityRow}`}
                          >
                            <div
                              className={`${layoutStyles.gridCell} ${styles.name}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                            >
                              {application.name}
                            </div>
                            <div
                              className={`${layoutStyles.gridCell} ${entityStyles.mono}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                            >
                              {application.code}
                            </div>
                            <div
                              className={`${layoutStyles.gridCell}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                            >
                              <BzTag type={application.enabled ? "success" : "danger"}>
                                {application.enabled ? "启用" : "停用"}
                              </BzTag>
                            </div>
                            <div
                              className={`${layoutStyles.gridCell}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                            >
                              {resolveScopeLabel(application.packageAccessScope)}
                            </div>
                            <div
                              className={`${layoutStyles.gridCell}${editable ? ` ${layoutStyles.editable}` : ""}`}
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
                              className={`${layoutStyles.gridCell}${editable ? ` ${layoutStyles.readonly}` : ""}`}
                            >
                              <BzTag type={application.effectiveVisible ? "success" : "warning"}>
                                {application.effectiveVisible ? "可见" : "不可见"}
                              </BzTag>
                            </div>
                            <div
                              className={`${layoutStyles.gridCell} ${styles.features}${editable ? ` ${layoutStyles.editable}` : ""}`}
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
                                <span className={styles.unselected}>暂无功能</span>
                              )}
                            </div>
                          </div>
                        ))
                      ) : (
                        <div className={entityStyles.permissionEmptyState}>暂无匹配应用或功能</div>
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

async function loadPackageCatalog(signal?: AbortSignal): Promise<UserFeaturePackageEntry[]> {
  return listUserFeaturePackageCatalog({ signal });
}

async function loadPackageTypeLabels(signal?: AbortSignal): Promise<Record<string, string>> {
  try {
    const result = await batchListDictOptions(["USER_APPLICATION_PACKAGE_TYPE"], { signal });
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

function resolveStatusLabel(status: WebUserStatus): string {
  if (status === "ACTIVE") return "启用";
  if (status === "DISABLED") return "停用";
  return "已注销";
}

function resolveStatusType(status: WebUserStatus): "success" | "danger" {
  return status === "ACTIVE" ? "success" : "danger";
}
