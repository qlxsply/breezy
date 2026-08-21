import type { OptionHTMLAttributes, ReactNode } from "react";

interface BzOptionProps extends Omit<OptionHTMLAttributes<HTMLOptionElement>, "value"> {
  label?: string;
  value?: string | number | null;
  children?: ReactNode;
}

export function BzOption({ label = "", value = "", children, ...rest }: BzOptionProps) {
  return (
    <option
      {...rest}
      value={String(value ?? "") || ""}
    >
      {children ?? label}
    </option>
  );
}
