"use client";

import { BzCheckbox } from "@admin/shared/ui/bz";
import type { ReactNode } from "react";

import styles from "./TableCheckbox.module.css";

export interface TableCheckboxProps {
  value?: boolean;
  disabled?: boolean;
  className?: string;
  children?: ReactNode;
  onValueChange?: (value: boolean) => void;
}

export function TableCheckbox({
  value = false,
  disabled = false,
  className,
  children,
  onValueChange,
}: TableCheckboxProps) {
  return (
    <span
      className={[styles.control, disabled ? styles.disabled : "", className]
        .filter(Boolean)
        .join(" ")}
    >
      <BzCheckbox
        modelValue={value}
        disabled={disabled}
        onValueChange={onValueChange}
      >
        {children}
      </BzCheckbox>
    </span>
  );
}
