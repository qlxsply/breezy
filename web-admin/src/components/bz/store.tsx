"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

export type BzMessageType = "info" | "success" | "warning" | "error";

interface MessageItem {
  id: string;
  type: BzMessageType;
  content: string;
  duration: number;
}

interface ConfirmState {
  open: boolean;
  title: string;
  content: string;
  confirmText: string;
  cancelText: string;
  resolver: ((value: boolean) => void) | null;
}

interface BzUiContextValue {
  messages: MessageItem[];
  confirmState: ConfirmState;
  pushMessage: (type: BzMessageType, content: string, duration?: number) => void;
  removeMessage: (id: string) => void;
  confirm: (options: Partial<Omit<ConfirmState, "open" | "resolver">>) => Promise<boolean>;
  resolveConfirm: (value: boolean) => void;
}

const BzUiContext = createContext<BzUiContextValue | null>(null);

function createMessageId() {
  return `bz-msg-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export function BzUiProvider({ children }: { children: React.ReactNode }) {
  const [messages, setMessages] = useState<MessageItem[]>([]);
  const [confirmState, setConfirmState] = useState<ConfirmState>({
    open: false,
    title: "提示",
    content: "",
    confirmText: "确定",
    cancelText: "取消",
    resolver: null,
  });

  const removeMessage = useCallback((id: string) => {
    setMessages((current) => current.filter((item) => item.id !== id));
  }, []);

  const pushMessage = useCallback((type: BzMessageType, content: string, duration = 3000) => {
    const id = createMessageId();
    setMessages((current) => [...current, { id, type, content, duration }]);
  }, []);

  const confirm = useCallback((options: Partial<Omit<ConfirmState, "open" | "resolver">>) => {
    return new Promise<boolean>((resolve) => {
      setConfirmState({
        open: true,
        title: options.title ?? "提示",
        content: options.content ?? "",
        confirmText: options.confirmText ?? "确定",
        cancelText: options.cancelText ?? "取消",
        resolver: resolve,
      });
    });
  }, []);

  const resolveConfirm = useCallback((value: boolean) => {
    setConfirmState((current) => {
      current.resolver?.(value);
      return {
        ...current,
        open: false,
        resolver: null,
      };
    });
  }, []);

  const value = useMemo(
    () => ({ messages, confirmState, pushMessage, removeMessage, confirm, resolveConfirm }),
    [confirm, confirmState, messages, pushMessage, removeMessage, resolveConfirm],
  );

  return <BzUiContext.Provider value={value}>{children}</BzUiContext.Provider>;
}

function useBzUiContext() {
  const context = useContext(BzUiContext);
  if (!context) {
    throw new Error("Bz UI hooks must be used inside BzUiProvider");
  }
  return context;
}

export function useBzMessage() {
  const { pushMessage } = useBzUiContext();
  return {
    info: (content: string, duration?: number) => pushMessage("info", content, duration),
    success: (content: string, duration?: number) => pushMessage("success", content, duration),
    warning: (content: string, duration?: number) => pushMessage("warning", content, duration),
    error: (content: string, duration?: number) => pushMessage("error", content, duration),
  };
}

export function useBzConfirm() {
  const { confirm } = useBzUiContext();
  return confirm;
}

export function useBzMessagesState() {
  return useBzUiContext();
}

export function BzMessageAutoDismiss({ id, duration }: { id: string; duration: number }) {
  const { removeMessage } = useBzUiContext();

  useEffect(() => {
    const timer = window.setTimeout(() => removeMessage(id), duration);
    return () => window.clearTimeout(timer);
  }, [duration, id, removeMessage]);

  return null;
}
