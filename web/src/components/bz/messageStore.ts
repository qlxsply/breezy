import { ref } from "vue";

export type BzMessageType = "success" | "error" | "info" | "warning";

export interface BzMessageItem {
  id: number;
  content: string;
  type: BzMessageType;
}

const messages = ref<BzMessageItem[]>([]);
const timers = new Map<number, number>();
let messageIdSeed = 0;

export function pushBzMessage(
  content: string,
  type: BzMessageType = "info",
  duration = 3000,
): number {
  const id = ++messageIdSeed;
  messages.value = [...messages.value, { id, content, type }];

  if (duration > 0) {
    const timer = window.setTimeout(() => {
      removeBzMessage(id);
    }, duration);
    timers.set(id, timer);
  }

  return id;
}

export function removeBzMessage(id: number) {
  messages.value = messages.value.filter((item) => item.id !== id);
  const timer = timers.get(id);
  if (timer) {
    window.clearTimeout(timer);
    timers.delete(id);
  }
}

export function useBzMessageStore() {
  return {
    messages,
  };
}
