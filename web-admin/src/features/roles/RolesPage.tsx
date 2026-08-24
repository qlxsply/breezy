"use client";

import { usePermission } from "@admin/features/resources/permissions";
import {
  createRole,
  deleteRole,
  getRoleGrantSelection,
  listRoleGrantResources,
  pageRoles,
  updateRole,
  updateRoleGrantSelection,
} from "@admin/features/roles/api/client";
import type {
  RoleEntry,
  RoleGrantResourceEntry,
  RoleGrantSelection,
} from "@admin/features/roles/model/types";
import { ROLE_PERMISSIONS } from "@admin/features/roles/permissions";
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

import styles from "./RolesPage.module.css";
import { RolePermissionDialog } from "./ui/RolePermissionDialog";
import { RoleTable } from "./ui/RoleTable";

const defaultRoleModel: RoleEntry = { id: "", code: "", name: "", enabled: true };
const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const INITIAL_FILTERS = {
  keyword: "",
  enabled: "" as "" | "true" | "false",
};

type RoleFilters = typeof INITIAL_FILTERS;

export function RolesPage() {
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [manageOpen, setManageOpen] = useState(false);
  const [manageMode, setManageMode] = useState<"create" | "detail" | "edit">("detail");
  const [manageTarget, setManageTarget] = useState<RoleEntry | null>(null);
  const [manageSelection, setManageSelection] = useState<RoleGrantSelection>({
    resourceIds: [],
  });
  const [manageLoading, setManageLoading] = useState(false);
  const [grantResourcesLoading, setGrantResourcesLoading] = useState(false);
  const [grantResourceRows, setGrantResourceRows] = useState<RoleGrantResourceEntry[]>([]);
  const grantSelectionRequestRef = useRef<AbortController | null>(null);
  const grantSelectionGenerationRef = useRef(0);
  const grantResourcesRequestRef = useRef<AbortController | null>(null);
  const grantResourcesGenerationRef = useRef(0);

  const canView = usePermission(ROLE_PERMISSIONS.view);
  const canCreate = usePermission(ROLE_PERMISSIONS.create);
  const canEdit = usePermission(ROLE_PERMISSIONS.edit);
  const canDelete = usePermission(ROLE_PERMISSIONS.delete);
  const canGrantView = usePermission(ROLE_PERMISSIONS.permissionView);
  const canGrantEdit = usePermission(ROLE_PERMISSIONS.permissionEdit);
  const canGrant = canGrantView || canGrantEdit;
  const canManage = canEdit || canGrantEdit;
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
  } = useAdminPagedQuery<RoleEntry, RoleFilters>({
    initialFilters: INITIAL_FILTERS,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canView,
    normalizeFilters: (filters) => ({ ...filters, keyword: filters.keyword.trim() }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) =>
      pageRoles(
        {
          keyword: filters.keyword,
          enabled: filters.enabled === "" ? undefined : filters.enabled === "true",
          page: { pageNo: targetPage, pageSize: targetSize },
        },
        { signal },
      ),
  });

  const reloadGrantResources = useCallback(async () => {
    grantResourcesRequestRef.current?.abort();
    const generation = grantResourcesGenerationRef.current + 1;
    grantResourcesGenerationRef.current = generation;
    const controller = new AbortController();
    grantResourcesRequestRef.current = controller;
    setGrantResourcesLoading(true);
    try {
      const rows = await listRoleGrantResources({ signal: controller.signal });
      if (
        generation === grantResourcesGenerationRef.current &&
        grantResourcesRequestRef.current === controller &&
        !controller.signal.aborted
      ) {
        setGrantResourceRows(rows);
      }
    } catch (cause) {
      if (
        generation === grantResourcesGenerationRef.current &&
        grantResourcesRequestRef.current === controller &&
        !isAbortError(cause)
      ) {
        setGrantResourceRows([]);
      }
    } finally {
      if (
        generation === grantResourcesGenerationRef.current &&
        grantResourcesRequestRef.current === controller
      ) {
        grantResourcesRequestRef.current = null;
        setGrantResourcesLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    if (!canGrant) {
      grantResourcesGenerationRef.current += 1;
      grantResourcesRequestRef.current?.abort();
      grantResourcesRequestRef.current = null;
      setGrantResourcesLoading(false);
      setGrantResourceRows([]);
      return;
    }
    void reloadGrantResources();
    return () => {
      grantResourcesGenerationRef.current += 1;
      grantResourcesRequestRef.current?.abort();
      grantResourcesRequestRef.current = null;
    };
  }, [canGrant, reloadGrantResources]);

  useEffect(() => {
    return () => {
      grantSelectionGenerationRef.current += 1;
      grantSelectionRequestRef.current?.abort();
      grantSelectionRequestRef.current = null;
    };
  }, []);

  async function openCreate() {
    if (!canCreate) return;
    cancelGrantSelectionRequest();
    setManageMode("create");
    setManageTarget({ ...defaultRoleModel });
    setManageSelection({ resourceIds: [] });
    setManageOpen(true);
    if (canGrant && grantResourceRows.length === 0) {
      await reloadGrantResources();
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
    await refresh();
  }

  async function openManage(role: RoleEntry, mode: "detail" | "edit") {
    grantSelectionRequestRef.current?.abort();
    const generation = grantSelectionGenerationRef.current + 1;
    grantSelectionGenerationRef.current = generation;
    grantSelectionRequestRef.current = null;
    setManageTarget(role);
    setManageMode(mode);
    setManageOpen(true);

    if (!canGrant) {
      setManageSelection({ resourceIds: [] });
      setManageLoading(false);
      return;
    }

    const controller = new AbortController();
    grantSelectionRequestRef.current = controller;
    setManageLoading(true);
    try {
      if (grantResourceRows.length === 0) await reloadGrantResources();
      if (
        generation !== grantSelectionGenerationRef.current ||
        grantSelectionRequestRef.current !== controller ||
        controller.signal.aborted
      ) {
        return;
      }
      const selection = await getRoleGrantSelection(role.id, { signal: controller.signal });
      if (
        generation === grantSelectionGenerationRef.current &&
        grantSelectionRequestRef.current === controller &&
        !controller.signal.aborted
      ) {
        setManageSelection(selection);
      }
    } catch (cause) {
      if (
        generation === grantSelectionGenerationRef.current &&
        grantSelectionRequestRef.current === controller &&
        !isAbortError(cause)
      ) {
        setManageSelection({ resourceIds: [] });
        message.error(cause instanceof Error ? cause.message : "角色授权加载失败");
      }
    } finally {
      if (
        generation === grantSelectionGenerationRef.current &&
        grantSelectionRequestRef.current === controller
      ) {
        grantSelectionRequestRef.current = null;
        setManageLoading(false);
      }
    }
  }

  function cancelGrantSelectionRequest() {
    grantSelectionGenerationRef.current += 1;
    grantSelectionRequestRef.current?.abort();
    grantSelectionRequestRef.current = null;
    setManageLoading(false);
  }

  function cancelGrantResourcesRequest() {
    grantResourcesGenerationRef.current += 1;
    grantResourcesRequestRef.current?.abort();
    grantResourcesRequestRef.current = null;
    setGrantResourcesLoading(false);
  }

  function closeManage() {
    cancelGrantSelectionRequest();
    cancelGrantResourcesRequest();
    setManageOpen(false);
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
      closeManage();
      message.success("新增成功");
      await refresh();
      return;
    }

    const roleChanged =
      canEdit &&
      (manageTarget.code !== nextRole.code ||
        manageTarget.name !== nextRole.name ||
        manageTarget.enabled !== nextRole.enabled);

    if (!roleChanged && !payload.permissionChanged) {
      closeManage();
      return;
    }

    if (roleChanged) {
      await updateRole(manageTarget.id, nextRole);
    }

    if (canGrantEdit && payload.permissionChanged) {
      await updateRoleGrantSelection(manageTarget.id, payload.selection);
    }

    setManageSelection({ resourceIds: [...payload.selection.resourceIds] });
    closeManage();

    if (roleChanged && payload.permissionChanged) {
      message.success("保存成功，角色权限变更将在受影响用户重新登录后完全生效");
    } else if (payload.permissionChanged) {
      message.success("角色权限已保存，受影响用户需要重新登录");
    } else {
      message.success("保存成功");
    }

    await refresh();
  }

  return (
    <AdminListPageTemplate
      className={styles.page}
      regionClassName={styles.region}
      queryPanelClassName={styles.queryPanel}
      toolbarRowClassName={styles.toolbarRow}
      businessActionsClassName={styles.businessActions}
      queryToolsClassName={styles.queryTools}
      tableAreaClassName={styles.tableArea}
      queryPanelVisible={queryPanelVisible}
      queryPanel={
        <AdminSearchForm
          visible={queryPanelVisible}
          onSubmit={submit}
          onReset={reset}
        >
          <AdminSearchField label="角色">
            <BzInput
              modelValue={draftFilters.keyword}
              placeholder="搜索角色编码/名称"
              clearable
              onValueChange={(keyword) => setDraftFilters((filters) => ({ ...filters, keyword }))}
            />
          </AdminSearchField>
          <AdminSearchField label="状态">
            <BzSelect
              modelValue={draftFilters.enabled}
              placeholder="全部状态"
              clearable
              onValueChange={(enabled) =>
                setDraftFilters((filters) => ({
                  ...filters,
                  enabled: (enabled ?? "") as "" | "true" | "false",
                }))
              }
            >
              <BzOption
                label="启用"
                value="true"
              />
              <BzOption
                label="停用"
                value="false"
              />
            </BzSelect>
          </AdminSearchField>
        </AdminSearchForm>
      }
      businessActions={
        canCreate ? (
          <BzButton
            className={layoutStyles.toolbarPrimary}
            buttonType="primary"
            onClick={openCreate}
          >
            新增
          </BzButton>
        ) : null
      }
      queryTools={
        <AdminTableTools
          queryPanelVisible={queryPanelVisible}
          onToggleQueryPanel={() => setQueryPanelVisible((v) => !v)}
          onRefresh={() => void refresh()}
        />
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
          <RoleTable
            rows={page.elements}
            loading={loading}
            canEdit={canManage}
            canDelete={canDelete}
            onDetail={openDetail}
            onEdit={openEdit}
            onRemove={onRemove}
          />
        </>
      }
      footer={
        <div className={styles.tableFooter}>
          <AdminTablePagination
            total={page.totalElements}
            pageNo={pageNo}
            pageSize={pageSize}
            pageSizes={PAGE_SIZE_OPTIONS}
            onPageChange={setPageNo}
            onPageSizeChange={setPageSize}
          />
        </div>
      }
      overlays={
        manageOpen ? (
          <RolePermissionDialog
            mode={manageMode}
            role={manageTarget}
            resources={grantResourceRows}
            selection={manageSelection}
            loading={manageLoading || grantResourcesLoading}
            canEditBasic={canEdit}
            canViewPermissions={canGrant}
            canEditPermissions={canGrantEdit}
            onClose={closeManage}
            onSubmit={onManageSubmit}
          />
        ) : null
      }
    />
  );
}

function isAbortError(cause: unknown): boolean {
  return cause instanceof Error && cause.name === "AbortError";
}
