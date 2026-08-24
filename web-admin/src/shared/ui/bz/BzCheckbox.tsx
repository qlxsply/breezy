import styles from "./BzCheckbox.module.css";

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
        styles.checkbox,
        modelValue ? styles.checked : "",
        disabled ? styles.disabled : "",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
    >
      <span className={styles.input}>
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
        <span className={styles.inner} />
      </span>
      {children ? <span className={styles.label}>{children}</span> : null}
    </label>
  );
}
