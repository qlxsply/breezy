import { BzCard } from "@admin/shared/ui/bz";

export function AdminPlaceholderPage({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          <BzCard
            className="admin-panel placeholder-panel"
            shadow="never"
          >
            <div className="placeholder-body">
              <div className="placeholder-mark">{title.slice(0, 2) || "页面"}</div>
              <div className="placeholder-title">{title}</div>
              <div className="placeholder-desc">{description}</div>
            </div>
          </BzCard>
        </div>
      </div>
    </div>
  );
}
