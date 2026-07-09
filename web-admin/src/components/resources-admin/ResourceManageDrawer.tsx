"use client";

import {
  createResource,
  getResource,
  updateResource,
  updateResourcePermissions,
} from "@admin/api/resources";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import type { BzTableColumn } from "@admin/components/bz";
import {
  BzButton,
  BzInput,
  BzOption,
  BzSelect,
  BzSwitch,
  BzTable,
  BzTextField,
} from "@admin/components/bz";
import { message } from "@admin/core/message";
import { refreshRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import type {
  ManageResourceType,
  ResourceManageEntry,
  ResourceManagePermissionSelection,
  ResourceManageSaveRequest,
  ResourcePermissionOption,
} from "@admin/types/resource-manage";
import { type ReactNode, useEffect, useMemo, useState } from "react";

type DrawerMode = "create" | "detail" | "edit";

interface ResourceManageDrawerProps {
  mode: DrawerMode;
  resourceId: string | null;
  parentId: string | null;
  allResources: ResourceManageEntry[];
  permissions: ResourcePermissionOption[];
  canEdit: boolean;
  canPermissionEdit: boolean;
  onClose: () => void;
  onSaved: () => void;
}

interface ResourceFormState {
  id?: string;
  parentId: string;
  code: string;
  name: string;
  resourceType: ManageResourceType;
  path: string;
  component: string;
  icon: string;
  sortNo: number;
  visible: boolean;
  enabled: boolean;
  defaultEntry: boolean;
  systemBuiltin: boolean;
  remark: string;
  permissionIds: string[];
}

const RESOURCE_TYPE_LABEL: Record<ManageResourceType, string> = {
  DIRECTORY: "目录",
  MENU: "菜单",
  FUNCTION: "功能",
  BUTTON: "按钮",
};

const ALLOWED_CHILDREN: Record<ManageResourceType, ManageResourceType[]> = {
  DIRECTORY: ["DIRECTORY", "MENU"],
  MENU: ["MENU", "FUNCTION", "BUTTON"],
  FUNCTION: ["BUTTON"],
  BUTTON: [],
};

const ROOT_ALLOWED_TYPES: ManageResourceType[] = ["DIRECTORY", "MENU"];

const EMPTY_FORM: ResourceFormState = {
  parentId: "",
  code: "",
  name: "",
  resourceType: "DIRECTORY",
  path: "",
  component: "",
  icon: "",
  sortNo: 10,
  visible: true,
  enabled: true,
  defaultEntry: false,
  systemBuiltin: false,
  remark: "",
  permissionIds: [],
};

function flattenRows(rows: ResourceManageEntry[]): ResourceManageEntry[] {
  const result: ResourceManageEntry[] = [];
  const walk = (items: ResourceManageEntry[]) => {
    items.forEach((row) => {
      result.push(row);
      walk(row.children);
    });
  };
  walk(rows);
  return result;
}

function buildParentOptions(
  rows: ResourceManageEntry[],
  currentId?: string,
): Array<{ id: string; label: string }> {
  const excludeIds = new Set<string>();
  if (currentId) excludeIds.add(currentId);
  return flattenRows(rows)
    .filter((row) => canHaveChildren(row.resourceType) && !excludeIds.has(row.id))
    .map((row) => ({
      id: row.id,
      label: `${row.name}（${RESOURCE_TYPE_LABEL[row.resourceType]}）`,
    }));
}

function canHaveChildren(resourceType: ManageResourceType): boolean {
  return ALLOWED_CHILDREN[resourceType].length > 0;
}

function nextSortNo(rows: ResourceManageEntry[], parentId: string | null): number {
  const siblings = flattenRows(rows).filter((row) =>
    parentId ? row.parentId === parentId : !row.parentId,
  );
  const maxNo = siblings.reduce((max, row) => Math.max(max, row.sortNo || 0), 0);
  return Math.max(10, maxNo + 10);
}

export function ResourceManageDrawer({
  mode,
  resourceId,
  parentId,
  allResources,
  permissions,
  canEdit: _canEdit,
  canPermissionEdit,
  onClose,
  onSaved,
}: ResourceManageDrawerProps) {
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState<ResourceFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState("");

  const isDetail = mode === "detail";
  const editable = !isDetail;
  const typeLocked = mode === "edit"; // 编辑时不可更改资源类型

  const rowMap = useMemo(() => {
    const map = new Map<string, ResourceManageEntry>();
    flattenRows(allResources).forEach((item) => map.set(item.id, item));
    return map;
  }, [allResources]);

  const currentParent = form.parentId ? rowMap.get(form.parentId) : undefined;
  const allowedTypes = currentParent
    ? ALLOWED_CHILDREN[currentParent.resourceType]
    : ROOT_ALLOWED_TYPES;
  const showPermissionArea = form.resourceType === "BUTTON";
  const canSavePermissions = editable && showPermissionArea && canPermissionEdit;

  const parentOptions = useMemo(
    () => buildParentOptions(allResources, form.id),
    [allResources, form.id],
  );

  const selectedPermissions = useMemo(
    () => permissions.filter((p) => form.permissionIds.includes(p.id)),
    [form.permissionIds, permissions],
  );

  const permissionColumns = useMemo<Array<BzTableColumn<ResourcePermissionOption>>>(
    () => [
      { key: "name", title: "名称", minWidth: 160 },
      { key: "code", title: "编码", minWidth: 200 },
    ],
    [],
  );

  useEffect(() => {
    if (mode === "create") {
      const base = { ...EMPTY_FORM };
      if (parentId) {
        base.parentId = parentId;
        const parent = rowMap.get(parentId);
        if (parent) {
          const nextTypes = ALLOWED_CHILDREN[parent.resourceType];
          base.resourceType = nextTypes.length > 0 ? nextTypes[0] : "BUTTON";
        }
        base.sortNo = nextSortNo(allResources, parentId);
      } else {
        base.sortNo = nextSortNo(allResources, null);
      }
      setForm(base);
      setFormError("");
      return;
    }

    if (resourceId) {
      setLoading(true);
      getResource(resourceId)
        .then((detail) => {
          setForm({
            id: detail.id,
            parentId: detail.parentId ?? "",
            code: detail.code,
            name: detail.name,
            resourceType: detail.resourceType,
            path: detail.path ?? "",
            component: detail.component ?? "",
            icon: detail.icon ?? "",
            sortNo: detail.sortNo,
            visible: detail.visible,
            enabled: detail.enabled,
            defaultEntry: detail.defaultEntry,
            systemBuiltin: detail.systemBuiltin,
            remark: detail.remark ?? "",
            permissionIds: [...detail.permissionIds],
          });
          setFormError("");
        })
        .finally(() => setLoading(false));
    }
  }, [mode, resourceId, parentId]);

  function updateForm<K extends keyof ResourceFormState>(key: K, value: ResourceFormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleParentChange(value: string) {
    const parent = value ? rowMap.get(value) : undefined;
    const nextTypes = parent ? ALLOWED_CHILDREN[parent.resourceType] : ROOT_ALLOWED_TYPES;
    const nextType = nextTypes.includes(form.resourceType) ? form.resourceType : nextTypes[0];
    setForm((current) => ({
      ...current,
      parentId: value,
      resourceType: nextType,
      sortNo: current.id ? current.sortNo : nextSortNo(allResources, value || null),
    }));
  }

  function handleTypeChange(resourceType: ManageResourceType) {
    setForm((current) => ({
      ...current,
      resourceType,
      path: resourceType === "MENU" || resourceType === "FUNCTION" ? current.path : "",
      component: resourceType === "MENU" || resourceType === "FUNCTION" ? current.component : "",
      permissionIds: resourceType === "BUTTON" ? current.permissionIds : [],
    }));
  }

  function togglePermission(permissionId: string, checked: boolean) {
    setForm((current) => {
      const next = new Set(current.permissionIds);
      if (checked) next.add(permissionId);
      else next.delete(permissionId);
      return { ...current, permissionIds: Array.from(next) };
    });
  }

  function validateForm(): string {
    if (!form.name.trim()) return "资源名称不能为空";
    if (!form.code.trim()) return "资源编码不能为空";
    if (!Number.isFinite(form.sortNo)) return "排序号不能为空";
    if (!allowedTypes.includes(form.resourceType)) return "当前父级不允许创建该资源类型";
    if ((form.resourceType === "MENU" || form.resourceType === "FUNCTION") && !form.path.trim())
      return "菜单或功能资源必须填写路由路径";
    if (
      (form.resourceType === "MENU" || form.resourceType === "FUNCTION") &&
      !form.component.trim()
    )
      return "菜单或功能资源必须填写组件路径";
    return "";
  }

  async function handleSave() {
    if (isDetail) return;
    const error = validateForm();
    setFormError(error);
    if (error) return;

    const payload: ResourceManageSaveRequest = {
      parentId: form.parentId || null,
      code: form.code.trim(),
      name: form.name.trim(),
      resourceType: form.resourceType,
      path:
        form.resourceType === "MENU" || form.resourceType === "FUNCTION"
          ? blankToNull(form.path)
          : null,
      component:
        form.resourceType === "MENU" || form.resourceType === "FUNCTION"
          ? blankToNull(form.component)
          : null,
      icon:
        form.resourceType === "DIRECTORY" || form.resourceType === "MENU"
          ? blankToNull(form.icon)
          : null,
      sortNo: form.sortNo,
      visible: form.visible,
      enabled: form.enabled,
      defaultEntry:
        form.resourceType === "MENU" || form.resourceType === "FUNCTION"
          ? form.defaultEntry
          : false,
      systemBuiltin: form.systemBuiltin,
      remark: blankToNull(form.remark),
    };

    const permissionSelection: ResourceManagePermissionSelection = {
      permissionIds: form.resourceType === "BUTTON" ? [...form.permissionIds] : [],
    };

    if (mode === "create") {
      const created = await createResource(payload);
      if (form.resourceType === "BUTTON" && canSavePermissions) {
        await updateResourcePermissions(created.id, permissionSelection);
      }
      await refreshRegistryLoaded();
      message.success("资源创建成功");
    } else {
      if (!form.id) return;
      await updateResource(form.id, payload);
      if (form.resourceType === "BUTTON" && canSavePermissions) {
        await updateResourcePermissions(form.id, permissionSelection);
      }
      await refreshRegistryLoaded();
      message.success("资源更新成功");
    }
    onSaved();
    onClose();
  }

  // ---- 单元格渲染辅助函数 ----

  function renderCell(children: ReactNode, { mono }: { mono?: boolean } = {}) {
    return <td className={`role-info-cell${mono ? " mono" : ""}`}>{children}</td>;
  }

  function renderEditCell(children: ReactNode, { mono }: { mono?: boolean } = {}) {
    return (
      <td className={`role-info-cell role-info-cell--edit${mono ? " mono" : ""}`}>{children}</td>
    );
  }

  function renderValue(value: string | number, { mono }: { mono?: boolean } = {}) {
    return renderCell(value || "-", { mono });
  }

  function renderInput(
    value: string,
    placeholder: string,
    onChange: (v: string) => void,
    { mono, disabled }: { mono?: boolean; disabled?: boolean } = {},
  ) {
    if (disabled) return renderValue(value, { mono });
    return renderEditCell(
      <BzInput
        modelValue={value}
        placeholder={placeholder}
        className={`role-info-input${mono ? " mono" : ""}`}
        onValueChange={onChange}
      />,
      { mono },
    );
  }

  function renderTextarea(value: string, placeholder: string, onChange: (v: string) => void) {
    if (!editable) return renderCell(<pre className="admin-log-pre">{value || "-"}</pre>);
    return renderEditCell(
      <BzTextField
        modelValue={value}
        type="textarea"
        rows={3}
        placeholder={placeholder}
        onValueChange={onChange}
      />,
    );
  }

  function renderSwitch(
    value: boolean,
    onChange: (v: boolean) => void,
    { disabled }: { disabled?: boolean } = {},
  ) {
    if (disabled || !editable) return renderCell(value ? "是" : "否");
    return renderEditCell(
      <BzSwitch
        modelValue={value}
        disabled={disabled}
        onValueChange={onChange}
      />,
    );
  }

  function renderSelectCell(
    value: string,
    onChange: (v: string) => void,
    options: Array<{ value: string; label: string; disabled?: boolean }>,
    { disabled }: { disabled?: boolean } = {},
  ) {
    if (disabled || !editable) {
      const selected = options.find((o) => o.value === value);
      return renderCell(selected?.label || value || "-");
    }
    return renderEditCell(
      <BzSelect
        className="role-info-select"
        modelValue={value}
        onValueChange={(v) => onChange(v ?? "")}
      >
        {options.map((o) => (
          <BzOption
            key={o.value}
            value={o.value}
            label={o.label}
            disabled={o.disabled}
          />
        ))}
      </BzSelect>,
    );
  }

  function thRequired(required: boolean) {
    return <span className={required ? "is-required" : undefined} />;
  }

  const drawerTitle =
    mode === "create"
      ? "新增资源"
      : mode === "detail"
        ? `${RESOURCE_TYPE_LABEL[form.resourceType]}详情`
        : "编辑资源";

  const footer = isDetail ? (
    <BzButton onClick={onClose}>关闭</BzButton>
  ) : (
    <>
      <BzButton onClick={onClose}>取消</BzButton>
      <BzButton
        buttonType="primary"
        onClick={handleSave}
      >
        保存
      </BzButton>
    </>
  );

  const parentSelectOptions = [
    { value: "", label: "无父级，作为根资源" },
    ...parentOptions.map((o) => ({ value: o.id, label: o.label })),
  ];

  const typeSelectOptions = (
    ["DIRECTORY", "MENU", "FUNCTION", "BUTTON"] as ManageResourceType[]
  ).map((type) => ({
    value: type,
    label: RESOURCE_TYPE_LABEL[type],
    disabled: !allowedTypes.includes(type),
  }));

  const iconDisabled = !(form.resourceType === "DIRECTORY" || form.resourceType === "MENU");
  const routeDisabled = !(form.resourceType === "MENU" || form.resourceType === "FUNCTION");
  const defaultEntryDisabled = routeDisabled;

  return (
    <AdminEntityDrawer
      open
      className="role-manage-drawer"
      title={drawerTitle}
      width="1000px"
      loading={loading}
      onClose={onClose}
      footer={footer}
    >
      <div className="role-manage-shell">
        <section className="role-manage-section">
          <div className="role-manage-section__head">
            <div className="role-manage-section__title">基础信息</div>
          </div>

          <div className="role-info-table-wrap">
            <table
              className="role-info-table"
              aria-label="资源基础信息"
            >
              <tbody>
                <tr>
                  <th>父级资源</th>
                  {editable
                    ? renderSelectCell(form.parentId, handleParentChange, parentSelectOptions)
                    : renderCell(currentParent?.name || "-")}
                  <th>资源类型</th>
                  {editable && !typeLocked
                    ? renderSelectCell(
                        form.resourceType,
                        handleTypeChange as (v: string) => void,
                        typeSelectOptions,
                      )
                    : renderCell(RESOURCE_TYPE_LABEL[form.resourceType])}
                  <th>{thRequired(editable)}资源名称</th>
                  {editable
                    ? renderInput(form.name, "例如：资源管理", (v) => updateForm("name", v))
                    : renderValue(form.name)}
                </tr>
                <tr>
                  <th>{thRequired(editable)}资源编码</th>
                  {editable
                    ? renderInput(
                        form.code,
                        "例如：platform.resource",
                        (v) => updateForm("code", v),
                        { mono: true },
                      )
                    : renderValue(form.code, { mono: true })}
                  <th>图标</th>
                  {editable
                    ? renderInput(form.icon, "例如：Setting", (v) => updateForm("icon", v), {
                        disabled: iconDisabled,
                      })
                    : renderValue(form.icon)}
                  <th>排序号</th>
                  {editable
                    ? renderInput(
                        String(form.sortNo),
                        "例如：10",
                        (v) => updateForm("sortNo", Number(v || 0)),
                        { mono: true },
                      )
                    : renderValue(form.sortNo, { mono: true })}
                </tr>
                <tr>
                  <th>备注</th>
                  {editable
                    ? renderTextarea(form.remark, "资源说明", (v) => updateForm("remark", v))
                    : renderValue(form.remark)}
                  <th></th>
                  <td></td>
                  <th></th>
                  <td></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section className="role-manage-section">
          <div className="role-manage-section__head">
            <div className="role-manage-section__title">路由信息</div>
          </div>

          <div className="role-info-table-wrap">
            <table
              className="role-info-table"
              aria-label="资源路由信息"
            >
              <tbody>
                <tr>
                  <th>路由路径</th>
                  {editable
                    ? renderInput(
                        form.path,
                        "例如：/admin/resources",
                        (v) => updateForm("path", v),
                        { disabled: routeDisabled },
                      )
                    : renderValue(form.path)}
                  <th>组件路径</th>
                  {editable
                    ? renderInput(
                        form.component,
                        "例如：pages/ResourcesAdminPage",
                        (v) => updateForm("component", v),
                        { disabled: routeDisabled },
                      )
                    : renderValue(form.component)}
                  <th></th>
                  <td></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section className="role-manage-section">
          <div className="role-manage-section__head">
            <div className="role-manage-section__title">状态配置</div>
          </div>

          <div className="role-info-table-wrap">
            <table
              className="role-info-table"
              aria-label="资源状态配置"
            >
              <tbody>
                <tr>
                  <th>可见</th>
                  {renderSwitch(form.visible, (v) => updateForm("visible", v))}
                  <th>启用</th>
                  {renderSwitch(form.enabled, (v) => updateForm("enabled", v))}
                  <th>默认入口</th>
                  {renderSwitch(form.defaultEntry, (v) => updateForm("defaultEntry", v), {
                    disabled: defaultEntryDisabled,
                  })}
                </tr>
                <tr>
                  <th>系统内置</th>
                  {renderSwitch(form.systemBuiltin, (v) => updateForm("systemBuiltin", v))}
                  <th></th>
                  <td></td>
                  <th></th>
                  <td></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        {showPermissionArea ? (
          <section className="role-manage-section">
            <div className="role-manage-section__head">
              <div className="role-manage-section__title">权限码绑定</div>
              <div className="role-manage-section__stat">共 {selectedPermissions.length} 项</div>
            </div>
            {isDetail ? (
              <div className="admin-table-surface">
                <BzTable
                  columns={permissionColumns}
                  data={selectedPermissions}
                  rowKey="id"
                  size="small"
                  emptyText="暂无权限码绑定"
                />
              </div>
            ) : (
              <div className="resource-manage-permission-box">
                {permissions.map((permission) => {
                  const checked = form.permissionIds.includes(permission.id);
                  return (
                    <label
                      key={permission.id}
                      className="resource-manage-permission-item"
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        disabled={!canSavePermissions}
                        onChange={(event) => togglePermission(permission.id, event.target.checked)}
                      />
                      <span>{permission.name}</span>
                      <span className="resource-manage-permission-code">({permission.code})</span>
                    </label>
                  );
                })}
              </div>
            )}
          </section>
        ) : null}

        {formError ? (
          <div
            className="form-error"
            style={{ marginTop: 12 }}
          >
            {formError}
          </div>
        ) : null}
      </div>
    </AdminEntityDrawer>
  );
}

function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}
