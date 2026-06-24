"use client";

import { useEffect, useRef, useState } from "react";

export function useAdminQueryPanelLayout(queryPanelVisible: boolean) {
  const queryCardRef = useRef<HTMLDivElement | null>(null);
  const queryGridRef = useRef<HTMLFormElement | null>(null);
  const [queryExpanded, setQueryExpanded] = useState(false);
  const [querySingleRow, setQuerySingleRow] = useState(true);

  useEffect(() => {
    if (!queryPanelVisible) {
      return;
    }

    const card = queryCardRef.current;
    const grid = queryGridRef.current;
    if (!card || !grid) {
      return;
    }

    let frame = 0;

    const refreshCollapseState = () => {
      cancelAnimationFrame(frame);

      frame = window.requestAnimationFrame(() => {
        const fields = Array.from(grid.querySelectorAll<HTMLElement>(".admin-query-field"));
        const actions = grid.querySelector<HTMLElement>(".admin-query-actions");
        const items = actions ? [...fields, actions] : fields;

        if (items.length === 0) {
          card.style.removeProperty("--admin-query-collapsed-height");
          card.style.removeProperty("--admin-query-expanded-height");
          setQuerySingleRow(true);
          return;
        }

        const previousMaxHeight = grid.style.maxHeight;
        const previousActionGridRow = actions?.style.gridRow || "";
        const previousActionGridColumn = actions?.style.gridColumn || "";

        grid.style.maxHeight = "none";
        if (actions) {
          actions.style.gridRow = "auto";
          actions.style.gridColumn = "auto";
        }

        const rowTops = [...new Set(items.map((item) => Math.round(item.offsetTop)))].sort((left, right) => left - right);
        const firstRowTop = rowTops[0] || 0;
        const firstRowItems = items.filter((item) => Math.round(item.offsetTop) === firstRowTop);
        const firstRowBottom = Math.max(...firstRowItems.map((item) => item.offsetTop + item.offsetHeight), 0);
        const collapsedHeight = Math.max(firstRowBottom - firstRowTop, 0);
        const expandedHeight = grid.scrollHeight;

        grid.style.maxHeight = previousMaxHeight;
        if (actions) {
          actions.style.gridRow = previousActionGridRow;
          actions.style.gridColumn = previousActionGridColumn;
        }

        card.style.setProperty("--admin-query-collapsed-height", `${collapsedHeight}px`);
        card.style.setProperty("--admin-query-expanded-height", `${expandedHeight}px`);

        const nextSingleRow = rowTops.length <= 1;
        setQuerySingleRow(nextSingleRow);
        if (nextSingleRow) {
          setQueryExpanded(false);
        }
      });
    };

    refreshCollapseState();

    const observer = new ResizeObserver(() => {
      refreshCollapseState();
    });

    observer.observe(grid);
    window.addEventListener("resize", refreshCollapseState);

    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
      window.removeEventListener("resize", refreshCollapseState);
    };
  }, [queryPanelVisible]);

  return {
    queryCardRef,
    queryGridRef,
    queryExpanded,
    setQueryExpanded,
    querySingleRow,
  };
}
