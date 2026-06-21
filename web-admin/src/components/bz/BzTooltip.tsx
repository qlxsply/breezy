import type { ReactNode } from "react";

interface BzTooltipProps {
  content?: string;
  children: ReactNode;
}

export function BzTooltip({ content = "", children }: BzTooltipProps) {
  return <span title={content || undefined}>{children}</span>;
}
