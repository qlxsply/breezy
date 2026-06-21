"use client";

import { useCallback, useEffect, useMemo, useState } from "react";

import {
  downloadSystemFile,
  fetchSystemFileView,
  getLogicalFilePhysicalDetail,
  listPhysicalFileLogicalRefs,
  listSystemNodes,
} from "@admin/api/system-files";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/registry/permissions.registry";
import type {
  PhysicalFileDetail,
  StorageListQuery,
  StorageSortBy,
  StorageSortOrder,
  StorageViewMode,
  SystemFileItem,
} from "@admin/types/file-storage";
import { BzAlert } from "../bz/BzAlert";
import { BzButton } from "../bz/BzButton";
import { BzButtonGroup } from "../bz/BzButtonGroup";
import { BzCard } from "../bz/BzCard";
import { BzDropdown } from "../bz/BzDropdown";
import { BzDropdownItem } from "../bz/BzDropdownItem";
import { BzDropdownMenu } from "../bz/BzDropdownMenu";
import { BzEmpty } from "../bz/BzEmpty";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzLoading } from "../bz/BzLoading";
import { BzOption } from "../bz/BzOption";
import { BzSelect } from "../bz/BzSelect";
import { BzTable, type BzTableColumn } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

const PREVIEW_SIZE_LIMIT = 5 * 1024 * 1024;

type RowActionType = "primary" | "success" | "warning" | "danger" | "default";

interface RowAction {
  key: string;
  label: string;
  type?: RowActionType;
  disabled?: boolean;
  handler: () => void;
}

function formatSize(size?: string | number | null): string {
  const bytes = typeof size === "string" ? Number(size) : size;
  if (!bytes || !Number.isFinite(bytes) || bytes <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  let value = bytes;
  let index = 0;
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024;
    index += 1;
  }
  return `${value.toFixed(index === 0 ? 0 : 2)} ${units[index]}`;
}

function formatDateTime(value: string): string {
  if (!value) return "-";
  try {
    const date = new Date(value);
    if (isNaN(date.getTime())) return "-";
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    const h = String(date.getHours()).padStart(2, "0");
    const min = String(date.getMinutes()).padStart(2, "0");
    const s = String(date.getSeconds()).padStart(2, "0");
    return `${y}-${m}-${d} ${h}:${min}:${s}`;
  } catch {
    return "-";
  }
}

function extractErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return fallback;
}

function extractExtension(fileName: string): string {
  if (!fileName) return "";
  const idx = fileName.lastIndexOf(".");
  if (idx < 0 || idx === fileName.length - 1) return "";
  return fileName.substring(idx + 1).toLowerCase();
}

function resolvePreviewType(fileName: string, contentType: string): string {
  if (contentType) {
    return contentType.toLowerCase();
  }
  const extension = extractExtension(fileName);
  if (!extension) return "";
  if (["png", "jpg", "jpeg", "gif", "webp", "bmp", "svg"].includes(extension)) {
    return `image/${extension === "jpg" ? "jpeg" : extension}`;
  }
  if (["json"].includes(extension)) return "application/json";
  if (["xml"].includes(extension)) return "application/xml";
  if (["yaml", "yml"].includes(extension)) return "application/yaml";
  if (["csv"].includes(extension)) return "text/csv";
  if (["md", "txt", "log", "sql"].includes(extension)) return "text/plain";
  return "";
}

function isTextType(contentType: string): boolean {
  if (!contentType) return false;
  const normalized = contentType.toLowerCase().split(";")[0]!.trim();
  if (normalized.startsWith("text/")) return true;
  return ["application/json", "application/xml", "application/yaml", "application/sql"].includes(normalized);
}

export function SystemFilesPage() {
  const canAdmin = hasResourceCodeAccess("system-file-admin-view");
  const canPhysical = hasResourceCodeAccess("system-file-physical-view");
  const canReverse = hasResourceCodeAccess("system-file-reference-view");
  const canView = hasResourceCodeAccess("system-file-preview");
  const canDownload = hasResourceCodeAccess("system-file-download");

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [items, setItems] = useState<SystemFileItem[]>([]);
  const [currentParentId, setCurrentParentId] = useState<string | undefined>(undefined);
  const [breadcrumbs, setBreadcrumbs] = useState<Array<{ id: string; name: string }>>([]);

  const [keywordInput, setKeywordInput] = useState("");
  const [activeKeyword, setActiveKeyword] = useState("");
  const [sortBy, setSortBy] = useState<StorageSortBy>("NAME");
  const [sortOrder, setSortOrder] = useState<StorageSortOrder>("ASC");
  const [viewMode, setViewMode] = useState<StorageViewMode>("LIST");

  const [previewUrl, setPreviewUrl] = useState("");
  const [previewName, setPreviewName] = useState("");
  const [previewContentType, setPreviewContentType] = useState("");
  const [previewText, setPreviewText] = useState("");
  const [previewResolvedType, setPreviewResolvedType] = useState("");

  const [physicalDetail, setPhysicalDetail] = useState<PhysicalFileDetail | null>(null);
  const [detailErrorMessage, setDetailErrorMessage] = useState("");
  const [reverseRefs, setReverseRefs] = useState<SystemFileItem[]>([]);
  const [refsLoading, setRefsLoading] = useState(false);
  const [refsErrorMessage, setRefsErrorMessage] = useState("");
  const [refsKeywordInput, setRefsKeywordInput] = useState("");
  const [refsKeyword, setRefsKeyword] = useState("");

  const previewIsImage = previewResolvedType.startsWith("image/");
  const previewIsText = isTextType(previewResolvedType);

  const currentPath = useMemo(() => {
    if (breadcrumbs.length === 0) return "/";
    return `/${breadcrumbs.map((b) => b.name).join("/")}`;
  }, [breadcrumbs]);

  const activeKeywordRef = { current: activeKeyword };
  activeKeywordRef.current = activeKeyword;

  const currentParentIdRef = { current: currentParentId };
  currentParentIdRef.current = currentParentId;

  const sortByRef = { current: sortBy };
  sortByRef.current = sortBy;

  const sortOrderRef = { current: sortOrder };
  sortOrderRef.current = sortOrder;

  const canPhysicalRef = { current: canPhysical };
  canPhysicalRef.current = canPhysical;

  const canReverseRef = { current: canReverse };
  canReverseRef.current = canReverse;

  const clearPreview = useCallback(() => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    setPreviewUrl("");
    setPreviewName("");
    setPreviewContentType("");
    setPreviewResolvedType("");
    setPreviewText("");
  }, [previewUrl]);

  const reload = useCallback(async () => {
    clearPreview();
    if (!canAdmin) {
      setItems([]);
      return;
    }
    setLoading(true);
    setErrorMessage("");
    try {
      const query: StorageListQuery = {
        parentId: currentParentIdRef.current,
        sortBy: sortByRef.current,
        sortOrder: sortOrderRef.current,
      };
      if (activeKeywordRef.current.trim()) {
        query.keyword = activeKeywordRef.current.trim();
        query.recursive = true;
      }
      setItems(await listSystemNodes(query));
    } catch (err) {
      setErrorMessage(extractErrorMessage(err, "文件列表加载失败"));
    } finally {
      setLoading(false);
    }
  }, [canAdmin, clearPreview]);

  const loadReverseRefs = useCallback(async () => {
    if (!canReverseRef.current || !physicalDetail) return;
    setRefsLoading(true);
    setRefsErrorMessage("");
    try {
      setReverseRefs(
        await listPhysicalFileLogicalRefs(
          physicalDetail.physicalFileId,
          refsKeyword,
          sortByRef.current,
          sortOrderRef.current,
        ),
      );
    } catch (err) {
      setReverseRefs([]);
      setRefsErrorMessage(extractErrorMessage(err, "反向引用查询失败"));
    } finally {
      setRefsLoading(false);
    }
  }, [physicalDetail, refsKeyword]);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    return () => clearPreview();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function applySearch() {
    setActiveKeyword(keywordInput.trim());
  }

  function clearSearch() {
    setKeywordInput("");
    setActiveKeyword("");
  }

  function enterFolder(folder: SystemFileItem) {
    const fromSearch = !!activeKeyword.trim();
    if (fromSearch) {
      setKeywordInput("");
      setActiveKeyword("");
      setBreadcrumbs([{ id: folder.id, name: folder.name }]);
    } else {
      setBreadcrumbs((prev) => [...prev, { id: folder.id, name: folder.name }]);
    }
    setCurrentParentId(folder.id);
  }

  function goRoot() {
    setBreadcrumbs([]);
    setCurrentParentId(undefined);
    setKeywordInput("");
    setActiveKeyword("");
    clearPhysicalDetail();
  }

  function goBack() {
    if (breadcrumbs.length === 0) return;
    const next = breadcrumbs.slice(0, -1);
    setBreadcrumbs(next);
    setCurrentParentId(next.length > 0 ? next[next.length - 1]!.id : undefined);
    setKeywordInput("");
    setActiveKeyword("");
  }

  async function previewFile(row: SystemFileItem) {
    if (!canView || row.type !== "FILE") return;
    clearPreview();
    const size = row.size ? Number(row.size) : 0;
    if (Number.isFinite(size) && size > PREVIEW_SIZE_LIMIT) {
      message.warning("文件过大，无法预览");
      return;
    }
    try {
      const blob = await fetchSystemFileView(row.id);
      setPreviewName(row.name);
      setPreviewContentType(row.contentType || blob.type || "");
      const resolved = resolvePreviewType(row.name, previewContentType || blob.type || "");
      setPreviewResolvedType(resolved);
      if (resolved) {
        setPreviewContentType(resolved);
      }
      if (resolved.startsWith("image/")) {
        setPreviewUrl(URL.createObjectURL(blob));
        setPreviewText("");
        return;
      }
      if (isTextType(resolved)) {
        setPreviewText(await blob.text());
        setPreviewUrl("");
        return;
      }
      setPreviewUrl("");
      setPreviewText("");
    } catch (err) {
      message.error(extractErrorMessage(err, "文件预览失败"));
    }
  }

  async function downloadFile(row: SystemFileItem) {
    if (!canDownload || row.type !== "FILE") return;
    try {
      const result = await downloadSystemFile(row.id, row.name);
      const url = URL.createObjectURL(result.blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = result.fileName || row.name;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      message.error(extractErrorMessage(err, "文件下载失败"));
    }
  }

  async function loadPhysicalDetail(item: SystemFileItem) {
    if (!canPhysicalRef.current || item.type !== "FILE") return;
    setDetailErrorMessage("");
    setRefsErrorMessage("");
    try {
      const detail = await getLogicalFilePhysicalDetail(item.id);
      setPhysicalDetail(detail);
      setRefsKeywordInput("");
      setRefsKeyword("");
      setReverseRefs([]);
      if (canReverseRef.current) {
        setRefsLoading(true);
        try {
          setReverseRefs(
            await listPhysicalFileLogicalRefs(
              detail.physicalFileId,
              "",
              sortByRef.current,
              sortOrderRef.current,
            ),
          );
        } catch (err) {
          setRefsErrorMessage(extractErrorMessage(err, "反向引用查询失败"));
        } finally {
          setRefsLoading(false);
        }
      }
    } catch (err) {
      setDetailErrorMessage(extractErrorMessage(err, "物理文件信息加载失败"));
      setPhysicalDetail(null);
    }
  }

  function clearPhysicalDetail() {
    setPhysicalDetail(null);
    setDetailErrorMessage("");
    setRefsErrorMessage("");
    setRefsKeywordInput("");
    setRefsKeyword("");
    setReverseRefs([]);
  }

  function applyRefsSearch() {
    setRefsKeyword(refsKeywordInput.trim());
  }

  function clearRefsSearch() {
    setRefsKeywordInput("");
    setRefsKeyword("");
  }

  async function copyId(id: string) {
    try {
      await navigator.clipboard.writeText(id);
      message.success("ID已复制");
    } catch {
      message.warning("复制失败，请手动复制");
    }
  }

  async function copyPreviewText() {
    if (!previewText) {
      message.warning("暂无可复制的内容");
      return;
    }
    try {
      await navigator.clipboard.writeText(previewText);
      message.success("文本已复制");
    } catch {
      message.error("复制失败，请手动复制");
    }
  }

  function getItemActions(item: SystemFileItem): RowAction[] {
    const actions: RowAction[] = [];
    if (item.type === "FILE") {
      if (canView) {
        actions.push({
          key: "preview",
          label: "预览",
          handler: () => { previewFile(item).catch(() => {}); },
        });
      }
      if (canDownload) {
        actions.push({
          key: "download",
          label: "下载",
          handler: () => { downloadFile(item).catch(() => {}); },
        });
      }
      if (canPhysical) {
        actions.push({
          key: "physical",
          label: "物理信息",
          handler: () => { loadPhysicalDetail(item).catch(() => {}); },
        });
      }
    }
    actions.push({
      key: "copy",
      label: "复制ID",
      handler: () => { copyId(item.id).catch(() => {}); },
    });
    return actions;
  }

  function getPrimaryActions(item: SystemFileItem): RowAction[] {
    const actions = getItemActions(item);
    if (actions.length <= 3) return actions;
    return actions.slice(0, 2);
  }

  function getExtraActions(item: SystemFileItem): RowAction[] {
    const actions = getItemActions(item);
    if (actions.length <= 3) return [];
    return actions.slice(2);
  }

  const listColumns: BzTableColumn<SystemFileItem>[] = [
    {
      key: "name",
      title: "名称",
      minWidth: 260,
      render: (row) => (
        <div>
          {row.type === "FOLDER" ? (
            <BzButton link onClick={() => enterFolder(row)}>
              {"📁 "}{row.name}
            </BzButton>
          ) : (
            <span>{"📄 "}{row.name}</span>
          )}
          <div className="node-id">{row.id}</div>
        </div>
      ),
    },
    {
      key: "owner",
      title: "Owner",
      width: 160,
      render: (row) => <span>{row.ownerType}/{row.ownerId}</span>,
    },
    {
      key: "type",
      title: "类型",
      width: 120,
      render: (row) => (
        <BzTag size="small">{row.type === "FOLDER" ? "目录" : "文件"}</BzTag>
      ),
    },
    {
      key: "size",
      title: "大小",
      width: 120,
      render: (row) => <span>{row.type === "FILE" ? formatSize(row.size) : "-"}</span>,
    },
    {
      key: "updatedAt",
      title: "更新时间",
      width: 180,
      render: (row) => <span>{formatDateTime(row.updatedAt || row.createdAt || "")}</span>,
    },
    {
      key: "actions",
      title: "操作",
      minWidth: 220,
      render: (row) => {
        const primary = getPrimaryActions(row);
        const extra = getExtraActions(row);
        return (
          <div className="action-buttons">
            {primary.map((action) => (
              <BzButton
                key={action.key}
                size="small"
                buttonType={action.type || "default"}
                disabled={action.disabled}
                onClick={action.handler}
              >
                {action.label}
              </BzButton>
            ))}
            {extra.length > 0 ? (
              <BzDropdown
                dropdownContent={
                  <BzDropdownMenu>
                    {extra.map((action) => (
                      <BzDropdownItem
                        key={action.key}
                        disabled={action.disabled}
                        onClick={action.handler}
                      >
                        {action.label}
                      </BzDropdownItem>
                    ))}
                  </BzDropdownMenu>
                }
              >
                <BzButton size="small">更多</BzButton>
              </BzDropdown>
            ) : null}
          </div>
        );
      },
    },
  ];

  const refsColumns: BzTableColumn<SystemFileItem>[] = [
    {
      key: "name",
      title: "文件",
      minWidth: 260,
      render: (row) => (
        <div>
          {"📄 "}{row.name}
          <div className="node-id">{row.id}</div>
        </div>
      ),
    },
    {
      key: "owner",
      title: "Owner",
      width: 160,
      render: (row) => <span>{row.ownerType}/{row.ownerId}</span>,
    },
    {
      key: "updatedAt",
      title: "更新时间",
      width: 180,
      render: (row) => <span>{formatDateTime(row.updatedAt || row.createdAt || "")}</span>,
    },
    {
      key: "actions",
      title: "操作",
      minWidth: 200,
      render: (row) => (
        <div className="row-actions">
          {canView ? (
            <BzButton size="small" onClick={() => { previewFile(row).catch(() => {}); }}>
              预览
            </BzButton>
          ) : null}
          {canDownload ? (
            <BzButton size="small" onClick={() => { downloadFile(row).catch(() => {}); }}>
              下载
            </BzButton>
          ) : null}
          <BzButton size="small" onClick={() => { copyId(row.id).catch(() => {}); }}>
            复制ID
          </BzButton>
        </div>
      ),
    },
  ];

  const showSearchTip = !!activeKeyword;

  return (
    <div className="app-shell">
      <div className="content">
        <div className="list-page-stack">
          {canAdmin ? (
            <section className="list-page-actions">
              <div className="list-page-actions-main">
                <BzButton onClick={goRoot}>根目录</BzButton>
                <BzButton disabled={breadcrumbs.length === 0} onClick={goBack}>
                  返回上级
                </BzButton>
                <BzButton onClick={() => reload()}>刷新</BzButton>
                <span className="path" title={currentPath}>
                  当前路径：{currentPath}
                </span>
              </div>
            </section>
          ) : null}

          {canAdmin ? (
            <BzCard className="list-page-query-card" shadow="never">
              <BzForm
                className="list-page-filter-form is-inline"
                onSubmit={(e) => {
                  e.preventDefault();
                  applySearch();
                }}
              >
                <BzFormItem className="list-page-filter-item">
                  <div className="list-page-filter-field">
                    <div className="list-page-filter-label">搜索</div>
                    <BzInput
                      modelValue={keywordInput}
                      className="list-page-filter-control keyword"
                      placeholder="按名称搜索当前目录及子目录"
                      clearable
                      onValueChange={setKeywordInput}
                      onKeyUp={(e) => {
                        if (e.key === "Enter") applySearch();
                      }}
                    />
                  </div>
                </BzFormItem>
                <BzFormItem className="list-page-filter-actions">
                  <BzButton buttonType="primary" onClick={applySearch}>
                    搜索
                  </BzButton>
                  <BzButton disabled={!activeKeyword} onClick={clearSearch}>
                    清空
                  </BzButton>
                </BzFormItem>
                <BzFormItem className="list-page-filter-item">
                  <div className="list-page-filter-field">
                    <div className="list-page-filter-label">排序</div>
                    <div className="list-page-sort-group">
                      <BzSelect
                        modelValue={sortBy}
                        className="sm"
                        onValueChange={(v) => setSortBy((v ?? "NAME") as StorageSortBy)}
                      >
                        <BzOption label="名称" value="NAME" />
                        <BzOption label="大小" value="SIZE" />
                        <BzOption label="类型" value="TYPE" />
                        <BzOption label="最新修改" value="UPDATED_AT" />
                      </BzSelect>
                      <BzSelect
                        modelValue={sortOrder}
                        className="sm"
                        onValueChange={(v) => setSortOrder((v ?? "ASC") as StorageSortOrder)}
                      >
                        <BzOption label="升序" value="ASC" />
                        <BzOption label="降序" value="DESC" />
                      </BzSelect>
                    </div>
                  </div>
                </BzFormItem>
                <BzFormItem className="list-page-filter-item">
                  <div className="list-page-filter-field">
                    <div className="list-page-filter-label">视图</div>
                    <BzButtonGroup>
                      <BzButton
                        size="small"
                        buttonType={viewMode === "GRID" ? "primary" : "default"}
                        onClick={() => setViewMode("GRID")}
                      >
                        平铺
                      </BzButton>
                      <BzButton
                        size="small"
                        buttonType={viewMode === "LIST" ? "primary" : "default"}
                        onClick={() => setViewMode("LIST")}
                      >
                        详细列表
                      </BzButton>
                    </BzButtonGroup>
                  </div>
                </BzFormItem>
              </BzForm>
            </BzCard>
          ) : null}

          <BzCard className="list-page-result-card board" shadow="never">
            <BzLoading loading={loading} text="加载中...">
              {!canAdmin ? (
                <BzEmpty description="无权限查看文件管理页面" />
              ) : errorMessage ? (
                <div className="state-block">
                  <BzAlert title={errorMessage} type="error" showIcon />
                  <BzButton size="small" buttonType="primary" onClick={() => reload()}>
                    重试
                  </BzButton>
                </div>
              ) : !loading && items.length === 0 ? (
                <BzEmpty description={activeKeyword ? "没有匹配结果" : "暂无数据"} />
              ) : (
                <div>
                  {showSearchTip ? (
                    <BzAlert
                      className="search-tip"
                      type="info"
                      closable={false}
                      showIcon
                      title="搜索中：仅显示当前目录及子目录名称匹配结果"
                    />
                  ) : null}

                  {viewMode === "GRID" ? (
                    <div className="grid">
                      {items.map((item) => {
                        const primary = getPrimaryActions(item);
                        const extra = getExtraActions(item);
                        return (
                          <BzCard key={item.id} className="node-card" shadow="never">
                            <div className="node-header">
                              {item.type === "FOLDER" ? (
                                <BzButton link className="title" onClick={() => enterFolder(item)}>
                                  {"📁 "}{item.name}
                                </BzButton>
                              ) : (
                                <div className="title">{"📄 "}{item.name}</div>
                              )}
                              <div className="node-id" title={item.id}>
                                {item.id}
                              </div>
                            </div>

                            <div className="node-meta">
                              <BzTag size="small">{item.type === "FOLDER" ? "目录" : "文件"}</BzTag>
                              <span className="meta">
                                Owner：{item.ownerType}/{item.ownerId}
                              </span>
                              <span className="meta">
                                大小：{item.type === "FILE" ? formatSize(item.size) : "-"}
                              </span>
                              <span className="meta">
                                更新：{formatDateTime(item.updatedAt || item.createdAt || "")}
                              </span>
                            </div>

                            <div className="action-buttons">
                              {primary.map((action) => (
                                <BzButton
                                  key={action.key}
                                  size="small"
                                  buttonType={action.type || "default"}
                                  disabled={action.disabled}
                                  onClick={action.handler}
                                >
                                  {action.label}
                                </BzButton>
                              ))}
                              {extra.length > 0 ? (
                                <BzDropdown
                                  dropdownContent={
                                    <BzDropdownMenu>
                                      {extra.map((action) => (
                                        <BzDropdownItem
                                          key={action.key}
                                          disabled={action.disabled}
                                          onClick={action.handler}
                                        >
                                          {action.label}
                                        </BzDropdownItem>
                                      ))}
                                    </BzDropdownMenu>
                                  }
                                >
                                  <BzButton size="small">更多</BzButton>
                                </BzDropdown>
                              ) : null}
                            </div>
                          </BzCard>
                        );
                      })}
                    </div>
                  ) : (
                    <BzTable data={items} columns={listColumns} rowKey="id" size="small" />
                  )}
                </div>
              )}
            </BzLoading>
          </BzCard>

          {previewName ? (
            <BzCard className="preview-panel">
              <div className="preview-header">
                <div>
                  <div className="preview-title">预览：{previewName}</div>
                  <div className="muted">{previewContentType || "unknown"}</div>
                </div>
                <div className="row-actions">
                  {previewIsText ? (
                    <BzButton size="small" onClick={() => { copyPreviewText().catch(() => {}); }}>
                      复制文本
                    </BzButton>
                  ) : null}
                  <BzButton size="small" onClick={clearPreview}>
                    关闭
                  </BzButton>
                </div>
              </div>
              <div className="preview-body">
                {previewIsImage ? (
                  <img src={previewUrl} alt="preview" className="preview-image" />
                ) : previewIsText ? (
                  <pre className="preview-text">{previewText}</pre>
                ) : (
                  <BzEmpty description="该文件类型不支持内嵌预览，请使用下载。" />
                )}
              </div>
            </BzCard>
          ) : null}

          {physicalDetail ? (
            <BzCard className="physical-panel">
              <div className="physical-header">
                <div>
                  <div className="preview-title">物理文件详情：{physicalDetail.fileName}</div>
                  <div className="muted">逻辑文件：{physicalDetail.logicalFileName}</div>
                </div>
                <BzButton size="small" onClick={clearPhysicalDetail}>
                  关闭
                </BzButton>
              </div>

              <div className="physical-grid">
                <div className="pair">
                  <span className="label">逻辑文件ID</span>
                  <span className="value mono">{physicalDetail.logicalFileId}</span>
                </div>
                <div className="pair">
                  <span className="label">物理文件ID</span>
                  <span className="value mono">{physicalDetail.physicalFileId}</span>
                </div>
                <div className="pair">
                  <span className="label">Owner</span>
                  <span className="value">
                    {physicalDetail.logicalOwnerType}/{physicalDetail.logicalOwnerId}
                  </span>
                </div>
                <div className="pair">
                  <span className="label">Hash</span>
                  <span className="value mono">{physicalDetail.hash}</span>
                </div>
                <div className="pair">
                  <span className="label">相对路径</span>
                  <span className="value mono">{physicalDetail.relativePath}</span>
                </div>
                <div className="pair">
                  <span className="label">绝对路径</span>
                  <span className="value mono">{physicalDetail.absolutePath}</span>
                </div>
                <div className="pair">
                  <span className="label">大小</span>
                  <span className="value">{formatSize(physicalDetail.fileSize)}</span>
                </div>
                <div className="pair">
                  <span className="label">内容类型</span>
                  <span className="value">{physicalDetail.contentType || "-"}</span>
                </div>
                <div className="pair">
                  <span className="label">引用数</span>
                  <span className="value">{physicalDetail.refCount}</span>
                </div>
              </div>

              {detailErrorMessage ? (
                <BzAlert
                  title={detailErrorMessage}
                  type="error"
                  showIcon
                  className="detail-error"
                />
              ) : null}

              <div className="refs-head">
                <h4>同物理文件逻辑引用</h4>
                <div className="refs-search">
                  <BzInput
                    modelValue={refsKeywordInput}
                    placeholder="按名称筛选引用"
                    clearable
                    onValueChange={setRefsKeywordInput}
                    onKeyUp={(e) => {
                      if (e.key === "Enter") applyRefsSearch();
                    }}
                  />
                  {canReverse ? (
                    <BzButton onClick={applyRefsSearch}>查询</BzButton>
                  ) : null}
                  <BzButton disabled={!refsKeyword} onClick={clearRefsSearch}>
                    清空
                  </BzButton>
                </div>
              </div>

              {!canReverse ? (
                <BzEmpty description="无权限查看反向引用信息" />
              ) : (
                <BzLoading loading={refsLoading} text="引用查询中..." className="refs-body">
                  {refsErrorMessage ? (
                    <BzAlert title={refsErrorMessage} type="error" showIcon />
                  ) : reverseRefs.length > 0 ? (
                    <div className="refs-table">
                      <BzTable
                        data={reverseRefs}
                        columns={refsColumns}
                        rowKey="id"
                        size="small"
                      />
                    </div>
                  ) : !refsLoading ? (
                    <BzEmpty description="未查到引用记录" />
                  ) : null}
                </BzLoading>
              )}
            </BzCard>
          ) : null}
        </div>
      </div>
    </div>
  );
}
