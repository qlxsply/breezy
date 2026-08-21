import type { ReactNode } from "react";

interface BzDropdownMenuProps {
  children?: ReactNode;
}

export function BzDropdownMenu({ children }: BzDropdownMenuProps) {
  return <ul className="bz-dropdown-menu">{children}</ul>;
}
