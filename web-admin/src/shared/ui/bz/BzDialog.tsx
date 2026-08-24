"use client";

import { useEffect } from "react";
import { createPortal } from "react-dom";

import { BzButton } from "./BzButton";
import styles from "./BzDialog.module.css";
import { BzIconClose } from "./BzIconClose";

const openDialogStack: symbol[] = [];

function pushDialog(id: symbol) {
  if (!openDialogStack.includes(id)) {
    openDialogStack.push(id);
  }
  syncBodyOverflow();
}

function removeDialog(id: symbol) {
  const index = openDialogStack.indexOf(id);
  if (index >= 0) {
    openDialogStack.splice(index, 1);
  }
  syncBodyOverflow();
}

function topDialog() {
  return openDialogStack[openDialogStack.length - 1];
}

function syncBodyOverflow() {
  if (typeof document === "undefined") {
    return;
  }
  document.body.style.overflow = openDialogStack.length > 0 ? "hidden" : "";
}

function resolveSize(value: number | string) {
  if (typeof value === "number") {
    return `${value}px`;
  }
  if (/^\d+$/.test(value)) {
    return `${value}px`;
  }
  return value;
}

interface BzDialogProps {
  modelValue: boolean;
  title?: string;
  width?: number | string;
  maxWidth?: number | string;
  top?: string;
  closeOnOverlay?: boolean;
  closeOnClickModal?: boolean;
  closeOnEsc?: boolean;
  showClose?: boolean;
  confirmText?: string;
  cancelText?: string;
  children?: React.ReactNode;
  footer?: React.ReactNode;
  onUpdateModelValue?: (value: boolean) => void;
  onConfirm?: () => void;
  onCancel?: () => void;
  onClose?: () => void;
}

export function BzDialog({
  modelValue,
  title = "提示",
  width = 520,
  maxWidth = "min(92vw, 720px)",
  top = "",
  closeOnOverlay = true,
  closeOnClickModal,
  closeOnEsc = true,
  showClose = true,
  confirmText = "确定",
  cancelText = "取消",
  children,
  footer,
  onUpdateModelValue,
  onConfirm,
  onCancel,
  onClose,
}: BzDialogProps) {
  const instanceId = Symbol("bz-dialog-instance");

  useEffect(() => {
    if (modelValue) {
      pushDialog(instanceId);
      return () => removeDialog(instanceId);
    }
    removeDialog(instanceId);
    return undefined;
  }, [instanceId, modelValue]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (!modelValue || !closeOnEsc || topDialog() !== instanceId) {
        return;
      }
      if (event.key === "Escape") {
        event.preventDefault();
        handleCancel();
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  });

  function handleCancel() {
    onCancel?.();
    onClose?.();
    onUpdateModelValue?.(false);
  }

  if (!modelValue || typeof document === "undefined") {
    return null;
  }

  const overlayStyle = top.trim() ? { alignItems: "flex-start", paddingTop: top } : undefined;
  const dialogStyle = { width: resolveSize(width), maxWidth: resolveSize(maxWidth) };

  return createPortal(
    <div
      className={styles.overlay}
      style={overlayStyle}
      onClick={() => {
        const allowCloseOnOverlay = closeOnClickModal ?? closeOnOverlay;
        if (allowCloseOnOverlay) {
          handleCancel();
        }
      }}
    >
      <div
        className={styles.dialog}
        style={dialogStyle}
        role="dialog"
        aria-modal="true"
        onClick={(event) => event.stopPropagation()}
      >
        <header className={styles.header}>
          <div className={styles.title}>{title}</div>
          {showClose ? (
            <button
              className={styles.close}
              type="button"
              onClick={handleCancel}
            >
              <BzIconClose />
            </button>
          ) : null}
        </header>

        <section className={styles.body}>{children}</section>

        <footer className={styles.footer}>
          {footer ?? (
            <>
              <BzButton onClick={handleCancel}>{cancelText}</BzButton>
              <BzButton
                buttonType="primary"
                onClick={onConfirm}
              >
                {confirmText}
              </BzButton>
            </>
          )}
        </footer>
      </div>
    </div>,
    document.body,
  );
}
