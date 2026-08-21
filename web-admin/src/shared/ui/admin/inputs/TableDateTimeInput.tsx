"use client";

import { formatDateTimeInputValue, resolveDateTimePrecision } from "@admin/shared/lib/formatter";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import type { CSSProperties } from "react";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

export interface TableDateTimeInputProps {
  value: string;
  disabled?: boolean;
  placeholder?: string;
  min?: string;
  max?: string;
  firstDay?: "monday" | "sunday";
  dateTimePattern?: string;
  timeZone?: string;
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

interface CalendarDay extends DateTimeParts {
  key: string;
  currentMonth: boolean;
}

type PickerMode = "date" | "time";
type DateSubview = "calendar" | "year" | "month";
type TimeUnit = "period" | "hour" | "minute" | "second";
type TimeValue = number | "AM" | "PM";

const MONTHS_EN = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];
const MONTHS_ZH = [
  "一月",
  "二月",
  "三月",
  "四月",
  "五月",
  "六月",
  "七月",
  "八月",
  "九月",
  "十月",
  "十一月",
  "十二月",
];
const HOURS_24 = Array.from({ length: 24 }, (_, index) => index);
const HOURS_12 = Array.from({ length: 12 }, (_, index) => index + 1);
const MINUTES = Array.from({ length: 60 }, (_, index) => index);
const SECONDS = Array.from({ length: 60 }, (_, index) => index);
const PERIODS = ["AM", "PM"] as const;
const TIME_ROW_HEIGHT = 36;

export function TableDateTimeInput({
  value,
  disabled = false,
  placeholder,
  min,
  max,
  firstDay = "monday",
  dateTimePattern = "yyyy-MM-dd HH:mm:ss",
  timeZone = "Asia/Shanghai",
  onValueChange,
}: TableDateTimeInputProps) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const triggerRef = useRef<HTMLDivElement | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState<PickerMode>("date");
  const [dateSubview, setDateSubview] = useState<DateSubview>("calendar");
  const [draft, setDraft] = useState<DateTimeParts>(
    () => parseCanonical(value) || currentZonedParts(timeZone),
  );
  const [viewYear, setViewYear] = useState(draft.year);
  const [viewMonth, setViewMonth] = useState(draft.month);
  const [yearBase, setYearBase] = useState(Math.floor(draft.year / 10) * 10);
  const [inputText, setInputText] = useState(() =>
    formatDateTimeInputValue(value, dateTimePattern, timeZone),
  );
  const [invalid, setInvalid] = useState(false);
  const [panelStyle, setPanelStyle] = useState<CSSProperties>({});
  const pattern = dateTimePattern;
  const precision = resolveDateTimePrecision(pattern);
  const formatInfo = inspectFormat(pattern);
  const panelWidth = mode === "date" ? 320 : timePanelWidth(formatInfo.is12, precision);
  const calendarDays = buildCalendarDays(viewYear, viewMonth, firstDay);
  const minParts = parseCanonical(min || "");
  const maxParts = parseCanonical(max || "");

  useEffect(() => {
    if (open) return;
    setInputText(formatDateTimeInputValue(value, pattern, timeZone));
    const committed = parseCanonical(value);
    if (committed) setDraft(committed);
  }, [open, pattern, timeZone, value]);

  useEffect(() => {
    if (!open) return;
    function handleOutside(event: PointerEvent) {
      const target = event.target as Node;
      if (!rootRef.current?.contains(target) && !panelRef.current?.contains(target)) cancel();
    }
    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") cancel();
    }
    document.addEventListener("pointerdown", handleOutside);
    document.addEventListener("keydown", handleEscape);
    window.addEventListener("resize", updatePanelPosition);
    window.addEventListener("scroll", updatePanelPosition, true);
    requestAnimationFrame(updatePanelPosition);
    return () => {
      document.removeEventListener("pointerdown", handleOutside);
      document.removeEventListener("keydown", handleEscape);
      window.removeEventListener("resize", updatePanelPosition);
      window.removeEventListener("scroll", updatePanelPosition, true);
    };
  }, [open, panelWidth]);

  function updatePanelPosition() {
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    const margin = 12;
    const width = Math.min(panelWidth, window.innerWidth - margin * 2);
    const estimatedHeight = mode === "date" ? 414 : 340;
    const left = Math.max(margin, Math.min(rect.left, window.innerWidth - width - margin));
    const top =
      rect.bottom + 4 + estimatedHeight <= window.innerHeight
        ? rect.bottom + 4
        : Math.max(margin, rect.top - estimatedHeight - 4);
    setPanelStyle({ position: "fixed", left, top, width });
  }

  function openEditor() {
    if (disabled || open) return;
    const typed = parseFormatted(inputText, pattern, timeZone);
    const initial = clampParts(
      typed || parseCanonical(value) || currentZonedParts(timeZone),
      minParts,
      maxParts,
    );
    setDraft(initial);
    setViewYear(initial.year);
    setViewMonth(initial.month);
    setYearBase(Math.floor(initial.year / 10) * 10);
    setMode("date");
    setDateSubview("calendar");
    setInvalid(false);
    setOpen(true);
  }

  function cancel() {
    setInputText(formatDateTimeInputValue(value, pattern, timeZone));
    setInvalid(false);
    setOpen(false);
  }

  function commit(next = draft) {
    const normalized = normalizeParts(clampParts(next, minParts, maxParts), precision);
    const canonical = serializeCanonical(normalized, precision);
    setDraft(normalized);
    setInputText(formatDateTimeInputValue(canonical, pattern, timeZone));
    setInvalid(false);
    setOpen(false);
    onValueChange?.(canonical);
  }

  function commitTyped() {
    if (!inputText.trim()) {
      setInvalid(false);
      setOpen(false);
      onValueChange?.("");
      return;
    }
    const parsed = parseFormatted(inputText, pattern, timeZone);
    if (!parsed || !selectable(parsed, minParts, maxParts)) {
      setInvalid(true);
      return;
    }
    commit(parsed);
  }

  function selectNow() {
    const now = clampParts(
      normalizeParts(currentZonedParts(timeZone), precision),
      minParts,
      maxParts,
    );
    setDraft(now);
    setViewYear(now.year);
    setViewMonth(now.month);
    setYearBase(Math.floor(now.year / 10) * 10);
  }

  function selectDate(day: CalendarDay) {
    const next = clampParts(
      { ...draft, year: day.year, month: day.month, day: day.day },
      minParts,
      maxParts,
    );
    setDraft(next);
    setViewYear(day.year);
    setViewMonth(day.month);
  }

  function moveMonth(delta: number) {
    const date = new Date(Date.UTC(viewYear, viewMonth - 1 + delta, 1));
    setViewYear(date.getUTCFullYear());
    setViewMonth(date.getUTCMonth() + 1);
    setYearBase(Math.floor(date.getUTCFullYear() / 10) * 10);
  }

  function updateTime(unit: TimeUnit, value: TimeValue) {
    let hour = draft.hour;
    if (unit === "period") hour = to24Hour(hour % 12 || 12, value as "AM" | "PM");
    if (unit === "hour") {
      hour = formatInfo.is12
        ? to24Hour(Number(value), draft.hour < 12 ? "AM" : "PM")
        : Number(value);
    }
    const next = clampParts(
      {
        ...draft,
        hour,
        minute: unit === "minute" ? Number(value) : draft.minute,
        second: unit === "second" ? Number(value) : draft.second,
      },
      minParts,
      maxParts,
    );
    setDraft(next);
  }

  const panel =
    open && typeof document !== "undefined"
      ? createPortal(
          <div
            ref={panelRef}
            className="table-date-time-picker"
            data-mode={mode}
            style={panelStyle}
          >
            {mode === "date" ? (
              <DatePanel
                subview={dateSubview}
                draft={draft}
                days={calendarDays}
                viewYear={viewYear}
                viewMonth={viewMonth}
                yearBase={yearBase}
                min={minParts}
                max={maxParts}
                timeZone={timeZone}
                onSubviewChange={setDateSubview}
                onMoveMonth={moveMonth}
                onYearBaseChange={setYearBase}
                onYearSelect={(year) => {
                  setViewYear(year);
                  setYearBase(Math.floor(year / 10) * 10);
                  setDateSubview("month");
                }}
                onMonthSelect={(month) => {
                  setViewMonth(month);
                  setDateSubview("calendar");
                }}
                onDateSelect={selectDate}
              />
            ) : (
              <TimePanel
                draft={draft}
                is12={formatInfo.is12}
                precision={precision}
                onChange={updateTime}
              />
            )}
            <footer className="table-date-time-picker__footer">
              <BzButton
                size="small"
                onClick={selectNow}
              >
                此刻
              </BzButton>
              <div>
                <BzButton
                  size="small"
                  onClick={() => setMode(mode === "date" ? "time" : "date")}
                >
                  {mode === "date" ? "选择时间" : "选择日期"}
                </BzButton>
                <BzButton
                  size="small"
                  buttonType="primary"
                  onClick={() => commit()}
                >
                  确定
                </BzButton>
              </div>
            </footer>
          </div>,
          document.body,
        )
      : null;

  return (
    <div
      ref={rootRef}
      className="table-date-time-input"
    >
      <div
        ref={triggerRef}
        className={`table-date-time-input__field${disabled ? " is-disabled" : ""}${invalid ? " is-invalid" : ""}`}
      >
        <input
          className="table-date-time-input__inner"
          value={inputText}
          disabled={disabled}
          placeholder={placeholder || pattern}
          autoComplete="off"
          spellCheck={false}
          aria-invalid={invalid}
          onFocus={openEditor}
          onClick={openEditor}
          onChange={(event) => {
            setInputText(event.target.value);
            setInvalid(false);
          }}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              commitTyped();
            }
          }}
        />
      </div>
      <div className={`table-date-time-input__trigger-cell${disabled ? " is-disabled" : ""}`}>
        <button
          className="table-date-time-input__trigger"
          type="button"
          disabled={disabled}
          aria-label="打开日期时间选择器"
          onClick={() => (open ? cancel() : openEditor())}
        >
          <svg
            width="15"
            height="15"
            viewBox="0 0 16 16"
            fill="none"
            aria-hidden="true"
          >
            <path
              d="M4 1.75v2M12 1.75v2M2.5 5.5h11M3.25 3h9.5c.69 0 1.25.56 1.25 1.25v8.5c0 .69-.56 1.25-1.25 1.25h-9.5C2.56 14 2 13.44 2 12.75v-8.5C2 3.56 2.56 3 3.25 3Z"
              stroke="currentColor"
              strokeWidth="1.2"
            />
          </svg>
        </button>
      </div>
      {panel}
    </div>
  );
}

function DatePanel({
  subview,
  draft,
  days,
  viewYear,
  viewMonth,
  yearBase,
  min,
  max,
  timeZone,
  onSubviewChange,
  onMoveMonth,
  onYearBaseChange,
  onYearSelect,
  onMonthSelect,
  onDateSelect,
}: {
  subview: DateSubview;
  draft: DateTimeParts;
  days: CalendarDay[];
  viewYear: number;
  viewMonth: number;
  yearBase: number;
  min: DateTimeParts | null;
  max: DateTimeParts | null;
  timeZone: string;
  onSubviewChange: (subview: DateSubview) => void;
  onMoveMonth: (delta: number) => void;
  onYearBaseChange: (base: number) => void;
  onYearSelect: (year: number) => void;
  onMonthSelect: (month: number) => void;
  onDateSelect: (day: CalendarDay) => void;
}) {
  if (subview === "year") {
    const years = Array.from({ length: 12 }, (_, index) => yearBase - 1 + index);
    return (
      <>
        <PickerCompactHeader
          title={`${yearBase} - ${yearBase + 10}`}
          onPrevious={() => onYearBaseChange(yearBase - 10)}
          onNext={() => onYearBaseChange(yearBase + 10)}
        />
        <div className="table-date-time-picker__grid">
          {years.map((year) => (
            <button
              type="button"
              className={year === viewYear ? "is-selected" : undefined}
              disabled={!yearSelectable(year, min, max)}
              key={year}
              onClick={() => onYearSelect(year)}
            >
              {year}
            </button>
          ))}
        </div>
      </>
    );
  }
  if (subview === "month") {
    return (
      <>
        <PickerCompactHeader
          title={`${viewYear} 年`}
          onPrevious={() => onYearSelect(viewYear - 1)}
          onNext={() => onYearSelect(viewYear + 1)}
        />
        <div className="table-date-time-picker__grid">
          {MONTHS_ZH.map((month, index) => (
            <button
              type="button"
              className={index + 1 === viewMonth ? "is-selected" : undefined}
              disabled={!monthSelectable(viewYear, index + 1, min, max)}
              key={month}
              onClick={() => onMonthSelect(index + 1)}
            >
              {month}
            </button>
          ))}
        </div>
      </>
    );
  }
  const weekdays = days[0]?.key.endsWith("monday")
    ? ["一", "二", "三", "四", "五", "六", "日"]
    : ["日", "一", "二", "三", "四", "五", "六"];
  const today = currentZonedParts(timeZone);
  return (
    <>
      <header className="table-date-time-picker__header">
        <button
          type="button"
          title="上一年"
          onClick={() => onMoveMonth(-12)}
        >
          «
        </button>
        <button
          type="button"
          title="上个月"
          onClick={() => onMoveMonth(-1)}
        >
          ‹
        </button>
        <div>
          <button
            type="button"
            onClick={() => onSubviewChange("year")}
          >
            {viewYear}年
          </button>
          <button
            type="button"
            onClick={() => onSubviewChange("month")}
          >
            {viewMonth}月
          </button>
        </div>
        <button
          type="button"
          title="下个月"
          onClick={() => onMoveMonth(1)}
        >
          ›
        </button>
        <button
          type="button"
          title="下一年"
          onClick={() => onMoveMonth(12)}
        >
          »
        </button>
      </header>
      <div className="table-date-time-picker__weekdays">
        {weekdays.map((weekday) => (
          <span key={weekday}>{weekday}</span>
        ))}
      </div>
      <div className="table-date-time-picker__days">
        {days.map((day) => (
          <button
            type="button"
            className={[
              !day.currentMonth ? "is-outside" : "",
              sameDate(day, draft) ? "is-selected" : "",
              sameDate(day, today) ? "is-today" : "",
            ]
              .filter(Boolean)
              .join(" ")}
            disabled={!daySelectable(day, min, max)}
            key={day.key}
            onClick={() => onDateSelect(day)}
          >
            <span>{day.day}</span>
          </button>
        ))}
      </div>
    </>
  );
}

function PickerCompactHeader({
  title,
  onPrevious,
  onNext,
}: {
  title: string;
  onPrevious: () => void;
  onNext: () => void;
}) {
  return (
    <header className="table-date-time-picker__compact-header">
      <button
        type="button"
        onClick={onPrevious}
      >
        «
      </button>
      <strong>{title}</strong>
      <button
        type="button"
        onClick={onNext}
      >
        »
      </button>
    </header>
  );
}

function TimePanel({
  draft,
  is12,
  precision,
  onChange,
}: {
  draft: DateTimeParts;
  is12: boolean;
  precision: "minute" | "second";
  onChange: (unit: TimeUnit, value: TimeValue) => void;
}) {
  return (
    <>
      <div className="table-date-time-picker__time-title">选择时间</div>
      <div className="table-date-time-picker__time">
        {is12 ? (
          <TimeColumn
            unit="period"
            values={PERIODS}
            selected={draft.hour < 12 ? "AM" : "PM"}
            onChange={onChange}
          />
        ) : null}
        <TimeColumn
          unit="hour"
          values={is12 ? HOURS_12 : HOURS_24}
          selected={is12 ? draft.hour % 12 || 12 : draft.hour}
          onChange={onChange}
        />
        <TimeColumn
          unit="minute"
          values={MINUTES}
          selected={draft.minute}
          onChange={onChange}
        />
        {precision === "second" ? (
          <TimeColumn
            unit="second"
            values={SECONDS}
            selected={draft.second}
            onChange={onChange}
          />
        ) : null}
      </div>
    </>
  );
}

function TimeColumn({
  unit,
  values,
  selected,
  onChange,
}: {
  unit: TimeUnit;
  values: readonly TimeValue[];
  selected: TimeValue;
  onChange: (unit: TimeUnit, value: TimeValue) => void;
}) {
  const columnRef = useRef<HTMLDivElement | null>(null);
  const timerRef = useRef<number | null>(null);
  useLayoutEffect(() => {
    const index = values.indexOf(selected);
    if (columnRef.current && index >= 0) columnRef.current.scrollTop = index * TIME_ROW_HEIGHT;
  }, [selected, values]);
  useEffect(
    () => () => {
      if (timerRef.current !== null) window.clearTimeout(timerRef.current);
    },
    [],
  );
  return (
    <div
      ref={columnRef}
      className="table-date-time-picker__time-column"
      onScroll={() => {
        if (timerRef.current !== null) window.clearTimeout(timerRef.current);
        timerRef.current = window.setTimeout(() => {
          if (!columnRef.current) return;
          const index = clamp(
            Math.round(columnRef.current.scrollTop / TIME_ROW_HEIGHT),
            0,
            values.length - 1,
          );
          columnRef.current.scrollTo({ top: index * TIME_ROW_HEIGHT, behavior: "smooth" });
          if (values[index] !== selected) onChange(unit, values[index]);
        }, 110);
      }}
    >
      <ul>
        {values.map((item, index) => (
          <li
            className={item === selected ? "is-selected" : undefined}
            key={item}
            onClick={() => {
              columnRef.current?.scrollTo({ top: index * TIME_ROW_HEIGHT, behavior: "smooth" });
              if (item !== selected) onChange(unit, item);
            }}
          >
            {typeof item === "number" ? pad(item) : item}
          </li>
        ))}
      </ul>
    </div>
  );
}

function inspectFormat(format: string): { is12: boolean } {
  const tokens = tokenizeFormat(format)
    .filter((part) => part.type === "token")
    .map((part) => part.value);
  return { is12: tokens.includes("h") || tokens.includes("hh") };
}

function tokenizeFormat(format: string): Array<{ type: "token" | "literal"; value: string }> {
  const tokens = [
    "yyyy",
    "MMM",
    "XXX",
    "MM",
    "dd",
    "HH",
    "hh",
    "mm",
    "ss",
    "M",
    "d",
    "H",
    "h",
    "m",
    "s",
    "a",
  ];
  const parts: Array<{ type: "token" | "literal"; value: string }> = [];
  let index = 0;
  while (index < format.length) {
    if (format[index] === "'") {
      let literal = "";
      index++;
      while (index < format.length) {
        if (format[index] === "'") {
          if (format[index + 1] === "'") {
            literal += "'";
            index += 2;
            continue;
          }
          index++;
          break;
        }
        literal += format[index++];
      }
      if (literal) parts.push({ type: "literal", value: literal });
      continue;
    }
    const token = tokens.find((candidate) => format.startsWith(candidate, index));
    if (token) {
      parts.push({ type: "token", value: token });
      index += token.length;
      continue;
    }
    let literal = format[index++];
    while (
      index < format.length &&
      format[index] !== "'" &&
      !tokens.some((candidate) => format.startsWith(candidate, index))
    ) {
      literal += format[index++];
    }
    parts.push({ type: "literal", value: literal });
  }
  return parts;
}

function parseFormatted(text: string, format: string, timeZone: string): DateTimeParts | null {
  const captures: string[] = [];
  let expression = "^";
  for (const part of tokenizeFormat(format)) {
    if (part.type === "literal") {
      expression += escapeRegExp(part.value);
      continue;
    }
    captures.push(part.value);
    const patterns: Record<string, string> = {
      yyyy: "(\\d{4})",
      MMM: `(${MONTHS_EN.join("|")})`,
      MM: "(0[1-9]|1[0-2])",
      M: "(0?[1-9]|1[0-2])",
      dd: "(0[1-9]|[12]\\d|3[01])",
      d: "(0?[1-9]|[12]\\d|3[01])",
      HH: "([01]\\d|2[0-3])",
      H: "([01]?\\d|2[0-3])",
      hh: "(0[1-9]|1[0-2])",
      h: "(0?[1-9]|1[0-2])",
      mm: "([0-5]\\d)",
      m: "([0-5]?\\d)",
      ss: "([0-5]\\d)",
      s: "([0-5]?\\d)",
      a: "(AM|PM|am|pm|上午|下午)",
      XXX: "(Z|[+-]\\d{2}:\\d{2})",
    };
    expression += patterns[part.value];
  }
  const matched = text.trim().match(new RegExp(`${expression}$`, "i"));
  if (!matched) return null;
  const result = currentZonedParts(timeZone);
  let hour12: number | null = null;
  let period = "";
  captures.forEach((token, index) => {
    const raw = matched[index + 1];
    if (token === "yyyy") result.year = Number(raw);
    if (token === "MMM")
      result.month = MONTHS_EN.findIndex((month) => month.toLowerCase() === raw.toLowerCase()) + 1;
    if (token === "MM" || token === "M") result.month = Number(raw);
    if (token === "dd" || token === "d") result.day = Number(raw);
    if (token === "HH" || token === "H") result.hour = Number(raw);
    if (token === "hh" || token === "h") hour12 = Number(raw);
    if (token === "mm" || token === "m") result.minute = Number(raw);
    if (token === "ss" || token === "s") result.second = Number(raw);
    if (token === "a") period = raw.toUpperCase();
  });
  if (hour12 !== null)
    result.hour = to24Hour(hour12, period === "PM" || period === "下午" ? "PM" : "AM");
  return validParts(result) ? result : null;
}

function parseCanonical(value: string): DateTimeParts | null {
  const matched = value.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/);
  if (!matched) return null;
  const result = {
    year: Number(matched[1]),
    month: Number(matched[2]),
    day: Number(matched[3]),
    hour: Number(matched[4]),
    minute: Number(matched[5]),
    second: Number(matched[6] || 0),
  };
  return validParts(result) ? result : null;
}

function serializeCanonical(value: DateTimeParts, precision: "minute" | "second"): string {
  const base = `${pad(value.year, 4)}-${pad(value.month)}-${pad(value.day)}T${pad(value.hour)}:${pad(value.minute)}`;
  return precision === "second" ? `${base}:${pad(value.second)}` : base;
}

function currentZonedParts(timeZone: string): DateTimeParts {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map((part) => [part.type, Number(part.value)]));
  return {
    year: values.year,
    month: values.month,
    day: values.day,
    hour: values.hour,
    minute: values.minute,
    second: values.second,
  };
}

function buildCalendarDays(
  year: number,
  month: number,
  firstDay: "monday" | "sunday",
): CalendarDay[] {
  const sundayBased = new Date(Date.UTC(year, month - 1, 1)).getUTCDay();
  const offset = firstDay === "monday" ? (sundayBased + 6) % 7 : sundayBased;
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(Date.UTC(year, month - 1, index - offset + 1));
    const dayYear = date.getUTCFullYear();
    const dayMonth = date.getUTCMonth() + 1;
    return {
      key: `${dayYear}-${dayMonth}-${date.getUTCDate()}-${firstDay}`,
      year: dayYear,
      month: dayMonth,
      day: date.getUTCDate(),
      hour: 0,
      minute: 0,
      second: 0,
      currentMonth: dayMonth === month,
    };
  });
}

function normalizeParts(value: DateTimeParts, precision: "minute" | "second"): DateTimeParts {
  return { ...value, second: precision === "second" ? value.second : 0 };
}

function clampParts(
  value: DateTimeParts,
  min: DateTimeParts | null,
  max: DateTimeParts | null,
): DateTimeParts {
  if (min && compareParts(value, min) < 0) return { ...min };
  if (max && compareParts(value, max) > 0) return { ...max };
  return value;
}

function selectable(
  value: DateTimeParts,
  min: DateTimeParts | null,
  max: DateTimeParts | null,
): boolean {
  return (!min || compareParts(value, min) >= 0) && (!max || compareParts(value, max) <= 0);
}

function daySelectable(
  value: DateTimeParts,
  min: DateTimeParts | null,
  max: DateTimeParts | null,
): boolean {
  const start = { ...value, hour: 0, minute: 0, second: 0 };
  const end = { ...value, hour: 23, minute: 59, second: 59 };
  return (!min || compareParts(end, min) >= 0) && (!max || compareParts(start, max) <= 0);
}

function monthSelectable(
  year: number,
  month: number,
  min: DateTimeParts | null,
  max: DateTimeParts | null,
): boolean {
  return (
    daySelectable({ year, month, day: 1, hour: 0, minute: 0, second: 0 }, min, max) ||
    daySelectable(
      { year, month, day: daysInMonth(year, month), hour: 0, minute: 0, second: 0 },
      min,
      max,
    )
  );
}

function yearSelectable(
  year: number,
  min: DateTimeParts | null,
  max: DateTimeParts | null,
): boolean {
  return (!min || year >= min.year) && (!max || year <= max.year);
}

function compareParts(left: DateTimeParts, right: DateTimeParts): number {
  return serializeCanonical(left, "second").localeCompare(serializeCanonical(right, "second"));
}

function validParts(value: DateTimeParts): boolean {
  return (
    value.year >= 1 &&
    value.year <= 9999 &&
    value.month >= 1 &&
    value.month <= 12 &&
    value.day >= 1 &&
    value.day <= daysInMonth(value.year, value.month) &&
    value.hour >= 0 &&
    value.hour <= 23 &&
    value.minute >= 0 &&
    value.minute <= 59 &&
    value.second >= 0 &&
    value.second <= 59
  );
}

function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

function sameDate(left: DateTimeParts, right: DateTimeParts): boolean {
  return left.year === right.year && left.month === right.month && left.day === right.day;
}

function to24Hour(hour: number, period: "AM" | "PM"): number {
  return (hour % 12) + (period === "PM" ? 12 : 0);
}

function timePanelWidth(is12: boolean, precision: "minute" | "second"): number {
  if (is12) return precision === "second" ? 280 : 240;
  return precision === "second" ? 240 : 200;
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
