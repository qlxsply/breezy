import type { ReactNode } from "react";

import styles from "./AdminFormSection.module.css";
import { AdminSection, type AdminSectionProps } from "./AdminSection";

interface AdminFormSectionProps extends Pick<
  AdminSectionProps,
  "title" | "children" | "className" | "bodyClassName" | "headClassName" | "rightClassName"
> {
  actions?: ReactNode;
  footer?: ReactNode;
  footerAlign?: "left" | "right";
  footerClassName?: string;
}

export function AdminFormSection({
  title,
  actions,
  children,
  footer,
  footerAlign = "right",
  className,
  headClassName,
  bodyClassName,
  rightClassName,
  footerClassName,
}: AdminFormSectionProps) {
  return (
    <AdminSection
      title={title}
      right={actions}
      className={className}
      headClassName={headClassName}
      bodyClassName={bodyClassName}
      rightClassName={rightClassName}
    >
      {children}
      {footer ? (
        <div
          className={[styles.footer, styles[footerAlign], footerClassName]
            .filter(Boolean)
            .join(" ")}
        >
          {footer}
        </div>
      ) : null}
    </AdminSection>
  );
}
