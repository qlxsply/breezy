<template>
  <div class="app-shell">
    <div class="content">
      <div class="workspace">
        <section class="panel history card">
          <div class="panel-header">
            <div
              v-if="canUse"
              class="panel-actions"
            >
              <bz-input
                v-model="historyKeyword"
                size="small"
                placeholder="名称搜索"
                style="width: 160px"
                @keyup.enter="loadRecords"
              />
              <button
                class="header-icon-btn"
                title="搜索"
                @click="loadRecords"
              >
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  aria-hidden="true"
                >
                  <circle
                    cx="11"
                    cy="11"
                    r="6.5"
                    stroke="currentColor"
                    stroke-width="1.8"
                  />
                  <path
                    d="M16 16l5 5"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
              <button
                class="header-icon-btn"
                :class="{ active: compareMode }"
                :title="compareMode ? '退出对比模式' : '进入对比模式'"
                @click="toggleCompareMode"
              >
                <svg
                  v-if="compareMode"
                  viewBox="0 0 24 24"
                  fill="none"
                  aria-hidden="true"
                >
                  <path
                    d="M6 6l12 12M18 6L6 18"
                    stroke="currentColor"
                    stroke-width="1.9"
                    stroke-linecap="round"
                  />
                </svg>
                <svg
                  v-else
                  viewBox="0 0 24 24"
                  fill="none"
                  aria-hidden="true"
                >
                  <rect
                    x="3"
                    y="4"
                    width="18"
                    height="16"
                    rx="2"
                    stroke="currentColor"
                    stroke-width="1.6"
                  />
                  <path
                    d="M12 4V20"
                    stroke="currentColor"
                    stroke-width="1.6"
                  />
                  <path
                    d="M6 8H9"
                    stroke="currentColor"
                    stroke-width="1.4"
                    stroke-linecap="round"
                  />
                  <path
                    d="M6 11H10"
                    stroke="currentColor"
                    stroke-width="1.4"
                    stroke-linecap="round"
                  />
                  <path
                    d="M6 14H8.5"
                    stroke="currentColor"
                    stroke-width="1.4"
                    stroke-linecap="round"
                  />
                  <path
                    d="M14.5 8H18"
                    stroke="currentColor"
                    stroke-width="1.4"
                    stroke-linecap="round"
                  />
                  <path
                    d="M14.5 11H17"
                    stroke="currentColor"
                    stroke-width="1.4"
                    stroke-linecap="round"
                  />
                  <path
                    d="M14.5 14H18.5"
                    stroke="currentColor"
                    stroke-width="1.4"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
              <button
                class="header-icon-btn"
                :class="{ active: deleteMode, danger: deleteMode }"
                :title="deleteMode ? '退出删除模式' : '进入删除模式'"
                @click="toggleDeleteMode"
              >
                <svg
                  v-if="deleteMode"
                  viewBox="0 0 24 24"
                  fill="none"
                  aria-hidden="true"
                >
                  <path
                    d="M6 6l12 12M18 6L6 18"
                    stroke="currentColor"
                    stroke-width="1.9"
                    stroke-linecap="round"
                  />
                </svg>
                <svg
                  v-else
                  viewBox="0 0 24 24"
                  fill="none"
                  aria-hidden="true"
                >
                  <path
                    d="M4 7H20"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                  />
                  <path
                    d="M9 5.5C9 4.67 9.67 4 10.5 4H13.5C14.33 4 15 4.67 15 5.5V7H9V5.5Z"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linejoin="round"
                  />
                  <rect
                    x="6"
                    y="7"
                    width="12"
                    height="13"
                    rx="2"
                    stroke="currentColor"
                    stroke-width="1.8"
                  />
                  <path
                    d="M10 11V17"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                  />
                  <path
                    d="M14 11V17"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
            </div>
          </div>
          <div
            v-if="!canUse"
            class="panel-body empty"
          >
            无权限使用 JSON 工具
          </div>
          <div
            v-else-if="recordsLoading"
            class="panel-body empty"
          >
            加载中...
          </div>
          <div
            v-else-if="displayRecords.length === 0"
            class="panel-body empty"
          >
            暂无记录，保存后将展示在这里。
          </div>
          <div
            v-else
            class="panel-body list"
            @dragover.prevent
            @drop="handleDropToEnd"
          >
            <ul class="history-list">
              <li
                v-for="record in displayRecords"
                :key="record.id"
                class="history-item"
                :class="{
                  active: record.id === activeRecordId,
                  selected: isModeSelected(record.id),
                  'selection-mode': selectionMode,
                  dragging: record.id === draggingId,
                  dragover: record.id === dragOverId,
                }"
                :draggable="canUse && !selectionMode && editingId !== record.id && !record.isDraft"
                @dragstart="handleDragStart(record.id)"
                @dragover.prevent="handleDragOver(record.id)"
                @drop="handleDrop(record.id)"
                @dragend="handleDragEnd"
                @click="handleHistoryItemClick(record.id)"
              >
                <div
                  class="history-name"
                  @dblclick.stop="startRename(record)"
                >
                  <template v-if="editingId === record.id">
                    <input
                      :ref="(el) => setRenameInputRef(record.id, el as HTMLInputElement | null)"
                      v-model="editingName"
                      class="rename-input"
                      @keydown.enter.prevent="confirmRename"
                      @keydown.esc.prevent="cancelRename"
                      @blur="confirmRename"
                      @click.stop
                    />
                  </template>
                  <template v-else>
                    <span class="name-text">{{ record.name }}</span>
                    <span
                      v-if="record.isDraft ? isDirtyDraft(record.id) : isDirtySaved(record.id)"
                      class="dirty-dot"
                      :class="record.isDraft ? 'draft' : 'saved'"
                    ></span>
                  </template>
                </div>
                <div class="history-meta">{{ formatDateTime(record.updatedAt) }}</div>
                <div
                  v-if="selectionMode"
                  class="compare-pick"
                  @click.stop
                >
                  <input
                    type="checkbox"
                    class="compare-checkbox"
                    :checked="isModeSelected(record.id)"
                    :disabled="!isModeSelectable(record.id)"
                    @change="handleModeCheckboxEvent(record.id, $event)"
                  />
                </div>
              </li>
            </ul>
          </div>
          <div
            v-if="canUse && selectionMode"
            class="compare-action-bar"
          >
            <span class="compare-count">{{ selectionModeCountLabel }}</span>
            <button
              class="compare-action-btn primary"
              :class="{ danger: deleteMode }"
              :disabled="selectionActionDisabled"
              :title="selectionActionTitle"
              @click="handleSelectionAction"
            >
              <svg
                v-if="compareMode"
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <rect
                  x="3"
                  y="4"
                  width="18"
                  height="16"
                  rx="2"
                  stroke="currentColor"
                  stroke-width="1.6"
                />
                <path
                  d="M12 4V20"
                  stroke="currentColor"
                  stroke-width="1.6"
                />
                <path
                  d="M6 8H9"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                />
                <path
                  d="M6 11H10"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                />
                <path
                  d="M6 14H8.5"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                />
                <path
                  d="M14.5 8H18"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                />
                <path
                  d="M14.5 11H17"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                />
                <path
                  d="M14.5 14H18.5"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                />
              </svg>
              <svg
                v-else
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <path
                  d="M4 7H20"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                />
                <path
                  d="M9 5.5C9 4.67 9.67 4 10.5 4H13.5C14.33 4 15 4.67 15 5.5V7H9V5.5Z"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linejoin="round"
                />
                <rect
                  x="6"
                  y="7"
                  width="12"
                  height="13"
                  rx="2"
                  stroke="currentColor"
                  stroke-width="1.8"
                />
                <path
                  d="M10 11V17"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                />
                <path
                  d="M14 11V17"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                />
              </svg>
            </button>
            <button
              class="compare-action-btn"
              :disabled="selectionClearDisabled"
              title="清空选择"
              @click="clearModeSelection"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <path
                  d="M6 6l12 12M18 6L6 18"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                />
              </svg>
            </button>
          </div>
        </section>

        <section class="panel editor card">
          <div
            v-if="!compareMode"
            class="panel-header"
          >
            <div
              v-if="canUse"
              class="panel-actions editor-actions"
            >
              <bz-button
                size="small"
                @click="createNewRecord"
                >新建</bz-button
              >
              <bz-button
                size="small"
                type="primary"
                @click="handleFormat"
                >格式化</bz-button
              >
              <bz-button
                size="small"
                type="primary"
                :loading="saving"
                :disabled="saving"
                @click="handleSave"
              >
                保存
              </bz-button>
              <bz-button
                size="small"
                @click="handleCompressCopy"
                >压缩复制</bz-button
              >
              <bz-button
                size="small"
                :disabled="!canDownload"
                @click="handleDownload"
                >下载</bz-button
              >
              <span
                class="action-divider"
                aria-hidden="true"
              ></span>
              <bz-button
                size="small"
                :disabled="!canFold"
                @click="expandAll"
                >展开</bz-button
              >
              <bz-button
                size="small"
                :disabled="!canFold"
                @click="collapseAll"
                >折叠</bz-button
              >
              <bz-button
                size="small"
                :disabled="!canFold"
                @click="expandStep"
                >逐层展开</bz-button
              >
              <span
                class="action-divider"
                aria-hidden="true"
              ></span>
              <bz-input
                ref="searchInputRef"
                v-model="searchQuery"
                size="small"
                placeholder="搜索 JSON"
                style="width: 200px"
              />
              <bz-button
                size="small"
                :type="searchCaseSensitive ? 'primary' : 'default'"
                @click="toggleCaseSensitive"
                >区分大小写</bz-button
              >
              <bz-button
                size="small"
                :type="searchExactMatch ? 'primary' : 'default'"
                @click="toggleExactMatch"
                >完整匹配</bz-button
              >
              <span
                v-if="searchQuery.trim()"
                class="search-meta"
              >
                {{ matchCount > 0 ? `共 ${matchCount} 处匹配` : "无匹配" }}
                <template v-if="matchCount > 0"
                  >· 当前 {{ currentMatchIndex }} / {{ matchCount }}</template
                >
              </span>
              <bz-button
                v-if="hasMultipleMatches"
                size="small"
                @click="jumpPrevMatch"
                >上一处</bz-button
              >
              <bz-button
                v-if="hasMultipleMatches"
                size="small"
                @click="jumpNextMatch"
                >下一处</bz-button
              >
            </div>
          </div>
          <div
            v-if="!canUse"
            class="panel-body empty"
          >
            无权限使用 JSON 工具
          </div>
          <div
            v-else
            class="panel-body panel-body-host compare-only-host"
          >
            <div
              v-show="!compareMode"
              class="editor-body"
            >
              <div
                ref="editorRef"
                class="editor-container"
              ></div>
            </div>
            <div
              v-show="compareMode"
              class="compare-body"
            >
              <json-diff-viewer
                :left-label="compareResult?.left.name || ''"
                :right-label="compareResult?.right.name || ''"
                :rows="compareResult ? compareResult.lineRows : []"
              />
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defaultKeymap, history, historyKeymap, indentWithTab } from "@codemirror/commands";
import { json } from "@codemirror/lang-json";
import {
  bracketMatching,
  foldable,
  foldAll,
  foldEffect,
  foldGutter,
  foldKeymap,
  foldState,
  indentOnInput,
  syntaxTree,
  unfoldAll,
  unfoldEffect,
} from "@codemirror/language";
import type { Diagnostic } from "@codemirror/lint";
import { linter } from "@codemirror/lint";
import { search } from "@codemirror/search";
import type { Range } from "@codemirror/state";
import { EditorState, StateEffect, StateField } from "@codemirror/state";
import type { DecorationSet } from "@codemirror/view";
import {
  crosshairCursor,
  Decoration,
  drawSelection,
  dropCursor,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  highlightSpecialChars,
  keymap,
  lineNumbers,
  placeholder,
  rectangularSelection,
  WidgetType,
} from "@codemirror/view";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

import {
  batchDeleteJsonFmtRecords,
  deleteJsonFmtRecord,
  getJsonFmtRecord,
  listJsonFmtRecords,
  renameJsonFmtRecord,
  reorderJsonFmtRecords,
  saveJsonFmtRecord,
} from "../api/jsonfmt";
import JsonDiffViewer from "../components/json-formatter/JsonDiffViewer.vue";
import { hasApiPermission } from "../registry/permissions.registry";
import { clearPageShortcuts, setPageShortcuts } from "../registry/shortcuts.registry";
import type { JsonFmtRecordDetail, JsonFmtRecordListItem } from "../types/jsonfmt";
import { formatDateTime } from "../utils/formatter";
import { message } from "../utils/message";

interface DraftRecord {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

interface RecordUiState {
  content: string;
  lastSavedContent: string;
  searchQuery: string;
  searchCaseSensitive: boolean;
  searchExactMatch: boolean;
  currentMatchIndex: number;
  expandLevel: number;
  foldedRanges: Array<{ from: number; to: number }>;
}

type JsonDiffType =
  | "ONLY_LEFT"
  | "ONLY_RIGHT"
  | "VALUE_CHANGED"
  | "TYPE_CHANGED"
  | "ARRAY_ORDER_CHANGED";

type JsonDiffLineKind = "same" | "removed" | "added" | "changed" | "placeholder";

interface JsonCompareSnapshot {
  id: string;
  name: string;
  pretty: string;
  value: unknown;
}

interface JsonDiffEntry {
  path: string;
  type: JsonDiffType;
}

interface JsonDiffLineRow {
  leftLineNo: number | null;
  rightLineNo: number | null;
  leftText: string;
  rightText: string;
  leftKind: JsonDiffLineKind;
  rightKind: JsonDiffLineKind;
}

interface JsonCompareResult {
  left: JsonCompareSnapshot;
  right: JsonCompareSnapshot;
  entries: JsonDiffEntry[];
  lineRows: JsonDiffLineRow[];
}

const rawInput = ref("");
const editorRef = ref<HTMLDivElement | null>(null);
const searchQuery = ref("");
const searchCaseSensitive = ref(false);
const searchExactMatch = ref(false);
const matchCount = ref(0);
const currentMatchIndex = ref(0);
const matchRanges = ref<Array<{ from: number; to: number }>>([]);
const searchInputRef = ref<HTMLInputElement | null>(null);
const records = ref<JsonFmtRecordListItem[]>([]);
const draftRecords = ref<DraftRecord[]>([]);
const recordsLoading = ref(false);
const historyKeyword = ref("");
const activeRecordId = ref<string | null>(null);
const activeRecord = ref<JsonFmtRecordDetail | null>(null);
const activeRecordName = ref("");
const draftCounter = ref(1);
const dirtySavedIds = ref<Set<string>>(new Set());
const dirtyDraftIds = ref<Set<string>>(new Set());
const saving = ref(false);
const editingId = ref<string | null>(null);
const editingName = ref("");
const renameInputRefs = new Map<string, HTMLInputElement>();
const draggingId = ref<string | null>(null);
const dragOverId = ref<string | null>(null);
const expandLevel = ref(0);
const maxExpandDepth = ref(0);
const actionMode = ref<"none" | "compare" | "delete">("none");
const compareSelectedIds = ref<string[]>([]);
const deleteSelectedIds = ref<string[]>([]);
const compareResult = ref<JsonCompareResult | null>(null);
let editorView: EditorView | null = null;
let ignoreEditorChange = false;
let searchTimer: number | null = null;
let errorFlashTimer: number | null = null;
let errorLineTimer: number | null = null;
let suppressSearchWatch = false;
const recordStates = new Map<string, RecordUiState>();

const canUse = computed(() => hasApiPermission("jfm.use"));
const canDownload = computed(() => rawInput.value.trim().length > 0);
const canFold = computed(() => canUse.value);
const hasMultipleMatches = computed(() => matchCount.value > 1);
const compareMode = computed(() => actionMode.value === "compare");
const deleteMode = computed(() => actionMode.value === "delete");
const selectionMode = computed(() => actionMode.value !== "none");
const selectionModeCountLabel = computed(() => {
  if (compareMode.value) {
    return `已选 ${compareSelectedIds.value.length} / 2`;
  }
  return `已选 ${deleteSelectedIds.value.length}`;
});
const selectionActionDisabled = computed(() => {
  if (compareMode.value) {
    return compareSelectedIds.value.length !== 2;
  }
  if (deleteMode.value) {
    return deleteSelectedIds.value.length === 0;
  }
  return true;
});
const selectionClearDisabled = computed(() => {
  if (compareMode.value) {
    return compareSelectedIds.value.length === 0;
  }
  if (deleteMode.value) {
    return deleteSelectedIds.value.length === 0;
  }
  return true;
});
const selectionActionTitle = computed(() => (compareMode.value ? "开始对比" : "删除已选"));

const displayRecords = computed(() => {
  const drafts = [...draftRecords.value]
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .map((item) => ({
      id: item.id,
      name: item.name,
      updatedAt: item.updatedAt,
      isDraft: true,
    }));
  const saved = records.value.map((item) => ({
    id: item.id,
    name: item.name,
    updatedAt: item.updatedAt,
    isDraft: false,
  }));
  return [...drafts, ...saved];
});

const currentRecordName = computed(() => {
  const recordId = activeRecordId.value;
  if (!recordId) {
    return "";
  }
  const draft = draftRecords.value.find((item) => item.id === recordId);
  if (draft) {
    return draft.name;
  }
  const saved = records.value.find((item) => item.id === recordId);
  if (saved) {
    return saved.name;
  }
  return activeRecordName.value || activeRecord.value?.name || "";
});

const editorTheme = EditorView.theme({
  "&": {
    height: "100%",
    backgroundColor: "#fbfcff",
    color: "var(--text-main)",
  },
  ".cm-scroller": {
    fontFamily: "SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace",
    lineHeight: "1.6",
  },
  ".cm-content": {
    padding: "12px 16px",
  },
  ".cm-gutters": {
    backgroundColor: "#f8fafc",
    borderRight: "1px solid var(--border-color)",
    color: "var(--text-muted)",
  },
  ".cm-line": {
    padding: "0 4px",
  },
});

function isDraftId(recordId: string | null): boolean {
  return Boolean(recordId && recordId.startsWith("draft-"));
}

function isDirtyDraft(recordId: string): boolean {
  return dirtyDraftIds.value.has(recordId);
}

function isDirtySaved(recordId: string): boolean {
  return dirtySavedIds.value.has(recordId);
}

function updateDirtySet(setRef: typeof dirtySavedIds, recordId: string, dirty: boolean) {
  const next = new Set(setRef.value);
  if (dirty) {
    next.add(recordId);
  } else {
    next.delete(recordId);
  }
  setRef.value = next;
}

function markRecordDirty(recordId: string, dirty: boolean) {
  if (isDraftId(recordId)) {
    updateDirtySet(dirtyDraftIds, recordId, true);
    return;
  } else {
    updateDirtySet(dirtySavedIds, recordId, dirty);
  }
}

function toggleCompareMode() {
  if (compareMode.value) {
    exitActionMode();
    return;
  }
  actionMode.value = "compare";
  clearCompareSelection();
  deleteSelectedIds.value = [];
}

function toggleDeleteMode() {
  if (deleteMode.value) {
    exitActionMode();
    return;
  }
  actionMode.value = "delete";
  clearDeleteSelection();
  compareSelectedIds.value = [];
  compareResult.value = null;
}

function exitActionMode() {
  actionMode.value = "none";
  clearModeSelection();
  compareResult.value = null;
}

function clearCompareSelection() {
  compareSelectedIds.value = [];
}

function clearDeleteSelection() {
  deleteSelectedIds.value = [];
}

function clearModeSelection() {
  clearCompareSelection();
  clearDeleteSelection();
}

function isCompareSelected(recordId: string): boolean {
  return compareSelectedIds.value.includes(recordId);
}

function isDeleteSelected(recordId: string): boolean {
  return deleteSelectedIds.value.includes(recordId);
}

function isModeSelected(recordId: string): boolean {
  if (compareMode.value) {
    return isCompareSelected(recordId);
  }
  if (deleteMode.value) {
    return isDeleteSelected(recordId);
  }
  return false;
}

function isRecordReadyForCompare(recordId: string): boolean {
  if (recordId === activeRecordId.value) {
    return true;
  }
  if (isDraftId(recordId)) {
    return true;
  }
  return recordStates.has(recordId);
}

function isCompareSelectable(recordId: string): boolean {
  if (isCompareSelected(recordId)) {
    return true;
  }
  return compareSelectedIds.value.length < 2;
}

function isDeleteSelectable(_recordId: string): boolean {
  return deleteMode.value;
}

function isModeSelectable(recordId: string): boolean {
  if (compareMode.value) {
    return isCompareSelectable(recordId);
  }
  if (deleteMode.value) {
    return isDeleteSelectable(recordId);
  }
  return false;
}

function handleHistoryItemClick(recordId: string) {
  if (compareMode.value) {
    if (isCompareSelected(recordId)) {
      compareSelectedIds.value = compareSelectedIds.value.filter((id) => id !== recordId);
      return;
    }
    void handleCompareCheckboxChange(recordId, true);
    return;
  }
  if (deleteMode.value) {
    if (isDeleteSelected(recordId)) {
      deleteSelectedIds.value = deleteSelectedIds.value.filter((id) => id !== recordId);
      return;
    }
    deleteSelectedIds.value = [...deleteSelectedIds.value, recordId];
    return;
  }
  void selectRecord(recordId);
}

async function handleCompareCheckboxChange(recordId: string, checked: boolean) {
  if (!checked) {
    compareSelectedIds.value = compareSelectedIds.value.filter((id) => id !== recordId);
    return;
  }

  if (compareSelectedIds.value.length >= 2) {
    message.warning("最多只能选择两条记录");
    return;
  }

  if (!isRecordReadyForCompare(recordId)) {
    const loaded = await ensureRecordReadyForCompare(recordId);
    if (!loaded) {
      return;
    }
  }

  const snapshot = tryBuildCompareSnapshot(recordId, true);
  if (!snapshot) {
    return;
  }

  compareSelectedIds.value = [...compareSelectedIds.value, recordId];
}

function handleModeCheckboxEvent(recordId: string, event: Event) {
  const target = event.target;
  if (!(target instanceof HTMLInputElement)) {
    return;
  }
  if (compareMode.value) {
    void handleCompareCheckboxChange(recordId, target.checked);
    return;
  }
  if (deleteMode.value) {
    if (target.checked) {
      if (!isDeleteSelected(recordId)) {
        deleteSelectedIds.value = [...deleteSelectedIds.value, recordId];
      }
    } else {
      deleteSelectedIds.value = deleteSelectedIds.value.filter((id) => id !== recordId);
    }
  }
}

async function ensureRecordReadyForCompare(recordId: string): Promise<boolean> {
  if (isRecordReadyForCompare(recordId)) {
    return true;
  }
  if (isDraftId(recordId)) {
    return true;
  }
  try {
    const detail = await getJsonFmtRecord(recordId);
    if (!recordStates.has(recordId)) {
      initRecordStateFromDetail(detail);
    }
    return true;
  } catch (_err) {
    message.error("记录加载失败，无法用于对比");
    return false;
  }
}

function startCompare() {
  if (compareSelectedIds.value.length !== 2) {
    message.warning("请先选择两条记录");
    return;
  }

  const leftId = compareSelectedIds.value[0];
  const rightId = compareSelectedIds.value[1];

  const left = tryBuildCompareSnapshot(leftId, true);
  if (!left) {
    return;
  }

  const right = tryBuildCompareSnapshot(rightId, true);
  if (!right) {
    return;
  }

  compareResult.value = buildCompareResult(left, right);
}

function handleSelectionAction() {
  if (compareMode.value) {
    startCompare();
    return;
  }
  if (deleteMode.value) {
    void deleteSelectedRecords();
  }
}

function tryBuildCompareSnapshot(
  recordId: string,
  notifyOnError: boolean,
): JsonCompareSnapshot | null {
  const content = getRecordContentForCompare(recordId);
  if (content === null) {
    if (notifyOnError) {
      message.warning("该记录尚未缓存，请先点击打开后再参与对比");
    }
    return null;
  }

  try {
    const parsed = parseJsonInput(content);
    return {
      id: recordId,
      name: getRecordNameById(recordId),
      pretty: parsed.pretty,
      value: parsed.value,
    };
  } catch (err) {
    if (recordId === activeRecordId.value) {
      focusJsonError(content, err, true);
    }
    if (notifyOnError) {
      message.error(`记录「${getRecordNameById(recordId)}」JSON 格式错误，无法对比`);
    }
    return null;
  }
}

function getRecordNameById(recordId: string): string {
  const draft = draftRecords.value.find((item) => item.id === recordId);
  if (draft) {
    return draft.name;
  }
  const saved = records.value.find((item) => item.id === recordId);
  if (saved) {
    return saved.name;
  }
  if (activeRecordId.value === recordId) {
    return currentRecordName.value || "未命名记录";
  }
  return "未命名记录";
}

function getRecordContentForCompare(recordId: string): string | null {
  if (recordId === activeRecordId.value) {
    return getEditorContent();
  }
  const state = recordStates.get(recordId);
  if (!state) {
    return null;
  }
  return state.content;
}

function buildCompareResult(
  left: JsonCompareSnapshot,
  right: JsonCompareSnapshot,
): JsonCompareResult {
  const entries: JsonDiffEntry[] = [];
  compareJsonNode(left.value, right.value, "$", entries);
  return {
    left,
    right,
    entries,
    lineRows: buildLineDiffRows(left.pretty, right.pretty),
  };
}

function compareJsonNode(left: unknown, right: unknown, path: string, entries: JsonDiffEntry[]) {
  if (left === undefined && right !== undefined) {
    entries.push(createDiffEntry(path, "ONLY_RIGHT"));
    return;
  }
  if (left !== undefined && right === undefined) {
    entries.push(createDiffEntry(path, "ONLY_LEFT"));
    return;
  }

  const leftType = resolveJsonType(left);
  const rightType = resolveJsonType(right);
  if (leftType !== rightType) {
    entries.push(createDiffEntry(path, "TYPE_CHANGED"));
    return;
  }

  if (leftType === "array" && Array.isArray(left) && Array.isArray(right)) {
    if (isArrayOrderChanged(left, right)) {
      entries.push(createDiffEntry(path, "ARRAY_ORDER_CHANGED"));
    }
    const maxLength = Math.max(left.length, right.length);
    for (let i = 0; i < maxLength; i += 1) {
      compareJsonNode(left[i], right[i], `${path}[${i}]`, entries);
    }
    return;
  }

  if (leftType === "object" && isPlainObject(left) && isPlainObject(right)) {
    const leftKeys = Object.keys(left);
    const rightKeys = Object.keys(right);
    const keySet = new Set<string>([...leftKeys, ...rightKeys]);
    const allKeys = [...keySet].sort();

    for (const key of allKeys) {
      const hasLeft = Object.prototype.hasOwnProperty.call(left, key);
      const hasRight = Object.prototype.hasOwnProperty.call(right, key);
      const childPath = buildChildPath(path, key);
      if (!hasLeft && hasRight) {
        compareJsonNode(undefined, right[key], childPath, entries);
        continue;
      }
      if (hasLeft && !hasRight) {
        compareJsonNode(left[key], undefined, childPath, entries);
        continue;
      }
      compareJsonNode(left[key], right[key], childPath, entries);
    }
    return;
  }

  if (!deepEqualJson(left, right)) {
    entries.push(createDiffEntry(path, "VALUE_CHANGED"));
  }
}

function createDiffEntry(path: string, type: JsonDiffType): JsonDiffEntry {
  return {
    path,
    type,
  };
}

function buildChildPath(parentPath: string, key: string): string {
  if (/^[A-Za-z_$][A-Za-z0-9_$]*$/.test(key)) {
    return `${parentPath}.${key}`;
  }
  return `${parentPath}[${JSON.stringify(key)}]`;
}

function resolveJsonType(
  value: unknown,
): "null" | "array" | "object" | "string" | "number" | "boolean" | "undefined" {
  if (value === undefined) {
    return "undefined";
  }
  if (value === null) {
    return "null";
  }
  if (Array.isArray(value)) {
    return "array";
  }
  if (typeof value === "object") {
    return "object";
  }
  if (typeof value === "string") {
    return "string";
  }
  if (typeof value === "number") {
    return "number";
  }
  return "boolean";
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  if (value === null || typeof value !== "object") {
    return false;
  }
  return !Array.isArray(value);
}

function deepEqualJson(left: unknown, right: unknown): boolean {
  if (left === right) {
    return true;
  }
  if (left === null || right === null) {
    return left === right;
  }
  if (Array.isArray(left) && Array.isArray(right)) {
    if (left.length !== right.length) {
      return false;
    }
    for (let i = 0; i < left.length; i += 1) {
      if (!deepEqualJson(left[i], right[i])) {
        return false;
      }
    }
    return true;
  }
  if (isPlainObject(left) && isPlainObject(right)) {
    const leftKeys = Object.keys(left);
    const rightKeys = Object.keys(right);
    if (leftKeys.length !== rightKeys.length) {
      return false;
    }
    for (const key of leftKeys) {
      if (!Object.prototype.hasOwnProperty.call(right, key)) {
        return false;
      }
      if (!deepEqualJson(left[key], right[key])) {
        return false;
      }
    }
    return true;
  }
  return false;
}

function isArrayOrderChanged(left: unknown[], right: unknown[]): boolean {
  if (left.length !== right.length) {
    return false;
  }
  if (deepEqualJson(left, right)) {
    return false;
  }
  const leftSignature = left.map((item) => stableStringify(item)).sort();
  const rightSignature = right.map((item) => stableStringify(item)).sort();
  if (leftSignature.length !== rightSignature.length) {
    return false;
  }
  for (let i = 0; i < leftSignature.length; i += 1) {
    if (leftSignature[i] !== rightSignature[i]) {
      return false;
    }
  }
  return true;
}

function stableStringify(value: unknown): string {
  if (value === null) {
    return "null";
  }
  if (Array.isArray(value)) {
    return `[${value.map((item) => stableStringify(item)).join(",")}]`;
  }
  if (isPlainObject(value)) {
    const keys = Object.keys(value).sort();
    const pairs = keys.map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`);
    return `{${pairs.join(",")}}`;
  }
  return JSON.stringify(value);
}

function buildLineDiffRows(leftPretty: string, rightPretty: string): JsonDiffLineRow[] {
  const leftLines = splitLines(leftPretty);
  const rightLines = splitLines(rightPretty);
  const ops = buildLineOps(leftLines, rightLines);
  const merged = mergeChangedBlocks(ops);

  const rows: JsonDiffLineRow[] = [];
  let leftNo = 1;
  let rightNo = 1;

  for (const op of merged) {
    if (op.type === "same") {
      rows.push({
        leftLineNo: leftNo,
        rightLineNo: rightNo,
        leftText: op.left,
        rightText: op.right,
        leftKind: "same",
        rightKind: "same",
      });
      leftNo += 1;
      rightNo += 1;
      continue;
    }

    if (op.type === "changed") {
      rows.push({
        leftLineNo: leftNo,
        rightLineNo: rightNo,
        leftText: op.left,
        rightText: op.right,
        leftKind: "changed",
        rightKind: "changed",
      });
      leftNo += 1;
      rightNo += 1;
      continue;
    }

    if (op.type === "remove") {
      rows.push({
        leftLineNo: leftNo,
        rightLineNo: null,
        leftText: op.left,
        rightText: "",
        leftKind: "removed",
        rightKind: "placeholder",
      });
      leftNo += 1;
      continue;
    }

    rows.push({
      leftLineNo: null,
      rightLineNo: rightNo,
      leftText: "",
      rightText: op.right,
      leftKind: "placeholder",
      rightKind: "added",
    });
    rightNo += 1;
  }

  return rows;
}

function splitLines(text: string): string[] {
  if (!text) {
    return [""];
  }
  return text.replace(/\r\n/g, "\n").split("\n");
}

type LineOp =
  | { type: "same"; left: string; right: string }
  | { type: "remove"; left: string }
  | { type: "add"; right: string }
  | { type: "changed"; left: string; right: string };

function buildLineOps(leftLines: string[], rightLines: string[]): LineOp[] {
  const m = leftLines.length;
  const n = rightLines.length;
  const dp: number[][] = Array.from({ length: m + 1 }, () => Array<number>(n + 1).fill(0));

  for (let i = m - 1; i >= 0; i -= 1) {
    for (let j = n - 1; j >= 0; j -= 1) {
      if (leftLines[i] === rightLines[j]) {
        dp[i][j] = dp[i + 1][j + 1] + 1;
      } else {
        dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
      }
    }
  }

  const ops: LineOp[] = [];
  let i = 0;
  let j = 0;
  while (i < m && j < n) {
    if (leftLines[i] === rightLines[j]) {
      ops.push({ type: "same", left: leftLines[i], right: rightLines[j] });
      i += 1;
      j += 1;
      continue;
    }
    if (dp[i + 1][j] >= dp[i][j + 1]) {
      ops.push({ type: "remove", left: leftLines[i] });
      i += 1;
    } else {
      ops.push({ type: "add", right: rightLines[j] });
      j += 1;
    }
  }

  while (i < m) {
    ops.push({ type: "remove", left: leftLines[i] });
    i += 1;
  }
  while (j < n) {
    ops.push({ type: "add", right: rightLines[j] });
    j += 1;
  }

  return ops;
}

function mergeChangedBlocks(ops: LineOp[]): LineOp[] {
  const merged: LineOp[] = [];
  let cursor = 0;

  while (cursor < ops.length) {
    const current = ops[cursor];
    if (current.type !== "remove") {
      merged.push(current);
      cursor += 1;
      continue;
    }

    const removes: string[] = [];
    while (cursor < ops.length && ops[cursor].type === "remove") {
      removes.push((ops[cursor] as { type: "remove"; left: string }).left);
      cursor += 1;
    }

    const adds: string[] = [];
    let tempCursor = cursor;
    while (tempCursor < ops.length && ops[tempCursor].type === "add") {
      adds.push((ops[tempCursor] as { type: "add"; right: string }).right);
      tempCursor += 1;
    }

    if (adds.length === 0) {
      removes.forEach((line) => merged.push({ type: "remove", left: line }));
      continue;
    }

    const pairCount = Math.min(removes.length, adds.length);
    for (let idx = 0; idx < pairCount; idx += 1) {
      merged.push({ type: "changed", left: removes[idx], right: adds[idx] });
    }
    for (let idx = pairCount; idx < removes.length; idx += 1) {
      merged.push({ type: "remove", left: removes[idx] });
    }
    for (let idx = pairCount; idx < adds.length; idx += 1) {
      merged.push({ type: "add", right: adds[idx] });
    }
    cursor = tempCursor;
  }

  return merged;
}

const setSearchRangesEffect = StateEffect.define<Array<{ from: number; to: number }>>();
const searchMatchDecoration = Decoration.mark({ class: "cm-search-match" });
const searchMatchField = StateField.define<DecorationSet>({
  create() {
    return Decoration.none;
  },
  update(value, tr) {
    for (const effect of tr.effects) {
      if (effect.is(setSearchRangesEffect)) {
        const ranges = effect.value
          .filter((range) => range.from < range.to)
          .map((range) => searchMatchDecoration.range(range.from, range.to));
        return Decoration.set(ranges, true);
      }
    }
    if (tr.docChanged) {
      return value.map(tr.changes);
    }
    return value;
  },
  provide(field) {
    return EditorView.decorations.from(field);
  },
});

const setParseButtonsEffect = StateEffect.define<Array<Range<Decoration>>>();
const parseButtonsField = StateField.define<DecorationSet>({
  create() {
    return Decoration.none;
  },
  update(value, tr) {
    for (const effect of tr.effects) {
      if (effect.is(setParseButtonsEffect)) {
        return Decoration.set(effect.value, true);
      }
    }
    if (tr.docChanged) {
      return value.map(tr.changes);
    }
    return value;
  },
  provide(field) {
    return EditorView.decorations.from(field);
  },
});

const setErrorLineEffect = StateEffect.define<number | null>();
const errorLineDecoration = Decoration.line({ class: "cm-error-line" });
const errorLineField = StateField.define<DecorationSet>({
  create() {
    return Decoration.none;
  },
  update(value, tr) {
    for (const effect of tr.effects) {
      if (effect.is(setErrorLineEffect)) {
        if (effect.value === null) {
          return Decoration.none;
        }
        return Decoration.set([errorLineDecoration.range(effect.value)], true);
      }
    }
    if (tr.docChanged) {
      return value.map(tr.changes);
    }
    return value;
  },
  provide(field) {
    return EditorView.decorations.from(field);
  },
});

const setErrorFlashEffect = StateEffect.define<{ from: number; to: number } | null>();
const errorFlashDecoration = Decoration.mark({ class: "cm-error-flash" });
const errorFlashField = StateField.define<DecorationSet>({
  create() {
    return Decoration.none;
  },
  update(value, tr) {
    for (const effect of tr.effects) {
      if (effect.is(setErrorFlashEffect)) {
        if (!effect.value) {
          return Decoration.none;
        }
        const range = effect.value;
        if (range.from >= range.to) {
          return Decoration.none;
        }
        return Decoration.set([errorFlashDecoration.range(range.from, range.to)], true);
      }
    }
    if (tr.docChanged) {
      return value.map(tr.changes);
    }
    return value;
  },
  provide(field) {
    return EditorView.decorations.from(field);
  },
});

const jsonLintExtension = linter(
  (view): Diagnostic[] => {
    const docText = view.state.doc.toString();
    const error = computeJsonError(docText);
    if (!error) {
      return [];
    }
    return [
      {
        from: error.range.from,
        to: error.range.to,
        severity: "error",
        message: error.error instanceof Error ? error.error.message : "JSON 解析失败",
      },
    ];
  },
  { delay: 250 },
);

const pageShortcutKeymap = keymap.of([
  {
    key: "Mod-s",
    preventDefault: true,
    run: () => {
      handleSave();
      return true;
    },
  },
  {
    key: "Shift-Mod-f",
    preventDefault: true,
    run: () => {
      focusSearch();
      return true;
    },
  },
]);

function buildRecordState(content: string, lastSavedContent: string): RecordUiState {
  return {
    content,
    lastSavedContent,
    searchQuery: "",
    searchCaseSensitive: false,
    searchExactMatch: false,
    currentMatchIndex: 0,
    expandLevel: 0,
    foldedRanges: [],
  };
}

function ensureRecordState(
  recordId: string,
  content: string,
  lastSavedContent: string,
): RecordUiState {
  const existing = recordStates.get(recordId);
  if (existing) {
    return existing;
  }
  const state = buildRecordState(content, lastSavedContent);
  recordStates.set(recordId, state);
  return state;
}

function getFoldedRanges(): Array<{ from: number; to: number }> {
  if (!editorView) {
    return [];
  }
  const ranges: Array<{ from: number; to: number }> = [];
  const state = editorView.state.field(foldState, false);
  if (!state) {
    return ranges;
  }
  state.between(0, editorView.state.doc.length, (from, to) => {
    ranges.push({ from, to });
  });
  return ranges;
}

function applyFoldedRanges(ranges: Array<{ from: number; to: number }>) {
  if (!editorView) {
    return;
  }
  unfoldAll(editorView);
  if (ranges.length === 0) {
    return;
  }
  editorView.dispatch({ effects: ranges.map((range) => foldEffect.of(range)) });
}

function persistActiveRecordState() {
  if (!activeRecordId.value) {
    return;
  }
  const recordId = activeRecordId.value;
  const content = getEditorContent();
  const state = recordStates.get(recordId) ?? ensureRecordState(recordId, content, content);
  state.content = content;
  state.searchQuery = searchQuery.value;
  state.searchCaseSensitive = searchCaseSensitive.value;
  state.searchExactMatch = searchExactMatch.value;
  state.currentMatchIndex = currentMatchIndex.value;
  state.expandLevel = expandLevel.value;
  state.foldedRanges = getFoldedRanges();
  markRecordDirty(recordId, state.content !== state.lastSavedContent);
}

function restoreRecordState(recordId: string, state: RecordUiState) {
  activeRecordId.value = recordId;
  if (searchTimer) {
    window.clearTimeout(searchTimer);
    searchTimer = null;
  }
  suppressSearchWatch = true;
  searchQuery.value = state.searchQuery;
  searchCaseSensitive.value = state.searchCaseSensitive;
  searchExactMatch.value = state.searchExactMatch;
  setEditorContent(state.content);
  nextTick(() => {
    suppressSearchWatch = false;
  });
  applyFoldedRanges(state.foldedRanges);
  expandLevel.value = state.expandLevel;
  if (searchQuery.value.trim()) {
    const total = matchRanges.value.length;
    if (total > 0) {
      const safeIndex = Math.min(Math.max(state.currentMatchIndex, 1), total) - 1;
      currentMatchIndex.value = safeIndex + 1;
      jumpToMatchIndex(safeIndex);
    }
  }
  markRecordDirty(recordId, state.content !== state.lastSavedContent);
}

function syncActiveRecordContent(content: string) {
  if (!activeRecordId.value) {
    return;
  }
  const recordId = activeRecordId.value;
  const state = recordStates.get(recordId) ?? ensureRecordState(recordId, content, content);
  state.content = content;
  markRecordDirty(recordId, state.content !== state.lastSavedContent);
}

function createEditor() {
  if (!editorRef.value || editorView) {
    return;
  }
  const state = EditorState.create({
    doc: rawInput.value,
    extensions: [
      lineNumbers(),
      highlightActiveLineGutter(),
      highlightSpecialChars(),
      history(),
      drawSelection(),
      dropCursor(),
      EditorState.allowMultipleSelections.of(true),
      indentOnInput(),
      bracketMatching(),
      foldGutter(),
      highlightActiveLine(),
      rectangularSelection(),
      crosshairCursor(),
      pageShortcutKeymap,
      keymap.of([indentWithTab, ...defaultKeymap, ...historyKeymap, ...foldKeymap]),
      json(),
      search({ top: false }),
      placeholder("在此粘贴或输入 JSON 文本..."),
      EditorView.lineWrapping,
      editorTheme,
      searchMatchField,
      parseButtonsField,
      errorFlashField,
      errorLineField,
      jsonLintExtension,
      EditorView.updateListener.of((update) => {
        if (!update.docChanged) {
          return;
        }
        if (ignoreEditorChange) {
          return;
        }
        rawInput.value = update.state.doc.toString();
        syncActiveRecordContent(rawInput.value);
        recomputeMatches();
        recomputeParseButtons();
        recomputeMaxDepth(true);
        scheduleErrorLineUpdate();
      }),
    ],
  });
  editorView = new EditorView({
    state,
    parent: editorRef.value,
  });
  applySearchQuery(searchQuery.value.trim());
  recomputeParseButtons();
  recomputeMaxDepth();
}

function destroyEditor() {
  editorView?.destroy();
  editorView = null;
}

function setEditorContent(value: string) {
  const nextValue = value ?? "";
  if (!editorView) {
    rawInput.value = nextValue;
    return;
  }
  const current = editorView.state.doc.toString();
  if (current !== nextValue) {
    ignoreEditorChange = true;
    editorView.dispatch({
      changes: { from: 0, to: current.length, insert: nextValue },
    });
    ignoreEditorChange = false;
  }
  rawInput.value = nextValue;
  applySearchQuery(searchQuery.value.trim());
  recomputeParseButtons();
  recomputeMaxDepth();
  scheduleErrorLineUpdate();
}

function getEditorContent(): string {
  if (editorView) {
    return editorView.state.doc.toString();
  }
  return rawInput.value;
}

function applySearchQuery(_value: string) {
  recomputeMatches();
}

function toggleCaseSensitive() {
  searchCaseSensitive.value = !searchCaseSensitive.value;
}

function toggleExactMatch() {
  searchExactMatch.value = !searchExactMatch.value;
}

function refreshSearch() {
  applySearchQuery(searchQuery.value.trim());
  jumpToFirstMatch();
}

async function loadRecords() {
  if (!canUse.value) {
    records.value = [];
    return;
  }
  recordsLoading.value = true;
  try {
    const list = await listJsonFmtRecords(historyKeyword.value);
    records.value = list;
    if (activeRecordId.value && !isDraftId(activeRecordId.value)) {
      const matchedName = list.find((item) => item.id === activeRecordId.value)?.name;
      if (matchedName) {
        activeRecordName.value = matchedName;
      }
    }
    if (isDraftId(activeRecordId.value)) {
      return;
    }
    if (!activeRecordId.value && list.length === 0 && draftRecords.value.length === 0) {
      createNewRecord();
      return;
    }
    if (!activeRecordId.value && list.length > 0) {
      await selectRecord(list[0].id);
      return;
    }
    if (activeRecordId.value) {
      const matched = list.find((item) => item.id === activeRecordId.value);
      if (!matched && list.length > 0) {
        await selectRecord(list[0].id);
      } else if (!matched && list.length === 0 && draftRecords.value.length === 0) {
        createNewRecord();
      }
    }
  } catch (_err) {
    message.warning("记录列表加载失败");
  } finally {
    recordsLoading.value = false;
  }
}

function normalizeRecordContent(detail: JsonFmtRecordDetail): string {
  const content = detail.content || "";
  if (!content) {
    return "";
  }
  try {
    const result = parseJsonInput(content.trim());
    return result.pretty;
  } catch (_err) {
    return content;
  }
}

function initRecordStateFromDetail(detail: JsonFmtRecordDetail): RecordUiState {
  const normalized = normalizeRecordContent(detail);
  const state = buildRecordState(normalized, normalized);
  recordStates.set(detail.id, state);
  return state;
}

async function selectRecord(recordId: string) {
  if (!canUse.value) {
    return;
  }
  if (!recordId) {
    return;
  }
  if (recordId === activeRecordId.value) {
    return;
  }
  if (editingId.value) {
    confirmRename();
  }
  persistActiveRecordState();
  if (isDraftId(recordId)) {
    const draft = draftRecords.value.find((item) => item.id === recordId);
    activeRecordName.value = draft?.name ?? "";
    activeRecord.value = null;
    const draftState = recordStates.get(recordId) ?? ensureRecordState(recordId, "", "");
    restoreRecordState(recordId, draftState);
    return;
  }
  const cached = recordStates.get(recordId);
  if (cached) {
    const saved = records.value.find((item) => item.id === recordId);
    activeRecordName.value = saved?.name ?? "";
    activeRecord.value = null;
    restoreRecordState(recordId, cached);
    return;
  }
  try {
    const detail = await getJsonFmtRecord(recordId);
    activeRecord.value = detail;
    activeRecordName.value = detail.name;
    const state = initRecordStateFromDetail(detail);
    restoreRecordState(recordId, state);
  } catch (_err) {
    message.error("记录加载失败");
  }
}

async function deleteSelectedRecords() {
  if (!deleteMode.value) {
    return;
  }
  const selectedIds = [...deleteSelectedIds.value];
  if (selectedIds.length === 0) {
    message.warning("请先勾选要删除的记录");
    return;
  }
  const activeId = activeRecordId.value;
  const baseId = activeId && selectedIds.includes(activeId) ? activeId : null;
  const deleted = await removeRecords(selectedIds, baseId);
  if (!deleted) {
    return;
  }
  clearDeleteSelection();
  compareResult.value = null;
  message.success(`已删除 ${selectedIds.length} 条记录`);
}

async function deleteActiveRecordByShortcut() {
  if (!canUse.value) {
    return;
  }
  if (!activeRecordId.value) {
    return;
  }
  const deleted = await removeRecords([activeRecordId.value], activeRecordId.value);
  if (!deleted) {
    return;
  }
  compareResult.value = null;
  message.success("已删除当前记录");
}

async function removeRecords(
  recordIds: string[],
  preferredBaseId: string | null,
): Promise<boolean> {
  const normalizedIds = Array.from(
    new Set(recordIds.map((id) => id.trim()).filter((id) => id.length > 0)),
  );
  if (normalizedIds.length === 0) {
    return false;
  }

  const deletingSet = new Set(normalizedIds);
  const baseId = preferredBaseId ?? activeRecordId.value;
  const nextRecordId = baseId ? pickNextRecordIdAfterDelete(baseId, deletingSet) : null;

  const savedIds = normalizedIds.filter((id) => !isDraftId(id));
  try {
    if (savedIds.length === 1) {
      await deleteJsonFmtRecord(savedIds[0]);
    } else if (savedIds.length > 1) {
      await batchDeleteJsonFmtRecords({ recordIds: savedIds });
    }
  } catch (_err) {
    message.error("删除失败");
    return false;
  }

  records.value = records.value.filter((item) => !deletingSet.has(item.id));
  draftRecords.value = draftRecords.value.filter((item) => !deletingSet.has(item.id));
  removeIdsFromSet(dirtySavedIds, deletingSet);
  removeIdsFromSet(dirtyDraftIds, deletingSet);
  normalizedIds.forEach((id) => {
    recordStates.delete(id);
  });
  compareSelectedIds.value = compareSelectedIds.value.filter((id) => !deletingSet.has(id));
  deleteSelectedIds.value = deleteSelectedIds.value.filter((id) => !deletingSet.has(id));

  if (compareResult.value) {
    if (
      deletingSet.has(compareResult.value.left.id) ||
      deletingSet.has(compareResult.value.right.id)
    ) {
      compareResult.value = null;
    }
  }

  if (activeRecordId.value && deletingSet.has(activeRecordId.value)) {
    if (nextRecordId) {
      await selectRecord(nextRecordId);
    } else {
      clearActiveRecord();
      if (records.value.length === 0 && draftRecords.value.length === 0) {
        createNewRecord();
      }
    }
  }

  return true;
}

function pickNextRecordIdAfterDelete(baseId: string, deletingSet: Set<string>): string | null {
  const list = displayRecords.value;
  const currentIndex = list.findIndex((item) => item.id === baseId);

  if (currentIndex < 0) {
    for (const item of list) {
      if (!deletingSet.has(item.id)) {
        return item.id;
      }
    }
    return null;
  }

  for (let i = currentIndex + 1; i < list.length; i += 1) {
    if (!deletingSet.has(list[i].id)) {
      return list[i].id;
    }
  }
  for (let i = currentIndex - 1; i >= 0; i -= 1) {
    if (!deletingSet.has(list[i].id)) {
      return list[i].id;
    }
  }
  return null;
}

function clearActiveRecord() {
  activeRecordId.value = null;
  activeRecord.value = null;
  activeRecordName.value = "";
  setEditorContent("");
}

function removeIdsFromSet(setRef: typeof dirtySavedIds, deletingSet: Set<string>) {
  const next = new Set(Array.from(setRef.value).filter((id) => !deletingSet.has(id)));
  setRef.value = next;
}

function isEditableDeleteTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false;
  }
  const tag = target.tagName.toLowerCase();
  if (tag === "input" || tag === "textarea" || tag === "select") {
    return true;
  }
  if (target.isContentEditable) {
    return true;
  }
  return target.closest(".cm-editor") !== null;
}

function createNewRecord() {
  if (!canUse.value) {
    return;
  }
  if (editingId.value) {
    confirmRename();
  }
  persistActiveRecordState();
  const index = draftCounter.value++;
  const now = new Date().toISOString();
  const draftId = `draft-${Date.now()}-${index}`;
  const draft: DraftRecord = {
    id: draftId,
    name: `未命名-${index}`,
    createdAt: now,
    updatedAt: now,
  };
  draftRecords.value = [draft, ...draftRecords.value];
  activeRecordName.value = draft.name;
  const state = buildRecordState("", "");
  state.content = "";
  recordStates.set(draftId, state);
  markRecordDirty(draftId, true);
  activeRecord.value = null;
  restoreRecordState(draftId, state);
}

function handleFormat() {
  if (!canUse.value) {
    return;
  }
  const raw = getEditorContent();
  const input = raw.trim();
  if (!input) {
    message.warning("请输入 JSON 内容");
    return;
  }
  try {
    const result = parseJsonInput(raw);
    setEditorContent(result.pretty);
    syncActiveRecordContent(result.pretty);
    if (result.usedEscaped) {
      message.info("已识别转义 JSON 并自动解析");
    }
  } catch (err) {
    focusJsonError(raw, err, true);
    message.error(err instanceof Error ? err.message : "JSON 解析失败");
  }
}

async function handleSave() {
  if (!canUse.value) {
    return;
  }
  if (!activeRecordId.value) {
    createNewRecord();
    return;
  }
  persistActiveRecordState();
  const raw = getEditorContent();
  const input = raw.trim();
  if (!input) {
    message.warning("请输入 JSON 内容");
    return;
  }
  if (saving.value) {
    return;
  }
  let parsed: { value: unknown; pretty: string; usedEscaped: boolean } | null = null;
  try {
    parsed = parseJsonInput(raw);
  } catch (_err) {
    focusJsonError(raw, _err, true);
    message.error("JSON 解析失败，请检查输入内容");
    return;
  }
  saving.value = true;
  try {
    const recordId = activeRecordId.value;
    const draft = recordId ? draftRecords.value.find((item) => item.id === recordId) : null;
    const recordName = currentRecordName.value || draft?.name;
    const currentContent = getEditorContent();
    const detail = await saveJsonFmtRecord({
      id: recordId && !isDraftId(recordId) ? recordId : undefined,
      name: recordName || undefined,
      content: parsed.pretty,
    });
    message.success("保存成功");
    updateRecordsAfterSave(detail);
    if (recordId && recordId === detail.id) {
      activeRecordName.value = detail.name;
    }
    if (recordId && isDraftId(recordId)) {
      draftRecords.value = draftRecords.value.filter((item) => item.id !== recordId);
      updateDirtySet(dirtyDraftIds, recordId, false);
      const previousState = recordStates.get(recordId);
      recordStates.delete(recordId);
      activeRecordId.value = detail.id;
      activeRecord.value = detail;
      activeRecordName.value = detail.name;
      const nextState = previousState ?? buildRecordState(currentContent, currentContent);
      nextState.content = currentContent;
      nextState.lastSavedContent = currentContent;
      recordStates.set(detail.id, nextState);
      restoreRecordState(detail.id, nextState);
      updateDirtySet(dirtySavedIds, detail.id, false);
      return;
    }
    if (recordId) {
      const state =
        recordStates.get(recordId) ?? ensureRecordState(recordId, currentContent, currentContent);
      state.content = currentContent;
      state.lastSavedContent = currentContent;
      markRecordDirty(recordId, false);
    }
  } catch (_err) {
    message.error("保存失败");
  } finally {
    saving.value = false;
  }
}

function handleDownload() {
  if (!canUse.value) {
    return;
  }
  const raw = getEditorContent();
  const input = raw.trim();
  if (!input) {
    message.warning("请输入 JSON 内容");
    return;
  }
  let parsed: { value: unknown; pretty: string; usedEscaped: boolean } | null = null;
  try {
    parsed = parseJsonInput(raw);
  } catch (_err) {
    focusJsonError(raw, _err, true);
    message.error("JSON 解析失败，无法下载");
    return;
  }
  const blob = new Blob([parsed.pretty], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = resolveDownloadName();
  anchor.click();
  URL.revokeObjectURL(url);
}

async function handleCompressCopy() {
  if (!canUse.value) {
    return;
  }
  const raw = getEditorContent();
  const input = raw.trim();
  if (!input) {
    message.warning("请输入 JSON 内容");
    return;
  }
  let parsed: { value: unknown; pretty: string; usedEscaped: boolean } | null = null;
  try {
    parsed = parseJsonInput(raw);
  } catch (_err) {
    focusJsonError(raw, _err, true);
    message.error("JSON 解析失败，无法压缩复制");
    return;
  }

  const compact = toCompactJson(parsed.value);
  try {
    await navigator.clipboard.writeText(compact);
    message.success("压缩 JSON 已复制");
  } catch (_err) {
    message.warning("复制失败，请手动复制");
  }
}

function expandAll() {
  if (!editorView || !canFold.value) {
    return;
  }
  unfoldAll(editorView);
  expandLevel.value = maxExpandDepth.value;
}

function collapseAll() {
  if (!editorView || !canFold.value) {
    return;
  }
  foldAll(editorView);
  expandLevel.value = 0;
}

function startRename(record: { id: string; name: string }) {
  if (!canUse.value) {
    return;
  }
  if (selectionMode.value) {
    return;
  }
  if (!record) {
    return;
  }
  editingId.value = record.id;
  editingName.value = record.name;
  nextTick(() => {
    const input = renameInputRefs.get(record.id);
    input?.focus();
    input?.select();
  });
}

async function confirmRename() {
  if (!editingId.value) {
    return;
  }
  const recordId = editingId.value;
  const name = editingName.value.trim();
  const currentName = displayRecords.value.find((item) => item.id === recordId)?.name?.trim() || "";
  if (!name) {
    message.warning("名称不能为空");
    return;
  }
  editingId.value = null;
  if (name === currentName) {
    editingName.value = "";
    return;
  }
  if (isDraftId(recordId)) {
    const now = new Date().toISOString();
    draftRecords.value = draftRecords.value.map((item) =>
      item.id === recordId ? { ...item, name, updatedAt: now } : item,
    );
    if (activeRecordId.value === recordId) {
      activeRecordName.value = name;
    }
    message.success("重命名成功");
    return;
  }
  try {
    await renameJsonFmtRecord(recordId, { name });
    records.value = records.value.map((item) =>
      item.id === recordId ? { ...item, name, updatedAt: new Date().toISOString() } : item,
    );
    if (activeRecordId.value === recordId && activeRecord.value) {
      activeRecord.value = { ...activeRecord.value, name };
    }
    if (activeRecordId.value === recordId) {
      activeRecordName.value = name;
    }
    message.success("重命名成功");
  } catch (_err) {
    message.error("重命名失败");
  }
}

function cancelRename() {
  editingId.value = null;
  editingName.value = "";
}

function setRenameInputRef(id: string, el: HTMLInputElement | null) {
  if (!id) {
    return;
  }
  if (el) {
    renameInputRefs.set(id, el);
  } else {
    renameInputRefs.delete(id);
  }
}

function handleDragStart(id: string) {
  if (!canUse.value) {
    return;
  }
  if (isDraftId(id)) {
    return;
  }
  if (editingId.value) {
    return;
  }
  draggingId.value = id;
}

function updateRecordsAfterSave(detail: JsonFmtRecordDetail) {
  const index = records.value.findIndex((item) => item.id === detail.id);
  const normalizedKeyword = historyKeyword.value.trim().toLowerCase();
  const nameMatches = !normalizedKeyword || detail.name.toLowerCase().includes(normalizedKeyword);
  if (index >= 0) {
    if (!nameMatches) {
      records.value.splice(index, 1);
      return;
    }
    records.value[index] = {
      ...records.value[index],
      name: detail.name,
      updatedAt: detail.updatedAt,
    };
    return;
  }
  if (!nameMatches) {
    return;
  }
  records.value.unshift({
    id: detail.id,
    name: detail.name,
    orderNo: 0,
    updatedAt: detail.updatedAt,
  });
}

function handleDragOver(id: string) {
  if (!draggingId.value || draggingId.value === id) {
    return;
  }
  if (isDraftId(id) || isDraftId(draggingId.value)) {
    return;
  }
  dragOverId.value = id;
}

async function handleDrop(targetId: string) {
  const sourceId = draggingId.value;
  if (!sourceId || sourceId === targetId) {
    dragOverId.value = null;
    draggingId.value = null;
    return;
  }
  if (isDraftId(sourceId) || isDraftId(targetId)) {
    handleDragEnd();
    return;
  }
  await reorderRecords(sourceId, targetId);
}

async function handleDropToEnd() {
  if (!draggingId.value) {
    return;
  }
  const sourceId = draggingId.value;
  if (isDraftId(sourceId)) {
    handleDragEnd();
    return;
  }
  const last = records.value[records.value.length - 1];
  if (!last || last.id === sourceId) {
    dragOverId.value = null;
    draggingId.value = null;
    return;
  }
  await reorderRecords(sourceId, last.id, true);
}

function handleDragEnd() {
  dragOverId.value = null;
  draggingId.value = null;
}

async function reorderRecords(sourceId: string, targetId: string, moveAfter = false) {
  if (!canUse.value) {
    return;
  }
  if (isDraftId(sourceId) || isDraftId(targetId)) {
    handleDragEnd();
    return;
  }
  const items = [...records.value];
  const fromIndex = items.findIndex((item) => item.id === sourceId);
  const toIndex = items.findIndex((item) => item.id === targetId);
  if (fromIndex < 0 || toIndex < 0) {
    handleDragEnd();
    return;
  }
  const [moved] = items.splice(fromIndex, 1);
  const insertIndex = moveAfter ? toIndex + 1 : toIndex;
  items.splice(insertIndex, 0, moved);
  records.value = items;
  handleDragEnd();
  try {
    await reorderJsonFmtRecords({ orderedIds: items.map((item) => item.id) });
  } catch (_err) {
    message.error("排序保存失败");
    await loadRecords();
  }
}

function parseJsonInput(input: string): { value: unknown; pretty: string; usedEscaped: boolean } {
  const direct = tryParseJson(input);
  if (direct.success) {
    const resolved = unwrapJsonStringLayers(direct.value);
    return {
      value: resolved.value,
      pretty: toPrettyJson(resolved.value),
      usedEscaped: resolved.usedEscaped,
    };
  }

  const unescaped = tryUnescape(input);
  if (unescaped !== null) {
    const parsedUnescaped = tryParseJson(unescaped);
    if (parsedUnescaped.success) {
      const resolved = unwrapJsonStringLayers(parsedUnescaped.value);
      return {
        value: resolved.value,
        pretty: toPrettyJson(resolved.value),
        usedEscaped: true,
      };
    }
  }

  if (direct.error) {
    throw direct.error;
  }
  throw new Error("JSON 解析失败，请检查输入内容");
}

function tryParseJson(input: string): { success: boolean; value: unknown; error?: unknown } {
  try {
    return { success: true, value: JSON.parse(input) };
  } catch (err) {
    return { success: false, value: null, error: err };
  }
}

function tryUnescape(input: string): string | null {
  try {
    return JSON.parse(`"${input.replace(/"/g, '\\"')}"`);
  } catch (_err) {
    return null;
  }
}

function toPrettyJson(value: unknown): string {
  return escapeInvisibleUnicodeInJsonText(JSON.stringify(value, null, 2));
}

function toCompactJson(value: unknown): string {
  return escapeInvisibleUnicodeInJsonText(JSON.stringify(value));
}

function escapeInvisibleUnicodeInJsonText(text: string): string {
  return text.replace(/(?:\u00A0|\u200B|\u200C|\u200D|\u2060|\uFEFF)/g, (char) => {
    const code = char.codePointAt(0);
    if (code == null) {
      return char;
    }
    return `\\u${code.toString(16).toUpperCase().padStart(4, "0")}`;
  });
}

function unwrapJsonStringLayers(
  value: unknown,
  maxDepth = 6,
): { value: unknown; usedEscaped: boolean } {
  let current = value;
  let usedEscaped = false;

  for (let i = 0; i < maxDepth; i += 1) {
    if (typeof current !== "string") {
      break;
    }
    const trimmed = current.trim();
    if (!trimmed) {
      break;
    }
    const nested = tryParseJson(trimmed);
    if (!nested.success) {
      break;
    }
    current = nested.value;
    usedEscaped = true;
  }

  return { value: current, usedEscaped };
}

function resolveJsonErrorRange(text: string, err: unknown): { from: number; to: number } | null {
  if (!text) {
    return null;
  }
  const message = err instanceof Error ? err.message : "";
  const match = /position\s+(\d+)/i.exec(message);
  let pos = match ? Number(match[1]) : text.length - 1;
  if (!Number.isFinite(pos)) {
    pos = text.length - 1;
  }
  if (text.length === 0) {
    return null;
  }
  const clamped = Math.min(Math.max(pos, 0), Math.max(text.length - 1, 0));
  return { from: clamped, to: Math.min(clamped + 1, text.length) };
}

function computeJsonError(
  text: string,
): { range: { from: number; to: number }; error: unknown } | null {
  if (!text.trim()) {
    return null;
  }
  const direct = tryParseJson(text);
  if (direct.success) {
    return null;
  }
  const unescaped = tryUnescape(text);
  if (unescaped !== null) {
    const nested = tryParseJson(unescaped);
    if (nested.success) {
      return null;
    }
  }
  const error = direct.error ?? new Error("JSON 解析失败");
  const range = resolveJsonErrorRange(text, error);
  if (!range) {
    return null;
  }
  return { range, error };
}

function scheduleErrorLineUpdate() {
  if (!editorView) {
    return;
  }
  if (errorLineTimer) {
    window.clearTimeout(errorLineTimer);
  }
  errorLineTimer = window.setTimeout(() => {
    errorLineTimer = null;
    const docText = editorView?.state.doc.toString() ?? "";
    const error = computeJsonError(docText);
    if (!editorView) {
      return;
    }
    if (!error) {
      editorView.dispatch({ effects: setErrorLineEffect.of(null) });
      return;
    }
    const lineStart = editorView.state.doc.lineAt(error.range.from).from;
    editorView.dispatch({ effects: setErrorLineEffect.of(lineStart) });
  }, 250);
}

function focusJsonError(text: string, err: unknown, scrollIntoView = false) {
  if (!editorView) {
    return;
  }
  const range = resolveJsonErrorRange(text, err);
  if (!range) {
    return;
  }
  if (!scrollIntoView) {
    return;
  }
  editorView.dispatch({ effects: EditorView.scrollIntoView(range.from, { y: "center" }) });
  triggerErrorFlash(range);
}

function triggerErrorFlash(range: { from: number; to: number }) {
  if (!editorView) {
    return;
  }
  editorView.dispatch({ effects: setErrorFlashEffect.of(range) });
  if (errorFlashTimer) {
    window.clearTimeout(errorFlashTimer);
  }
  errorFlashTimer = window.setTimeout(() => {
    editorView?.dispatch({ effects: setErrorFlashEffect.of(null) });
    errorFlashTimer = null;
  }, 2000);
}

function applySearchHighlights(ranges: Array<{ from: number; to: number }>) {
  if (!editorView) {
    return;
  }
  editorView.dispatch({ effects: setSearchRangesEffect.of(ranges) });
}

function applyParseButtons(buttons: Array<Range<Decoration>>) {
  if (!editorView) {
    return;
  }
  editorView.dispatch({ effects: setParseButtonsEffect.of(buttons) });
}

function recomputeMatches() {
  const query = searchQuery.value.trim();
  if (!editorView || !query) {
    matchRanges.value = [];
    matchCount.value = 0;
    currentMatchIndex.value = 0;
    applySearchHighlights([]);
    return;
  }
  const ranges = searchExactMatch.value
    ? findExactMatches(editorView.state.doc.toString(), query)
    : findLooseMatches(editorView.state.doc.toString(), query);
  matchRanges.value = ranges;
  matchCount.value = ranges.length;
  currentMatchIndex.value = ranges.length > 0 ? 1 : 0;
  applySearchHighlights(ranges);
}

function jumpToFirstMatch() {
  if (!editorView || matchRanges.value.length === 0) {
    return;
  }
  currentMatchIndex.value = 1;
  jumpToMatchIndex(0);
}

function jumpPrevMatch() {
  if (!editorView || matchRanges.value.length === 0) {
    return;
  }
  const total = matchRanges.value.length;
  if (total <= 1) {
    return;
  }
  let nextIndex = currentMatchIndex.value - 2;
  if (nextIndex < 0) {
    nextIndex = total - 1;
    message.info("跳转到了最后一个匹配项");
  }
  currentMatchIndex.value = nextIndex + 1;
  jumpToMatchIndex(nextIndex);
}

function jumpNextMatch() {
  if (!editorView || matchRanges.value.length === 0) {
    return;
  }
  const total = matchRanges.value.length;
  if (total <= 1) {
    return;
  }
  let nextIndex = currentMatchIndex.value;
  if (nextIndex >= total) {
    nextIndex = 0;
    message.info("跳转到了第一个匹配项");
  }
  currentMatchIndex.value = nextIndex + 1;
  jumpToMatchIndex(nextIndex);
}

function jumpToMatchIndex(index: number) {
  const range = matchRanges.value[index];
  if (!range || !editorView) {
    return;
  }
  editorView.dispatch({ selection: { anchor: range.from, head: range.to }, scrollIntoView: true });
}

function findExactMatches(docText: string, query: string): Array<{ from: number; to: number }> {
  if (!editorView) {
    return [];
  }
  const ranges: Array<{ from: number; to: number }> = [];
  const tree = syntaxTree(editorView.state);
  const queryNormalized = normalizeQuery(query);
  tree.iterate({
    enter(node) {
      if (!isJsonValueOrKey(node.name)) {
        return;
      }
      const raw = docText.slice(node.from, node.to);
      const normalized = normalizeNodeText(node.name, raw);
      if (matchesExact(normalized, queryNormalized)) {
        const matchRange = resolveValueMatchRange(node.name, raw, node.from);
        ranges.push(matchRange);
      }
    },
  });
  return ranges;
}

function findLooseMatches(docText: string, query: string): Array<{ from: number; to: number }> {
  if (!editorView) {
    return [];
  }
  const ranges: Array<{ from: number; to: number }> = [];
  const tree = syntaxTree(editorView.state);
  const queryNormalized = normalizeQuery(query);
  tree.iterate({
    enter(node) {
      if (!isJsonValueOrKey(node.name)) {
        return;
      }
      const raw = docText.slice(node.from, node.to);
      const normalized = normalizeNodeText(node.name, raw);
      let searchIndex = indexOfQuery(normalized, queryNormalized);
      while (searchIndex !== -1) {
        const matchRange = resolveValueMatchRange(
          node.name,
          raw,
          node.from,
          searchIndex,
          queryNormalized.length,
        );
        ranges.push(matchRange);
        const nextIndex = searchIndex + Math.max(queryNormalized.length, 1);
        searchIndex =
          nextIndex >= normalized.length
            ? -1
            : indexOfQuery(normalized, queryNormalized, nextIndex);
      }
    },
  });
  return ranges;
}

function normalizeQuery(query: string): string {
  return searchCaseSensitive.value ? query : query.toLowerCase();
}

function normalizeNodeText(nodeName: string, raw: string): string {
  const trimmed = raw.trim();
  let value = trimmed;
  if (isStringNode(nodeName)) {
    value = stripQuotes(trimmed);
  }
  return searchCaseSensitive.value ? value : value.toLowerCase();
}

function matchesExact(value: string, query: string): boolean {
  return value === query;
}

function indexOfQuery(value: string, query: string, fromIndex = 0): number {
  if (!query) {
    return -1;
  }
  return value.indexOf(query, fromIndex);
}

function resolveValueMatchRange(
  nodeName: string,
  raw: string,
  start: number,
  matchIndex = 0,
  matchLength?: number,
) {
  if (isStringNode(nodeName)) {
    const offset = raw.indexOf('"') + 1;
    const matchLen = matchLength ?? stripQuotes(raw).length;
    return { from: start + offset + matchIndex, to: start + offset + matchIndex + matchLen };
  }
  const trimmed = raw.trim();
  const rawOffset = raw.indexOf(trimmed);
  const matchLen = matchLength ?? trimmed.length;
  return { from: start + rawOffset + matchIndex, to: start + rawOffset + matchIndex + matchLen };
}

function isJsonValueOrKey(nodeName: string): boolean {
  return (
    isStringNode(nodeName) ||
    nodeName === "Number" ||
    nodeName === "True" ||
    nodeName === "False" ||
    nodeName === "Null"
  );
}

function isStringNode(nodeName: string): boolean {
  return nodeName === "String" || nodeName === "PropertyName";
}

function stripQuotes(raw: string): string {
  if (raw.startsWith('"') && raw.endsWith('"')) {
    return raw.substring(1, raw.length - 1);
  }
  return raw;
}

class ParseJsonWidget extends WidgetType {
  private readonly from: number;
  private readonly to: number;
  private readonly raw: string;

  constructor(from: number, to: number, raw: string) {
    super();
    this.from = from;
    this.to = to;
    this.raw = raw;
  }

  eq(other: ParseJsonWidget): boolean {
    return this.from === other.from && this.to === other.to && this.raw === other.raw;
  }

  toDOM(): HTMLElement {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "cm-json-parse-btn";
    button.textContent = "解析 JSON";
    button.addEventListener("click", (event) => {
      event.preventDefault();
      event.stopPropagation();
      handleParseJsonString(this.from, this.to, this.raw);
    });
    return button;
  }
}

function recomputeParseButtons() {
  if (!editorView) {
    return;
  }
  const docText = editorView.state.doc.toString();
  const ranges: Array<Range<Decoration>> = [];
  const tree = syntaxTree(editorView.state);
  tree.iterate({
    enter(node) {
      if (node.name !== "String") {
        return;
      }
      const raw = docText.slice(node.from, node.to);
      if (!isEscapedJsonValue(raw)) {
        return;
      }
      const widget = new ParseJsonWidget(node.from, node.to, raw);
      ranges.push(Decoration.widget({ widget, side: 1 }).range(node.to));
    },
  });
  applyParseButtons(ranges);
}

function recomputeMaxDepth(preserveExpandLevel = false) {
  if (!editorView) {
    maxExpandDepth.value = 0;
    if (!preserveExpandLevel) {
      expandLevel.value = 0;
    }
    return;
  }
  maxExpandDepth.value = computeMaxDepth(editorView.state);
  if (preserveExpandLevel) {
    expandLevel.value = Math.min(expandLevel.value, maxExpandDepth.value);
  } else {
    expandLevel.value = 0;
  }
}

function expandStep() {
  if (!editorView) {
    return;
  }
  if (maxExpandDepth.value <= 0) {
    return;
  }
  if (expandLevel.value >= maxExpandDepth.value) {
    message.info("已展开到最深层");
    return;
  }
  expandLevel.value += 1;
  const ranges = collectFoldRangesWithDepth(editorView.state);
  if (ranges.length === 0) {
    message.info("无可展开内容");
    return;
  }
  const effects = ranges.map(({ range, depth }) =>
    depth <= expandLevel.value ? unfoldEffect.of(range) : foldEffect.of(range),
  );
  editorView.dispatch({ effects });
}

function computeMaxDepth(state: EditorState): number {
  const tree = syntaxTree(state);
  let maxDepth = 0;
  const stack: Array<{ to: number; depth: number }> = [];
  tree.iterate({
    enter(node) {
      while (stack.length > 0 && node.from >= stack[stack.length - 1].to) {
        stack.pop();
      }
      if (!isContainerNode(node.name)) {
        return;
      }
      const depth = (stack[stack.length - 1]?.depth ?? 0) + 1;
      stack.push({ to: node.to, depth });
      if (depth > maxDepth) {
        maxDepth = depth;
      }
    },
    leave(node) {
      if (!isContainerNode(node.name)) {
        return;
      }
      if (stack.length > 0 && stack[stack.length - 1].to === node.to) {
        stack.pop();
      }
    },
  });
  return maxDepth;
}

function collectFoldRangesWithDepth(
  state: EditorState,
): Array<{ range: { from: number; to: number }; depth: number }> {
  const tree = syntaxTree(state);
  const ranges: Array<{ range: { from: number; to: number }; depth: number }> = [];
  const stack: Array<{ to: number; depth: number }> = [];
  tree.iterate({
    enter(node) {
      while (stack.length > 0 && node.from >= stack[stack.length - 1].to) {
        stack.pop();
      }
      if (!isContainerNode(node.name)) {
        return;
      }
      const depth = (stack[stack.length - 1]?.depth ?? 0) + 1;
      stack.push({ to: node.to, depth });
      const line = state.doc.lineAt(node.from);
      const foldRange = foldable(state, line.from, line.to);
      if (foldRange) {
        ranges.push({ range: foldRange, depth });
      }
    },
    leave(node) {
      if (!isContainerNode(node.name)) {
        return;
      }
      if (stack.length > 0 && stack[stack.length - 1].to === node.to) {
        stack.pop();
      }
    },
  });
  return ranges;
}

function isContainerNode(nodeName: string): boolean {
  return nodeName === "Object" || nodeName === "Array";
}

function isEscapedJsonValue(raw: string): boolean {
  return parseEscapedJson(raw) !== null;
}

function parseEscapedJson(raw: string): string | null {
  const direct = tryParseJson(raw);
  if (!direct.success) {
    return null;
  }

  const resolved = unwrapJsonStringLayers(direct.value);
  if (!resolved.usedEscaped) {
    return null;
  }
  if (resolved.value == null || typeof resolved.value !== "object") {
    return null;
  }
  return toPrettyJson(resolved.value);
}

function handleParseJsonString(from: number, to: number, raw: string) {
  if (!editorView) {
    return;
  }
  const current = editorView.state.doc.sliceString(from, to);
  if (current !== raw) {
    recomputeParseButtons();
    return;
  }
  const pretty = parseEscapedJson(raw);
  if (!pretty) {
    return;
  }
  const line = editorView.state.doc.lineAt(from);
  const indentMatch = line.text.match(/^\s*/);
  const indent = indentMatch ? indentMatch[0] : "";
  const formatted = indentMultilineJson(pretty, indent);
  editorView.dispatch({ changes: { from, to, insert: formatted } });
  recomputeMaxDepth();
  recomputeParseButtons();
}

function indentMultilineJson(pretty: string, indent: string): string {
  if (!pretty.includes("\n")) {
    return pretty;
  }
  const lines = pretty.split("\n");
  return lines.map((line, index) => (index === 0 ? line : `${indent}${line}`)).join("\n");
}

function resolveDownloadName(): string {
  const baseName = currentRecordName.value || "jsonfmt";
  const sanitized = baseName.replace(/[\\/:*?"<>|]/g, "-").trim();
  if (!sanitized) {
    return "jsonfmt.json";
  }
  return sanitized.endsWith(".json") ? sanitized : `${sanitized}.json`;
}

function focusSearch() {
  if (!canUse.value) {
    return;
  }
  searchInputRef.value?.focus();
  searchInputRef.value?.select();
}

function handleKeydown(event: KeyboardEvent) {
  if (event.isComposing) {
    return;
  }
  if (event.defaultPrevented) {
    return;
  }
  const key = event.key.toLowerCase();
  if (key === "escape" && selectionMode.value) {
    event.preventDefault();
    exitActionMode();
    return;
  }
  if (
    key === "delete" &&
    actionMode.value === "none" &&
    !event.altKey &&
    !event.ctrlKey &&
    !event.metaKey &&
    !event.shiftKey
  ) {
    if (isEditableDeleteTarget(event.target)) {
      return;
    }
    event.preventDefault();
    if (canUse.value) {
      void deleteActiveRecordByShortcut();
    }
    return;
  }
  if (event.altKey && !event.ctrlKey && !event.metaKey && key === "c") {
    event.preventDefault();
    if (canUse.value) {
      void handleCompressCopy();
    }
    return;
  }
  const isMeta = event.metaKey || event.ctrlKey;
  if (!isMeta) {
    return;
  }
  if (!event.shiftKey && key === "s") {
    event.preventDefault();
    if (canUse.value) {
      handleSave();
    }
    return;
  }
  if (event.shiftKey && key === "f") {
    event.preventDefault();
    if (canUse.value) {
      focusSearch();
    }
    return;
  }
}

watch(searchQuery, (value) => {
  if (suppressSearchWatch) {
    return;
  }
  if (searchTimer) {
    window.clearTimeout(searchTimer);
  }
  searchTimer = window.setTimeout(() => {
    applySearchQuery(value.trim());
    jumpToFirstMatch();
  }, 200);
});

watch(searchCaseSensitive, () => {
  if (suppressSearchWatch) {
    return;
  }
  refreshSearch();
});

watch(searchExactMatch, () => {
  if (suppressSearchWatch) {
    return;
  }
  refreshSearch();
});

watch(
  displayRecords,
  (list) => {
    const idSet = new Set(list.map((item) => item.id));
    compareSelectedIds.value = compareSelectedIds.value.filter((id) => idSet.has(id));
    deleteSelectedIds.value = deleteSelectedIds.value.filter((id) => idSet.has(id));
    if (compareResult.value) {
      const leftExists = idSet.has(compareResult.value.left.id);
      const rightExists = idSet.has(compareResult.value.right.id);
      if (!leftExists || !rightExists) {
        compareResult.value = null;
      }
    }
  },
  { deep: false },
);

onMounted(() => {
  createEditor();
  loadRecords();
  setPageShortcuts([
    { keys: "Ctrl/Cmd + S", action: "保存" },
    { keys: "Ctrl/Cmd + Shift + F", action: "搜索" },
    { keys: "Alt + C", action: "压缩复制" },
    { keys: "Delete", action: "删除当前记录" },
    { keys: "Esc", action: "退出对比/删除模式" },
  ]);
  window.addEventListener("keydown", handleKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleKeydown);
  clearPageShortcuts();
  if (searchTimer) {
    window.clearTimeout(searchTimer);
  }
  if (errorFlashTimer) {
    window.clearTimeout(errorFlashTimer);
  }
  if (errorLineTimer) {
    window.clearTimeout(errorLineTimer);
  }
  destroyEditor();
});
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  width: 100%;
  padding: 16px 24px;
  box-sizing: border-box;
}

.card {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 14px;
}

.workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr);
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel-header {
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
}

.panel-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.panel-actions.editor-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.header-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  color: #475569;
  cursor: pointer;
  transition: all 0.16s ease;
}

.header-icon-btn:hover:not(:disabled) {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #2563eb;
}

.header-icon-btn.active {
  border-color: #3b82f6;
  color: #2563eb;
  background: #dbeafe;
}

.header-icon-btn.active.danger {
  border-color: #ef4444;
  color: #dc2626;
  background: #fee2e2;
}

.header-icon-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.header-icon-btn svg {
  width: 15px;
  height: 15px;
}

.action-divider {
  width: 1px;
  height: 22px;
  background: var(--border-color);
  margin: 0 4px;
}

.search-meta {
  font-size: 11px;
  color: var(--text-muted);
}

.panel-body {
  padding: 16px;
  color: var(--text-muted);
  font-size: 13px;
}

.panel-body.list {
  padding: 8px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.panel-body-host {
  padding: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}

.compare-only-host {
  overflow: hidden;
}

.editor-body {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}

.editor-container {
  flex: 1;
  min-height: 0;
}

.editor-container :deep(.cm-editor) {
  height: 100%;
}

.editor-container :deep(.cm-scroller) {
  overflow: auto;
}

.editor-container :deep(.cm-lintRange) {
  text-decoration: underline wavy #ef4444;
}

.editor-container :deep(.cm-error-line) {
  background: rgba(254, 202, 202, 0.35);
}

.editor-container :deep(.cm-activeLine) {
  background: rgba(59, 130, 246, 0.08);
}

.editor-container :deep(.cm-error-flash) {
  background: rgba(239, 68, 68, 0.35);
  border-radius: 2px;
  animation: errorFlash 0.6s ease-in-out 0s 3;
}

@keyframes errorFlash {
  0% {
    background: rgba(239, 68, 68, 0.6);
  }
  100% {
    background: rgba(239, 68, 68, 0);
  }
}

.editor-container :deep(.cm-search-match) {
  background: #fde68a;
  color: #111827;
  border-radius: 4px;
  padding: 0 2px;
}

.editor-container :deep(.cm-foldGutter) {
  width: 18px;
}

.editor-container :deep(.cm-foldGutter .cm-gutterElement) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.editor-container :deep(.cm-foldGutter .cm-foldMarker) {
  font-size: 14px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.editor-container :deep(.cm-json-parse-btn) {
  margin-left: 6px;
  border: 1px solid #c7d2fe;
  background: #eef2ff;
  color: #4338ca;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
  cursor: pointer;
}

.editor-container :deep(.cm-json-parse-btn:hover) {
  background: #e0e7ff;
}

.panel-body.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  text-align: center;
  flex: 1;
}

.history-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-item {
  border: 1px solid transparent;
  border-radius: 10px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  background: #f8fafc;
  transition:
    border 0.15s ease,
    background 0.15s ease;
  position: relative;
}

.history-item.selection-mode {
  padding-right: 34px;
}

.compare-pick {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
}

.compare-checkbox {
  width: 14px;
  height: 14px;
  margin: 0;
  accent-color: #3b82f6;
}

.history-item:hover {
  border-color: #bae6fd;
  background: #eef6ff;
}

.history-item.active {
  border-color: #60a5fa;
  background: #e8f1ff;
}

.history-item.selected {
  border-color: #3b82f6;
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.2);
}

.history-item.dragging {
  opacity: 0.5;
}

.history-item.dragover {
  border-color: #facc15;
  background: #fff7ed;
}

.history-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--text-main);
}

.name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.dirty-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  margin-left: 6px;
  vertical-align: middle;
}

.dirty-dot.saved {
  background: #22c55e;
}

.dirty-dot.draft {
  background: #ef4444;
}

.drag-hint {
  font-size: 11px;
  color: var(--text-muted);
}

.history-meta {
  font-size: 11px;
  color: var(--text-muted);
}

.compare-action-bar {
  border-top: 1px solid var(--border-color);
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
}

.compare-count {
  margin-right: auto;
  font-size: 12px;
  color: var(--text-muted);
}

.compare-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  cursor: pointer;
  transition: all 0.16s ease;
}

.compare-action-btn.primary {
  color: #2563eb;
  border-color: #93c5fd;
  background: #eff6ff;
}

.compare-action-btn:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.compare-action-btn.primary:hover:not(:disabled) {
  border-color: #60a5fa;
  background: #dbeafe;
}

.compare-action-btn.primary.danger {
  color: #dc2626;
  border-color: #fca5a5;
  background: #fee2e2;
}

.compare-action-btn.primary.danger:hover:not(:disabled) {
  border-color: #f87171;
  background: #fecaca;
}

.compare-action-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.compare-action-btn svg {
  width: 15px;
  height: 15px;
}

.compare-body {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
  flex: 1;
}

.rename-input {
  flex: 1;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 4px 8px;
  font-size: 12px;
}

.input {
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: #fff;
  padding: 8px 10px;
  font-size: 12px;
  width: 160px;
  box-sizing: border-box;
}

.input.sm {
  width: 140px;
}

@media (max-width: 1024px) {
  .workspace {
    grid-template-columns: 1fr;
    grid-template-rows: auto;
  }

  .history {
    grid-row: auto;
  }
}

@media (max-width: 768px) {
  .content {
    padding: 12px;
  }

  .input,
  .input.sm {
    width: 100%;
  }

  .drag-hint {
    display: none;
  }
}
</style>
