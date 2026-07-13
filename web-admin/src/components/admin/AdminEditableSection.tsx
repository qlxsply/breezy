import type { ReactNode } from "react";

import { AdminSection, type AdminSectionProps } from "./AdminSection";

interface AdminEditableSectionProps extends Pick<AdminSectionProps, "title" | "children" | "className" | "bodyClassName" | "headClassName" | "rightClassName"> {
  actions?: ReactNode;
}

export function AdminEditableSection({
  title,
  actions,
  children,
  className,
  headClassName,
  bodyClassName,
  rightClassName,
}: AdminEditableSectionProps) {
  return <AdminSection title={title} right={actions} className={["admin-editable-section", className].filter(Boolean).join(" ")} headClassName={headClassName} bodyClassName={bodyClassName} rightClassName={rightClassName}>{children}</AdminSection>;
}
