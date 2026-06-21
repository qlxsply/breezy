import type { ReactNode, SelectHTMLAttributes } from "react";

import { BzIconClose } from "./BzIconClose";

interface BzSelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, "value" | "onChange"> {
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
    <div className={["bz-select", disabled ? "is-disabled" : "", className].filter(Boolean).join(" ")}>
      <select
        {...rest}
        className="bz-select__inner"
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
          className="bz-select__clear"
          type="button"
          onClick={() => {
            onValueChange?.(undefined);
            onChange?.(undefined);
          }}
        >
          <BzIconClose size={20} />
        </button>
      ) : null}
      <span className="bz-select__arrow">▾</span>
    </div>
  );
}
