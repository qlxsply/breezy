import type { ReactNode } from "react";

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
        "admin-info-cell",
        "admin-info-cell",
        `admin-info-cell--${state}`,
        state === "editable" ? "admin-info-cell--edit" : "",
        mono ? "mono" : "",
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
