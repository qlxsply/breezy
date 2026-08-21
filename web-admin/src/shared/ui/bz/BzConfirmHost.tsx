"use client";

import { BzDialog } from "./BzDialog";
import { useBzMessagesState } from "./store";

export function BzConfirmHost() {
  const { confirmState, resolveConfirm } = useBzMessagesState();

  return (
    <BzDialog
      modelValue={confirmState.open}
      title={confirmState.title}
      confirmText={confirmState.confirmText}
      cancelText={confirmState.cancelText}
      onConfirm={() => resolveConfirm(true)}
      onCancel={() => resolveConfirm(false)}
      onUpdateModelValue={(value) => {
        if (!value) {
          resolveConfirm(false);
        }
      }}
    >
      <div style={{ color: "#475569", lineHeight: 1.7 }}>{confirmState.content}</div>
    </BzDialog>
  );
}
