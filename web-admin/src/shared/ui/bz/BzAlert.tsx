"use client";

import { useState } from "react";

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
    <div className={["bz-alert", `bz-alert--${type}`, className].filter(Boolean).join(" ")}>
      {showIcon ? (
        <span
          className="bz-alert__icon"
          aria-hidden="true"
        />
      ) : null}
      <div className="bz-alert__title">{title}</div>
      {closable ? (
        <button
          className="bz-alert__close bz-dialog__close"
          type="button"
          onClick={() => setClosed(true)}
        >
          <BzIconClose />
        </button>
      ) : null}
    </div>
  );
}
