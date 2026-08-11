"use client";

import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { TableInput, TableSelect, TableTextArea } from "@admin/components/admin-inputs";
import { BzButton, BzCheckbox, BzInput, BzTag } from "@admin/components/bz";
import type {
  SaveUserFeaturePackageRequest,
  UserFeatureAccessScope,
  UserFeatureApplicationEntry,
  UserFeaturePackageApplicationAccessEntry,
  UserFeaturePackageEntry,
} from "@admin/types/user-feature";
import { useEffect, useState } from "react";

export type UserFeaturePackageDrawerMode = "create" | "detail" | "edit";

interface PackageTypeOption {
  label: string;
  value: string;
}

interface AccessDraft {
  featureAccessScope: UserFeatureAccessScope;
  featureIds: string[];
}

interface UserFeaturePackageManageDrawerProps {
  open: boolean;
  mode: UserFeaturePackageDrawerMode;
  packageEntry: UserFeaturePackageEntry | null;
  applications: UserFeatureApplicationEntry[];
  packageTypeOptions: PackageTypeOption[];
  loading?: boolean;
  saving?: boolean;
  onClose: () => void;
  onSubmit: (payload: SaveUserFeaturePackageRequest) => void | Promise<void>;
}

const statusOptions = [
  { value: "true", label: "启用" },
  { value: "false", label: "停用" },
];

const defaultPackageOptions = [
  { value: "true", label: "是" },
  { value: "false", label: "否" },
];

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

export function UserFeaturePackageManageDrawer({
  open,
  mode,
  packageEntry,
  applications,
  packageTypeOptions,
  loading = false,
  saving = false,
  onClose,
  onSubmit,
}: UserFeaturePackageManageDrawerProps) {
  const editable = mode !== "detail";
  const [form, setForm] = useState<SaveUserFeaturePackageRequest>(defaultForm());
  const [accesses, setAccesses] = useState<Record<string, AccessDraft>>({});
  const [keyword, setKeyword] = useState("");

  useEffect(() => {
    if (!open) return;
    setKeyword("");
    if (!packageEntry) {
      setForm(defaultForm());
      setAccesses({});
      return;
    }
    setForm({
      code: packageEntry.code,
      name: packageEntry.name,
      packageType: packageEntry.packageType,
      description: packageEntry.description || "",
      enabled: packageEntry.enabled,
      defaultPackage: packageEntry.defaultPackage,
      applicationAccesses: [],
    });
    setAccesses(toAccessDraft(packageEntry.applicationAccesses));
  }, [open, packageEntry]);

  const rows = resolveApplicationRows(mode, packageEntry, applications);
  const normalizedKeyword = keyword.trim().toLocaleLowerCase();
  const filteredRows = rows.filter(
    (application) =>
      !normalizedKeyword ||
      application.name.toLocaleLowerCase().includes(normalizedKeyword) ||
      application.code.toLocaleLowerCase().includes(normalizedKeyword) ||
      application.features.some(
        (feature) =>
          feature.name.toLocaleLowerCase().includes(normalizedKeyword) ||
          feature.code.toLocaleLowerCase().includes(normalizedKeyword),
      ),
  );
  const selectedApplicationCount = Object.keys(accesses).length;

  function toggleApplication(applicationId: string, checked: boolean) {
    setAccesses((current) => {
      const next = { ...current };
      if (checked) {
        next[applicationId] = next[applicationId] || {
          featureAccessScope: "FULL",
          featureIds: [],
        };
      } else {
        delete next[applicationId];
      }
      return next;
    });
  }

  function toggleAllFeatures(applicationId: string, checked: boolean) {
    setAccesses((current) => {
      const entry = current[applicationId];
      if (!entry) return current;
      return {
        ...current,
        [applicationId]: {
          featureAccessScope: checked ? "FULL" : "PARTIAL",
          featureIds: [],
        },
      };
    });
  }

  function toggleFeature(applicationId: string, featureId: string, checked: boolean) {
    setAccesses((current) => {
      const entry = current[applicationId];
      if (!entry || entry.featureAccessScope === "FULL") return current;
      const featureIds = new Set(entry.featureIds);
      if (checked) featureIds.add(featureId);
      else featureIds.delete(featureId);
      return {
        ...current,
        [applicationId]: { ...entry, featureIds: Array.from(featureIds) },
      };
    });
  }

  function buildPayload(): SaveUserFeaturePackageRequest {
    return {
      code: form.code.trim(),
      name: form.name.trim(),
      packageType: form.packageType,
      description: form.description?.trim() || null,
      enabled: form.enabled,
      defaultPackage: form.defaultPackage,
      applicationAccesses: Object.entries(accesses).map(([applicationId, access]) => ({
        applicationId,
        featureAccessScope: access.featureAccessScope,
        featureIds: access.featureAccessScope === "FULL" ? [] : access.featureIds,
      })),
    };
  }

  const footer = (
    <>
      <BzButton onClick={onClose}>{mode === "detail" ? "关闭" : "取消"}</BzButton>
      {editable ? (
        <BzButton
          buttonType="primary"
          disabled={saving || (mode === "edit" && !packageEntry)}
          onClick={() => void onSubmit(buildPayload())}
        >
          确定
        </BzButton>
      ) : null}
    </>
  );

  return (
    <AdminEntityDrawer
      open={open}
      className="role-manage-drawer user-feature-package-drawer"
      title={mode === "create" ? "新增应用包" : mode === "edit" ? "编辑应用包" : "应用包详情"}
      width="1180px"
      loading={loading}
      onClose={onClose}
      footer={footer}
    >
      <div className="role-manage-shell">
        <section className="role-manage-section">
          <div className="role-manage-section__head">
            <div className="role-manage-section__title">应用包信息</div>
          </div>
          <div className="role-info-table-wrap">
            <table
              className="role-info-table"
              aria-label="应用包信息"
            >
              <tbody>
                <tr>
                  <th>
                    <span className={mode === "create" ? "is-required" : undefined}>编码</span>
                  </th>
                  {mode === "create" ? (
                    <td className="role-info-cell role-info-cell--edit mono">
                      <TableInput
                        value={form.code}
                        maxLength={128}
                        placeholder="请输入应用包编码"
                        onValueChange={(code) => setForm((current) => ({ ...current, code }))}
                      />
                    </td>
                  ) : (
                    <td className="role-info-cell mono">{form.code || "-"}</td>
                  )}
                  <th>
                    <span className={editable ? "is-required" : undefined}>名称</span>
                  </th>
                  {editable ? (
                    <td className="role-info-cell role-info-cell--edit">
                      <TableInput
                        value={form.name}
                        maxLength={128}
                        placeholder="请输入应用包名称"
                        onValueChange={(name) => setForm((current) => ({ ...current, name }))}
                      />
                    </td>
                  ) : (
                    <td className="role-info-cell">{form.name || "-"}</td>
                  )}
                  <th>类型</th>
                  {editable ? (
                    <td className="role-info-cell role-info-cell--edit">
                      <TableSelect
                        value={form.packageType}
                        options={packageTypeOptions}
                        onValueChange={(value) =>
                          setForm((current) => ({ ...current, packageType: String(value) }))
                        }
                      />
                    </td>
                  ) : (
                    <td className="role-info-cell">
                      {resolveOptionLabel(packageTypeOptions, form.packageType)}
                    </td>
                  )}
                </tr>
                <tr>
                  <th>状态</th>
                  {editable ? (
                    <td className="role-info-cell role-info-cell--edit">
                      <TableSelect
                        value={String(form.enabled)}
                        options={statusOptions}
                        onValueChange={(value) =>
                          setForm((current) => ({ ...current, enabled: value === "true" }))
                        }
                      />
                    </td>
                  ) : (
                    <td className="role-info-cell">
                      <BzTag type={form.enabled ? "success" : "danger"}>
                        {form.enabled ? "启用" : "停用"}
                      </BzTag>
                    </td>
                  )}
                  <th>默认包</th>
                  {editable ? (
                    <td className="role-info-cell role-info-cell--edit">
                      <TableSelect
                        value={String(form.defaultPackage)}
                        options={defaultPackageOptions}
                        onValueChange={(value) =>
                          setForm((current) => ({ ...current, defaultPackage: value === "true" }))
                        }
                      />
                    </td>
                  ) : (
                    <td className="role-info-cell">
                      <BzTag type={form.defaultPackage ? "success" : "info"}>
                        {form.defaultPackage ? "是" : "否"}
                      </BzTag>
                    </td>
                  )}
                  <th>授权应用</th>
                  <td className="role-info-cell">{selectedApplicationCount} 个</td>
                </tr>
                <tr>
                  <th>描述</th>
                  {editable ? (
                    <td
                      className="role-info-cell role-info-cell--edit"
                      colSpan={5}
                    >
                      <TableTextArea
                        value={form.description || ""}
                        rows={3}
                        maxLength={512}
                        placeholder="请输入应用包描述"
                        onValueChange={(description) =>
                          setForm((current) => ({ ...current, description }))
                        }
                      />
                    </td>
                  ) : (
                    <td
                      className="role-info-cell user-feature-package-description-cell"
                      colSpan={5}
                    >
                      {form.description || "-"}
                    </td>
                  )}
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section className="role-manage-section">
          <div className="role-manage-section__head">
            <div className="role-manage-section__title">应用授权</div>
            <div className="role-manage-section__stat">
              已选 {selectedApplicationCount} / {rows.length} 个应用
            </div>
          </div>
          <div className="role-permission-toolbar user-feature-access-toolbar">
            <BzInput
              modelValue={keyword}
              placeholder="搜索应用或功能"
              clearable
              className="role-permission-toolbar__search"
              onValueChange={setKeyword}
            />
            {editable ? (
              <BzButton
                className="permission-toolbar-button"
                onClick={() => setAccesses({})}
              >
                清空选择
              </BzButton>
            ) : null}
          </div>
          <div className="admin-grid-table role-permission-table user-feature-access-table">
            <div className="admin-grid-table__viewport">
              <div className="admin-grid-table__row admin-grid-table__row--head user-feature-access-table__head">
                <div className="admin-grid-table__cell admin-grid-table__cell--check" />
                <div className="admin-grid-table__cell">应用名称</div>
                <div className="admin-grid-table__cell">应用编码</div>
                <div className="admin-grid-table__cell">状态</div>
                <div className="admin-grid-table__cell">功能范围</div>
              </div>
              <div className="admin-grid-table__body">
                {filteredRows.length === 0 ? (
                  <div className="permission-empty">暂无可授权应用</div>
                ) : (
                  filteredRows.map((application) => {
                    const access = accesses[application.id];
                    const selected = Boolean(access);
                    const allSelected = access?.featureAccessScope === "FULL";
                    return (
                      <div
                        key={application.id}
                        className={[
                          "admin-grid-table__row",
                          "user-feature-access-table__row",
                          selected ? "is-selected" : "",
                        ]
                          .filter(Boolean)
                          .join(" ")}
                      >
                        <div className="admin-grid-table__cell admin-grid-table__cell--check">
                          <BzCheckbox
                            modelValue={selected}
                            disabled={!editable || (!application.enabled && !selected)}
                            onValueChange={(checked) => toggleApplication(application.id, checked)}
                          />
                        </div>
                        <div className="admin-grid-table__cell user-feature-access-table__name">
                          {application.name}
                        </div>
                        <div className="admin-grid-table__cell mono">{application.code}</div>
                        <div className="admin-grid-table__cell">
                          <BzTag type={application.enabled ? "success" : "danger"}>
                            {application.enabled ? "启用" : "停用"}
                          </BzTag>
                        </div>
                        <div className="admin-grid-table__cell user-feature-access-table__features">
                          {selected ? (
                            <>
                              <BzCheckbox
                                modelValue={allSelected}
                                disabled={!editable}
                                onValueChange={(checked) =>
                                  toggleAllFeatures(application.id, checked)
                                }
                              >
                                全部
                              </BzCheckbox>
                              {application.features.map((feature) => (
                                <BzCheckbox
                                  key={feature.id}
                                  modelValue={
                                    allSelected || Boolean(access?.featureIds.includes(feature.id))
                                  }
                                  disabled={
                                    !editable ||
                                    allSelected ||
                                    (!feature.enabled && !access?.featureIds.includes(feature.id))
                                  }
                                  onValueChange={(checked) =>
                                    toggleFeature(application.id, feature.id, checked)
                                  }
                                >
                                  {feature.name}
                                </BzCheckbox>
                              ))}
                            </>
                          ) : (
                            <span className="user-feature-access-table__unselected">未授权</span>
                          )}
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>
        </section>
      </div>
    </AdminEntityDrawer>
  );
}

function toAccessDraft(
  accesses: UserFeaturePackageApplicationAccessEntry[],
): Record<string, AccessDraft> {
  return Object.fromEntries(
    accesses.map((access) => [
      access.applicationId,
      {
        featureAccessScope: access.featureAccessScope,
        featureIds: [...access.featureIds],
      },
    ]),
  );
}

function resolveApplicationRows(
  mode: UserFeaturePackageDrawerMode,
  packageEntry: UserFeaturePackageEntry | null,
  applications: UserFeatureApplicationEntry[],
): UserFeatureApplicationEntry[] {
  if (mode !== "detail") return applications;
  if (!packageEntry) return [];
  return packageEntry.applicationAccesses.map((access) => {
    const application = applications.find((item) => item.id === access.applicationId);
    return (
      application || {
        id: access.applicationId,
        code: access.applicationCode,
        name: access.applicationName,
        enabled: true,
        featureCount: access.features.length,
        permissionBindingCount: 0,
        features: access.features,
      }
    );
  });
}

function resolveOptionLabel(options: PackageTypeOption[], value: string): string {
  return options.find((option) => option.value === value)?.label || value || "-";
}
