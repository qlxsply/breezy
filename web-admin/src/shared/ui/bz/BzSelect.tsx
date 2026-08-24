import type { ReactNode, SelectHTMLAttributes } from "react";

import { BzIconClose } from "./BzIconClose";
import styles from "./BzSelect.module.css";

interface BzSelectProps extends Omit<
  SelectHTMLAttributes<HTMLSelectElement>,
  "value" | "onChange"
> {
  modelValue?: string | number | null;
  placeholder?: string;
  clearable?: boolean;
  onValueChange?: (value: string | undefined) => void;
  onChange?: (value: string | undefined) => void;
  children?: ReactNode;
}

export function BzSelect({
  modelValue = "",
  placeholder = "",
  clearable = false,
  disabled = false,
  className,
  onValueChange,
  onChange,
  children,
  ...rest
}: BzSelectProps) {
  const selectValue = String(modelValue ?? "");
  const hasValue = selectValue !== "";

  return (
    <div
      className={[styles.root, disabled ? styles.disabled : "", className]
        .filter(Boolean)
        .join(" ")}
    >
      <select
        {...rest}
        className={styles.inner}
        value={selectValue}
        disabled={disabled}
        onChange={(event) => {
          const value = event.currentTarget.value === "" ? undefined : event.currentTarget.value;
          onValueChange?.(value);
          onChange?.(value);
        }}
      >
        <option value="">{placeholder || "请选择"}</option>
        {children}
      </select>
      {clearable && hasValue && !disabled ? (
        <button
          className={styles.clear}
          type="button"
          onClick={() => {
            onValueChange?.(undefined);
            onChange?.(undefined);
          }}
        >
          <BzIconClose size={20} />
        </button>
      ) : null}
      <span className={styles.arrow}>▾</span>
    </div>
  );
}
