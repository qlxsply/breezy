interface BzCheckboxProps {
  modelValue?: boolean;
  disabled?: boolean;
  onValueChange?: (value: boolean) => void;
  onChange?: (value: boolean) => void;
  children?: React.ReactNode;
  className?: string;
}

export function BzCheckbox({
  modelValue = false,
  disabled = false,
  onValueChange,
  onChange,
  children,
  className,
}: BzCheckboxProps) {
  return (
    <label
      className={[
        "bz-checkbox",
        modelValue ? "is-checked" : "",
        disabled ? "is-disabled" : "",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
    >
      <span className="bz-checkbox__input">
        <input
          type="checkbox"
          checked={modelValue}
          disabled={disabled}
          onChange={(event) => {
            const checked = event.target.checked;
            onValueChange?.(checked);
            onChange?.(checked);
          }}
        />
        <span className="bz-checkbox__inner" />
      </span>
      {children ? <span className="bz-checkbox__label">{children}</span> : null}
    </label>
  );
}
