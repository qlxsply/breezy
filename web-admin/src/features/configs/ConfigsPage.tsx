"use client";

import {
  batchResetConfigDefaults,
  getConfig,
  listConfigs,
  updateConfig,
  validateConfig,
} from "@admin/features/configs/api/client";
import type {
  ConfigItem,
  ConfigManagementStatus,
  ConfigViolation,
  JsonObject,
} from "@admin/features/configs/model/types";
import { CONFIG_PERMISSIONS } from "@admin/features/configs/permissions";
import {
  ConfigManageDrawer,
  type ConfigManageMode,
} from "@admin/features/configs/ui/ConfigManageDrawer";
import { usePermission } from "@admin/features/resources/permissions";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { bzConfirm } from "@admin/shared/lib/feedback/confirm";
import { message } from "@admin/shared/lib/feedback/message";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import { AdminListPageTemplate } from "@admin/shared/ui/admin/AdminListPageTemplate";
import { AdminSearchField, AdminSearchForm } from "@admin/shared/ui/admin/AdminSearchForm";
import { AdminTablePagination } from "@admin/shared/ui/admin/AdminTablePagination";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import { BzAlert } from "@admin/shared/ui/bz/BzAlert";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import { BzInput } from "@admin/shared/ui/bz/BzInput";
import { BzTable, type BzTableColumn } from "@admin/shared/ui/bz/BzTable";
import { BzTag } from "@admin/shared/ui/bz/BzTag";
import { useEffect, useRef, useState } from "react";

import styles from "./ConfigsPage.module.css";

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const INITIAL_FILTERS = {
  keyword: "",
  module: "",
  group: "",
};

type ConfigFilters = typeof INITIAL_FILTERS;

const STATUS_META: Record<
  ConfigManagementStatus,
  { label: string; type: "info" | "warning" | "danger" | "success" }
> = {
  DEFAULT_VALUE: { label: "使用默认值", type: "info" },
  CONFIGURED: { label: "已配置", type: "success" },
  INVALID_DATABASE_VALUE: { label: "配置异常", type: "danger" },
  RESTART_REQUIRED: { label: "等待重启", type: "warning" },
  READ_ONLY: { label: "只读", type: "info" },
};

function policyLabel(item: ConfigItem): string {
  return item.activationPolicy === "DYNAMIC" ? "动态生效" : "重启后生效";
}

export function ConfigsPage() {
  const canView = usePermission(CONFIG_PERMISSIONS.view);
  const canUpdate = usePermission(CONFIG_PERMISSIONS.edit);
  const canBatchReset = usePermission(CONFIG_PERMISSIONS.batchResetDefault);
  const [drawerSaving, setDrawerSaving] = useState(false);
  const [batchSaving, setBatchSaving] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<ConfigManageMode>("detail");
  const [current, setCurrent] = useState<ConfigItem | null>(null);
  const [batchMode, setBatchMode] = useState(false);
  const [selectedItems, setSelectedItems] = useState<Record<string, number>>({});
  const drawerControllerRef = useRef<AbortController | null>(null);
  const drawerRequestSequenceRef = useRef(0);
  const configSaveSequenceRef = useRef(0);
  const configSaveLockRef = useRef(false);
  const currentConfigKeyRef = useRef<string | null>(null);
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
  } = useAdminPagedQuery<ConfigItem, ConfigFilters>({
    initialFilters: INITIAL_FILTERS,
    pageSizes: PAGE_SIZE_OPTIONS,
    enabled: canView,
    normalizeFilters: (filters) => ({
      keyword: filters.keyword.trim(),
      module: filters.module.trim(),
      group: filters.group.trim(),
    }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) =>
      listConfigs(
        {
          ...filters,
          page: { pageNo: targetPage, pageSize: targetSize },
        },
        { signal },
      ),
  });

  function cancelDrawerRequest() {
    drawerRequestSequenceRef.current += 1;
    drawerControllerRef.current?.abort();
    drawerControllerRef.current = null;
  }

  useEffect(
    () => () => {
      cancelDrawerRequest();
      configSaveSequenceRef.current += 1;
      configSaveLockRef.current = false;
    },
    [],
  );

  async function openDrawer(item: ConfigItem, mode: ConfigManageMode) {
    cancelDrawerRequest();
    configSaveSequenceRef.current += 1;
    configSaveLockRef.current = false;
    currentConfigKeyRef.current = item.key;
    setDrawerSaving(false);
    const controller = new AbortController();
    const requestSequence = drawerRequestSequenceRef.current;
    drawerControllerRef.current = controller;
    setDrawerMode(mode);
    setCurrent(item);
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      const detail = await getConfig(item.key, { signal: controller.signal });
      if (
        requestSequence === drawerRequestSequenceRef.current &&
        drawerControllerRef.current === controller &&
        !controller.signal.aborted
      ) {
        setCurrent(detail);
      }
    } catch (cause) {
      if (
        requestSequence === drawerRequestSequenceRef.current &&
        drawerControllerRef.current === controller &&
        !isAbortError(cause)
      ) {
        message.error(cause instanceof Error ? cause.message : "配置详情加载失败");
        setDrawerOpen(false);
      }
    } finally {
      if (
        requestSequence === drawerRequestSequenceRef.current &&
        drawerControllerRef.current === controller
      ) {
        drawerControllerRef.current = null;
        setDrawerLoading(false);
      }
    }
  }

  function closeDrawer() {
    if (configSaveLockRef.current) return;
    cancelDrawerRequest();
    currentConfigKeyRef.current = null;
    setDrawerLoading(false);
    setDrawerOpen(false);
  }

  async function saveConfig(value: JsonObject, reason: string): Promise<ConfigViolation[]> {
    if (!current || configSaveLockRef.current) return [];
    const config = current;
    const saveSequence = ++configSaveSequenceRef.current;
    configSaveLockRef.current = true;
    currentConfigKeyRef.current = config.key;
    setDrawerSaving(true);
    try {
      const validation = await validateConfig(config.key, value);
      if (!isCurrentConfigSave(saveSequence, config.key)) return [];
      if (!validation.valid) {
        message.warning("配置校验未通过");
        return validation.violations;
      }
      const confirmed = await bzConfirm({
        title: "保存配置",
        content:
          config.activationPolicy === "RESTART_REQUIRED"
            ? "该配置保存后需要重启服务才会生效，确认保存吗？"
            : "确认保存当前配置吗？",
        confirmText: "保存",
        cancelText: "取消",
      });
      if (!isCurrentConfigSave(saveSequence, config.key) || !confirmed) return [];
      const result = await updateConfig(config.key, {
        expectedRevision: config.persistedRevision,
        reason: reason || undefined,
        value,
      });
      if (!isCurrentConfigSave(saveSequence, config.key)) return [];
      message.success(result.pendingRestart ? "保存成功，配置将在重启后生效" : "保存成功");
      configSaveLockRef.current = false;
      setDrawerSaving(false);
      closeDrawer();
      await refresh();
      return [];
    } catch (cause) {
      if (isCurrentConfigSave(saveSequence, config.key)) {
        message.error(cause instanceof Error ? cause.message : "配置保存失败");
      }
      return [];
    } finally {
      if (isCurrentConfigSave(saveSequence, config.key)) {
        configSaveLockRef.current = false;
        setDrawerSaving(false);
      }
    }
  }

  function isCurrentConfigSave(sequence: number, configKey: string): boolean {
    return sequence === configSaveSequenceRef.current && configKey === currentConfigKeyRef.current;
  }

  function isBatchSelectable(item: ConfigItem): boolean {
    return item.configured && item.editPolicy === "ADMIN_EDITABLE";
  }

  function toggleSelection(item: ConfigItem, checked: boolean) {
    if (!isBatchSelectable(item)) return;
    setSelectedItems((previous) => {
      const next = { ...previous };
      if (checked) next[item.key] = item.persistedRevision;
      else delete next[item.key];
      return next;
    });
  }

  function toggleCurrentPage(checked: boolean) {
    setSelectedItems((previous) => {
      const next = { ...previous };
      page.elements.filter(isBatchSelectable).forEach((item) => {
        if (checked) next[item.key] = item.persistedRevision;
        else delete next[item.key];
      });
      return next;
    });
  }

  function beginBatchReset() {
    setBatchMode(true);
    setSelectedItems({});
  }

  function cancelBatchReset() {
    setBatchMode(false);
    setSelectedItems({});
  }

  async function confirmBatchReset() {
    const items = Object.entries(selectedItems).map(([configKey, expectedRevision]) => ({
      configKey,
      expectedRevision,
    }));
    if (!items.length) return;
    const confirmed = await bzConfirm({
      title: "批量重置默认值",
      content: `确认将已选 ${items.length} 项配置重置为代码默认值吗？`,
      confirmText: "重置",
      cancelText: "取消",
    });
    if (!confirmed) return;
    setBatchSaving(true);
    try {
      const results = await batchResetConfigDefaults(items);
      const restartCount = results.filter((result) => result.pendingRestart).length;
      message.success(
        restartCount > 0
          ? `已重置 ${results.length} 项配置，其中 ${restartCount} 项需要重启后生效`
          : `已重置 ${results.length} 项配置`,
      );
      cancelBatchReset();
      await refresh();
    } finally {
      setBatchSaving(false);
    }
  }

  function getRowActions(row: ConfigItem): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      {
        key: "detail",
        label: "详情",
        level: "default",
        onClick: () => void openDrawer(row, "detail"),
      },
    ];
    if (canUpdate) {
      actions.push({
        key: "edit",
        label: "编辑",
        level: "primary",
        disabled: row.editPolicy !== "ADMIN_EDITABLE",
        onClick: () => void openDrawer(row, "edit"),
      });
    }
    return actions;
  }

  const columns: BzTableColumn<ConfigItem>[] = [
    {
      key: "key",
      title: "配置键",
      width: 320,
      render: (row) => <span className={styles.codeText}>{row.key}</span>,
    },
    { key: "title", title: "配置名称", minWidth: 180, render: (row) => row.title },
    { key: "module", title: "模块", width: 120, render: (row) => row.module },
    { key: "group", title: "分组", width: 130, render: (row) => row.group },
    {
      key: "status",
      title: "状态",
      width: 130,
      render: (row) => (
        <BzTag type={STATUS_META[row.status].type}>{STATUS_META[row.status].label}</BzTag>
      ),
    },
    {
      key: "activationPolicy",
      title: "生效方式",
      width: 130,
      render: (row) => (
        <BzTag type={row.activationPolicy === "DYNAMIC" ? "success" : "warning"}>
          {policyLabel(row)}
        </BzTag>
      ),
    },
    { key: "revision", title: "版本", width: 90, render: (row) => row.persistedRevision },
  ];
  const actionsColumn = createAdminActionsColumn({
    rows: page.elements,
    getActions: getRowActions,
  });
  if (actionsColumn) columns.push(actionsColumn);

  return (
    <AdminListPageTemplate
      queryPanelVisible={queryPanelVisible}
      queryPanel={
        <AdminSearchForm
          visible={queryPanelVisible}
          onSubmit={submit}
          onReset={reset}
        >
          <AdminSearchField label="关键词">
            <BzInput
              modelValue={draftFilters.keyword}
              placeholder="配置键、名称或说明"
              clearable
              onValueChange={(keyword) => setDraftFilters((filters) => ({ ...filters, keyword }))}
            />
          </AdminSearchField>
          <AdminSearchField label="模块">
            <BzInput
              modelValue={draftFilters.module}
              placeholder="例如 system"
              clearable
              onValueChange={(module) => setDraftFilters((filters) => ({ ...filters, module }))}
            />
          </AdminSearchField>
          <AdminSearchField label="分组">
            <BzInput
              modelValue={draftFilters.group}
              placeholder="例如 security"
              clearable
              onValueChange={(group) => setDraftFilters((filters) => ({ ...filters, group }))}
            />
          </AdminSearchField>
        </AdminSearchForm>
      }
      batchToolbar={
        batchMode ? (
          <div className="admin-batch-toolbar">
            <div className="admin-batch-toolbar__summary">
              批量重置默认值，已选 {Object.keys(selectedItems).length} 项
            </div>
            <div className="admin-batch-toolbar__actions">
              <BzButton
                buttonType="primary"
                loading={batchSaving}
                disabled={!Object.keys(selectedItems).length}
                onClick={() => void confirmBatchReset()}
              >
                确认
              </BzButton>
              <BzButton
                disabled={batchSaving}
                onClick={cancelBatchReset}
              >
                取消
              </BzButton>
            </div>
          </div>
        ) : null
      }
      businessActions={
        canBatchReset && !batchMode ? (
          <BzButton onClick={beginBatchReset}>批量重置默认值</BzButton>
        ) : null
      }
      queryTools={
        !batchMode ? (
          <AdminTableTools
            queryPanelVisible={queryPanelVisible}
            onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
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
          <BzTable
            data={page.elements}
            columns={columns}
            rowKey="key"
            loading={loading}
            rowSelection={
              batchMode
                ? {
                    selectedRowKeys: Object.keys(selectedItems),
                    isRowSelectable: isBatchSelectable,
                    onToggle: toggleSelection,
                    onToggleCurrentPage: (_rows, selected) => toggleCurrentPage(selected),
                  }
                : undefined
            }
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
        <ConfigManageDrawer
          open={drawerOpen}
          mode={drawerMode}
          item={current}
          loading={drawerLoading}
          saving={drawerSaving}
          canEdit={canUpdate}
          onClose={closeDrawer}
          onSubmit={saveConfig}
        />
      }
    />
  );
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}
