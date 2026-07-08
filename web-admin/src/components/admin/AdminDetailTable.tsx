"use client";

import { Fragment, type ReactNode, useEffect, useMemo, useState } from "react";

type ValueAlign = "left" | "center" | "right";
type FieldSpan = number | "full";

export interface AdminDetailField {
  label: ReactNode;
  value: ReactNode;
  span?: FieldSpan;
  align?: ValueAlign;
  multiline?: boolean;
}

export interface AdminDetailSection {
  title: ReactNode;
  fields: AdminDetailField[];
}

interface AdminDetailTableProps {
  sections: AdminDetailSection[];
  variant?: "sectioned" | "plain";
}

interface ResolvedField extends AdminDetailField {
  spanPairs: number;
}

export function AdminDetailTable({ sections, variant = "sectioned" }: AdminDetailTableProps) {
  const pairCount = useResponsivePairCount();

  const resolvedSections = useMemo(
    () => sections.map((section) => ({ ...section, rows: buildRows(section.fields, pairCount) })),
    [pairCount, sections],
  );

  return (
    <div className={["admin-detail-table-stack", variant === "plain" ? "is-plain" : ""].filter(Boolean).join(" ")}>
      {resolvedSections.map((section) => (
        <section
          key={String(section.title)}
          className={[
            "admin-detail-table-section",
            variant === "plain" ? "admin-detail-table-section--plain" : "",
          ]
            .filter(Boolean)
            .join(" ")}
        >
          {variant === "sectioned" ? <div className="admin-detail-table-section__title">{section.title}</div> : null}
          <table className="admin-detail-table">
            <colgroup>
              {Array.from({ length: pairCount }).flatMap((_, index) => [
                <col key={`label-${index}`} className="admin-detail-table__col--label" />,
                <col key={`value-${index}`} />,
              ])}
            </colgroup>
            <tbody>
              {section.rows.map((row, rowIndex) => (
                <tr key={`${String(section.title)}-${rowIndex}`}>
                  {row.map((field, fieldIndex) => (
                    <Fragment key={`${String(section.title)}-${rowIndex}-${fieldIndex}`}>
                      <th
                        className="admin-detail-table__cell-label"
                        scope="row"
                      >
                        {field.label}
                      </th>
                      <td
                        colSpan={field.spanPairs * 2 - 1}
                        className={[
                          "admin-detail-table__cell-value",
                          field.align === "center"
                            ? "is-center"
                            : field.align === "right"
                              ? "is-right"
                              : "",
                        ]
                          .filter(Boolean)
                          .join(" ")}
                      >
                        <div
                          className={[
                            "admin-detail-table__value-wrap",
                            field.multiline ? "is-multiline" : "",
                          ]
                            .filter(Boolean)
                            .join(" ")}
                        >
                          {field.value}
                        </div>
                      </td>
                    </Fragment>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ))}
    </div>
  );
}

function useResponsivePairCount(): number {
  const [pairCount, setPairCount] = useState(2);

  useEffect(() => {
    function update() {
      setPairCount(resolvePairCount(window.innerWidth));
    }

    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, []);

  return pairCount;
}

function resolvePairCount(width: number): number {
  if (width < 768) return 1;
  if (width < 1280) return 2;
  return 3;
}

function buildRows(fields: AdminDetailField[], pairCount: number): ResolvedField[][] {
  const rows: ResolvedField[][] = [];
  let currentRow: ResolvedField[] = [];
  let usedPairs = 0;

  function flush() {
    if (!currentRow.length) return;
    const remainingPairs = pairCount - usedPairs;
    if (remainingPairs > 0) {
      currentRow[currentRow.length - 1].spanPairs += remainingPairs;
    }
    rows.push(currentRow);
    currentRow = [];
    usedPairs = 0;
  }

  for (const field of fields) {
    const desiredPairs = resolveSpanPairs(field.span, pairCount);
    if (currentRow.length > 0 && usedPairs + desiredPairs > pairCount) {
      flush();
    }

    currentRow.push({ ...field, spanPairs: desiredPairs });
    usedPairs += desiredPairs;

    if (usedPairs >= pairCount) {
      flush();
    }
  }

  flush();
  return rows;
}

function resolveSpanPairs(span: FieldSpan | undefined, pairCount: number): number {
  if (span === "full") return pairCount;
  if (!Number.isFinite(span)) return 1;
  return Math.max(1, Math.min(pairCount, Number(span)));
}
