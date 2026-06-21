"use client";

import {
  createDictItem,
  deleteDictItem,
  listDictItems,
  listDictTypes,
  updateDictItem,
} from "@admin/api/dicts";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/permissions-registry";
import type { DictItem, DictTypeItem } from "@admin/types/dict-admin";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzDialog } from "../bz/BzDialog";
import { BzEmpty } from "../bz/BzEmpty";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzInputNumber } from "../bz/BzInputNumber";
import { BzLoading } from "../bz/BzLoading";
import { BzOption } from "../bz/BzOption";
import { BzSelect } from "../bz/BzSelect";
import { BzSwitch } from "../bz/BzSwitch";
import { BzTable, type BzTableColumn } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";
import { BzTextField } from "../bz/BzTextField";
import { BzTree } from "../bz/BzTree";

interface TreeDictType {
  id: string;
  label: string;
  code: string;
}

const TAG_TYPE_LABEL_MAP: Record<string, string> = {
  info: "信息",
  success: "成功",
  warning: "警告",
  danger: "危险",
};

const TAG_TYPE_OPTIONS = Object.entries(TAG_TYPE_LABEL_MAP).map(([value, label]) => ({
  value,
  label,
}));

function safeTrim(value: string): string {
  return value.trim();
}

function nextSortNo(items: DictItem[]): number {
  if (items.length === 0) return 1;
  return Math.max(...items.map((item) => item.sortNo)) + 1;
}

export function DictAdminPage() {
  const canView = hasResourceCodeAccess("dict-manage-view");
  const canEdit = hasResourceCodeAccess("dict-manage-edit");

  const [types, setTypes] = useState<DictTypeItem[]>([]);
  const [typesLoading, setTypesLoading] = useState(false);
  const [typeSearch, setTypeSearch] = useState("");
  const [selectedTypeId, setSelectedTypeId] = useState("");

  const [items, setItems] = useState<DictItem[]>([]);
  const [itemsLoading, setItemsLoading] = useState(false);

  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const pageSizeOptions = [10, 20, 30, 50, 100];

  const [editorOpen, setEditorOpen] = useState(false);
  const [editorMode, setEditorMode] = useState<"create" | "edit">("create");
  const [itemForm, setItemForm] = useState({
    id: "",
    label: "",
    value: "",
    tagType: "",
    sort: 0,
    remark: "",
    enabled: true,
  });
  const [editorSaving, setEditorSaving] = useState(false);

  const selectedType = useMemo(
    () => types.find((t) => t.id === selectedTypeId) ?? null,
    [types, selectedTypeId],
  );

  const selectedTypeRef = useRef(selectedType);
  selectedTypeRef.current = selectedType;

  const loadTypes = useCallback(async () => {
    setTypesLoading(true);
    try {
      const result = await listDictTypes({
        page: { pageNo: 1, pageSize: 1000 },
      });
      setTypes(result.elements);
    } finally {
      setTypesLoading(false);
    }
  }, []);

  useEffect(() => {
    if (canView || canEdit) {
      loadTypes();
    }
  }, [canView, canEdit, loadTypes]);

  const filteredTypes = useMemo(() => {
    const kw = safeTrim(typeSearch).toLowerCase();
    if (!kw) return types;
    return types.filter(
      (t) => t.name.toLowerCase().includes(kw) || t.code.toLowerCase().includes(kw),
    );
  }, [types, typeSearch]);

  const treeData = useMemo<TreeDictType[]>(
    () =>
      filteredTypes.map((t) => ({
        id: t.id,
        label: t.name,
        code: t.code,
      })),
    [filteredTypes],
  );

  const loadItems = useCallback(async (typeId: string) => {
    if (!typeId) {
      setItems([]);
      return;
    }
    setItemsLoading(true);
    try {
      const result = await listDictItems(typeId);
      setItems(result);
      setPageNo(1);
    } finally {
      setItemsLoading(false);
    }
  }, []);

  const selectType = useCallback(
    (typeId: string) => {
      setSelectedTypeId(typeId);
      loadItems(typeId);
    },
    [loadItems],
  );

  const refreshItems = useCallback(() => {
    if (selectedTypeRef.current) {
      loadItems(selectedTypeRef.current.id);
    }
  }, [loadItems]);

  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const isFirstPage = pageNo <= 1;
  const isLastPage = pageNo >= totalPages;

  const paginatedItems = useMemo(() => {
    const start = (pageNo - 1) * pageSize;
    return items.slice(start, start + pageSize);
  }, [items, pageNo, pageSize]);

  const pageTokens = useMemo<Array<number | "ellipsis">>(() => {
    const total = totalPages;
    const current = Math.min(Math.max(pageNo, 1), total);
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", total];
    if (current >= total - 3)
      return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
    return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
  }, [totalPages, pageNo]);

  const goToPage = useCallback(
    (target: number) => {
      const next = Math.min(Math.max(target, 1), totalPages);
      if (next !== pageNo) setPageNo(next);
    },
    [totalPages, pageNo],
  );

  const openCreate = useCallback(() => {
    setEditorMode("create");
    setItemForm({
      id: "",
      label: "",
      value: "",
      tagType: "",
      sort: nextSortNo(items),
      remark: "",
      enabled: true,
    });
    setEditorOpen(true);
  }, [items]);

  const openEdit = useCallback((item: DictItem) => {
    setEditorMode("edit");
    setItemForm({
      id: item.id,
      label: item.itemLabel,
      value: item.itemValue,
      tagType: item.tagType || "",
      sort: item.sortNo,
      remark: item.description || "",
      enabled: item.enabled,
    });
    setEditorOpen(true);
  }, []);

  const saveItem = useCallback(async () => {
    const type = selectedTypeRef.current;
    if (!type) return;
    if (!safeTrim(itemForm.label) || !safeTrim(itemForm.value)) {
      message.error("请完整填写标签和值");
      return;
    }

    setEditorSaving(true);
    try {
      if (editorMode === "create") {
        const itemCode = `item-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
        await createDictItem(type.id, {
          parentItemId: null,
          itemCode,
          itemLabel: safeTrim(itemForm.label),
          itemValue: safeTrim(itemForm.value),
          sortNo: itemForm.sort,
          enabled: itemForm.enabled,
          defaultItem: false,
          tagColor: null,
          tagType: safeTrim(itemForm.tagType) || null,
          extraJson: null,
          description: safeTrim(itemForm.remark) || null,
        });
        message.success("添加成功");
      } else {
        await updateDictItem(itemForm.id, {
          parentItemId: null,
          itemLabel: safeTrim(itemForm.label),
          itemValue: safeTrim(itemForm.value),
          sortNo: itemForm.sort,
          enabled: itemForm.enabled,
          defaultItem: false,
          tagColor: null,
          tagType: safeTrim(itemForm.tagType) || null,
          extraJson: null,
          description: safeTrim(itemForm.remark) || null,
        });
        message.success("保存成功");
      }
      setEditorOpen(false);
      refreshItems();
    } finally {
      setEditorSaving(false);
    }
  }, [itemForm, editorMode, refreshItems]);

  const removeItem = useCallback(
    async (item: DictItem) => {
      const confirmed = await bzConfirm({
        title: "删除字典项",
        content: `确认删除字典项 ${item.itemLabel}？`,
        confirmText: "删除",
        cancelText: "取消",
      });
      if (!confirmed) return;
      await deleteDictItem(item.id);
      message.success("已删除");
      refreshItems();
    },
    [refreshItems],
  );

  const columns = useMemo<BzTableColumn<DictItem>[]>(
    () => [
      {
        key: "itemLabel",
        title: "标签",
        minWidth: 130,
        render: (row) => <span style={{ fontWeight: 700 }}>{row.itemLabel}</span>,
      },
      {
        key: "itemValue",
        title: "值",
        minWidth: 130,
        className: "mono subdued",
        render: (row) => row.itemValue,
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
              {TAG_TYPE_LABEL_MAP[row.tagType] || row.tagType}
            </BzTag>
          ) : (
            <span style={{ color: "#94a3b8" }}>-</span>
          ),
      },
      {
        key: "sortNo",
        title: "排序",
        width: 80,
        render: (row) => row.sortNo,
      },
      {
        key: "description",
        title: "备注",
        minWidth: 140,
        render: (row) => (
          <span
            style={{
              color: "#64748b",
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
              display: "block",
            }}
          >
            {row.description || "-"}
          </span>
        ),
      },
      {
        key: "enabled",
        title: "状态",
        width: 80,
        render: (row) =>
          row.enabled ? (
            <BzTag
              size="small"
              type="success"
            >
              启用
            </BzTag>
          ) : (
            <BzTag
              size="small"
              type="warning"
            >
              停用
            </BzTag>
          ),
      },
      ...(canEdit
        ? [
            {
              key: "actions" as const,
              title: "操作",
              width: 140,
              render: (row: DictItem) => (
                <div
                  style={{
                    display: "flex",
                    gap: 8,
                    flexWrap: "wrap",
                  }}
                >
                  <BzButton
                    size="small"
                    link
                    onClick={() => openEdit(row)}
                  >
                    编辑
                  </BzButton>
                  <BzButton
                    size="small"
                    link
                    buttonType="danger"
                    onClick={() => removeItem(row)}
                  >
                    删除
                  </BzButton>
                </div>
              ),
            },
          ]
        : []),
    ],
    [canEdit, openEdit, removeItem],
  );

  if (!canView && !canEdit) {
    return (
      <div className="admin-page">
        <div className="content">
          <BzEmpty description="暂无权限访问字典管理" />
        </div>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="content">
        <div className="dict-admin-layout">
          <BzCard
            className="admin-panel dict-type-panel"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">字典类型</div>
              </div>
            }
          >
            <div className="dict-type-search">
              <BzInput
                modelValue={typeSearch}
                placeholder="搜索字典类型"
                clearable
                onValueChange={setTypeSearch}
              />
            </div>
            <div className="dict-type-tree">
              <BzLoading loading={typesLoading}>
                {treeData.length > 0 ? (
                  <BzTree
                    data={treeData}
                    nodeKey="id"
                    defaultExpandAll
                  >
                    {(node) => {
                      const isSelected = selectedTypeId === node.id;
                      return (
                        <button
                          type="button"
                          className={["dict-type-tree-item", isSelected ? "is-selected" : ""]
                            .filter(Boolean)
                            .join(" ")}
                          onClick={() => selectType(String(node.id))}
                        >
                          <span className="dict-type-tree-item__name">
                            {String(node.label ?? "-")}
                          </span>
                          <span className="dict-type-tree-item__code">
                            {String(node.code ?? "")}
                          </span>
                        </button>
                      );
                    }}
                  </BzTree>
                ) : (
                  <BzEmpty description="暂无字典类型" />
                )}
              </BzLoading>
            </div>
          </BzCard>

          <BzCard
            className="admin-panel dict-items-panel"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">
                  {selectedType ? `${selectedType.name} - 字典项` : "字典项"}
                </div>
                <div className="admin-table-tools">
                  {canEdit && selectedType ? (
                    <BzButton
                      className="admin-toolbar-primary"
                      buttonType="primary"
                      onClick={openCreate}
                    >
                      新增
                    </BzButton>
                  ) : null}
                  {selectedType ? (
                    <button
                      className="admin-vben-circle-button"
                      type="button"
                      title="刷新列表"
                      onClick={refreshItems}
                    >
                      <i
                        className="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh"
                        aria-hidden="true"
                      />
                    </button>
                  ) : null}
                </div>
              </div>
            }
          >
            <div className="admin-table-surface">
              <BzTable
                data={paginatedItems}
                columns={columns}
                rowKey="id"
                loading={itemsLoading}
                emptyText={selectedType ? "暂无字典项" : "请在左侧选择字典类型"}
                size="small"
              />
            </div>

            {items.length > 0 ? (
              <div className="dict-pagination-bar">
                <div className="dict-pagination-summary">共 {items.length} 条记录</div>
                <div className="dict-pagination-right">
                  <label className="dict-page-size">
                    <select
                      className="dict-page-size__select"
                      value={pageSize}
                      onChange={(e) => {
                        setPageSize(Number(e.target.value));
                        setPageNo(1);
                      }}
                    >
                      {pageSizeOptions.map((s) => (
                        <option
                          key={s}
                          value={s}
                        >
                          {s}条/页
                        </option>
                      ))}
                    </select>
                  </label>
                  <div className="dict-page-list">
                    <button
                      className="dict-page-btn dict-page-btn--icon"
                      type="button"
                      disabled={isFirstPage}
                      onClick={() => goToPage(1)}
                    >
                      <span aria-hidden="true">|&lt;</span>
                    </button>
                    <button
                      className="dict-page-btn dict-page-btn--icon"
                      type="button"
                      disabled={isFirstPage}
                      onClick={() => goToPage(pageNo - 1)}
                    >
                      <span aria-hidden="true">&lt;</span>
                    </button>
                    {pageTokens.map((token, i) =>
                      typeof token === "number" ? (
                        <button
                          key={i}
                          className={`dict-page-btn${token === pageNo ? " is-active" : ""}`}
                          type="button"
                          onClick={() => goToPage(token)}
                        >
                          {token}
                        </button>
                      ) : (
                        <span
                          key={i}
                          className="dict-page-ellipsis"
                        >
                          ...
                        </span>
                      ),
                    )}
                    <button
                      className="dict-page-btn dict-page-btn--icon"
                      type="button"
                      disabled={isLastPage}
                      onClick={() => goToPage(pageNo + 1)}
                    >
                      <span aria-hidden="true">&gt;</span>
                    </button>
                    <button
                      className="dict-page-btn dict-page-btn--icon"
                      type="button"
                      disabled={isLastPage}
                      onClick={() => goToPage(totalPages)}
                    >
                      <span aria-hidden="true">&gt;|</span>
                    </button>
                  </div>
                </div>
              </div>
            ) : null}
          </BzCard>
        </div>

        <BzDialog
          modelValue={editorOpen}
          title={editorMode === "create" ? "新增字典项" : "编辑字典项"}
          width={560}
          confirmText="保存"
          cancelText="取消"
          closeOnOverlay={false}
          onConfirm={saveItem}
          onClose={() => setEditorOpen(false)}
          onUpdateModelValue={setEditorOpen}
          footer={
            <>
              <BzButton onClick={() => setEditorOpen(false)}>取消</BzButton>
              <BzButton
                buttonType="primary"
                loading={editorSaving}
                onClick={saveItem}
              >
                保存
              </BzButton>
            </>
          }
        >
          <BzForm onSubmit={(e) => e.preventDefault()}>
            <div className="dict-form-grid">
              <BzFormItem label="标签">
                <BzTextField
                  modelValue={itemForm.label}
                  placeholder="显示名称"
                  maxlength={128}
                  onValueChange={(v) => setItemForm((prev) => ({ ...prev, label: v }))}
                />
              </BzFormItem>
              <BzFormItem label="值">
                <BzTextField
                  modelValue={itemForm.value}
                  placeholder="实际值"
                  maxlength={512}
                  onValueChange={(v) => setItemForm((prev) => ({ ...prev, value: v }))}
                />
              </BzFormItem>
              <BzFormItem label="标签类型">
                <BzSelect
                  modelValue={itemForm.tagType || undefined}
                  placeholder="无"
                  clearable
                  onValueChange={(v) => setItemForm((prev) => ({ ...prev, tagType: v ?? "" }))}
                >
                  {TAG_TYPE_OPTIONS.map((o) => (
                    <BzOption
                      key={o.value}
                      label={o.label}
                      value={o.value}
                    />
                  ))}
                </BzSelect>
              </BzFormItem>
              <BzFormItem label="排序">
                <BzInputNumber
                  modelValue={itemForm.sort}
                  min={0}
                  onValueChange={(v) => setItemForm((prev) => ({ ...prev, sort: v }))}
                />
              </BzFormItem>
            </div>
            <div className="dict-switch-grid">
              <BzFormItem label="启用状态">
                <BzSwitch
                  modelValue={itemForm.enabled}
                  activeText="启用"
                  inactiveText="停用"
                  onValueChange={(v) => setItemForm((prev) => ({ ...prev, enabled: v }))}
                />
              </BzFormItem>
            </div>
            <div className="dict-textarea-stack">
              <BzFormItem label="备注">
                <BzTextField
                  modelValue={itemForm.remark}
                  placeholder="备注说明"
                  type="textarea"
                  rows={3}
                  maxlength={255}
                  showCounter
                  onValueChange={(v) => setItemForm((prev) => ({ ...prev, remark: v }))}
                />
              </BzFormItem>
            </div>
          </BzForm>
        </BzDialog>
      </div>
    </div>
  );
}
