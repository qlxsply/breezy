import type { ButtonHTMLAttributes, ReactNode } from "react";

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
        "bz-button",
        `bz-button--${buttonType}`,
        `bz-button--${size}`,
        loading ? "is-loading" : "",
        link || text ? "is-link" : "",
        text ? "is-text" : "",
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
          className="bz-button-spinner"
          aria-hidden="true"
        />
      ) : null}
      <span className="bz-button-text">{children}</span>
    </button>
  );
}
