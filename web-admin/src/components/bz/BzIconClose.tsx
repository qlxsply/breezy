import type { CSSProperties } from "react";

interface BzIconCloseProps {
  size?: number;
  className?: string;
}

export function BzIconClose({ size = 16, className }: BzIconCloseProps) {
  const style: CSSProperties = {
    width: size,
    height: size,
  };

  return <span className={["bz-icon-close", className].filter(Boolean).join(" ")} style={style} aria-hidden="true" />;
}
