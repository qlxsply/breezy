<template>
  <div
    ref="rootRef"
    class="bz-date-picker"
    :class="{ 'is-disabled': disabled }"
  >
    <div
      class="bz-date-picker__input-wrap"
      @click="togglePanel"
    >
      <input
        class="bz-date-picker__input"
        :value="displayText"
        :placeholder="placeholderText"
        :disabled="disabled"
        readonly
      />
      <button
        v-if="clearable && selectedDate && !disabled"
        class="bz-date-picker__clear"
        type="button"
        @click.stop="clearValue"
      >
        <BzIconClose :size="20" />
      </button>
      <span class="bz-date-picker__icon">📅</span>
    </div>

    <Teleport to="body">
      <div
        v-if="panelOpen"
        ref="panelRef"
        class="bz-date-picker__panel"
        :style="panelStyle"
      >
        <div class="bz-date-picker__header">
          <button
            class="bz-date-picker__nav"
            type="button"
            @click="prevYear"
          >
            «
          </button>
          <button
            class="bz-date-picker__nav"
            type="button"
            @click="prevMonth"
          >
            ‹
          </button>
          <div class="bz-date-picker__title">{{ panelYear }} 年 {{ panelMonth + 1 }} 月</div>
          <button
            class="bz-date-picker__nav"
            type="button"
            @click="nextMonth"
          >
            ›
          </button>
          <button
            class="bz-date-picker__nav"
            type="button"
            @click="nextYear"
          >
            »
          </button>
        </div>

        <div class="bz-date-picker__week-row">
          <span
            v-for="week in weekNames"
            :key="week"
            class="bz-date-picker__week-cell"
            >{{ week }}</span
          >
        </div>

        <div class="bz-date-picker__days-grid">
          <button
            v-for="day in dayCells"
            :key="day.key"
            class="bz-date-picker__day"
            :class="{
              'is-other-month': !day.currentMonth,
              'is-today': day.isToday,
              'is-selected': day.isSelected,
            }"
            type="button"
            @click="pickDay(day.date)"
          >
            {{ day.date.getDate() }}
          </button>
        </div>

        <div
          v-if="type === 'datetime'"
          class="bz-date-picker__time-row"
        >
          <label>
            时
            <select v-model="selectedHour">
              <option
                v-for="hour in 24"
                :key="hour"
                :value="hour - 1"
              >
                {{ pad2(hour - 1) }}
              </option>
            </select>
          </label>
          <label>
            分
            <select v-model="selectedMinute">
              <option
                v-for="minute in 60"
                :key="minute"
                :value="minute - 1"
              >
                {{ pad2(minute - 1) }}
              </option>
            </select>
          </label>
        </div>

        <div class="bz-date-picker__footer">
          <button
            class="bz-date-picker__text-btn"
            type="button"
            @click="selectToday"
          >
            今天
          </button>
          <button
            class="bz-date-picker__text-btn"
            type="button"
            @click="clearValue"
          >
            清空
          </button>
          <button
            class="bz-date-picker__primary-btn"
            type="button"
            @click="applySelection"
          >
            确定
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import type { CSSProperties } from "vue";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

import BzIconClose from "./BzIconClose.vue";

defineOptions({
  name: "BzDatePicker",
});

type PickerType = "date" | "datetime";

const props = withDefaults(
  defineProps<{
    modelValue?: string | null;
    type?: PickerType;
    placeholder?: string;
    clearable?: boolean;
    disabled?: boolean;
    valueFormat?: string;
  }>(),
  {
    modelValue: "",
    type: "date",
    placeholder: "",
    clearable: false,
    disabled: false,
    valueFormat: "",
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: string | undefined): void;
  (e: "change", value: string | undefined): void;
}>();

const weekNames = ["日", "一", "二", "三", "四", "五", "六"];
const rootRef = ref<HTMLElement | null>(null);
const panelRef = ref<HTMLElement | null>(null);
const panelOpen = ref(false);
const selectedDate = ref<Date | null>(null);
const panelYear = ref(new Date().getFullYear());
const panelMonth = ref(new Date().getMonth());
const selectedHour = ref(0);
const selectedMinute = ref(0);
const panelLeft = ref(0);
const panelTop = ref(0);

const PANEL_WIDTH = 300;
const PANEL_MARGIN = 8;
const PANEL_GAP = 6;
const PANEL_FALLBACK_HEIGHT = 336;

const placeholderText = computed(
  () => props.placeholder || (props.type === "datetime" ? "请选择日期时间" : "请选择日期"),
);

const activeFormat = computed(() => {
  if (props.valueFormat) {
    return props.valueFormat;
  }
  return props.type === "datetime" ? "YYYY-MM-DDTHH:mm" : "YYYY-MM-DD";
});

const displayText = computed(() => {
  if (!selectedDate.value) {
    return "";
  }
  return formatByPattern(
    selectedDate.value,
    props.type === "datetime" ? "YYYY-MM-DD HH:mm" : "YYYY-MM-DD",
  );
});

const panelStyle = computed<CSSProperties>(() => ({
  position: "fixed",
  left: `${panelLeft.value}px`,
  top: `${panelTop.value}px`,
  width: `${PANEL_WIDTH}px`,
  zIndex: 1800,
}));

const dayCells = computed(() => {
  const firstDay = new Date(panelYear.value, panelMonth.value, 1);
  const start = new Date(firstDay);
  start.setDate(1 - firstDay.getDay());

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    return {
      key: formatByPattern(date, "YYYY-MM-DD"),
      date,
      currentMonth: date.getMonth() === panelMonth.value,
      isToday: isSameDate(date, new Date()),
      isSelected: selectedDate.value ? isSameDate(date, selectedDate.value) : false,
    };
  });
});

watch(
  () => props.modelValue,
  (value) => {
    const parsed = parseInputValue(value);
    selectedDate.value = parsed;
    if (parsed) {
      panelYear.value = parsed.getFullYear();
      panelMonth.value = parsed.getMonth();
      selectedHour.value = parsed.getHours();
      selectedMinute.value = parsed.getMinutes();
    }
  },
  { immediate: true },
);

function togglePanel() {
  if (props.disabled) {
    return;
  }
  panelOpen.value = !panelOpen.value;
  if (panelOpen.value) {
    void nextTick(() => {
      updatePanelPosition();
    });
  }
}

function prevYear() {
  panelYear.value -= 1;
}

function nextYear() {
  panelYear.value += 1;
}

function prevMonth() {
  if (panelMonth.value === 0) {
    panelMonth.value = 11;
    panelYear.value -= 1;
    return;
  }
  panelMonth.value -= 1;
}

function nextMonth() {
  if (panelMonth.value === 11) {
    panelMonth.value = 0;
    panelYear.value += 1;
    return;
  }
  panelMonth.value += 1;
}

function pickDay(date: Date) {
  const next = new Date(date);
  if (selectedDate.value) {
    next.setHours(selectedDate.value.getHours(), selectedDate.value.getMinutes(), 0, 0);
  } else {
    next.setHours(selectedHour.value, selectedMinute.value, 0, 0);
  }
  selectedDate.value = next;
  panelYear.value = next.getFullYear();
  panelMonth.value = next.getMonth();

  if (props.type === "date") {
    applySelection();
  }
}

function selectToday() {
  const now = new Date();
  selectedDate.value = new Date(now);
  selectedHour.value = now.getHours();
  selectedMinute.value = now.getMinutes();
  panelYear.value = now.getFullYear();
  panelMonth.value = now.getMonth();
  applySelection();
}

function clearValue() {
  selectedDate.value = null;
  emit("update:modelValue", undefined);
  emit("change", undefined);
  panelOpen.value = false;
}

function applySelection() {
  if (!selectedDate.value) {
    clearValue();
    return;
  }

  const next = new Date(selectedDate.value);
  if (props.type === "datetime") {
    next.setHours(selectedHour.value, selectedMinute.value, 0, 0);
  } else {
    next.setHours(0, 0, 0, 0);
  }
  selectedDate.value = next;
  const output = formatByPattern(next, activeFormat.value);
  emit("update:modelValue", output);
  emit("change", output);
  panelOpen.value = false;
}

function parseInputValue(value?: string | null): Date | null {
  if (!value) {
    return null;
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return null;
  }
  return parsed;
}

function formatByPattern(date: Date, pattern: string): string {
  return pattern
    .replace("YYYY", String(date.getFullYear()))
    .replace("MM", pad2(date.getMonth() + 1))
    .replace("DD", pad2(date.getDate()))
    .replace("HH", pad2(date.getHours()))
    .replace("mm", pad2(date.getMinutes()));
}

function pad2(value: number): string {
  return String(value).padStart(2, "0");
}

function isSameDate(left: Date, right: Date): boolean {
  return (
    left.getFullYear() === right.getFullYear() &&
    left.getMonth() === right.getMonth() &&
    left.getDate() === right.getDate()
  );
}

function onClickOutside(event: MouseEvent) {
  if (!rootRef.value || !panelOpen.value) {
    return;
  }
  const target = event.target as Node;
  const clickedInsideTrigger = rootRef.value.contains(target);
  const clickedInsidePanel = panelRef.value?.contains(target) ?? false;
  if (!clickedInsideTrigger && !clickedInsidePanel) {
    panelOpen.value = false;
  }
}

function updatePanelPosition() {
  if (!panelOpen.value || !rootRef.value) {
    return;
  }

  const triggerRect = rootRef.value.getBoundingClientRect();
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  const panelHeight = panelRef.value?.offsetHeight ?? PANEL_FALLBACK_HEIGHT;

  let nextLeft = triggerRect.left;
  if (nextLeft + PANEL_WIDTH + PANEL_MARGIN > viewportWidth) {
    nextLeft = viewportWidth - PANEL_WIDTH - PANEL_MARGIN;
  }
  nextLeft = Math.max(PANEL_MARGIN, nextLeft);

  let nextTop = triggerRect.bottom + PANEL_GAP;
  if (nextTop + panelHeight + PANEL_MARGIN > viewportHeight) {
    const topByAbove = triggerRect.top - panelHeight - PANEL_GAP;
    nextTop =
      topByAbove >= PANEL_MARGIN
        ? topByAbove
        : Math.max(PANEL_MARGIN, viewportHeight - panelHeight - PANEL_MARGIN);
  }

  panelLeft.value = Math.round(nextLeft);
  panelTop.value = Math.round(nextTop);
}

function onViewportChange() {
  if (!panelOpen.value) {
    return;
  }
  updatePanelPosition();
}

watch(panelOpen, (open) => {
  if (!open) {
    return;
  }
  void nextTick(() => {
    updatePanelPosition();
  });
});

onMounted(() => {
  document.addEventListener("mousedown", onClickOutside);
  window.addEventListener("resize", onViewportChange);
  window.addEventListener("scroll", onViewportChange, true);
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", onClickOutside);
  window.removeEventListener("resize", onViewportChange);
  window.removeEventListener("scroll", onViewportChange, true);
});
</script>

<style scoped>
.bz-date-picker {
  position: relative;
  width: 100%;
}

.bz-date-picker__input-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 100%;
  min-height: 32px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.bz-date-picker__input-wrap:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

.bz-date-picker__input {
  width: 100%;
  height: 30px;
  border: none;
  outline: none;
  background: transparent;
  padding: 0 58px 0 10px;
  font-size: 13px;
}

.bz-date-picker__clear {
  position: absolute;
  right: 24px;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background-color 0.16s ease;
}

.bz-date-picker__clear:hover {
  color: #64748b;
  background: #f1f5f9;
}

.bz-date-picker__icon {
  position: absolute;
  right: 8px;
  font-size: 13px;
}

.bz-date-picker__panel {
  box-sizing: border-box;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.14);
  padding: 12px;
}

.bz-date-picker__header {
  display: grid;
  grid-template-columns: 28px 28px 1fr 28px 28px;
  align-items: center;
  gap: 6px;
}

.bz-date-picker__title {
  text-align: center;
  font-size: 13px;
  font-weight: 600;
}

.bz-date-picker__nav {
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  height: 28px;
}

.bz-date-picker__week-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-top: 10px;
}

.bz-date-picker__week-cell {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 24px;
}

.bz-date-picker__days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.bz-date-picker__day {
  border: none;
  border-radius: 6px;
  background: #fff;
  height: 30px;
  cursor: pointer;
  font-size: 12px;
}

.bz-date-picker__day:hover {
  background: #eff6ff;
}

.bz-date-picker__day.is-other-month {
  color: #94a3b8;
}

.bz-date-picker__day.is-today {
  border: 1px solid #93c5fd;
}

.bz-date-picker__day.is-selected {
  background: #3b82f6;
  color: #fff;
}

.bz-date-picker__time-row {
  display: flex;
  gap: 12px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

.bz-date-picker__time-row label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.bz-date-picker__time-row select {
  height: 28px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
}

.bz-date-picker__footer {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.bz-date-picker__text-btn,
.bz-date-picker__primary-btn {
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
  min-height: 28px;
  padding: 0 10px;
  cursor: pointer;
}

.bz-date-picker__primary-btn {
  border-color: #3b82f6;
  background: #3b82f6;
  color: #fff;
}
</style>
