import { pushBzMessage } from "../components/bz/messageStore";

export type MessageType = "success" | "error" | "info" | "warning";

function addMessage(content: string, type: MessageType = "info", duration = 3000) {
  pushBzMessage(content, type, duration);
}

export const message = {
  success: (content: string) => addMessage(content, "success"),
  error: (content: string) => addMessage(content, "error"),
  info: (content: string) => addMessage(content, "info"),
  warning: (content: string) => addMessage(content, "warning"),
};
