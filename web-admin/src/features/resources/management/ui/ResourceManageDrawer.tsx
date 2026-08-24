"use client";

import {
  createResource,
  getResource,
  getResourcePermissions,
  updateResource,
  updateResourcePermissions,
} from "@admin/features/resources/management/api/client";
import type {
  ManageResourceType,
  ResourceManageEntry,
  ResourceManagePermissionSelection,
  ResourceManageSaveRequest,
  ResourcePermissionOption,
} from "@admin/features/resources/management/model/types";
import styles from "@admin/features/resources/management/ui/ResourceManageDrawer.module.css";
import { message } from "@admin/shared/lib/feedback/message";
import entityStyles from "@admin/shared/ui/admin/AdminEntity.module.css";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import { AdminInfoCell } from "@admin/shared/ui/admin/AdminInfoCell";
import {
  TableCheckbox,
  TableInput,
  TableSelect,
  TableTextArea,
} from "@admin/shared/ui/admin/inputs";
import { BzButton, BzSwitch } from "@admin/shared/ui/bz";
import { type ReactNode, useEffect, useMemo, useRef, useState } from "react";

type DrawerMode = "create" | "detail" | "edit";

interface ResourceManageDrawerProps {
  mode: DrawerMode;
  resourceId: string | null;
  parentId: string | null;
  allResources: ResourceManageEntry[];
  permissions: ResourcePermissionOption[];
  canEdit: boolean;
  canPermissionView: boolean;
  canPermissionEdit: boolean;
  onClose: () => void;
  onSaved: () => Promise<void>;
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
  canEdit,
  canPermissionView,
  canPermissionEdit,
  onClose,
  onSaved,
}: ResourceManageDrawerProps) {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<ResourceFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const saveSequenceRef = useRef(0);
  const savingRef = useRef(false);
  const drawerIdentity = `${mode}:${resourceId ?? ""}:${parentId ?? ""}`;
  const drawerIdentityRef = useRef(drawerIdentity);
  drawerIdentityRef.current = drawerIdentity;

  const isDetail = mode === "detail";
  const editable = !isDetail && canEdit;
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
  const showPermissionArea =
    form.resourceType === "BUTTON" && (canPermissionView || canPermissionEdit);
  const canSavePermissions = editable && showPermissionArea && canPermissionEdit;

  const parentOptions = useMemo(
    () => buildParentOptions(allResources, form.id),
    [allResources, form.id],
  );

  useEffect(() => {
    saveSequenceRef.current += 1;
    savingRef.current = false;
    setSaving(false);
  }, [drawerIdentity]);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

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
      setLoading(false);
      return () => controller.abort();
    }

    if (resourceId) {
      setLoading(true);
      void getResource(resourceId, { signal: controller.signal })
        .then(async (detail) => {
          const permissionIds =
            detail.resourceType === "BUTTON" && (canPermissionView || canPermissionEdit)
              ? (await getResourcePermissions(resourceId, { signal: controller.signal }))
                  .permissionIds
              : [];

          if (!active) return;
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
            permissionIds: [...permissionIds],
          });
          setFormError("");
        })
        .catch((error: unknown) => {
          if (!active || isAbortError(error)) return;
          setFormError(
            error instanceof Error && error.message ? error.message : "资源详情加载失败",
          );
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    } else {
      setLoading(false);
    }

    return () => {
      active = false;
      controller.abort();
    };
  }, [canPermissionView, mode, parentId, resourceId]);

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
    if (!editable || savingRef.current) return;
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

    const successMessage = mode === "create" ? "资源创建成功" : "资源更新成功";
    const saveIdentity = drawerIdentity;
    const saveSequence = ++saveSequenceRef.current;
    savingRef.current = true;
    setSaving(true);
    try {
      if (mode === "create") {
        const created = await createResource(payload);
        if (form.resourceType === "BUTTON" && canSavePermissions) {
          await updateResourcePermissions(created.id, permissionSelection);
        }
      } else {
        if (!form.id) return;
        await updateResource(form.id, payload);
        if (form.resourceType === "BUTTON" && canSavePermissions) {
          await updateResourcePermissions(form.id, permissionSelection);
        }
      }
      await onSaved();
      if (!isCurrentSave(saveSequence, saveIdentity)) return;
      message.success(successMessage);
      savingRef.current = false;
      setSaving(false);
      onClose();
    } catch (cause) {
      if (isCurrentSave(saveSequence, saveIdentity)) {
        setFormError(cause instanceof Error ? cause.message : "资源保存失败");
      }
    } finally {
      if (isCurrentSave(saveSequence, saveIdentity)) {
        savingRef.current = false;
        setSaving(false);
      }
    }
  }

  function isCurrentSave(sequence: number, identity: string): boolean {
    return sequence === saveSequenceRef.current && identity === drawerIdentityRef.current;
  }

  // ---- 单元格渲染辅助函数 ----

  function renderCell(
    children: ReactNode,
    { mono, colSpan }: { mono?: boolean; colSpan?: number } = {},
  ) {
    return (
      <AdminInfoCell
        state={isDetail ? "display" : "readonly"}
        mono={mono}
        colSpan={colSpan}
      >
        {children}
      </AdminInfoCell>
    );
  }

  function renderValue(
    value: string | number,
    { mono, colSpan }: { mono?: boolean; colSpan?: number } = {},
  ) {
    return renderCell(value || "-", { mono, colSpan });
  }

  function renderInput(
    value: string,
    placeholder: string,
    onChange: (v: string) => void,
    {
      mono,
      disabled,
      maxLength,
      type,
    }: {
      mono?: boolean;
      disabled?: boolean;
      maxLength?: number;
      type?: "text" | "number";
    } = {},
  ) {
    if (disabled) return renderValue(value, { mono });
    return (
      <AdminInfoCell
        state="editable"
        mono={mono}
      >
        <TableInput
          value={value}
          type={type}
          maxLength={maxLength}
          placeholder={placeholder}
          onValueChange={onChange}
        />
      </AdminInfoCell>
    );
  }

  function renderTextarea(
    value: string,
    placeholder: string,
    onChange: (v: string) => void,
    colSpan?: number,
  ) {
    if (!editable)
      return renderCell(<pre className={entityStyles.preformattedValue}>{value || "-"}</pre>, {
        colSpan,
      });
    return (
      <AdminInfoCell
        state="editable"
        colSpan={colSpan}
      >
        <TableTextArea
          value={value}
          rows={3}
          maxLength={512}
          placeholder={placeholder}
          onValueChange={onChange}
        />
      </AdminInfoCell>
    );
  }

  function renderSwitch(
    value: boolean,
    onChange: (v: boolean) => void,
    { disabled }: { disabled?: boolean } = {},
  ) {
    if (disabled || !editable) return renderCell(value ? "是" : "否");
    return (
      <AdminInfoCell state="editable">
        <span className={styles.statusSwitch}>
          <BzSwitch
            modelValue={value}
            disabled={disabled}
            onValueChange={onChange}
          />
        </span>
      </AdminInfoCell>
    );
  }

  function renderSelectCell(
    value: string,
    onChange: (v: string) => void,
    options: Array<{ value: string; label: string; disabled?: boolean }>,
    { disabled, showSearch }: { disabled?: boolean; showSearch?: boolean } = {},
  ) {
    if (disabled || !editable) {
      const selected = options.find((option) => option.value === value);
      return renderCell(selected?.label || value || "-");
    }
    return (
      <AdminInfoCell state="editable">
        <TableSelect
          value={value}
          options={options}
          allowClear={false}
          showSearch={showSearch}
          onValueChange={(nextValue) =>
            onChange(Array.isArray(nextValue) ? "" : String(nextValue ?? ""))
          }
        />
      </AdminInfoCell>
    );
  }

  function thRequired(required: boolean) {
    return <span className={required ? entityStyles.required : undefined} />;
  }

  const drawerTitle =
    mode === "create"
      ? "新增资源"
      : mode === "detail"
        ? `${RESOURCE_TYPE_LABEL[form.resourceType]}详情`
        : "编辑资源";

  const footer = !editable ? (
    <BzButton
      disabled={saving}
      onClick={onClose}
    >
      关闭
    </BzButton>
  ) : (
    <>
      <BzButton
        disabled={saving}
        onClick={onClose}
      >
        取消
      </BzButton>
      <BzButton
        buttonType="primary"
        loading={saving}
        disabled={saving}
        onClick={() => void handleSave()}
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
      className={`${entityStyles.manageDrawer} ${styles.root}`}
      title={drawerTitle}
      width="1000px"
      loading={loading}
      closeDisabled={saving}
      onClose={onClose}
      footer={footer}
    >
      <div className={entityStyles.shell}>
        <section className={entityStyles.section}>
          <div className={entityStyles.sectionHead}>
            <div className={entityStyles.sectionTitle}>基础信息</div>
          </div>

          <div className={entityStyles.infoTableWrap}>
            <table
              className={entityStyles.infoTable}
              aria-label="资源基础信息"
            >
              <tbody>
                <tr>
                  <th>父级资源</th>
                  {editable
                    ? renderSelectCell(form.parentId, handleParentChange, parentSelectOptions, {
                        showSearch: true,
                      })
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
                    ? renderInput(form.name, "例如：资源管理", (v) => updateForm("name", v), {
                        maxLength: 128,
                      })
                    : renderValue(form.name)}
                </tr>
                <tr>
                  <th>{thRequired(editable)}资源编码</th>
                  {editable
                    ? renderInput(
                        form.code,
                        "例如：platform.resource",
                        (v) => updateForm("code", v),
                        { mono: true, maxLength: 128 },
                      )
                    : renderValue(form.code, { mono: true })}
                  <th>图标</th>
                  {editable
                    ? renderInput(form.icon, "例如：Setting", (v) => updateForm("icon", v), {
                        disabled: iconDisabled,
                        maxLength: 128,
                      })
                    : renderValue(form.icon)}
                  <th>排序号</th>
                  {editable
                    ? renderInput(
                        String(form.sortNo),
                        "例如：10",
                        (v) => updateForm("sortNo", Number(v || 0)),
                        { mono: true, maxLength: 10, type: "number" },
                      )
                    : renderValue(form.sortNo, { mono: true })}
                </tr>
                <tr>
                  <th>备注</th>
                  {editable
                    ? renderTextarea(form.remark, "资源说明", (v) => updateForm("remark", v), 5)
                    : renderValue(form.remark, { colSpan: 5 })}
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section className={entityStyles.section}>
          <div className={entityStyles.sectionHead}>
            <div className={entityStyles.sectionTitle}>路由信息</div>
          </div>

          <div className={entityStyles.infoTableWrap}>
            <table
              className={entityStyles.infoTable}
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
                        { disabled: routeDisabled, maxLength: 512 },
                      )
                    : renderValue(form.path)}
                  <th>组件路径</th>
                  {editable
                    ? renderInput(
                        form.component,
                        "例如：features/resources/management/ResourcesPage",
                        (v) => updateForm("component", v),
                        { disabled: routeDisabled, maxLength: 512 },
                      )
                    : renderValue(form.component)}
                  <th></th>
                  <td></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section className={entityStyles.section}>
          <div className={entityStyles.sectionHead}>
            <div className={entityStyles.sectionTitle}>状态配置</div>
          </div>

          <div className={entityStyles.infoTableWrap}>
            <table
              className={entityStyles.infoTable}
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
          <section className={entityStyles.section}>
            <div className={entityStyles.sectionHead}>
              <div className={entityStyles.sectionTitle}>权限码绑定</div>
              <div className={entityStyles.sectionStat}>共 {form.permissionIds.length} 项</div>
            </div>
            <div
              className={`${styles.permissionBox}${canSavePermissions ? "" : ` ${styles.readonly}`}`}
            >
              {permissions.length ? (
                permissions.map((permission) => {
                  const checked = form.permissionIds.includes(permission.id);
                  return (
                    <TableCheckbox
                      key={permission.id}
                      className={styles.permissionItem}
                      value={checked}
                      disabled={!canSavePermissions}
                      onValueChange={(value) => togglePermission(permission.id, value)}
                    >
                      <span>{permission.name}</span>
                      <span className={styles.permissionCode}>({permission.code})</span>
                    </TableCheckbox>
                  );
                })
              ) : (
                <div className={entityStyles.infoCellReadonly}>暂无权限码绑定</div>
              )}
            </div>
          </section>
        ) : null}

        {formError ? <div className={styles.formError}>{formError}</div> : null}
      </div>
    </AdminEntityDrawer>
  );
}

function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}
