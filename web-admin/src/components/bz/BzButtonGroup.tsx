import type { ReactNode } from "react";

interface BzButtonGroupProps {
  children?: ReactNode;
}

export function BzButtonGroup({ children }: BzButtonGroupProps) {
  return <div className="bz-button-group">{children}</div>;
}
