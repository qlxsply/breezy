import type { AdminActionItem } from "@admin/types/admin-action";
import { BzDropdown, BzDropdownItem, BzDropdownMenu } from "@admin/components/bz";

const MAX_DIRECT_ACTIONS = 3;
const MORE_TRIGGER_LABEL = "更多";
const ACTION_BUTTON_MIN_WIDTH = 44;
const ACTION_BUTTON_HORIZONTAL_PADDING = 20;
const ACTION_BUTTON_GAP = 8;
const ACTION_COLUMN_CELL_HORIZONTAL_PADDING = 24;

function estimateActionLabelWidth(label: string): number {
  return Array.from(label).reduce((total, char) => total + (/[\u0000-\u00ff]/.test(char) ? 8 : 14), 0);
}

export function partitionAdminActions(actions: AdminActionItem[] = []) {
  const visibleActions = actions.length > MAX_DIRECT_ACTIONS ? actions.slice(0, 2) : actions;
  const moreActions = actions.length > MAX_DIRECT_ACTIONS ? actions.slice(2) : [];
  return { visibleActions, moreActions };
}

export function estimateAdminActionButtonWidth(label: string): number {
  return Math.max(ACTION_BUTTON_MIN_WIDTH, estimateActionLabelWidth(label) + ACTION_BUTTON_HORIZONTAL_PADDING);
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
          className={`admin-action-link is-${action.tone || "neutral"}`}
          type="button"
          disabled={action.disabled}
          onClick={action.handler}
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
                  onClick={action.handler}
                >
                  <span className={`admin-action-dropdown-item is-${action.tone || "neutral"}`}>
                    {action.label}
                  </span>
                </BzDropdownItem>
              ))}
            </BzDropdownMenu>
          }
        >
          <button className="admin-action-link is-more" type="button">
            更多
          </button>
        </BzDropdown>
      ) : null}
    </div>
  );
}
