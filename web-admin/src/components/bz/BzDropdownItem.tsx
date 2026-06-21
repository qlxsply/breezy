import { useBzDropdownContext } from "./BzDropdownContext";

interface BzDropdownItemProps {
  disabled?: boolean;
  children?: React.ReactNode;
  onClick?: () => void;
}

export function BzDropdownItem({ disabled = false, children, onClick }: BzDropdownItemProps) {
  const ctx = useBzDropdownContext();

  function handleClick() {
    if (disabled) return;
    onClick?.();
    ctx?.close();
  }

  return (
    <li>
      <button className={["bz-dropdown-item", disabled ? "is-disabled" : ""].filter(Boolean).join(" ")} type="button" disabled={disabled} onClick={handleClick}>
        {children}
      </button>
    </li>
  );
}
