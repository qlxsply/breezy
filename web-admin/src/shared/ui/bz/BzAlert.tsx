"use client";

import { useState } from "react";

import styles from "./BzAlert.module.css";
import { BzIconClose } from "./BzIconClose";

export interface BzAlertProps {
  title: string;
  type?: "info" | "success" | "warning" | "error";
  showIcon?: boolean;
  closable?: boolean;
  className?: string;
}

export function BzAlert({
  title,
  type = "info",
  showIcon = false,
  closable = true,
  className,
}: BzAlertProps) {
  const [closed, setClosed] = useState(false);

  if (closed) {
    return null;
  }

  return (
    <div className={[styles.alert, styles[type], className].filter(Boolean).join(" ")}>
      {showIcon ? (
        <span
          className={styles.icon}
          aria-hidden="true"
        />
      ) : null}
      <div className={styles.title}>{title}</div>
      {closable ? (
        <button
          className={styles.close}
          type="button"
          onClick={() => setClosed(true)}
        >
          <BzIconClose />
        </button>
      ) : null}
    </div>
  );
}
