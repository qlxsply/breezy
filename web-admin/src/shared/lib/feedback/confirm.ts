export interface ConfirmOptions {
  title?: string;
  content?: string;
  confirmText?: string;
  cancelText?: string;
}

type ConfirmHandler = (options: ConfirmOptions) => Promise<boolean>;

let confirmHandler: ConfirmHandler | null = null;

export function registerConfirmHandler(handler: ConfirmHandler | null): void {
  confirmHandler = handler;
}

export function bzConfirm(options: ConfirmOptions): Promise<boolean> {
  if (confirmHandler) {
    return confirmHandler(options);
  }

  if (typeof window !== "undefined") {
    // eslint-disable-next-line no-alert
    return Promise.resolve(window.confirm(options.content || options.title || "确认继续吗？"));
  }

  return Promise.resolve(false);
}
