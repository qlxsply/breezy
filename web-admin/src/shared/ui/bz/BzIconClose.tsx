import type { CSSProperties } from "react";

import styles from "./BzIconClose.module.css";

interface BzIconCloseProps {
  size?: number;
  className?: string;
}

export function BzIconClose({ size = 16, className }: BzIconCloseProps) {
  const style: CSSProperties = {
    width: size,
    height: size,
  };

  return (
    <span
      className={[styles.icon, className].filter(Boolean).join(" ")}
      style={style}
      aria-hidden="true"
    />
  );
}
