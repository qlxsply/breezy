import type { ReactNode } from "react";

import styles from "./AdminListPageTemplate.module.css";

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
    <div className={styles.page}>
      <div className={styles.content}>
        <div className={styles.pageStack}>
          <div className={[styles.template, className].filter(Boolean).join(" ")}>
            <div className={[styles.region, regionClassName].filter(Boolean).join(" ")}>
              {queryPanelVisible && queryPanel ? (
                <div className={[styles.queryPanel, queryPanelClassName].filter(Boolean).join(" ")}>
                  {queryPanel}
                </div>
              ) : null}

              {batchToolbar ? (
                batchToolbar
              ) : (
                <div className={[styles.toolbarRow, toolbarRowClassName].filter(Boolean).join(" ")}>
                  <div
                    className={[styles.businessActions, businessActionsClassName]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    {businessActions}
                  </div>
                  <div
                    className={[styles.queryTools, queryToolsClassName].filter(Boolean).join(" ")}
                  >
                    {queryTools}
                  </div>
                </div>
              )}

              <div
                className={[styles.tableSurface, styles.tableArea, tableAreaClassName]
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
