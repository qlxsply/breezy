interface BzLoadingProps {
  loading?: boolean;
  children?: React.ReactNode;
  text?: string;
  className?: string;
}

export function BzLoading({ loading = false, children, text, className }: BzLoadingProps) {
  return (
    <div className={["bz-loading-container", className].filter(Boolean).join(" ")}>
      {children}
      {loading ? (
        <div
          className="bz-loading-mask"
          aria-live="polite"
        >
          <div>
            <div
              className="bz-loading-spinner"
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
