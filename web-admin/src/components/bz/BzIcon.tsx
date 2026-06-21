import type { HTMLAttributes, ReactNode } from "react";

interface BzIconProps extends HTMLAttributes<HTMLSpanElement> {
  children?: ReactNode;
}

export function BzIcon({ children, className, ...rest }: BzIconProps) {
  return (
    <span {...rest} className={["bz-icon", className].filter(Boolean).join(" ")}>
      {children}
    </span>
  );
}
