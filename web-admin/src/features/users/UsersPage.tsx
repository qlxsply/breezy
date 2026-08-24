"use client";

import { batchListDictionaryOptions as batchListDictOptions } from "@admin/features/dicts/public/dictionary-client";
import type { PublicDictionaryItem as DictItem } from "@admin/features/dicts/public/types";
import { usePermission } from "@admin/features/resources/permissions";
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
} from "@admin/features/users/api/client";
import type { UserEntry, UserStatus } from "@admin/features/users/model/types";
import { USER_PERMISSIONS } from "@admin/features/users/permissions";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { bzConfirm } from "@admin/shared/lib/feedback/confirm";
import { message } from "@admin/shared/lib/feedback/message";
import { AdminListPageTemplate } from "@admin/shared/ui/admin/AdminListPageTemplate";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { AdminSearchField, AdminSearchForm } from "@admin/shared/ui/admin/AdminSearchForm";
import { AdminTablePagination } from "@admin/shared/ui/admin/AdminTablePagination";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import { BzAlert } from "@admin/shared/ui/bz/BzAlert";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import { BzInput } from "@admin/shared/ui/bz/BzInput";
import { BzOption } from "@admin/shared/ui/bz/BzOption";
import { BzSelect } from "@admin/shared/ui/bz/BzSelect";
import { useCallback, useEffect, useRef, useState } from "react";

import { PasswordResetDialog } from "./ui/PasswordResetDialog";
import { UserManageDrawer } from "./ui/UserManageDrawer";
import { UserTable } from "./ui/UserTable";
import styles from "./UsersPage.module.css";

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

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const INITIAL_FILTERS = {
  keyword: "",
  status: "" as "" | UserStatus,
};

type UserFilters = typeof INITIAL_FILTERS;

export function UsersPage() {
  type BatchAction = "disable" | "reset-password" | "delete";
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
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
  const roleRequestRef = useRef<AbortController | null>(null);
  const roleRequestGenerationRef = useRef(0);
  const canView = usePermission(USER_PERMISSIONS.view);
  const canCreate = usePermission(USER_PERMISSIONS.create);
  const canEdit = usePermission(USER_PERMISSIONS.edit);
  const canToggle = usePermission(USER_PERMISSIONS.edit);
  const canReset = usePermission(USER_PERMISSIONS.resetPassword);
  const canBatchDisable = usePermission(USER_PERMISSIONS.batchDisable);
  const canBatchReset = usePermission(USER_PERMISSIONS.batchResetPassword);
  const canBatchDelete = usePermission(USER_PERMISSIONS.batchDelete);
  const canRoleEdit = usePermission(USER_PERMISSIONS.roleEdit);
  const canRoleView = usePermission(USER_PERMISSIONS.roleView);
  const canRoles = canRoleEdit || canRoleView;
  const canDelete = usePermission(USER_PERMISSIONS.delete);
  const {
    page,
    draftFilters,
    setDraftFilters,
    pageNo,
    pageSize,
    loading,
    error,
    submit,
    reset,
    refresh,
    setPageNo,
    setPageSize,
  } = useAdminPagedQuery<UserEntry, UserFilters>({
    initialFilters: INITIAL_FILTERS,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canView,
    normalizeFilters: (filters) => ({ ...filters, keyword: filters.keyword.trim() }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) =>
      pageUsers(
        {
          usernameLike: filters.keyword,
          status: filters.status,
          page: { pageNo: targetPage, pageSize: targetSize },
        },
        { signal },
      ),
  });
  const rows = page.elements;

  const loadDictionaries = useCallback(async (signal: AbortSignal) => {
    try {
      const result = await batchListDictOptions(["USER_TYPE"], { signal });
      setUserTypeMetaMap(toDictMetaMap(result.USER_TYPE));
    } catch (cause) {
      if (isAbortError(cause)) return;
      setUserTypeMetaMap({
        SYSTEM: { label: "系统账号", tagType: "warning" },
        ADMIN: { label: "账号", tagType: "info" },
        USER: { label: "用户", tagType: "info" },
        GUEST: { label: "游客", tagType: "danger" },
      });
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void loadDictionaries(controller.signal);
    return () => controller.abort();
  }, [loadDictionaries]);

  useEffect(() => {
    return () => {
      roleRequestGenerationRef.current += 1;
      roleRequestRef.current?.abort();
      roleRequestRef.current = null;
    };
  }, []);

  function openCreate() {
    if (!canCreate) return;
    cancelRoleRequest();
    setManageMode("create");
    setManageTarget({
      id: "",
      username: "",
      nickname: "",
      userType: "ADMIN",
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
      if (canRoleEdit && manageTarget.userType !== "USER") {
        await updateUserRoles(manageTarget.id, payload.roleIds);
      }
      message.success("保存成功");
    }
    closeManage();
    await refresh();
  }

  async function onToggle(user: UserEntry) {
    if (!canToggle) return;
    const nextStatus: UserStatus = user.status === "ENABLED" ? "DISABLED" : "ENABLED";
    await updateUser(user.id, { nickname: user.nickname, status: nextStatus });
    message.success(nextStatus === "ENABLED" ? "启用成功" : "停用成功");
    await refresh();
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
    await refresh();
  }

  async function openManage(user: UserEntry, mode: "detail" | "edit") {
    roleRequestRef.current?.abort();
    const generation = roleRequestGenerationRef.current + 1;
    roleRequestGenerationRef.current = generation;
    roleRequestRef.current = null;
    setManageTarget(user);
    setManageMode(mode);
    setManageOpen(true);
    if (user.userType === "USER" || !canRoles) {
      setRoleSelected([]);
      setRoleLoading(false);
      return;
    }
    const controller = new AbortController();
    roleRequestRef.current = controller;
    setRoleLoading(true);
    try {
      const roleIds = await getUserRoles(user.id, { signal: controller.signal });
      if (
        generation === roleRequestGenerationRef.current &&
        roleRequestRef.current === controller &&
        !controller.signal.aborted
      ) {
        setRoleSelected(roleIds);
      }
    } catch (cause) {
      if (
        generation === roleRequestGenerationRef.current &&
        roleRequestRef.current === controller &&
        !isAbortError(cause)
      ) {
        setRoleSelected([]);
        message.error(cause instanceof Error ? cause.message : "用户角色加载失败");
      }
    } finally {
      if (
        generation === roleRequestGenerationRef.current &&
        roleRequestRef.current === controller
      ) {
        roleRequestRef.current = null;
        setRoleLoading(false);
      }
    }
  }

  function cancelRoleRequest() {
    roleRequestGenerationRef.current += 1;
    roleRequestRef.current?.abort();
    roleRequestRef.current = null;
    setRoleLoading(false);
  }

  function closeManage() {
    cancelRoleRequest();
    setManageOpen(false);
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
    await refresh();
  }

  function isProtectedUser(user: UserEntry): boolean {
    return user.userType === "SYSTEM" || (user.userType === "ADMIN" && user.username === "admin");
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
    await refresh();
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
      className={styles.page}
      queryPanelVisible={queryPanelVisible}
      queryPanel={
        <AdminSearchForm
          visible={queryPanelVisible}
          onSubmit={submit}
          onReset={reset}
        >
          <AdminSearchField label="账号">
            <BzInput
              modelValue={draftFilters.keyword}
              placeholder="按账号或昵称搜索"
              clearable
              onValueChange={(keyword) => setDraftFilters((filters) => ({ ...filters, keyword }))}
            />
          </AdminSearchField>
          <AdminSearchField label="状态">
            <BzSelect
              modelValue={draftFilters.status}
              placeholder="全部状态"
              clearable
              onValueChange={(status) =>
                setDraftFilters((filters) => ({
                  ...filters,
                  status: (status ?? "") as "" | UserStatus,
                }))
              }
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
          </AdminSearchField>
        </AdminSearchForm>
      }
      batchToolbar={
        batchAction ? (
          <div className={layoutStyles.batchToolbar}>
            <div className={layoutStyles.batchToolbarSummary}>
              {batchLabel}，已选 {selectedIds.length} 项
            </div>
            <div className={layoutStyles.batchToolbarActions}>
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
                className={layoutStyles.toolbarPrimary}
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
            onRefresh={() => void refresh()}
          />
        ) : null
      }
      table={
        <>
          {error ? (
            <BzAlert
              key={error.message}
              title={error.message}
              type="error"
              closable={false}
            />
          ) : null}
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
        </>
      }
      footer={
        <AdminTablePagination
          total={page.totalElements}
          pageNo={pageNo}
          pageSize={pageSize}
          pageSizes={PAGE_SIZE_OPTIONS}
          onPageChange={setPageNo}
          onPageSizeChange={setPageSize}
        />
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
              onClose={closeManage}
              onSubmit={onManageSubmit}
            />
          ) : null}
        </>
      }
    />
  );
}

function isAbortError(cause: unknown): boolean {
  return cause instanceof Error && cause.name === "AbortError";
}
