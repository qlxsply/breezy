import type { ReactNode } from "react";

import styles from "./BzButtonGroup.module.css";

interface BzButtonGroupProps {
  children?: ReactNode;
}

export function BzButtonGroup({ children }: BzButtonGroupProps) {
  return <div className={styles.group}>{children}</div>;
}
