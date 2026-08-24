"use client";

import { BzIconClose } from "./BzIconClose";
import styles from "./BzTextField.module.css";

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
  className?: string;
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
  className,
}: BzTextFieldProps) {
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
    <div className={[styles.field, className].filter(Boolean).join(" ")}>
      <div
        className={[
          styles.control,
          disabled ? styles.disabled : "",
          type === "textarea" ? styles.textareaControl : "",
        ]
          .filter(Boolean)
          .join(" ")}
      >
        {type === "textarea" ? (
          <textarea
            className={styles.textarea}
            value={modelValue}
            placeholder={placeholder}
            disabled={disabled}
            readOnly={readonly}
            rows={rows}
            maxLength={maxlength}
            onInput={handleInput}
            onChange={handleChange}
            onKeyUp={onKeyUp}
            onFocus={(e) => onFocus?.(e)}
            onBlur={(e) => onBlur?.(e)}
          />
        ) : (
          <input
            className={styles.input}
            value={modelValue}
            placeholder={placeholder}
            disabled={disabled}
            readOnly={readonly}
            type={type}
            maxLength={maxlength}
            onInput={handleInput}
            onChange={handleChange}
            onKeyUp={onKeyUp}
            onFocus={(e) => onFocus?.(e)}
            onBlur={(e) => onBlur?.(e)}
          />
        )}
        {clearable && hasValue && !disabled && !readonly && (
          <button
            className={styles.clear}
            type="button"
            onClick={clearValue}
          >
            <BzIconClose size={20} />
          </button>
        )}
      </div>
      {showCounter && typeof maxlength === "number" && (
        <div className={styles.helper}>
          <div className={styles.counter}>
            {currentLength}/{maxlength}
          </div>
        </div>
      )}
    </div>
  );
}
