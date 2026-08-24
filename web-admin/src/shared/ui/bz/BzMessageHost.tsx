"use client";

import { BzButton } from "./BzButton";
import { BzIconClose } from "./BzIconClose";
import styles from "./BzMessageHost.module.css";
import { BzMessageAutoDismiss, useBzMessagesState } from "./store";

export function BzMessageHost() {
  const { messages, removeMessage } = useBzMessagesState();

  if (messages.length === 0) {
    return null;
  }

  return (
    <div
      className={styles.host}
      aria-live="polite"
    >
      {messages.map((item) => (
        <div
          key={item.id}
          className={[styles.message, styles[item.type]].filter(Boolean).join(" ")}
        >
          <BzMessageAutoDismiss
            id={item.id}
            duration={item.duration}
          />
          <div className={styles.content}>{item.content}</div>
          <BzButton
            className={styles.close}
            link
            onClick={() => removeMessage(item.id)}
          >
            <BzIconClose />
          </BzButton>
        </div>
      ))}
    </div>
  );
}
