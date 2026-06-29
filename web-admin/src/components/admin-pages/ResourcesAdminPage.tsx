"use client";

import {
  createResource,
  deleteResource,
  getResource,
  listPermissions,
  listResources,
  updateResource,
  updateResourcePermissions,
} from "@admin/api/resources";
import { AdminActionBar } from "@admin/components/admin/AdminActionBar";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import {
  BzButton,
  BzCard,
  BzFormItem,
  BzInput,
  BzOption,
  BzSelect,
  BzSwitch,
} from "@admin/components/bz";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { refreshRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type {
  ManageResourceType,
  ResourceManageEntry,
  ResourceManagePermissionSelection,
  ResourceManageSaveRequest,
  ResourcePermissionOption,
} from "@admin/types/resource-manage";
import { useEffect, useMemo, useState } from "react";

type DrawerPurpose = "create" | "edit";

interface ResourceTableRow {
  row: ResourceManageEntry;
  level: number;
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

const RESOURCE_TYPE_CLASS: Record<ManageResourceType, string> = {
  DIRECTORY: "is-directory",
  MENU: "is-menu",
  FUNCTION: "is-function",
  BUTTON: "is-button",
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

export function ResourcesAdminPage() {
  const [rows, setRows] = useState<ResourceManageEntry[]>([]);
  const [permissions, setPermissions] = useState<ResourcePermissionOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [typeFilterDraft, setTypeFilterDraft] = useState("");
  const [enabledFilterDraft, setEnabledFilterDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [enabledFilter, setEnabledFilter] = useState("");
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerPurpose, setDrawerPurpose] = useState<DrawerPurpose>("create");
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [form, setForm] = useState<ResourceFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState("");

  const canView = hasResourceCodeAccess("resource-manage-view");
  const canCreate = hasResourceCodeAccess("resource-manage-create");
  const canEdit = hasResourceCodeAccess("resource-manage-edit");
  const canDelete = hasResourceCodeAccess("resource-manage-delete");
  const canPermissionView = hasResourceCodeAccess("resource-manage-permission-view");
  const canPermissionEdit = hasResourceCodeAccess("resource-manage-permission-edit");
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  useEffect(() => {
    void reload();
  }, []);

  const flatRows = useMemo(() => flattenRows(rows), [rows]);

  const rowMap = useMemo(() => {
    const map = new Map<string, ResourceManageEntry>();
    flatRows.forEach((item) => map.set(item.id, item));
    return map;
  }, [flatRows]);

  const hasActiveFilter = Boolean(keyword.trim() || typeFilter || enabledFilter);

  const filteredRoots = useMemo(
    () => filterTree(rows, keyword.trim().toLowerCase(), typeFilter, enabledFilter),
    [rows, keyword, typeFilter, enabledFilter],
  );

  const tableRows = useMemo(
    () => flattenVisibleRows(filteredRoots, expandedIds, hasActiveFilter),
    [expandedIds, filteredRoots, hasActiveFilter],
  );

  const parentOptions = useMemo(() => buildParentOptions(rows, form.id), [rows, form.id]);

  const currentParent = form.parentId ? rowMap.get(form.parentId) : undefined;
  const allowedTypes = currentParent ? ALLOWED_CHILDREN[currentParent.resourceType] : ROOT_ALLOWED_TYPES;
  const canHaveChildren = (resourceType: ManageResourceType) => ALLOWED_CHILDREN[resourceType].length > 0;
  const showPermissionArea = form.resourceType === "BUTTON" && (canPermissionView || canPermissionEdit);
  const canSavePermissions = showPermissionArea && canPermissionEdit;
  const readOnly =
    drawerPurpose === "create"
        ? !canCreate
        : !canEdit;
  const structureReadOnly = readOnly;

  async function reload() {
    if (!canView) return;
    setLoading(true);
    try {
      const [resourceTree, permissionRows] = await Promise.all([
        listResources(),
        canPermissionView || canPermissionEdit ? listPermissions() : Promise.resolve<ResourcePermissionOption[]>([]),
      ]);
      setRows(resourceTree);
      setPermissions(permissionRows);
      setExpandedIds(new Set(flattenRows(resourceTree).map((item) => item.id)));
    } finally {
      setLoading(false);
    }
  }

  function updateForm<K extends keyof ResourceFormState>(key: K, value: ResourceFormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function applyFilters() {
    setKeyword(keywordDraft.trim());
    setTypeFilter(typeFilterDraft);
    setEnabledFilter(enabledFilterDraft);
    setExpandedIds(new Set(flattenRows(rows).map((item) => item.id)));
  }

  function openCreateRoot() {
    if (!canCreate) return;
    setDrawerPurpose("create");
    setForm({ ...EMPTY_FORM, sortNo: nextSortNo(rows, null), resourceType: "DIRECTORY" });
    setFormError("");
    setDrawerOpen(true);
  }

  function openCreateChild(target: ResourceManageEntry) {
    if (!canCreate) return;
    const nextTypes = ALLOWED_CHILDREN[target.resourceType];
    if (nextTypes.length === 0) return;
    setDrawerPurpose("create");
    setForm({
      ...EMPTY_FORM,
      parentId: target.id,
      resourceType: nextTypes[0],
      sortNo: nextSortNo(rows, target.id),
      visible: true,
      enabled: true,
    });
    setFormError("");
    setDrawerOpen(true);
  }

  async function openEdit(target: ResourceManageEntry, purpose: DrawerPurpose = "edit") {
    if (purpose === "edit" && !canEdit) return;
    setDrawerPurpose(purpose);
    setDrawerLoading(true);
    setDrawerOpen(true);
    setFormError("");
    try {
      const detail = await getResource(target.id);
      setForm(toForm(detail));
    } finally {
      setDrawerLoading(false);
    }
  }

  async function onDelete(target: ResourceManageEntry) {
    if (!canDelete) return;
    if (target.systemBuiltin) {
      message.warning("系统内置资源不允许删除");
      return;
    }
    const descendantCount = countDescendants(rowMap.get(target.id) ?? target);
    const confirmed = await bzConfirm({
      title: "删除资源",
      content:
        descendantCount > 0
          ? `确认删除「${target.name}」及其 ${descendantCount} 个子资源？`
          : `确认删除「${target.name}」？`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteResource(target.id);
    await refreshRegistryLoaded();
    message.success("删除成功");
    await reload();
  }

  function closeDrawer() {
    setDrawerOpen(false);
    setDrawerLoading(false);
    setForm(EMPTY_FORM);
    setFormError("");
  }

  function toggleExpand(id: string) {
    setExpandedIds((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function expandAll() {
    setExpandedIds(new Set(flattenRows(filteredRoots).map((item) => item.id)));
  }

  function collapseAll() {
    setExpandedIds(new Set());
  }

  function resetFilters() {
    setKeywordDraft("");
    setTypeFilterDraft("");
    setEnabledFilterDraft("");
    setKeyword("");
    setTypeFilter("");
    setEnabledFilter("");
    setExpandedIds(new Set(flattenRows(rows).map((item) => item.id)));
  }

  function handleParentChange(parentId: string) {
    const parent = parentId ? rowMap.get(parentId) : undefined;
    const nextTypes = parent ? ALLOWED_CHILDREN[parent.resourceType] : ROOT_ALLOWED_TYPES;
    const nextType = nextTypes.includes(form.resourceType) ? form.resourceType : nextTypes[0];
    setForm((current) => ({
      ...current,
      parentId,
      resourceType: nextType,
      sortNo: current.id ? current.sortNo : nextSortNo(rows, parentId || null),
    }));
  }

  function handleTypeChange(resourceType: ManageResourceType) {
    setForm((current) => ({
      ...current,
      resourceType,
      path: resourceType === "MENU" || resourceType === "FUNCTION" ? current.path : "",
      component: resourceType === "MENU" || resourceType === "FUNCTION" ? current.component : "",
      icon: resourceType === "DIRECTORY" || resourceType === "MENU" ? current.icon : "",
      defaultEntry:
        resourceType === "MENU" || resourceType === "FUNCTION" ? current.defaultEntry : false,
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
    if ((form.resourceType === "MENU" || form.resourceType === "FUNCTION") && !form.path.trim()) {
      return "菜单或功能资源必须填写路由路径";
    }
    if ((form.resourceType === "MENU" || form.resourceType === "FUNCTION") && !form.component.trim()) {
      return "菜单或功能资源必须填写组件路径";
    }
    return "";
  }

  async function handleSubmit() {
    if (readOnly) return;
    const error = validateForm();
    setFormError(error);
    if (error) return;
    const payload = toSaveRequest(form);
    const permissionSelection: ResourceManagePermissionSelection = {
      permissionIds: form.resourceType === "BUTTON" ? [...form.permissionIds] : [],
    };
    if (drawerPurpose === "create") {
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
    closeDrawer();
    await reload();
  }

  const drawerFooter = readOnly ? (
    <div className="permission-dialog-footer__actions">
      <BzButton onClick={closeDrawer}>关闭</BzButton>
    </div>
  ) : (
    <div className="permission-dialog-footer__actions">
      <BzButton onClick={closeDrawer}>取消</BzButton>
      <BzButton buttonType="primary" onClick={handleSubmit}>
        保存
      </BzButton>
    </div>
  );

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          {queryPanelVisible ? (
            <BzCard className="admin-panel admin-filter-card" shadow="never">
              <div
                ref={queryCardRef}
                className={[
                  "admin-query-layout",
                  querySingleRow ? "is-single-row" : queryExpanded ? "is-expanded" : "is-collapsed",
                ].join(" ")}
              >
                <div className="admin-query-header">
                  <div className="admin-query-title">筛选条件</div>
                </div>
                <form
                  ref={queryGridRef}
                  className="bz-form admin-query-grid"
                  onSubmit={(event) => {
                    event.preventDefault();
                    applyFilters();
                  }}
                >
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">关键字</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={keywordDraft}
                        placeholder="搜索资源名称、编码、路径、组件"
                        clearable
                        onValueChange={setKeywordDraft}
                        onKeyUp={(event) => {
                          if (event.key === "Enter") applyFilters();
                        }}
                      />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">资源类型</div>
                    <div className="admin-query-field__control">
                      <BzSelect
                        modelValue={typeFilterDraft}
                        placeholder="全部类型"
                        clearable
                        onValueChange={(value) => setTypeFilterDraft(value ?? "")}
                      >
                        <BzOption value="DIRECTORY" label="目录" />
                        <BzOption value="MENU" label="菜单" />
                        <BzOption value="FUNCTION" label="功能" />
                        <BzOption value="BUTTON" label="按钮" />
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">启用状态</div>
                    <div className="admin-query-field__control">
                      <BzSelect
                        modelValue={enabledFilterDraft}
                        placeholder="全部状态"
                        clearable
                        onValueChange={(value) => setEnabledFilterDraft(value ?? "")}
                      >
                        <BzOption value="true" label="启用" />
                        <BzOption value="false" label="停用" />
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <div className="admin-query-actions">
                    <BzButton className="admin-filter-secondary" nativeType="button" onClick={resetFilters}>
                      重置
                    </BzButton>
                    <BzButton className="admin-filter-primary" buttonType="primary" nativeType="button" onClick={applyFilters}>
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
                        <i className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`} aria-hidden="true" />
                      </button>
                    ) : null}
                  </div>
                </form>
              </div>
            </BzCard>
          ) : null}

          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div>
                  <div className="admin-table-title">资源管理</div>
                </div>
                <div className="admin-table-tools">
                  <BzButton onClick={expandAll}>全部展开</BzButton>
                  <BzButton onClick={collapseAll}>全部收起</BzButton>
                  {canCreate ? (
                    <BzButton className="admin-toolbar-primary" buttonType="primary" onClick={openCreateRoot}>
                      新增
                    </BzButton>
                  ) : null}
                  <AdminTableTools
                    queryPanelVisible={queryPanelVisible}
                    onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
                    onRefresh={() => reload()}
                  />
                </div>
              </div>
            }
          >
            <div className="admin-table-surface">

            <div className="resource-manage-table-wrap">
              <table className="resource-manage-table">
                <thead>
                  <tr>
                    <th style={{ width: "320px" }}>资源名称</th>
                    <th>类型</th>
                    <th>编码</th>
                    <th>路由路径</th>
                    <th>组件路径</th>
                    <th>排序</th>
                    <th>可见</th>
                    <th>启用</th>
                    <th>默认入口</th>
                    <th>内置</th>
                    <th style={{ width: "260px" }}>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {tableRows.length === 0 ? (
                    <tr>
                      <td colSpan={11}>
                        <div className="resource-manage-empty">暂无资源数据</div>
                      </td>
                    </tr>
                  ) : (
                    tableRows.map(({ row, level }) => {
                      const childrenAllowed = canHaveChildren(row.resourceType);
                      const actions: AdminActionItem[] = [];
                      if (canCreate && childrenAllowed) {
                        actions.push({ key: "create-child", label: "新增子项", handler: () => openCreateChild(row) });
                      }
                      if (canEdit) {
                        actions.push({ key: "edit", label: "编辑", handler: () => void openEdit(row, "edit") });
                      }
                      if (canDelete) {
                        actions.push({
                          key: "delete",
                          label: "删除",
                          tone: "delete",
                          disabled: row.systemBuiltin,
                          handler: () => void onDelete(row),
                        });
                      }
                      const hasChildren = row.children.length > 0;
                      const expanded = expandedIds.has(row.id);
                      return (
                        <tr key={row.id}>
                          <td>
                            <div className="resource-name-cell">
                              <span className="resource-indent" style={{ width: `${level * 24}px` }} />
                              <button
                                className={`resource-toggle${!hasChildren ? " is-placeholder" : ""}`}
                                type="button"
                                onClick={() => hasChildren && toggleExpand(row.id)}
                              >
                                {hasChildren ? (expanded || hasActiveFilter ? "▾" : "▸") : "▸"}
                              </button>
                              <span className="resource-name-main">{row.name}</span>
                            </div>
                          </td>
                          <td>
                            <span className={`resource-type-tag ${RESOURCE_TYPE_CLASS[row.resourceType]}`}>
                              {RESOURCE_TYPE_LABEL[row.resourceType]}
                            </span>
                          </td>
                          <td className="resource-mono">{row.code}</td>
                          <td className="resource-muted">{row.path || "-"}</td>
                          <td className="resource-muted">{row.component || "-"}</td>
                          <td>{row.sortNo}</td>
                          <td>{row.visible ? "是" : "否"}</td>
                          <td>
                            <span className={row.enabled ? "resource-status-on" : "resource-status-off"}>
                              {row.enabled ? "启用" : "停用"}
                            </span>
                          </td>
                          <td>{row.defaultEntry ? "是" : "否"}</td>
                          <td>{row.systemBuiltin ? "是" : "否"}</td>
                          <td>
                            <AdminActionBar actions={actions} />
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
            </div>
          </BzCard>
        </div>

        {drawerOpen ? (
          <AdminEntityDrawer
            open={drawerOpen}
            title={resolveDrawerTitle(drawerPurpose, form.resourceType)}
            width="min(860px, 100vw)"
            loading={drawerLoading}
            onClose={closeDrawer}
            footer={drawerFooter}
          >
            <div className="detail-grid resource-manage-drawer-grid">
              <section className="resource-manage-section detail-field detail-field--wide">
                <div className="resource-manage-section__title">基础信息</div>
                <div className="resource-manage-form-grid">
                  <div className="resource-manage-form-item">
                    <div className="resource-manage-form-label">父级资源</div>
                    <BzSelect modelValue={form.parentId} disabled={structureReadOnly} onValueChange={(value) => handleParentChange(value ?? "") }>
                      <BzOption value="" label="无父级，作为根资源" />
                      {parentOptions.map((option) => (
                        <BzOption key={option.id} value={option.id} label={option.label} />
                      ))}
                    </BzSelect>
                  </div>
                  <div className="resource-manage-form-item">
                    <div className="resource-manage-form-label">资源类型</div>
                    <BzSelect
                      modelValue={form.resourceType}
                      disabled={structureReadOnly}
                      onValueChange={(value) => handleTypeChange((value as ManageResourceType) || "DIRECTORY")}
                    >
                      {(["DIRECTORY", "MENU", "FUNCTION", "BUTTON"] as ManageResourceType[]).map((type) => (
                        <BzOption
                          key={type}
                          value={type}
                          label={RESOURCE_TYPE_LABEL[type]}
                          disabled={!allowedTypes.includes(type)}
                        />
                      ))}
                    </BzSelect>
                  </div>
                  <div className="resource-manage-form-item">
                    <div className="resource-manage-form-label">资源名称</div>
                    <BzInput modelValue={form.name} disabled={structureReadOnly} placeholder="例如：资源管理" onValueChange={(value) => updateForm("name", value)} />
                  </div>
                  <div className="resource-manage-form-item">
                    <div className="resource-manage-form-label">资源编码</div>
                    <BzInput modelValue={form.code} disabled={structureReadOnly} placeholder="例如：platform.resource" onValueChange={(value) => updateForm("code", value)} />
                  </div>
                  <div className="resource-manage-form-item">
                    <div className="resource-manage-form-label">图标</div>
                    <BzInput modelValue={form.icon} disabled={structureReadOnly || !(form.resourceType === "DIRECTORY" || form.resourceType === "MENU")} placeholder="例如：Setting" onValueChange={(value) => updateForm("icon", value)} />
                  </div>
                  <div className="resource-manage-form-item">
                    <div className="resource-manage-form-label">排序号</div>
                    <BzInput
                      modelValue={String(form.sortNo)}
                      disabled={structureReadOnly}
                      placeholder="例如：10"
                      onValueChange={(value) => updateForm("sortNo", Number(value || 0))}
                    />
                  </div>
                  <div className="resource-manage-form-item resource-manage-form-item--full">
                    <div className="resource-manage-form-label">备注</div>
                    <textarea
                      className="resource-manage-textarea"
                      value={form.remark}
                      disabled={structureReadOnly}
                      placeholder="资源说明"
                      rows={3}
                      onChange={(event) => updateForm("remark", event.target.value)}
                    />
                  </div>
                </div>
              </section>

              <section className="resource-manage-section detail-field detail-field--wide">
                <div className="resource-manage-section__title">路由信息</div>
                <div className="resource-manage-form-grid">
                  <div className="resource-manage-form-item">
                    <div className="resource-manage-form-label">路由路径</div>
                    <BzInput
                      modelValue={form.path}
                      disabled={structureReadOnly || !(form.resourceType === "MENU" || form.resourceType === "FUNCTION")}
                      placeholder="例如：/admin/resources"
                      onValueChange={(value) => updateForm("path", value)}
                    />
                  </div>
                  <div className="resource-manage-form-item">
                    <div className="resource-manage-form-label">组件路径</div>
                    <BzInput
                      modelValue={form.component}
                      disabled={structureReadOnly || !(form.resourceType === "MENU" || form.resourceType === "FUNCTION")}
                      placeholder="例如：pages/ResourcesAdminPage"
                      onValueChange={(value) => updateForm("component", value)}
                    />
                  </div>
                </div>
              </section>

              <section className="resource-manage-section detail-field detail-field--wide">
                <div className="resource-manage-section__title">状态配置</div>
                <div className="resource-manage-switch-row">
                  <label className="resource-manage-switch-item">
                    <span>可见</span>
                    <BzSwitch modelValue={form.visible} disabled={structureReadOnly} onValueChange={(value) => updateForm("visible", value)} />
                  </label>
                  <label className="resource-manage-switch-item">
                    <span>启用</span>
                    <BzSwitch modelValue={form.enabled} disabled={structureReadOnly} onValueChange={(value) => updateForm("enabled", value)} />
                  </label>
                  <label className="resource-manage-switch-item">
                    <span>默认入口</span>
                    <BzSwitch
                      modelValue={form.defaultEntry}
                      disabled={structureReadOnly || !(form.resourceType === "MENU" || form.resourceType === "FUNCTION")}
                      onValueChange={(value) => updateForm("defaultEntry", value)}
                    />
                  </label>
                  <label className="resource-manage-switch-item">
                    <span>系统内置</span>
                    <BzSwitch modelValue={form.systemBuiltin} disabled={structureReadOnly} onValueChange={(value) => updateForm("systemBuiltin", value)} />
                  </label>
                </div>
              </section>

              <section className="resource-manage-section detail-field detail-field--wide">
                <div className="resource-manage-section__title">权限码绑定</div>
                {showPermissionArea ? (
                  <>
                    <div className="resource-manage-permission-box">
                      {permissions.map((permission) => {
                        const checked = form.permissionIds.includes(permission.id);
                        return (
                          <label key={permission.id} className="resource-manage-permission-item">
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
                    <div className="resource-manage-tip">只有 BUTTON 类型资源允许绑定权限码。保存时会建立按钮资源与权限码的关联关系。</div>
                  </>
                ) : (
                  <div className="resource-manage-tip">只有 BUTTON 类型资源允许绑定权限码。</div>
                )}
              </section>

              {formError ? <div className="resource-manage-error">{formError}</div> : null}
            </div>
          </AdminEntityDrawer>
        ) : null}
      </div>
    </div>
  );
}

function flattenRows(rows: ResourceManageEntry[]): ResourceManageEntry[] {
  const result: ResourceManageEntry[] = [];
  rows.forEach((row) => {
    result.push(row);
    result.push(...flattenRows(row.children));
  });
  return result;
}

function flattenVisibleRows(rows: ResourceManageEntry[], expandedIds: Set<string>, forceExpand: boolean): ResourceTableRow[] {
  const result: ResourceTableRow[] = [];
  const walk = (items: ResourceManageEntry[], level: number) => {
    items.forEach((row) => {
      result.push({ row, level });
      if (row.children.length > 0 && (forceExpand || expandedIds.has(row.id))) {
        walk(row.children, level + 1);
      }
    });
  };
  walk(rows, 0);
  return result;
}

function filterTree(
  rows: ResourceManageEntry[],
  keyword: string,
  typeFilter: string,
  enabledFilter: string,
): ResourceManageEntry[] {
  return rows
    .map((row) => {
      const children = filterTree(row.children, keyword, typeFilter, enabledFilter);
      const selfMatched = matchRow(row, keyword, typeFilter, enabledFilter);
      if (!keyword && !typeFilter && !enabledFilter) {
        return { ...row, children };
      }
      if (selfMatched || children.length > 0) {
        return { ...row, children };
      }
      return null;
    })
    .filter((row): row is ResourceManageEntry => Boolean(row));
}

function matchRow(row: ResourceManageEntry, keyword: string, typeFilter: string, enabledFilter: string): boolean {
  if (typeFilter && row.resourceType !== typeFilter) return false;
  if (enabledFilter && String(row.enabled) !== enabledFilter) return false;
  if (!keyword) return true;
  return [row.name, row.code, row.path, row.component]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword));
}

function buildParentOptions(rows: ResourceManageEntry[], currentId?: string): Array<{ id: string; label: string }> {
  const result: Array<{ id: string; label: string }> = [];
  const current = currentId ? findById(rows, currentId) : null;
  const currentDescendants = current ? new Set(flattenRows(current.children).map((row) => row.id)) : new Set<string>();
  const walk = (items: ResourceManageEntry[], level: number) => {
    items.forEach((row) => {
      if (row.id !== currentId && !currentDescendants.has(row.id)) {
        result.push({
          id: row.id,
          label: `${"　".repeat(level)}${row.name}（${RESOURCE_TYPE_LABEL[row.resourceType]}）`,
        });
      }
      walk(row.children, level + 1);
    });
  };
  walk(rows, 0);
  return result;
}

function findById(rows: ResourceManageEntry[], id: string): ResourceManageEntry | null {
  for (const row of rows) {
    if (row.id === id) return row;
    const child = findById(row.children, id);
    if (child) return child;
  }
  return null;
}

function nextSortNo(rows: ResourceManageEntry[], parentId: string | null): number {
  const siblings = flattenRows(rows).filter((row) => (row.parentId ?? null) === parentId);
  if (siblings.length === 0) return 10;
  return Math.max(...siblings.map((row) => row.sortNo)) + 10;
}

function countDescendants(row: ResourceManageEntry): number {
  return flattenRows(row.children).length;
}

function toForm(entry: ResourceManageEntry): ResourceFormState {
  return {
    id: entry.id,
    parentId: entry.parentId ?? "",
    code: entry.code,
    name: entry.name,
    resourceType: entry.resourceType,
    path: entry.path ?? "",
    component: entry.component ?? "",
    icon: entry.icon ?? "",
    sortNo: entry.sortNo,
    visible: entry.visible,
    enabled: entry.enabled,
    defaultEntry: entry.defaultEntry,
    systemBuiltin: entry.systemBuiltin,
    remark: entry.remark ?? "",
    permissionIds: [...entry.permissionIds],
  };
}

function toSaveRequest(form: ResourceFormState): ResourceManageSaveRequest {
  return {
    parentId: form.parentId || null,
    code: form.code.trim(),
    name: form.name.trim(),
    resourceType: form.resourceType,
    path: form.resourceType === "MENU" || form.resourceType === "FUNCTION" ? blankToNull(form.path) : null,
    component:
      form.resourceType === "MENU" || form.resourceType === "FUNCTION" ? blankToNull(form.component) : null,
    icon: form.resourceType === "DIRECTORY" || form.resourceType === "MENU" ? blankToNull(form.icon) : null,
    sortNo: form.sortNo,
    visible: form.visible,
    enabled: form.enabled,
    defaultEntry:
      form.resourceType === "MENU" || form.resourceType === "FUNCTION" ? form.defaultEntry : false,
    systemBuiltin: form.systemBuiltin,
    remark: blankToNull(form.remark),
  };
}

function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function resolveDrawerTitle(purpose: DrawerPurpose, resourceType: ManageResourceType): string {
  if (purpose === "create") return "新增资源";
  return "编辑资源";
}
