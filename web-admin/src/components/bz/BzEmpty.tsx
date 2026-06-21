interface BzEmptyProps {
  description?: string;
  className?: string;
}

export function BzEmpty({ description = "暂无数据", className }: BzEmptyProps) {
  return (
    <div className={["bz-empty", className].filter(Boolean).join(" ")}>
      <div className="bz-empty__icon" aria-hidden="true" />
      <div className="bz-empty__description">{description}</div>
    </div>
  );
}
