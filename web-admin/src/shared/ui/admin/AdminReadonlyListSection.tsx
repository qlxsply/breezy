import type { ReactNode } from "react";

import { AdminSection, type AdminSectionProps } from "./AdminSection";

interface AdminReadonlyListSectionProps extends Pick<
  AdminSectionProps,
  "title" | "children" | "className" | "bodyClassName" | "headClassName" | "rightClassName"
> {
  meta?: ReactNode;
}

export function AdminReadonlyListSection({
  title,
  meta,
  children,
  className,
  headClassName,
  bodyClassName,
  rightClassName,
}: AdminReadonlyListSectionProps) {
  return (
    <AdminSection
      title={title}
      right={meta}
      className={["admin-readonly-list-section", className].filter(Boolean).join(" ")}
      headClassName={headClassName}
      bodyClassName={bodyClassName}
      rightClassName={rightClassName}
    >
      {children}
    </AdminSection>
  );
}
