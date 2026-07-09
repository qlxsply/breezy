import type { RoleGrantResourceEntry } from "../../types/role-admin";

type DiffStatus = "added" | "removed";

export interface RolePermissionTreeNodeView {
  row: RoleGrantResourceEntry;
  children: RolePermissionTreeNodeView[];
}

interface RolePermissionTreeNodeProps {
  depth: number;
  node: RolePermissionTreeNodeView;
  expandedIds: Set<string>;
  selectedIds: Set<string>;
  canEdit: boolean;
  readonly?: boolean;
  diffStatusById?: Map<string, DiffStatus>;
  onToggleExpand: (id: string) => void;
  onToggleSelect: (payload: { id: string; checked: boolean }) => void;
  onToggleButton: (payload: { id: string; checked: boolean }) => void;
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

function resolveNodeKind(
  row: RoleGrantResourceEntry,
): "DIRECTORY" | "MENU" | "FUNCTION" | "BUTTON" {
  if (row.type === "DIRECTORY") return "DIRECTORY";
  if (row.type === "MENU") return "MENU";
  if (row.type === "BUTTON") return "BUTTON";
  return "FUNCTION";
}

function resolveDepthClass(depth: number): string {
  if (depth <= 0) return "role-permission-resource--depth-0";
  if (depth === 1) return "role-permission-resource--depth-1";
  if (depth === 2) return "role-permission-resource--depth-2";
  if (depth === 3) return "role-permission-resource--depth-3";
  if (depth === 4) return "role-permission-resource--depth-4";
  return "role-permission-resource--depth-5";
}

function renderNodeIcon(expanded: boolean, hasNestedChildren: boolean) {
  if (!hasNestedChildren) {
    return (
      <svg
        className="permission-node-toggle__icon"
        viewBox="0 0 14 14"
        aria-hidden="true"
      >
        <circle
          cx="7"
          cy="7"
          r="1.75"
          fill="currentColor"
        />
      </svg>
    );
  }

  return expanded ? (
    <svg
      className="permission-node-toggle__icon"
      viewBox="0 0 14 14"
      aria-hidden="true"
    >
      <path
        d="M3.5 5.25L7 8.75l3.5-3.5"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  ) : (
    <svg
      className="permission-node-toggle__icon"
      viewBox="0 0 14 14"
      aria-hidden="true"
    >
      <path
        d="M5.25 3.5L8.75 7l-3.5 3.5"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function RolePermissionTreeNode({
  depth,
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
  const buttonChildren = node.children.filter((child) => child.row.type === "BUTTON");
  const nestedChildren = node.children.filter((child) => child.row.type !== "BUTTON");
  const hasNestedChildren = nestedChildren.length > 0;
  const expanded = expandedIds.has(node.row.id);
  const checked = selectedIds.has(node.row.id);
  const indeterminate = !checked && hasSelectedDescendant(node, selectedIds);
  const diffClass = resolveDiffClass(node.row.id, diffStatusById);

  function typeLabel() {
    if (kind === "DIRECTORY") return "目录";
    if (kind === "MENU") return "菜单";
    if (kind === "BUTTON") return "按钮";
    return "功能";
  }

  function typeClass() {
    if (kind === "DIRECTORY") return "permission-tag-dir";
    if (kind === "MENU") return "permission-tag-menu";
    if (kind === "BUTTON") return "permission-tag-button";
    return "permission-tag-function";
  }

  return (
    <>
      <div
        className={`admin-grid-table__row role-permission-row--${kind.toLowerCase()} ${diffClass}`}
      >
        <div className="admin-grid-table__cell admin-grid-table__cell--check">
          <input
            className="admin-node-checkbox"
            type="checkbox"
            checked={checked}
            disabled={readonly || !canEdit || !node.row.enabled}
            ref={(el) => {
              if (el) el.indeterminate = indeterminate;
            }}
            onChange={(event) => onToggleSelect({ id: node.row.id, checked: event.target.checked })}
          />
        </div>

        <div className="admin-grid-table__cell role-permission-cell--resource">
          <div className={`role-permission-resource ${resolveDepthClass(depth)}`}>
            <button
              className={`permission-node-toggle${!hasNestedChildren ? " is-placeholder" : ""}`}
              type="button"
              onClick={() => hasNestedChildren && onToggleExpand(node.row.id)}
            >
              {renderNodeIcon(expanded, hasNestedChildren)}
            </button>
            <span className="role-permission-resource__name">{node.row.name}</span>
          </div>
        </div>

        <div className="admin-grid-table__cell role-permission-cell--type">
          <span className={`permission-tag ${typeClass()}`}>{typeLabel()}</span>
        </div>

        <div className="admin-grid-table__cell role-permission-cell--code mono">
          {node.row.code || "-"}
        </div>

        <div className="admin-grid-table__cell role-permission-cell--status">
          <span
            className={`role-permission-status${node.row.enabled ? " is-enabled" : " is-disabled"}`}
          >
            {node.row.enabled ? "启用" : "停用"}
          </span>
        </div>

        <div className="admin-grid-table__cell role-permission-cell--actions">
          {buttonChildren.length > 0 ? (
            <div
              className="role-permission-button-list"
              role="group"
              aria-label={`${node.row.name}按钮权限`}
            >
              {buttonChildren.map((child) => {
                const buttonChecked = selectedIds.has(child.row.id);
                return (
                  <label
                    key={child.row.id}
                    className={[
                      "permission-button-chip",
                      buttonChecked ? "is-checked" : "",
                      readonly || !canEdit || !child.row.enabled ? "is-disabled" : "",
                      resolveDiffClass(child.row.id, diffStatusById),
                    ]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    <input
                      className="permission-button-chip__checkbox"
                      type="checkbox"
                      checked={buttonChecked}
                      disabled={readonly || !canEdit || !child.row.enabled}
                      onChange={(event) =>
                        onToggleButton({
                          id: child.row.id,
                          checked: event.target.checked,
                        })
                      }
                    />
                    <span className="permission-button-chip__name">{child.row.name}</span>
                  </label>
                );
              })}
            </div>
          ) : (
            <span className="role-permission-empty">-</span>
          )}
        </div>
      </div>

      {expanded && hasNestedChildren
        ? nestedChildren.map((child) => (
            <RolePermissionTreeNode
              key={child.row.id}
              depth={depth + 1}
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
          ))
        : null}
    </>
  );
}
