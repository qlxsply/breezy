"use client";

import { forwardRef, type InputHTMLAttributes, useState } from "react";

export interface TableInputProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  "defaultValue" | "type" | "value"
> {
  value?: string | null;
  defaultValue?: string;
  type?: "text" | "password" | "number";
  onValueChange?: (value: string) => void;
}

export const TableInput = forwardRef<HTMLInputElement, TableInputProps>(function TableInput(
  {
    value,
    defaultValue,
    type = "text",
    maxLength,
    onChange,
    onValueChange,
    disabled = false,
    readOnly = false,
    className,
    ...rest
  },
  ref,
) {
  const normalizeValue = (nextValue: string) =>
    typeof maxLength === "number" && maxLength >= 0 ? nextValue.slice(0, maxLength) : nextValue;
  const [innerValue, setInnerValue] = useState(() => normalizeValue(defaultValue ?? ""));
  const controlled = value !== undefined;
  const currentValue = normalizeValue(controlled ? String(value ?? "") : innerValue);

  return (
    <div
      className={[
        "table-input-control",
        "table-common-input",
        disabled ? "is-disabled" : "",
        readOnly ? "is-readonly" : "",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
    >
      <input
        {...rest}
        ref={ref}
        className="table-common-input__inner"
        type={type}
        value={currentValue}
        maxLength={maxLength}
        disabled={disabled}
        readOnly={readOnly}
        onChange={(event) => {
          const nextValue = normalizeValue(event.currentTarget.value);
          if (!controlled) {
            setInnerValue(nextValue);
          }
          if (event.currentTarget.value !== nextValue) {
            event.currentTarget.value = nextValue;
          }
          onValueChange?.(nextValue);
          onChange?.(event);
        }}
      />
    </div>
  );
});
