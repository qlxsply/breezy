import styles from "./BzFormItem.module.css";

interface BzFormItemProps {
  label?: React.ReactNode;
  meta?: React.ReactNode;
  className?: string;
  contentClassName?: string;
  contentWidth?: number | string;
  children?: React.ReactNode;
}

function resolveCssSize(value?: number | string): string | undefined {
  if (value === undefined) return undefined;
  return typeof value === "number" ? `${value}px` : value;
}

export function BzFormItem({
  label,
  meta,
  className,
  contentClassName,
  contentWidth,
  children,
}: BzFormItemProps) {
  const needsContentWrap = meta !== undefined || contentWidth !== undefined;

  return (
    <div className={[styles.item, className].filter(Boolean).join(" ")}>
      {label ? <label className={styles.label}>{label}</label> : null}
      {needsContentWrap ? (
        <div
          className={[styles.content, contentClassName].filter(Boolean).join(" ")}
          style={{ width: resolveCssSize(contentWidth) }}
        >
          {children}
        </div>
      ) : (
        children
      )}
      {meta ? <div className={styles.meta}>{meta}</div> : null}
    </div>
  );
}
