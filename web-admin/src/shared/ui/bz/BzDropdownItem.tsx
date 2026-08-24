import { useBzDropdownContext } from "./BzDropdownContext";
import styles from "./BzDropdownItem.module.css";

interface BzDropdownItemProps {
  disabled?: boolean;
  children?: React.ReactNode;
  onClick?: () => void;
  className?: string;
}

export function BzDropdownItem({
  disabled = false,
  children,
  onClick,
  className,
}: BzDropdownItemProps) {
  const ctx = useBzDropdownContext();

  function handleClick() {
    if (disabled) return;
    onClick?.();
    ctx?.close();
  }

  return (
    <li>
      <button
        className={[styles.item, disabled ? styles.disabled : "", className]
          .filter(Boolean)
          .join(" ")}
        type="button"
        disabled={disabled}
        onClick={handleClick}
      >
        {children}
      </button>
    </li>
  );
}
