import type { ReactNode } from "react";

import { BzButton } from "../bz/BzButton";
import { AdminDetailTable, type AdminDetailSection } from "./AdminDetailTable";
import { AdminEntityDrawer } from "./AdminEntityDrawer";

interface AdminDetailDrawerTemplateProps {
  open: boolean;
  title: string;
  sections: AdminDetailSection[];
  width?: string;
  loading?: boolean;
  className?: string;
  footer?: ReactNode;
  plain?: boolean;
  onClose: () => void;
}

export function AdminDetailDrawerTemplate({
  open,
  title,
  sections,
  width = "960px",
  loading = false,
  className,
  footer,
  plain = true,
  onClose,
}: AdminDetailDrawerTemplateProps) {
  return (
    <AdminEntityDrawer
      open={open}
      title={title}
      width={width}
      loading={loading}
      className={className}
      onClose={onClose}
      footer={footer ?? <BzButton onClick={onClose}>关闭</BzButton>}
    >
      <AdminDetailTable sections={sections} variant={plain ? "plain" : "sectioned"} />
    </AdminEntityDrawer>
  );
}
