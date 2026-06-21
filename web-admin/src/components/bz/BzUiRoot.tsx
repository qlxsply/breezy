"use client";

import { BzConfirmHost } from "./BzConfirmHost";
import { BzFeedbackBridge } from "./BzFeedbackBridge";
import { BzMessageHost } from "./BzMessageHost";
import { BzUiProvider } from "./store";

export function BzUiRoot({ children }: { children: React.ReactNode }) {
  return (
    <BzUiProvider>
      <BzFeedbackBridge />
      {children}
      <BzMessageHost />
      <BzConfirmHost />
    </BzUiProvider>
  );
}
