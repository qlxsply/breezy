"use client";

import { BzLoading } from "@admin/components/bz";
import type { ReactNode } from "react";

interface AdminEntityDrawerProps {
  open: boolean;
  title: string;
  width?: string;
  loading?: boolean;
  children?: ReactNode;
  footer?: ReactNode;
  onClose: () => void;
}

export function AdminEntityDrawer({
  open,
  title,
  width = "960px",
  loading = false,
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
        onClick={onClose}
      />
      <section
        className="admin-entity-drawer__panel"
        style={{ width }}
        >
        <header className="admin-entity-drawer__header">
          <div className="admin-entity-drawer__title-wrap">
            <div className="admin-entity-drawer__title">{title}</div>
          </div>
          <button
            className="admin-entity-drawer__close"
            type="button"
            aria-label="关闭抽屉"
            onClick={onClose}
          >
            <span aria-hidden="true">x</span>
          </button>
        </header>

        <div className="admin-entity-drawer__body">
          <BzLoading loading={loading}>{children}</BzLoading>
        </div>

        {footer ? <footer className="admin-entity-drawer__footer">{footer}</footer> : null}
      </section>
    </div>
  );
}
