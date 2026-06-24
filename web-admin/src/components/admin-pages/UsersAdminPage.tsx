"use client";

import { batchListDictOptions } from "@admin/api/dicts";
import { listRoles } from "@admin/api/roles";
import {
  createUser,
  deleteUser,
  getUserRoles,
  pageUsers,
  resetUserPassword,
  updateUser,
  updateUserRoles,
} from "@admin/api/users";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/permissions-registry";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import type { RoleEntry } from "@admin/types/role-admin";
import type { UserEntry, UserStatus } from "@admin/types/user-admin";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOption } from "../bz/BzOption";
import { BzPagination } from "../bz/BzPagination";
import { BzSelect } from "../bz/BzSelect";
import { PasswordResetDialog } from "../users-admin/PasswordResetDialog";
import { UserFormDialog } from "../users-admin/UserFormDialog";
import { UserRoleDialog } from "../users-admin/UserRoleDialog";
import { UserTable } from "../users-admin/UserTable";

function toDictMetaMap(
  items?: DictItem[],
): Record<string, { label: string; tagType?: string | null }> {
  const map: Record<string, { label: string; tagType?: string | null }> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = {
      label: item.itemLabel || item.itemValue,
      tagType: item.tagType || undefined,
    };
  }
  return map;
}

export function UsersAdminPage() {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<UserEntry[]>([]);
  const [page, setPage] = useState<PageResult<UserEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });

  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [statusDraft, setStatusDraft] = useState<"" | UserStatus>("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedStatus, setAppliedStatus] = useState<"" | UserStatus>("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const pageSizeOptions = [10, 20, 30, 50, 100];

  const [userTypeMetaMap, setUserTypeMetaMap] = useState<
    Record<string, { label: string; tagType?: string | null }>
  >({});

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
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

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
      let result = await pageUsers({
        usernameLike: appliedKeywordRef.current,
        status: appliedStatusRef.current,
        page: { pageNo: pn, pageSize: ps },
      });
      if (result.totalElements > 0 && pn > Math.max(1, result.totalPages)) {
        setPageNo(Math.max(1, result.totalPages));
        result = await pageUsers({
          usernameLike: appliedKeywordRef.current,
          status: appliedStatusRef.current,
          page: { pageNo: Math.max(1, result.totalPages), pageSize: ps },
        });
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
      setUserTypeMetaMap({
        SYSTEM: { label: "系统账号", tagType: "warning" },
        INTERNAL: { label: "账号", tagType: "info" },
        EXTERNAL: { label: "用户", tagType: "info" },
        GUEST: { label: "游客", tagType: "danger" },
      });
    }
  }, []);

  useEffect(() => {
    loadDictionaries();
  }, [loadDictionaries]);

  // reload whenever filter/page state changes
  useEffect(() => {
    reload();
  }, [pageNo, pageSize, appliedKeyword, appliedStatus, reload]);

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

  async function onSubmit(payload: {
    username: string;
    nickname: string;
    password?: string;
    status: UserStatus;
  }) {
    if (dialogMode === "create") {
      if (!payload.password) return;
      await createUser({
        username: payload.username,
        nickname: payload.nickname,
        password: payload.password,
      });
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
    const confirmed = await bzConfirm({
      title: "删除用户",
      content: `确认删除：${user.username} ?`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteUser(user.id);
    message.success("删除成功");
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
                    <div className="admin-query-field__label">账号</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={keywordDraft}
                        placeholder="按账号或昵称搜索"
                        clearable
                        onValueChange={setKeywordDraft}
                        onKeyUp={(e) => {
                          if (e.key === "Enter") applyFilters();
                        }}
                      />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">状态</div>
                    <div className="admin-query-field__control">
                      <BzSelect
                        modelValue={statusDraft}
                        placeholder="全部状态"
                        clearable
                        onValueChange={(v) => setStatusDraft((v ?? "") as "" | UserStatus)}
                      >
                        <BzOption label="启用" value="ENABLED" />
                        <BzOption label="停用" value="DISABLED" />
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
                        onClick={() => setQueryExpanded((v) => !v)}
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
                <div className="admin-table-title">账号</div>
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
                  <button
                    className={`admin-vben-circle-button${queryPanelVisible ? " is-active" : ""}`}
                    type="button"
                    title={queryPanelVisible ? "关闭搜索框" : "打开搜索框"}
                    onClick={() => setQueryPanelVisible((v) => !v)}
                  >
                    <i
                      className="admin-vben-circle-button__icon admin-vben-circle-button__icon--search"
                      aria-hidden="true"
                    />
                  </button>
                  <button
                    className="admin-vben-circle-button"
                    type="button"
                    title="刷新列表"
                    onClick={() => reload()}
                  >
                    <i
                      className="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh"
                      aria-hidden="true"
                    />
                  </button>
                </div>
              </div>
            }
          >
            <div className="admin-table-surface">
              <UserTable
                rows={rows}
                loading={loading}
                canEdit={canEdit}
                canToggle={canToggle}
                canReset={canReset}
                canRoles={canRoles}
                canDelete={canDelete}
                userTypeMetaMap={userTypeMetaMap}
                onEdit={openEdit}
                onToggle={onToggle}
                onReset={openReset}
                onRoles={openRoles}
                onRemove={onRemove}
              />
            </div>

            {page.totalElements > 0 ? (
              <div className="dict-pagination-bar">
                <div className="dict-pagination-summary">共 {page.totalElements} 条记录</div>
                <div className="dict-pagination-right">
                  <BzPagination
                    total={page.totalElements}
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
            <UserFormDialog
              mode={dialogMode}
              model={dialogModel}
              onClose={() => setDialogOpen(false)}
              onSubmit={onSubmit}
            />
          ) : null}
          {resetOpen ? (
            <PasswordResetDialog
              onClose={() => setResetOpen(false)}
              onSubmit={onResetSubmit}
            />
          ) : null}
          {roleOpen ? (
            <UserRoleDialog
              userName={roleTarget?.username || ""}
              roles={roleList}
              selectedIds={roleSelected}
              loading={roleLoading}
              canSave={canRoleEdit}
              onClose={() => setRoleOpen(false)}
              onSubmit={onRoleSubmit}
            />
          ) : null}
        </div>
      </div>
    </div>
  );
}
