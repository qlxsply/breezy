import type { AdminActionItem } from "@admin/types/admin-action";
import { BzDropdown, BzDropdownItem, BzDropdownMenu } from "@admin/components/bz";

export function AdminActionBar({ actions = [] }: { actions?: AdminActionItem[] }) {
  const visibleActions = actions.length > 3 ? actions.slice(0, 2) : actions;
  const moreActions = actions.length > 3 ? actions.slice(2) : [];

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
