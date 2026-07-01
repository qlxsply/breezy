"use client";

import {
  buildDateTimeRangeSubmitValue,
  dateTimeInputToEpochMillisString,
  formatDateTimeInputValue,
  getUserDateTimeFormatPattern,
  getUserTimeZone,
  resolveDateTimePrecision,
  resolveUserTimeZoneCode,
} from "@admin/core/formatter";
import { usePersonalizedConfigs } from "@admin/core/registry/auth-registry";
import { createPortal } from "react-dom";
import { useEffect, useMemo, useRef, useState } from "react";

import { BzButton } from "../bz/BzButton";

interface AdminDateTimeRangeFieldProps {
  startValue?: string;
  endValue?: string;
  placeholder?: string;
  allowSingleSided?: boolean;
  onRangeChange?: (range: { start: string; end: string }) => void;
}

type Precision = "minute" | "second";
type PickerKey = "start-date" | "start-time" | "end-date" | "end-time" | null;

interface ZonedDateParts {
  year: number;
  month: number;
  day: number;
}

interface DateTextFieldProps {
  value: string;
  active: boolean;
  placeholder: string;
  onValueChange: (value: string) => void;
  onIconClick: () => void;
}

interface TimeTextFieldProps {
  value: string;
  active: boolean;
  placeholder: string;
  onValueChange: (value: string) => void;
  onIconClick: () => void;
}

export function AdminDateTimeRangeField({
  startValue = "",
  endValue = "",
  placeholder = "请选择时间范围",
  allowSingleSided = true,
  onRangeChange,
}: AdminDateTimeRangeFieldProps) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const [open, setOpen] = useState(false);
  const [panelStyle, setPanelStyle] = useState<React.CSSProperties>({});

  const configs = usePersonalizedConfigs();
  const dateTimePattern = useMemo(() => {
    const configured = configs.find((item) => item.code === "USER_DATE_TIME_FORMAT")?.value?.trim();
    return configured ? resolvePattern(configured) : getUserDateTimeFormatPattern();
  }, [configs]);
  const precision = useMemo(() => resolveDateTimePrecision(dateTimePattern), [dateTimePattern]);
  const timeZone = useMemo(() => {
    const configured = configs.find((item) => item.code === "USER_TIME_ZONE")?.value?.trim();
    return configured ? resolveUserTimeZoneCode(configured) : getUserTimeZone();
  }, [configs]);

  const displayText = useMemo(() => {
    const startLabel = formatDateTimeInputValue(startValue, dateTimePattern);
    const endLabel = formatDateTimeInputValue(endValue, dateTimePattern);
    if (startLabel && endLabel) return `${startLabel}–${endLabel}`;
    if (startLabel) return `${startLabel} 起`;
    if (endLabel) return `截至 ${endLabel}`;
    return "";
  }, [dateTimePattern, endValue, startValue]);

  function updatePanelPosition() {
    if (!triggerRef.current || !panelRef.current) return;
    const triggerRect = triggerRef.current.getBoundingClientRect();
    const panelRect = panelRef.current.getBoundingClientRect();
    const viewportWidth = window.innerWidth;
    const margin = 12;

    let left = triggerRect.left;
    if (left + panelRect.width > viewportWidth - margin) {
      left = Math.max(margin, viewportWidth - panelRect.width - margin);
    }

    setPanelStyle({
      position: "fixed",
      left: `${left}px`,
      top: `${triggerRect.bottom + 8}px`,
      width: `${Math.max(560, triggerRect.width + 180)}px`,
    });
  }

  useEffect(() => {
    if (!open) return;
    requestAnimationFrame(updatePanelPosition);

    function handleResize() {
      updatePanelPosition();
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }

    function handleOutside(event: MouseEvent) {
      const target = event.target as Node;
      if (!rootRef.current?.contains(target) && !panelRef.current?.contains(target)) {
        setOpen(false);
      }
    }

    window.addEventListener("resize", handleResize);
    window.addEventListener("scroll", handleResize, true);
    document.addEventListener("keydown", handleEscape);
    document.addEventListener("mousedown", handleOutside);

    return () => {
      window.removeEventListener("resize", handleResize);
      window.removeEventListener("scroll", handleResize, true);
      document.removeEventListener("keydown", handleEscape);
      document.removeEventListener("mousedown", handleOutside);
    };
  }, [open]);

  return (
    <div ref={rootRef} className="admin-datetime-range">
      <button
        ref={triggerRef}
        className={`admin-datetime-range-trigger${displayText ? " has-value" : ""}${open ? " is-open" : ""}`}
        type="button"
        title={displayText || placeholder}
        onClick={() => setOpen((value) => !value)}
      >
        <span className={`admin-datetime-range-trigger__text${displayText ? "" : " is-placeholder"}`}>
          {displayText || placeholder}
        </span>
        <span className="admin-datetime-range-trigger__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path
              d="M7 3v3M17 3v3M4 8h16M5 5h14a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z"
              fill="none"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="1.7"
            />
          </svg>
        </span>
      </button>

      {open && typeof document !== "undefined"
        ? createPortal(
            <div ref={panelRef} className="admin-datetime-range-popover" style={panelStyle}>
              <AdminDateTimeRangePanel
                startValue={startValue}
                endValue={endValue}
                precision={precision}
                timeZone={timeZone}
                allowSingleSided={allowSingleSided}
                onCancel={() => setOpen(false)}
                onClear={() => {
                  onRangeChange?.({ start: "", end: "" });
                  setOpen(false);
                }}
                onConfirm={(range) => {
                  onRangeChange?.(range);
                  setOpen(false);
                }}
              />
            </div>,
            document.body,
          )
        : null}
    </div>
  );
}

function AdminDateTimeRangePanel({
  startValue,
  endValue,
  precision,
  timeZone,
  allowSingleSided,
  onCancel,
  onClear,
  onConfirm,
}: {
  startValue: string;
  endValue: string;
  precision: Precision;
  timeZone: string;
  allowSingleSided: boolean;
  onCancel: () => void;
  onClear: () => void;
  onConfirm: (range: { start: string; end: string }) => void;
}) {
  const [startDateText, setStartDateText] = useState(toDateInputText(splitDatePart(startValue)));
  const [startTimeText, setStartTimeText] = useState(splitTimePart(startValue, precision));
  const [endDateText, setEndDateText] = useState(toDateInputText(splitDatePart(endValue)));
  const [endTimeText, setEndTimeText] = useState(splitTimePart(endValue, precision));
  const [error, setError] = useState("");
  const [activePicker, setActivePicker] = useState<PickerKey>(null);
  const [calendarMonth, setCalendarMonth] = useState(() => initCalendarMonth(startValue || endValue, timeZone));

  const shortcuts = useMemo(() => buildShortcutRanges(timeZone, precision), [timeZone, precision]);
  const visibleDateValue = normalizeDateText(activePicker?.startsWith("end") ? endDateText : startDateText);
  const visibleTimeValue = normalizeTimeValue(activePicker?.startsWith("end") ? endTimeText : startTimeText, precision);

  function updateShortcut(start: string, end: string) {
    setError("");
    setStartDateText(toDateInputText(splitDatePart(start)));
    setStartTimeText(splitTimePart(start, precision));
    setEndDateText(toDateInputText(splitDatePart(end)));
    setEndTimeText(splitTimePart(end, precision));
    setCalendarMonth(initCalendarMonth(start, timeZone));
  }

  function updateCalendarDate(dateValue: string) {
    if (activePicker === "end-date") {
      setEndDateText(toDateInputText(dateValue));
    } else {
      setStartDateText(toDateInputText(dateValue));
    }
    setActivePicker(null);
  }

  function updateTimePart(next: string) {
    if (activePicker === "end-time") {
      setEndTimeText(next);
    } else {
      setStartTimeText(next);
    }
  }

  function confirm() {
    setError("");
    const start = buildDateTimeValue(normalizeDateText(startDateText), normalizeTimeValue(startTimeText, precision), precision);
    const end = buildDateTimeValue(normalizeDateText(endDateText), normalizeTimeValue(endTimeText, precision), precision);

    if (!allowSingleSided && Boolean(start) !== Boolean(end)) {
      setError("请选择完整的时间范围");
      return;
    }

    if (start && end) {
      const startMillis = Number(dateTimeInputToEpochMillisString(start));
      const endMillis = Number(dateTimeInputToEpochMillisString(end));
      if (Number.isFinite(startMillis) && Number.isFinite(endMillis) && startMillis > endMillis) {
        setError("开始时间不能晚于结束时间");
        return;
      }
    }

    onConfirm({ start, end });
  }

  return (
    <div className="admin-datetime-range-panel">
      <div className="admin-datetime-range-panel__sidebar">
        <div className="admin-datetime-range-panel__sidebar-title">快捷范围</div>
        {shortcuts.map((shortcut) => (
          <button
            key={shortcut.key}
            className="admin-datetime-range-panel__shortcut"
            type="button"
            onClick={() => updateShortcut(shortcut.start, shortcut.end)}
          >
            {shortcut.label}
          </button>
        ))}
      </div>

      <div className="admin-datetime-range-panel__main">
        <PickerRow
          label="开始时间"
          dateValue={startDateText}
          timeValue={startTimeText}
          precision={precision}
          activeDate={activePicker === "start-date"}
          activeTime={activePicker === "start-time"}
          onDateValueChange={setStartDateText}
          onTimeValueChange={setStartTimeText}
          onDateIconClick={() => {
            setCalendarMonth(initCalendarMonth(buildDateTimeValue(normalizeDateText(startDateText), normalizeTimeValue(startTimeText, precision), precision), timeZone));
            setActivePicker((current) => (current === "start-date" ? null : "start-date"));
          }}
          onTimeIconClick={() => setActivePicker((current) => (current === "start-time" ? null : "start-time"))}
        />

        <PickerRow
          label="结束时间"
          dateValue={endDateText}
          timeValue={endTimeText}
          precision={precision}
          activeDate={activePicker === "end-date"}
          activeTime={activePicker === "end-time"}
          onDateValueChange={setEndDateText}
          onTimeValueChange={setEndTimeText}
          onDateIconClick={() => {
            setCalendarMonth(initCalendarMonth(buildDateTimeValue(normalizeDateText(endDateText), normalizeTimeValue(endTimeText, precision), precision), timeZone));
            setActivePicker((current) => (current === "end-date" ? null : "end-date"));
          }}
          onTimeIconClick={() => setActivePicker((current) => (current === "end-time" ? null : "end-time"))}
        />

        {error ? <div className="admin-datetime-range-panel__error">{error}</div> : null}

        <div className="admin-datetime-range-panel__overlay-zone">
          {activePicker === "start-date" || activePicker === "end-date" ? (
            <CalendarPanel
              month={calendarMonth}
              selectedDate={visibleDateValue}
              timeZone={timeZone}
              onMonthChange={setCalendarMonth}
              onSelect={updateCalendarDate}
            />
          ) : null}

          {activePicker === "start-time" || activePicker === "end-time" ? (
            <TimePanel
              precision={precision}
              selectedTime={visibleTimeValue}
              onSelect={updateTimePart}
            />
          ) : null}
        </div>

        <div className="admin-datetime-range-panel__footer">
          <button className="admin-datetime-range-panel__clear" type="button" onClick={onClear}>
            清空
          </button>
          <div className="admin-datetime-range-panel__footer-actions">
            <BzButton onClick={onCancel}>取消</BzButton>
            <BzButton buttonType="primary" onClick={confirm}>
              确定
            </BzButton>
          </div>
        </div>
      </div>
    </div>
  );
}

function PickerRow({
  label,
  dateValue,
  timeValue,
  precision,
  activeDate,
  activeTime,
  onDateValueChange,
  onTimeValueChange,
  onDateIconClick,
  onTimeIconClick,
}: {
  label: string;
  dateValue: string;
  timeValue: string;
  precision: Precision;
  activeDate: boolean;
  activeTime: boolean;
  onDateValueChange: (value: string) => void;
  onTimeValueChange: (value: string) => void;
  onDateIconClick: () => void;
  onTimeIconClick: () => void;
}) {
  return (
    <div className="admin-datetime-range-panel__row">
      <div className="admin-datetime-range-panel__row-label">{label}</div>
      <div className="admin-datetime-range-panel__row-controls">
        <DateTextField
          value={dateValue}
          active={activeDate}
          placeholder="年/月/日"
          onValueChange={onDateValueChange}
          onIconClick={onDateIconClick}
        />
        <TimeTextField
          value={normalizeTimeValue(timeValue, precision)}
          active={activeTime}
          placeholder={precision === "second" ? "00:00:00" : "00:00:00"}
          onValueChange={onTimeValueChange}
          onIconClick={onTimeIconClick}
        />
      </div>
    </div>
  );
}

function DateTextField({ value, active, placeholder, onValueChange, onIconClick }: DateTextFieldProps) {
  return (
    <div className={`admin-datetime-range-panel__input-shell${active ? " is-active" : ""}`}>
      <input
        className="admin-datetime-range-panel__text-input"
        value={value}
        placeholder={placeholder}
        onChange={(event) => onValueChange(filterDateText(event.currentTarget.value))}
        onBlur={(event) => onValueChange(toDateInputText(normalizeDateText(event.currentTarget.value)))}
      />
      <button className="admin-datetime-range-panel__icon-button" type="button" onClick={onIconClick}>
        <CalendarIcon />
      </button>
    </div>
  );
}

function TimeTextField({ value, active, placeholder, onValueChange, onIconClick }: TimeTextFieldProps) {
  return (
    <div className={`admin-datetime-range-panel__input-shell${active ? " is-active" : ""}`}>
      <input
        className="admin-datetime-range-panel__text-input"
        value={value}
        placeholder={placeholder}
        onChange={(event) => onValueChange(filterTimeText(event.currentTarget.value))}
      />
      <button className="admin-datetime-range-panel__icon-button" type="button" onClick={onIconClick}>
        <ClockIcon />
      </button>
    </div>
  );
}

function CalendarPanel({
  month,
  selectedDate,
  timeZone,
  onMonthChange,
  onSelect,
}: {
  month: { year: number; month: number };
  selectedDate: string;
  timeZone: string;
  onMonthChange: (value: { year: number; month: number }) => void;
  onSelect: (value: string) => void;
}) {
  const cells = buildCalendarCells(month.year, month.month, timeZone);
  const monthLabel = `${month.year}年${String(month.month).padStart(2, "0")}月`;
  const today = getTodayDateString(timeZone);

  return (
    <div className="admin-datetime-range-calendar">
      <div className="admin-datetime-range-calendar__head">
        <button className="admin-datetime-range-calendar__month" type="button">
          {monthLabel}
          <span aria-hidden="true">▼</span>
        </button>
        <div className="admin-datetime-range-calendar__switches">
          <button type="button" onClick={() => onMonthChange(shiftMonth(month, -1))}>
            <ArrowUpIcon left />
          </button>
          <button type="button" onClick={() => onMonthChange(shiftMonth(month, 1))}>
            <ArrowUpIcon />
          </button>
        </div>
      </div>
      <div className="admin-datetime-range-calendar__weekdays">
        {["一", "二", "三", "四", "五", "六", "日"].map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>
      <div className="admin-datetime-range-calendar__grid">
        {cells.map((cell) => {
          const active = cell.value === selectedDate;
          const isToday = cell.value === today;
          return (
            <button
              key={cell.key}
              className={`admin-datetime-range-calendar__cell${cell.muted ? " is-muted" : ""}${active ? " is-active" : ""}${isToday ? " is-today" : ""}`}
              type="button"
              onClick={() => onSelect(cell.value)}
            >
              {cell.label}
            </button>
          );
        })}
      </div>
      <div className="admin-datetime-range-calendar__footer">
        <button type="button" onClick={() => onSelect("")}>清除</button>
        <button type="button" onClick={() => onSelect(today)}>今天</button>
      </div>
    </div>
  );
}

function TimePanel({
  precision,
  selectedTime,
  onSelect,
}: {
  precision: Precision;
  selectedTime: string;
  onSelect: (value: string) => void;
}) {
  const normalized = normalizeTimeValue(selectedTime, precision);
  const [selectedHour, selectedMinute, selectedSecond] = normalized.split(":");
  const hours = buildNumberList(0, 23);
  const minutes = buildNumberList(0, 59);
  const seconds = precision === "second" ? buildNumberList(0, 59) : [];

  function compose(nextHour = selectedHour, nextMinute = selectedMinute, nextSecond = selectedSecond || "00") {
    if (precision === "second") {
      onSelect(`${nextHour}:${nextMinute}:${nextSecond}`);
      return;
    }
    onSelect(`${nextHour}:${nextMinute}`);
  }

  return (
    <div className={`admin-datetime-range-time${precision === "second" ? " is-second" : ""}`}>
      <TimeColumn values={hours} selected={selectedHour} onSelect={(value) => compose(value)} />
      <TimeColumn values={minutes} selected={selectedMinute} onSelect={(value) => compose(selectedHour, value)} />
      {precision === "second" ? (
        <TimeColumn values={seconds} selected={selectedSecond || "00"} onSelect={(value) => compose(selectedHour, selectedMinute, value)} />
      ) : null}
    </div>
  );
}

function TimeColumn({ values, selected, onSelect }: { values: string[]; selected: string; onSelect: (value: string) => void }) {
  return (
    <div className="admin-datetime-range-time__column">
      {values.map((value) => (
        <button
          key={value}
          className={`admin-datetime-range-time__cell${value === selected ? " is-active" : ""}`}
          type="button"
          onClick={() => onSelect(value)}
        >
          {value}
        </button>
      ))}
    </div>
  );
}

function buildCalendarCells(year: number, month: number, timeZone: string) {
  const first = new Date(Date.UTC(year, month - 1, 1, 12, 0, 0));
  const last = new Date(Date.UTC(year, month, 0, 12, 0, 0));
  const firstWeekday = normalizeWeekday(getWeekday(first, timeZone));
  const previousMonthLast = new Date(Date.UTC(year, month - 1, 0, 12, 0, 0));
  const cells: Array<{ key: string; label: string; value: string; muted: boolean }> = [];

  for (let index = firstWeekday - 1; index > 0; index -= 1) {
    const day = previousMonthLast.getUTCDate() - index + 1;
    const value = toDateString(previousMonthLast.getUTCFullYear(), previousMonthLast.getUTCMonth() + 1, day);
    cells.push({ key: `prev-${value}`, label: String(day), value, muted: true });
  }

  for (let day = 1; day <= last.getUTCDate(); day += 1) {
    const value = toDateString(year, month, day);
    cells.push({ key: value, label: String(day), value, muted: false });
  }

  let nextDay = 1;
  while (cells.length < 42) {
    const nextMonth = shiftMonth({ year, month }, 1);
    const value = toDateString(nextMonth.year, nextMonth.month, nextDay);
    cells.push({ key: `next-${value}`, label: String(nextDay), value, muted: true });
    nextDay += 1;
  }

  return cells;
}

function initCalendarMonth(value: string, timeZone: string): { year: number; month: number } {
  const dateValue = splitDatePart(value);
  if (dateValue) {
    const [year, month] = dateValue.split("-").map(Number);
    if (Number.isFinite(year) && Number.isFinite(month)) {
      return { year, month };
    }
  }
  const today = getTodayDateString(timeZone).split("-").map(Number);
  return { year: today[0] || new Date().getFullYear(), month: today[1] || new Date().getMonth() + 1 };
}

function shiftMonth(current: { year: number; month: number }, delta: number) {
  const pivot = new Date(Date.UTC(current.year, current.month - 1 + delta, 1, 12, 0, 0));
  return { year: pivot.getUTCFullYear(), month: pivot.getUTCMonth() + 1 };
}

function normalizeWeekday(value: number): number {
  return value === 0 ? 7 : value;
}

function getWeekday(date: Date, timeZone: string): number {
  const formatter = new Intl.DateTimeFormat("en-US", { timeZone, weekday: "short" });
  const weekday = formatter.format(date);
  return ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].indexOf(weekday);
}

function splitDatePart(value: string): string {
  return value ? value.slice(0, 10) : "";
}

function toDateInputText(value: string): string {
  return value ? value.replace(/-/g, "/") : "";
}

function normalizeDateText(value: string): string {
  if (!value) return "";
  const normalized = value.trim().replace(/[.]/g, "/").replace(/-/g, "/");
  const matched = normalized.match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})$/);
  if (!matched) return "";
  const [, year, month, day] = matched;
  return `${year}-${pad(Number(month))}-${pad(Number(day))}`;
}

function splitTimePart(value: string, precision: Precision): string {
  if (!value) return precision === "second" ? "00:00:00" : "00:00";
  const raw = value.split("T")[1] || "";
  return normalizeTimeValue(raw, precision);
}

function normalizeTimeValue(value: string, precision: Precision): string {
  if (!value) return precision === "second" ? "00:00:00" : "00:00";
  const normalized = filterTimeText(value);
  if (precision === "second") {
    if (normalized.length >= 8) return normalized.slice(0, 8);
    if (normalized.length >= 5) return `${normalized.slice(0, 5)}:00`;
    return "00:00:00";
  }
  if (normalized.length >= 5) return normalized.slice(0, 5);
  return "00:00";
}

function filterDateText(value: string): string {
  return value.replace(/[^\d/\-]/g, "").slice(0, 10);
}

function filterTimeText(value: string): string {
  return value.replace(/[^\d:]/g, "").slice(0, 8);
}

function buildDateTimeValue(date: string, time: string, precision: Precision): string {
  if (!date) return "";
  return `${date}T${normalizeTimeValue(time, precision)}`;
}

function buildShortcutRanges(timeZone: string, precision: Precision) {
  const today = getTodayMarker(timeZone);
  const yesterday = shiftDays(today, -1);
  const sevenDaysStart = shiftDays(today, -6);
  const thirtyDaysStart = shiftDays(today, -29);
  const monthStart = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), 1, 12, 0, 0));
  const lastMonthStart = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth() - 1, 1, 12, 0, 0));
  const lastMonthEnd = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), 0, 12, 0, 0));
  const currentMonthEnd = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth() + 1, 0, 12, 0, 0));

  return [
    { key: "today", label: "今天", ...buildDayRange(today, today, timeZone, precision) },
    { key: "yesterday", label: "昨天", ...buildDayRange(yesterday, yesterday, timeZone, precision) },
    { key: "last7", label: "近 7 天", ...buildDayRange(sevenDaysStart, today, timeZone, precision) },
    { key: "last30", label: "近 30 天", ...buildDayRange(thirtyDaysStart, today, timeZone, precision) },
    { key: "thisMonth", label: "本月", ...buildDayRange(monthStart, currentMonthEnd, timeZone, precision) },
    { key: "lastMonth", label: "上月", ...buildDayRange(lastMonthStart, lastMonthEnd, timeZone, precision) },
  ];
}

function buildDayRange(startDate: Date, endDate: Date, timeZone: string, precision: Precision) {
  return {
    start: toDateTimeString(startDate, timeZone, precision, false),
    end: toDateTimeString(endDate, timeZone, precision, true),
  };
}

function toDateTimeString(date: Date, timeZone: string, precision: Precision, endOfDay: boolean) {
  const parts = getDateParts(date, timeZone);
  const timePart = endOfDay
    ? precision === "second"
      ? "23:59:59"
      : "23:59"
    : precision === "second"
      ? "00:00:00"
      : "00:00";
  return `${toDateString(parts.year, parts.month, parts.day)}T${timePart}`;
}

function getTodayMarker(timeZone: string): Date {
  const parts = getDateParts(new Date(), timeZone);
  return new Date(Date.UTC(parts.year, parts.month - 1, parts.day, 12, 0, 0));
}

function getTodayDateString(timeZone: string): string {
  const parts = getDateParts(new Date(), timeZone);
  return toDateString(parts.year, parts.month, parts.day);
}

function shiftDays(date: Date, amount: number): Date {
  const next = new Date(date.getTime());
  next.setUTCDate(next.getUTCDate() + amount);
  return next;
}

function getDateParts(date: Date, timeZone: string): ZonedDateParts {
  const formatter = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
  const values: Record<string, string> = {};
  formatter.formatToParts(date).forEach((chunk) => {
    if (chunk.type !== "literal") values[chunk.type] = chunk.value;
  });
  return {
    year: Number(values.year ?? "0"),
    month: Number(values.month ?? "1"),
    day: Number(values.day ?? "1"),
  };
}

function toDateString(year: number, month: number, day: number): string {
  return `${year}-${pad(month)}-${pad(day)}`;
}

function buildNumberList(start: number, end: number): string[] {
  const values: string[] = [];
  for (let value = start; value <= end; value += 1) {
    values.push(pad(value));
  }
  return values;
}

function resolvePattern(value: string): string {
  if (value === "YYYY_MM_DD_HH_MM_SS") return "yyyy-MM-dd HH:mm:ss";
  if (value === "YYYY_SLASH_MM_DD_HH_MM_SS") return "yyyy/MM/dd HH:mm:ss";
  if (value === "DD_SLASH_MM_YYYY_HH_MM_SS") return "dd/MM/yyyy HH:mm:ss";
  if (value === "MM_DD_YYYY_HH_MM") return "MM-dd-yyyy HH:mm";
  if (value === "YYYY_MM_DD_HH_MM") return "yyyy-MM-dd HH:mm";
  return value || "yyyy-MM-dd HH:mm:ss";
}

function pad(value: number): string {
  return value < 10 ? `0${value}` : String(value);
}

function CalendarIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M7 3v3M17 3v3M4 8h16M5 5h14a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.7"
      />
    </svg>
  );
}

function ClockIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.7" />
      <path d="M12 7.5v5l3 1.8" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
    </svg>
  );
}

function ArrowUpIcon({ left = false }: { left?: boolean }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" style={{ transform: left ? "rotate(-90deg)" : "rotate(90deg)" }}>
      <path d="m9 6 6 6-6 6" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
    </svg>
  );
}

export function buildAdminDateTimeRangeSubmitParams(start: string, end: string) {
  return buildDateTimeRangeSubmitValue(start, end);
}
