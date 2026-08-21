"use client";

import {
  createDictItem,
  createDictType,
  deleteDictType,
  getDictType,
  listDictItems,
  listDictTypes,
  sortDictItems,
  updateDictItem,
  updateDictItemStatus,
  updateDictType,
  updateDictTypeStatus,
} from "@admin/features/dicts/api/client";
import type {
  DictItem,
  DictStructureType,
  DictTypeItem,
  DictValueType,
} from "@admin/features/dicts/model/types";
import { DICT_PERMISSIONS } from "@admin/features/dicts/permissions";
import { usePermission } from "@admin/features/resources/permissions";
import { useAdminPagedQuery } from "@admin/shared/hooks/useAdminPagedQuery";
import { bzConfirm } from "@admin/shared/lib/feedback/confirm";
import { message } from "@admin/shared/lib/feedback/message";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import { AdminEntityDrawer } from "@admin/shared/ui/admin/AdminEntityDrawer";
import { AdminInfoCell } from "@admin/shared/ui/admin/AdminInfoCell";
import { AdminSearchField, AdminSearchForm } from "@admin/shared/ui/admin/AdminSearchForm";
import { AdminTablePagination } from "@admin/shared/ui/admin/AdminTablePagination";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import { TableInput, TableSelect, TableTextArea } from "@admin/shared/ui/admin/inputs";
import {
  BzAlert,
  BzButton,
  BzCard,
  BzDialog,
  BzDragHandle,
  BzForm,
  BzFormItem,
  BzInput,
  BzOption,
  BzSelect,
  BzSwitch,
  BzTable,
  type BzTableColumn,
  BzTag,
  BzTextField,
  BzTooltip,
} from "@admin/shared/ui/bz";
import type { DragEvent } from "react";
import { useEffect, useMemo, useRef, useState } from "react";

import styles from "./DictsPage.module.css";

type DrawerMode = "create" | "detail" | "edit";
type ItemEditorMode = "create" | "edit";

const pageSizeOptions = [10, 20, 30, 50, 100];
const initialFilters = { code: "", name: "" };

type DictFilters = typeof initialFilters;

const valueTypeOptions: Array<{ value: DictValueType; label: string }> = [
  { value: "STRING", label: "字符串" },
  { value: "NUMBER", label: "数字" },
  { value: "BOOLEAN", label: "布尔" },
];

const structureTypeOptions: Array<{ value: DictStructureType; label: string }> = [
  { value: "FLAT", label: "平铺" },
  { value: "TREE", label: "树形" },
];

const sourceTypeLabelMap: Record<string, string> = {
  BUILTIN: "内建",
  CUSTOM: "自定义",
};

const tagTypeLabelMap: Record<string, string> = {
  info: "信息",
  success: "成功",
  warning: "警告",
  danger: "危险",
};

const tagTypeOptions = Object.entries(tagTypeLabelMap).map(([value, label]) => ({ value, label }));

function safeTrim(value: string) {
  return value.trim();
}

function nextSortNo(items: DictItem[]) {
  if (items.length === 0) return 1;
  return Math.max(...items.map((item) => item.sortNo)) + 1;
}

export function DictsPage() {
  const canView = usePermission(DICT_PERMISSIONS.view);
  const canEdit = usePermission(DICT_PERMISSIONS.edit);
  const canCreate = canEdit;

  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [batchMode, setBatchMode] = useState(false);
  const [selectedTypeIds, setSelectedTypeIds] = useState<string[]>([]);
  const {
    page,
    draftFilters,
    setDraftFilters,
    pageNo,
    pageSize,
    loading,
    error,
    submit,
    reset,
    refresh,
    setPageNo,
    setPageSize,
  } = useAdminPagedQuery<DictTypeItem, DictFilters>({
    initialFilters,
    pageSizes: pageSizeOptions,
    enabled: canView,
    normalizeFilters: (filters) => ({
      code: filters.code.trim(),
      name: filters.name.trim(),
    }),
    query: ({ filters, pageNo: targetPage, pageSize: targetSize, signal }) =>
      listDictTypes(
        {
          code: filters.code || undefined,
          name: filters.name || undefined,
          page: { pageNo: targetPage, pageSize: targetSize },
        },
        { signal },
      ),
  });

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [drawerError, setDrawerError] = useState("");
  const [drawerMode, setDrawerMode] = useState<DrawerMode>("detail");
  const [currentType, setCurrentType] = useState<DictTypeItem | null>(null);
  const [currentItems, setCurrentItems] = useState<DictItem[]>([]);
  const [typeForm, setTypeForm] = useState({
    code: "",
    name: "",
    description: "",
    enumClass: "",
    valueType: "STRING" as DictValueType | "",
    structureType: "FLAT" as DictStructureType | "",
    enabled: true,
  });
  const [savingType, setSavingType] = useState(false);

  const [itemEditorOpen, setItemEditorOpen] = useState(false);
  const [itemEditorMode, setItemEditorMode] = useState<ItemEditorMode>("create");
  const [editingItemId, setEditingItemId] = useState("");
  const [itemForm, setItemForm] = useState({
    code: "",
    label: "",
    value: "",
    tagType: "",
    enabled: true,
    description: "",
  });
  const [savingItem, setSavingItem] = useState(false);
  const [itemDragSourceId, setItemDragSourceId] = useState<string | null>(null);
  const [itemDragTargetId, setItemDragTargetId] = useState<string | null>(null);
  const [sortingItems, setSortingItems] = useState(false);

  const currentTypeRef = useRef<DictTypeItem | null>(null);
  const drawerRequestControllerRef = useRef<AbortController | null>(null);
  const drawerRequestSequenceRef = useRef(0);
  currentTypeRef.current = currentType;

  const rows = page.elements;

  useEffect(() => {
    return () => {
      drawerRequestSequenceRef.current += 1;
      drawerRequestControllerRef.current?.abort();
    };
  }, []);

  function beginDrawerRequest() {
    drawerRequestSequenceRef.current += 1;
    drawerRequestControllerRef.current?.abort();
    const controller = new AbortController();
    drawerRequestControllerRef.current = controller;
    return { controller, sequence: drawerRequestSequenceRef.current };
  }

  function cancelDrawerRequest() {
    drawerRequestSequenceRef.current += 1;
    drawerRequestControllerRef.current?.abort();
    drawerRequestControllerRef.current = null;
  }

  function isCurrentDrawerRequest(sequence: number, controller: AbortController) {
    return (
      sequence === drawerRequestSequenceRef.current &&
      controller === drawerRequestControllerRef.current &&
      !controller.signal.aborted
    );
  }

  async function loadDrawer(typeId: string, mode: DrawerMode) {
    const { controller, sequence } = beginDrawerRequest();
    setCurrentType(null);
    setCurrentItems([]);
    setDrawerError("");
    setDrawerMode(mode);
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      const options = { signal: controller.signal };
      const [type, items] = await Promise.all([
        getDictType(typeId, options),
        listDictItems(typeId, options),
      ]);
      if (!isCurrentDrawerRequest(sequence, controller)) return;
      setCurrentType(type);
      setCurrentItems(items);
      setTypeForm({
        code: type.code,
        name: type.name,
        description: type.description || "",
        enumClass: type.enumClass || "",
        valueType: type.valueType,
        structureType: type.structureType,
        enabled: type.enabled,
      });
    } catch (cause) {
      if (isCurrentDrawerRequest(sequence, controller) && !isAbortError(cause)) {
        setDrawerError(cause instanceof Error ? cause.message : "字典详情加载失败");
      }
    } finally {
      if (isCurrentDrawerRequest(sequence, controller)) {
        drawerRequestControllerRef.current = null;
        setDrawerLoading(false);
      }
    }
  }

  function closeDrawer() {
    cancelDrawerRequest();
    setDrawerLoading(false);
    setDrawerOpen(false);
    setCurrentType(null);
    setCurrentItems([]);
    setDrawerError("");
    setItemEditorOpen(false);
    setTypeForm({
      code: "",
      name: "",
      description: "",
      enumClass: "",
      valueType: "STRING",
      structureType: "FLAT",
      enabled: true,
    });
  }

  function openCreateType() {
    if (!canCreate) return;
    cancelDrawerRequest();
    setDrawerMode("create");
    setCurrentType(null);
    setCurrentItems([]);
    setDrawerError("");
    setTypeForm({
      code: "",
      name: "",
      description: "",
      enumClass: "",
      valueType: "STRING",
      structureType: "FLAT",
      enabled: true,
    });
    setDrawerOpen(true);
    setDrawerLoading(false);
  }

  async function refreshDrawerItems() {
    const type = currentTypeRef.current;
    if (!type) return;
    const { controller, sequence } = beginDrawerRequest();
    setDrawerError("");
    setDrawerLoading(true);
    try {
      const options = { signal: controller.signal };
      const [nextType, nextItems] = await Promise.all([
        getDictType(type.id, options),
        listDictItems(type.id, options),
      ]);
      if (!isCurrentDrawerRequest(sequence, controller)) return;
      setCurrentType(nextType);
      setCurrentItems(nextItems);
      setTypeForm((prev) => ({
        ...prev,
        enabled: nextType.enabled,
      }));
    } catch (cause) {
      if (isCurrentDrawerRequest(sequence, controller) && !isAbortError(cause)) {
        setDrawerError(cause instanceof Error ? cause.message : "字典详情刷新失败");
      }
    } finally {
      if (isCurrentDrawerRequest(sequence, controller)) {
        drawerRequestControllerRef.current = null;
        setDrawerLoading(false);
      }
    }
  }

  async function handleTypeStatus(row: DictTypeItem) {
    if (!canEdit) return;
    await updateDictTypeStatus(row.id, !row.enabled);
    message.success(!row.enabled ? "已启用" : "已停用");
    await refresh();
    if (currentTypeRef.current?.id === row.id && drawerOpen) {
      await refreshDrawerItems();
    }
  }

  async function handleDeleteType(row: DictTypeItem) {
    if (!canEdit) return;
    const confirmed = await bzConfirm({
      title: "删除字典",
      content: `确认删除字典 ${row.name}？`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteDictType(row.id);
    message.success("删除成功");
    if (currentTypeRef.current?.id === row.id) {
      closeDrawer();
    }
    await refresh();
  }

  async function submitType() {
    const type = currentTypeRef.current;
    const code = safeTrim(typeForm.code);
    const name = safeTrim(typeForm.name);
    if (drawerMode === "create" && !code) {
      message.warning("字典编码不能为空");
      return;
    }
    if (!name) {
      message.warning("字典名称不能为空");
      return;
    }
    if (!typeForm.valueType || !typeForm.structureType) {
      message.warning("值类型和结构不能为空");
      return;
    }
    setSavingType(true);
    try {
      if (drawerMode === "create") {
        await createDictType({
          code,
          name,
          description: safeTrim(typeForm.description) || null,
          enumClass: safeTrim(typeForm.enumClass) || null,
          valueType: typeForm.valueType,
          structureType: typeForm.structureType,
          enabled: typeForm.enabled,
          items: currentItems.map((item) => ({
            clientKey: item.id,
            parentClientKey: item.parentItemId || null,
            itemCode: item.itemCode,
            itemLabel: item.itemLabel,
            itemValue: item.itemValue,
            sortNo: item.sortNo,
            enabled: item.enabled,
            defaultItem: item.defaultItem,
            tagColor: item.tagColor,
            tagType: item.tagType,
            extraJson: item.extraJson,
            description: item.description,
          })),
        });
        message.success("新增成功");
      } else {
        if (!type) return;
        await updateDictType(type.id, {
          name,
          description: safeTrim(typeForm.description) || null,
          enumClass: safeTrim(typeForm.enumClass) || null,
          valueType: typeForm.valueType,
          structureType: typeForm.structureType,
          enabled: typeForm.enabled,
          items: currentItems.map((item) => ({
            id: item.id,
            clientKey: item.id,
            parentClientKey: item.parentItemId || null,
            itemCode: item.itemCode,
            itemLabel: item.itemLabel,
            itemValue: item.itemValue,
            sortNo: item.sortNo,
            enabled: item.enabled,
            defaultItem: item.defaultItem,
            tagColor: item.tagColor,
            tagType: item.tagType,
            extraJson: item.extraJson,
            description: item.description,
          })),
        });
        message.success("保存成功");
      }
      closeDrawer();
      await refresh();
    } finally {
      setSavingType(false);
    }
  }

  async function handleBatchDelete() {
    if (!canEdit || selectedTypeIds.length === 0) return;
    const selectedRows = rows.filter((row) => selectedTypeIds.includes(row.id));
    const protectedRows = selectedRows.filter((row) => row.sourceType === "BUILTIN");
    if (protectedRows.length > 0) {
      message.warning("系统内建字典不允许批量删除");
      return;
    }
    const confirmed = await bzConfirm({
      title: "批量删除字典",
      content: `确认删除已选 ${selectedTypeIds.length} 个字典？`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await Promise.all(selectedTypeIds.map((id) => deleteDictType(id)));
    message.success("批量删除成功");
    setSelectedTypeIds([]);
    setBatchMode(false);
    await refresh();
  }

  function toggleTypeSelection(typeId: string, checked: boolean) {
    setSelectedTypeIds((prev) => {
      const next = new Set(prev);
      if (checked) next.add(typeId);
      else next.delete(typeId);
      return Array.from(next);
    });
  }

  function toggleSelectAllCurrentPage(checked: boolean) {
    const selectableIds = rows.filter((row) => row.sourceType !== "BUILTIN").map((row) => row.id);
    setSelectedTypeIds((prev) => {
      const next = new Set(prev);
      if (checked) selectableIds.forEach((id) => next.add(id));
      else selectableIds.forEach((id) => next.delete(id));
      return Array.from(next);
    });
  }

  function openCreateItem() {
    setItemEditorMode("create");
    setEditingItemId("");
    setItemForm({
      code: "",
      label: "",
      value: "",
      tagType: "",
      enabled: true,
      description: "",
    });
    setItemEditorOpen(true);
  }

  function openEditItem(item: DictItem) {
    setItemEditorMode("edit");
    setEditingItemId(item.id);
    setItemForm({
      code: item.itemCode,
      label: item.itemLabel,
      value: item.itemValue,
      tagType: item.tagType || "",
      enabled: item.enabled,
      description: item.description || "",
    });
    setItemEditorOpen(true);
  }

  async function saveItem() {
    const type = currentTypeRef.current;
    const code = safeTrim(itemForm.code);
    const label = safeTrim(itemForm.label);
    const value = safeTrim(itemForm.value);
    if (!code || !label || !value) {
      message.warning("请完整填写编码、标签和值");
      return;
    }
    if (drawerMode === "create") {
      if (itemEditorMode === "create") {
        const clientKey = `draft-${crypto.randomUUID()}`;
        setCurrentItems((items) => [
          ...items,
          {
            id: clientKey,
            dictTypeId: "",
            parentItemId: null,
            itemCode: code,
            itemLabel: label,
            itemValue: value,
            sortNo: nextSortNo(items),
            enabled: itemForm.enabled,
            defaultItem: false,
            tagColor: null,
            tagType: safeTrim(itemForm.tagType) || null,
            extraJson: null,
            description: safeTrim(itemForm.description) || null,
          },
        ]);
      } else {
        setCurrentItems((items) =>
          items.map((item) =>
            item.id === editingItemId
              ? {
                  ...item,
                  itemLabel: label,
                  itemValue: value,
                  enabled: itemForm.enabled,
                  tagType: safeTrim(itemForm.tagType) || null,
                  description: safeTrim(itemForm.description) || null,
                }
              : item,
          ),
        );
      }
      setItemEditorOpen(false);
      return;
    }
    if (!type) return;
    setSavingItem(true);
    try {
      if (itemEditorMode === "create") {
        await createDictItem(type.id, {
          parentItemId: null,
          itemCode: code,
          itemLabel: label,
          itemValue: value,
          sortNo: nextSortNo(currentItems),
          enabled: itemForm.enabled,
          defaultItem: false,
          tagColor: null,
          tagType: safeTrim(itemForm.tagType) || null,
          extraJson: null,
          description: safeTrim(itemForm.description) || null,
        });
        message.success("添加成功");
      } else {
        const editingItem = currentItems.find((item) => item.id === editingItemId);
        await updateDictItem(editingItemId, {
          parentItemId: null,
          itemLabel: label,
          itemValue: value,
          sortNo: editingItem?.sortNo ?? nextSortNo(currentItems),
          enabled: itemForm.enabled,
          defaultItem: false,
          tagColor: null,
          tagType: safeTrim(itemForm.tagType) || null,
          extraJson: null,
          description: safeTrim(itemForm.description) || null,
        });
        message.success("保存成功");
      }
      setItemEditorOpen(false);
      await refreshDrawerItems();
    } finally {
      setSavingItem(false);
    }
  }

  async function handleItemStatus(item: DictItem) {
    const type = currentTypeRef.current;
    if (!canEdit) return;
    if (drawerMode === "create") {
      setCurrentItems((items) =>
        items.map((current) =>
          current.id === item.id ? { ...current, enabled: !current.enabled } : current,
        ),
      );
      return;
    }
    if (!type) return;
    await updateDictItemStatus(item.id, !item.enabled);
    message.success(!item.enabled ? "已启用" : "已停用");
    await refreshDrawerItems();
  }

  function startItemDrag(event: DragEvent<HTMLButtonElement>, itemId: string) {
    if (drawerMode === "detail" || sortingItems) return;
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", itemId);
    setItemDragSourceId(itemId);
    setItemDragTargetId(itemId);
  }

  function dragOverItem(event: DragEvent<HTMLTableRowElement>, itemId: string) {
    if (!itemDragSourceId || drawerMode === "detail" || sortingItems) return;
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
    setItemDragTargetId(itemId);
  }

  async function dropItem(event: DragEvent<HTMLTableRowElement>, targetId: string) {
    event.preventDefault();
    const type = currentTypeRef.current;
    const sourceIndex = currentItems.findIndex((item) => item.id === itemDragSourceId);
    const targetIndex = currentItems.findIndex((item) => item.id === targetId);
    clearItemDrag();
    if (sourceIndex < 0 || targetIndex < 0 || sourceIndex === targetIndex || sortingItems) return;

    const reordered = [...currentItems];
    const [moved] = reordered.splice(sourceIndex, 1);
    reordered.splice(targetIndex, 0, moved);
    const normalized = reordered.map((item, index) => ({ ...item, sortNo: index + 1 }));
    setCurrentItems(normalized);
    if (drawerMode === "create") return;
    if (!type) return;
    setSortingItems(true);
    try {
      await sortDictItems(
        type.id,
        normalized.map((item) => item.id),
      );
      message.success("字典项排序已更新");
      await refreshDrawerItems();
    } catch {
      await refreshDrawerItems();
    } finally {
      setSortingItems(false);
    }
  }

  function clearItemDrag() {
    setItemDragSourceId(null);
    setItemDragTargetId(null);
  }

  function handleDeleteItem(item: DictItem) {
    setCurrentItems((items) =>
      items
        .filter((current) => current.id !== item.id)
        .map((current, index) => ({ ...current, sortNo: index + 1 })),
    );
  }

  const columns = useMemo<Array<BzTableColumn<DictTypeItem>>>(() => {
    const baseColumns: Array<BzTableColumn<DictTypeItem>> = [
      {
        key: "code",
        title: "编码",
        width: 240,
        render: (row) => <>{row.code}</>,
      },
      {
        key: "name",
        title: "名称",
        width: 220,
        render: (row) => <>{row.name}</>,
      },
      {
        key: "valueType",
        title: "值类型",
        width: 100,
        render: (row) => <>{resolveValueTypeLabel(row.valueType)}</>,
      },
      {
        key: "structureType",
        title: "结构",
        width: 100,
        render: (row) => <>{resolveStructureTypeLabel(row.structureType)}</>,
      },
      {
        key: "sourceType",
        title: "来源",
        width: 100,
        render: (row) => <>{sourceTypeLabelMap[row.sourceType] || row.sourceType}</>,
      },
      {
        key: "enabled",
        title: "状态",
        width: 100,
        render: (row) => (
          <BzTag type={row.enabled ? "success" : "danger"}>{row.enabled ? "启用" : "停用"}</BzTag>
        ),
      },
      {
        key: "description",
        title: "描述",
        width: 260,
        className: styles.typeDescriptionCell,
        headerClassName: styles.typeDescriptionCell,
        render: (row) => (
          <span
            className={`cell-text ${styles.typeDescriptionText}`}
            title={row.description || undefined}
          >
            {row.description || "-"}
          </span>
        ),
      },
    ];
    const actionsColumn = createAdminActionsColumn({ rows, getActions: getTypeRowActions });
    return actionsColumn ? [...baseColumns, actionsColumn] : baseColumns;
  }, [canView, canEdit, drawerOpen, rows]);

  const itemColumns = useMemo<Array<BzTableColumn<DictItem>>>(() => {
    const actionColumn =
      drawerMode !== "detail" && canEdit
        ? createAdminActionsColumn({ rows: currentItems, getActions: getItemRowActions })
        : null;

    return [
      {
        key: "order",
        title: "",
        width: 42,
        className: styles.itemsTableDragCell,
        headerClassName: styles.itemsTableDragCell,
        render: (row, index) =>
          drawerMode !== "detail" && canEdit ? (
            <BzDragHandle
              disabled={sortingItems}
              onDragStart={(event) => startItemDrag(event, row.id)}
              onDragEnd={clearItemDrag}
            />
          ) : (
            <span className={styles.itemsTableOrder}>{index + 1}</span>
          ),
      },
      {
        key: "itemCode",
        title: "编码",
        minWidth: 170,
        render: (row) => <>{row.itemCode}</>,
      },
      {
        key: "itemLabel",
        title: "标签",
        minWidth: 150,
        render: (row) => <>{row.itemLabel}</>,
      },
      {
        key: "itemValue",
        title: "值",
        minWidth: 180,
        render: (row) => <>{row.itemValue}</>,
      },
      {
        key: "tagType",
        title: "标签类型",
        width: 100,
        render: (row) =>
          row.tagType ? (
            <BzTag
              size="small"
              type={row.tagType as "info" | "success" | "warning" | "danger"}
            >
              {tagTypeLabelMap[row.tagType] || row.tagType}
            </BzTag>
          ) : (
            <>-</>
          ),
      },
      {
        key: "enabled",
        title: "状态",
        width: 90,
        render: (row) => (
          <BzTag type={row.enabled ? "success" : "danger"}>{row.enabled ? "启用" : "停用"}</BzTag>
        ),
      },
      {
        key: "description",
        title: "备注",
        width: 260,
        render: (row) => {
          const text = row.description || "-";
          return (
            <BzTooltip content={row.description || ""}>
              <span className={styles.itemRemark}>{text}</span>
            </BzTooltip>
          );
        },
      },
      ...(actionColumn ? [actionColumn] : []),
    ];
  }, [drawerMode, canEdit, currentItems, sortingItems]);

  const drawerTitle =
    drawerMode === "create" ? "新增字典" : drawerMode === "detail" ? "字典详情" : "编辑字典";

  const drawerFooter = (
    <>
      <BzButton onClick={closeDrawer}>{drawerMode === "detail" ? "关闭" : "取消"}</BzButton>
      {drawerMode !== "detail" ? (
        <BzButton
          buttonType="primary"
          loading={savingType}
          onClick={() => void submitType()}
        >
          确定
        </BzButton>
      ) : null}
    </>
  );

  if (!canView) {
    return (
      <div className="admin-page">
        <div className="content">
          <BzAlert
            title="暂无权限访问字典管理"
            type="warning"
            closable={false}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          <BzCard
            className="admin-panel admin-table-card admin-list-card"
            shadow="never"
          >
            <div className="admin-list-region">
              {queryPanelVisible ? (
                <div className="admin-list-query-panel">
                  <AdminSearchForm
                    visible={queryPanelVisible}
                    onSubmit={submit}
                    onReset={reset}
                  >
                    <AdminSearchField label="编码">
                      <BzInput
                        modelValue={draftFilters.code}
                        placeholder="请输入字典编码"
                        clearable
                        onValueChange={(code) =>
                          setDraftFilters((filters) => ({ ...filters, code }))
                        }
                      />
                    </AdminSearchField>
                    <AdminSearchField label="名称">
                      <BzInput
                        modelValue={draftFilters.name}
                        placeholder="请输入字典名称"
                        clearable
                        onValueChange={(name) =>
                          setDraftFilters((filters) => ({ ...filters, name }))
                        }
                      />
                    </AdminSearchField>
                  </AdminSearchForm>
                </div>
              ) : null}

              {batchMode ? (
                <div className="admin-batch-toolbar">
                  <div className="admin-batch-toolbar__summary">
                    批量删除中，已选 {selectedTypeIds.length} 项
                  </div>
                  <div className="admin-batch-toolbar__actions">
                    <BzButton
                      buttonType="primary"
                      disabled={selectedTypeIds.length === 0}
                      onClick={() => void handleBatchDelete()}
                    >
                      确认删除
                    </BzButton>
                    <BzButton
                      onClick={() => {
                        setBatchMode(false);
                        setSelectedTypeIds([]);
                      }}
                    >
                      取消
                    </BzButton>
                  </div>
                </div>
              ) : (
                <div className="admin-list-toolbar-row">
                  <div className="admin-list-business-actions">
                    {canCreate ? (
                      <BzButton
                        className="admin-toolbar-primary"
                        buttonType="primary"
                        onClick={openCreateType}
                      >
                        新增
                      </BzButton>
                    ) : null}
                    {canEdit ? (
                      <BzButton onClick={() => setBatchMode(true)}>批量删除</BzButton>
                    ) : null}
                  </div>
                  <div className="admin-list-query-tools">
                    <AdminTableTools
                      queryPanelVisible={queryPanelVisible}
                      onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
                      onRefresh={() => void refresh()}
                    />
                  </div>
                </div>
              )}

              <div className="admin-table-surface admin-list-table-area">
                {error ? (
                  <BzAlert
                    key={error.message}
                    title={error.message}
                    type="error"
                    closable={false}
                  />
                ) : null}
                <BzTable
                  columns={columns}
                  data={rows}
                  loading={loading}
                  rowKey="id"
                  emptyText="暂无字典记录"
                  size="small"
                  rowSelection={
                    batchMode
                      ? {
                          selectedRowKeys: selectedTypeIds,
                          isRowSelectable: (row) => row.sourceType !== "BUILTIN",
                          onToggle: (row, selected) => toggleTypeSelection(row.id, selected),
                          onToggleCurrentPage: (_rows, selected) =>
                            toggleSelectAllCurrentPage(selected),
                        }
                      : undefined
                  }
                />
              </div>
              <AdminTablePagination
                total={page.totalElements}
                pageNo={pageNo}
                pageSize={pageSize}
                pageSizes={pageSizeOptions}
                onPageChange={setPageNo}
                onPageSizeChange={setPageSize}
              />
            </div>
          </BzCard>
        </div>

        <AdminEntityDrawer
          open={drawerOpen}
          title={drawerTitle}
          width="1180px"
          className="admin-entity-manage-drawer"
          loading={drawerLoading}
          onClose={closeDrawer}
          footer={drawerFooter}
        >
          {drawerError ? (
            <BzAlert
              key={drawerError}
              title={drawerError}
              type="error"
              closable={false}
            />
          ) : null}
          {currentType || drawerMode === "create" ? (
            <div className="admin-entity-shell">
              <section className="admin-entity-section">
                <div className="admin-entity-section__head">
                  <div className="admin-entity-section__title">字典基础信息</div>
                </div>
                <div className="admin-info-table-wrap">
                  <table
                    className="admin-info-table"
                    aria-label="字典基础信息"
                  >
                    <tbody>
                      <tr>
                        <th>
                          <span className={drawerMode === "create" ? "is-required" : undefined}>
                            编码
                          </span>
                        </th>
                        {drawerMode === "create" ? (
                          <AdminInfoCell
                            state="editable"
                            mono
                          >
                            <TableInput
                              value={typeForm.code}
                              maxLength={128}
                              placeholder="请输入字典编码"
                              onValueChange={(value) =>
                                setTypeForm((prev) => ({ ...prev, code: value }))
                              }
                            />
                          </AdminInfoCell>
                        ) : (
                          <AdminInfoCell
                            state={drawerMode === "edit" ? "readonly" : "display"}
                            mono
                          >
                            {currentType?.code || "-"}
                          </AdminInfoCell>
                        )}
                        <th>
                          <span className={drawerMode !== "detail" ? "is-required" : undefined}>
                            名称
                          </span>
                        </th>
                        {drawerMode === "detail" ? (
                          <AdminInfoCell className="table-input-display-cell">
                            {currentType?.name || "-"}
                          </AdminInfoCell>
                        ) : (
                          <AdminInfoCell state="editable">
                            <TableInput
                              value={typeForm.name}
                              maxLength={128}
                              placeholder="请输入字典名称"
                              onValueChange={(value) =>
                                setTypeForm((prev) => ({ ...prev, name: value }))
                              }
                            />
                          </AdminInfoCell>
                        )}
                        <th>值类型</th>
                        {drawerMode === "detail" ? (
                          <AdminInfoCell className="table-input-display-cell">
                            {currentType?.valueType
                              ? resolveValueTypeLabel(currentType.valueType)
                              : "-"}
                          </AdminInfoCell>
                        ) : (
                          <AdminInfoCell state="editable">
                            <TableSelect
                              value={typeForm.valueType}
                              options={valueTypeOptions}
                              allowClear={false}
                              onValueChange={(value) =>
                                setTypeForm((prev) => ({
                                  ...prev,
                                  valueType: value as DictValueType | "",
                                }))
                              }
                            />
                          </AdminInfoCell>
                        )}
                      </tr>
                      <tr>
                        <th>结构</th>
                        {drawerMode === "detail" ? (
                          <AdminInfoCell className="table-input-display-cell">
                            {currentType?.structureType
                              ? resolveStructureTypeLabel(currentType.structureType)
                              : "-"}
                          </AdminInfoCell>
                        ) : (
                          <AdminInfoCell state="editable">
                            <TableSelect
                              value={typeForm.structureType}
                              options={structureTypeOptions}
                              allowClear={false}
                              onValueChange={(value) =>
                                setTypeForm((prev) => ({
                                  ...prev,
                                  structureType: value as DictStructureType | "",
                                }))
                              }
                            />
                          </AdminInfoCell>
                        )}
                        <th>来源</th>
                        <AdminInfoCell state={drawerMode === "detail" ? "display" : "readonly"}>
                          <BzTag type={currentType?.sourceType === "BUILTIN" ? "warning" : "info"}>
                            {drawerMode === "create"
                              ? "自定义"
                              : sourceTypeLabelMap[currentType?.sourceType || ""] ||
                                currentType?.sourceType ||
                                "-"}
                          </BzTag>
                        </AdminInfoCell>
                        <th>状态</th>
                        <AdminInfoCell state={drawerMode === "detail" ? "display" : "editable"}>
                          {drawerMode === "detail" ? (
                            <BzTag type={currentType?.enabled ? "success" : "danger"}>
                              {currentType?.enabled ? "启用" : "停用"}
                            </BzTag>
                          ) : (
                            <span className={styles.manageStatusSwitch}>
                              <BzSwitch
                                modelValue={typeForm.enabled}
                                activeText="启用"
                                inactiveText="停用"
                                onValueChange={(value) =>
                                  setTypeForm((prev) => ({ ...prev, enabled: value }))
                                }
                              />
                            </span>
                          )}
                        </AdminInfoCell>
                      </tr>
                      <tr>
                        <th>枚举类</th>
                        {drawerMode === "detail" ? (
                          <AdminInfoCell
                            className="table-input-display-cell"
                            mono
                            colSpan={5}
                          >
                            {currentType?.enumClass || "-"}
                          </AdminInfoCell>
                        ) : (
                          <AdminInfoCell
                            state="editable"
                            colSpan={5}
                          >
                            <TableInput
                              value={typeForm.enumClass}
                              maxLength={255}
                              placeholder="请输入枚举类全限定名"
                              onValueChange={(value) =>
                                setTypeForm((prev) => ({ ...prev, enumClass: value }))
                              }
                            />
                          </AdminInfoCell>
                        )}
                      </tr>
                      <tr>
                        <th>描述</th>
                        {drawerMode === "detail" ? (
                          <AdminInfoCell colSpan={5}>
                            {currentType?.description || "-"}
                          </AdminInfoCell>
                        ) : (
                          <AdminInfoCell
                            state="editable"
                            colSpan={5}
                          >
                            <TableTextArea
                              value={typeForm.description}
                              rows={3}
                              maxLength={255}
                              placeholder="请输入字典描述"
                              onValueChange={(description) =>
                                setTypeForm((prev) => ({ ...prev, description }))
                              }
                            />
                          </AdminInfoCell>
                        )}
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>

              <section className={`admin-entity-section ${styles.itemsSection}`}>
                <div className="admin-entity-section__head">
                  <div className="admin-entity-section__title">字典项</div>
                  <div className="admin-table-tools">
                    {drawerMode !== "detail" && canEdit ? (
                      <BzButton
                        className="admin-toolbar-primary"
                        buttonType="primary"
                        onClick={openCreateItem}
                      >
                        新增
                      </BzButton>
                    ) : null}
                    {drawerMode !== "create" ? (
                      <button
                        className="admin-vben-circle-button"
                        type="button"
                        title="刷新字典项"
                        onClick={() => void refreshDrawerItems()}
                      >
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        >
                          <path d="M21.5 2v6h-6M2.5 12a9 9 0 0 1 15.46-6.35L21.5 8" />
                          <path d="M2.5 22v-6h6M21.5 12a9 9 0 0 1-15.46 6.35L2.5 16" />
                        </svg>
                      </button>
                    ) : null}
                  </div>
                </div>
                <div className={`admin-table-surface ${styles.itemsTable}`}>
                  <BzTable
                    columns={itemColumns}
                    data={currentItems}
                    rowKey="id"
                    emptyText="暂无字典项"
                    size="small"
                    rowClassName={(row) =>
                      itemDragTargetId === row.id ? "is-drag-target" : undefined
                    }
                    onRowDragOver={(event, row) => dragOverItem(event, row.id)}
                    onRowDrop={(event, row) => void dropItem(event, row.id)}
                  />
                </div>
              </section>
            </div>
          ) : null}
        </AdminEntityDrawer>

        <BzDialog
          modelValue={itemEditorOpen}
          title={itemEditorMode === "create" ? "新增字典项" : "编辑字典项"}
          width={560}
          closeOnOverlay={false}
          onConfirm={() => void saveItem()}
          onClose={() => setItemEditorOpen(false)}
          onUpdateModelValue={setItemEditorOpen}
          footer={
            <>
              <BzButton onClick={() => setItemEditorOpen(false)}>取消</BzButton>
              <BzButton
                buttonType="primary"
                loading={savingItem}
                onClick={() => void saveItem()}
              >
                保存
              </BzButton>
            </>
          }
        >
          <BzForm onSubmit={(event) => event.preventDefault()}>
            <div className={`group-form-grid ${styles.itemEditorForm}`}>
              <BzFormItem label="编码">
                <BzTextField
                  modelValue={itemForm.code}
                  maxlength={128}
                  readonly={itemEditorMode === "edit"}
                  onValueChange={(code) => setItemForm((prev) => ({ ...prev, code }))}
                />
              </BzFormItem>
              <BzFormItem label="标签">
                <BzTextField
                  modelValue={itemForm.label}
                  maxlength={128}
                  onValueChange={(value) => setItemForm((prev) => ({ ...prev, label: value }))}
                />
              </BzFormItem>
              <BzFormItem label="值">
                <BzTextField
                  modelValue={itemForm.value}
                  maxlength={512}
                  onValueChange={(value) => setItemForm((prev) => ({ ...prev, value }))}
                />
              </BzFormItem>
              <BzFormItem label="标签类型">
                <BzSelect
                  modelValue={itemForm.tagType || undefined}
                  placeholder="无"
                  onValueChange={(value) =>
                    setItemForm((prev) => ({ ...prev, tagType: value || "" }))
                  }
                >
                  {tagTypeOptions.map((item) => (
                    <BzOption
                      key={item.value}
                      label={item.label}
                      value={item.value}
                    />
                  ))}
                </BzSelect>
              </BzFormItem>
              <BzFormItem label="启用状态">
                <BzSwitch
                  modelValue={itemForm.enabled}
                  activeText="启用"
                  inactiveText="停用"
                  onValueChange={(value) => setItemForm((prev) => ({ ...prev, enabled: value }))}
                />
              </BzFormItem>
              <BzFormItem
                label="备注"
                className="group-form-grid__wide"
              >
                <BzTextField
                  modelValue={itemForm.description}
                  type="textarea"
                  rows={3}
                  maxlength={255}
                  showCounter
                  onValueChange={(value) =>
                    setItemForm((prev) => ({ ...prev, description: value }))
                  }
                />
              </BzFormItem>
            </div>
          </BzForm>
        </BzDialog>
      </div>
    </div>
  );

  function getTypeRowActions(row: DictTypeItem): AdminActionItem[] {
    const actions: AdminActionItem[] = [];
    if (canView || canEdit) {
      actions.push({
        key: `detail-${row.id}`,
        label: "详情",
        level: "default",
        onClick: () => void loadDrawer(row.id, "detail"),
      });
    }
    if (canEdit) {
      const disabled = row.sourceType === "BUILTIN";
      actions.push({
        key: `edit-${row.id}`,
        label: "编辑",
        level: "primary",
        disabled,
        onClick: () => void loadDrawer(row.id, "edit"),
      });
      actions.push({
        key: `status-${row.id}`,
        label: row.enabled ? "停用" : "启用",
        level: row.enabled ? "warning" : "success",
        disabled,
        onClick: () => void handleTypeStatus(row),
      });
      actions.push({
        key: `delete-${row.id}`,
        label: "删除",
        level: "danger",
        disabled,
        onClick: () => void handleDeleteType(row),
      });
    }
    return actions;
  }

  function getItemRowActions(row: DictItem): AdminActionItem[] {
    return [
      {
        key: `edit-item-${row.id}`,
        label: "编辑",
        level: "primary",
        onClick: () => openEditItem(row),
      },
      {
        key: `status-item-${row.id}`,
        label: row.enabled ? "停用" : "启用",
        level: row.enabled ? "warning" : "success",
        onClick: () => void handleItemStatus(row),
      },
      {
        key: `delete-item-${row.id}`,
        label: "删除",
        level: "danger",
        onClick: () => handleDeleteItem(row),
      },
    ];
  }
}

function resolveValueTypeLabel(value: DictValueType) {
  return valueTypeOptions.find((item) => item.value === value)?.label || value;
}

function resolveStructureTypeLabel(value: DictStructureType) {
  return structureTypeOptions.find((item) => item.value === value)?.label || value;
}

function isAbortError(cause: unknown) {
  return cause instanceof Error && cause.name === "AbortError";
}
