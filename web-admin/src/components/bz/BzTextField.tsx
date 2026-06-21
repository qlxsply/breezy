"use client";

import { useState } from "react";
import { BzIconClose } from "./BzIconClose";

interface BzTextFieldProps {
  modelValue?: string;
  placeholder?: string;
  clearable?: boolean;
  disabled?: boolean;
  readonly?: boolean;
  type?: "text" | "password" | "number" | "search" | "textarea";
  rows?: number;
  maxlength?: number;
  showCounter?: boolean;
  onValueChange?: (value: string) => void;
  onChange?: (value: string) => void;
  onKeyUp?: (event: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
  onFocus?: (event: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
  onBlur?: (event: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
}

export function BzTextField({
  modelValue = "",
  placeholder = "",
  clearable = false,
  disabled = false,
  readonly = false,
  type = "text",
  rows = 3,
  maxlength,
  showCounter = false,
  onValueChange,
  onChange,
  onKeyUp,
  onFocus,
  onBlur,
}: BzTextFieldProps) {
  const [focused, setFocused] = useState(false);
  const hasValue = modelValue.length > 0;
  const currentLength = modelValue.length;

  function normalizeValue(value: string): string {
    if (typeof maxlength === "number" && maxlength >= 0) {
      return value.slice(0, maxlength);
    }
    return value;
  }

  function handleInput(event: React.FormEvent<HTMLInputElement | HTMLTextAreaElement>) {
    const target = event.target as HTMLInputElement | HTMLTextAreaElement;
    const nextValue = normalizeValue(target.value);
    if (target.value !== nextValue) {
      target.value = nextValue;
    }
    onValueChange?.(nextValue);
  }

  function handleChange(event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    onChange?.(normalizeValue(event.target.value));
  }

  function clearValue() {
    onValueChange?.("");
    onChange?.("");
  }

  return (
    <div className="bz-text-field">
      <div className={["bz-text-field__control", disabled ? "is-disabled" : "", type === "textarea" ? "is-textarea" : ""].filter(Boolean).join(" ")}>
        {type === "textarea" ? (
          <textarea
            className="bz-text-field__textarea"
            value={modelValue}
            placeholder={placeholder}
            disabled={disabled}
            readOnly={readonly}
            rows={rows}
            maxLength={maxlength}
            onInput={handleInput}
            onChange={handleChange}
            onKeyUp={onKeyUp}
            onFocus={(e) => { setFocused(true); onFocus?.(e); }}
            onBlur={(e) => { setFocused(false); onBlur?.(e); }}
          />
        ) : (
          <input
            className="bz-text-field__input"
            value={modelValue}
            placeholder={placeholder}
            disabled={disabled}
            readOnly={readonly}
            type={type}
            maxLength={maxlength}
            onInput={handleInput}
            onChange={handleChange}
            onKeyUp={onKeyUp}
            onFocus={(e) => { setFocused(true); onFocus?.(e); }}
            onBlur={(e) => { setFocused(false); onBlur?.(e); }}
          />
        )}
        {clearable && hasValue && !disabled && !readonly && (
          <button className="bz-text-field__clear" type="button" onClick={clearValue}>
            <BzIconClose size={20} />
          </button>
        )}
      </div>
      {showCounter && typeof maxlength === "number" && (
        <div className="bz-text-field__helper">
          <div className="bz-text-field__counter">{currentLength}/{maxlength}</div>
        </div>
      )}
    </div>
  );
}
