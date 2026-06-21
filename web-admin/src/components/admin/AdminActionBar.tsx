import type { AdminActionItem } from "@admin/types/admin-action";

export function AdminActionBar({ actions = [] }: { actions?: AdminActionItem[] }) {
  return (
    <div className="admin-action-bar">
      {actions.map((action) => (
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
    </div>
  );
}
