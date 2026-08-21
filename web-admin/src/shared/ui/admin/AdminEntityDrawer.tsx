"use client";

import { BzLoading } from "@admin/shared/ui/bz";
import type { ReactNode } from "react";

interface AdminEntityDrawerProps {
  open: boolean;
  title: string;
  width?: string;
  loading?: boolean;
  closeDisabled?: boolean;
  className?: string;
  children?: ReactNode;
  footer?: ReactNode;
  onClose: () => void;
}

export function AdminEntityDrawer({
  open,
  title,
  width = "960px",
  loading = false,
  closeDisabled = false,
  className,
  children,
  footer,
  onClose,
}: AdminEntityDrawerProps) {
  if (!open) return null;

  return (
    <div className="admin-entity-drawer">
      <button
        className="admin-entity-drawer__mask"
        type="button"
        aria-label="关闭抽屉"
        disabled={closeDisabled}
        onClick={onClose}
      />
      <section
        className={["admin-entity-drawer__panel", className].filter(Boolean).join(" ")}
        style={{ width, maxWidth: "100vw" }}
      >
        <header className="admin-entity-drawer__header">
          <div className="admin-entity-drawer__title-wrap">
            <div className="admin-entity-drawer__title">{title}</div>
          </div>
        </header>

        <div className="admin-entity-drawer__body">
          <BzLoading loading={loading}>{children}</BzLoading>
        </div>

        {footer ? <footer className="admin-entity-drawer__footer">{footer}</footer> : null}
      </section>
    </div>
  );
}
