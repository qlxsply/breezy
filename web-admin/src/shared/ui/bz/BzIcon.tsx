import type { HTMLAttributes, ReactNode } from "react";

import styles from "./BzIcon.module.css";

interface BzIconProps extends HTMLAttributes<HTMLSpanElement> {
  children?: ReactNode;
}

export function BzIcon({ children, className, ...rest }: BzIconProps) {
  return (
    <span
      {...rest}
      className={[styles.icon, className].filter(Boolean).join(" ")}
    >
      {children}
    </span>
  );
}
