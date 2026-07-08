"use client";

import {
  createRole,
  deleteRole,
  getRoleGrantSelection,
  listRoleGrantResources,
  listRoles,
  updateRole,
  updateRoleGrantSelection,
} from "@admin/api/roles";
import { AdminListPageTemplate } from "@admin/components/admin/AdminListPageTemplate";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type {
  RoleEntry,
  RoleGrantResourceEntry,
  RoleGrantSelection,
} from "@admin/types/role-admin";
import { useCallback, useEffect, useMemo, useState } from "react";

import { BzButton } from "../bz/BzButton";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOption } from "../bz/BzOption";
import { BzPagination } from "../bz/BzPagination";
import { BzSelect } from "../bz/BzSelect";
import { RolePermissionDialog } from "../roles-admin/RolePermissionDialog";
import { RoleTable } from "../roles-admin/RoleTable";

const defaultRoleModel: RoleEntry = { id: "", code: "", name: "", enabled: true };

export function RolesAdminPage() {
  const [rows, setRows] = useState<RoleEntry[]>([]);
  const [loading, setLoading] = useState(false);

  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [enabledDraft, setEnabledDraft] = useState<"" | "true" | "false">("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedEnabled, setAppliedEnabled] = useState<"" | "true" | "false">("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const pageSizeOptions = [10, 20, 30, 50, 100];
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const [manageOpen, setManageOpen] = useState(false);
  const [manageMode, setManageMode] = useState<"create" | "detail" | "edit">("detail");
  const [manageTarget, setManageTarget] = useState<RoleEntry | null>(null);
  const [manageSelection, setManageSelection] = useState<RoleGrantSelection>({
    resourceIds: [],
  });
  const [manageLoading, setManageLoading] = useState(false);
  const [grantResourcesLoading, setGrantResourcesLoading] = useState(false);
  const [grantResourceRows, setGrantResourceRows] = useState<RoleGrantResourceEntry[]>([]);

  const canCreate = hasResourceCodeAccess("role-manage-create");
  const canEdit = hasResourceCodeAccess("role-manage-edit");
  const canDelete = hasResourceCodeAccess("role-manage-delete");
  const canGrantView = hasResourceCodeAccess("role-manage-permission-view");
  const canGrantEdit = hasResourceCodeAccess("role-manage-permission-edit");
  const canGrant = canGrantView || canGrantEdit;
  const canManage = canEdit || canGrantEdit;

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await listRoles());
    } finally {
      setLoading(false);
    }
  }, []);

  const reloadGrantResources = useCallback(async () => {
    setGrantResourcesLoading(true);
    try {
      setGrantResourceRows(await listRoleGrantResources());
    } catch {
      setGrantResourceRows([]);
    } finally {
      setGrantResourcesLoading(false);
    }
  }, []);

  useEffect(() => {
    Promise.all([reloadGrantResources(), reload()]);
  }, [reload, reloadGrantResources]);

  const filteredRows = useMemo(() => {
    const kw = appliedKeyword.trim().toLowerCase();
    const enabledValue = appliedEnabled === "" ? "" : appliedEnabled === "true";
    return rows.filter((role) => {
      if (enabledValue !== "" && role.enabled !== enabledValue) return false;
      if (!kw) return true;
      return role.code.toLowerCase().includes(kw) || role.name.toLowerCase().includes(kw);
    });
  }, [rows, appliedKeyword, appliedEnabled]);

  const totalPages = Math.max(1, Math.ceil(filteredRows.length / pageSize));

  useEffect(() => {
    if (pageNo > totalPages) {
      setPageNo(totalPages);
    }
  }, [pageNo, totalPages]);

  const pagedRows = useMemo(() => {
    const start = (pageNo - 1) * pageSize;
    return filteredRows.slice(start, start + pageSize);
  }, [filteredRows, pageNo, pageSize]);

  function applyFilters() {
    setAppliedKeyword(keywordDraft);
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

  async function openCreate() {
    if (!canCreate) return;
    setManageMode("create");
    setManageTarget({ ...defaultRoleModel });
    setManageSelection({ resourceIds: [] });
    setManageOpen(true);
    if (canGrant && grantResourceRows.length === 0) {
      setManageLoading(true);
      try {
        await reloadGrantResources();
      } finally {
        setManageLoading(false);
      }
    }
  }

  function openEdit(role: RoleEntry) {
    if (!canManage) return;
    void openManage(role, "edit");
  }

  function openDetail(role: RoleEntry) {
    void openManage(role, "detail");
  }

  async function onRemove(role: RoleEntry) {
    if (!canDelete) return;
    const confirmed = await bzConfirm({
      title: "删除角色",
      content: `确认删除该角色吗？删除后不可恢复。角色：${role.name}（${role.code}）`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteRole(role.id);
    message.success("删除成功");
    await reload();
  }

  async function openManage(role: RoleEntry, mode: "detail" | "edit") {
    setManageTarget(role);
    setManageMode(mode);
    setManageOpen(true);

    if (!canGrant) {
      setManageSelection({ resourceIds: [] });
      setManageLoading(false);
      return;
    }

    setManageLoading(true);
    try {
      if (grantResourceRows.length === 0) await reloadGrantResources();
      setManageSelection(await getRoleGrantSelection(role.id));
    } finally {
      setManageLoading(false);
    }
  }

  async function onManageSubmit(payload: {
    role: RoleEntry;
    selection: RoleGrantSelection;
    permissionChanged: boolean;
  }) {
    if (!manageTarget) return;

    const nextRole = {
      code: payload.role.code.trim(),
      name: payload.role.name.trim(),
      enabled: payload.role.enabled,
    };

    if (manageMode === "create") {
      const createdRole = await createRole(nextRole);
      if (canGrantEdit && payload.permissionChanged) {
        await updateRoleGrantSelection(createdRole.id, payload.selection);
      }
      setManageSelection({ resourceIds: [...payload.selection.resourceIds] });
      setManageOpen(false);
      message.success("新增成功");
      await reload();
      return;
    }

    const roleChanged =
      canEdit &&
      (manageTarget.code !== nextRole.code ||
        manageTarget.name !== nextRole.name ||
        manageTarget.enabled !== nextRole.enabled);

    if (!roleChanged && !payload.permissionChanged) {
      setManageOpen(false);
      return;
    }

    if (roleChanged) {
      await updateRole(manageTarget.id, nextRole);
    }

    if (canGrantEdit && payload.permissionChanged) {
      await updateRoleGrantSelection(manageTarget.id, payload.selection);
    }

    setManageSelection({ resourceIds: [...payload.selection.resourceIds] });
    setManageOpen(false);

    if (roleChanged && payload.permissionChanged) {
      message.success("保存成功，角色权限变更将在受影响用户重新登录后完全生效");
    } else if (payload.permissionChanged) {
      message.success("角色权限已保存，受影响用户需要重新登录");
    } else {
      message.success("保存成功");
    }

    await reload();
  }

  return (
    <AdminListPageTemplate
      className="role-admin-page role-admin-card"
      regionClassName="role-admin-region"
      queryPanelClassName="role-admin-query-panel"
      toolbarRowClassName="role-admin-toolbar-row"
      businessActionsClassName="role-admin-business-actions"
      queryToolsClassName="role-admin-query-tools"
      tableAreaClassName="role-admin-table-area"
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
            onSubmit={(e) => {
              e.preventDefault();
              applyFilters();
            }}
          >
            <BzFormItem className="admin-query-field">
              <div className="admin-query-field__label">角色</div>
              <div className="admin-query-field__control">
                <BzInput
                  modelValue={keywordDraft}
                  placeholder="搜索角色编码/名称"
                  clearable
                  onValueChange={setKeywordDraft}
                  onKeyUp={(event) => {
                    if (event.key === "Enter") applyFilters();
                  }}
                />
              </div>
            </BzFormItem>
            <BzFormItem className="admin-query-field">
              <div className="admin-query-field__label">状态</div>
              <div className="admin-query-field__control">
                <BzSelect
                  modelValue={enabledDraft}
                  placeholder="全部状态"
                  clearable
                  onValueChange={(v) => setEnabledDraft((v ?? "") as "" | "true" | "false")}
                >
                  <BzOption label="启用" value="true" />
                  <BzOption label="停用" value="false" />
                </BzSelect>
              </div>
            </BzFormItem>
            <div className="admin-query-actions">
              <BzButton className="admin-filter-secondary" nativeType="button" onClick={resetFilters}>重置</BzButton>
              <BzButton className="admin-filter-primary" buttonType="primary" nativeType="button" onClick={applyFilters}>搜索</BzButton>
              {!querySingleRow ? (
                <button className="admin-filter-toggle" type="button" aria-expanded={queryExpanded} onClick={() => setQueryExpanded((v) => !v)}>
                  <span>{queryExpanded ? "收起" : "展开"}</span>
                  <i className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`} aria-hidden="true" />
                </button>
              ) : null}
            </div>
          </form>
        </div>
      }
      businessActions={canCreate ? <BzButton className="admin-toolbar-primary" buttonType="primary" onClick={openCreate}>新增</BzButton> : null}
      queryTools={<AdminTableTools queryPanelVisible={queryPanelVisible} onToggleQueryPanel={() => setQueryPanelVisible((v) => !v)} onRefresh={reload} />}
      table={<RoleTable rows={pagedRows} loading={loading} canEdit={canManage} canDelete={canDelete} onDetail={openDetail} onEdit={openEdit} onRemove={onRemove} />}
      footer={
        filteredRows.length > 0 ? (
          <div className="dict-pagination-bar role-admin-table-footer">
            <div className="dict-pagination-summary">共 {filteredRows.length} 条记录</div>
            <div className="dict-pagination-right">
              <BzPagination
                total={filteredRows.length}
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
      overlays={manageOpen ? <RolePermissionDialog mode={manageMode} role={manageTarget} resources={grantResourceRows} selection={manageSelection} loading={manageLoading || grantResourcesLoading} canEditBasic={canEdit} canViewPermissions={canGrant} canEditPermissions={canGrantEdit} onClose={() => setManageOpen(false)} onSubmit={onManageSubmit} /> : null}
    />
  );
}
