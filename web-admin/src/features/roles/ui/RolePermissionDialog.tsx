import type {
  RoleEntry,
  RoleGrantResourceEntry,
  RoleGrantSelection,
} from "@admin/features/roles/model/types";
import { formatDateTime } from "@admin/shared/lib/formatter";
import entityStyles from "@admin/shared/ui/admin/AdminEntity.module.css";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import { AdminInfoCell } from "@admin/shared/ui/admin/AdminInfoCell";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { TableInput, TableSelect } from "@admin/shared/ui/admin/inputs";
import { BzAlert } from "@admin/shared/ui/bz/BzAlert";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import { BzInput } from "@admin/shared/ui/bz/BzInput";
import { BzTag } from "@admin/shared/ui/bz/BzTag";
import { type ReactNode, useEffect, useMemo, useRef, useState } from "react";

import treeStyles from "./RolePermissionTree.module.css";
import { RolePermissionTreeNode, type RolePermissionTreeNodeView } from "./RolePermissionTreeNode";

type DiffStatus = "added" | "removed";

interface RolePermissionDialogProps {
  mode: "create" | "detail" | "edit";
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

const emptyRole: RoleEntry = { id: "", code: "", name: "", enabled: true };

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
  const [form, setForm] = useState<RoleEntry>(emptyRole);
  const [keyword, setKeyword] = useState("");
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [confirming, setConfirming] = useState(false);
  const [pendingSelection, setPendingSelection] = useState<RoleGrantSelection | null>(null);
  const [selectedResourceIds, setSelectedResourceIds] = useState<Set<string>>(new Set());
  const [error, setError] = useState("");
  const treeWrapRef = useRef<HTMLDivElement | null>(null);

  const basicEditable = mode !== "detail" && canEditBasic;
  const editable = mode !== "detail" && (canEditBasic || canEditPermissions);
  const permissionEditable = mode !== "detail" && canEditPermissions;
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
      list.sort(
        (left, right) => left.orderNo - right.orderNo || left.name.localeCompare(right.name),
      );
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
    setForm(role ? { ...role } : { ...emptyRole });
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

  const selectableResourceCount = useMemo(
    () => resources.filter((row) => row.selectable).length,
    [resources],
  );

  const headerSelectableRowIds = useMemo(
    () => resources.filter((row) => row.enabled && row.type !== "BUTTON").map((row) => row.id),
    [resources],
  );

  const allRowsSelected = useMemo(
    () =>
      headerSelectableRowIds.length > 0 &&
      headerSelectableRowIds.every((id) => selectedResourceIds.has(id)),
    [headerSelectableRowIds, selectedResourceIds],
  );

  const someRowsSelected = useMemo(
    () => headerSelectableRowIds.some((id) => selectedResourceIds.has(id)),
    [headerSelectableRowIds, selectedResourceIds],
  );

  const summarySelection = useMemo(
    () => buildSubmitSelection(selectedResourceIds),
    [selectedResourceIds, resources],
  );

  const summaryText = useMemo(
    () => `已选 ${summarySelection.resourceIds.length} / ${selectableResourceCount} 项`,
    [selectableResourceCount, summarySelection],
  );

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

  function toggleSelectAll(checked: boolean) {
    if (!permissionEditable) return;
    if (!checked) {
      setSelectedResourceIds(new Set());
      return;
    }

    const next = new Set<string>();
    resources.forEach((row) => {
      if (row.enabled) next.add(row.id);
    });
    setSelectedResourceIds(next);
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
    const addedResourceIds = new Set(
      [...afterResourceIds].filter((id) => !beforeResourceIds.has(id)),
    );
    const removedResourceIds = new Set(
      [...beforeResourceIds].filter((id) => !afterResourceIds.has(id)),
    );
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
    if (!editable) return;

    setError("");
    if (basicEditable) {
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
    const nextRole = { ...form, code: form.code.trim(), name: form.name.trim() };

    if (mode === "create") {
      onSubmit({
        role: nextRole,
        selection: nextSelection,
        permissionChanged: nextSelection.resourceIds.length > 0,
      });
      return;
    }

    const nextDiff = buildSelectionDiff(selection, nextSelection);
    if (!nextDiff.changed) {
      onSubmit({
        role: nextRole,
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
    setPendingSelection(null);
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
    <div className={entityStyles.drawerFooter}>
      <div className={entityStyles.drawerFooterSummary}>
        {confirming ? "确认保存后，受影响用户重新登录后权限才会完全生效。" : ""}
      </div>
      <div className={entityStyles.drawerFooterActions}>
        {!confirming ? (
          <>
            <BzButton onClick={onClose}>{editable ? "取消" : "关闭"}</BzButton>
            {editable ? (
              <BzButton
                buttonType="primary"
                onClick={handleSubmit}
              >
                保存
              </BzButton>
            ) : null}
          </>
        ) : (
          <>
            <BzButton onClick={backToEdit}>返回</BzButton>
            <BzButton
              buttonType="primary"
              onClick={confirmSubmit}
            >
              确认
            </BzButton>
          </>
        )}
      </div>
    </div>
  );

  function renderStatusValue() {
    if (basicEditable) {
      return (
        <AdminInfoCell state="editable">
          <TableSelect
            value={form.enabled ? "true" : "false"}
            options={[
              { label: "启用", value: "true" },
              { label: "停用", value: "false" },
            ]}
            allowClear={false}
            onValueChange={(value) =>
              setForm((current) => ({
                ...current,
                enabled: (Array.isArray(value) ? value[0] : value) === "true",
              }))
            }
          />
        </AdminInfoCell>
      );
    }
    return renderValueCell(
      <BzTag
        className={`${entityStyles.statusTag} ${
          form.enabled ? entityStyles.statusEnabled : entityStyles.statusDisabled
        }`}
        type={form.enabled ? "success" : "danger"}
      >
        {form.enabled ? "启用" : "停用"}
      </BzTag>,
    );
  }

  function renderValueCell(value: ReactNode, options?: { mono?: boolean }) {
    return (
      <AdminInfoCell
        state={mode === "detail" ? "display" : "readonly"}
        mono={options?.mono}
      >
        {value || "-"}
      </AdminInfoCell>
    );
  }

  function renderEditableTextCell(
    value: string,
    placeholder: string,
    onChange: (value: string) => void,
    options?: { mono?: boolean },
  ) {
    return (
      <AdminInfoCell
        state="editable"
        mono={options?.mono}
      >
        <TableInput
          value={value}
          placeholder={placeholder}
          className={options?.mono ? entityStyles.mono : undefined}
          onValueChange={onChange}
        />
      </AdminInfoCell>
    );
  }

  const drawerTitle = confirming
    ? "确认权限变更"
    : mode === "create"
      ? "新增角色"
      : mode === "detail"
        ? "角色详情"
        : "编辑角色";

  return (
    <AdminEntityDrawer
      open
      className={entityStyles.manageDrawer}
      title={drawerTitle}
      width="1180px"
      loading={loading}
      onClose={onClose}
      footer={footer}
    >
      <div className={entityStyles.shell}>
        <section className={entityStyles.section}>
          <div className={entityStyles.sectionHead}>
            <div className={entityStyles.sectionTitle}>角色信息</div>
          </div>

          <div className={entityStyles.infoTableWrap}>
            <table
              className={entityStyles.infoTable}
              aria-label="角色信息"
            >
              <tbody>
                <tr>
                  <th>
                    <span className={basicEditable ? entityStyles.required : undefined}>
                      角色编码
                    </span>
                  </th>
                  {basicEditable
                    ? renderEditableTextCell(
                        form.code,
                        "请输入角色编码",
                        (value) => setForm((current) => ({ ...current, code: value })),
                        { mono: true },
                      )
                    : renderValueCell(form.code || "-", { mono: true })}
                  <th>
                    <span className={basicEditable ? entityStyles.required : undefined}>
                      角色名称
                    </span>
                  </th>
                  {basicEditable
                    ? renderEditableTextCell(form.name, "请输入角色名称", (value) =>
                        setForm((current) => ({ ...current, name: value })),
                      )
                    : renderValueCell(form.name || "-")}
                  <th>
                    <span className={basicEditable ? entityStyles.required : undefined}>状态</span>
                  </th>
                  {renderStatusValue()}
                </tr>

                {mode !== "create" ? (
                  <>
                    <tr>
                      <th>创建人</th>
                      {renderValueCell(role?.createdBy || "-", { mono: true })}
                      <th>创建时间</th>
                      {renderValueCell(formatDateTime(role?.createdAt) || "-", { mono: true })}
                      <th>更新人</th>
                      {renderValueCell(role?.updatedBy || "-", { mono: true })}
                    </tr>
                    <tr>
                      <th>更新时间</th>
                      {renderValueCell(formatDateTime(role?.updatedAt) || "-", { mono: true })}
                      <th>权限概览</th>
                      {renderValueCell(showPermissionSection ? summaryText : "未开放权限查看")}
                      <th>资源总数</th>
                      {renderValueCell(`${resources.length} 项`, { mono: true })}
                    </tr>
                  </>
                ) : null}
              </tbody>
            </table>
          </div>

          {error ? (
            <BzAlert
              title={error}
              type="error"
              showIcon
              className={`form-error ${entityStyles.error}`}
            />
          ) : null}
        </section>

        {showPermissionSection ? (
          !confirming ? (
            <section className={entityStyles.section}>
              <div className={entityStyles.sectionHead}>
                <div className={entityStyles.sectionTitle}>权限内容</div>
                <div className={entityStyles.sectionStat}>{summaryText}</div>
              </div>

              <div className={entityStyles.permissionToolbar}>
                <BzInput
                  modelValue={keyword}
                  placeholder="搜索资源名称/编码/类型"
                  clearable
                  className={entityStyles.permissionToolbarSearch}
                  onValueChange={setKeyword}
                />
                <div className={entityStyles.permissionToolbarActions}>
                  <BzButton
                    className={entityStyles.permissionToolbarButton}
                    onClick={expandAll}
                  >
                    全部展开
                  </BzButton>
                  <BzButton
                    className={entityStyles.permissionToolbarButton}
                    onClick={collapseAll}
                  >
                    全部收起
                  </BzButton>
                  {permissionEditable ? (
                    <BzButton
                      className={entityStyles.permissionToolbarButton}
                      onClick={clearAll}
                    >
                      清空选择
                    </BzButton>
                  ) : null}
                </div>
              </div>

              <div className={`${layoutStyles.gridTable} admin-permission-table`}>
                <div
                  className={layoutStyles.gridViewport}
                  ref={treeWrapRef}
                >
                  <div
                    className={`${layoutStyles.gridRow} ${layoutStyles.gridHead} ${treeStyles.tableHead}`}
                  >
                    <div className={`${layoutStyles.gridCell} ${layoutStyles.gridCheck}`}>
                      {permissionEditable ? (
                        <input
                          className={treeStyles.nodeCheckbox}
                          type="checkbox"
                          checked={allRowsSelected}
                          ref={(el) => {
                            if (el) el.indeterminate = !allRowsSelected && someRowsSelected;
                          }}
                          onChange={(event) => toggleSelectAll(event.target.checked)}
                        />
                      ) : null}
                    </div>
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--resource`}>
                      资源名称
                    </div>
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--type`}>
                      类型
                    </div>
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--code`}>
                      资源编码
                    </div>
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--status`}>
                      状态
                    </div>
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--actions`}>
                      按钮权限
                    </div>
                  </div>

                  <div className={layoutStyles.gridBody}>
                    {filteredRoots.length === 0 ? (
                      <div className={treeStyles.emptyState}>暂无可授权资源</div>
                    ) : (
                      filteredRoots.map((node) => (
                        <RolePermissionTreeNode
                          key={node.row.id}
                          depth={0}
                          node={node}
                          expandedIds={displayExpandedIds}
                          selectedIds={selectedResourceIds}
                          canEdit={permissionEditable}
                          onToggleExpand={toggleExpand}
                          onToggleSelect={(payload) => toggleSelect(payload.id, payload.checked)}
                          onToggleButton={(payload) => toggleSelect(payload.id, payload.checked)}
                        />
                      ))
                    )}
                  </div>
                </div>
              </div>
            </section>
          ) : (
            <section className={entityStyles.section}>
              <div className={entityStyles.sectionHead}>
                <div className={entityStyles.sectionTitleWrap}>
                  <div className={entityStyles.sectionTitle}>确认权限变更</div>
                  <div className={entityStyles.sectionHint}>
                    绿色描边表示新增权限，红色删除线表示移除权限，未变化节点仅用于展示层级路径。
                  </div>
                </div>
                <div className={entityStyles.sectionStat}>{diffSummaryText}</div>
              </div>

              <div className={`${layoutStyles.gridTable} admin-permission-table`}>
                <div className={layoutStyles.gridViewport}>
                  <div
                    className={`${layoutStyles.gridRow} ${layoutStyles.gridHead} ${treeStyles.tableHead}`}
                  >
                    <div className={`${layoutStyles.gridCell} ${layoutStyles.gridCheck}`} />
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--resource`}>
                      资源名称
                    </div>
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--type`}>
                      类型
                    </div>
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--code`}>
                      资源编码
                    </div>
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--status`}>
                      状态
                    </div>
                    <div className={`${layoutStyles.gridCell} admin-permission-cell--actions`}>
                      按钮权限
                    </div>
                  </div>

                  <div className={layoutStyles.gridBody}>
                    {diffRoots.length === 0 ? (
                      <div className={treeStyles.emptyState}>权限未发生变更</div>
                    ) : (
                      diffRoots.map((node) => (
                        <RolePermissionTreeNode
                          key={node.row.id}
                          depth={0}
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
                </div>
              </div>
            </section>
          )
        ) : null}
      </div>
    </AdminEntityDrawer>
  );
}
