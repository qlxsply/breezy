"use client";

import { useAdminQueryPanelLayout } from "@admin/shared/hooks/useAdminQueryPanelLayout";
import type { FormEvent, ReactNode } from "react";

import { BzButton } from "../bz/BzButton";
import { BzFormItem } from "../bz/BzFormItem";

interface AdminSearchFormProps {
  visible: boolean;
  children: ReactNode;
  onSubmit: () => void;
  onReset: () => void;
  disabled?: boolean;
  className?: string;
}

export function AdminSearchForm({
  visible,
  children,
  onSubmit,
  onReset,
  disabled = false,
  className,
}: AdminSearchFormProps) {
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(visible);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit();
  }

  return (
    <div
      ref={queryCardRef}
      className={[
        "admin-query-layout",
        querySingleRow ? "is-single-row" : queryExpanded ? "is-expanded" : "is-collapsed",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
    >
      <form
        ref={queryGridRef}
        className="bz-form admin-query-grid"
        onSubmit={submit}
      >
        {children}
        <div className="admin-query-actions">
          <BzButton
            className="admin-filter-secondary"
            disabled={disabled}
            onClick={onReset}
          >
            重置
          </BzButton>
          <BzButton
            className="admin-filter-primary"
            buttonType="primary"
            nativeType="submit"
            disabled={disabled}
          >
            搜索
          </BzButton>
          {!querySingleRow ? (
            <button
              className="admin-filter-toggle"
              type="button"
              aria-expanded={queryExpanded}
              onClick={() => setQueryExpanded((value) => !value)}
            >
              <span>{queryExpanded ? "收起" : "展开"}</span>
              <i className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`} />
            </button>
          ) : null}
        </div>
      </form>
    </div>
  );
}

export function AdminSearchField({ label, children }: { label: ReactNode; children: ReactNode }) {
  return (
    <BzFormItem className="admin-query-field">
      <div className="admin-query-field__label">{label}</div>
      <div className="admin-query-field__control">{children}</div>
    </BzFormItem>
  );
}
