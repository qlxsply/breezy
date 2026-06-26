"use client";

import {
  createDictItem,
  deleteDictItem,
  deleteDictType,
  getDictType,
  listDictItems,
  listDictTypes,
  updateDictItem,
  updateDictItemStatus,
  updateDictType,
  updateDictTypeStatus,
  validateDisableDict,
  validateDisableDictItem,
} from "@admin/api/dicts";
import { AdminActionBar } from "@admin/components/admin/AdminActionBar";
import { AdminEntityDrawer } from "@admin/components/admin/AdminEntityDrawer";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { useAdminQueryPanelLayout } from "@admin/components/admin/useAdminQueryPanelLayout";
import {
  BzButton,
  BzCard,
  BzDialog,
  BzEmpty,
  BzForm,
  BzFormItem,
  BzInput,
  BzInputNumber,
  BzOption,
  BzPagination,
  BzSelect,
  BzSwitch,
  BzTable,
  type BzTableColumn,
  BzTag,
  BzTextField,
} from "@admin/components/bz";
import { bzConfirm } from "@admin/core/confirm";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { DictItem, DictStructureType, DictTypeItem, DictValueType } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import { useEffect, useMemo, useRef, useState } from "react";

type DrawerMode = "detail" | "edit";
type ItemEditorMode = "create" | "edit";

const pageSizeOptions = [10, 20, 30, 50, 100];

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

export function DictAdminPage() {
  const canView = hasResourceCodeAccess("dict-manage-view");
  const canEdit = hasResourceCodeAccess("dict-manage-edit");

  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState<PageResult<DictTypeItem>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [codeDraft, setCodeDraft] = useState("");
  const [nameDraft, setNameDraft] = useState("");
  const [appliedCode, setAppliedCode] = useState("");
  const [appliedName, setAppliedName] = useState("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerLoading, setDrawerLoading] = useState(false);
  const [drawerMode, setDrawerMode] = useState<DrawerMode>("detail");
  const [currentType, setCurrentType] = useState<DictTypeItem | null>(null);
  const [currentItems, setCurrentItems] = useState<DictItem[]>([]);
  const [typeForm, setTypeForm] = useState({
    name: "",
    description: "",
    enumClass: "",
    valueType: "STRING" as DictValueType,
    structureType: "FLAT" as DictStructureType,
    enabled: true,
  });
  const [savingType, setSavingType] = useState(false);

  const [itemEditorOpen, setItemEditorOpen] = useState(false);
  const [itemEditorMode, setItemEditorMode] = useState<ItemEditorMode>("create");
  const [editingItemId, setEditingItemId] = useState("");
  const [itemForm, setItemForm] = useState({
    label: "",
    value: "",
    tagType: "",
    sortNo: 1,
    enabled: true,
    description: "",
  });
  const [savingItem, setSavingItem] = useState(false);

  const currentTypeRef = useRef<DictTypeItem | null>(null);
  currentTypeRef.current = currentType;

  const rows = page.elements;

  useEffect(() => {
    if (!canView && !canEdit) return;
    void reload();
  }, [appliedCode, appliedName, pageNo, pageSize, canView, canEdit]);

  async function reload() {
    setLoading(true);
    try {
      const nextPage = await listDictTypes({
        code: appliedCode || undefined,
        name: appliedName || undefined,
        page: { pageNo, pageSize },
      });
      setPage(nextPage);
    } finally {
      setLoading(false);
    }
  }

  function applyFilters() {
    setAppliedCode(codeDraft.trim());
    setAppliedName(nameDraft.trim());
    setPageNo(1);
  }

  function resetFilters() {
    setCodeDraft("");
    setNameDraft("");
    setAppliedCode("");
    setAppliedName("");
    setPageNo(1);
  }

  async function loadDrawer(typeId: string, mode: DrawerMode) {
    setDrawerMode(mode);
    setDrawerOpen(true);
    setDrawerLoading(true);
    try {
      const [type, items] = await Promise.all([getDictType(typeId), listDictItems(typeId)]);
      setCurrentType(type);
      setCurrentItems(items);
      setTypeForm({
        name: type.name,
        description: type.description || "",
        enumClass: type.enumClass || "",
        valueType: type.valueType,
        structureType: type.structureType,
        enabled: type.enabled,
      });
    } finally {
      setDrawerLoading(false);
    }
  }

  function closeDrawer() {
    setDrawerOpen(false);
    setCurrentType(null);
    setCurrentItems([]);
    setItemEditorOpen(false);
  }

  async function refreshDrawerItems() {
    const type = currentTypeRef.current;
    if (!type) return;
    const [nextType, nextItems] = await Promise.all([getDictType(type.id), listDictItems(type.id)]);
    setCurrentType(nextType);
    setCurrentItems(nextItems);
    setTypeForm((prev) => ({
      ...prev,
      enabled: nextType.enabled,
    }));
  }

  async function handleTypeStatus(row: DictTypeItem) {
    if (!canEdit) return;
    if (row.enabled) {
      const validation = await validateDisableDict(row.code);
      if (!validation.allowed) {
        message.warning("该字典存在使用引用，当前无法停用");
        return;
      }
    }
    await updateDictTypeStatus(row.id, !row.enabled);
    message.success(!row.enabled ? "已启用" : "已停用");
    await reload();
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
    await reload();
  }

  async function submitType() {
    const type = currentTypeRef.current;
    if (!type) return;
    const name = safeTrim(typeForm.name);
    if (!name) {
      message.warning("字典名称不能为空");
      return;
    }
    setSavingType(true);
    try {
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
      closeDrawer();
      await reload();
    } finally {
      setSavingType(false);
    }
  }

  function openCreateItem() {
    setItemEditorMode("create");
    setEditingItemId("");
    setItemForm({
      label: "",
      value: "",
      tagType: "",
      sortNo: nextSortNo(currentItems),
      enabled: true,
      description: "",
    });
    setItemEditorOpen(true);
  }

  function openEditItem(item: DictItem) {
    setItemEditorMode("edit");
    setEditingItemId(item.id);
    setItemForm({
      label: item.itemLabel,
      value: item.itemValue,
      tagType: item.tagType || "",
      sortNo: item.sortNo,
      enabled: item.enabled,
      description: item.description || "",
    });
    setItemEditorOpen(true);
  }

  async function saveItem() {
    const type = currentTypeRef.current;
    if (!type) return;
    const label = safeTrim(itemForm.label);
    const value = safeTrim(itemForm.value);
    if (!label || !value) {
      message.warning("请完整填写标签和值");
      return;
    }
    setSavingItem(true);
    try {
      if (itemEditorMode === "create") {
        const itemCode = `item-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
        await createDictItem(type.id, {
          parentItemId: null,
          itemCode,
          itemLabel: label,
          itemValue: value,
          sortNo: itemForm.sortNo,
          enabled: itemForm.enabled,
          defaultItem: false,
          tagColor: null,
          tagType: safeTrim(itemForm.tagType) || null,
          extraJson: null,
          description: safeTrim(itemForm.description) || null,
        });
        message.success("添加成功");
      } else {
        await updateDictItem(editingItemId, {
          parentItemId: null,
          itemLabel: label,
          itemValue: value,
          sortNo: itemForm.sortNo,
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
    if (!type || !canEdit) return;
    if (item.enabled) {
      const validation = await validateDisableDictItem(type.code, item.id);
      if (!validation.allowed) {
        message.warning("该字典项存在使用引用，当前无法停用");
        return;
      }
    }
    await updateDictItemStatus(item.id, !item.enabled);
    message.success(!item.enabled ? "已启用" : "已停用");
    await refreshDrawerItems();
  }

  async function handleDeleteItem(item: DictItem) {
    const confirmed = await bzConfirm({
      title: "删除字典项",
      content: `确认删除字典项 ${item.itemLabel}？`,
      confirmText: "删除",
      cancelText: "取消",
    });
    if (!confirmed) return;
    await deleteDictItem(item.id);
    message.success("删除成功");
    await refreshDrawerItems();
  }

  const columns = useMemo<Array<BzTableColumn<DictTypeItem>>>(
    () => [
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
        render: (row) => <BzTag type={row.enabled ? "success" : "danger"}>{row.enabled ? "启用" : "停用"}</BzTag>,
      },
      {
        key: "description",
        title: "描述",
        minWidth: 220,
        render: (row) => (
          <span style={{ display: "block", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
            {row.description || "-"}
          </span>
        ),
      },
      {
        key: "actions",
        title: "操作",
        width: 210,
        render: (row) => <AdminActionBar actions={getTypeRowActions(row)} />,
      },
    ],
    [canView, canEdit, drawerOpen],
  );

  const itemColumns = useMemo<Array<BzTableColumn<DictItem>>>(() => {
    const actionColumn: Array<BzTableColumn<DictItem>> =
      drawerMode === "edit" && canEdit
        ? [
            {
              key: "actions",
              title: "操作",
              width: 180,
              render: (row) => <AdminActionBar actions={getItemRowActions(row)} />,
            },
          ]
        : [];

    return [
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
            <BzTag size="small" type={row.tagType as "info" | "success" | "warning" | "danger"}>
              {tagTypeLabelMap[row.tagType] || row.tagType}
            </BzTag>
          ) : (
            <>-</>
          ),
      },
      {
        key: "sortNo",
        title: "排序",
        width: 80,
        render: (row) => <>{row.sortNo}</>,
      },
      {
        key: "enabled",
        title: "状态",
        width: 90,
        render: (row) => <BzTag type={row.enabled ? "success" : "danger"}>{row.enabled ? "启用" : "停用"}</BzTag>,
      },
      {
        key: "description",
        title: "备注",
        minWidth: 180,
        render: (row) => (
          <span style={{ display: "block", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
            {row.description || "-"}
          </span>
        ),
      },
      ...actionColumn,
    ];
  }, [drawerMode, canEdit, currentItems]);

  const drawerTitle = drawerMode === "detail" ? "字典详情" : "编辑字典";

  const drawerFooter = (
    <>
      <BzButton onClick={closeDrawer}>{drawerMode === "detail" ? "关闭" : "取消"}</BzButton>
      {drawerMode === "edit" ? (
        <BzButton buttonType="primary" loading={savingType} onClick={() => void submitType()}>
          确定
        </BzButton>
      ) : null}
    </>
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
        <div className="admin-page-stack">
          {queryPanelVisible ? (
            <BzCard className="admin-panel admin-filter-card" shadow="never">
              <div
                ref={queryCardRef}
                className={[
                  "admin-query-layout",
                  querySingleRow ? "is-single-row" : queryExpanded ? "is-expanded" : "is-collapsed",
                ].join(" ")}
              >
                <div className="admin-query-header">
                  <div className="admin-query-title">筛选条件</div>
                </div>
                <form
                  ref={queryGridRef}
                  className="bz-form admin-query-grid"
                  onSubmit={(event) => {
                    event.preventDefault();
                    applyFilters();
                  }}
                >
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">编码</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={codeDraft}
                        placeholder="请输入字典编码"
                        clearable
                        onValueChange={setCodeDraft}
                        onKeyUp={(event) => event.key === "Enter" && applyFilters()}
                      />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">名称</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={nameDraft}
                        placeholder="请输入字典名称"
                        clearable
                        onValueChange={setNameDraft}
                        onKeyUp={(event) => event.key === "Enter" && applyFilters()}
                      />
                    </div>
                  </BzFormItem>
                  <div className="admin-query-actions">
                    <BzButton className="admin-filter-secondary" nativeType="button" onClick={resetFilters}>
                      重置
                    </BzButton>
                    <BzButton className="admin-filter-primary" buttonType="primary" nativeType="button" onClick={applyFilters}>
                      搜索
                    </BzButton>
                    {!querySingleRow ? (
                      <button
                        className="admin-filter-toggle"
                        type="button"
                        aria-expanded={queryExpanded}
                        onClick={() => setQueryExpanded((value) => !value)}
                      >
                        <span>{queryExpanded ? "收起" : "展开"}</span>
                        <i
                          className={`admin-filter-toggle__icon ${queryExpanded ? "is-up" : "is-down"}`}
                          aria-hidden="true"
                        />
                      </button>
                    ) : null}
                  </div>
                </form>
              </div>
            </BzCard>
          ) : null}

          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">字典列表</div>
                <div className="admin-table-tools">
                  <AdminTableTools
                    queryPanelVisible={queryPanelVisible}
                    onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
                    onRefresh={() => void reload()}
                  />
                </div>
              </div>
            }
          >
            <div className="admin-table-surface">
              <BzTable columns={columns} data={rows} loading={loading} rowKey="id" emptyText="暂无字典记录" size="small" />
            </div>
            {page.totalElements > 0 ? (
              <div className="dict-pagination-bar">
                <div className="dict-pagination-summary">共 {page.totalElements} 条记录</div>
                <div className="dict-pagination-right">
                  <BzPagination
                    total={page.totalElements}
                    pageSize={pageSize}
                    currentPage={pageNo}
                    pageSizes={pageSizeOptions}
                    onCurrentChange={setPageNo}
                    onSizeChange={(size) => {
                      if (!Number.isFinite(size) || size <= 0 || size === pageSize) return;
                      setPageSize(size);
                      setPageNo(1);
                    }}
                  />
                </div>
              </div>
            ) : null}
          </BzCard>
        </div>

        <AdminEntityDrawer open={drawerOpen} title={drawerTitle} width="1120px" loading={drawerLoading} onClose={closeDrawer} footer={drawerFooter}>
          {currentType ? (
            <div style={{ display: "grid", gap: 20 }}>
              {drawerMode === "detail" ? (
                <div className="detail-grid">
                  <div className="detail-field">
                    <span className="detail-field__label">编码</span>
                    <span className="detail-field__value">{currentType.code}</span>
                  </div>
                  <div className="detail-field">
                    <span className="detail-field__label">名称</span>
                    <span className="detail-field__value">{currentType.name}</span>
                  </div>
                  <div className="detail-field">
                    <span className="detail-field__label">值类型</span>
                    <span className="detail-field__value">{resolveValueTypeLabel(currentType.valueType)}</span>
                  </div>
                  <div className="detail-field">
                    <span className="detail-field__label">结构</span>
                    <span className="detail-field__value">{resolveStructureTypeLabel(currentType.structureType)}</span>
                  </div>
                  <div className="detail-field">
                    <span className="detail-field__label">来源</span>
                    <span className="detail-field__value">{sourceTypeLabelMap[currentType.sourceType] || currentType.sourceType}</span>
                  </div>
                  <div className="detail-field">
                    <span className="detail-field__label">状态</span>
                    <span className="detail-field__value">{currentType.enabled ? "启用" : "停用"}</span>
                  </div>
                  <div className="detail-field detail-field--wide">
                    <span className="detail-field__label">枚举类</span>
                    <span className="detail-field__value">{currentType.enumClass || "-"}</span>
                  </div>
                  <div className="detail-field detail-field--wide">
                    <span className="detail-field__label">描述</span>
                    <span className="detail-field__value">{currentType.description || "-"}</span>
                  </div>
                </div>
              ) : (
                <BzForm>
                  <div className="group-form-grid">
                    <BzFormItem label="编码">
                      <BzInput modelValue={currentType.code} disabled />
                    </BzFormItem>
                    <BzFormItem label="名称">
                      <BzInput modelValue={typeForm.name} onValueChange={(value) => setTypeForm((prev) => ({ ...prev, name: value }))} />
                    </BzFormItem>
                    <BzFormItem label="值类型">
                      <BzSelect modelValue={typeForm.valueType} onValueChange={(value) => setTypeForm((prev) => ({ ...prev, valueType: (value || "STRING") as DictValueType }))}>
                        {valueTypeOptions.map((item) => (
                          <BzOption key={item.value} label={item.label} value={item.value} />
                        ))}
                      </BzSelect>
                    </BzFormItem>
                    <BzFormItem label="结构">
                      <BzSelect modelValue={typeForm.structureType} onValueChange={(value) => setTypeForm((prev) => ({ ...prev, structureType: (value || "FLAT") as DictStructureType }))}>
                        {structureTypeOptions.map((item) => (
                          <BzOption key={item.value} label={item.label} value={item.value} />
                        ))}
                      </BzSelect>
                    </BzFormItem>
                    <BzFormItem label="来源">
                      <BzInput modelValue={sourceTypeLabelMap[currentType.sourceType] || currentType.sourceType} disabled />
                    </BzFormItem>
                    <BzFormItem label="启用状态">
                      <BzSwitch modelValue={typeForm.enabled} activeText="启用" inactiveText="停用" onValueChange={(value) => setTypeForm((prev) => ({ ...prev, enabled: value }))} />
                    </BzFormItem>
                    <BzFormItem label="枚举类">
                      <BzInput modelValue={typeForm.enumClass} onValueChange={(value) => setTypeForm((prev) => ({ ...prev, enumClass: value }))} />
                    </BzFormItem>
                    <BzFormItem label="描述" className="group-form-grid__wide">
                      <BzTextField modelValue={typeForm.description} type="textarea" rows={3} maxlength={255} showCounter onValueChange={(value) => setTypeForm((prev) => ({ ...prev, description: value }))} />
                    </BzFormItem>
                  </div>
                </BzForm>
              )}

              <section style={{ display: "grid", gap: 12 }}>
                <div className="admin-table-header">
                  <div className="admin-table-title">字典项</div>
                  <div className="admin-table-tools">
                    {drawerMode === "edit" && canEdit ? (
                      <BzButton className="admin-toolbar-primary" buttonType="primary" onClick={openCreateItem}>
                        新增
                      </BzButton>
                    ) : null}
                    <button className="admin-vben-circle-button" type="button" title="刷新字典项" onClick={() => void refreshDrawerItems()}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21.5 2v6h-6M2.5 12a9 9 0 0 1 15.46-6.35L21.5 8" /><path d="M2.5 22v-6h6M21.5 12a9 9 0 0 1-15.46 6.35L2.5 16" /></svg>
                    </button>
                  </div>
                </div>
                <div className="admin-table-surface">
                  <BzTable columns={itemColumns} data={currentItems} rowKey="id" emptyText="暂无字典项" size="small" />
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
              <BzButton buttonType="primary" loading={savingItem} onClick={() => void saveItem()}>
                保存
              </BzButton>
            </>
          }
        >
          <BzForm onSubmit={(event) => event.preventDefault()}>
            <div className="group-form-grid">
              <BzFormItem label="标签">
                <BzTextField modelValue={itemForm.label} maxlength={128} onValueChange={(value) => setItemForm((prev) => ({ ...prev, label: value }))} />
              </BzFormItem>
              <BzFormItem label="值">
                <BzTextField modelValue={itemForm.value} maxlength={512} onValueChange={(value) => setItemForm((prev) => ({ ...prev, value }))} />
              </BzFormItem>
              <BzFormItem label="标签类型">
                <BzSelect modelValue={itemForm.tagType || undefined} placeholder="无" clearable onValueChange={(value) => setItemForm((prev) => ({ ...prev, tagType: value || "" }))}>
                  {tagTypeOptions.map((item) => (
                    <BzOption key={item.value} label={item.label} value={item.value} />
                  ))}
                </BzSelect>
              </BzFormItem>
              <BzFormItem label="排序">
                <BzInputNumber modelValue={itemForm.sortNo} min={0} onValueChange={(value) => setItemForm((prev) => ({ ...prev, sortNo: value }))} />
              </BzFormItem>
              <BzFormItem label="启用状态">
                <BzSwitch modelValue={itemForm.enabled} activeText="启用" inactiveText="停用" onValueChange={(value) => setItemForm((prev) => ({ ...prev, enabled: value }))} />
              </BzFormItem>
              <BzFormItem label="备注" className="group-form-grid__wide">
                <BzTextField modelValue={itemForm.description} type="textarea" rows={3} maxlength={255} showCounter onValueChange={(value) => setItemForm((prev) => ({ ...prev, description: value }))} />
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
      actions.push({ key: `detail-${row.id}`, label: "详情", tone: "detail", handler: () => void loadDrawer(row.id, "detail") });
    }
    if (canEdit && row.sourceType !== "BUILTIN") {
      actions.push({ key: `edit-${row.id}`, label: "编辑", tone: "edit", handler: () => void loadDrawer(row.id, "edit") });
    }
    if (canEdit && row.sourceType !== "BUILTIN") {
      actions.push({
        key: `status-${row.id}`,
        label: row.enabled ? "停用" : "启用",
        tone: row.enabled ? "disable" : "enable",
        handler: () => void handleTypeStatus(row),
      });
    }
    if (canEdit && row.sourceType !== "BUILTIN") {
      actions.push({ key: `delete-${row.id}`, label: "删除", tone: "delete", handler: () => void handleDeleteType(row) });
    }
    return actions;
  }

  function getItemRowActions(row: DictItem): AdminActionItem[] {
    return [
      { key: `edit-item-${row.id}`, label: "编辑", tone: "edit", handler: () => openEditItem(row) },
      {
        key: `status-item-${row.id}`,
        label: row.enabled ? "停用" : "启用",
        tone: row.enabled ? "disable" : "enable",
        handler: () => void handleItemStatus(row),
      },
      { key: `delete-item-${row.id}`, label: "删除", tone: "delete", handler: () => void handleDeleteItem(row) },
    ];
  }
}

function resolveValueTypeLabel(value: DictValueType) {
  return valueTypeOptions.find((item) => item.value === value)?.label || value;
}

function resolveStructureTypeLabel(value: DictStructureType) {
  return structureTypeOptions.find((item) => item.value === value)?.label || value;
}
