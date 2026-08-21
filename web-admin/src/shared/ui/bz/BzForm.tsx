interface BzFormProps {
  inline?: boolean;
  className?: string;
  children?: React.ReactNode;
  onSubmit?: React.FormEventHandler<HTMLFormElement>;
}

export function BzForm({ inline = false, className, children, onSubmit }: BzFormProps) {
  return (
    <form
      className={["bz-form", inline ? "is-inline" : "", className].filter(Boolean).join(" ")}
      onSubmit={onSubmit}
    >
      {children}
    </form>
  );
}
