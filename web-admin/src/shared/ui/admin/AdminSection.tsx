import type { ReactNode } from "react";

import styles from "./AdminSection.module.css";

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
    <section className={[styles.section, className].filter(Boolean).join(" ")}>
      <div className={[styles.head, headClassName].filter(Boolean).join(" ")}>
        <div className={styles.title}>{title}</div>
        {right ? (
          <div className={[styles.right, rightClassName].filter(Boolean).join(" ")}>{right}</div>
        ) : null}
      </div>
      <div className={bodyClassName}>{children}</div>
    </section>
  );
}
