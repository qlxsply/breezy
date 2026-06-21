"use client";

import { forwardRef, useImperativeHandle, useRef } from "react";

import { BzIconClose } from "./BzIconClose";

interface BzInputProps {
  modelValue?: string | number | null;
  placeholder?: string;
  clearable?: boolean;
  disabled?: boolean;
  readOnly?: boolean;
  type?: "text" | "password" | "number" | "search";
  className?: string;
  onValueChange?: (value: string) => void;
  onChange?: (value: string) => void;
  onKeyUp?: React.KeyboardEventHandler<HTMLInputElement>;
  onFocus?: React.FocusEventHandler<HTMLInputElement>;
  onBlur?: React.FocusEventHandler<HTMLInputElement>;
}

export interface BzInputRef {
  focus: () => void;
  blur: () => void;
}

export const BzInput = forwardRef<BzInputRef, BzInputProps>(function BzInput(
  {
    modelValue = "",
    placeholder = "",
    clearable = false,
    disabled = false,
    readOnly = false,
    type = "text",
    className,
    onValueChange,
    onChange,
    onKeyUp,
    onFocus,
    onBlur,
  },
  ref,
) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const textValue = String(modelValue ?? "");
  const hasValue = textValue.length > 0;

  useImperativeHandle(ref, () => ({
    focus: () => inputRef.current?.focus(),
    blur: () => inputRef.current?.blur(),
  }));

  return (
    <div className={["bz-input", disabled ? "is-disabled" : "", className].filter(Boolean).join(" ")}>
      <input
        ref={inputRef}
        className="bz-input__inner"
        value={textValue}
        placeholder={placeholder}
        disabled={disabled}
        readOnly={readOnly}
        type={type}
        onInput={(event) => onValueChange?.(event.currentTarget.value)}
        onChange={(event) => onChange?.(event.currentTarget.value)}
        onKeyUp={onKeyUp}
        onFocus={onFocus}
        onBlur={onBlur}
      />
      {clearable && hasValue && !disabled && !readOnly ? (
        <button
          className="bz-input__clear"
          type="button"
          onClick={() => {
            onValueChange?.("");
            onChange?.("");
          }}
        >
          <BzIconClose size={20} />
        </button>
      ) : null}
    </div>
  );
});
