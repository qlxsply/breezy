import styles from "./BzLoading.module.css";

interface BzLoadingProps {
  loading?: boolean;
  children?: React.ReactNode;
  text?: string;
  className?: string;
}

export function BzLoading({ loading = false, children, text, className }: BzLoadingProps) {
  return (
    <div className={[styles.container, className].filter(Boolean).join(" ")}>
      {children}
      {loading ? (
        <div
          className={styles.mask}
          aria-live="polite"
        >
          <div>
            <div
              className={styles.spinner}
              aria-hidden="true"
            />
            {text ? (
              <div style={{ marginTop: 8, color: "#475569", fontSize: 12 }}>{text}</div>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
