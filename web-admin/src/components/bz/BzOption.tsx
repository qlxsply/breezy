import type { OptionHTMLAttributes, ReactNode } from "react";

interface BzOptionProps extends OptionHTMLAttributes<HTMLOptionElement> {
  label?: string;
  value?: string | number | null;
  children?: ReactNode;
}

export function BzOption({ label = "", value = "", children, ...rest }: BzOptionProps) {
  return (
    <option {...rest} value={String(value ?? "") || ""}>
      {children ?? label}
    </option>
  );
}
