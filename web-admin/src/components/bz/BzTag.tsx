import type { HTMLAttributes, ReactNode } from "react";

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
      className={["bz-tag", `bz-tag--${type}`, `bz-tag--${size}`, className]
        .filter(Boolean)
        .join(" ")}
    >
      {children}
    </span>
  );
}
