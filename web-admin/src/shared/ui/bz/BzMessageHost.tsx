"use client";

import { BzButton } from "./BzButton";
import { BzIconClose } from "./BzIconClose";
import { BzMessageAutoDismiss, useBzMessagesState } from "./store";

export function BzMessageHost() {
  const { messages, removeMessage } = useBzMessagesState();

  if (messages.length === 0) {
    return null;
  }

  return (
    <div
      className="bz-message-host"
      aria-live="polite"
    >
      {messages.map((item) => (
        <div
          key={item.id}
          className={["bz-message", `bz-message--${item.type}`].join(" ")}
        >
          <BzMessageAutoDismiss
            id={item.id}
            duration={item.duration}
          />
          <div className="bz-message__content">{item.content}</div>
          <BzButton
            className="bz-message__close"
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
