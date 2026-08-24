import type { HTMLAttributes, ReactNode } from "react";

import styles from "./BzCard.module.css";

interface BzCardProps extends HTMLAttributes<HTMLElement> {
  shadow?: "always" | "hover" | "never";
  header?: ReactNode;
  children?: ReactNode;
  headerClassName?: string;
  bodyClassName?: string;
}

export function BzCard({
  shadow = "always",
  header,
  children,
  className,
  headerClassName,
  bodyClassName,
  ...rest
}: BzCardProps) {
  return (
    <section
      {...rest}
      className={[styles.card, styles[shadow], className].filter(Boolean).join(" ")}
    >
      {header ? (
        <header className={[styles.header, headerClassName].filter(Boolean).join(" ")}>
          {header}
        </header>
      ) : null}
      <div className={[styles.body, bodyClassName].filter(Boolean).join(" ")}>{children}</div>
    </section>
  );
}
