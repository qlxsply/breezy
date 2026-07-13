interface BzFormItemProps {
  label?: React.ReactNode;
  meta?: React.ReactNode;
  className?: string;
  contentWidth?: number | string;
  children?: React.ReactNode;
}

function resolveCssSize(value?: number | string): string | undefined {
  if (value === undefined) return undefined;
  return typeof value === "number" ? `${value}px` : value;
}

export function BzFormItem({ label, meta, className, contentWidth, children }: BzFormItemProps) {
  return (
    <div className={["bz-form-item", className].filter(Boolean).join(" ")}>
      {label ? <label className="bz-form-item__label">{label}</label> : null}
      <div className="bz-form-item__content" style={{ width: resolveCssSize(contentWidth) }}>
        {children}
      </div>
      {meta ? <div className="bz-form-item__meta">{meta}</div> : null}
    </div>
  );
}
