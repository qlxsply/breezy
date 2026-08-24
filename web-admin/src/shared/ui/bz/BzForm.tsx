import styles from "./BzForm.module.css";

interface BzFormProps {
  inline?: boolean;
  className?: string;
  children?: React.ReactNode;
  onSubmit?: React.FormEventHandler<HTMLFormElement>;
}

export function BzForm({ inline = false, className, children, onSubmit }: BzFormProps) {
  return (
    <form
      className={[styles.form, inline ? styles.inline : "", className].filter(Boolean).join(" ")}
      onSubmit={onSubmit}
    >
      {children}
    </form>
  );
}
