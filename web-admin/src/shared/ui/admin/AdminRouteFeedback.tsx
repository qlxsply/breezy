import styles from "./AdminRouteFeedback.module.css";

export function AdminRouteFeedback({
  badge,
  title,
  description,
  action,
}: {
  badge: string;
  title: string;
  description: string;
  action?: React.ReactNode;
}) {
  return (
    <div className={styles.placeholder}>
      <section className={styles.card}>
        <span className={styles.meta}>{badge}</span>
        <h1>{title}</h1>
        <p>{description}</p>
        {action ? <div style={{ marginTop: 16 }}>{action}</div> : null}
      </section>
    </div>
  );
}
