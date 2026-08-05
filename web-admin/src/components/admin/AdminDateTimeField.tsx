"use client";

import { BzButton } from "@admin/components/bz/BzButton";
import {
  formatDateTimeInputValue,
  getUserDateTimeFormatPattern,
  getUserDateTimePrecision,
  getUserTimeZone,
} from "@admin/core/formatter";
import { usePersonalizedConfigs } from "@admin/core/registry/auth-registry";
import type { CSSProperties } from "react";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

interface AdminDateTimeFieldProps {
  modelValue: string;
  disabled?: boolean;
  placeholder?: string;
  onValueChange?: (value: string) => void;
}

interface DateTimeParts {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
  second: number;
}

interface CalendarDay {
  key: string;
  year: number;
  month: number;
  day: number;
  currentMonth: boolean;
}

const TIME_ROW_HEIGHT = 36;
const HOURS = Array.from({ length: 24 }, (_, index) => index);
const MINUTES = Array.from({ length: 60 }, (_, index) => index);
const SECONDS = Array.from({ length: 60 }, (_, index) => index);
const WEEKDAYS = ["一", "二", "三", "四", "五", "六", "日"];

export function AdminDateTimeField({
  modelValue,
  disabled = false,
  placeholder = "请选择日期时间",
  onValueChange,
}: AdminDateTimeFieldProps) {
  usePersonalizedConfigs();
  const rootRef = useRef<HTMLDivElement | null>(null);
  const triggerRef = useRef<HTMLDivElement | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<DateTimeParts>(() => parseCanonical(modelValue) || currentZonedParts());
  const [viewYear, setViewYear] = useState(draft.year);
  const [viewMonth, setViewMonth] = useState(draft.month);
  const [dateText, setDateText] = useState("");
  const [timeText, setTimeText] = useState("");
  const [dateInvalid, setDateInvalid] = useState(false);
  const [timeInvalid, setTimeInvalid] = useState(false);
  const [panelStyle, setPanelStyle] = useState<CSSProperties>({});
  const pattern = getUserDateTimeFormatPattern();
  const precision = getUserDateTimePrecision();
  const datePattern = pattern.split(/\s+/)[0] || "yyyy-MM-dd";
  const timePattern = precision === "second" ? "HH:mm:ss" : "HH:mm";
  const displayValue = formatDateTimeInputValue(modelValue);
  const calendarDays = buildCalendarDays(viewYear, viewMonth);

  function syncTexts(value: DateTimeParts) {
    setDateText(formatDate(value, datePattern));
    setTimeText(formatTime(value, precision));
    setDateInvalid(false);
    setTimeInvalid(false);
  }

  function updatePanelPosition() {
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    const margin = 12;
    const width = Math.min(620, window.innerWidth - margin * 2);
    const estimatedHeight = 445;
    const left = Math.max(margin, Math.min(rect.left, window.innerWidth - width - margin));
    const top = rect.bottom + 4 + estimatedHeight <= window.innerHeight
      ? rect.bottom + 4
      : Math.max(margin, rect.top - estimatedHeight - 4);
    setPanelStyle({ position: "fixed", left, top, width });
  }

  useEffect(() => {
    if (open) return;
    const committed = parseCanonical(modelValue);
    if (committed) setDraft(committed);
  }, [modelValue, open]);

  useEffect(() => {
    if (!open) return;
    requestAnimationFrame(updatePanelPosition);
    function cancelOnOutside(event: PointerEvent) {
      const target = event.target as Node;
      if (!rootRef.current?.contains(target) && !panelRef.current?.contains(target)) cancel();
    }
    function cancelOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") cancel();
    }
    window.addEventListener("resize", updatePanelPosition);
    window.addEventListener("scroll", updatePanelPosition, true);
    document.addEventListener("pointerdown", cancelOnOutside);
    document.addEventListener("keydown", cancelOnEscape);
    return () => {
      window.removeEventListener("resize", updatePanelPosition);
      window.removeEventListener("scroll", updatePanelPosition, true);
      document.removeEventListener("pointerdown", cancelOnOutside);
      document.removeEventListener("keydown", cancelOnEscape);
    };
  }, [open, modelValue]);

  function openEditor() {
    if (disabled || open) return;
    const initial = parseCanonical(modelValue) || currentZonedParts();
    setDraft(initial);
    setViewYear(initial.year);
    setViewMonth(initial.month);
    syncTexts(initial);
    setOpen(true);
  }

  function cancel() {
    const committed = parseCanonical(modelValue) || currentZonedParts();
    setDraft(committed);
    syncTexts(committed);
    setOpen(false);
  }

  function updateDraft(next: DateTimeParts) {
    setDraft(next);
    syncTexts(next);
  }

  function applyDateText(base = draft): DateTimeParts | null {
    const parsed = parseDate(dateText, datePattern, base);
    setDateInvalid(!parsed);
    if (!parsed) return null;
    setDraft(parsed);
    setViewYear(parsed.year);
    setViewMonth(parsed.month);
    setDateText(formatDate(parsed, datePattern));
    return parsed;
  }

  function applyTimeText(base = draft): DateTimeParts | null {
    const parsed = parseTime(timeText, base, precision);
    setTimeInvalid(!parsed);
    if (!parsed) return null;
    setDraft(parsed);
    setTimeText(formatTime(parsed, precision));
    return parsed;
  }

  function confirm() {
    const parsedDate = parseDate(dateText, datePattern, draft);
    if (!parsedDate) {
      setDateInvalid(true);
      return;
    }
    const parsedTime = parseTime(timeText, parsedDate, precision);
    if (!parsedTime) {
      setTimeInvalid(true);
      return;
    }
    onValueChange?.(serializeCanonical(parsedTime, precision));
    setOpen(false);
  }

  function moveMonth(delta: number) {
    const date = new Date(Date.UTC(viewYear, viewMonth - 1 + delta, 1));
    setViewYear(date.getUTCFullYear());
    setViewMonth(date.getUTCMonth() + 1);
  }

  function selectDay(day: CalendarDay) {
    const next = { ...draft, year: day.year, month: day.month, day: day.day };
    updateDraft(next);
    setViewYear(day.year);
    setViewMonth(day.month);
  }

  function selectNow() {
    const current = currentZonedParts();
    if (precision === "minute") current.second = 0;
    updateDraft(current);
    setViewYear(current.year);
    setViewMonth(current.month);
  }

  const panel = open && typeof document !== "undefined"
    ? createPortal(
        <div ref={panelRef} className="admin-date-time-picker-panel" style={panelStyle}>
          <div className="admin-date-time-picker-editor-row">
            <div className="admin-date-time-picker-editor-cell">
              <input
                className={dateInvalid ? "is-invalid" : ""}
                value={dateText}
                aria-label="日期"
                onChange={(event) => { setDateText(event.target.value); setDateInvalid(false); }}
                onBlur={() => applyDateText()}
                onKeyDown={(event) => { if (event.key === "Enter") applyDateText(); }}
              />
            </div>
            <div className="admin-date-time-picker-editor-cell">
              <input
                className={timeInvalid ? "is-invalid" : ""}
                value={timeText}
                aria-label="时间"
                onChange={(event) => { setTimeText(event.target.value); setTimeInvalid(false); }}
                onBlur={() => applyTimeText()}
                onKeyDown={(event) => { if (event.key === "Enter") applyTimeText(); }}
              />
            </div>
          </div>
          <div className="admin-date-time-picker-panel__main">
            <section className="admin-date-time-picker-calendar">
              <header className="admin-date-time-picker-calendar__header">
                <button type="button" title="上一年" onClick={() => moveMonth(-12)}>«</button>
                <button type="button" title="上个月" onClick={() => moveMonth(-1)}>‹</button>
                <strong>{viewYear} 年 {viewMonth} 月</strong>
                <button type="button" title="下个月" onClick={() => moveMonth(1)}>›</button>
                <button type="button" title="下一年" onClick={() => moveMonth(12)}>»</button>
              </header>
              <div className="admin-date-time-picker-calendar__weekdays">
                {WEEKDAYS.map((weekday) => <span key={weekday}>{weekday}</span>)}
              </div>
              <div className="admin-date-time-picker-calendar__days">
                {calendarDays.map((day) => {
                  const selected = sameDate(day, draft);
                  const today = sameDate(day, currentZonedParts());
                  return (
                    <button
                      key={day.key}
                      type="button"
                      className={[
                        !day.currentMonth ? "is-outside" : "",
                        selected ? "is-selected" : "",
                        today ? "is-today" : "",
                      ].filter(Boolean).join(" ")}
                      onClick={() => selectDay(day)}
                    >
                      {day.day}
                    </button>
                  );
                })}
              </div>
            </section>
            <section className="admin-date-time-picker-time">
              <div className="admin-date-time-picker-time__wheels">
                <div className="admin-date-time-picker-time__selection" />
                <TimeWheel values={HOURS} selected={draft.hour} onChange={(hour) => updateDraft({ ...draft, hour })}/>
                <span>:</span>
                <TimeWheel values={MINUTES} selected={draft.minute} onChange={(minute) => updateDraft({ ...draft, minute })}/>
                {precision === "second" ? (
                  <>
                    <span>:</span>
                    <TimeWheel values={SECONDS} selected={draft.second} onChange={(second) => updateDraft({ ...draft, second })}/>
                  </>
                ) : null}
              </div>
            </section>
          </div>
          <footer className="admin-date-time-picker-panel__footer">
            <BzButton size="small" onClick={selectNow}>此刻</BzButton>
            <div>
              <BzButton size="small" onClick={cancel}>取消</BzButton>
              <BzButton size="small" buttonType="primary" onClick={confirm}>确定</BzButton>
            </div>
          </footer>
        </div>,
        document.body,
      )
    : null;

  return (
    <div ref={rootRef} className="admin-date-time-field-root">
      <div ref={triggerRef} className={disabled ? "admin-date-time-field-trigger is-disabled" : "admin-date-time-field-trigger"}>
        <input
          className="admin-date-time-field-trigger__value"
          value={displayValue}
          placeholder={placeholder}
          readOnly
          disabled={disabled}
          aria-expanded={open}
          onClick={openEditor}
        />
        <button className="admin-date-time-field-trigger__open" type="button" disabled={disabled} aria-label="打开日期时间选择器" onClick={open ? cancel : openEditor}>
          <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true">
            <rect x="1.5" y="2.5" width="11" height="10" fill="none" stroke="currentColor" />
            <path d="M1.5 5.5H12.5M4.5 1.5V3.5M9.5 1.5V3.5" fill="none" stroke="currentColor" />
            <path d="M4 7H5M6.5 7H7.5M9 7H10M4 9H5M6.5 9H7.5M9 9H10" fill="none" stroke="currentColor" />
          </svg>
        </button>
      </div>
      {panel}
    </div>
  );
}

function TimeWheel({ values, selected, onChange }: {
  values: number[];
  selected: number;
  onChange: (value: number) => void;
}) {
  const wheelRef = useRef<HTMLDivElement | null>(null);
  const timerRef = useRef<number | null>(null);

  useLayoutEffect(() => {
    if (wheelRef.current) wheelRef.current.scrollTop = selected * TIME_ROW_HEIGHT;
  }, [selected]);

  useEffect(() => () => {
    if (timerRef.current !== null) window.clearTimeout(timerRef.current);
  }, []);

  function handleScroll() {
    if (!wheelRef.current) return;
    if (timerRef.current !== null) window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => {
      if (!wheelRef.current) return;
      const index = clamp(Math.round(wheelRef.current.scrollTop / TIME_ROW_HEIGHT), 0, values.length - 1);
      wheelRef.current.scrollTo({ top: index * TIME_ROW_HEIGHT, behavior: "smooth" });
      if (values[index] !== selected) onChange(values[index]);
    }, 70);
  }

  return (
    <div ref={wheelRef} className="admin-date-time-picker-time__wheel" onScroll={handleScroll}>
      <ul>
        {values.map((value, index) => (
          <li
            key={value}
            className={value === selected ? "is-selected" : ""}
            onClick={() => {
              wheelRef.current?.scrollTo({ top: index * TIME_ROW_HEIGHT, behavior: "smooth" });
              if (value !== selected) onChange(value);
            }}
          >
            {pad(value)}
          </li>
        ))}
      </ul>
    </div>
  );
}

function parseCanonical(value: string): DateTimeParts | null {
  const matched = value.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/);
  if (!matched) return null;
  const result = {
    year: Number(matched[1]), month: Number(matched[2]), day: Number(matched[3]),
    hour: Number(matched[4]), minute: Number(matched[5]), second: Number(matched[6] || 0),
  };
  return validParts(result) ? result : null;
}

function serializeCanonical(value: DateTimeParts, precision: "minute" | "second"): string {
  const base = `${pad(value.year, 4)}-${pad(value.month)}-${pad(value.day)}T${pad(value.hour)}:${pad(value.minute)}`;
  return precision === "second" ? `${base}:${pad(value.second)}` : base;
}

function formatDate(value: DateTimeParts, pattern: string): string {
  return pattern.replace(/yyyy/g, pad(value.year, 4)).replace(/MM/g, pad(value.month)).replace(/dd/g, pad(value.day));
}

function formatTime(value: DateTimeParts, precision: "minute" | "second"): string {
  return `${pad(value.hour)}:${pad(value.minute)}${precision === "second" ? `:${pad(value.second)}` : ""}`;
}

function parseDate(text: string, pattern: string, base: DateTimeParts): DateTimeParts | null {
  const tokens = Array.from(pattern.matchAll(/yyyy|MM|dd/g));
  let expression = "^";
  let cursor = 0;
  for (const token of tokens) {
    expression += escapeRegExp(pattern.slice(cursor, token.index));
    expression += token[0] === "yyyy" ? "(\\d{4})" : "(\\d{1,2})";
    cursor = (token.index || 0) + token[0].length;
  }
  expression += `${escapeRegExp(pattern.slice(cursor))}$`;
  const matched = text.trim().match(new RegExp(expression));
  if (!matched) return null;
  const result = { ...base };
  tokens.forEach((token, index) => {
    const number = Number(matched[index + 1]);
    if (token[0] === "yyyy") result.year = number;
    if (token[0] === "MM") result.month = number;
    if (token[0] === "dd") result.day = number;
  });
  return validParts(result) ? result : null;
}

function parseTime(text: string, base: DateTimeParts, precision: "minute" | "second"): DateTimeParts | null {
  const matched = text.trim().match(precision === "second" ? /^(\d{1,2}):(\d{1,2}):(\d{1,2})$/ : /^(\d{1,2}):(\d{1,2})$/);
  if (!matched) return null;
  const result = { ...base, hour: Number(matched[1]), minute: Number(matched[2]), second: precision === "second" ? Number(matched[3]) : 0 };
  return validParts(result) ? result : null;
}

function currentZonedParts(): DateTimeParts {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: getUserTimeZone(), year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23",
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map((part) => [part.type, Number(part.value)]));
  return { year: values.year, month: values.month, day: values.day, hour: values.hour, minute: values.minute, second: values.second };
}

function buildCalendarDays(year: number, month: number): CalendarDay[] {
  const firstWeekday = (new Date(Date.UTC(year, month - 1, 1)).getUTCDay() + 6) % 7;
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(Date.UTC(year, month - 1, index - firstWeekday + 1));
    const dayYear = date.getUTCFullYear();
    const dayMonth = date.getUTCMonth() + 1;
    const day = date.getUTCDate();
    return { key: `${dayYear}-${dayMonth}-${day}`, year: dayYear, month: dayMonth, day, currentMonth: dayMonth === month };
  });
}

function validParts(value: DateTimeParts): boolean {
  if (value.year < 1 || value.year > 9999 || value.month < 1 || value.month > 12) return false;
  const maxDay = new Date(Date.UTC(value.year, value.month, 0)).getUTCDate();
  return value.day >= 1 && value.day <= maxDay && value.hour >= 0 && value.hour <= 23
    && value.minute >= 0 && value.minute <= 59 && value.second >= 0 && value.second <= 59;
}

function sameDate(left: { year: number; month: number; day: number }, right: DateTimeParts): boolean {
  return left.year === right.year && left.month === right.month && left.day === right.day;
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

function pad(value: number, length = 2): string {
  return String(value).padStart(length, "0");
}
