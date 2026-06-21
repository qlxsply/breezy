"use client";

import type { ChangeEvent, MouseEvent } from "react";

import { BzIconClose } from "./BzIconClose";

type PickerType = "date" | "datetime";

interface BzDatePickerProps {
  modelValue?: string;
  placeholder?: string;
  clearable?: boolean;
  disabled?: boolean;
  type?: PickerType;
  valueFormat?: string;
  onValueChange?: (value: string) => void;
  onChange?: (value: string) => void;
}

function extractNativeValue(modelValue: string, type: PickerType): string {
  if (!modelValue) return "";
  if (type === "date") {
    return modelValue.slice(0, 10);
  }
  return modelValue.length >= 16 ? modelValue.slice(0, 16) : modelValue;
}

function formatOutput(value: string, type: PickerType): string {
  if (!value) return "";
  if (type === "date") {
    return value.slice(0, 10);
  }
  return value.length >= 16 ? value.slice(0, 16) : value;
}

export function BzDatePicker({
  modelValue = "",
  placeholder,
  clearable = false,
  disabled = false,
  type = "date",
  onValueChange,
  onChange,
}: BzDatePickerProps) {
  const inputValue = extractNativeValue(modelValue, type);
  const hasValue = inputValue.length > 0;
  const inputType = type === "datetime" ? "datetime-local" : "date";
  const defaultPlaceholder = placeholder ?? (type === "datetime" ? "选择日期时间" : "选择日期");

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const formatted = formatOutput(event.target.value, type);
    onValueChange?.(formatted);
    onChange?.(formatted);
  }

  function clearValue(event: MouseEvent) {
    event.stopPropagation();
    onValueChange?.("");
    onChange?.("");
  }

  return (
    <div className={["bz-date-picker", disabled ? "is-disabled" : ""].filter(Boolean).join(" ")}>
      <div className="bz-date-picker__input-wrap">
        <input
          className="bz-date-picker__input"
          type={inputType}
          value={inputValue}
          placeholder={defaultPlaceholder}
          disabled={disabled}
          onChange={handleChange}
        />
        {clearable && hasValue && !disabled && (
          <button
            className="bz-date-picker__clear"
            type="button"
            onClick={clearValue}
          >
            <BzIconClose size={20} />
          </button>
        )}
        <span className="bz-date-picker__icon">📅</span>
      </div>
    </div>
  );
}
