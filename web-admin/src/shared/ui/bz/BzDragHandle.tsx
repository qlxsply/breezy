import type { ButtonHTMLAttributes } from "react";

export type BzDragHandleProps = Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  "children" | "type" | "draggable"
>;

export function BzDragHandle({ className, title = "拖动调整顺序", ...props }: BzDragHandleProps) {
  return (
    <button
      {...props}
      className={["bz-drag-handle", className].filter(Boolean).join(" ")}
      type="button"
      draggable
      title={title}
      aria-label={props["aria-label"] || title}
    >
      <svg
        width="16"
        height="16"
        viewBox="0 0 16 16"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        focusable="false"
        aria-hidden="true"
      >
        <circle
          cx="5"
          cy="3.5"
          r="1.25"
          fill="currentColor"
        />
        <circle
          cx="11"
          cy="3.5"
          r="1.25"
          fill="currentColor"
        />
        <circle
          cx="5"
          cy="8"
          r="1.25"
          fill="currentColor"
        />
        <circle
          cx="11"
          cy="8"
          r="1.25"
          fill="currentColor"
        />
        <circle
          cx="5"
          cy="12.5"
          r="1.25"
          fill="currentColor"
        />
        <circle
          cx="11"
          cy="12.5"
          r="1.25"
          fill="currentColor"
        />
      </svg>
    </button>
  );
}
