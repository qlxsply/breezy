import { useEffect, useMemo, useRef, useState } from "react";

import type { RoleGrantResourceEntry, RoleGrantSelection } from "../../types/role-admin";
import { BzButton } from "../bz/BzButton";
import { BzDialog } from "../bz/BzDialog";
import { BzInput } from "../bz/BzInput";
import { BzLoading } from "../bz/BzLoading";
import { RolePermissionTreeNode, type RolePermissionTreeNodeView } from "./RolePermissionTreeNode";

type DiffStatus = "added" | "removed";

interface RolePermissionDialogProps {
  roleName: string;
  resources: RoleGrantResourceEntry[];
  selection: RoleGrantSelection;
  loading?: boolean;
  canSave?: boolean;
  onClose: () => void;
  onSubmit: (selection: RoleGrantSelection) => void;
}

export function RolePermissionDialog({ roleName, resources, selection, loading = false, canSave = true, onClose, onSubmit }: RolePermissionDialogProps) {
  const [keyword, setKeyword] = useState("");
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [confirming, setConfirming] = useState(false);
  const [pendingSelection, setPendingSelection] = useState<RoleGrantSelection | null>(null);
  const [selectedMenuNodeIds, setSelectedMenuNodeIds] = useState<Set<string>>(new Set());
  const [selectedFunctionNodeIds, setSelectedFunctionNodeIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    const ids = buildSelectedNodeIdSets();
    setSelectedMenuNodeIds(ids.menuNodeIds);
    setSelectedFunctionNodeIds(ids.functionNodeIds);
    setExpandedIds(new Set());
    setConfirming(false);
    setPendingSelection(null);
  }, [selection, resources]);

  function buildSelectedNodeIdSets(): { menuNodeIds: Set<string>; functionNodeIds: Set<string> } {
    const menuNodeIds = new Set<string>();
    const functionNodeIds = new Set<string>();
    const menuIdSet = new Set((selection.menuIds || []).map(String));
    const functionIdSet = new Set((selection.functionIds || []).map(String));

    resources.forEach((r) => {
      if (r.functionId && functionIdSet.has(r.functionId)) functionNodeIds.add(r.id);
    });

    const functionAncestorMenuNodeIds = new Set<string>();
    functionNodeIds.forEach((fid) => {
      collectAncestorIds(fid).forEach((aid) => {
        const a = resourceMap.get(aid);
        if (a?.type === "MENU") functionAncestorMenuNodeIds.add(aid);
      });
    });

    resources.forEach((r) => {
      if (r.menuId && menuIdSet.has(r.menuId) && r.type === "MENU" && !functionAncestorMenuNodeIds.has(r.id)) {
        menuNodeIds.add(r.id);
      }
    });

    return { menuNodeIds, functionNodeIds };
  }

  const resourceMap = useMemo(() => {
    const map = new Map<string, RoleGrantResourceEntry>();
    resources.forEach((r) => map.set(r.id, r));
    return map;
  }, [resources]);

  const childrenMap = useMemo(() => {
    const map = new Map<string | null, RoleGrantResourceEntry[]>();
    resources.forEach((row) => {
      const list = map.get(row.parentId ?? null) ?? [];
      list.push(row);
      list.sort((a, b) => a.orderNo - b.orderNo || a.name.localeCompare(b.name));
      map.set(row.parentId ?? null, list);
    });
    return map;
  }, [resources]);

  const selectedNodeIds = useMemo(() => {
    const result = new Set<string>();
    selectedMenuNodeIds.forEach((id) => result.add(id));
    selectedFunctionNodeIds.forEach((id) => {
      result.add(id);
      collectAncestorIds(id).forEach((aid) => {
        if (resourceMap.get(aid)?.type === "MENU") result.add(aid);
      });
    });
    return result;
  }, [selectedMenuNodeIds, selectedFunctionNodeIds, resourceMap]);

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
    walkTree(filteredRoots, () => { count += 1; });
    return count;
  }, [filteredRoots]);

  const summarySelection = useMemo(() => buildSubmitSelection(), [selectedNodeIds, selectedFunctionNodeIds, resources]);

  const summaryText = useMemo(() => {
    const menuCount = summarySelection.menuIds.length;
    const functionCount = summarySelection.functionIds.length;
    if (menuCount === 0 && functionCount === 0) return "未选择权限";
    return `已选择菜单 ${menuCount} 项，按钮权限 ${functionCount} 项`;
  }, [summarySelection]);

  const diff = useMemo(() => buildSelectionDiff(selection, pendingSelection ?? summarySelection), [selection, pendingSelection, summarySelection]);

  const diffStatusById = useMemo(() => {
    const map = new Map<string, DiffStatus>();
    resources.forEach((r) => {
      if (r.type === "MENU" && r.menuId) {
        if (diff.addedMenuIds.has(r.menuId)) map.set(r.id, "added");
        else if (diff.removedMenuIds.has(r.menuId)) map.set(r.id, "removed");
      }
      if (r.type === "BUTTON" && r.functionId) {
        if (diff.addedFunctionIds.has(r.functionId)) map.set(r.id, "added");
        else if (diff.removedFunctionIds.has(r.functionId)) map.set(r.id, "removed");
      }
    });
    return map;
  }, [resources, diff]);

  const diffRoots = useMemo(() => buildDiffTree(null), [childrenMap, diffStatusById]);

  const diffExpandedIds = useMemo(() => {
    const ids = new Set<string>();
    walkTree(diffRoots, (node) => {
      if (node.children.some((child) => child.row.type !== "BUTTON")) ids.add(node.row.id);
    });
    return ids;
  }, [diffRoots]);

  const diffSelectedNodeIds = useMemo(() => {
    const ids = new Set<string>();
    buildNodeIdsFromSelection(selection).forEach((id) => ids.add(id));
    buildNodeIdsFromSelection(pendingSelection ?? summarySelection).forEach((id) => ids.add(id));
    return ids;
  }, [selection, pendingSelection, summarySelection]);

  const diffSummaryText = useMemo(() => {
    const addedCount = diff.addedMenuIds.size + diff.addedFunctionIds.size;
    const removedCount = diff.removedMenuIds.size + diff.removedFunctionIds.size;
    return `新增 ${addedCount} 项 / 移除 ${removedCount} 项`;
  }, [diff]);

  function buildTree(parentId: string | null, kw: string, autoExpanded: Set<string>): RolePermissionTreeNodeView[] {
    const rows = childrenMap.get(parentId) ?? [];
    return rows
      .map((row) => {
        const children = buildTree(row.id, kw, autoExpanded);
        const matched = !kw || [row.name, row.code, row.description, ...row.permissionCodes].filter(Boolean).join(" ").toLowerCase().includes(kw);
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
        const changed = diffStatusById.has(row.id);
        if (!changed && children.length === 0) return null;
        return { row, children } satisfies RolePermissionTreeNodeView;
      })
      .filter((item): item is RolePermissionTreeNodeView => Boolean(item));
  }

  function walkTree(nodes: RolePermissionTreeNodeView[], handler: (node: RolePermissionTreeNodeView) => void) {
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

  function collectExpandableIds(): Set<string> {
    const ids = new Set<string>();
    resources.forEach((row) => {
      const nested = (childrenMap.get(row.id) ?? []).filter((child) => child.type !== "BUTTON");
      if (nested.length > 0) ids.add(row.id);
    });
    return ids;
  }

  function collectDescendantButtonIds(nodeId: string): string[] {
    const result: string[] = [];
    (childrenMap.get(nodeId) ?? []).forEach((child) => {
      if (child.type === "BUTTON") result.push(child.id);
      result.push(...collectDescendantButtonIds(child.id));
    });
    return result;
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
  }

  function clearAll() {
    setSelectedMenuNodeIds(new Set());
    setSelectedFunctionNodeIds(new Set());
  }

  function toggleSelect(nodeId: string, checked: boolean) {
    if (canSave === false) return;
    const row = resourceMap.get(nodeId);
    if (!row || !row.enabled || row.type === "DIRECTORY") return;

    if (row.type === "BUTTON") {
      setSelectedFunctionNodeIds((prev) => {
        const next = new Set(prev);
        if (checked) next.add(nodeId);
        else next.delete(nodeId);
        return next;
      });
      return;
    }

    if (row.type === "MENU") {
      setSelectedMenuNodeIds((prev) => {
        const next = new Set(prev);
        if (checked) next.add(nodeId);
        else next.delete(nodeId);
        return next;
      });
      if (!checked) {
        setSelectedFunctionNodeIds((prev) => {
          const next = new Set(prev);
          collectDescendantButtonIds(nodeId).forEach((id) => next.delete(id));
          return next;
        });
      }
    }
  }

  function buildSubmitSelection(): RoleGrantSelection {
    const menuIds = new Set<string>();
    const functionIds = new Set<string>();
    resources.forEach((r) => {
      if (r.menuId && selectedNodeIds.has(r.id)) menuIds.add(r.menuId);
      if (r.functionId && selectedFunctionNodeIds.has(r.id)) functionIds.add(r.functionId);
    });
    return { menuIds: Array.from(menuIds), functionIds: Array.from(functionIds) };
  }

  function buildSelectionDiff(before: RoleGrantSelection, after: RoleGrantSelection) {
    const beforeMenuIds = new Set((before.menuIds || []).map(String));
    const afterMenuIds = new Set((after.menuIds || []).map(String));
    const beforeFunctionIds = new Set((before.functionIds || []).map(String));
    const afterFunctionIds = new Set((after.functionIds || []).map(String));
    const addedMenuIds = new Set([...afterMenuIds].filter((id) => !beforeMenuIds.has(id)));
    const removedMenuIds = new Set([...beforeMenuIds].filter((id) => !afterMenuIds.has(id)));
    const addedFunctionIds = new Set([...afterFunctionIds].filter((id) => !beforeFunctionIds.has(id)));
    const removedFunctionIds = new Set([...beforeFunctionIds].filter((id) => !afterFunctionIds.has(id)));
    return {
      addedMenuIds, removedMenuIds, addedFunctionIds, removedFunctionIds,
      changed: addedMenuIds.size > 0 || removedMenuIds.size > 0 || addedFunctionIds.size > 0 || removedFunctionIds.size > 0,
    };
  }

  function buildNodeIdsFromSelection(s: RoleGrantSelection): Set<string> {
    const ids = new Set<string>();
    const menuIds = new Set((s.menuIds || []).map(String));
    const functionIds = new Set((s.functionIds || []).map(String));
    resources.forEach((r) => {
      if (r.menuId && menuIds.has(r.menuId)) ids.add(r.id);
      if (r.functionId && functionIds.has(r.functionId)) ids.add(r.id);
    });
    return ids;
  }

  function handleSubmit() {
    if (canSave === false) return;
    const nextSelection = buildSubmitSelection();
    const nextDiff = buildSelectionDiff(selection, nextSelection);
    if (!nextDiff.changed) {
      onSubmit(nextSelection);
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
    onSubmit(pendingSelection);
  }

  return (
    <BzDialog
      modelValue={true}
      title={confirming ? `确认角色权限变更 - ${roleName}` : `角色权限分配 - ${roleName}`}
      width="980px"
      onClose={onClose}
      footer={(
        <div className="permission-dialog-footer">
          <div className="permission-dialog-footer__summary">
            {confirming
              ? "确认保存后，受影响用户需要重新登录后权限才会完全生效。"
              : summaryText}
          </div>
          <div className="permission-dialog-footer__actions">
            {!confirming ? (
              <>
                <BzButton onClick={onClose}>取消</BzButton>
                {canSave !== false ? <BzButton buttonType="primary" onClick={handleSubmit}>保存</BzButton> : null}
              </>
            ) : (
              <>
                <BzButton onClick={backToEdit}>返回</BzButton>
                <BzButton buttonType="primary" onClick={confirmSubmit}>确认</BzButton>
              </>
            )}
          </div>
        </div>
      )}
    >
      {!confirming ? (
        <div className="permission-dialog-shell">
          <div className="permission-dialog-toolbar">
            <BzInput modelValue={keyword} placeholder="搜索目录、菜单、按钮或权限码" clearable onValueChange={setKeyword} />
            <BzButton className="permission-toolbar-button" onClick={expandAll}>全部展开</BzButton>
            <BzButton className="permission-toolbar-button" onClick={collapseAll}>全部收起</BzButton>
            <BzButton className="permission-toolbar-button" onClick={clearAll}>清空选择</BzButton>
          </div>

          <section className="permission-panel">
            <div className="permission-panel__head">
              <div>
                <div className="permission-panel__title">可选权限</div>
                <div className="permission-panel__hint">
                  目录仅作为分组展示；选择按钮权限时会自动带上所属菜单；选择菜单不会自动选择按钮。
                </div>
              </div>
              <div className="permission-panel__meta">
                {filteredResourceCount} / {resources.length} 项
              </div>
            </div>

            <BzLoading loading={loading} className="permission-tree-wrap">
              {filteredRoots.length === 0 ? (
                <div className="permission-empty">暂无可授权资源</div>
              ) : (
                filteredRoots.map((node) => (
                  <RolePermissionTreeNode
                    key={node.row.id}
                    node={node}
                    expandedIds={displayExpandedIds}
                    selectedIds={selectedNodeIds}
                    canEdit={canSave !== false}
                    onToggleExpand={toggleExpand}
                    onToggleSelect={(payload) => toggleSelect(payload.id, payload.checked)}
                  />
                ))
              )}
            </BzLoading>
          </section>
        </div>
      ) : (
        <div className="permission-dialog-shell">
          <section className="permission-panel">
            <div className="permission-panel__head">
              <div>
                <div className="permission-panel__title">确认权限变更</div>
                <div className="permission-panel__hint">
                  绿色边框表示新增权限；红色删除线表示移除权限；未变化节点仅作为层级路径展示。
                </div>
              </div>
              <div className="permission-panel__meta">
                {diffSummaryText}
              </div>
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
                  />
                ))
              )}
            </div>
          </section>
        </div>
      )}
    </BzDialog>
  );
}
