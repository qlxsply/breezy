"use client";

import { registerConfirmHandler } from "@admin/core/confirm";
import { registerMessageHandler } from "@admin/core/message";
import { useEffect } from "react";

import { useBzConfirm, useBzMessage } from "./store";

export function BzFeedbackBridge() {
  const message = useBzMessage();
  const confirm = useBzConfirm();

  useEffect(() => {
    registerMessageHandler((content, type, duration) => {
      if (type === "success") {
        message.success(content, duration);
        return;
      }
      if (type === "warning") {
        message.warning(content, duration);
        return;
      }
      if (type === "error") {
        message.error(content, duration);
        return;
      }
      message.info(content, duration);
    });

    registerConfirmHandler((options) => confirm(options));

    return () => {
      registerMessageHandler(null);
      registerConfirmHandler(null);
    };
  }, [confirm, message]);

  return null;
}
