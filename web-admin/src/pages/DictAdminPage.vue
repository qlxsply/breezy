<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card
          v-if="queryPanelVisible"
          class="admin-panel admin-filter-card"
          shadow="never"
        >
          <bz-form
            class="admin-filter-form"
            :class="{ 'is-collapsed': queryCollapsed }"
            @submit.prevent="applyFilters"
          >
            <bz-form-item class="admin-filter-item">
              <div class="admin-filter-field">
                <div class="admin-filter-label">关键词</div>
                <div class="admin-filter-control">
                  <bz-input
                    v-model="keywordDraft"
                    placeholder="搜索字典编码或名称"
                    clearable
                    @keyup.enter="applyFilters"
                  />
                </div>
              </div>
            </bz-form-item>

            <div class="admin-filter-actions">
              <bz-button
                class="admin-filter-secondary"
                @click="resetFilters"
              >
                重置
              </bz-button>
              <bz-button
                class="admin-filter-primary"
                type="primary"
                native-type="submit"
              >
                搜索
              </bz-button>
              <div
                class="admin-filter-toggle-placeholder"
                aria-hidden="true"
              ></div>
            </div>
          </bz-form>
        </bz-card>

        <bz-card
          class="admin-panel admin-table-card"
          shadow="never"
        >
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">数据字典</div>
              <div class="admin-table-tools">
                <bz-button
                  v-if="canEdit"
                  class="admin-toolbar-primary"
                  type="primary"
                  @click="openCreateTypeDrawer"
                >
                  新增字典
                </bz-button>
                <button
                  class="admin-vben-circle-button"
                  :class="{ 'is-active': queryPanelVisible }"
                  type="button"
                  :title="queryPanelVisible ? '关闭搜索框' : '打开搜索框'"
                  @click="queryPanelVisible = !queryPanelVisible"
                >
                  <i
                    class="admin-vben-circle-button__icon admin-vben-circle-button__icon--search"
                    aria-hidden="true"
                  ></i>
                </button>
                <button
                  class="admin-vben-circle-button"
                  type="button"
                  title="刷新列表"
                  @click="reloadTypes"
                >
                  <i
                    class="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh"
                    aria-hidden="true"
                  ></i>
                </button>
              </div>
            </div>
          </template>

          <div class="admin-table-surface">
            <bz-table
              v-loading="typeLoading"
              :data="dictTypes"
              row-key="id"
              empty-text="暂无字典"
              size="small"
            >
              <bz-table-column
                prop="code"
                label="字典编码"
                min-width="180"
                show-overflow-tooltip
              />
              <bz-table-column
                prop="name"
                label="名称"
                min-width="150"
                show-overflow-tooltip
              />
              <bz-table-column
                prop="description"
                label="描述"
                min-width="220"
                show-overflow-tooltip
              >
                <template #default="scope">
                  {{ scope.row.description || "-" }}
                </template>
              </bz-table-column>
              <bz-table-column
                prop="enumClass"
                label="枚举类"
                min-width="220"
                show-overflow-tooltip
              >
                <template #default="scope">
                  <span class="mono subdued">{{ scope.row.enumClass || "-" }}</span>
                </template>
              </bz-table-column>
              <bz-table-column
                label="值类型"
                width="110"
              >
                <template #default="scope">
                  <bz-tag size="small">{{ resolveValueTypeLabel(scope.row.valueType) }}</bz-tag>
                </template>
              </bz-table-column>
              <bz-table-column
                label="结构类型"
                width="110"
              >
                <template #default="scope">
                  <bz-tag size="small">{{
                    resolveStructureTypeLabel(scope.row.structureType)
                  }}</bz-tag>
                </template>
              </bz-table-column>
              <bz-table-column
                label="来源"
                width="100"
              >
                <template #default="scope">
                  <bz-tag
                    size="small"
                    :type="scope.row.sourceType === 'BUILTIN' ? 'info' : 'success'"
                  >
                    {{ resolveSourceTypeLabel(scope.row.sourceType) }}
                  </bz-tag>
                </template>
              </bz-table-column>
              <bz-table-column
                label="状态"
                width="100"
              >
                <template #default="scope">
                  <bz-tag
                    size="small"
                    :type="scope.row.enabled ? 'success' : 'warning'"
                  >
                    {{ scope.row.enabled ? "启用" : "停用" }}
                  </bz-tag>
                </template>
              </bz-table-column>
              <bz-table-column
                label="操作"
                width="150"
                fixed="right"
              >
                <template #default="scope">
                  <AdminActionBar
                    :actions="getRowActions(scope.row)"
                    :more-actions="getRowMoreActions(scope.row)"
                  />
                </template>
              </bz-table-column>
            </bz-table>
          </div>

          <div
            v-if="total > 0"
            class="dict-pagination-bar"
          >
            <div class="dict-pagination-summary">共 {{ total }} 条记录</div>
            <div class="dict-pagination-right">
              <label class="dict-page-size">
                <select
                  class="dict-page-size__select"
                  :value="pageSize"
                  @change="handlePageSizeSelect"
                >
                  <option
                    v-for="size in pageSizeOptions"
                    :key="size"
                    :value="size"
                  >
                    {{ size }}条/页
                  </option>
                </select>
              </label>
              <div class="dict-page-list">
                <button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isFirstPage"
                  @click="goToPage(1)"
                >
                  <span aria-hidden="true">|&lt;</span>
                </button>
                <button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isFirstPage"
                  @click="goToPage(pageNo - 1)"
                >
                  <span aria-hidden="true">&lt;</span>
                </button>
                <template
                  v-for="(token, tokenIndex) in pageTokens"
                  :key="`${String(token)}-${tokenIndex}`"
                >
                  <button
                    v-if="typeof token === 'number'"
                    class="dict-page-btn"
                    :class="{ 'is-active': token === pageNo }"
                    type="button"
                    @click="goToPage(token)"
                  >
                    {{ token }}
                  </button>
                  <span
                    v-else
                    class="dict-page-ellipsis"
                  >
                    ...
                  </span>
                </template>
                <button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isLastPage"
                  @click="goToPage(pageNo + 1)"
                >
                  <span aria-hidden="true">&gt;</span>
                </button>
                <button
                  class="dict-page-btn dict-page-btn--icon"
                  type="button"
                  :disabled="isLastPage"
                  @click="goToPage(totalPages)"
                >
                  <span aria-hidden="true">&gt;|</span>
                </button>
              </div>
            </div>
          </div>
        </bz-card>
      </div>
    </div>

    <AdminEntityDrawer
      :open="typeDrawer.visible"
      :loading="drawerLoading"
      :title="drawerTitle"
      :width="drawerWidth"
      @close="closeTypeDrawer"
    >
      <div class="dict-drawer-layout">
        <section class="dict-drawer-section">
          <div class="dict-drawer-section__head">
            <div class="dict-drawer-section__title">基础信息</div>
          </div>

          <div
            v-if="isDetailMode"
            class="dict-detail-grid"
          >
            <div class="dict-detail-field">
              <span class="dict-detail-field__label">字典编码</span>
              <span class="dict-detail-field__value mono">{{ typeForm.code || "-" }}</span>
            </div>
            <div class="dict-detail-field">
              <span class="dict-detail-field__label">名称</span>
              <span class="dict-detail-field__value">{{ typeForm.name || "-" }}</span>
            </div>
            <div class="dict-detail-field">
              <span class="dict-detail-field__label">值类型</span>
              <span class="dict-detail-field__value">{{
                resolveValueTypeLabel(typeForm.valueType)
              }}</span>
            </div>
            <div class="dict-detail-field">
              <span class="dict-detail-field__label">结构类型</span>
              <span class="dict-detail-field__value">{{
                resolveStructureTypeLabel(typeForm.structureType)
              }}</span>
            </div>
            <div class="dict-detail-field">
              <span class="dict-detail-field__label">来源</span>
              <span class="dict-detail-field__value">
                {{ typeForm.sourceType ? sourceTypeLabelMap[typeForm.sourceType] : "-" }}
              </span>
            </div>
            <div class="dict-detail-field">
              <span class="dict-detail-field__label">状态</span>
              <span class="dict-detail-field__value">{{ typeForm.enabled ? "启用" : "停用" }}</span>
            </div>
            <div class="dict-detail-field dict-detail-field--wide">
              <span class="dict-detail-field__label">枚举类</span>
              <span class="dict-detail-field__value mono">{{ typeForm.enumClass || "-" }}</span>
            </div>
            <div class="dict-detail-field dict-detail-field--wide">
              <span class="dict-detail-field__label">描述</span>
              <span class="dict-detail-field__value">{{ typeForm.description || "-" }}</span>
            </div>
          </div>

          <bz-form
            v-else
            label-width="88px"
            @submit.prevent
          >
            <div class="dict-form-grid">
              <bz-form-item label="编码">
                <bz-text-field
                  v-model="typeForm.code"
                  :maxlength="DICT_TYPE_CODE_MAX"
                  :disabled="isEditMode"
                />
              </bz-form-item>
              <bz-form-item label="名称">
                <bz-text-field
                  v-model="typeForm.name"
                  :maxlength="DICT_TYPE_NAME_MAX"
                />
              </bz-form-item>
              <bz-form-item label="值类型">
                <bz-select v-model="typeForm.valueType">
                  <bz-option
                    v-for="option in valueTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </bz-select>
              </bz-form-item>
              <bz-form-item label="结构类型">
                <bz-select v-model="typeForm.structureType">
                  <bz-option
                    v-for="option in structureTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </bz-select>
              </bz-form-item>
              <bz-form-item label="来源">
                <bz-input
                  :model-value="
                    typeForm.sourceType ? sourceTypeLabelMap[typeForm.sourceType] : '自定义'
                  "
                  disabled
                />
              </bz-form-item>
              <bz-form-item label="启用状态">
                <bz-switch v-model="typeForm.enabled" />
              </bz-form-item>
              <bz-form-item
                label="枚举类"
                class="dict-form-item--wide"
              >
                <bz-text-field
                  v-model="typeForm.enumClass"
                  :maxlength="DICT_TYPE_DESCRIPTION_MAX"
                />
              </bz-form-item>
              <bz-form-item
                label="描述"
                class="dict-form-item--wide"
              >
                <bz-text-field
                  v-model="typeForm.description"
                  type="textarea"
                  :rows="3"
                  :maxlength="DICT_TYPE_DESCRIPTION_MAX"
                  show-counter
                />
              </bz-form-item>
            </div>
          </bz-form>
        </section>

        <section
          v-if="typeForm.code"
          class="dict-drawer-section"
        >
          <div class="dict-drawer-section__head">
            <div>
              <div class="dict-drawer-section__title">字典项</div>
            </div>

            <div
              v-if="isEditableMode"
              class="dict-drawer-section__actions"
            >
              <bz-button @click="openCreateItemEditor()">新增</bz-button>
            </div>
          </div>

          <div
            v-if="isEditableMode && itemEditorVisible"
            class="dict-item-editor-card"
          >
            <div class="dict-item-editor-card__head">
              <div class="dict-drawer-section__title">{{ itemEditorTitle }}</div>
              <div class="dict-drawer-section__actions">
                <bz-button @click="closeItemEditor">取消</bz-button>
                <bz-button
                  type="primary"
                  @click="saveItem"
                >
                  保存
                </bz-button>
              </div>
            </div>

            <bz-form
              label-width="84px"
              @submit.prevent
            >
              <div class="dict-form-grid dict-form-grid--compact">
                <bz-form-item label="项编码">
                  <bz-text-field
                    v-model="itemForm.itemCode"
                    :maxlength="DICT_ITEM_CODE_MAX"
                    :disabled="itemEditorMode === 'edit'"
                  />
                </bz-form-item>
                <bz-form-item label="显示名称">
                  <bz-text-field
                    v-model="itemForm.itemLabel"
                    :maxlength="DICT_ITEM_LABEL_MAX"
                  />
                </bz-form-item>
                <bz-form-item label="实际值">
                  <bz-select
                    v-if="typeForm.valueType === 'BOOLEAN'"
                    v-model="itemForm.itemValue"
                  >
                    <bz-option
                      label="true"
                      value="true"
                    />
                    <bz-option
                      label="false"
                      value="false"
                    />
                  </bz-select>
                  <bz-text-field
                    v-else
                    v-model="itemForm.itemValue"
                    :type="typeForm.valueType === 'NUMBER' ? 'number' : 'text'"
                    :maxlength="DICT_ITEM_VALUE_MAX"
                  />
                  <div
                    v-if="itemValueTypeError"
                    class="dict-field-error"
                  >
                    {{ itemValueTypeError }}
                  </div>
                </bz-form-item>
                <bz-form-item label="排序号">
                  <bz-input-number
                    v-model="itemForm.sortNo"
                    :min="0"
                  />
                </bz-form-item>
              </div>

              <div
                v-if="isTreeType"
                class="dict-form-grid dict-form-grid--compact"
              >
                <bz-form-item label="父节点">
                  <bz-select
                    v-model="itemForm.parentItemId"
                    clearable
                  >
                    <bz-option
                      v-for="option in parentOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </bz-select>
                </bz-form-item>
              </div>

              <div class="dict-switch-grid">
                <bz-form-item label="默认项">
                  <bz-switch v-model="itemForm.defaultItem" />
                </bz-form-item>
                <bz-form-item label="启用状态">
                  <bz-switch
                    v-model="itemForm.enabled"
                    :disabled="itemForm.defaultItem"
                  />
                </bz-form-item>
              </div>

              <div class="dict-form-grid dict-form-grid--compact">
                <bz-form-item label="标签类型">
                  <bz-select
                    v-model="itemForm.tagType"
                    clearable
                  >
                    <bz-option
                      v-for="option in tagTypeOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </bz-select>
                </bz-form-item>
                <bz-form-item label="标签颜色">
                  <bz-select
                    v-model="itemForm.tagColor"
                    clearable
                  >
                    <bz-option
                      v-for="option in tagColorOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </bz-select>
                </bz-form-item>
              </div>

              <div
                v-if="itemForm.tagType || itemForm.tagColor"
                class="dict-tag-preview"
              >
                <span class="dict-tag-preview-label">标签预览</span>
                <span
                  class="dict-custom-tag"
                  :style="buildTagPreviewStyle(itemForm.tagColor, itemForm.tagType)"
                >
                  {{ resolveTagTypeLabel(itemForm.tagType) }}
                </span>
              </div>

              <div class="dict-textarea-stack">
                <bz-form-item label="扩展JSON">
                  <bz-text-field
                    v-model="itemForm.extraJson"
                    type="textarea"
                    :rows="2"
                    :maxlength="DICT_ITEM_EXTRA_JSON_MAX"
                    show-counter
                  />
                </bz-form-item>
                <bz-form-item label="说明">
                  <bz-text-field
                    v-model="itemForm.description"
                    type="textarea"
                    :rows="2"
                    :maxlength="DICT_ITEM_DESCRIPTION_MAX"
                    show-counter
                  />
                </bz-form-item>
              </div>
            </bz-form>
          </div>

          <div
            v-loading="itemLoading"
            class="dict-item-list-wrap"
          >
            <bz-tree
              v-if="isTreeType && treeItems.length > 0"
              :data="treeItems"
              node-key="id"
              default-expand-all
            >
              <template #default="{ data }">
                <div class="dict-tree-node">
                  <div class="dict-tree-node__main dict-item-line">
                    <span class="dict-item-code">{{ data.itemCode }}</span>
                    <span class="dict-item-label">{{ data.itemLabel }}</span>
                    <span class="dict-item-value">{{ data.itemValue }}</span>
                    <div class="dict-item-label-tags">
                      <bz-tag
                        v-if="data.defaultItem"
                        size="small"
                        type="success"
                      >
                        默认
                      </bz-tag>
                      <span
                        v-else
                        class="dict-item-default-placeholder"
                        aria-hidden="true"
                      ></span>
                      <bz-tag
                        size="small"
                        :type="data.enabled ? 'success' : 'warning'"
                      >
                        {{ data.enabled ? "启用" : "停用" }}
                      </bz-tag>
                      <span
                        v-if="data.tagType || data.tagColor"
                        class="dict-custom-tag"
                        :style="buildTagPreviewStyle(data.tagColor, data.tagType)"
                      >
                        {{ resolveTagTypeLabel(data.tagType) }}
                      </span>
                    </div>
                  </div>

                  <div
                    v-if="isEditableMode"
                    class="dict-tree-node__actions"
                  >
                    <button
                      class="dict-inline-link is-edit"
                      type="button"
                      @click.stop="openEditItemEditor(data.id)"
                    >
                      编辑
                    </button>
                    <button
                      class="dict-inline-link"
                      type="button"
                      @click.stop="openCreateItemEditor(data.id)"
                    >
                      新增子项
                    </button>
                    <button
                      class="dict-inline-link is-delete"
                      type="button"
                      @click.stop="removeItem(data.id)"
                    >
                      删除
                    </button>
                  </div>
                </div>
              </template>
            </bz-tree>

            <template v-else-if="!isTreeType">
              <button
                v-for="item in currentItems"
                :key="item.id"
                class="dict-item-row"
                :class="{
                  'is-dragging': dragState.draggingId === item.id,
                  'is-drop-before':
                    dragState.overId === item.id && dragState.overPosition === 'before',
                  'is-drop-after':
                    dragState.overId === item.id && dragState.overPosition === 'after',
                }"
                type="button"
                :draggable="isEditableMode"
                @dragstart="handleItemDragStart(item.id)"
                @dragover.prevent="handleItemDragOver($event, item.id)"
                @drop.prevent="handleItemDrop(item.id)"
                @dragend="clearItemDragState"
              >
                <div class="dict-item-row-main dict-item-line">
                  <span class="dict-item-code">{{ item.itemCode }}</span>
                  <span class="dict-item-label">{{ item.itemLabel }}</span>
                  <span class="dict-item-value">{{ item.itemValue }}</span>
                  <div class="dict-item-label-tags">
                    <bz-tag
                      v-if="item.defaultItem"
                      size="small"
                      type="success"
                    >
                      默认
                    </bz-tag>
                    <span
                      v-else
                      class="dict-item-default-placeholder"
                      aria-hidden="true"
                    ></span>
                    <bz-tag
                      size="small"
                      :type="item.enabled ? 'success' : 'warning'"
                    >
                      {{ item.enabled ? "启用" : "停用" }}
                    </bz-tag>
                    <span
                      v-if="item.tagType || item.tagColor"
                      class="dict-custom-tag"
                      :style="buildTagPreviewStyle(item.tagColor, item.tagType)"
                    >
                      {{ resolveTagTypeLabel(item.tagType) }}
                    </span>
                  </div>
                </div>

                <div
                  v-if="isEditableMode"
                  class="dict-item-row-actions"
                >
                  <button
                    class="dict-inline-link is-edit"
                    type="button"
                    @click.stop="openEditItemEditor(item.id)"
                  >
                    编辑
                  </button>
                  <button
                    class="dict-inline-link is-delete"
                    type="button"
                    @click.stop="removeItem(item.id)"
                  >
                    删除
                  </button>
                </div>
              </button>
            </template>

            <bz-empty
              v-if="!itemLoading && currentItems.length === 0"
              description="暂无字典项"
            />
          </div>
        </section>
      </div>

      <template #footer>
        <bz-button @click="closeTypeDrawer">{{ isDetailMode ? "关闭" : "取消" }}</bz-button>
        <bz-button
          v-if="!isDetailMode"
          type="primary"
          @click="saveType"
        >
          确定
        </bz-button>
      </template>
    </AdminEntityDrawer>
  </div>
</template>

<script setup lang="ts">
import type { CSSProperties } from "vue";
import { computed, onMounted, reactive, ref, watch } from "vue";

import {
  batchListDictOptions,
  createDictType,
  deleteDictType,
  getDictType,
  listDictItems,
  listDictTypes,
  updateDictType,
  updateDictTypeStatus,
} from "../api/dicts";
import AdminActionBar from "../components/admin/AdminActionBar.vue";
import AdminEntityDrawer from "../components/admin/AdminEntityDrawer.vue";
import { hasResourceCodeAccess } from "../registry/permissions.registry";
import type { AdminActionItem } from "../types/admin-action";
import type {
  DictItem,
  DictOption,
  DictSourceType,
  DictStructureType,
  DictTypeItem,
  DictValueType,
} from "../types/dict-admin";
import { bzConfirm } from "../utils/confirm";
import { message } from "../utils/message";

type OptionCode =
  | "DICT_VALUE_TYPE"
  | "DICT_STRUCTURE_TYPE"
  | "DICT_ITEM_TAG_TYPE"
  | "DICT_ITEM_TAG_COLOR";
type DragPosition = "before" | "after";
type TypeDrawerMode = "detail" | "edit" | "create";
type ItemEditorMode = "create" | "edit";

const DRAFT_ITEM_ID_PREFIX = "draft-item-";

interface TreeDictItem extends DictItem {
  children: TreeDictItem[];
}

const OPTION_CODES: OptionCode[] = [
  "DICT_VALUE_TYPE",
  "DICT_STRUCTURE_TYPE",
  "DICT_ITEM_TAG_TYPE",
  "DICT_ITEM_TAG_COLOR",
];

const FALLBACK_OPTIONS: Record<OptionCode, DictOption[]> = {
  DICT_VALUE_TYPE: [
    { label: "文本", value: "STRING" },
    { label: "数字", value: "NUMBER" },
    { label: "布尔", value: "BOOLEAN" },
  ],
  DICT_STRUCTURE_TYPE: [
    { label: "平铺结构", value: "FLAT" },
    { label: "树形结构", value: "TREE" },
  ],
  DICT_ITEM_TAG_TYPE: [
    { label: "信息", value: "info" },
    { label: "成功", value: "success" },
    { label: "警告", value: "warning" },
    { label: "危险", value: "danger" },
  ],
  DICT_ITEM_TAG_COLOR: [
    { label: "主题蓝", value: "#3B82F6" },
    { label: "成功绿", value: "#10B981" },
    { label: "警示橙", value: "#F59E0B" },
    { label: "危险红", value: "#EF4444" },
    { label: "石板灰", value: "#64748B" },
  ],
};

const sourceTypeLabelMap: Record<DictSourceType, string> = {
  BUILTIN: "内置",
  CUSTOM: "自定义",
};

const DICT_TYPE_CODE_MAX = 128;
const DICT_TYPE_NAME_MAX = 128;
const DICT_TYPE_DESCRIPTION_MAX = 255;
const DICT_ITEM_CODE_MAX = 128;
const DICT_ITEM_LABEL_MAX = 128;
const DICT_ITEM_VALUE_MAX = 512;
const DICT_ITEM_DESCRIPTION_MAX = 255;
const DICT_ITEM_EXTRA_JSON_MAX = 4000;

const typeLoading = ref(false);
const drawerLoading = ref(false);
const itemLoading = ref(false);

const queryPanelVisible = ref(false);
const queryCollapsed = ref(true);
const keywordDraft = ref("");
const appliedKeyword = ref("");
const pageNo = ref(1);
const pageSize = ref(10);
const total = ref(0);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;

const dictTypes = ref<DictTypeItem[]>([]);
const items = ref<DictItem[]>([]);
const itemDraftRows = ref<DictItem[]>([]);
const optionSets = ref<Record<OptionCode, DictOption[]>>({ ...FALLBACK_OPTIONS });
const currentTypeId = ref("");

const typeDrawer = reactive({
  visible: false,
  mode: "detail" as TypeDrawerMode,
});

const itemEditorVisible = ref(false);
const itemEditorMode = ref<ItemEditorMode>("create");

const dragState = reactive({
  draggingId: "",
  overId: "",
  overPosition: "before" as DragPosition,
  sorting: false,
});

const canView = computed(() => hasResourceCodeAccess("dict-manage-view"));
const canEdit = computed(() => hasResourceCodeAccess("dict-manage-edit"));
const isDetailMode = computed(() => typeDrawer.mode === "detail");
const isEditMode = computed(() => typeDrawer.mode === "edit");
const isEditableMode = computed(
  () => !isDetailMode.value && canEdit.value && typeForm.sourceType !== "BUILTIN",
);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));
const isFirstPage = computed(() => pageNo.value <= 1);
const isLastPage = computed(() => pageNo.value >= totalPages.value);
const pageTokens = computed<Array<number | "ellipsis">>(() => {
  const totalCount = totalPages.value;
  const current = Math.min(Math.max(pageNo.value, 1), totalCount);
  if (totalCount <= 7) {
    return Array.from({ length: totalCount }, (_, index) => index + 1);
  }
  if (current <= 4) {
    return [1, 2, 3, 4, 5, "ellipsis", totalCount];
  }
  if (current >= totalCount - 3) {
    return [
      1,
      "ellipsis",
      totalCount - 4,
      totalCount - 3,
      totalCount - 2,
      totalCount - 1,
      totalCount,
    ];
  }
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", totalCount];
});

const valueTypeOptions = computed(
  () => optionSets.value.DICT_VALUE_TYPE || FALLBACK_OPTIONS.DICT_VALUE_TYPE,
);
const structureTypeOptions = computed(
  () => optionSets.value.DICT_STRUCTURE_TYPE || FALLBACK_OPTIONS.DICT_STRUCTURE_TYPE,
);
const tagTypeOptions = computed(
  () => optionSets.value.DICT_ITEM_TAG_TYPE || FALLBACK_OPTIONS.DICT_ITEM_TAG_TYPE,
);
const tagColorOptions = computed(
  () => optionSets.value.DICT_ITEM_TAG_COLOR || FALLBACK_OPTIONS.DICT_ITEM_TAG_COLOR,
);

const currentItems = computed(() => (isEditableMode.value ? itemDraftRows.value : items.value));
const drawerTitle = computed(() => {
  if (typeDrawer.mode === "create") return "新增字典";
  if (typeDrawer.mode === "edit") return "编辑字典";
  return `字典详情`;
});
const drawerWidth = computed(() => (isDetailMode.value ? "960px" : "1080px"));
const isTreeType = computed(() => typeForm.structureType === "TREE");
const itemEditorTitle = computed(() =>
  itemEditorMode.value === "create" ? "新增字典项" : "编辑字典项",
);
const itemValueTypeError = computed(() => {
  if (typeForm.valueType !== "NUMBER") return "";
  const value = safeTrim(itemForm.itemValue);
  if (!value) return "";
  return isValidNumberLiteral(value) ? "" : "请输入合法数字，支持负数和小数";
});

const treeItems = computed<TreeDictItem[]>(() => {
  const source = currentItems.value;
  const nodeMap = new Map<string, TreeDictItem>();
  const roots: TreeDictItem[] = [];
  for (const item of source) {
    nodeMap.set(item.id, { ...item, children: [] });
  }
  for (const item of source) {
    const node = nodeMap.get(item.id);
    if (!node) continue;
    if (item.parentItemId && nodeMap.has(item.parentItemId)) {
      nodeMap.get(item.parentItemId)?.children.push(node);
    } else {
      roots.push(node);
    }
  }
  const sortNodes = (list: TreeDictItem[]) => {
    list.sort((a, b) => a.sortNo - b.sortNo || a.itemLabel.localeCompare(b.itemLabel));
    for (const node of list) {
      sortNodes(node.children);
    }
  };
  sortNodes(roots);
  return roots;
});

const parentOptions = computed<DictOption[]>(() => {
  const excluded = new Set<string>();
  if (itemForm.id) {
    excluded.add(itemForm.id);
    collectDescendantIds(itemForm.id, excluded);
  }
  return itemDraftRows.value
    .filter((item) => !excluded.has(item.id))
    .map((item) => ({
      label: buildParentOptionLabel(item),
      value: item.id,
    }));
});

const typeForm = reactive({
  id: "",
  code: "",
  name: "",
  description: "",
  enumClass: "",
  valueType: "STRING" as DictValueType,
  structureType: "FLAT" as DictStructureType,
  sourceType: "CUSTOM" as DictSourceType,
  enabled: true,
});

const itemForm = reactive({
  id: "",
  parentItemId: "",
  itemCode: "",
  itemLabel: "",
  itemValue: "",
  sortNo: 0,
  enabled: true,
  defaultItem: false,
  tagColor: "",
  tagType: "",
  extraJson: "",
  description: "",
});

watch(
  () => itemForm.defaultItem,
  (defaultItem) => {
    if (defaultItem) {
      itemForm.enabled = true;
      clearOtherDefaultItems(itemForm.id);
    }
  },
);

watch(
  () => typeForm.valueType,
  (valueType) => {
    if (valueType === "BOOLEAN") {
      itemForm.itemValue = itemForm.itemValue === "false" ? "false" : "true";
      return;
    }
    if (valueType === "NUMBER") {
      itemForm.itemValue = sanitizeNumberInput(itemForm.itemValue);
    }
  },
);

watch(
  () => itemForm.itemValue,
  (value) => {
    if (typeForm.valueType !== "NUMBER") return;
    const nextValue = sanitizeNumberInput(value);
    if (nextValue !== value) {
      itemForm.itemValue = nextValue;
    }
  },
);

onMounted(() => {
  if (canView.value || canEdit.value) {
    void initializePage();
  }
});

async function initializePage() {
  await Promise.all([loadOptionSets(), reloadTypes()]);
}

async function loadOptionSets() {
  try {
    const result = await batchListDictOptions([...OPTION_CODES]);
    optionSets.value = {
      DICT_VALUE_TYPE: toOptions(result.DICT_VALUE_TYPE, FALLBACK_OPTIONS.DICT_VALUE_TYPE),
      DICT_STRUCTURE_TYPE: toOptions(
        result.DICT_STRUCTURE_TYPE,
        FALLBACK_OPTIONS.DICT_STRUCTURE_TYPE,
      ),
      DICT_ITEM_TAG_TYPE: toOptions(result.DICT_ITEM_TAG_TYPE, FALLBACK_OPTIONS.DICT_ITEM_TAG_TYPE),
      DICT_ITEM_TAG_COLOR: toOptions(
        result.DICT_ITEM_TAG_COLOR,
        FALLBACK_OPTIONS.DICT_ITEM_TAG_COLOR,
      ),
    };
  } catch {
    optionSets.value = { ...FALLBACK_OPTIONS };
  }
}

async function reloadTypes() {
  typeLoading.value = true;
  try {
    const requestedPageNo = pageNo.value;
    let result = await listDictTypes({
      keyword: safeTrim(appliedKeyword.value) || undefined,
      page: {
        pageNo: requestedPageNo,
        pageSize: pageSize.value,
      },
    });

    if (result.totalPages > 0 && requestedPageNo > result.totalPages) {
      pageNo.value = result.totalPages;
      result = await listDictTypes({
        keyword: safeTrim(appliedKeyword.value) || undefined,
        page: {
          pageNo: pageNo.value,
          pageSize: pageSize.value,
        },
      });
    }

    total.value = result.totalElements;
    pageNo.value = result.pageNo;
    pageSize.value = result.pageSize;
    dictTypes.value = result.elements;
  } finally {
    typeLoading.value = false;
  }
}

function applyFilters() {
  appliedKeyword.value = keywordDraft.value;
  pageNo.value = 1;
  void reloadTypes();
}

function resetFilters() {
  keywordDraft.value = "";
  applyFilters();
}

function goToPage(nextPage: number) {
  const target = Math.min(Math.max(nextPage, 1), totalPages.value);
  if (target === pageNo.value) {
    return;
  }
  pageNo.value = target;
  void reloadTypes();
}

function handlePageSizeSelect(event: Event) {
  const nextPageSize = Number((event.target as HTMLSelectElement).value);
  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === pageSize.value) {
    return;
  }
  pageSize.value = nextPageSize;
  pageNo.value = 1;
  void reloadTypes();
}

function getRowActions(row: DictTypeItem): AdminActionItem[] {
  return [
    {
      key: `detail-${row.id}`,
      label: "详情",
      tone: "detail",
      handler: () => openTypeDrawer("detail", row.id),
    },
    {
      key: `edit-${row.id}`,
      label: "编辑",
      tone: "edit",
      disabled: !canEdit.value || row.sourceType === "BUILTIN",
      handler: () => openTypeDrawer("edit", row.id),
    },
  ];
}

function getRowMoreActions(row: DictTypeItem): AdminActionItem[] {
  return [
    {
      key: `toggle-${row.id}`,
      label: row.enabled ? "停用" : "启用",
      tone: row.enabled ? "disable" : "enable",
      disabled: !canEdit.value || row.sourceType === "BUILTIN",
      handler: () => toggleTypeStatus(row),
    },
    {
      key: `delete-${row.id}`,
      label: "删除",
      tone: "delete",
      disabled: !canEdit.value || row.sourceType === "BUILTIN",
      handler: () => removeType(row),
    },
  ];
}

async function openTypeDrawer(mode: TypeDrawerMode, id: string) {
  typeDrawer.mode = mode;
  typeDrawer.visible = true;
  await loadTypeContext(id);
}

function openCreateTypeDrawer() {
  typeDrawer.mode = "create";
  typeDrawer.visible = true;
  drawerLoading.value = false;
  currentTypeId.value = "";
  resetTypeForm();
  items.value = [];
  itemDraftRows.value = [];
  closeItemEditor();
}

function closeTypeDrawer() {
  if (dragState.sorting) return;
  typeDrawer.visible = false;
  closeItemEditor();
  clearItemDragState();
}

async function loadTypeContext(id: string) {
  drawerLoading.value = true;
  itemLoading.value = true;
  try {
    const type = await getDictType(id);
    currentTypeId.value = type.id;
    applyTypeForm(type);
    const nextItems = await listDictItems(id);
    items.value = sortItems(nextItems);
    itemDraftRows.value = sortItems(nextItems);
    clearItemDragState();
  } finally {
    drawerLoading.value = false;
    itemLoading.value = false;
  }
}

function resetTypeForm() {
  Object.assign(typeForm, {
    id: "",
    code: "",
    name: "",
    description: "",
    enumClass: "",
    valueType: (valueTypeOptions.value[0]?.value || "STRING") as DictValueType,
    structureType: (structureTypeOptions.value[0]?.value || "FLAT") as DictStructureType,
    sourceType: "CUSTOM" as DictSourceType,
    enabled: true,
  });
}

function applyTypeForm(item: DictTypeItem) {
  Object.assign(typeForm, {
    id: item.id,
    code: item.code,
    name: item.name,
    description: item.description || "",
    enumClass: item.enumClass || "",
    valueType: item.valueType,
    structureType: item.structureType,
    sourceType: item.sourceType,
    enabled: item.enabled,
  });
}

async function saveType() {
  if (!canEdit.value) return;
  if (!safeTrim(typeForm.code) || !safeTrim(typeForm.name)) {
    message.error("请完整填写字典编码和名称");
    return;
  }
  const payload = {
    code: safeTrim(typeForm.code),
    name: safeTrim(typeForm.name),
    description: safeTrim(typeForm.description),
    enumClass: safeTrim(typeForm.enumClass),
    valueType: typeForm.valueType,
    structureType: typeForm.structureType,
    enabled: typeForm.enabled,
    items: itemDraftRows.value.map((item) => ({
      id: isDraftItemId(item.id) ? undefined : item.id,
      clientKey: item.id,
      parentClientKey: item.parentItemId || null,
      itemCode: item.itemCode,
      itemLabel: item.itemLabel,
      itemValue: item.itemValue,
      sortNo: item.sortNo,
      enabled: item.enabled,
      defaultItem: item.defaultItem,
      tagColor: item.tagColor || null,
      tagType: item.tagType || null,
      extraJson: item.extraJson || null,
      description: item.description || null,
    })),
  };
  if (typeForm.id) {
    await updateDictType(typeForm.id, {
      name: payload.name,
      description: payload.description,
      enumClass: payload.enumClass,
      valueType: payload.valueType,
      structureType: payload.structureType,
      enabled: payload.enabled,
      items: payload.items,
    });
  } else {
    await createDictType(payload);
  }

  message.success(typeForm.id ? "保存成功" : "创建成功");
  closeTypeDrawer();
  currentTypeId.value = "";
  await reloadTypes();
}

async function toggleTypeStatus(row: DictTypeItem) {
  const nextEnabled = !row.enabled;
  const confirmed = await bzConfirm({
    title: nextEnabled ? "启用字典" : "停用字典",
    message: `确认${nextEnabled ? "启用" : "停用"}字典 ${row.name} (${row.code})？`,
    confirmText: nextEnabled ? "启用" : "停用",
    cancelText: "取消",
    danger: !nextEnabled,
  });
  if (!confirmed) return;
  await updateDictTypeStatus(row.id, nextEnabled);
  message.success(nextEnabled ? "字典已启用" : "字典已停用");
  await reloadTypes();
  if (currentTypeId.value === row.id && typeDrawer.visible) {
    await loadTypeContext(row.id);
  }
}

async function removeType(row: DictTypeItem) {
  if (!canEdit.value) return;
  const confirmed = await bzConfirm({
    title: "删除字典",
    message: `确认删除字典 ${row.name} (${row.code})？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) return;
  await deleteDictType(row.id);
  message.success("字典已删除");
  if (currentTypeId.value === row.id) {
    closeTypeDrawer();
    currentTypeId.value = "";
  }
  await reloadTypes();
}

function openCreateItemEditor(parentItemId?: string) {
  if (!isEditableMode.value) return;
  itemEditorVisible.value = true;
  itemEditorMode.value = "create";
  const normalizedParent = parentItemId || "";
  Object.assign(itemForm, {
    id: "",
    parentItemId: normalizedParent,
    itemCode: "",
    itemLabel: "",
    itemValue: typeForm.valueType === "BOOLEAN" ? "true" : "",
    sortNo: nextSortNo(normalizedParent || null),
    enabled: true,
    defaultItem: false,
    tagColor: "",
    tagType: "",
    extraJson: "",
    description: "",
  });
  if (itemForm.defaultItem) {
    itemForm.enabled = true;
  }
}

function openEditItemEditor(itemId: string) {
  if (!isEditableMode.value) return;
  const target = itemDraftRows.value.find((item) => item.id === itemId);
  if (!target) return;
  itemEditorVisible.value = true;
  itemEditorMode.value = "edit";
  Object.assign(itemForm, {
    id: target.id,
    parentItemId: target.parentItemId || "",
    itemCode: target.itemCode,
    itemLabel: target.itemLabel,
    itemValue: target.itemValue,
    sortNo: target.sortNo,
    enabled: target.enabled,
    defaultItem: target.defaultItem,
    tagColor: target.tagColor || "",
    tagType: target.tagType || "",
    extraJson: target.extraJson || "",
    description: target.description || "",
  });
  if (itemForm.defaultItem) {
    itemForm.enabled = true;
  }
}

function closeItemEditor() {
  itemEditorVisible.value = false;
}

function clearOtherDefaultItems(currentItemId: string) {
  itemDraftRows.value = itemDraftRows.value.map((item) => {
    if (currentItemId && item.id === currentItemId) return item;
    return item.defaultItem ? { ...item, defaultItem: false } : item;
  });
}

async function saveItem() {
  if (!isEditableMode.value) return;
  if (
    !safeTrim(itemForm.itemCode) ||
    !safeTrim(itemForm.itemLabel) ||
    !safeTrim(itemForm.itemValue)
  ) {
    message.error("请完整填写字典项编码、名称和值");
    return;
  }
  if (itemValueTypeError.value) {
    message.error(itemValueTypeError.value);
    return;
  }
  const parentItemId = isTreeType.value ? safeTrim(itemForm.parentItemId) || null : null;
  const payload: DictItem = {
    id: itemForm.id || newDraftItemId(),
    dictTypeId: currentTypeId.value,
    parentItemId,
    itemCode: safeTrim(itemForm.itemCode),
    itemLabel: safeTrim(itemForm.itemLabel),
    itemValue: safeTrim(itemForm.itemValue),
    sortNo: itemForm.sortNo,
    enabled: itemForm.defaultItem ? true : itemForm.enabled,
    defaultItem: itemForm.defaultItem,
    tagColor: safeTrim(itemForm.tagColor),
    tagType: safeTrim(itemForm.tagType),
    extraJson: safeTrim(itemForm.extraJson),
    description: safeTrim(itemForm.description),
  };

  const nextRows = [...itemDraftRows.value];
  const existingIndex = nextRows.findIndex((item) => item.id === payload.id);
  if (existingIndex >= 0) {
    nextRows.splice(existingIndex, 1, payload);
  } else {
    nextRows.push(payload);
  }
  itemDraftRows.value = sortItems(
    payload.defaultItem
      ? nextRows.map((item) => ({
          ...item,
          defaultItem: item.id === payload.id,
          enabled: item.id === payload.id ? true : item.enabled,
        }))
      : nextRows,
  );
  closeItemEditor();
}

async function removeItem(itemId: string) {
  if (!isEditableMode.value) return;
  const target = itemDraftRows.value.find((item) => item.id === itemId);
  if (!target) return;
  if (itemDraftRows.value.some((item) => item.parentItemId === target.id)) {
    message.error("请先删除子项");
    return;
  }
  const confirmed = await bzConfirm({
    title: "删除字典项",
    message: `确认删除字典项 ${target.itemLabel}？`,
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!confirmed) return;
  itemDraftRows.value = itemDraftRows.value.filter((item) => item.id !== target.id);
  message.success("字典项已从当前页面移除");
  if (itemForm.id === target.id) {
    closeItemEditor();
  }
}

function handleItemDragStart(itemId: string) {
  if (!isEditableMode.value || isTreeType.value) return;
  dragState.draggingId = itemId;
  dragState.overId = itemId;
  dragState.overPosition = "before";
}

function handleItemDragOver(event: DragEvent, itemId: string) {
  if (!dragState.draggingId || dragState.draggingId === itemId || !event.currentTarget) return;
  const target = event.currentTarget as HTMLElement;
  const rect = target.getBoundingClientRect();
  dragState.overId = itemId;
  dragState.overPosition = event.clientY < rect.top + rect.height / 2 ? "before" : "after";
}

function handleItemDrop(targetId: string) {
  if (!dragState.draggingId || dragState.sorting || isTreeType.value) {
    clearItemDragState();
    return;
  }
  const reordered = reorderItems(
    itemDraftRows.value,
    dragState.draggingId,
    targetId,
    dragState.overPosition,
  );
  clearItemDragState();
  if (!reordered) return;
  itemDraftRows.value = reordered.map((item, index) => ({ ...item, sortNo: index + 1 }));
}

function clearItemDragState() {
  dragState.draggingId = "";
  dragState.overId = "";
  dragState.overPosition = "before";
}

function reorderItems(
  list: DictItem[],
  sourceId: string,
  targetId: string,
  position: DragPosition,
): DictItem[] | null {
  const sourceIndex = list.findIndex((item) => item.id === sourceId);
  const targetIndex = list.findIndex((item) => item.id === targetId);
  if (sourceIndex < 0 || targetIndex < 0 || sourceIndex === targetIndex) return null;
  const reordered = [...list];
  const [source] = reordered.splice(sourceIndex, 1);
  let insertIndex = targetIndex;
  if (sourceIndex < targetIndex) insertIndex -= 1;
  if (position === "after") insertIndex += 1;
  reordered.splice(insertIndex, 0, source);
  return reordered.every((item, index) => item.id === list[index]?.id) ? null : reordered;
}

function nextSortNo(parentItemId: string | null): number {
  const siblings = itemDraftRows.value.filter(
    (item) => (item.parentItemId || null) === parentItemId,
  );
  if (siblings.length === 0) return 1;
  return Math.max(...siblings.map((item) => item.sortNo)) + 1;
}

function collectDescendantIds(itemId: string, bucket: Set<string>) {
  for (const child of itemDraftRows.value.filter((item) => item.parentItemId === itemId)) {
    bucket.add(child.id);
    collectDescendantIds(child.id, bucket);
  }
}

function buildParentOptionLabel(item: DictItem): string {
  const names: string[] = [item.itemLabel];
  let cursor = item.parentItemId;
  while (cursor) {
    const parent = itemDraftRows.value.find((current) => current.id === cursor);
    if (!parent) break;
    names.unshift(parent.itemLabel);
    cursor = parent.parentItemId;
  }
  return names.join(" / ");
}

function sortItems(list: DictItem[]): DictItem[] {
  return [...list].sort((a, b) => a.sortNo - b.sortNo || a.itemLabel.localeCompare(b.itemLabel));
}

function toOptions(source: DictItem[] | undefined, fallback: DictOption[]): DictOption[] {
  if (!source || source.length === 0) return fallback;
  return source.map((item) => ({ label: item.itemLabel, value: item.itemValue }));
}

function resolveOptionLabel(options: DictOption[], value: string): string {
  return options.find((item) => item.value === value)?.label || value;
}

function resolveValueTypeLabel(value: DictValueType): string {
  return resolveOptionLabel(valueTypeOptions.value, value);
}

function resolveStructureTypeLabel(value: DictStructureType): string {
  return resolveOptionLabel(structureTypeOptions.value, value);
}

function resolveSourceTypeLabel(value: DictSourceType): string {
  return sourceTypeLabelMap[value] || value;
}

function resolveTagTypeLabel(tagType: string | null | undefined): string {
  if (!tagType) return "标签";
  return resolveOptionLabel(tagTypeOptions.value, tagType);
}

function buildTagPreviewStyle(
  tagColor: string | null | undefined,
  tagType: string | null | undefined,
): CSSProperties {
  const typeStyleMap: Record<string, { color: string; background: string; borderColor: string }> = {
    info: { color: "#475569", background: "#f1f5f9", borderColor: "#cbd5e1" },
    success: { color: "#15803d", background: "#ecfdf3", borderColor: "#86efac" },
    warning: { color: "#b45309", background: "#fffbeb", borderColor: "#fcd34d" },
    danger: { color: "#b91c1c", background: "#fef2f2", borderColor: "#fca5a5" },
  };
  const preset = typeStyleMap[tagType || "info"];
  if (!tagColor) return preset;
  return {
    color: tagColor,
    borderColor: `${tagColor}55`,
    background: `${tagColor}14`,
  };
}

function safeTrim(value: string | null | undefined): string {
  return typeof value === "string" ? value.trim() : "";
}

function newDraftItemId(): string {
  return `${DRAFT_ITEM_ID_PREFIX}${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}

function isDraftItemId(id: string | null | undefined): boolean {
  return typeof id === "string" && id.startsWith(DRAFT_ITEM_ID_PREFIX);
}

function sanitizeNumberInput(value: string | null | undefined): string {
  const source = typeof value === "string" ? value : "";
  const filtered = source.replace(/[^\d.-]/g, "");
  const sign = filtered.startsWith("-") ? "-" : "";
  const unsigned = filtered.replace(/-/g, "");
  const [integerPart = "", ...decimalParts] = unsigned.split(".");
  const integer = integerPart.replace(/^0+(?=\d)/, "0");
  const decimal = decimalParts.join("");
  return decimalParts.length > 0 ? `${sign}${integer}.${decimal}` : `${sign}${integer}`;
}

function isValidNumberLiteral(value: string): boolean {
  return /^-?(0|[1-9]\d*)(\.\d+)?$/.test(value);
}
</script>

<style scoped>
.content {
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}

.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.subdued {
  color: var(--text-muted);
}

.dict-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 14px 20px 18px;
  color: #475569;
}

.dict-pagination-summary {
  font-size: 13px;
  color: #64748b;
}

.dict-pagination-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.dict-page-size__select {
  min-width: 92px;
  height: 32px;
  padding: 0 12px;
  color: #0f172a;
  background: #ffffff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  font-size: 13px;
}

.dict-page-list {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.dict-page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 10px;
  color: #334155;
  background: #ffffff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.dict-page-btn:hover:not(:disabled) {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.dict-page-btn.is-active {
  color: #ffffff;
  background: #1677ff;
  border-color: #1677ff;
}

.dict-page-btn:disabled {
  opacity: 0.52;
  cursor: not-allowed;
}

.dict-page-ellipsis {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  color: #94a3b8;
}

.dict-drawer-layout {
  display: grid;
  gap: 18px;
}

.dict-drawer-section {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  padding: 18px;
}

.dict-drawer-section__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.dict-drawer-section__title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.dict-drawer-section__tip {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.dict-drawer-section__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.dict-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.dict-detail-field {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  min-width: 0;
}

.dict-detail-field--wide,
.dict-form-item--wide {
  grid-column: 1 / -1;
}

.dict-detail-field__label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  line-height: 1.75;
}

.dict-detail-field__value {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.dict-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 14px;
}

.dict-form-grid--compact {
  margin-bottom: 8px;
}

.dict-switch-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 14px;
  margin: 4px 0 8px;
}

.dict-summary-section {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.dict-items-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-size: 12px;
  color: #64748b;
}

.dict-item-editor-card {
  margin-bottom: 14px;
  padding: 16px;
  border: 1px solid #dbe1ea;
  border-radius: 14px;
  background: #f8fbff;
}

.dict-item-editor-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.dict-item-list-wrap {
  display: grid;
  gap: 10px;
}

.dict-item-row {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 16px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.dict-item-row:hover {
  border-color: rgba(59, 130, 246, 0.35);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.dict-item-row.is-dragging {
  opacity: 0.56;
}

.dict-item-row.is-drop-before::before,
.dict-item-row.is-drop-after::after {
  content: "";
  position: absolute;
  left: 12px;
  right: 12px;
  height: 2px;
  border-radius: 999px;
  background: #3b82f6;
}

.dict-item-row.is-drop-before::before {
  top: -6px;
}

.dict-item-row.is-drop-after::after {
  bottom: -6px;
}

.dict-item-row-main,
.dict-tree-node__main {
  min-width: 0;
  flex: 1;
}

.dict-item-line {
  display: grid;
  grid-template-columns: minmax(140px, 0.9fr) minmax(140px, 1fr) minmax(160px, 1.1fr) auto;
  align-items: center;
  gap: 12px;
}

.dict-item-row-actions,
.dict-tree-node__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.dict-item-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.dict-item-label-tags {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
}

.dict-item-default-placeholder {
  display: inline-flex;
  width: 44px;
  min-width: 44px;
  height: 24px;
}

.dict-item-label {
  min-width: 0;
  overflow: hidden;
  color: var(--text-main);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dict-item-code,
.dict-item-value {
  min-width: 0;
  overflow: hidden;
  color: var(--text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dict-item-code {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.dict-tree-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
}

.dict-custom-tag {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
}

.dict-inline-link {
  padding: 0;
  color: #475569;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 13px;
}

.dict-inline-link.is-edit {
  color: #1677ff;
}

.dict-inline-link.is-delete {
  color: #dc2626;
}

.dict-tag-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}

.dict-tag-preview-label {
  font-size: 12px;
  color: #64748b;
}

.dict-textarea-stack {
  display: grid;
  gap: 8px;
}

.dict-field-error {
  margin-top: 6px;
  font-size: 12px;
  color: #dc2626;
}

@media (max-width: 1180px) {
  .dict-detail-grid,
  .dict-form-grid,
  .dict-switch-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .dict-drawer-section__head,
  .dict-item-editor-card__head,
  .dict-item-row,
  .dict-tree-node {
    flex-direction: column;
    align-items: stretch;
  }

  .dict-item-line {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .dict-item-label-tags {
    justify-content: flex-start;
  }
}
</style>
