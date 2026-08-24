"use client";

import { useAdminQueryPanelLayout } from "@admin/shared/hooks/useAdminQueryPanelLayout";
import type { FormEvent, ReactNode } from "react";

import { BzButton } from "../bz/BzButton";
import { BzFormItem } from "../bz/BzFormItem";
import styles from "./AdminSearchForm.module.css";

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
        styles.layout,
        querySingleRow ? styles.singleRow : queryExpanded ? styles.expanded : styles.collapsed,
        className,
      ]
        .filter(Boolean)
        .join(" ")}
    >
      <form
        ref={queryGridRef}
        className={styles.grid}
        onSubmit={submit}
      >
        {children}
        <div className={styles.actions}>
          <BzButton
            className={styles.secondary}
            disabled={disabled}
            onClick={onReset}
          >
            重置
          </BzButton>
          <BzButton
            className={styles.primary}
            buttonType="primary"
            nativeType="submit"
            disabled={disabled}
          >
            搜索
          </BzButton>
          {!querySingleRow ? (
            <button
              className={styles.toggle}
              type="button"
              aria-expanded={queryExpanded}
              onClick={() => setQueryExpanded((value) => !value)}
            >
              <span>{queryExpanded ? "收起" : "展开"}</span>
              <i className={`${styles.toggleIcon} ${queryExpanded ? styles.up : styles.down}`} />
            </button>
          ) : null}
        </div>
      </form>
    </div>
  );
}

export function AdminSearchField({ label, children }: { label: ReactNode; children: ReactNode }) {
  return (
    <BzFormItem className={styles.field}>
      <div className={styles.label}>{label}</div>
      <div className={styles.control}>{children}</div>
    </BzFormItem>
  );
}
