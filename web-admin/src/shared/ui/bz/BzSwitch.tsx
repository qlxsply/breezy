import styles from "./BzSwitch.module.css";

interface BzSwitchProps {
  modelValue: boolean;
  disabled?: boolean;
  activeText?: string;
  inactiveText?: string;
  onValueChange?: (value: boolean) => void;
  onChange?: (value: boolean) => void;
  className?: string;
}

export function BzSwitch({
  modelValue = false,
  disabled = false,
  activeText = "",
  inactiveText = "",
  onValueChange,
  onChange,
  className,
}: BzSwitchProps) {
  return (
    <label
      className={[
        styles.switch,
        modelValue ? styles.checked : "",
        disabled ? styles.disabled : "",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
    >
      <input
        className={styles.input}
        type="checkbox"
        checked={modelValue}
        disabled={disabled}
        onChange={(event) => {
          const checked = event.target.checked;
          onValueChange?.(checked);
          onChange?.(checked);
        }}
      />
      <span className={styles.core} />
      {activeText || inactiveText ? (
        <span className={styles.text}>{modelValue ? activeText : inactiveText}</span>
      ) : null}
    </label>
  );
}
