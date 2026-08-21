import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BzDragHandle } from "./BzDragHandle";

describe("BzDragHandle", () => {
  it("uses the title as the accessible label", () => {
    render(<BzDragHandle title="调整字段顺序" />);

    const handle = screen.getByRole("button", { name: "调整字段顺序" });
    expect(handle).toHaveAttribute("draggable", "true");
    expect(handle).toHaveAttribute("title", "调整字段顺序");
  });

  it("preserves custom attributes and events", () => {
    const onClick = vi.fn();
    render(
      <BzDragHandle
        aria-label="移动"
        className="custom-handle"
        onClick={onClick}
      />,
    );

    const handle = screen.getByRole("button", { name: "移动" });
    expect(handle).toHaveClass("bz-drag-handle", "custom-handle");

    fireEvent.click(handle);
    expect(onClick).toHaveBeenCalledOnce();
  });
});
