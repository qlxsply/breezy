import type { RoleGrantResourceEntry } from "@admin/features/roles/model/types";
import entityStyles from "@admin/shared/ui/admin/AdminEntity.module.css";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";

import styles from "./RolePermissionTree.module.css";

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
  if (status === "added") return styles.diffAdded;
  if (status === "removed") return styles.diffRemoved;
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
  if (depth <= 0) return styles.depth0;
  if (depth === 1) return styles.depth1;
  if (depth === 2) return styles.depth2;
  if (depth === 3) return styles.depth3;
  if (depth === 4) return styles.depth4;
  return styles.depth5;
}

function renderNodeIcon(expanded: boolean, hasNestedChildren: boolean) {
  if (!hasNestedChildren) {
    return (
      <svg
        className={styles.nodeToggleIcon}
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
      className={styles.nodeToggleIcon}
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
      className={styles.nodeToggleIcon}
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
    if (kind === "DIRECTORY") return styles.tagDirectory;
    if (kind === "MENU") return styles.tagMenu;
    if (kind === "BUTTON") return styles.tagButton;
    return styles.tagFunction;
  }

  function kindClass() {
    if (kind === "DIRECTORY") return styles.directory;
    if (kind === "MENU") return styles.menu;
    if (kind === "BUTTON") return "";
    return styles.function;
  }

  return (
    <>
      <div className={`${layoutStyles.gridRow} ${styles.row} ${kindClass()} ${diffClass}`}>
        <div className={`${layoutStyles.gridCell} ${layoutStyles.gridCheck}`}>
          <input
            className={styles.nodeCheckbox}
            type="checkbox"
            checked={checked}
            disabled={readonly || !canEdit || !node.row.enabled}
            ref={(el) => {
              if (el) el.indeterminate = indeterminate;
            }}
            onChange={(event) => onToggleSelect({ id: node.row.id, checked: event.target.checked })}
          />
        </div>

        <div className={`${layoutStyles.gridCell} ${styles.resourceCell}`}>
          <div className={`${styles.resource} ${resolveDepthClass(depth)}`}>
            <button
              className={`${styles.nodeToggle}${!hasNestedChildren ? ` ${styles.placeholder}` : ""}`}
              type="button"
              onClick={() => hasNestedChildren && onToggleExpand(node.row.id)}
            >
              {renderNodeIcon(expanded, hasNestedChildren)}
            </button>
            <span className={styles.resourceName}>{node.row.name}</span>
          </div>
        </div>

        <div className={layoutStyles.gridCell}>
          <span className={`${styles.tag} ${typeClass()}`}>{typeLabel()}</span>
        </div>

        <div className={`${layoutStyles.gridCell} ${entityStyles.mono}`}>
          {node.row.code || "-"}
        </div>

        <div className={layoutStyles.gridCell}>
          <span className={`${styles.status}${node.row.enabled ? "" : ` ${styles.disabled}`}`}>
            {node.row.enabled ? "启用" : "停用"}
          </span>
        </div>

        <div className={layoutStyles.gridCell}>
          {buttonChildren.length > 0 ? (
            <div
              className={styles.buttonList}
              role="group"
              aria-label={`${node.row.name}按钮权限`}
            >
              {buttonChildren.map((child) => {
                const buttonChecked = selectedIds.has(child.row.id);
                return (
                  <label
                    key={child.row.id}
                    className={[
                      styles.buttonChip,
                      buttonChecked ? styles.checked : "",
                      readonly || !canEdit || !child.row.enabled ? styles.disabled : "",
                      resolveDiffClass(child.row.id, diffStatusById),
                    ]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    <input
                      className={styles.buttonCheckbox}
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
                    <span className={styles.buttonName}>{child.row.name}</span>
                  </label>
                );
              })}
            </div>
          ) : (
            <span className={styles.empty}>-</span>
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
