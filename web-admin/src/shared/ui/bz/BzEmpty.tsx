import styles from "./BzEmpty.module.css";

interface BzEmptyProps {
  description?: string;
  className?: string;
}

export function BzEmpty({ description = "暂无数据", className }: BzEmptyProps) {
  return (
    <div className={[styles.empty, className].filter(Boolean).join(" ")}>
      <div
        className={styles.icon}
        aria-hidden="true"
      />
      <div className={styles.description}>{description}</div>
    </div>
  );
}
