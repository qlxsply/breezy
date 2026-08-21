import { BzDropdown, BzDropdownItem, BzDropdownMenu } from "@admin/shared/ui/bz";

import type { AdminActionItem } from "./admin-action";

const MAX_DIRECT_ACTIONS = 3;
const MORE_TRIGGER_LABEL = "更多";
const ACTION_BUTTON_MIN_WIDTH = 44;
const ACTION_BUTTON_HORIZONTAL_PADDING = 16;
const ACTION_BUTTON_GAP = 6;
const ACTION_COLUMN_CELL_HORIZONTAL_PADDING = 16;

function estimateActionLabelWidth(label: string): number {
  return Array.from(label).reduce(
    (total, char) => total + (char.codePointAt(0)! <= 0xff ? 8 : 14),
    0,
  );
}

export function partitionAdminActions(actions: AdminActionItem[] = []) {
  const visibleActions = actions.length > MAX_DIRECT_ACTIONS ? actions.slice(0, 2) : actions;
  const moreActions = actions.length > MAX_DIRECT_ACTIONS ? actions.slice(2) : [];
  return { visibleActions, moreActions };
}

export function estimateAdminActionButtonWidth(label: string): number {
  return Math.max(
    ACTION_BUTTON_MIN_WIDTH,
    estimateActionLabelWidth(label) + ACTION_BUTTON_HORIZONTAL_PADDING,
  );
}

export function estimateAdminActionBarWidth(actions: AdminActionItem[] = []): number {
  const { visibleActions, moreActions } = partitionAdminActions(actions);
  const renderedLabels = visibleActions.map((action) => action.label);
  if (moreActions.length > 0) renderedLabels.push(MORE_TRIGGER_LABEL);
  if (renderedLabels.length === 0) return 0;

  const buttonWidth = renderedLabels.reduce(
    (total, label) => total + estimateAdminActionButtonWidth(label),
    0,
  );
  const gaps = ACTION_BUTTON_GAP * Math.max(0, renderedLabels.length - 1);
  return buttonWidth + gaps + ACTION_COLUMN_CELL_HORIZONTAL_PADDING;
}

export function AdminActionBar({ actions = [] }: { actions?: AdminActionItem[] }) {
  const { visibleActions, moreActions } = partitionAdminActions(actions);

  return (
    <div className="admin-action-bar">
      {visibleActions.map((action) => (
        <button
          key={action.key}
          className={`admin-action-link is-${action.level || "default"}`}
          type="button"
          disabled={action.disabled}
          onClick={action.onClick}
        >
          {action.label}
        </button>
      ))}

      {moreActions.length > 0 ? (
        <BzDropdown
          minWidth={112}
          dropdownContent={
            <BzDropdownMenu>
              {moreActions.map((action) => (
                <BzDropdownItem
                  key={action.key}
                  disabled={action.disabled}
                  onClick={action.onClick}
                >
                  <span className={`admin-action-dropdown-item is-${action.level || "default"}`}>
                    {action.label}
                  </span>
                </BzDropdownItem>
              ))}
            </BzDropdownMenu>
          }
        >
          <button
            className="admin-action-link is-more"
            type="button"
          >
            更多
          </button>
        </BzDropdown>
      ) : null}
    </div>
  );
}
