import type { RoleGrantResourceEntry } from "../../types/role-admin";

type DiffStatus = "added" | "removed";

export interface RolePermissionTreeNodeView {
  row: RoleGrantResourceEntry;
  children: RolePermissionTreeNodeView[];
}

interface RolePermissionTreeNodeProps {
  node: RolePermissionTreeNodeView;
  expandedIds: Set<string>;
  selectedIds: Set<string>;
  canEdit: boolean;
  readonly?: boolean;
  diffStatusById?: Map<string, DiffStatus>;
  onToggleExpand: (id: string) => void;
  onToggleSelect: (payload: { id: string; checked: boolean }) => void;
}

function resolveDiffClass(node: RolePermissionTreeNodeView, diffStatusById?: Map<string, DiffStatus>): string {
  const status = diffStatusById?.get(node.row.id);
  if (status === "added") return "is-diff-added";
  if (status === "removed") return "is-diff-removed";
  return "";
}

function hasSelectedDescendant(node: RolePermissionTreeNodeView, selectedIds: Set<string>): boolean {
  return node.children.some((child) => selectedIds.has(child.row.id) || hasSelectedDescendant(child, selectedIds));
}

export function RolePermissionTreeNode({
  node, expandedIds, selectedIds, canEdit, readonly = false, diffStatusById,
  onToggleExpand, onToggleSelect,
}: RolePermissionTreeNodeProps) {
  const buttonChildren = node.children.filter((child) => child.row.type === "BUTTON");
  const nestedChildren = node.children.filter((child) => child.row.type !== "BUTTON");
  const hasNested = nestedChildren.length > 0;
  const expanded = expandedIds.has(node.row.id);
  const checked = selectedIds.has(node.row.id);
  const selectable = node.row.type !== "DIRECTORY";
  const indeterminate = selectable && !checked && hasSelectedDescendant(node, selectedIds);
  const diffClass = resolveDiffClass(node, diffStatusById);

  function typeLabel() {
    if (node.row.type === "DIRECTORY") return "目录";
    if (node.row.type === "MENU") return "菜单";
    return "按钮";
  }

  function typeClass() {
    if (node.row.type === "DIRECTORY") return "permission-tag-dir";
    if (node.row.type === "MENU") return "permission-tag-menu";
    return "permission-tag-button";
  }

  function isNodeChecked(child: RolePermissionTreeNodeView): boolean {
    return selectedIds.has(child.row.id);
  }

  function childDiffClass(child: RolePermissionTreeNodeView): string {
    return resolveDiffClass(child, diffStatusById);
  }

  return (
    <div className="permission-tree-node">
      <div className={`permission-node-row ${node.row.type === "DIRECTORY" ? "is-directory" : ""} ${diffClass}`}>
        <button
          className={`permission-node-toggle${!hasNested ? " is-placeholder" : ""}`}
          type="button"
          onClick={() => hasNested && onToggleExpand(node.row.id)}
        >
          {hasNested ? (expanded ? "▾" : "▸") : "▸"}
        </button>

        {readonly || !selectable ? (
          <span className="permission-node-checkbox-placeholder" />
        ) : (
          <input
            className="permission-node-checkbox"
            type="checkbox"
            checked={checked}
            disabled={!canEdit || !node.row.enabled}
            ref={(el) => { if (el) el.indeterminate = indeterminate; }}
            onChange={(event) => onToggleSelect({ id: node.row.id, checked: event.target.checked })}
          />
        )}

        <span className="permission-node-name">{node.row.name}</span>
        <span className={`permission-tag ${typeClass()}`}>{typeLabel()}</span>
        {!node.row.enabled ? <span className="permission-tag permission-tag-disabled">停用</span> : null}
      </div>

      {buttonChildren.length > 0 ? (
        <div className="permission-button-tags">
          {buttonChildren.map((child) => (
            readonly ? (
              <span key={child.row.id} className={`permission-button-tag is-readonly${isNodeChecked(child) ? " is-checked" : ""} ${childDiffClass(child)}`}>
                <span className="permission-button-tag__name">{child.row.name}</span>
              </span>
            ) : (
              <label key={child.row.id} className={`permission-button-tag${isNodeChecked(child) ? " is-checked" : ""}${!canEdit || !child.row.enabled ? " is-disabled" : ""}`}>
                <input
                  className="permission-button-tag__checkbox"
                  type="checkbox"
                  checked={isNodeChecked(child)}
                  disabled={!canEdit || !child.row.enabled}
                  onChange={(event) => onToggleSelect({ id: child.row.id, checked: event.target.checked })}
                />
                <span className="permission-button-tag__name">{child.row.name}</span>
              </label>
            )
          ))}
        </div>
      ) : null}

      {expanded && nestedChildren.length > 0 ? (
        <div className="permission-tree-children">
          {nestedChildren.map((child) => (
            <RolePermissionTreeNode
              key={child.row.id}
              node={child}
              expandedIds={expandedIds}
              selectedIds={selectedIds}
              canEdit={canEdit}
              readonly={readonly}
              diffStatusById={diffStatusById}
              onToggleExpand={onToggleExpand}
              onToggleSelect={onToggleSelect}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}
