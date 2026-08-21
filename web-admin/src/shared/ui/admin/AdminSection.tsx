import type { ReactNode } from "react";

export interface AdminSectionProps {
  title: ReactNode;
  right?: ReactNode;
  children?: ReactNode;
  className?: string;
  headClassName?: string;
  bodyClassName?: string;
  rightClassName?: string;
}

export function AdminSection({
  title,
  right,
  children,
  className,
  headClassName,
  bodyClassName,
  rightClassName,
}: AdminSectionProps) {
  return (
    <section className={["admin-section", className].filter(Boolean).join(" ")}>
      <div className={["admin-section__head", headClassName].filter(Boolean).join(" ")}>
        <div className="admin-section__title">{title}</div>
        {right ? (
          <div className={["admin-section__right", rightClassName].filter(Boolean).join(" ")}>
            {right}
          </div>
        ) : null}
      </div>
      <div className={["admin-section__body", bodyClassName].filter(Boolean).join(" ")}>
        {children}
      </div>
    </section>
  );
}
