import type { ReactNode } from "react";

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
      className={["admin-form-section", className].filter(Boolean).join(" ")}
      headClassName={headClassName}
      bodyClassName={bodyClassName}
      rightClassName={rightClassName}
    >
      {children}
      {footer ? (
        <div
          className={["admin-form-section__footer", `is-${footerAlign}`, footerClassName]
            .filter(Boolean)
            .join(" ")}
        >
          {footer}
        </div>
      ) : null}
    </AdminSection>
  );
}
