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
  onToggleButton: (payload: { id: string; checked: boolean; permissionCode: string }) => void;
}

function resolveDiffClass(nodeId: string, diffStatusById?: Map<string, DiffStatus>): string {
  const status = diffStatusById?.get(nodeId);
  if (status === "added") return "is-diff-added";
  if (status === "removed") return "is-diff-removed";
  return "";
}

function hasSelectedDescendant(
  node: RolePermissionTreeNodeView,
  selectedIds: Set<string>,
): boolean {
  return node.children.some(
    (child) => selectedIds.has(child.row.id) || hasSelectedDescendant(child, selectedIds),
  );
}

function resolveNodeKind(row: RoleGrantResourceEntry): "DIRECTORY" | "MENU" | "FUNCTION" {
  if (row.menuId) return row.type === "DIRECTORY" ? "DIRECTORY" : "MENU";
  return "FUNCTION";
}

export function RolePermissionTreeNode({
  node,
  expandedIds,
  selectedIds,
  canEdit,
  readonly = false,
  diffStatusById,
  onToggleExpand,
  onToggleSelect,
  onToggleButton,
}: RolePermissionTreeNodeProps) {
  const kind = resolveNodeKind(node.row);
  const nestedChildren = node.children;
  const hasNested = nestedChildren.length > 0;
  const expanded = expandedIds.has(node.row.id);
  const checked = selectedIds.has(node.row.id);
  const indeterminate = !checked && hasSelectedDescendant(node, selectedIds);
  const diffClass = resolveDiffClass(node.row.id, diffStatusById);
  const buttonChecked = checked;

  function typeLabel() {
    if (kind === "DIRECTORY") return "目录";
    if (kind === "MENU") return "菜单";
    return "功能";
  }

  function typeClass() {
    if (kind === "DIRECTORY") return "permission-tag-dir";
    if (kind === "MENU") return "permission-tag-menu";
    return "permission-tag-function";
  }

  return (
    <div className="permission-tree-node">
      <div className={`permission-node-row permission-node-row--${kind.toLowerCase()} ${diffClass}`}>
        <button
          className={`permission-node-toggle${!hasNested ? " is-placeholder" : ""}`}
          type="button"
          onClick={() => hasNested && onToggleExpand(node.row.id)}
        >
          {hasNested ? (expanded ? "▾" : "▸") : "▸"}
        </button>

        <input
          className="permission-node-checkbox"
          type="checkbox"
          checked={checked}
          disabled={readonly || !canEdit || !node.row.enabled}
          ref={(el) => {
            if (el) el.indeterminate = indeterminate;
          }}
          onChange={(event) => onToggleSelect({ id: node.row.id, checked: event.target.checked })}
        />

        <span className="permission-node-name">{node.row.name}</span>
        <span className={`permission-tag ${typeClass()}`}>{typeLabel()}</span>
        {!node.row.enabled ? <span className="permission-tag permission-tag-disabled">停用</span> : null}
        {node.row.code ? <span className="permission-node-code">{node.row.code}</span> : null}
      </div>

      {node.row.permissionCodes.length > 0 ? (
        <div className="permission-button-section">
          <div className="permission-button-section__hint">
            按钮权限标识，勾选任一项等同勾选该功能
          </div>
          <div className="permission-button-tags">
          {node.row.permissionCodes.map((permissionCode) =>
            readonly ? (
              <label
                key={permissionCode}
                className={`permission-button-tag is-readonly${buttonChecked ? " is-checked" : ""} ${diffClass}`}
              >
                <input
                  className="permission-button-tag__checkbox"
                  type="checkbox"
                  checked={buttonChecked}
                  disabled
                  readOnly
                />
                <span className="permission-button-tag__name">{permissionCode}</span>
              </label>
            ) : (
              <label
                key={permissionCode}
                className={`permission-button-tag${buttonChecked ? " is-checked" : ""}${!canEdit || !node.row.enabled ? " is-disabled" : ""}`}
              >
                <input
                  className="permission-button-tag__checkbox"
                  type="checkbox"
                  checked={buttonChecked}
                  disabled={!canEdit || !node.row.enabled}
                  onChange={(event) =>
                    onToggleButton({
                      id: node.row.id,
                      checked: event.target.checked,
                      permissionCode,
                    })
                  }
                />
                <span className="permission-button-tag__name">{permissionCode}</span>
              </label>
            ),
          )}
          </div>
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
              onToggleButton={onToggleButton}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}
