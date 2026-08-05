"use client";

import {
  buildDateTimeRangeSubmitValue,
  formatDateTimeInputValue,
  getUserDateTimeFormatPattern,
  getUserTimeZone,
  resolveDateTimePrecision,
  resolveUserDateTimeFormatCode,
  resolveUserTimeZoneCode,
} from "@admin/core/formatter";
import { usePersonalizedConfigs } from "@admin/core/registry/auth-registry";
import type {
  ClipboardEvent,
  CSSProperties,
  MouseEvent as ReactMouseEvent,
  RefObject,
} from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";

import { BzButton } from "../bz/BzButton";

interface AdminDateTimeRangeFieldProps {
  startValue?: string;
  endValue?: string;
  placeholder?: string;
  allowSingleSided?: boolean;
  onRangeChange?: (range: { start: string; end: string }) => void;
}

type Precision = "minute" | "second";
type RangeRow = "start" | "end";
type DateTimePart = "year" | "month" | "day" | "hour" | "minute" | "second";
type PickerType = "date" | "time";

interface ActivePicker {
  row: RangeRow;
  type: PickerType;
}

interface ZonedDateParts {
  year: number;
  month: number;
  day: number;
}

interface DateTimeParts {
  year: string;
  month: string;
  day: string;
  hour: string;
  minute: string;
  second: string;
}

const RANGE_ROWS: RangeRow[] = ["start", "end"];
const DATE_PARTS: DateTimePart[] = ["year", "month", "day"];
const TIME_PARTS: DateTimePart[] = ["hour", "minute", "second"];

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
  const [panelStyle, setPanelStyle] = useState<CSSProperties>({});

  const configs = usePersonalizedConfigs();
  const dateTimePattern = useMemo(() => {
    const configured = configs.find((item) => item.code === "USER_DATE_TIME_FORMAT")?.value?.trim();
    return configured ? resolveUserDateTimeFormatCode(configured) : getUserDateTimeFormatPattern();
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
    const viewportWidth = window.innerWidth;
    const margin = 12;
    const width = Math.max(320, Math.min(540, viewportWidth - margin * 2));
    let left = triggerRect.left;
    if (left + width > viewportWidth - margin) {
      left = Math.max(margin, viewportWidth - width - margin);
    }

    setPanelStyle({
      position: "fixed",
      left: `${left}px`,
      top: `${triggerRect.bottom + 8}px`,
      width: `${width}px`,
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
    <div
      ref={rootRef}
      className="admin-datetime-range"
    >
      <button
        ref={triggerRef}
        className={`admin-datetime-range-trigger${displayText ? " has-value" : ""}${open ? " is-open" : ""}`}
        type="button"
        title={displayText || placeholder}
        onClick={() => setOpen((value) => !value)}
      >
        <span
          className={`admin-datetime-range-trigger__text${displayText ? "" : " is-placeholder"}`}
        >
          {displayText || placeholder}
        </span>
        <span
          className="admin-datetime-range-trigger__icon"
          aria-hidden="true"
        >
          <CalendarIcon />
        </span>
      </button>

      {open && typeof document !== "undefined"
        ? createPortal(
            <div
              ref={panelRef}
              className="admin-datetime-range-popover"
              style={panelStyle}
            >
              <AdminDateTimeRangePanel
                startValue={startValue}
                endValue={endValue}
                precision={precision}
                timeZone={timeZone}
                allowSingleSided={allowSingleSided}
                onCancel={() => setOpen(false)}
                onClear={() => {
                  onRangeChange?.({ start: "", end: "" });
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
  const mainRef = useRef<HTMLDivElement | null>(null);
  const pickerRef = useRef<HTMLDivElement | null>(null);
  const dateFieldRefs = useRef<Record<RangeRow, HTMLDivElement | null>>({ start: null, end: null });
  const timeFieldRefs = useRef<Record<RangeRow, HTMLDivElement | null>>({ start: null, end: null });
  const inputRefs = useRef<
    Record<RangeRow, Partial<Record<DateTimePart, HTMLInputElement | null>>>
  >({
    start: {},
    end: {},
  });
  const [range, setRange] = useState<{ start: DateTimeParts; end: DateTimeParts }>({
    start: parseDateTimeParts(startValue, precision),
    end: parseDateTimeParts(endValue, precision),
  });
  const [error, setError] = useState("");
  const [activePicker, setActivePicker] = useState<ActivePicker | null>(null);
  const [calendarMonth, setCalendarMonth] = useState(() =>
    initCalendarMonth(startValue || endValue, timeZone),
  );
  const [pickerStyle, setPickerStyle] = useState<CSSProperties>({});
  const [activeShortcut, setActiveShortcut] = useState("");

  const shortcuts = useMemo(() => buildShortcutRanges(timeZone, precision), [timeZone, precision]);
  const yearOptions = useMemo(
    () => buildYearOptions(calendarMonth.year, timeZone),
    [calendarMonth.year, timeZone],
  );
  const monthOptions = useMemo(() => buildNumberList(1, 12), []);

  useEffect(() => {
    if (!activePicker) return;
    requestAnimationFrame(() => positionPicker(activePicker));
  }, [activePicker, calendarMonth, precision]);

  useEffect(() => {
    if (!activePicker) return;

    function handleMouseDown(event: MouseEvent) {
      const target = event.target as Node;
      if (pickerRef.current?.contains(target)) return;
      if (
        dateFieldRefs.current.start?.contains(target) ||
        dateFieldRefs.current.end?.contains(target)
      )
        return;
      if (
        timeFieldRefs.current.start?.contains(target) ||
        timeFieldRefs.current.end?.contains(target)
      )
        return;
      setActivePicker(null);
    }

    document.addEventListener("mousedown", handleMouseDown);
    return () => document.removeEventListener("mousedown", handleMouseDown);
  }, [activePicker]);

  function positionPicker(picker: ActivePicker) {
    const anchor =
      picker.type === "date"
        ? dateFieldRefs.current[picker.row]
        : timeFieldRefs.current[picker.row];
    if (!anchor || !mainRef.current) return;
    const anchorRect = anchor.getBoundingClientRect();
    const mainRect = mainRef.current.getBoundingClientRect();
    const width = picker.type === "date" ? 260 : precision === "second" ? 168 : 112;
    const maxLeft = Math.max(8, mainRect.width - width - 8);
    const left = clamp(anchorRect.left - mainRect.left, 8, maxLeft);
    setPickerStyle({
      left: `${left}px`,
      top: `${anchorRect.bottom - mainRect.top + 8}px`,
      width: `${width}px`,
    });
  }

  function setInputRef(row: RangeRow, part: DateTimePart, element: HTMLInputElement | null) {
    inputRefs.current[row][part] = element;
  }

  function setDateFieldRef(row: RangeRow, element: HTMLDivElement | null) {
    dateFieldRefs.current[row] = element;
  }

  function setTimeFieldRef(row: RangeRow, element: HTMLDivElement | null) {
    timeFieldRefs.current[row] = element;
  }

  function setRowParts(row: RangeRow, next: Partial<DateTimeParts>) {
    setRange((current) => ({
      ...current,
      [row]: { ...current[row], ...next },
    }));
  }

  function focusSegment(row: RangeRow, part: DateTimePart) {
    const input = inputRefs.current[row][part];
    if (!input || input.offsetParent === null) return;
    requestAnimationFrame(() => {
      input.focus();
      input.select();
    });
  }

  function clearShortcutState() {
    if (activeShortcut) setActiveShortcut("");
  }

  function ensureTimeDefaults(row: RangeRow) {
    setRange((current) => ({
      ...current,
      [row]: normalizeTimeDefaults(current[row], precision),
    }));
  }

  function handleFieldShellClick(
    event: ReactMouseEvent<HTMLDivElement>,
    row: RangeRow,
    type: PickerType,
  ) {
    const target = event.target as HTMLElement;
    if (target.closest("button") || target.closest("input")) return;
    setActivePicker(null);
    if (type === "time") ensureTimeDefaults(row);
    focusSegment(row, type === "date" ? "year" : "hour");
  }

  function openDatePicker(row: RangeRow) {
    setError("");
    setCalendarMonth(monthForRow(range[row], timeZone));
    setActivePicker({ row, type: "date" });
  }

  function openTimePicker(row: RangeRow) {
    setError("");
    ensureTimeDefaults(row);
    setActivePicker({ row, type: "time" });
  }

  function handleSegmentInput(row: RangeRow, part: DateTimePart, rawValue: string) {
    const nextValue = cleanDigits(rawValue, part);
    clearShortcutState();
    setError("");

    setRange((current) => {
      const nextRow = { ...current[row] };
      if (TIME_PARTS.includes(part)) {
        Object.assign(nextRow, normalizeTimeDefaults(nextRow, precision));
      }
      nextRow[part] = nextValue;
      return { ...current, [row]: nextRow };
    });

    if (nextValue.length >= limitForPart(part)) {
      const nextPart = getNextPart(part, precision);
      if (nextPart) focusSegment(row, nextPart);
    }
  }

  function handleSegmentBlur(row: RangeRow, part: DateTimePart, value: string) {
    setRowParts(row, { [part]: normalizePartValue(part, value) } as Partial<DateTimeParts>);
  }

  function handleSegmentPaste(
    event: ClipboardEvent<HTMLInputElement>,
    row: RangeRow,
    part: DateTimePart,
  ) {
    const digits = event.clipboardData.getData("text").replace(/\D/g, "");
    if (!digits) return;

    if (DATE_PARTS.includes(part) && digits.length >= 8) {
      event.preventDefault();
      clearShortcutState();
      setRowParts(row, {
        year: digits.slice(0, 4),
        month: digits.slice(4, 6),
        day: digits.slice(6, 8),
      });
      focusSegment(row, "hour");
      return;
    }

    if (TIME_PARTS.includes(part) && digits.length >= 4) {
      event.preventDefault();
      clearShortcutState();
      setRowParts(row, {
        hour: digits.slice(0, 2),
        minute: digits.slice(2, 4),
        second: precision === "second" ? digits.slice(4, 6) || "00" : "",
      });
      const nextPart = precision === "second" ? "second" : null;
      if (nextPart) focusSegment(row, nextPart);
    }
  }

  function handleDateSelect(value: string) {
    if (!activePicker || activePicker.type !== "date") return;
    clearShortcutState();
    setError("");
    if (!value) {
      setRowParts(activePicker.row, { year: "", month: "", day: "" });
      setActivePicker(null);
      return;
    }
    const [year, month, day] = value.split("-");
    setRowParts(activePicker.row, { year, month, day });
    setActivePicker(null);
    focusSegment(activePicker.row, "hour");
  }

  function handleTimeSelect(
    part: Extract<DateTimePart, "hour" | "minute" | "second">,
    value: string,
  ) {
    if (!activePicker || activePicker.type !== "time") return;
    clearShortcutState();
    ensureTimeDefaults(activePicker.row);
    setRowParts(activePicker.row, { [part]: value } as Partial<DateTimeParts>);
  }

  function applyShortcut(shortcutKey: string) {
    const today = datePartsToMarker(getTodayDateParts(timeZone));
    const yesterday = shiftDays(today, -1);
    const last7Start = shiftDays(today, -6);
    const last30Start = shiftDays(today, -29);
    const monthStart = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), 1, 12, 0, 0));
    const monthEnd = new Date(
      Date.UTC(today.getUTCFullYear(), today.getUTCMonth() + 1, 0, 12, 0, 0),
    );
    const lastMonthStart = new Date(
      Date.UTC(today.getUTCFullYear(), today.getUTCMonth() - 1, 1, 12, 0, 0),
    );
    const lastMonthEnd = new Date(
      Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), 0, 12, 0, 0),
    );

    let start = today;
    let end = today;
    if (shortcutKey === "yesterday") start = end = yesterday;
    if (shortcutKey === "last7") {
      start = last7Start;
      end = today;
    }
    if (shortcutKey === "last30") {
      start = last30Start;
      end = today;
    }
    if (shortcutKey === "thisMonth") {
      start = monthStart;
      end = monthEnd;
    }
    if (shortcutKey === "lastMonth") {
      start = lastMonthStart;
      end = lastMonthEnd;
    }

    setActiveShortcut(shortcutKey);
    setError("");
    setActivePicker(null);
    setRange({
      start: buildPartsFromMarker(start, precision, false),
      end: buildPartsFromMarker(end, precision, true),
    });
    setCalendarMonth({ year: start.getUTCFullYear(), month: start.getUTCMonth() + 1 });
  }

  function validateRange() {
    const startAny = hasAnyValue(range.start);
    const endAny = hasAnyValue(range.end);
    if (startAny && !hasDateValue(range.start)) return "请填写完整的开始日期";
    if (endAny && !hasDateValue(range.end)) return "请填写完整的结束日期";

    const start = buildComparableDate(range.start, precision);
    const end = buildComparableDate(range.end, precision);
    if (!allowSingleSided && Boolean(start) !== Boolean(end)) return "请选择完整的时间范围";
    if (start && end && start.getTime() > end.getTime()) return "开始时间不能晚于结束时间";
    return "";
  }

  function confirm() {
    setError("");
    const message = validateRange();
    if (message) {
      setError(message);
      return;
    }

    onConfirm({
      start: buildDateTimeValueFromParts(range.start, precision),
      end: buildDateTimeValueFromParts(range.end, precision),
    });
  }

  function clearRange() {
    clearShortcutState();
    setError("");
    setActivePicker(null);
    setRange({ start: emptyParts(), end: emptyParts() });
    onClear();
  }

  const selectedDate = activePicker ? toDateStringFromParts(range[activePicker.row]) : "";
  const selectedTime = activePicker
    ? toTimeStringFromParts(range[activePicker.row], precision)
    : "";

  return (
    <div className="admin-datetime-range-panel">
      <div className="admin-datetime-range-panel__sidebar">
        <div className="admin-datetime-range-panel__sidebar-title">快捷范围</div>
        {shortcuts.map((shortcut) => (
          <button
            key={shortcut.key}
            className={`admin-datetime-range-panel__shortcut${shortcut.key === activeShortcut ? " is-active" : ""}`}
            type="button"
            onClick={() => applyShortcut(shortcut.key)}
          >
            {shortcut.label}
          </button>
        ))}
      </div>

      <div
        ref={mainRef}
        className="admin-datetime-range-panel__main"
      >
        {RANGE_ROWS.map((row) => (
          <div
            key={row}
            className="admin-datetime-range-panel__row"
          >
            <div className="admin-datetime-range-panel__row-label">
              {row === "start" ? "开始时间" : "结束时间"}
            </div>
            <div
              ref={(element) => setDateFieldRef(row, element)}
              className={`admin-datetime-range-panel__seg-field${activePicker?.row === row && activePicker.type === "date" ? " is-active" : ""}`}
              onClick={(event) => handleFieldShellClick(event, row, "date")}
            >
              <div className="admin-datetime-range-panel__segments">
                <input
                  ref={(element) => setInputRef(row, "year", element)}
                  className="admin-datetime-range-panel__seg-input admin-datetime-range-panel__seg-input--year"
                  value={range[row].year}
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="年"
                  onInput={(event) => handleSegmentInput(row, "year", event.currentTarget.value)}
                  onBlur={(event) => handleSegmentBlur(row, "year", event.currentTarget.value)}
                  onPaste={(event) => handleSegmentPaste(event, row, "year")}
                  onFocus={() => {
                    setActivePicker(null);
                    setError("");
                  }}
                />
                <span className="admin-datetime-range-panel__seg-separator">/</span>
                <input
                  ref={(element) => setInputRef(row, "month", element)}
                  className="admin-datetime-range-panel__seg-input admin-datetime-range-panel__seg-input--date"
                  value={range[row].month}
                  inputMode="numeric"
                  maxLength={2}
                  placeholder="月"
                  onInput={(event) => handleSegmentInput(row, "month", event.currentTarget.value)}
                  onBlur={(event) => handleSegmentBlur(row, "month", event.currentTarget.value)}
                  onPaste={(event) => handleSegmentPaste(event, row, "month")}
                  onFocus={() => {
                    setActivePicker(null);
                    setError("");
                  }}
                />
                <span className="admin-datetime-range-panel__seg-separator">/</span>
                <input
                  ref={(element) => setInputRef(row, "day", element)}
                  className="admin-datetime-range-panel__seg-input admin-datetime-range-panel__seg-input--date"
                  value={range[row].day}
                  inputMode="numeric"
                  maxLength={2}
                  placeholder="日"
                  onInput={(event) => handleSegmentInput(row, "day", event.currentTarget.value)}
                  onBlur={(event) => handleSegmentBlur(row, "day", event.currentTarget.value)}
                  onPaste={(event) => handleSegmentPaste(event, row, "day")}
                  onFocus={() => {
                    setActivePicker(null);
                    setError("");
                  }}
                />
              </div>
              <button
                className="admin-datetime-range-panel__icon-button"
                type="button"
                onClick={() => openDatePicker(row)}
              >
                <CalendarIcon />
              </button>
            </div>
            <div
              ref={(element) => setTimeFieldRef(row, element)}
              className={`admin-datetime-range-panel__seg-field${activePicker?.row === row && activePicker.type === "time" ? " is-active" : ""}`}
              onClick={(event) => handleFieldShellClick(event, row, "time")}
            >
              <div className="admin-datetime-range-panel__segments">
                <input
                  ref={(element) => setInputRef(row, "hour", element)}
                  className="admin-datetime-range-panel__seg-input admin-datetime-range-panel__seg-input--time"
                  value={range[row].hour}
                  inputMode="numeric"
                  maxLength={2}
                  placeholder="时"
                  onInput={(event) => handleSegmentInput(row, "hour", event.currentTarget.value)}
                  onBlur={(event) => handleSegmentBlur(row, "hour", event.currentTarget.value)}
                  onPaste={(event) => handleSegmentPaste(event, row, "hour")}
                  onFocus={() => {
                    ensureTimeDefaults(row);
                    setActivePicker(null);
                    setError("");
                  }}
                />
                <span className="admin-datetime-range-panel__seg-separator">:</span>
                <input
                  ref={(element) => setInputRef(row, "minute", element)}
                  className="admin-datetime-range-panel__seg-input admin-datetime-range-panel__seg-input--time"
                  value={range[row].minute}
                  inputMode="numeric"
                  maxLength={2}
                  placeholder="分"
                  onInput={(event) => handleSegmentInput(row, "minute", event.currentTarget.value)}
                  onBlur={(event) => handleSegmentBlur(row, "minute", event.currentTarget.value)}
                  onPaste={(event) => handleSegmentPaste(event, row, "minute")}
                  onFocus={() => {
                    ensureTimeDefaults(row);
                    setActivePicker(null);
                    setError("");
                  }}
                />
                <span
                  className={`admin-datetime-range-panel__seg-separator${precision === "second" ? "" : " is-hidden"}`}
                >
                  :
                </span>
                <input
                  ref={(element) => setInputRef(row, "second", element)}
                  className={`admin-datetime-range-panel__seg-input admin-datetime-range-panel__seg-input--time${precision === "second" ? "" : " is-hidden"}`}
                  value={precision === "second" ? range[row].second : ""}
                  inputMode="numeric"
                  maxLength={2}
                  placeholder="秒"
                  onInput={(event) => handleSegmentInput(row, "second", event.currentTarget.value)}
                  onBlur={(event) => handleSegmentBlur(row, "second", event.currentTarget.value)}
                  onPaste={(event) => handleSegmentPaste(event, row, "second")}
                  onFocus={() => {
                    ensureTimeDefaults(row);
                    setActivePicker(null);
                    setError("");
                  }}
                />
              </div>
              <button
                className="admin-datetime-range-panel__icon-button"
                type="button"
                onClick={() => openTimePicker(row)}
              >
                <ClockIcon />
              </button>
            </div>
          </div>
        ))}

        {error ? <div className="admin-datetime-range-panel__error">{error}</div> : null}

        {activePicker ? (
          <div
            ref={pickerRef}
            className="admin-datetime-range-panel__picker-popover"
            style={pickerStyle}
          >
            {activePicker.type === "date" ? (
              <CalendarPanel
                month={calendarMonth}
                selectedDate={selectedDate}
                today={getTodayDateString(timeZone)}
                yearOptions={yearOptions}
                monthOptions={monthOptions}
                onYearChange={(value) =>
                  setCalendarMonth((current) => ({ ...current, year: value }))
                }
                onMonthChange={(value) =>
                  setCalendarMonth((current) => ({ ...current, month: value }))
                }
                onSelect={handleDateSelect}
              />
            ) : (
              <TimePanel
                precision={precision}
                selectedTime={selectedTime}
                onSelect={handleTimeSelect}
              />
            )}
          </div>
        ) : null}

        <div className="admin-datetime-range-panel__footer">
          <button
            className="admin-datetime-range-panel__clear"
            type="button"
            onClick={clearRange}
          >
            清空
          </button>
          <div className="admin-datetime-range-panel__footer-actions">
            <BzButton onClick={onCancel}>取消</BzButton>
            <BzButton
              buttonType="primary"
              onClick={confirm}
            >
              确定
            </BzButton>
          </div>
        </div>
      </div>
    </div>
  );
}

function CalendarPanel({
  month,
  selectedDate,
  today,
  yearOptions,
  monthOptions,
  onYearChange,
  onMonthChange,
  onSelect,
}: {
  month: { year: number; month: number };
  selectedDate: string;
  today: string;
  yearOptions: number[];
  monthOptions: string[];
  onYearChange: (value: number) => void;
  onMonthChange: (value: number) => void;
  onSelect: (value: string) => void;
}) {
  const cells = buildCalendarCells(month.year, month.month);

  return (
    <div className="admin-datetime-range-calendar">
      <div className="admin-datetime-range-calendar__head">
        <div className="admin-datetime-range-calendar__title">
          <select
            className="admin-datetime-range-calendar__select"
            value={month.year}
            aria-label="选择年份"
            onChange={(event) => onYearChange(Number(event.currentTarget.value))}
          >
            {yearOptions.map((year) => (
              <option
                key={year}
                value={year}
              >
                {year}年
              </option>
            ))}
          </select>
          <select
            className="admin-datetime-range-calendar__select"
            value={month.month}
            aria-label="选择月份"
            onChange={(event) => onMonthChange(Number(event.currentTarget.value))}
          >
            {monthOptions.map((item) => (
              <option
                key={item}
                value={Number(item)}
              >
                {item}月
              </option>
            ))}
          </select>
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
              className={[
                "admin-datetime-range-calendar__cell",
                cell.muted ? "is-muted" : "",
                isToday ? "is-today" : "",
                active ? "is-active" : "",
              ]
                .filter(Boolean)
                .join(" ")}
              type="button"
              onClick={() => onSelect(cell.value)}
            >
              {cell.label}
            </button>
          );
        })}
      </div>
      <div className="admin-datetime-range-calendar__footer">
        <button
          type="button"
          onClick={() => onSelect("")}
        >
          清除
        </button>
        <button
          type="button"
          onClick={() => onSelect(today)}
        >
          今天
        </button>
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
  onSelect: (part: Extract<DateTimePart, "hour" | "minute" | "second">, value: string) => void;
}) {
  const normalized = normalizeTimeString(selectedTime, precision);
  const [selectedHour, selectedMinute, selectedSecond] = normalized.split(":");
  const hourRef = useRef<HTMLDivElement | null>(null);
  const minuteRef = useRef<HTMLDivElement | null>(null);
  const secondRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    [hourRef.current, minuteRef.current, secondRef.current].forEach((column) => {
      const activeCell = column?.querySelector<HTMLElement>(
        ".admin-datetime-range-time__cell.is-active",
      );
      if (column && activeCell) {
        column.scrollTop = Math.max(0, activeCell.offsetTop - 56);
      }
    });
  }, [selectedHour, selectedMinute, selectedSecond, precision]);

  return (
    <div className={`admin-datetime-range-time${precision === "second" ? " is-second" : ""}`}>
      <TimeColumn
        columnRef={hourRef}
        values={buildNumberList(0, 23)}
        selected={selectedHour}
        onSelect={(value) => onSelect("hour", value)}
      />
      <TimeColumn
        columnRef={minuteRef}
        values={buildNumberList(0, 59)}
        selected={selectedMinute}
        onSelect={(value) => onSelect("minute", value)}
      />
      {precision === "second" ? (
        <TimeColumn
          columnRef={secondRef}
          values={buildNumberList(0, 59)}
          selected={selectedSecond || "00"}
          onSelect={(value) => onSelect("second", value)}
        />
      ) : null}
    </div>
  );
}

function TimeColumn({
  columnRef,
  values,
  selected,
  onSelect,
}: {
  columnRef: RefObject<HTMLDivElement | null>;
  values: string[];
  selected: string;
  onSelect: (value: string) => void;
}) {
  return (
    <div
      ref={columnRef}
      className="admin-datetime-range-time__column"
    >
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

function emptyParts(): DateTimeParts {
  return { year: "", month: "", day: "", hour: "", minute: "", second: "" };
}

function parseDateTimeParts(value: string, precision: Precision): DateTimeParts {
  if (!value) return emptyParts();
  const matched = String(value)
    .trim()
    .match(/^(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2})(?::(\d{2}))?)?$/);
  if (!matched) return emptyParts();
  return {
    year: matched[1] || "",
    month: matched[2] || "",
    day: matched[3] || "",
    hour: matched[4] || "",
    minute: matched[5] || "",
    second: precision === "second" ? matched[6] || "00" : "",
  };
}

function buildDateTimeValueFromParts(parts: DateTimeParts, precision: Precision): string {
  if (!hasDateValue(parts)) return "";
  const hour = normalizePartValue("hour", parts.hour || "0") || "00";
  const minute = normalizePartValue("minute", parts.minute || "0") || "00";
  if (precision === "second") {
    const second = normalizePartValue("second", parts.second || "0") || "00";
    return `${parts.year}-${parts.month}-${parts.day}T${hour}:${minute}:${second}`;
  }
  return `${parts.year}-${parts.month}-${parts.day}T${hour}:${minute}`;
}

function buildComparableDate(parts: DateTimeParts, precision: Precision): Date | null {
  if (!hasDateValue(parts)) return null;
  return new Date(
    Number(parts.year),
    Number(parts.month) - 1,
    Number(parts.day),
    Number(normalizePartValue("hour", parts.hour || "0") || "00"),
    Number(normalizePartValue("minute", parts.minute || "0") || "00"),
    Number(
      precision === "second" ? normalizePartValue("second", parts.second || "0") || "00" : "00",
    ),
  );
}

function hasAnyValue(parts: DateTimeParts): boolean {
  return Object.values(parts).some(Boolean);
}

function hasDateValue(parts: DateTimeParts): boolean {
  return Boolean(parts.year && parts.month && parts.day);
}

function normalizeTimeDefaults(parts: DateTimeParts, precision: Precision): DateTimeParts {
  return {
    ...parts,
    hour: parts.hour || "00",
    minute: parts.minute || "00",
    second: precision === "second" ? parts.second || "00" : "",
  };
}

function toDateStringFromParts(parts: DateTimeParts): string {
  return hasDateValue(parts) ? `${parts.year}-${parts.month}-${parts.day}` : "";
}

function toTimeStringFromParts(parts: DateTimeParts, precision: Precision): string {
  const normalized = normalizeTimeDefaults(parts, precision);
  if (precision === "second") return `${normalized.hour}:${normalized.minute}:${normalized.second}`;
  return `${normalized.hour}:${normalized.minute}`;
}

function monthForRow(parts: DateTimeParts, timeZone: string): { year: number; month: number } {
  if (parts.year && parts.month) {
    return { year: Number(parts.year), month: Number(parts.month) };
  }
  const today = getTodayDateParts(timeZone);
  return { year: today.year, month: today.month };
}

function buildYearOptions(selectedYear: number, timeZone: string): number[] {
  const currentYear = getTodayDateParts(timeZone).year || new Date().getFullYear();
  const minYear = Math.min(currentYear - 20, selectedYear - 10);
  const maxYear = Math.max(currentYear + 20, selectedYear + 10);
  const values: number[] = [];
  for (let year = minYear; year <= maxYear; year += 1) {
    values.push(year);
  }
  return values;
}

function buildCalendarCells(year: number, month: number) {
  const first = new Date(Date.UTC(year, month - 1, 1, 12, 0, 0));
  const last = new Date(Date.UTC(year, month, 0, 12, 0, 0));
  const previousMonthLast = new Date(Date.UTC(year, month - 1, 0, 12, 0, 0));
  const firstWeekday = normalizeWeekday(first.getUTCDay());
  const cells: Array<{ key: string; label: string; value: string; muted: boolean }> = [];

  for (let index = firstWeekday - 1; index > 0; index -= 1) {
    const day = previousMonthLast.getUTCDate() - index + 1;
    const previousMonth = shiftMonth({ year, month }, -1);
    cells.push({
      key: `prev-${previousMonth.year}-${previousMonth.month}-${day}`,
      label: String(day),
      value: toDateString(previousMonth.year, previousMonth.month, day),
      muted: true,
    });
  }

  for (let day = 1; day <= last.getUTCDate(); day += 1) {
    cells.push({
      key: `current-${day}`,
      label: String(day),
      value: toDateString(year, month, day),
      muted: false,
    });
  }

  let nextDay = 1;
  const nextMonth = shiftMonth({ year, month }, 1);
  while (cells.length < 42) {
    cells.push({
      key: `next-${nextDay}`,
      label: String(nextDay),
      value: toDateString(nextMonth.year, nextMonth.month, nextDay),
      muted: true,
    });
    nextDay += 1;
  }

  return cells;
}

function initCalendarMonth(value: string, timeZone: string): { year: number; month: number } {
  const matched = value.match(/^(\d{4})-(\d{2})/);
  if (matched) {
    return { year: Number(matched[1]), month: Number(matched[2]) };
  }
  const today = getTodayDateParts(timeZone);
  return { year: today.year, month: today.month };
}

function buildShortcutRanges(timeZone: string, precision: Precision) {
  return [
    {
      key: "today",
      label: "今天",
      ...buildDayRange(getTodayMarker(timeZone), getTodayMarker(timeZone), precision),
    },
    {
      key: "yesterday",
      label: "昨天",
      ...buildDayRange(
        shiftDays(getTodayMarker(timeZone), -1),
        shiftDays(getTodayMarker(timeZone), -1),
        precision,
      ),
    },
    {
      key: "last7",
      label: "近 7 天",
      ...buildDayRange(
        shiftDays(getTodayMarker(timeZone), -6),
        getTodayMarker(timeZone),
        precision,
      ),
    },
    {
      key: "last30",
      label: "近 30 天",
      ...buildDayRange(
        shiftDays(getTodayMarker(timeZone), -29),
        getTodayMarker(timeZone),
        precision,
      ),
    },
    {
      key: "thisMonth",
      label: "本月",
      ...buildDayRange(
        new Date(
          Date.UTC(
            getTodayMarker(timeZone).getUTCFullYear(),
            getTodayMarker(timeZone).getUTCMonth(),
            1,
            12,
            0,
            0,
          ),
        ),
        new Date(
          Date.UTC(
            getTodayMarker(timeZone).getUTCFullYear(),
            getTodayMarker(timeZone).getUTCMonth() + 1,
            0,
            12,
            0,
            0,
          ),
        ),
        precision,
      ),
    },
    {
      key: "lastMonth",
      label: "上月",
      ...buildDayRange(
        new Date(
          Date.UTC(
            getTodayMarker(timeZone).getUTCFullYear(),
            getTodayMarker(timeZone).getUTCMonth() - 1,
            1,
            12,
            0,
            0,
          ),
        ),
        new Date(
          Date.UTC(
            getTodayMarker(timeZone).getUTCFullYear(),
            getTodayMarker(timeZone).getUTCMonth(),
            0,
            12,
            0,
            0,
          ),
        ),
        precision,
      ),
    },
  ];
}

function buildDayRange(startDate: Date, endDate: Date, precision: Precision) {
  return {
    start: buildPartsFromMarker(startDate, precision, false),
    end: buildPartsFromMarker(endDate, precision, true),
  };
}

function buildPartsFromMarker(
  marker: Date,
  precision: Precision,
  endOfDay: boolean,
): DateTimeParts {
  const year = String(marker.getUTCFullYear());
  const month = pad(marker.getUTCMonth() + 1);
  const day = pad(marker.getUTCDate());
  return {
    year,
    month,
    day,
    hour: endOfDay ? "23" : "00",
    minute: endOfDay ? "59" : "00",
    second: precision === "second" ? (endOfDay ? "59" : "00") : "",
  };
}

function getTodayMarker(timeZone: string): Date {
  return datePartsToMarker(getTodayDateParts(timeZone));
}

function getTodayDateParts(timeZone: string): ZonedDateParts {
  return getDateParts(new Date(), timeZone);
}

function getTodayDateString(timeZone: string): string {
  const today = getTodayDateParts(timeZone);
  return toDateString(today.year, today.month, today.day);
}

function datePartsToMarker(parts: ZonedDateParts): Date {
  return new Date(Date.UTC(parts.year, parts.month - 1, parts.day, 12, 0, 0));
}

function shiftDays(marker: Date, amount: number): Date {
  const next = new Date(marker.getTime());
  next.setUTCDate(next.getUTCDate() + amount);
  return next;
}

function shiftMonth(current: { year: number; month: number }, delta: number) {
  const pivot = new Date(Date.UTC(current.year, current.month - 1 + delta, 1, 12, 0, 0));
  return { year: pivot.getUTCFullYear(), month: pivot.getUTCMonth() + 1 };
}

function getDateParts(date: Date, timeZone: string): ZonedDateParts {
  const formatter = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
  const values: Record<string, string> = {};
  formatter.formatToParts(date).forEach((part) => {
    if (part.type !== "literal") values[part.type] = part.value;
  });
  return {
    year: Number(values.year ?? "0"),
    month: Number(values.month ?? "1"),
    day: Number(values.day ?? "1"),
  };
}

function normalizeTimeString(value: string, precision: Precision): string {
  const digits = value.replace(/[^\d:]/g, "");
  if (!digits) return precision === "second" ? "00:00:00" : "00:00";
  if (precision === "second") {
    if (digits.length >= 8) return digits.slice(0, 8);
    if (digits.length >= 5) return `${digits.slice(0, 5)}:00`;
    return "00:00:00";
  }
  if (digits.length >= 5) return digits.slice(0, 5);
  return "00:00";
}

function cleanDigits(value: string, part: DateTimePart): string {
  return value.replace(/\D/g, "").slice(0, limitForPart(part));
}

function limitForPart(part: DateTimePart): number {
  return part === "year" ? 4 : 2;
}

function getNextPart(part: DateTimePart, precision: Precision): DateTimePart | null {
  if (part === "year") return "month";
  if (part === "month") return "day";
  if (part === "day") return "hour";
  if (part === "hour") return "minute";
  if (part === "minute") return precision === "second" ? "second" : null;
  return null;
}

function normalizePartValue(part: DateTimePart, value: string): string {
  const digits = value.replace(/\D/g, "");
  if (!digits) return "";
  if (part === "year") return digits.slice(0, 4);

  let min = 0;
  let max = 59;
  if (part === "month") {
    min = 1;
    max = 12;
  }
  if (part === "day") {
    min = 1;
    max = 31;
  }
  if (part === "hour") {
    max = 23;
  }
  return pad(clamp(Number(digits), min, max));
}

function buildNumberList(start: number, end: number): string[] {
  const values: string[] = [];
  for (let current = start; current <= end; current += 1) {
    values.push(pad(current));
  }
  return values;
}

function normalizeWeekday(value: number): number {
  return value === 0 ? 7 : value;
}

function toDateString(year: number, month: number, day: number): string {
  return `${year}-${pad(month)}-${pad(day)}`;
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

function pad(value: number | string): string {
  const num = Number(value);
  if (Number.isFinite(num)) return num < 10 ? `0${num}` : String(num);
  return String(value);
}

function CalendarIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
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
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle
        cx="12"
        cy="12"
        r="8.5"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
      />
      <path
        d="M12 7.5v5l3 1.8"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.7"
      />
    </svg>
  );
}

export function buildAdminDateTimeRangeSubmitParams(start: string, end: string) {
  return buildDateTimeRangeSubmitValue(start, end);
}
