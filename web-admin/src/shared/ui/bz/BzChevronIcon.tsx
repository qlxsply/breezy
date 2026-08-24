import styles from "./BzChevronIcon.module.css";

interface BzChevronIconProps {
  direction?: "right" | "down";
  size?: number;
  className?: string;
}

export function BzChevronIcon({ direction = "right", size = 14, className }: BzChevronIconProps) {
  return (
    <svg
      className={[styles.icon, styles[direction], className].filter(Boolean).join(" ")}
      viewBox="0 0 16 16"
      width={size}
      height={size}
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M6 3.5L10.5 8L6 12.5"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
