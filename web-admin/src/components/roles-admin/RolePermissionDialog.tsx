import { useEffect, useMemo, useRef, useState } from "react";

import { formatDateTime } from "../../core/formatter";
import type { RoleEntry, RoleGrantResourceEntry, RoleGrantSelection } from "../../types/role-admin";
import { AdminEntityDrawer } from "../admin/AdminEntityDrawer";
import { BzAlert } from "../bz/BzAlert";
import { BzButton } from "../bz/BzButton";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzSwitch } from "../bz/BzSwitch";
import { RolePermissionTreeNode, type RolePermissionTreeNodeView } from "./RolePermissionTreeNode";

type DiffStatus = "added" | "removed";

interface RolePermissionDialogProps {
  mode: "detail" | "edit";
  role: RoleEntry | null;
  resources: RoleGrantResourceEntry[];
  selection: RoleGrantSelection;
  loading?: boolean;
  canEditBasic?: boolean;
  canViewPermissions?: boolean;
  canEditPermissions?: boolean;
  onClose: () => void;
  onSubmit: (payload: {
    role: RoleEntry;
    selection: RoleGrantSelection;
    permissionChanged: boolean;
  }) => void;
}

export function RolePermissionDialog({
  mode,
  role,
  resources,
  selection,
  loading = false,
  canEditBasic = false,
  canViewPermissions = false,
  canEditPermissions = false,
  onClose,
  onSubmit,
}: RolePermissionDialogProps) {
  const [form, setForm] = useState<RoleEntry>({ id: "", code: "", name: "", enabled: true });
  const [keyword, setKeyword] = useState("");
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [confirming, setConfirming] = useState(false);
  const [pendingSelection, setPendingSelection] = useState<RoleGrantSelection | null>(null);
  const [selectedResourceIds, setSelectedResourceIds] = useState<Set<string>>(new Set());
  const [error, setError] = useState("");
  const treeWrapRef = useRef<HTMLDivElement | null>(null);
  const editable = mode === "edit" && (canEditBasic || canEditPermissions);
  const permissionEditable = mode === "edit" && canEditPermissions;
  const showPermissionSection = canViewPermissions || canEditPermissions;

  const resourceMap = useMemo(() => {
    const map = new Map<string, RoleGrantResourceEntry>();
    resources.forEach((row) => map.set(row.id, row));
    return map;
  }, [resources]);

  const childrenMap = useMemo(() => {
    const map = new Map<string | null, RoleGrantResourceEntry[]>();
    resources.forEach((row) => {
      const list = map.get(row.parentId ?? null) ?? [];
      list.push(row);
      list.sort((left, right) => left.orderNo - right.orderNo || left.name.localeCompare(right.name));
      map.set(row.parentId ?? null, list);
    });
    return map;
  }, [resources]);

  const defaultExpandedIds = useMemo(() => {
    const ids = new Set<string>();
    resources.forEach((row) => {
      if ((childrenMap.get(row.id) ?? []).length > 0) ids.add(row.id);
    });
    return ids;
  }, [childrenMap, resources]);

  useEffect(() => {
    if (role) {
      setForm({ ...role });
    }
    setSelectedResourceIds(buildSelectedResourceIds(selection));
    setExpandedIds(new Set(defaultExpandedIds));
    setConfirming(false);
    setPendingSelection(null);
    setError("");
  }, [defaultExpandedIds, role, selection, resources]);

  useEffect(() => {
    if (expandedIds.size === 0) {
      treeWrapRef.current?.scrollTo({ top: 0, behavior: "auto" });
    }
  }, [expandedIds]);

  const keywordText = keyword.trim().toLowerCase();

  const filteredTreeResult = useMemo(() => {
    const autoExpandedIds = new Set<string>();
    const roots = buildTree(null, keywordText, autoExpandedIds);
    return { roots, autoExpandedIds };
  }, [childrenMap, keywordText, resourceMap]);

  const filteredRoots = filteredTreeResult.roots;

  const displayExpandedIds = useMemo(() => {
    if (!keywordText) return expandedIds;
    return new Set([...expandedIds, ...filteredTreeResult.autoExpandedIds]);
  }, [expandedIds, keywordText, filteredTreeResult]);

  const filteredResourceCount = useMemo(() => {
    let count = 0;
    walkTree(filteredRoots, () => {
      count += 1;
    });
    return count;
  }, [filteredRoots]);

  const summarySelection = useMemo(
    () => buildSubmitSelection(selectedResourceIds),
    [selectedResourceIds, resources],
  );

  const summaryText = useMemo(() => {
    const resourceCount = summarySelection.resourceIds.length;
    if (resourceCount === 0) return "未选择权限";
    return `已选择资源 ${resourceCount} 项`;
  }, [summarySelection]);

  const diff = useMemo(
    () => buildSelectionDiff(selection, pendingSelection ?? summarySelection),
    [selection, pendingSelection, summarySelection],
  );

  const diffStatusById = useMemo(() => {
    const map = new Map<string, DiffStatus>();
    resources.forEach((row) => {
      if (diff.addedResourceIds.has(row.resourceId)) map.set(row.id, "added");
      else if (diff.removedResourceIds.has(row.resourceId)) map.set(row.id, "removed");
    });
    return map;
  }, [resources, diff]);

  const diffVisibleNodeIds = useMemo(() => {
    const ids = new Set<string>();
    buildNodeIdsFromSelection(selection).forEach((id) => ids.add(id));
    buildNodeIdsFromSelection(pendingSelection ?? summarySelection).forEach((id) => ids.add(id));
    return ids;
  }, [selection, pendingSelection, summarySelection]);

  const diffRoots = useMemo(() => buildDiffTree(null), [childrenMap, diffVisibleNodeIds]);

  const diffExpandedIds = useMemo(() => {
    const ids = new Set<string>();
    walkTree(diffRoots, (node) => {
      if (node.children.length > 0) ids.add(node.row.id);
    });
    return ids;
  }, [diffRoots]);

  const diffSelectedNodeIds = diffVisibleNodeIds;

  const diffSummaryText = useMemo(() => {
    const addedCount = diff.addedResourceIds.size;
    const removedCount = diff.removedResourceIds.size;
    return `新增 ${addedCount} 项 / 移除 ${removedCount} 项`;
  }, [diff]);

  function buildSelectedResourceIds(currentSelection: RoleGrantSelection): Set<string> {
    const next = new Set<string>();
    const resourceIds = new Set((currentSelection.resourceIds || []).map(String));

    resources.forEach((row) => {
      if (resourceIds.has(row.resourceId)) next.add(row.id);
    });

    Array.from(next).forEach((id) => {
      collectAncestorIds(id).forEach((ancestorId) => next.add(ancestorId));
    });
    return next;
  }

  function buildTree(
    parentId: string | null,
    kw: string,
    autoExpanded: Set<string>,
  ): RolePermissionTreeNodeView[] {
    const rows = childrenMap.get(parentId) ?? [];
    return rows
      .map((row) => {
        const children = buildTree(row.id, kw, autoExpanded);
        const matched =
          !kw ||
          [row.name, row.code, row.description, row.type]
            .filter(Boolean)
            .join(" ")
            .toLowerCase()
            .includes(kw);
        if (!matched && children.length === 0) return null;
        if (kw && children.length > 0) autoExpanded.add(row.id);
        return { row, children } satisfies RolePermissionTreeNodeView;
      })
      .filter((item): item is RolePermissionTreeNodeView => Boolean(item));
  }

  function buildDiffTree(parentId: string | null): RolePermissionTreeNodeView[] {
    const rows = childrenMap.get(parentId) ?? [];
    return rows
      .map((row) => {
        const children = buildDiffTree(row.id);
        const visible = diffVisibleNodeIds.has(row.id);
        if (!visible && children.length === 0) return null;
        return { row, children } satisfies RolePermissionTreeNodeView;
      })
      .filter((item): item is RolePermissionTreeNodeView => Boolean(item));
  }

  function walkTree(
    nodes: RolePermissionTreeNodeView[],
    handler: (node: RolePermissionTreeNodeView) => void,
  ) {
    for (const node of nodes) {
      handler(node);
      if (node.children.length > 0) walkTree(node.children, handler);
    }
  }

  function collectAncestorIds(nodeId: string): string[] {
    const result: string[] = [];
    let current = resourceMap.get(nodeId)?.parentId ?? null;
    while (current) {
      result.push(current);
      current = resourceMap.get(current)?.parentId ?? null;
    }
    return result;
  }

  function collectDescendantIds(nodeId: string): string[] {
    const result: string[] = [];
    (childrenMap.get(nodeId) ?? []).forEach((child) => {
      result.push(child.id);
      result.push(...collectDescendantIds(child.id));
    });
    return result;
  }

  function collectExpandableIds(): Set<string> {
    const ids = new Set<string>();
    resources.forEach((row) => {
      if ((childrenMap.get(row.id) ?? []).length > 0) ids.add(row.id);
    });
    return ids;
  }

  function hasSelectedDescendant(nodeId: string, currentIds: Set<string>): boolean {
    return collectDescendantIds(nodeId).some((childId) => currentIds.has(childId));
  }

  function pruneEmptyAncestors(nodeId: string, currentIds: Set<string>) {
    let current = resourceMap.get(nodeId)?.parentId ?? null;
    while (current) {
      if (!hasSelectedDescendant(current, currentIds)) currentIds.delete(current);
      current = resourceMap.get(current)?.parentId ?? null;
    }
  }

  function toggleExpand(nodeId: string) {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(nodeId)) next.delete(nodeId);
      else next.add(nodeId);
      return next;
    });
  }

  function expandAll() {
    setExpandedIds(collectExpandableIds());
  }

  function collapseAll() {
    setExpandedIds(new Set());
    treeWrapRef.current?.scrollTo({ top: 0, behavior: "auto" });
  }

  function clearAll() {
    if (!permissionEditable) return;
    setSelectedResourceIds(new Set());
  }

  function toggleSelect(nodeId: string, checked: boolean) {
    if (!permissionEditable) return;
    const row = resourceMap.get(nodeId);
    if (!row || !row.enabled) return;

    setSelectedResourceIds((prev) => {
      const next = new Set(prev);
      const descendants = collectDescendantIds(nodeId);

      if (checked) {
        next.add(nodeId);
        collectAncestorIds(nodeId).forEach((ancestorId) => next.add(ancestorId));
        descendants.forEach((descendantId) => {
          const descendant = resourceMap.get(descendantId);
          if (descendant?.enabled) next.add(descendantId);
        });
        return next;
      }

      next.delete(nodeId);
      descendants.forEach((descendantId) => next.delete(descendantId));
      pruneEmptyAncestors(nodeId, next);
      return next;
    });
  }

  function buildSubmitSelection(currentIds: Set<string>): RoleGrantSelection {
    const resourceIds = new Set<string>();
    resources.forEach((row) => {
      if (currentIds.has(row.id) && row.selectable) resourceIds.add(row.resourceId);
    });
    return { resourceIds: Array.from(resourceIds) };
  }

  function buildSelectionDiff(before: RoleGrantSelection, after: RoleGrantSelection) {
    const beforeResourceIds = new Set((before.resourceIds || []).map(String));
    const afterResourceIds = new Set((after.resourceIds || []).map(String));
    const addedResourceIds = new Set([...afterResourceIds].filter((id) => !beforeResourceIds.has(id)));
    const removedResourceIds = new Set([...beforeResourceIds].filter((id) => !afterResourceIds.has(id)));
    return {
      addedResourceIds,
      removedResourceIds,
      changed: addedResourceIds.size > 0 || removedResourceIds.size > 0,
    };
  }

  function buildNodeIdsFromSelection(currentSelection: RoleGrantSelection): Set<string> {
    return buildSelectedResourceIds(currentSelection);
  }

  function handleSubmit() {
    if (!role || !editable) return;

    setError("");
    if (canEditBasic) {
      if (!form.code.trim()) {
        setError("编码不能为空");
        return;
      }
      if (!form.name.trim()) {
        setError("名称不能为空");
        return;
      }
      if (!/^[a-zA-Z0-9_-]+$/.test(form.code.trim())) {
        setError("编码建议仅包含字母/数字/_/-");
        return;
      }
    }

    const nextSelection = buildSubmitSelection(selectedResourceIds);
    const nextDiff = buildSelectionDiff(selection, nextSelection);
    if (!nextDiff.changed) {
      onSubmit({
        role: { ...form, code: form.code.trim(), name: form.name.trim() },
        selection: nextSelection,
        permissionChanged: false,
      });
      return;
    }
    setPendingSelection(nextSelection);
    setConfirming(true);
  }

  function backToEdit() {
    setConfirming(false);
  }

  function confirmSubmit() {
    if (!pendingSelection) return;
    onSubmit({
      role: { ...form, code: form.code.trim(), name: form.name.trim() },
      selection: pendingSelection,
      permissionChanged: true,
    });
  }

  const footer = (
    <div className="permission-dialog-footer">
      <div className="permission-dialog-footer__summary">
        {confirming
          ? "确认保存后，受影响用户需要重新登录后权限才会完全生效。"
          : showPermissionSection
            ? summaryText
            : ""}
      </div>
      <div className="permission-dialog-footer__actions">
        {!confirming ? (
          <>
            <BzButton onClick={onClose}>{editable ? "取消" : "关闭"}</BzButton>
            {editable ? (
              <BzButton buttonType="primary" onClick={handleSubmit}>
                保存
              </BzButton>
            ) : null}
          </>
        ) : (
          <>
            <BzButton onClick={backToEdit}>返回</BzButton>
            <BzButton buttonType="primary" onClick={confirmSubmit}>
              确认
            </BzButton>
          </>
        )}
      </div>
    </div>
  );

  return (
    <AdminEntityDrawer
      open
      title={confirming ? "确认权限变更" : mode === "detail" ? "角色详情" : "编辑角色"}
      width="1120px"
      loading={loading}
      onClose={onClose}
      footer={footer}
    >
      <section className="permission-panel role-manage-section">
        <div className="permission-panel__head">
          <div>
            <div className="permission-panel__title">基础信息</div>
          </div>
        </div>

        <BzForm>
          <div className="role-manage-form-grid">
            <BzFormItem
              label={
                canEditBasic && mode === "edit" ? (
                  <>
                    编码<span className="form-required-mark">*</span>
                  </>
                ) : (
                  "编码"
                )
              }
            >
              <BzInput
                modelValue={form.code}
                className="mono"
                placeholder="请输入角色编码"
                disabled={!canEditBasic || mode === "detail"}
                readOnly={!canEditBasic || mode === "detail"}
                onValueChange={(value) => setForm((current) => ({ ...current, code: value }))}
              />
            </BzFormItem>
            <BzFormItem
              label={
                canEditBasic && mode === "edit" ? (
                  <>
                    名称<span className="form-required-mark">*</span>
                  </>
                ) : (
                  "名称"
                )
              }
            >
              <BzInput
                modelValue={form.name}
                placeholder="请输入角色名称"
                disabled={!canEditBasic || mode === "detail"}
                readOnly={!canEditBasic || mode === "detail"}
                onValueChange={(value) => setForm((current) => ({ ...current, name: value }))}
              />
            </BzFormItem>
            <BzFormItem label="状态">
              <BzSwitch
                modelValue={form.enabled}
                activeText="启用"
                inactiveText="停用"
                disabled={!canEditBasic || mode === "detail"}
                onValueChange={(value) => setForm((current) => ({ ...current, enabled: value }))}
              />
            </BzFormItem>
            <BzFormItem label="创建人">
              <BzInput modelValue={role?.createdBy || "-"} disabled readOnly />
            </BzFormItem>
            <BzFormItem label="创建时间">
              <BzInput modelValue={formatDateTime(role?.createdAt)} disabled readOnly />
            </BzFormItem>
            <BzFormItem label="更新时间">
              <BzInput modelValue={formatDateTime(role?.updatedAt)} disabled readOnly />
            </BzFormItem>
          </div>
        </BzForm>

        {error ? <BzAlert title={error} type="error" showIcon className="form-error" /> : null}
      </section>

      {showPermissionSection ? !confirming ? (
        <div className="permission-dialog-shell">
          <div className="permission-dialog-toolbar">
            <BzInput
              modelValue={keyword}
              placeholder="搜索资源名称/编码/类型"
              clearable
              onValueChange={setKeyword}
            />
            <BzButton className="permission-toolbar-button" onClick={expandAll}>
              全部展开
            </BzButton>
            <BzButton className="permission-toolbar-button" onClick={collapseAll}>
              全部收起
            </BzButton>
            {permissionEditable ? (
              <BzButton className="permission-toolbar-button" onClick={clearAll}>
                清空选择
              </BzButton>
            ) : null}
          </div>

          <section className="permission-panel permission-panel--drawer">
            <div className="permission-panel__head">
              <div>
                <div className="permission-panel__title">权限内容</div>
              </div>
              <div className="permission-panel__meta">
                {filteredResourceCount} / {resources.length} 项
              </div>
            </div>

            <div className="permission-tree-wrap" ref={treeWrapRef}>
              {filteredRoots.length === 0 ? (
                <div className="permission-empty">暂无可授权资源</div>
              ) : null}
              {filteredRoots.map((node) => (
                <RolePermissionTreeNode
                  key={node.row.id}
                  node={node}
                  expandedIds={displayExpandedIds}
                  selectedIds={selectedResourceIds}
                  canEdit={permissionEditable}
                  onToggleExpand={toggleExpand}
                  onToggleSelect={(payload) => toggleSelect(payload.id, payload.checked)}
                  onToggleButton={(payload) => toggleSelect(payload.id, payload.checked)}
                />
              ))}
            </div>
          </section>
        </div>
      ) : (
        <div className="permission-dialog-shell">
          <section className="permission-panel permission-panel--drawer">
            <div className="permission-panel__head">
              <div>
                <div className="permission-panel__title">确认权限变更</div>
                <div className="permission-panel__hint">
                  绿色边框表示新增权限；红色删除线表示移除权限；未变化节点仅用于展示层级路径。
                </div>
              </div>
              <div className="permission-panel__meta">{diffSummaryText}</div>
            </div>

            <div className="permission-tree-wrap">
              {diffRoots.length === 0 ? (
                <div className="permission-empty">权限未发生变更</div>
              ) : (
                diffRoots.map((node) => (
                  <RolePermissionTreeNode
                    key={node.row.id}
                    node={node}
                    expandedIds={diffExpandedIds}
                    selectedIds={diffSelectedNodeIds}
                    canEdit={false}
                    readonly
                    diffStatusById={diffStatusById}
                    onToggleExpand={toggleExpand}
                    onToggleSelect={() => {}}
                    onToggleButton={() => {}}
                  />
                ))
              )}
            </div>
          </section>
        </div>
      ) : null}
    </AdminEntityDrawer>
  );
}
