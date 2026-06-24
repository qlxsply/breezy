"use client";

import { type MouseEvent, type ReactNode,useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";

type PopoverPlacement = "top" | "bottom";

interface BzOverflowTooltipProps {
  text: string;
  children: ReactNode;
  className?: string;
  maxWidth?: number;
}

interface PopoverState {
  visible: boolean;
  top: number;
  left: number;
  placement: PopoverPlacement;
  ready: boolean;
}

const VIEWPORT_PADDING = 8;
const POPOVER_GAP = 10;

let activePopoverCloser: (() => void) | null = null;

export function BzOverflowTooltip({
  text,
  children,
  className,
  maxWidth = 560,
}: BzOverflowTooltipProps) {
  const triggerRef = useRef<HTMLSpanElement | null>(null);
  const popoverRef = useRef<HTMLDivElement | null>(null);
  const [mounted, setMounted] = useState(false);
  const [copied, setCopied] = useState(false);
  const [popover, setPopover] = useState<PopoverState>({
    visible: false,
    top: 0,
    left: 0,
    placement: "bottom",
    ready: false,
  });

  const normalizedText = useMemo(() => text || "-", [text]);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!popover.visible) {
      return;
    }

    const update = () => {
      updatePosition();
    };

    window.addEventListener("resize", update);
    window.addEventListener("scroll", update, true);
    return () => {
      window.removeEventListener("resize", update);
      window.removeEventListener("scroll", update, true);
    };
  }, [popover.visible]);

  useEffect(
    () => {
      if (!popover.visible) {
        return;
      }

      const handlePointerDown = (event: MouseEvent | globalThis.MouseEvent) => {
        const trigger = triggerRef.current;
        const content = popoverRef.current;
        const target = event.target as Node | null;
        if (!target) {
          return;
        }
        if (trigger?.contains(target) || content?.contains(target)) {
          return;
        }
        setPopover((current) => ({ ...current, visible: false, ready: false }));
        setCopied(false);
      };

      const handleEscape = (event: KeyboardEvent) => {
        if (event.key !== "Escape") {
          return;
        }
        setPopover((current) => ({ ...current, visible: false, ready: false }));
        setCopied(false);
      };

      document.addEventListener("mousedown", handlePointerDown);
      document.addEventListener("keydown", handleEscape);
      return () => {
        document.removeEventListener("mousedown", handlePointerDown);
        document.removeEventListener("keydown", handleEscape);
      };
    },
    [popover.visible],
  );

  useEffect(() => {
    return () => {
      if (activePopoverCloser === hidePopover) {
        activePopoverCloser = null;
      }
    };
  });

  function hasOverflow(): boolean {
    const element = triggerRef.current?.firstElementChild as HTMLElement | null;
    if (!element) {
      return false;
    }
    return element.scrollWidth > element.clientWidth || element.scrollHeight > element.clientHeight;
  }

  function updatePosition() {
    const trigger = triggerRef.current;
    const content = popoverRef.current;
    if (!trigger || !content) {
      return;
    }

    const rect = trigger.getBoundingClientRect();
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const popoverWidth = Math.min(content.offsetWidth || maxWidth, viewportWidth - VIEWPORT_PADDING * 2);
    const popoverHeight = content.offsetHeight || 0;
    const topCandidate = rect.top - popoverHeight - POPOVER_GAP;
    const bottomCandidate = rect.bottom + POPOVER_GAP;
    const canShowTop = topCandidate >= VIEWPORT_PADDING;
    const canShowBottom = bottomCandidate + popoverHeight <= viewportHeight - VIEWPORT_PADDING;

    const placement: PopoverPlacement = !canShowBottom && canShowTop ? "top" : "bottom";
    const top =
      placement === "bottom"
        ? Math.min(viewportHeight - popoverHeight - VIEWPORT_PADDING, bottomCandidate)
        : Math.max(VIEWPORT_PADDING, topCandidate);
    const left = Math.max(
      VIEWPORT_PADDING,
      Math.min(rect.left, viewportWidth - popoverWidth - VIEWPORT_PADDING),
    );

    setPopover((current) => ({
      ...current,
      top,
      left,
      placement,
      ready: true,
    }));
  }

  function hidePopover() {
    setPopover((current) => ({ ...current, visible: false, ready: false }));
    setCopied(false);
    if (activePopoverCloser === hidePopover) {
      activePopoverCloser = null;
    }
  }

  function handleTriggerClick(event: MouseEvent<HTMLSpanElement>) {
    event.stopPropagation();
    event.preventDefault();
    const selectedText = window.getSelection()?.toString() ?? "";
    if (selectedText.trim()) {
      return;
    }
    if (!hasOverflow()) {
      hidePopover();
      return;
    }
    if (popover.visible) {
      hidePopover();
      return;
    }
    if (activePopoverCloser && activePopoverCloser !== hidePopover) {
      activePopoverCloser();
    }
    activePopoverCloser = hidePopover;
    setPopover((current) => ({ ...current, visible: true, ready: false }));
    requestAnimationFrame(() => {
      updatePosition();
    });
  }

  async function handleCopy() {
    const success = await copyText(normalizedText);
    if (!success) {
      return;
    }
    setCopied(true);
    window.setTimeout(() => {
      setCopied(false);
    }, 1200);
  }

  return (
    <>
      <span
        ref={triggerRef}
        className={["bz-overflow-tooltip__trigger", className].filter(Boolean).join(" ")}
        onClick={handleTriggerClick}
      >
        {children}
      </span>
      {mounted && popover.visible
        ? createPortal(
            <div
              ref={popoverRef}
              className={["bz-overflow-tooltip__popover", `is-${popover.placement}`].join(" ")}
              style={{
                top: `${popover.top}px`,
                left: `${popover.left}px`,
                maxWidth: `${maxWidth}px`,
                opacity: popover.ready ? 1 : 0,
                pointerEvents: popover.ready ? "auto" : "none",
              }}
            >
              <div className="bz-overflow-tooltip__content">{normalizedText}</div>
              <button
                type="button"
                className="bz-overflow-tooltip__copy"
                aria-label="复制全部内容"
                onClick={(event) => {
                  event.stopPropagation();
                  void handleCopy();
                }}
              >
                <span className="bz-overflow-tooltip__glyph" aria-hidden="true">
                  {copied ? <CheckIcon /> : <CopyIcon />}
                </span>
              </button>
            </div>,
            document.body,
          )
        : null}
    </>
  );
}

async function copyText(text: string): Promise<boolean> {
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // ignore and fallback
    }
  }

  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "true");
  textarea.style.position = "fixed";
  textarea.style.left = "-9999px";
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand("copy");
  document.body.removeChild(textarea);
  return copied;
}

function CopyIcon() {
  return (
    <svg className="bz-overflow-tooltip__svg is-copy" viewBox="0 0 24 24" fill="none">
      <rect x="9" y="9" width="10" height="10" rx="2" stroke="currentColor" strokeWidth="1.7" />
      <path
        d="M7 15H6C4.9 15 4 14.1 4 13V6C4 4.9 4.9 4 6 4H13C14.1 4 15 4.9 15 6V7"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg className="bz-overflow-tooltip__svg is-check" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="8" stroke="currentColor" strokeWidth="1.7" />
      <path
        d="M8.5 12.4L10.9 14.8L15.8 9.8"
        stroke="currentColor"
        strokeWidth="1.9"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
