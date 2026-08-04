"use client";

import {
  batchResetConfigDefaults,
  getConfig,
  listConfigs,
  updateConfig,
  validateConfig,
} from "@admin/api/configs";
import { createAdminActionsColumn } from "@admin/components/admin/admin-actions-column";
import { AdminListPageTemplate } from "@admin/components/admin/AdminListPageTemplate";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import { BzButton } from "@admin/components/bz/BzButton";
import { BzFormItem } from "@admin/components/bz/BzFormItem";
import { BzInput } from "@admin/components/bz/BzInput";
import { BzPagination } from "@admin/components/bz/BzPagination";
import { BzTable, type BzTableColumn } from "@admin/components/bz/BzTable";
import { BzTag } from "@admin/components/bz/BzTag";
import {
  ConfigManageDrawer,
  type ConfigManageMode,
} from "@admin/components/configs-admin/ConfigManageDrawer";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type {
  ConfigItem,
  ConfigManagementStatus,
  ConfigViolation,
  JsonObject,
} from "@admin/types/config-admin";
import type { PageResult } from "@admin/types/page";
import { useCallback, useEffect, useRef, useState } from "react";

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const EMPTY_PAGE: PageResult<ConfigItem> = {
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
};

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

export function ConfigsAdminPage() {
  const canUpdate = hasResourceCodeAccess("config-system-edit");
  const canBatchReset = hasResourceCodeAccess("config-system-batch-reset-default");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [moduleDraft, setModuleDraft] = useState("");
  const [groupDraft, setGroupDraft] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [appliedModule, setAppliedModule] = useState("");
  const [appliedGroup, setAppliedGroup] = useState("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [page, setPage] = useState<PageResult<ConfigItem>>(EMPTY_PAGE);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<ConfigManageMode>("detail");
  const [current, setCurrent] = useState<ConfigItem | null>(null);
  const [batchMode, setBatchMode] = useState(false);
  const [selectedItems, setSelectedItems] = useState<Record<string, number>>({});
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const pageNoRef = useRef(pageNo);
  const pageSizeRef = useRef(pageSize);
  const keywordRef = useRef(appliedKeyword);
  const moduleRef = useRef(appliedModule);
  const groupRef = useRef(appliedGroup);
  pageNoRef.current = pageNo;
  pageSizeRef.current = pageSize;
  keywordRef.current = appliedKeyword;
  moduleRef.current = appliedModule;
  groupRef.current = appliedGroup;

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      const pn = pageNoRef.current;
      const ps = pageSizeRef.current;
      let result = await listConfigs({
        keyword: keywordRef.current,
        module: moduleRef.current,
        group: groupRef.current,
        page: { pageNo: pn, pageSize: ps },
      });
      if (result.totalElements > 0 && pn > Math.max(1, result.totalPages)) {
        const lastPage = Math.max(1, result.totalPages);
        setPageNo(lastPage);
        result = await listConfigs({
          keyword: keywordRef.current,
          module: moduleRef.current,
          group: groupRef.current,
          page: { pageNo: lastPage, pageSize: ps },
        });
      }
      setPage(result);
      setPageNo(result.pageNo || 1);
      setPageSize(result.pageSize || ps);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [appliedGroup, appliedKeyword, appliedModule, pageNo, pageSize, reload]);

  function applyFilters() {
    setAppliedKeyword(keywordDraft.trim());
    setAppliedModule(moduleDraft.trim());
    setAppliedGroup(groupDraft.trim());
    setPageNo(1);
  }

  function resetFilters() {
    setKeywordDraft("");
    setModuleDraft("");
    setGroupDraft("");
    setAppliedKeyword("");
    setAppliedModule("");
    setAppliedGroup("");
    setPageNo(1);
  }

  async function openDrawer(item: ConfigItem, mode: ConfigManageMode) {
    setDrawerMode(mode);
    setCurrent(item);
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      setCurrent(await getConfig(item.key));
    } finally {
      setDrawerLoading(false);
    }
  }

  async function saveConfig(value: JsonObject, reason: string): Promise<ConfigViolation[]> {
    if (!current) return [];
    setSaving(true);
    try {
      const validation = await validateConfig(current.key, value);
      if (!validation.valid) {
        message.warning("配置校验未通过");
        return validation.violations;
      }
      const confirmed = await bzConfirm({
        title: "保存配置",
        content:
          current.activationPolicy === "RESTART_REQUIRED"
            ? "该配置保存后需要重启服务才会生效，确认保存吗？"
            : "确认保存当前配置吗？",
        confirmText: "保存",
        cancelText: "取消",
      });
      if (!confirmed) return [];
      const result = await updateConfig(current.key, {
        expectedRevision: current.persistedRevision,
        reason: reason || undefined,
        value,
      });
      message.success(result.pendingRestart ? "保存成功，配置将在重启后生效" : "保存成功");
      setDrawerOpen(false);
      await reload();
      return [];
    } finally {
      setSaving(false);
    }
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
    setSaving(true);
    try {
      const results = await batchResetConfigDefaults(items);
      const restartCount = results.filter((result) => result.pendingRestart).length;
      message.success(
        restartCount > 0
          ? `已重置 ${results.length} 项配置，其中 ${restartCount} 项需要重启后生效`
          : `已重置 ${results.length} 项配置`,
      );
      cancelBatchReset();
      await reload();
    } finally {
      setSaving(false);
    }
  }

  function getRowActions(row: ConfigItem): AdminActionItem[] {
    const actions: AdminActionItem[] = [
      { key: "detail", label: "详情", tone: "detail", handler: () => void openDrawer(row, "detail") },
    ];
    if (canUpdate && row.editPolicy === "ADMIN_EDITABLE") {
      actions.push({ key: "edit", label: "编辑", tone: "edit", handler: () => void openDrawer(row, "edit") });
    }
    return actions;
  }

  const selectableRows = page.elements.filter(isBatchSelectable);
  const allCurrentPageSelected =
    selectableRows.length > 0 && selectableRows.every((item) => item.key in selectedItems);
  const someCurrentPageSelected =
    !allCurrentPageSelected && selectableRows.some((item) => item.key in selectedItems);
  const columns: BzTableColumn<ConfigItem>[] = [
    ...(batchMode
      ? [
          {
            key: "select",
            title: "",
            width: 48,
            headerRender: () => (
              <input
                className="user-manage-checkbox"
                type="checkbox"
                checked={allCurrentPageSelected}
                disabled={!selectableRows.length}
                ref={(element) => {
                  if (element) element.indeterminate = someCurrentPageSelected;
                }}
                onChange={(event) => toggleCurrentPage(event.target.checked)}
              />
            ),
            render: (row: ConfigItem) => (
              <input
                className="user-manage-checkbox"
                type="checkbox"
                checked={row.key in selectedItems}
                disabled={!isBatchSelectable(row)}
                onChange={(event) => toggleSelection(row, event.target.checked)}
              />
            ),
          } as BzTableColumn<ConfigItem>,
        ]
      : []),
    {
      key: "key",
      title: "配置键",
      width: 320,
      render: (row) => <span className="configs-code-text">{row.key}</span>,
    },
    { key: "title", title: "配置名称", minWidth: 180, render: (row) => row.title },
    { key: "module", title: "模块", width: 120, render: (row) => row.module },
    { key: "group", title: "分组", width: 130, render: (row) => row.group },
    {
      key: "status",
      title: "状态",
      width: 130,
      render: (row) => <BzTag type={STATUS_META[row.status].type}>{STATUS_META[row.status].label}</BzTag>,
    },
    {
      key: "activationPolicy",
      title: "生效方式",
      width: 130,
      render: (row) => <BzTag type={row.activationPolicy === "DYNAMIC" ? "success" : "warning"}>{policyLabel(row)}</BzTag>,
    },
    { key: "revision", title: "版本", width: 90, render: (row) => row.persistedRevision },
  ];
  const actionsColumn = createAdminActionsColumn({ rows: page.elements, getActions: getRowActions });
  if (actionsColumn) columns.push(actionsColumn);

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
          <form ref={queryGridRef} className="bz-form admin-query-grid" onSubmit={(event) => { event.preventDefault(); applyFilters(); }}>
            <QueryField label="关键词"><BzInput modelValue={keywordDraft} placeholder="配置键、名称或说明" clearable onValueChange={setKeywordDraft} /></QueryField>
            <QueryField label="模块"><BzInput modelValue={moduleDraft} placeholder="例如 system" clearable onValueChange={setModuleDraft} /></QueryField>
            <QueryField label="分组"><BzInput modelValue={groupDraft} placeholder="例如 security" clearable onValueChange={setGroupDraft} /></QueryField>
            <div className="admin-query-actions">
              <BzButton className="admin-filter-secondary" onClick={resetFilters}>重置</BzButton>
              <BzButton className="admin-filter-primary" buttonType="primary" onClick={applyFilters}>搜索</BzButton>
              {!querySingleRow ? (
                <button className="admin-filter-toggle" type="button" aria-expanded={queryExpanded} onClick={() => setQueryExpanded((value) => !value)}>
                  <span>{queryExpanded ? "收起" : "展开"}</span>
                  <i className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`} />
                </button>
              ) : null}
            </div>
          </form>
        </div>
      }
      batchToolbar={
        batchMode ? (
          <div className="admin-batch-toolbar">
            <div className="admin-batch-toolbar__summary">批量重置默认值，已选 {Object.keys(selectedItems).length} 项</div>
            <div className="admin-batch-toolbar__actions">
              <BzButton buttonType="primary" loading={saving} disabled={!Object.keys(selectedItems).length} onClick={() => void confirmBatchReset()}>确认</BzButton>
              <BzButton disabled={saving} onClick={cancelBatchReset}>取消</BzButton>
            </div>
          </div>
        ) : null
      }
      businessActions={canBatchReset && !batchMode ? <BzButton onClick={beginBatchReset}>批量重置默认值</BzButton> : null}
      queryTools={
        !batchMode ? (
          <AdminTableTools
            queryPanelVisible={queryPanelVisible}
            onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
            onRefresh={() => void reload()}
          />
        ) : null
      }
      table={<BzTable data={page.elements} columns={columns} rowKey="key" loading={loading} />}
      footer={
        page.totalElements > 0 ? (
          <div className="dict-pagination-bar admin-list-table-footer">
            <div className="dict-pagination-summary">共 {page.totalElements} 条记录</div>
            <div className="dict-pagination-right">
              <BzPagination
                total={page.totalElements}
                pageSize={pageSize}
                currentPage={pageNo}
                pageSizes={PAGE_SIZE_OPTIONS}
                onCurrentChange={setPageNo}
                onSizeChange={(size) => { setPageSize(size); setPageNo(1); }}
              />
            </div>
          </div>
        ) : null
      }
      overlays={
        <ConfigManageDrawer
          open={drawerOpen}
          mode={drawerMode}
          item={current}
          loading={drawerLoading}
          saving={saving}
          canEdit={canUpdate}
          onClose={() => setDrawerOpen(false)}
          onSubmit={saveConfig}
        />
      }
    />
  );
}

function QueryField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <BzFormItem className="admin-query-field">
      <div className="admin-query-field__label">{label}</div>
      <div className="admin-query-field__control">{children}</div>
    </BzFormItem>
  );
}
