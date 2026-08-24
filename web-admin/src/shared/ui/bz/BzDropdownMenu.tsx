import type { ReactNode } from "react";

import styles from "./BzDropdownMenu.module.css";

interface BzDropdownMenuProps {
  children?: ReactNode;
}

export function BzDropdownMenu({ children }: BzDropdownMenuProps) {
  return <ul className={styles.menu}>{children}</ul>;
}
