import Link from "next/link";
import type { ComponentProps } from "react";

import type { BzButtonSize, BzButtonType } from "./BzButton";
import styles from "./BzButton.module.css";

interface BzButtonLinkProps extends ComponentProps<typeof Link> {
  buttonType?: BzButtonType;
  size?: BzButtonSize;
}

export function BzButtonLink({
  buttonType = "default",
  size = "medium",
  className,
  children,
  ...props
}: BzButtonLinkProps) {
  return (
    <Link
      {...props}
      className={[styles.button, styles[buttonType], styles[size], className]
        .filter(Boolean)
        .join(" ")}
    >
      <span className={styles.label}>{children}</span>
    </Link>
  );
}
