import type { HTMLAttributes, ReactNode } from "react";

interface BzCardProps extends HTMLAttributes<HTMLElement> {
  shadow?: "always" | "hover" | "never";
  header?: ReactNode;
  children?: ReactNode;
}

export function BzCard({ shadow = "always", header, children, className, ...rest }: BzCardProps) {
  return (
    <section
      {...rest}
      className={["bz-card", `bz-card--${shadow}`, className].filter(Boolean).join(" ")}
    >
      {header ? <header className="bz-card__header">{header}</header> : null}
      <div className="bz-card__body">{children}</div>
    </section>
  );
}
