import type { HTMLAttributes, ReactNode } from "react";

import styles from "./BzTag.module.css";

interface BzTagProps extends HTMLAttributes<HTMLSpanElement> {
  type?: "info" | "warning" | "danger" | "success";
  size?: "small" | "medium";
  children?: ReactNode;
}

export function BzTag({
  type = "info",
  size = "medium",
  className,
  children,
  ...rest
}: BzTagProps) {
  return (
    <span
      {...rest}
      className={[styles.tag, styles[type], styles[size], className].filter(Boolean).join(" ")}
    >
      {children}
    </span>
  );
}
