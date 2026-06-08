import { computed, ref } from "vue";

export interface BzConfirmOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
  closeOnOverlay?: boolean;
}

interface ConfirmTask {
  options: Required<BzConfirmOptions>;
  resolve: (result: boolean) => void;
}

const queue = ref<ConfirmTask[]>([]);
const currentTask = ref<ConfirmTask | null>(null);

const currentConfirm = computed(() => currentTask.value?.options ?? null);

function normalizeOptions(options: string | BzConfirmOptions): Required<BzConfirmOptions> {
  if (typeof options === "string") {
    return {
      title: "确认操作",
      message: options,
      confirmText: "确定",
      cancelText: "取消",
      danger: false,
      closeOnOverlay: false,
    };
  }
  return {
    title: options.title || "确认操作",
    message: options.message,
    confirmText: options.confirmText || "确定",
    cancelText: options.cancelText || "取消",
    danger: options.danger === true,
    closeOnOverlay: options.closeOnOverlay === true,
  };
}

function showNext() {
  if (currentTask.value || queue.value.length === 0) {
    return;
  }
  const [first, ...rest] = queue.value;
  queue.value = rest;
  currentTask.value = first;
}

export function bzConfirm(options: string | BzConfirmOptions): Promise<boolean> {
  return new Promise<boolean>((resolve) => {
    queue.value = [
      ...queue.value,
      {
        options: normalizeOptions(options),
        resolve,
      },
    ];
    showNext();
  });
}

export function resolveCurrentConfirm(result: boolean) {
  if (!currentTask.value) {
    return;
  }
  const task = currentTask.value;
  currentTask.value = null;
  task.resolve(result);
  showNext();
}

export function useBzConfirmStore() {
  return {
    currentConfirm,
  };
}
