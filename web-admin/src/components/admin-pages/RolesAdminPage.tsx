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
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/permissions-registry";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import type {
  RoleEntry,
  RoleGrantResourceEntry,
  RoleGrantSelection,
} from "@admin/types/role-admin";
import { useCallback, useEffect, useMemo, useState } from "react";

import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOption } from "../bz/BzOption";
import { BzPagination } from "../bz/BzPagination";
import { BzSelect } from "../bz/BzSelect";
import { RoleFormDialog } from "../roles-admin/RoleFormDialog";
import { RolePermissionDialog } from "../roles-admin/RolePermissionDialog";
import { RoleTable } from "../roles-admin/RoleTable";

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

  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<"create" | "edit">("create");
  const [dialogModel, setDialogModel] = useState<RoleEntry | null>(null);

  const [grantOpen, setGrantOpen] = useState(false);
  const [grantTarget, setGrantTarget] = useState<RoleEntry | null>(null);
  const [grantSelection, setGrantSelection] = useState<RoleGrantSelection>({
    menuIds: [],
    functionIds: [],
  });
  const [grantLoading, setGrantLoading] = useState(false);
  const [grantResourcesLoading, setGrantResourcesLoading] = useState(false);
  const [grantResourceRows, setGrantResourceRows] = useState<RoleGrantResourceEntry[]>([]);

  const canCreate = hasResourceCodeAccess("role-manage-create");
  const canEdit = hasResourceCodeAccess("role-manage-edit");
  const canDelete = hasResourceCodeAccess("role-manage-delete");
  const canGrantView = hasResourceCodeAccess("role-manage-permission-view");
  const canGrantEdit = hasResourceCodeAccess("role-manage-permission-edit");
  const canGrant = canGrantView || canGrantEdit;

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

  function openCreate() {
    if (!canCreate) return;
    setDialogMode("create");
    setDialogModel({ id: "", code: "", name: "", enabled: true });
    setDialogOpen(true);
  }

  function openEdit(role: RoleEntry) {
    if (!canEdit) return;
    setDialogMode("edit");
    setDialogModel({ ...role });
    setDialogOpen(true);
  }

  async function onSubmit(payload: RoleEntry) {
    if (dialogMode === "create") {
      await createRole({ code: payload.code, name: payload.name, enabled: payload.enabled });
    } else if (dialogModel) {
      await updateRole(dialogModel.id, {
        code: payload.code,
        name: payload.name,
        enabled: payload.enabled,
      });
    }
    setDialogOpen(false);
    await reload();
  }

  async function onRemove(role: RoleEntry) {
    if (!canDelete) return;
    const confirmed = await bzConfirm({
      title: "删除角色",
      content: `确认删除：${role.name} (${role.code})？`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteRole(role.id);
    message.success("删除成功");
    await reload();
  }

  async function openGrants(role: RoleEntry) {
    if (!canGrant) return;
    setGrantTarget(role);
    setGrantOpen(true);
    setGrantLoading(true);
    try {
      if (grantResourceRows.length === 0) await reloadGrantResources();
      setGrantSelection(await getRoleGrantSelection(role.id));
    } finally {
      setGrantLoading(false);
    }
  }

  async function onGrantSubmit(selection: RoleGrantSelection) {
    if (!grantTarget || !canGrantEdit) return;
    await updateRoleGrantSelection(grantTarget.id, selection);
    setGrantSelection({ menuIds: [...selection.menuIds], functionIds: [...selection.functionIds] });
    message.success("角色授权已保存，受影响用户需要重新登录");
    setGrantOpen(false);
  }

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          {queryPanelVisible ? (
            <BzCard
              className="admin-panel admin-filter-card"
              shadow="never"
            >
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
                  onSubmit={(e) => {
                    e.preventDefault();
                    applyFilters();
                  }}
                >
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">关键字</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={keywordDraft}
                        placeholder="按编码、名称搜索"
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
                    <BzButton className="admin-filter-secondary" nativeType="button" onClick={resetFilters}>
                      重置
                    </BzButton>
                    <BzButton className="admin-filter-primary" buttonType="primary" nativeType="button" onClick={applyFilters}>
                      搜索
                    </BzButton>
                    {!querySingleRow ? (
                      <button className="admin-filter-toggle" type="button" aria-expanded={queryExpanded} onClick={() => setQueryExpanded((v) => !v)}>
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
                <div className="admin-table-title">角色管理</div>
                <div className="admin-table-tools">
                  {canCreate ? (
                    <BzButton
                      className="admin-toolbar-primary"
                      buttonType="primary"
                      onClick={openCreate}
                    >
                      新增
                    </BzButton>
                  ) : null}
                  <AdminTableTools
                    queryPanelVisible={queryPanelVisible}
                    onToggleQueryPanel={() => setQueryPanelVisible((v) => !v)}
                    onRefresh={reload}
                  />
                </div>
              </div>
            }
          >
            <div className="admin-table-surface">
              <RoleTable
                rows={pagedRows}
                loading={loading}
                canEdit={canEdit}
                canDelete={canDelete}
                canPermissions={canGrant}
                onEdit={openEdit}
                onRemove={onRemove}
                onPermissions={openGrants}
              />
            </div>

            {filteredRows.length > 0 ? (
              <div className="dict-pagination-bar">
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
            ) : null}
          </BzCard>

          {dialogOpen ? (
            <RoleFormDialog
              mode={dialogMode}
              model={dialogModel}
              onClose={() => setDialogOpen(false)}
              onSubmit={onSubmit}
            />
          ) : null}

          {grantOpen ? (
            <RolePermissionDialog
              roleName={grantTarget?.name || ""}
              resources={grantResourceRows}
              selection={grantSelection}
              loading={grantLoading || grantResourcesLoading}
              canSave={canGrantEdit}
              onClose={() => setGrantOpen(false)}
              onSubmit={onGrantSubmit}
            />
          ) : null}
        </div>
      </div>
    </div>
  );
}
