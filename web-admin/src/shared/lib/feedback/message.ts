export type MessageType = "success" | "error" | "info" | "warning";

type MessageHandler = (content: string, type: MessageType, duration?: number) => void;

let messageHandler: MessageHandler | null = null;

export function registerMessageHandler(handler: MessageHandler | null): void {
  messageHandler = handler;
}

function dispatchMessage(content: string, type: MessageType, duration = 3000): void {
  if (messageHandler) {
    messageHandler(content, type, duration);
    return;
  }

  const logger = type === "error" ? console.error : console.warn;
  logger(`[message:${type}] ${content}`);
}

export const message = {
  success: (content: string, duration?: number) => dispatchMessage(content, "success", duration),
  error: (content: string, duration?: number) => dispatchMessage(content, "error", duration),
  info: (content: string, duration?: number) => dispatchMessage(content, "info", duration),
  warning: (content: string, duration?: number) => dispatchMessage(content, "warning", duration),
};
