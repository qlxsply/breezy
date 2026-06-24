"use client";

import { listApis } from "@admin/api/apis";
import {
  createResource,
  deleteResource,
  getResourceApis,
  listResources,
  updateResource,
  updateResourceApis,
} from "@admin/api/resources";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { refreshRegistryLoaded } from "@admin/core/registry/bootstrap-registry";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { ApiEntry } from "@admin/types/api-admin";
import type {
  ResourceEntry,
  ResourceEntryCreate,
  ResourceEntryUpdate,
} from "@admin/types/resource-admin";
import { useCallback, useEffect, useMemo, useState } from "react";

import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzOption } from "../bz/BzOption";
import { BzSelect } from "../bz/BzSelect";
import { ResourceApiDialog } from "../resources-admin/ResourceApiDialog";
import { ResourceFormDialog } from "../resources-admin/ResourceFormDialog";
import { ResourceTable } from "../resources-admin/ResourceTable";

export function ResourcesAdminPage() {
  const [rows, setRows] = useState<ResourceEntry[]>([]);
  const [loading, setLoading] = useState(false);

  const [q, setQ] = useState("");
  const [type, setType] = useState("");
  const [scope, setScope] = useState("");
  const [level, setLevel] = useState("");

  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<"create" | "edit">("create");
  const [dialogModel, setDialogModel] = useState<ResourceEntry | null>(null);
  const [expandAll, setExpandAll] = useState(true);
  const [expandSignal, setExpandSignal] = useState(0);

  const [apiDialogOpen, setApiDialogOpen] = useState(false);
  const [apiTarget, setApiTarget] = useState<ResourceEntry | null>(null);
  const [apiList, setApiList] = useState<ApiEntry[]>([]);
  const [selectedApiIds, setSelectedApiIds] = useState<string[]>([]);
  const [apiLoading, setApiLoading] = useState(false);

  const canCreate = hasResourceCodeAccess("res.add");
  const canEdit = hasResourceCodeAccess("res.edit");
  const canDelete = hasResourceCodeAccess("res.del");
  const canApiView = hasResourceCodeAccess("res.api.view");
  const canApiEdit = hasResourceCodeAccess("res.api.edit");
  const canApi = canApiView || canApiEdit;

  const reload = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await listResources());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const filtered = useMemo(() => {
    const kw = q.trim().toLowerCase();
    const hasFilter = Boolean(kw || type || scope || level);
    if (!hasFilter) return rows;

    const byId = new Map(rows.map((r) => [r.id, r]));
    const matchedIds = new Set<string>();

    const matches = rows.filter((r) => {
      if (type && r.type !== type) return false;
      if (scope && r.scope !== scope) return false;
      if (level && r.level !== level) return false;
      if (!kw) return true;
      const name = r.name.toLowerCase();
      const code = r.code.toLowerCase();
      const url = (r.url ?? "").toLowerCase();
      const target = (r.loadTarget ?? "").toLowerCase();
      const desc = (r.description ?? "").toLowerCase();
      return (
        name.includes(kw) ||
        code.includes(kw) ||
        url.includes(kw) ||
        target.includes(kw) ||
        desc.includes(kw)
      );
    });

    const addWithAncestors = (row: ResourceEntry) => {
      let current: ResourceEntry | undefined = row;
      while (current) {
        if (matchedIds.has(current.id)) break;
        matchedIds.add(current.id);
        const pid = current.parentId ?? null;
        if (!pid) break;
        current = byId.get(pid);
      }
    };

    matches.forEach(addWithAncestors);
    return rows.filter((r) => matchedIds.has(r.id));
  }, [rows, q, type, scope, level]);

  function openCreate() {
    if (!canCreate) return;
    setDialogMode("create");
    setDialogModel({
      id: "",
      parentId: "",
      name: "",
      icon: "",
      description: "",
      code: "",
      type: "MENU",
      scope: "SETTING",
      openMode: "PAGE",
      url: "",
      loadTarget: "",
      orderNo: 100,
      level: "CUSTOM",
      enabled: true,
      guestAccess: false,
    });
    setDialogOpen(true);
  }

  function openEdit(row: ResourceEntry) {
    if (!canEdit) return;
    if (row.level === "SYSTEM") return;
    setDialogMode("edit");
    setDialogModel({ ...row });
    setDialogOpen(true);
  }

  async function onRemove(row: ResourceEntry) {
    if (!canDelete) return;
    if (row.level === "SYSTEM") return;
    const confirmed = await bzConfirm({
      title: "删除资源",
      content: `确认删除：${row.name} (${row.code}) ?`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteResource(row.id);
    message.success("删除成功");
    await reload();
  }

  async function openApi(row: ResourceEntry) {
    if (!canApi) return;
    setApiTarget(row);
    setApiDialogOpen(true);
    setApiLoading(true);
    try {
      if (apiList.length === 0) {
        setApiList(await listApis());
      }
      setSelectedApiIds(await getResourceApis(row.id));
    } finally {
      setApiLoading(false);
    }
  }

  async function onApiSubmit(ids: string[]) {
    if (!apiTarget) return;
    if (!canApiEdit) return;
    await updateResourceApis(apiTarget.id, ids);
    setApiDialogOpen(false);
    await reload();
  }

  async function onSubmit(model: ResourceEntry) {
    if (dialogMode === "create") {
      const req: ResourceEntryCreate = { ...model, level: "CUSTOM" };
      await createResource(req);
    } else {
      const req: ResourceEntryUpdate = model;
      await updateResource(model.id, req);
    }
    setDialogOpen(false);
    await reload();
  }

  function expandAllRows() {
    setExpandAll(true);
    setExpandSignal((s) => s + 1);
  }

  function collapseAllRows() {
    setExpandAll(false);
    setExpandSignal((s) => s + 1);
  }

  async function onRefreshPermissions() {
    await refreshRegistryLoaded();
  }

  function applyFilters() {
    expandAllRows();
  }

  function resetFilters() {
    setQ("");
    setType("");
    setScope("");
    setLevel("");
    expandAllRows();
  }

  return (
    <div className="app-shell">
      <div className="content">
        <div className="list-page-stack">
          <section className="list-page-actions">
            <div className="list-page-actions-main">
              {canCreate ? (
                <BzButton
                  buttonType="primary"
                  onClick={openCreate}
                >
                  新增
                </BzButton>
              ) : null}
              <BzButton onClick={onRefreshPermissions}>刷新权限</BzButton>
              <BzButton onClick={reload}>刷新</BzButton>
              <BzButton onClick={expandAllRows}>全部展开</BzButton>
              <BzButton onClick={collapseAllRows}>全部折叠</BzButton>
            </div>
          </section>

          <BzCard
            className="list-page-query-card"
            shadow="never"
          >
            <BzForm
              className="list-page-filter-form"
              inline={true}
            >
              <BzFormItem className="list-page-filter-item">
                <div className="list-page-filter-field">
                  <div className="list-page-filter-label">关键词</div>
                  <BzInput
                    modelValue={q}
                    className="list-page-filter-control"
                    placeholder="按名称/编码/URL 搜索"
                    clearable
                    onValueChange={setQ}
                    onKeyUp={(event) => {
                      if (event.key === "Enter") applyFilters();
                    }}
                  />
                </div>
              </BzFormItem>

              <BzFormItem className="list-page-filter-item">
                <div className="list-page-filter-field">
                  <div className="list-page-filter-label">类型</div>
                  <BzSelect
                    modelValue={type}
                    className="list-page-filter-control"
                    placeholder="全部类型"
                    clearable
                    onValueChange={(v) => setType(v ?? "")}
                  >
                    <BzOption
                      label="菜单"
                      value="MENU"
                    />
                    <BzOption
                      label="按钮"
                      value="BUTTON"
                    />
                    <BzOption
                      label="功能"
                      value="FEATURE"
                    />
                    <BzOption
                      label="数据"
                      value="DATA"
                    />
                  </BzSelect>
                </div>
              </BzFormItem>

              <BzFormItem className="list-page-filter-item">
                <div className="list-page-filter-field">
                  <div className="list-page-filter-label">入口</div>
                  <BzSelect
                    modelValue={scope}
                    className="list-page-filter-control"
                    placeholder="全部入口"
                    clearable
                    onValueChange={(v) => setScope(v ?? "")}
                  >
                    <BzOption
                      label="工具入口"
                      value="TOOL"
                    />
                    <BzOption
                      label="设置入口"
                      value="SETTING"
                    />
                    <BzOption
                      label="信息入口"
                      value="INFO"
                    />
                    <BzOption
                      label="非入口"
                      value="NONE"
                    />
                  </BzSelect>
                </div>
              </BzFormItem>

              <BzFormItem className="list-page-filter-item">
                <div className="list-page-filter-field">
                  <div className="list-page-filter-label">级别</div>
                  <BzSelect
                    modelValue={level}
                    className="list-page-filter-control"
                    placeholder="全部级别"
                    clearable
                    onValueChange={(v) => setLevel(v ?? "")}
                  >
                    <BzOption
                      label="系统"
                      value="SYSTEM"
                    />
                    <BzOption
                      label="自定义"
                      value="CUSTOM"
                    />
                  </BzSelect>
                </div>
              </BzFormItem>

              <BzFormItem className="list-page-filter-actions">
                <BzButton
                  buttonType="primary"
                  onClick={applyFilters}
                >
                  搜索
                </BzButton>
                <BzButton onClick={resetFilters}>重置</BzButton>
              </BzFormItem>
            </BzForm>
          </BzCard>

          <BzCard className="list-page-result-card">
            <ResourceTable
              rows={filtered}
              loading={loading}
              expandAll={expandAll}
              expandSignal={expandSignal}
              canEdit={canEdit}
              canDelete={canDelete}
              canApi={canApi}
              onEdit={openEdit}
              onRemove={onRemove}
              onApi={openApi}
            />
          </BzCard>
        </div>

        {dialogOpen ? (
          <ResourceFormDialog
            mode={dialogMode}
            model={dialogModel}
            resources={rows}
            onClose={() => setDialogOpen(false)}
            onSubmit={onSubmit}
          />
        ) : null}

        {apiDialogOpen ? (
          <ResourceApiDialog
            resourceName={apiTarget?.name || ""}
            apis={apiList}
            selectedIds={selectedApiIds}
            loading={apiLoading}
            canSave={canApiEdit}
            onClose={() => setApiDialogOpen(false)}
            onSubmit={onApiSubmit}
          />
        ) : null}
      </div>
    </div>
  );
}
