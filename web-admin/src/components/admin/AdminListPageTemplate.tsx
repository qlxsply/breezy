import type { ReactNode } from "react";

interface AdminListPageTemplateProps {
  queryPanelVisible?: boolean;
  queryPanel?: ReactNode;
  businessActions?: ReactNode;
  queryTools?: ReactNode;
  batchToolbar?: ReactNode;
  table: ReactNode;
  footer?: ReactNode;
  overlays?: ReactNode;
  className?: string;
  regionClassName?: string;
  queryPanelClassName?: string;
  toolbarRowClassName?: string;
  businessActionsClassName?: string;
  queryToolsClassName?: string;
  tableAreaClassName?: string;
}

export function AdminListPageTemplate({
  queryPanelVisible = false,
  queryPanel,
  businessActions,
  queryTools,
  batchToolbar,
  table,
  footer,
  overlays,
  className,
  regionClassName,
  queryPanelClassName,
  toolbarRowClassName,
  businessActionsClassName,
  queryToolsClassName,
  tableAreaClassName,
}: AdminListPageTemplateProps) {
  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          <div className={["admin-list-template", className].filter(Boolean).join(" ")}>
            <div className={["admin-list-region", regionClassName].filter(Boolean).join(" ")}>
              {queryPanelVisible && queryPanel ? (
                <div
                  className={["admin-list-query-panel", queryPanelClassName]
                    .filter(Boolean)
                    .join(" ")}
                >
                  {queryPanel}
                </div>
              ) : null}

              {batchToolbar ? (
                batchToolbar
              ) : (
                <div
                  className={["admin-list-toolbar-row", toolbarRowClassName]
                    .filter(Boolean)
                    .join(" ")}
                >
                  <div
                    className={["admin-list-business-actions", businessActionsClassName]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    {businessActions}
                  </div>
                  <div
                    className={["admin-list-query-tools", queryToolsClassName]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    {queryTools}
                  </div>
                </div>
              )}

              <div
                className={["admin-table-surface", "admin-list-table-area", tableAreaClassName]
                  .filter(Boolean)
                  .join(" ")}
              >
                {table}
              </div>

              {footer}
            </div>
          </div>

          {overlays}
        </div>
      </div>
    </div>
  );
}
