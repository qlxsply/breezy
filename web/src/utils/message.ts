// /src/utils/message.ts
import { ref } from "vue";

import { pushBzMessage } from "../components/bz/messageStore";

export type MessageType = "success" | "error" | "info" | "warning";

export interface MessageRecord {
  id: number;
  content: string;
  type: MessageType;
}

const latestMessage = ref<MessageRecord | null>(null);
const messageHistory = ref<MessageRecord[]>([]);
let idCounter = 0;
let clearTimer: number | null = null;

function addMessage(content: string, type: MessageType = "info", duration = 3000) {
  const id = idCounter++;
  const record: MessageRecord = { id, content, type };
  latestMessage.value = record;
  messageHistory.value = [record, ...messageHistory.value].slice(0, 10);

  pushBzMessage(content, type, duration);

  if (clearTimer) {
    window.clearTimeout(clearTimer);
  }
  if (duration > 0) {
    clearTimer = window.setTimeout(() => {
      latestMessage.value = null;
      clearTimer = null;
    }, duration);
  }
}

export const message = {
  success: (content: string) => addMessage(content, "success"),
  error: (content: string) => addMessage(content, "error"),
  info: (content: string) => addMessage(content, "info"),
  warning: (content: string) => addMessage(content, "warning"),
};

export function useMessageStore() {
  return {
    latestMessage,
    messageHistory,
  };
}
