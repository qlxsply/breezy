"use client";

import { batchListDictOptions } from "@admin/api/dicts";
import {
  batchDeleteUsers,
  batchResetUserPassword,
  batchUpdateUserStatus,
  createUser,
  deleteUser,
  getUserRoles,
  pageUsers,
  resetUserPassword,
  updateUser,
  updateUserRoles,
} from "@admin/api/users";
import { AdminListPageTemplate } from "@admin/components/admin/AdminListPageTemplate";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import type { UserEntry, UserStatus } from "@admin/types/user-admin";
import { useCallback, useEffect, useRef, useState } from "react";

import { BzButton } from "../bz/BzButton";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOption } from "../bz/BzOption";
import { BzPagination } from "../bz/BzPagination";
import { BzSelect } from "../bz/BzSelect";
import { PasswordResetDialog } from "../users-admin/PasswordResetDialog";
import { UserManageDrawer } from "../users-admin/UserManageDrawer";
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
  type BatchAction = "disable" | "reset-password" | "delete";
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
  const [batchAction, setBatchAction] = useState<BatchAction | null>(null);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const [userTypeMetaMap, setUserTypeMetaMap] = useState<
    Record<string, { label: string; tagType?: string | null }>
  >({});

  const [resetOpen, setResetOpen] = useState(false);
  const [resetTarget, setResetTarget] = useState<UserEntry | null>(null);

  const [manageOpen, setManageOpen] = useState(false);
  const [manageMode, setManageMode] = useState<"create" | "detail" | "edit">("detail");
  const [manageTarget, setManageTarget] = useState<UserEntry | null>(null);
  const [roleSelected, setRoleSelected] = useState<string[]>([]);
  const [roleLoading, setRoleLoading] = useState(false);
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const canCreate = hasResourceCodeAccess("user-manage-create");
  const canEdit = hasResourceCodeAccess("user-manage-edit");
  const canToggle = hasResourceCodeAccess("user-manage-edit");
  const canReset = hasResourceCodeAccess("user-manage-reset-password");
  const canBatchDisable = hasResourceCodeAccess("user-manage-batch-disable");
  const canBatchReset = hasResourceCodeAccess("user-manage-batch-reset-password");
  const canBatchDelete = hasResourceCodeAccess("user-manage-batch-delete");
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
    setManageMode("create");
    setManageTarget({
      id: "",
      username: "",
      nickname: "",
      userType: "INTERNAL",
      status: "ENABLED",
    });
    setRoleSelected([]);
    setManageOpen(true);
  }

  function openEdit(user: UserEntry) {
    if (!canEdit && !canRoles) return;
    void openManage(user, "edit");
  }

  function openDetail(user: UserEntry) {
    void openManage(user, "detail");
  }

  async function onManageSubmit(payload: {
    username: string;
    nickname: string;
    password?: string;
    status: UserStatus;
    roleIds: string[];
  }) {
    if (manageMode === "create") {
      if (!payload.password) return;
      await createUser({
        username: payload.username,
        nickname: payload.nickname,
        password: payload.password,
        roleIds: payload.roleIds,
      });
      message.success("新增成功");
    } else if (manageTarget) {
      await updateUser(manageTarget.id, { nickname: payload.nickname, status: payload.status });
      if (canRoleEdit && manageTarget.userType !== "EXTERNAL") {
        await updateUserRoles(manageTarget.id, payload.roleIds);
      }
      message.success("保存成功");
    }
    setManageOpen(false);
    await reload();
  }

  async function onToggle(user: UserEntry) {
    if (!canToggle) return;
    const nextStatus: UserStatus = user.status === "ENABLED" ? "DISABLED" : "ENABLED";
    await updateUser(user.id, { nickname: user.nickname, status: nextStatus });
    message.success(nextStatus === "ENABLED" ? "启用成功" : "停用成功");
    await reload();
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
    message.success("重置密码成功");
  }

  async function openManage(user: UserEntry, mode: "detail" | "edit") {
    setManageTarget(user);
    setManageMode(mode);
    setManageOpen(true);
    if (user.userType === "EXTERNAL" || !canRoles) {
      setRoleSelected([]);
      return;
    }
    setRoleLoading(true);
    try {
      setRoleSelected(await getUserRoles(user.id));
    } finally {
      setRoleLoading(false);
    }
  }

  async function onRemove(user: UserEntry) {
    if (!canDelete) return;
    const confirmed = await bzConfirm({
      title: "删除用户",
      content: `确认删除该用户吗？删除后不可恢复。账号：${user.username}`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteUser(user.id);
    message.success("删除成功");
    await reload();
  }

  function isProtectedUser(user: UserEntry): boolean {
    return (
      user.userType === "SYSTEM" || (user.userType === "INTERNAL" && user.username === "admin")
    );
  }

  function beginBatch(nextAction: BatchAction) {
    setBatchAction(nextAction);
    setSelectedIds([]);
  }

  function cancelBatch() {
    setBatchAction(null);
    setSelectedIds([]);
  }

  function toggleSelect(user: UserEntry, checked: boolean) {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (checked) next.add(user.id);
      else next.delete(user.id);
      return Array.from(next);
    });
  }

  function toggleSelectAllCurrentPage(checked: boolean) {
    const currentPageSelectableIds = rows
      .filter((row) => !isProtectedUser(row))
      .map((row) => row.id);
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (checked) {
        currentPageSelectableIds.forEach((id) => next.add(id));
      } else {
        currentPageSelectableIds.forEach((id) => next.delete(id));
      }
      return Array.from(next);
    });
  }

  async function confirmBatchAction() {
    if (!batchAction || selectedIds.length === 0) return;
    if (batchAction === "disable") {
      await batchUpdateUserStatus(selectedIds, "DISABLED");
      message.success("批量停用成功");
    }
    if (batchAction === "reset-password") {
      const confirmed = await bzConfirm({
        title: "批量重置密码",
        content: `确认将已选 ${selectedIds.length} 个账号的密码重置为 123456？`,
        confirmText: "重置",
        cancelText: "取消",
      });
      if (!confirmed) return;
      await batchResetUserPassword(selectedIds);
      message.success("批量重置密码成功");
    }
    if (batchAction === "delete") {
      const confirmed = await bzConfirm({
        title: "批量删除用户",
        content: `确认删除已选 ${selectedIds.length} 个账号？删除后不可恢复。`,
        confirmText: "删除",
        cancelText: "取消",
      });
      if (!confirmed) return;
      await batchDeleteUsers(selectedIds);
      message.success("批量删除成功");
    }
    cancelBatch();
    await reload();
  }

  const batchLabel =
    batchAction === "disable"
      ? "批量停用中"
      : batchAction === "reset-password"
        ? "批量重置密码中"
        : batchAction === "delete"
          ? "批量删除中"
          : "";

  return (
    <AdminListPageTemplate
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
                  <BzOption
                    label="启用"
                    value="ENABLED"
                  />
                  <BzOption
                    label="停用"
                    value="DISABLED"
                  />
                </BzSelect>
              </div>
            </BzFormItem>
            <div className="admin-query-actions">
              <BzButton
                className="admin-filter-secondary"
                nativeType="button"
                onClick={resetFilters}
              >
                重置
              </BzButton>
              <BzButton
                className="admin-filter-primary"
                buttonType="primary"
                nativeType="button"
                onClick={applyFilters}
              >
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
                  <i
                    className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`}
                    aria-hidden="true"
                  />
                </button>
              ) : null}
            </div>
          </form>
        </div>
      }
      batchToolbar={
        batchAction ? (
          <div className="admin-batch-toolbar">
            <div className="admin-batch-toolbar__summary">
              {batchLabel}，已选 {selectedIds.length} 项
            </div>
            <div className="admin-batch-toolbar__actions">
              <BzButton
                buttonType="primary"
                disabled={selectedIds.length === 0}
                onClick={() => void confirmBatchAction()}
              >
                确认
              </BzButton>
              <BzButton onClick={cancelBatch}>取消</BzButton>
            </div>
          </div>
        ) : null
      }
      businessActions={
        !batchAction ? (
          <>
            {canCreate ? (
              <BzButton
                className="admin-toolbar-primary"
                buttonType="primary"
                onClick={openCreate}
              >
                新增
              </BzButton>
            ) : null}
            {canBatchDisable ? (
              <BzButton onClick={() => beginBatch("disable")}>批量停用</BzButton>
            ) : null}
            {canBatchReset ? (
              <BzButton onClick={() => beginBatch("reset-password")}>批量重置密码</BzButton>
            ) : null}
            {canBatchDelete ? (
              <BzButton onClick={() => beginBatch("delete")}>批量删除</BzButton>
            ) : null}
          </>
        ) : null
      }
      queryTools={
        !batchAction ? (
          <AdminTableTools
            queryPanelVisible={queryPanelVisible}
            onToggleQueryPanel={() => setQueryPanelVisible((v) => !v)}
            onRefresh={() => reload()}
          />
        ) : null
      }
      table={
        <UserTable
          rows={rows}
          loading={loading}
          batchMode={Boolean(batchAction)}
          selectedIds={selectedIds}
          canEdit={canEdit}
          canToggle={canToggle}
          canReset={canReset}
          canRoles={canRoles}
          canDelete={canDelete}
          userTypeMetaMap={userTypeMetaMap}
          onDetail={openDetail}
          onEdit={openEdit}
          onToggle={onToggle}
          onReset={openReset}
          onRemove={onRemove}
          onToggleSelect={toggleSelect}
          onToggleSelectAll={toggleSelectAllCurrentPage}
        />
      }
      footer={
        page.totalElements > 0 ? (
          <div className="dict-pagination-bar admin-list-table-footer">
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
        ) : null
      }
      overlays={
        <>
          {resetOpen ? (
            <PasswordResetDialog
              onClose={() => setResetOpen(false)}
              onSubmit={onResetSubmit}
            />
          ) : null}
          {manageOpen ? (
            <UserManageDrawer
              open={manageOpen}
              mode={manageMode}
              model={manageTarget}
              selectedIds={roleSelected}
              loading={roleLoading}
              canEditBasic={manageMode === "create" ? canCreate : canEdit}
              canEditRoles={canRoleEdit}
              canViewRoles={canRoles}
              userTypeMetaMap={userTypeMetaMap}
              onClose={() => setManageOpen(false)}
              onSubmit={onManageSubmit}
            />
          ) : null}
        </>
      }
    />
  );
}
