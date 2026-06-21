"use client";

import { useCallback, useEffect, useMemo, useState } from "react";

import { createRole, deleteRole, getRoleGrantSelection, listRoleGrantResources, listRoles, updateRole, updateRoleGrantSelection } from "@admin/api/roles";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/registry/permissions.registry";
import type { RoleEntry, RoleGrantResourceEntry, RoleGrantSelection } from "@admin/types/role-admin";
import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOption } from "../bz/BzOption";
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

  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<"create" | "edit">("create");
  const [dialogModel, setDialogModel] = useState<RoleEntry | null>(null);

  const [grantOpen, setGrantOpen] = useState(false);
  const [grantTarget, setGrantTarget] = useState<RoleEntry | null>(null);
  const [grantSelection, setGrantSelection] = useState<RoleGrantSelection>({ menuIds: [], functionIds: [] });
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
  const isFirstPage = pageNo <= 1;
  const isLastPage = pageNo >= totalPages;

  const pageTokens = useMemo(() => {
    const total = totalPages;
    const current = Math.min(Math.max(pageNo, 1), total);
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", total];
    if (current >= total - 3) return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
    return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
  }, [totalPages, pageNo]);

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

  function goToPage(nextPage: number) {
    const target = Math.min(Math.max(nextPage, 1), totalPages);
    if (target === pageNo) return;
    setPageNo(target);
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
      await updateRole(dialogModel.id, { code: payload.code, name: payload.name, enabled: payload.enabled });
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
            <BzCard className="admin-panel admin-filter-card" shadow="never">
              <BzForm className="admin-filter-form" onSubmit={(e) => { e.preventDefault(); applyFilters(); }}>
                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">关键字</div>
                    <div className="admin-filter-control">
                      <BzInput modelValue={keywordDraft} placeholder="按编码、名称搜索" clearable
                        onValueChange={setKeywordDraft}
                        onKeyUp={(event) => { if (event.key === "Enter") applyFilters(); }}
                      />
                    </div>
                  </div>
                </BzFormItem>

                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">状态</div>
                    <div className="admin-filter-control">
                      <BzSelect modelValue={enabledDraft} placeholder="全部状态" clearable onValueChange={(v) => setEnabledDraft((v ?? "") as "" | "true" | "false")}>
                        <BzOption label="启用" value="true" />
                        <BzOption label="停用" value="false" />
                      </BzSelect>
                    </div>
                  </div>
                </BzFormItem>

                <div className="admin-filter-actions">
                  <BzButton className="admin-filter-secondary" onClick={resetFilters}>重置</BzButton>
                  <BzButton className="admin-filter-primary" buttonType="primary" nativeType="submit">搜索</BzButton>
                  <div className="admin-filter-toggle-placeholder" aria-hidden="true" />
                </div>
              </BzForm>
            </BzCard>
          ) : null}

          <BzCard className="admin-panel admin-table-card" shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">角色管理</div>
                <div className="admin-table-tools">
                  {canCreate ? <BzButton className="admin-toolbar-primary" buttonType="primary" onClick={openCreate}>新增</BzButton> : null}
                  <button className={`admin-vben-circle-button${queryPanelVisible ? " is-active" : ""}`} type="button" title={queryPanelVisible ? "关闭搜索框" : "打开搜索框"} onClick={() => setQueryPanelVisible((v) => !v)}>
                    <i className="admin-vben-circle-button__icon admin-vben-circle-button__icon--search" aria-hidden="true" />
                  </button>
                  <button className="admin-vben-circle-button" type="button" title="刷新列表" onClick={reload}>
                    <i className="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh" aria-hidden="true" />
                  </button>
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
                  <label className="dict-page-size">
                    <select className="dict-page-size__select" value={pageSize} onChange={(event) => { setPageSize(Number(event.target.value)); setPageNo(1); }}>
                      {pageSizeOptions.map((size) => (
                        <option key={size} value={size}>{size}条/页</option>
                      ))}
                    </select>
                  </label>
                  <div className="dict-page-list">
                    <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isFirstPage} onClick={() => goToPage(1)}><span aria-hidden="true">|&lt;</span></button>
                    <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isFirstPage} onClick={() => goToPage(pageNo - 1)}><span aria-hidden="true">&lt;</span></button>
                    {pageTokens.map((token, index) =>
                      typeof token === "number" ? (
                        <button key={index} className={`dict-page-btn${token === pageNo ? " is-active" : ""}`} type="button" onClick={() => goToPage(token)}>{token}</button>
                      ) : (
                        <span key={index} className="dict-page-ellipsis">...</span>
                      )
                    )}
                    <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isLastPage} onClick={() => goToPage(pageNo + 1)}><span aria-hidden="true">&gt;</span></button>
                    <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isLastPage} onClick={() => goToPage(totalPages)}><span aria-hidden="true">&gt;|</span></button>
                  </div>
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
