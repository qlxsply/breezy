"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { batchListDictOptions } from "@admin/api/dicts";
import { listRoles } from "@admin/api/roles";
import { createUser, deleteUser, getUserRoles, pageUsers, resetUserPassword, updateUser, updateUserRoles } from "@admin/api/users";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/registry/permissions.registry";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import type { RoleEntry } from "@admin/types/role-admin";
import type { UserEntry, UserStatus } from "@admin/types/user-admin";
import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOption } from "../bz/BzOption";
import { BzSelect } from "../bz/BzSelect";
import { PasswordResetDialog } from "../users-admin/PasswordResetDialog";
import { UserFormDialog } from "../users-admin/UserFormDialog";
import { UserRoleDialog } from "../users-admin/UserRoleDialog";
import { UserTable } from "../users-admin/UserTable";

function toDictMetaMap(items?: DictItem[]): Record<string, { label: string; tagType?: string | null }> {
  const map: Record<string, { label: string; tagType?: string | null }> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = { label: item.itemLabel || item.itemValue, tagType: item.tagType || undefined };
  }
  return map;
}

export function UsersAdminPage() {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<UserEntry[]>([]);
  const [page, setPage] = useState<PageResult<UserEntry>>({
    pageNo: 1, pageSize: 10, numberOfElements: 0, totalPages: 0, totalElements: 0, elements: [],
  });

  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [statusDraft, setStatusDraft] = useState<"" | UserStatus>("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedStatus, setAppliedStatus] = useState<"" | UserStatus>("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const pageSizeOptions = [10, 20, 30, 50, 100];

  const [userTypeMetaMap, setUserTypeMetaMap] = useState<Record<string, { label: string; tagType?: string | null }>>({});

  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<"create" | "edit">("create");
  const [dialogModel, setDialogModel] = useState<UserEntry | null>(null);

  const [resetOpen, setResetOpen] = useState(false);
  const [resetTarget, setResetTarget] = useState<UserEntry | null>(null);

  const [roleOpen, setRoleOpen] = useState(false);
  const [roleTarget, setRoleTarget] = useState<UserEntry | null>(null);
  const [roleList, setRoleList] = useState<RoleEntry[]>([]);
  const [roleSelected, setRoleSelected] = useState<string[]>([]);
  const [roleLoading, setRoleLoading] = useState(false);

  const canCreate = hasResourceCodeAccess("user-manage-create");
  const canEdit = hasResourceCodeAccess("user-manage-edit");
  const canToggle = hasResourceCodeAccess("user-manage-edit");
  const canReset = hasResourceCodeAccess("user-manage-reset-password");
  const canRoleEdit = hasResourceCodeAccess("user-manage-role-edit");
  const canRoles = canRoleEdit || hasResourceCodeAccess("user-manage-role-view");
  const canDelete = hasResourceCodeAccess("user-manage-delete");

  const pageNoRef = useRef(pageNo);
  const pageSizeRef = useRef(pageSize);
  const appliedKeywordRef = useRef(appliedKeyword);
  const appliedStatusRef = useRef(appliedStatus);
  pageNoRef.current = pageNo;
  pageSizeRef.current = pageSize;
  appliedKeywordRef.current = appliedKeyword;
  appliedStatusRef.current = appliedStatus;

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      const pn = pageNoRef.current;
      const ps = pageSizeRef.current;
      let result = await pageUsers({ usernameLike: appliedKeywordRef.current, status: appliedStatusRef.current, page: { pageNo: pn, pageSize: ps } });
      if (result.totalElements > 0 && pn > Math.max(1, result.totalPages)) {
        setPageNo(Math.max(1, result.totalPages));
        result = await pageUsers({ usernameLike: appliedKeywordRef.current, status: appliedStatusRef.current, page: { pageNo: Math.max(1, result.totalPages), pageSize: ps } });
      }
      setPage(result);
      setPageNo(result.pageNo || 1);
      setPageSize(result.pageSize || ps);
      setRows(result.elements);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadDictionaries = useCallback(async () => {
    try {
      const result = await batchListDictOptions(["USER_TYPE"]);
      setUserTypeMetaMap(toDictMetaMap(result.USER_TYPE));
    } catch {
      setUserTypeMetaMap({ SYSTEM: { label: "系统账号", tagType: "warning" }, INTERNAL: { label: "账号", tagType: "info" }, EXTERNAL: { label: "用户", tagType: "info" }, GUEST: { label: "游客", tagType: "danger" } });
    }
  }, []);

  useEffect(() => { loadDictionaries(); }, [loadDictionaries]);

  // reload whenever filter/page state changes
  useEffect(() => {
    reload();
  }, [pageNo, pageSize, appliedKeyword, appliedStatus, reload]);

  const totalPages = Math.max(1, page.totalPages || 1);
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

  async function applyFilters() {
    setAppliedKeyword(keywordDraft.trim());
    setAppliedStatus(statusDraft);
    setPageNo(1);
  }

  async function resetFilters() {
    setKeywordDraft("");
    setStatusDraft("");
    setPageNo(1);
    setAppliedKeyword("");
    setAppliedStatus("");
  }

  function goToPage(target: number) {
    const next = Math.min(Math.max(target, 1), totalPages);
    if (next === pageNo) return;
    setPageNo(next);
  }

  function openCreate() {
    if (!canCreate) return;
    setDialogMode("create");
    setDialogModel({ id: "", username: "", nickname: "", userType: "INTERNAL", status: "ENABLED" });
    setDialogOpen(true);
  }

  function openEdit(user: UserEntry) {
    if (!canEdit) return;
    setDialogMode("edit");
    setDialogModel({ ...user });
    setDialogOpen(true);
  }

  async function onSubmit(payload: { username: string; nickname: string; password?: string; status: UserStatus }) {
    if (dialogMode === "create") {
      if (!payload.password) return;
      await createUser({ username: payload.username, nickname: payload.nickname, password: payload.password });
    } else if (dialogModel) {
      await updateUser(dialogModel.id, { nickname: payload.nickname, status: payload.status });
    }
    setDialogOpen(false);
  }

  async function onToggle(user: UserEntry) {
    if (!canToggle) return;
    const nextStatus: UserStatus = user.status === "ENABLED" ? "DISABLED" : "ENABLED";
    await updateUser(user.id, { nickname: user.nickname, status: nextStatus });
  }

  function openReset(user: UserEntry) {
    if (!canReset) return;
    setResetTarget(user);
    setResetOpen(true);
  }

  async function onResetSubmit() {
    if (!resetTarget) return;
    await resetUserPassword(resetTarget.id);
    setResetOpen(false);
  }

  async function openRoles(user: UserEntry) {
    if (!canRoles) return;
    if (user.userType === "EXTERNAL") return;
    setRoleTarget(user);
    setRoleOpen(true);
    setRoleLoading(true);
    try {
      if (roleList.length === 0) setRoleList(await listRoles());
      setRoleSelected(await getUserRoles(user.id));
    } finally {
      setRoleLoading(false);
    }
  }

  async function onRoleSubmit(roleIds: string[]) {
    if (!roleTarget || !canRoleEdit) return;
    await updateUserRoles(roleTarget.id, roleIds);
    setRoleOpen(false);
  }

  async function onRemove(user: UserEntry) {
    if (!canDelete) return;
    const confirmed = await bzConfirm({ title: "删除用户", content: `确认删除：${user.username} ?`, confirmText: "删除", cancelText: "取消" });
    if (!confirmed) return;
    await deleteUser(user.id);
    message.success("删除成功");
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
                    <div className="admin-filter-label">账号</div>
                    <div className="admin-filter-control">
                      <BzInput modelValue={keywordDraft} placeholder="按账号或昵称搜索" clearable onValueChange={setKeywordDraft} onKeyUp={(e) => { if (e.key === "Enter") applyFilters(); }} />
                    </div>
                  </div>
                </BzFormItem>
                <BzFormItem className="admin-filter-item">
                  <div className="admin-filter-field">
                    <div className="admin-filter-label">状态</div>
                    <div className="admin-filter-control">
                      <BzSelect modelValue={statusDraft} placeholder="全部状态" clearable onValueChange={(v) => setStatusDraft((v ?? "") as "" | UserStatus)}>
                        <BzOption label="启用" value="ENABLED" />
                        <BzOption label="停用" value="DISABLED" />
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
            header={<div className="admin-table-header">
              <div className="admin-table-title">账号</div>
              <div className="admin-table-tools">
                {canCreate ? <BzButton className="admin-toolbar-primary" buttonType="primary" onClick={openCreate}>新增</BzButton> : null}
                <button className={`admin-vben-circle-button${queryPanelVisible ? " is-active" : ""}`} type="button" title={queryPanelVisible ? "关闭搜索框" : "打开搜索框"} onClick={() => setQueryPanelVisible((v) => !v)}>
                  <i className="admin-vben-circle-button__icon admin-vben-circle-button__icon--search" aria-hidden="true" />
                </button>
                <button className="admin-vben-circle-button" type="button" title="刷新列表" onClick={() => reload()}>
                  <i className="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh" aria-hidden="true" />
                </button>
              </div>
            </div>}
          >
            <div className="admin-table-surface">
              <UserTable rows={rows} loading={loading} canEdit={canEdit} canToggle={canToggle} canReset={canReset} canRoles={canRoles} canDelete={canDelete} userTypeMetaMap={userTypeMetaMap}
                onEdit={openEdit} onToggle={onToggle} onReset={openReset} onRoles={openRoles} onRemove={onRemove} />
            </div>

            {page.totalElements > 0 ? (
              <div className="dict-pagination-bar">
                <div className="dict-pagination-summary">共 {page.totalElements} 条记录</div>
                <div className="dict-pagination-right">
                  <label className="dict-page-size">
                    <select className="dict-page-size__select" value={pageSize} onChange={(e) => { setPageSize(Number(e.target.value)); setPageNo(1); }}>
                      {pageSizeOptions.map((s) => <option key={s} value={s}>{s}条/页</option>)}
                    </select>
                  </label>
                  <div className="dict-page-list">
                    <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isFirstPage} onClick={() => goToPage(1)}><span aria-hidden="true">|&lt;</span></button>
                    <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isFirstPage} onClick={() => goToPage(pageNo - 1)}><span aria-hidden="true">&lt;</span></button>
                    {pageTokens.map((token, i) => typeof token === "number" ? (
                      <button key={i} className={`dict-page-btn${token === pageNo ? " is-active" : ""}`} type="button" onClick={() => goToPage(token)}>{token}</button>
                    ) : <span key={i} className="dict-page-ellipsis">...</span>)}
                    <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isLastPage} onClick={() => goToPage(pageNo + 1)}><span aria-hidden="true">&gt;</span></button>
                    <button className="dict-page-btn dict-page-btn--icon" type="button" disabled={isLastPage} onClick={() => goToPage(totalPages)}><span aria-hidden="true">&gt;|</span></button>
                  </div>
                </div>
              </div>
            ) : null}
          </BzCard>

          {dialogOpen ? <UserFormDialog mode={dialogMode} model={dialogModel} onClose={() => setDialogOpen(false)} onSubmit={onSubmit} /> : null}
          {resetOpen ? <PasswordResetDialog onClose={() => setResetOpen(false)} onSubmit={onResetSubmit} /> : null}
          {roleOpen ? <UserRoleDialog userName={roleTarget?.username || ""} roles={roleList} selectedIds={roleSelected} loading={roleLoading} canSave={canRoleEdit} onClose={() => setRoleOpen(false)} onSubmit={onRoleSubmit} /> : null}
        </div>
      </div>
    </div>
  );
}
