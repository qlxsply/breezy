"use client";

import { type CSSProperties, forwardRef, type TextareaHTMLAttributes, useState } from "react";

export interface TableTextAreaProps extends Omit<
  TextareaHTMLAttributes<HTMLTextAreaElement>,
  "style"
> {
  showCount?: boolean;
  onValueChange?: (value: string) => void;
  style?: CSSProperties;
  textareaStyle?: CSSProperties;
}

export const TableTextArea = forwardRef<HTMLTextAreaElement, TableTextAreaProps>(
  function TableTextArea(
    {
      value,
      defaultValue,
      onChange,
      onValueChange,
      maxLength,
      placeholder,
      rows = 3,
      showCount = true,
      disabled = false,
      readOnly = false,
      className,
      style,
      textareaStyle,
      ...rest
    },
    ref,
  ) {
    const [innerValue, setInnerValue] = useState(() => String(defaultValue ?? ""));
    const controlled = value !== undefined;
    const currentValue = controlled ? String(value ?? "") : innerValue;
    const currentLength = currentValue.length;
    const countState = getCountState(currentLength, maxLength);

    return (
      <div
        className={[
          "table-input-control",
          "table-text-area",
          disabled ? "is-disabled" : "",
          readOnly ? "is-readonly" : "",
          className,
        ]
          .filter(Boolean)
          .join(" ")}
        style={style}
      >
        <textarea
          {...rest}
          ref={ref}
          className="table-text-area__input"
          value={currentValue}
          placeholder={placeholder}
          rows={rows}
          maxLength={maxLength}
          disabled={disabled}
          readOnly={readOnly}
          style={textareaStyle}
          onChange={(event) => {
            const nextValue = event.currentTarget.value;
            if (!controlled) {
              setInnerValue(nextValue);
            }
            onValueChange?.(nextValue);
            onChange?.(event);
          }}
        />

        <div
          className="table-text-area__mirror"
          aria-hidden="true"
        >
          <div className="table-text-area__mirror-content">
            <span>{currentValue}</span>
            {showCount && currentLength > 0 ? (
              <span
                className="table-text-area__count"
                data-state={countState}
              >
                {maxLength ? `${currentLength}/${maxLength}` : currentLength}
              </span>
            ) : null}
          </div>
        </div>
      </div>
    );
  },
);

function getCountState(
  count: number,
  maxLength?: number,
): "normal" | "safe" | "attention" | "near" | "full" {
  if (!maxLength || maxLength <= 0) {
    return "normal";
  }
  const ratio = count / maxLength;
  if (ratio < 0.6) {
    return "safe";
  }
  if (ratio < 0.9) {
    return "attention";
  }
  if (ratio < 1) {
    return "near";
  }
  return "full";
}
