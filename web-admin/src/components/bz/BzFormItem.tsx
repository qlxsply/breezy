interface BzFormItemProps {
  label?: React.ReactNode;
  className?: string;
  children?: React.ReactNode;
}

export function BzFormItem({ label, className, children }: BzFormItemProps) {
  return (
    <div className={["bz-form-item", className].filter(Boolean).join(" ")}>
      {label ? <label className="bz-form-item__label">{label}</label> : null}
      {children}
    </div>
  );
}
