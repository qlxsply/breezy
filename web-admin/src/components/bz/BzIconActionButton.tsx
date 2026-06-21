import type { CSSProperties, MouseEvent } from "react";

type IconName =
  | "plus"
  | "minus"
  | "edit"
  | "search"
  | "refresh"
  | "pause"
  | "resume"
  | "check"
  | "check-note"
  | "delete"
  | "save"
  | "close"
  | "upload"
  | "download"
  | "copy"
  | "compare"
  | "list"
  | "more";

type ActionTone = "default" | "primary" | "danger";

type ActionNativeType = "button" | "submit" | "reset";

interface BzIconActionButtonProps {
  icon: IconName;
  tone?: ActionTone;
  disabled?: boolean;
  title?: string;
  ariaLabel?: string;
  nativeType?: ActionNativeType;
  size?: number;
  onClick?: (event: MouseEvent<HTMLButtonElement>) => void;
}

function renderIcon(icon: IconName) {
  const svg = (children: React.ReactNode) => (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      {children}
    </svg>
  );

  switch (icon) {
    case "plus":
      return svg(
        <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      );
    case "minus":
      return svg(
        <path d="M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      );
    case "search":
      return svg(
        <>
          <circle cx="11" cy="11" r="6.5" stroke="currentColor" strokeWidth="1.8" />
          <path d="M16 16l5 5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </>
      );
    case "refresh":
      return svg(
        <>
          <path d="M20 11a8 8 0 0 0-13.66-5.66L4 8" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M4 4v4h4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M4 13a8 8 0 0 0 13.66 5.66L20 16" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M20 20v-4h-4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
        </>
      );
    case "pause":
      return svg(
        <>
          <rect x="6.5" y="5.5" width="4" height="13" rx="1" stroke="currentColor" strokeWidth="1.8" />
          <rect x="13.5" y="5.5" width="4" height="13" rx="1" stroke="currentColor" strokeWidth="1.8" />
        </>
      );
    case "resume":
      return svg(
        <path d="M8 6.5v11l9-5.5-9-5.5z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
      );
    case "check":
      return svg(
        <path d="M5.5 12.5l4 4L18.5 7.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      );
    case "check-note":
      return svg(
        <>
          <path d="M4 5.5h16v13H4z" stroke="currentColor" strokeWidth="1.7" />
          <path d="M7 9.5h6M7 12.5h4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
          <path d="M13.8 15.2l1.7 1.8 3.2-3.6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
        </>
      );
    case "delete":
      return svg(
        <>
          <path d="M4 7H20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          <path d="M9 5.5C9 4.67 9.67 4 10.5 4H13.5C14.33 4 15 4.67 15 5.5V7H9V5.5Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
          <rect x="6" y="7" width="12" height="13" rx="2" stroke="currentColor" strokeWidth="1.8" />
          <path d="M10 11V17" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
          <path d="M14 11V17" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        </>
      );
    case "save":
      return svg(
        <>
          <path d="M5 4h11l3 3v13H5z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
          <path d="M8 4v6h8V4" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
          <path d="M9 18h6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
        </>
      );
    case "close":
      return svg(
        <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
      );
    case "upload":
      return svg(
        <>
          <path d="M12 16V6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          <path d="M8.5 9.5L12 6l3.5 3.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M5 18.5h14" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </>
      );
    case "download":
      return svg(
        <>
          <path d="M12 5v10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          <path d="M8.5 12.5 12 16l3.5-3.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M5 19h14" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        </>
      );
    case "copy":
      return svg(
        <>
          <rect x="8" y="8" width="11" height="12" rx="2" stroke="currentColor" strokeWidth="1.7" />
          <path d="M6 15H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v1" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
        </>
      );
    case "compare":
      return svg(
        <>
          <rect x="3" y="4" width="18" height="16" rx="2" stroke="currentColor" strokeWidth="1.6" />
          <path d="M12 4V20" stroke="currentColor" strokeWidth="1.6" />
          <path d="M6 8H9" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
          <path d="M6 11H10" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
          <path d="M6 14H8.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
          <path d="M14.5 8H18" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
          <path d="M14.5 11H17" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
          <path d="M14.5 14H18.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
        </>
      );
    case "list":
      return svg(
        <>
          <path d="M8 7h11M8 12h11M8 17h11" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          <circle cx="4.5" cy="7" r="1" fill="currentColor" />
          <circle cx="4.5" cy="12" r="1" fill="currentColor" />
          <circle cx="4.5" cy="17" r="1" fill="currentColor" />
        </>
      );
    case "more":
      return svg(
        <>
          <circle cx="6" cy="12" r="1.7" fill="currentColor" />
          <circle cx="12" cy="12" r="1.7" fill="currentColor" />
          <circle cx="18" cy="12" r="1.7" fill="currentColor" />
        </>
      );
    case "edit":
    default:
      return svg(
        <>
          <path d="M3 17.25V21h3.75L18.2 9.56l-3.75-3.75L3 17.25z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
          <path d="M13.9 6.06l3.75 3.75" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
        </>
      );
  }
}

export function BzIconActionButton({
  icon,
  tone = "default",
  disabled = false,
  title,
  ariaLabel,
  nativeType = "button",
  size = 28,
  onClick,
}: BzIconActionButtonProps) {
  const style: CSSProperties = {
    "--bz-icon-action-size": `${size}px`,
  } as CSSProperties;

  return (
    <button
      className={[
        "bz-icon-action-button",
        `bz-icon-action-button--${tone}`,
      ].join(" ")}
      style={style}
      type={nativeType}
      disabled={disabled}
      title={title}
      aria-label={ariaLabel || title}
      onClick={(event) => {
        if (disabled) {
          event.preventDefault();
          return;
        }
        onClick?.(event);
      }}
    >
      {renderIcon(icon)}
    </button>
  );
}
