"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { BzDropdownContext, type BzDropdownContextValue } from "./BzDropdownContext";

interface BzDropdownProps {
  children?: React.ReactNode;
  minWidth?: number;
  offset?: number;
  dropdownContent?: React.ReactNode;
}

export function BzDropdown({ children, minWidth = 120, offset = 6, dropdownContent }: BzDropdownProps) {
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [panelStyle, setPanelStyle] = useState<React.CSSProperties>({ minWidth });

  function updatePosition() {
    if (!triggerRef.current || !panelRef.current) return;
    const triggerRect = triggerRef.current.getBoundingClientRect();
    const panelRect = panelRef.current.getBoundingClientRect();
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const margin = 8;

    let left = triggerRect.right + offset;
    if (left + panelRect.width > viewportWidth - margin) {
      left = Math.min(Math.max(triggerRect.left, margin), Math.max(margin, viewportWidth - margin - panelRect.width));
    }
    if (left < margin) left = margin;
    if (left + panelRect.width > viewportWidth - margin) left = Math.max(margin, viewportWidth - margin - panelRect.width);

    const spaceBelow = viewportHeight - triggerRect.bottom - margin;
    const spaceAbove = triggerRect.top - margin;
    let top = triggerRect.bottom + offset;
    if (panelRect.height > spaceBelow && spaceAbove > spaceBelow) {
      top = triggerRect.top - offset - panelRect.height;
    }
    if (top < margin) top = margin;
    if (top + panelRect.height > viewportHeight - margin) top = Math.max(margin, viewportHeight - margin - panelRect.height);

    setPanelStyle({ position: "fixed", top: `${top}px`, left: `${left}px`, minWidth: `${minWidth}px` });
  }

  const close = useCallback(() => {
    setOpen(false);
  }, []);

  const contextValue: BzDropdownContextValue = { close };

  function toggle() {
    if (open) { close(); return; }
    setOpen(true);
    requestAnimationFrame(() => updatePosition());
  }

  useEffect(() => {
    if (!open) return;
    function onScroll() { updatePosition(); }
    function onResize() { updatePosition(); }
    window.addEventListener("scroll", onScroll, true);
    window.addEventListener("resize", onResize);
    return () => {
      window.removeEventListener("scroll", onScroll, true);
      window.removeEventListener("resize", onResize);
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;
    function handleClickOutside(event: MouseEvent) {
      if (!rootRef.current || !panelRef.current) return;
      const target = event.target as Node;
      if (!rootRef.current.contains(target) && !panelRef.current.contains(target)) {
        close();
      }
    }
    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") close();
    }
    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open, close]);

  return (
    <BzDropdownContext.Provider value={contextValue}>
      <div ref={rootRef} className="bz-dropdown">
        <div ref={triggerRef} className="bz-dropdown__trigger" onClick={toggle}>
          {children}
        </div>
        {open && createPortal(
          <div ref={panelRef} className="bz-dropdown__panel" style={panelStyle}>
            {dropdownContent}
          </div>,
          document.body,
        )}
      </div>
    </BzDropdownContext.Provider>
  );
}
