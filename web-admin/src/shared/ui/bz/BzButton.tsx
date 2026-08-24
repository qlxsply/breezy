import type { ButtonHTMLAttributes, ReactNode } from "react";

import styles from "./BzButton.module.css";

type BzButtonType = "default" | "primary" | "danger" | "success" | "warning";
type BzButtonSize = "small" | "medium" | "large";

interface BzButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, "type"> {
  buttonType?: BzButtonType;
  size?: BzButtonSize;
  loading?: boolean;
  link?: boolean;
  text?: boolean;
  nativeType?: "button" | "submit" | "reset";
  children?: ReactNode;
}

export function BzButton({
  buttonType = "default",
  size = "medium",
  loading = false,
  disabled = false,
  link = false,
  text = false,
  nativeType = "button",
  className,
  children,
  onClick,
  ...rest
}: BzButtonProps) {
  const isDisabled = disabled || loading;

  return (
    <button
      {...rest}
      className={[
        styles.button,
        styles[buttonType],
        styles[size],
        link || text ? styles.link : "",
        text ? styles.text : "",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      disabled={isDisabled}
      type={nativeType}
      onClick={(event) => {
        if (isDisabled) {
          event.preventDefault();
          return;
        }
        onClick?.(event);
      }}
    >
      {loading ? (
        <span
          className={styles.spinner}
          aria-hidden="true"
        />
      ) : null}
      <span className={styles.label}>{children}</span>
    </button>
  );
}
