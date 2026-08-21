"use client";

import type { DateTimePrecision } from "@admin/shared/lib/formatter";
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
  precision?: DateTimePrecision;
  onValueChange?: (value: string) => void;
  onChange?: (value: string) => void;
}

function extractNativeValue(
  modelValue: string,
  type: PickerType,
  precision: DateTimePrecision,
): string {
  if (!modelValue) return "";
  if (type === "date") {
    return modelValue.slice(0, 10);
  }
  if (precision === "second") {
    if (modelValue.length >= 19) return modelValue.slice(0, 19);
    if (modelValue.length >= 16) return `${modelValue.slice(0, 16)}:00`;
  }
  return modelValue.length >= 16 ? modelValue.slice(0, 16) : modelValue;
}

function formatOutput(value: string, type: PickerType, precision: DateTimePrecision): string {
  if (!value) return "";
  if (type === "date") {
    return value.slice(0, 10);
  }
  if (precision === "second") {
    if (value.length >= 19) return value.slice(0, 19);
    if (value.length >= 16) return `${value.slice(0, 16)}:00`;
  }
  return value.length >= 16 ? value.slice(0, 16) : value;
}

export function BzDatePicker({
  modelValue = "",
  placeholder,
  clearable = false,
  disabled = false,
  type = "date",
  precision = "minute",
  onValueChange,
  onChange,
}: BzDatePickerProps) {
  const inputValue = extractNativeValue(modelValue, type, precision);
  const hasValue = inputValue.length > 0;
  const inputType = type === "datetime" ? "datetime-local" : "date";
  const defaultPlaceholder = placeholder ?? (type === "datetime" ? "选择日期时间" : "选择日期");

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const formatted = formatOutput(event.target.value, type, precision);
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
          step={type === "datetime" ? (precision === "second" ? 1 : 60) : undefined}
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
