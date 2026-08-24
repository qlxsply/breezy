import type { ReactNode } from "react";

import styles from "./AdminEntity.module.css";

export type AdminInfoCellState = "display" | "editable" | "readonly";

interface AdminInfoCellProps {
  state?: AdminInfoCellState;
  mono?: boolean;
  align?: "left" | "center" | "right";
  colSpan?: number;
  className?: string;
  children: ReactNode;
}

export function AdminInfoCell({
  state = "display",
  mono = false,
  align = "left",
  colSpan,
  className,
  children,
}: AdminInfoCellProps) {
  return (
    <td
      colSpan={colSpan}
      className={[
        styles.infoCell,
        state === "readonly" ? styles.infoCellReadonly : "",
        state === "editable" ? `${styles.infoCellEditable} ${styles.infoCellEdit}` : "",
        mono ? styles.mono : "",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      style={{ textAlign: align }}
    >
      {children}
    </td>
  );
}
