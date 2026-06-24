"use client";

import {
  downloadSystemFile,
  fetchSystemFileView,
  getLogicalFilePhysicalDetail,
  listPhysicalFileLogicalRefs,
  listSystemNodes,
} from "@admin/api/system-files";
import { AdminActionBar } from "@admin/components/admin/AdminActionBar";
import { AdminTableTools } from "@admin/components/admin/AdminTableTools";
import { message } from "@admin/core/message";
import { hasResourceCodeAccess } from "@admin/core/registry/resources-registry";
import type { AdminActionItem } from "@admin/types/admin-action";
import type { PhysicalFileDetail, StorageListQuery, StorageSortBy, StorageSortOrder, SystemFileItem } from "@admin/types/file-storage";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { BzAlert } from "../bz/BzAlert";
import { BzButton } from "../bz/BzButton";
import { BzCard } from "../bz/BzCard";
import { BzEmpty } from "../bz/BzEmpty";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzLoading } from "../bz/BzLoading";
import { BzOption } from "../bz/BzOption";
import { BzSelect } from "../bz/BzSelect";
import { BzTable, type BzTableColumn } from "../bz/BzTable";
import { BzTag } from "../bz/BzTag";

const PREVIEW_SIZE_LIMIT = 5 * 1024 * 1024;

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
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [queryExpanded, setQueryExpanded] = useState(false);
  const [querySingleRow, setQuerySingleRow] = useState(true);

  const [keywordInput, setKeywordInput] = useState("");
  const [activeKeyword, setActiveKeyword] = useState("");
  const [sortBy, setSortBy] = useState<StorageSortBy>("NAME");
  const [sortOrder, setSortOrder] = useState<StorageSortOrder>("ASC");
  const queryCardRef = useRef<HTMLDivElement | null>(null);
  const queryGridRef = useRef<HTMLFormElement | null>(null);

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

  const pathSegments = useMemo(() => [{ id: "", name: "/" }, ...breadcrumbs], [breadcrumbs]);

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
        parentId: currentParentId,
        sortBy,
        sortOrder,
      };
      if (activeKeyword.trim()) {
        query.keyword = activeKeyword.trim();
        query.recursive = true;
      }
      setItems(await listSystemNodes(query));
    } catch (err) {
      setErrorMessage(extractErrorMessage(err, "文件列表加载失败"));
    } finally {
      setLoading(false);
    }
  }, [activeKeyword, canAdmin, clearPreview, currentParentId, sortBy, sortOrder]);

  useEffect(() => {
    void reload();
  }, [reload]);

  useEffect(() => {
    return () => clearPreview();
  }, [clearPreview]);

  useEffect(() => {
    if (!queryPanelVisible) {
      return;
    }

    const card = queryCardRef.current;
    const grid = queryGridRef.current;
    if (!card || !grid) {
      return;
    }

    let frame = 0;

    const refreshCollapseState = () => {
      cancelAnimationFrame(frame);

      frame = window.requestAnimationFrame(() => {
        const fields = Array.from(grid.querySelectorAll<HTMLElement>(".admin-query-field"));
        const actions = grid.querySelector<HTMLElement>(".admin-query-actions");
        const items = actions ? [...fields, actions] : fields;

        if (items.length === 0) {
          card.style.removeProperty("--admin-query-collapsed-height");
          card.style.removeProperty("--admin-query-expanded-height");
          setQuerySingleRow(true);
          return;
        }

        const previousMaxHeight = grid.style.maxHeight;
        const previousActionGridRow = actions?.style.gridRow || "";
        const previousActionGridColumn = actions?.style.gridColumn || "";

        grid.style.maxHeight = "none";
        if (actions) {
          actions.style.gridRow = "auto";
          actions.style.gridColumn = "auto";
        }

        const rowTops = [...new Set(items.map((item) => Math.round(item.offsetTop)))].sort((left, right) => left - right);
        const firstRowTop = rowTops[0] || 0;
        const firstRowItems = items.filter((item) => Math.round(item.offsetTop) === firstRowTop);
        const firstRowBottom = Math.max(...firstRowItems.map((item) => item.offsetTop + item.offsetHeight), 0);
        const collapsedHeight = Math.max(firstRowBottom - firstRowTop, 0);
        const expandedHeight = grid.scrollHeight;

        grid.style.maxHeight = previousMaxHeight;
        if (actions) {
          actions.style.gridRow = previousActionGridRow;
          actions.style.gridColumn = previousActionGridColumn;
        }

        card.style.setProperty("--admin-query-collapsed-height", `${collapsedHeight}px`);
        card.style.setProperty("--admin-query-expanded-height", `${expandedHeight}px`);

        const nextSingleRow = rowTops.length <= 1;
        setQuerySingleRow(nextSingleRow);
        if (nextSingleRow) {
          setQueryExpanded(false);
        }
      });
    };

    refreshCollapseState();

    const observer = new ResizeObserver(() => {
      refreshCollapseState();
    });

    observer.observe(grid);
    window.addEventListener("resize", refreshCollapseState);

    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
      window.removeEventListener("resize", refreshCollapseState);
    };
  }, [queryPanelVisible]);

  async function loadReverseRefs(detail: PhysicalFileDetail, keyword: string) {
    if (!canReverse) return;
    setRefsLoading(true);
    setRefsErrorMessage("");
    try {
      setReverseRefs(await listPhysicalFileLogicalRefs(detail.physicalFileId, keyword, sortBy, sortOrder));
    } catch (err) {
      setReverseRefs([]);
      setRefsErrorMessage(extractErrorMessage(err, "反向引用查询失败"));
    } finally {
      setRefsLoading(false);
    }
  }

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

  function clearPhysicalDetail() {
    setPhysicalDetail(null);
    setDetailErrorMessage("");
    setRefsErrorMessage("");
    setRefsKeywordInput("");
    setRefsKeyword("");
    setReverseRefs([]);
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
    clearPhysicalDetail();
  }

  function goToBreadcrumb(index: number) {
    if (index <= 0) {
      goRoot();
      return;
    }
    const next = breadcrumbs.slice(0, index);
    setBreadcrumbs(next);
    setCurrentParentId(next[next.length - 1]?.id);
    setKeywordInput("");
    setActiveKeyword("");
    clearPhysicalDetail();
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
      const resolved = resolvePreviewType(row.name, row.contentType || blob.type || "");
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
    if (!canPhysical || item.type !== "FILE") return;
    setDetailErrorMessage("");
    setRefsErrorMessage("");
    try {
      const detail = await getLogicalFilePhysicalDetail(item.id);
      setPhysicalDetail(detail);
      setRefsKeywordInput("");
      setRefsKeyword("");
      setReverseRefs([]);
      if (canReverse) {
        await loadReverseRefs(detail, "");
      }
    } catch (err) {
      setDetailErrorMessage(extractErrorMessage(err, "物理文件信息加载失败"));
      setPhysicalDetail(null);
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

  function applyRefsSearch() {
    const nextKeyword = refsKeywordInput.trim();
    setRefsKeyword(nextKeyword);
    if (physicalDetail) {
      void loadReverseRefs(physicalDetail, nextKeyword);
    }
  }

  function clearRefsSearch() {
    setRefsKeywordInput("");
    setRefsKeyword("");
    if (physicalDetail) {
      void loadReverseRefs(physicalDetail, "");
    }
  }

  function getItemActions(item: SystemFileItem): AdminActionItem[] {
    const actions: AdminActionItem[] = [];
    if (item.type === "FOLDER") {
      actions.push({ key: "enter", label: "进入", tone: "detail", handler: () => enterFolder(item) });
    }
    if (item.type === "FILE") {
      if (canView) {
        actions.push({ key: "preview", label: "预览", tone: "detail", handler: () => void previewFile(item) });
      }
      if (canDownload) {
        actions.push({ key: "download", label: "下载", tone: "neutral", handler: () => void downloadFile(item) });
      }
      if (canPhysical) {
        actions.push({ key: "physical", label: "物理信息", tone: "edit", handler: () => void loadPhysicalDetail(item) });
      }
    }
    return actions;
  }

  const listColumns: Array<BzTableColumn<SystemFileItem>> = [
    {
      key: "name",
      title: "名称",
      width: 340,
      render: (row) =>
        row.type === "FOLDER" ? (
          <button type="button" className="system-files-link" onClick={() => enterFolder(row)}>
            <span aria-hidden="true">📁</span>
            <span>{row.name}</span>
          </button>
        ) : (
          <span className="system-files-name">
            <span aria-hidden="true">📄</span>
            <span>{row.name}</span>
          </span>
        ),
    },
    {
      key: "id",
      title: "ID",
      width: 300,
      render: (row) => <span className="mono subdued">{row.id}</span>,
    },
    {
      key: "owner",
      title: "Owner",
      width: 300,
      render: (row) => (
        <span>
          {row.ownerType}/{row.ownerId}
        </span>
      ),
    },
    {
      key: "type",
      title: "类型",
      width: 100,
      render: (row) => <BzTag size="small">{row.type === "FOLDER" ? "目录" : "文件"}</BzTag>,
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
      render: (row) => <AdminActionBar actions={getItemActions(row)} />,
    },
  ];

  const refsColumns: Array<BzTableColumn<SystemFileItem>> = [
    {
      key: "name",
      title: "名称",
      minWidth: 220,
      render: (row) => (
        <span className="system-files-name">
          <span aria-hidden="true">📄</span>
          <span>{row.name}</span>
        </span>
      ),
    },
    {
      key: "id",
      title: "ID",
      minWidth: 220,
      render: (row) => <span className="mono subdued">{row.id}</span>,
    },
    {
      key: "owner",
      title: "Owner",
      width: 160,
      render: (row) => (
        <span>
          {row.ownerType}/{row.ownerId}
        </span>
      ),
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
      minWidth: 180,
      render: (row) => (
        <AdminActionBar actions={getItemActions(row).filter((action) => action.key !== "physical" && action.key !== "enter")} />
      ),
    },
  ];

  const showSearchTip = !!activeKeyword;

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          {canAdmin && queryPanelVisible ? (
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
                  onSubmit={(e) => {
                    e.preventDefault();
                    applySearch();
                  }}
                >
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">搜索</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={keywordInput}
                        placeholder="按名称搜索当前目录及子目录"
                        clearable
                        onValueChange={setKeywordInput}
                        onKeyUp={(e) => {
                          if (e.key === "Enter") applySearch();
                        }}
                      />
                    </div>
                  </BzFormItem>
                  <BzFormItem className="admin-query-field">
                    <div className="admin-query-field__label">排序</div>
                    <div className="admin-query-field__control" style={{ display: "flex", gap: 8 }}>
                      <BzSelect modelValue={sortBy} onValueChange={(v) => setSortBy((v ?? "NAME") as StorageSortBy)}>
                        <BzOption label="名称" value="NAME" />
                        <BzOption label="大小" value="SIZE" />
                        <BzOption label="类型" value="TYPE" />
                        <BzOption label="最新修改" value="UPDATED_AT" />
                      </BzSelect>
                      <BzSelect modelValue={sortOrder} onValueChange={(v) => setSortOrder((v ?? "ASC") as StorageSortOrder)}>
                        <BzOption label="升序" value="ASC" />
                        <BzOption label="降序" value="DESC" />
                      </BzSelect>
                    </div>
                  </BzFormItem>
                  <div className="admin-query-actions">
                    <BzButton
                      className="admin-filter-secondary"
                      nativeType="button"
                      onClick={clearSearch}
                      disabled={!activeKeyword && !keywordInput}
                    >
                      重置
                    </BzButton>
                    <BzButton className="admin-filter-primary" buttonType="primary" nativeType="button" onClick={applySearch}>
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
                <div style={{ display: "grid", gap: 6, minWidth: 0 }}>
                  <div className="admin-table-title">系统文件</div>
                  <div className="system-files-breadcrumbs" title={currentPath}>
                    {pathSegments.map((segment, index) => (
                      <span key={`${segment.id || "root"}-${index}`} className="system-files-breadcrumbs__segment-wrap">
                        {index > 0 ? <span className="system-files-breadcrumbs__separator">/</span> : null}
                        <button
                          type="button"
                          className={`system-files-breadcrumbs__segment${index === pathSegments.length - 1 ? " is-current" : ""}`}
                          onClick={() => goToBreadcrumb(index)}
                        >
                          {segment.name}
                        </button>
                      </span>
                    ))}
                  </div>
                </div>
                <div className="admin-table-tools">
                  <AdminTableTools
                    queryPanelVisible={queryPanelVisible}
                    onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
                    onRefresh={() => void reload()}
                  />
                  <button className="admin-vben-circle-button" type="button" title="返回根目录" onClick={goRoot}>
                    <span className="system-files-root-icon" aria-hidden="true">/</span>
                  </button>
                  <button className="admin-vben-circle-button" type="button" title="返回上级" onClick={goBack} disabled={breadcrumbs.length === 0}>
                    <span className="system-files-root-icon" aria-hidden="true">..</span>
                  </button>
                </div>
              </div>
            }
          >
            <BzLoading loading={loading} text="加载中...">
              {!canAdmin ? (
                <BzEmpty description="无权限查看文件管理页面" />
              ) : errorMessage ? (
                <div className="state-block">
                  <BzAlert title={errorMessage} type="error" showIcon />
                  <BzButton size="small" buttonType="primary" onClick={() => void reload()}>
                    重试
                  </BzButton>
                </div>
              ) : !loading && items.length === 0 ? (
                <BzEmpty description={activeKeyword ? "没有匹配结果" : "暂无数据"} />
              ) : (
                <div>
                  {showSearchTip ? (
                    <BzAlert className="search-tip" type="info" closable={false} showIcon title="搜索中：仅显示当前目录及子目录名称匹配结果" />
                  ) : null}
                  <div className="admin-table-surface">
                    <BzTable data={items} columns={listColumns} rowKey="id" size="small" />
                  </div>
                </div>
              )}
            </BzLoading>
          </BzCard>

          {previewName ? (
            <BzCard className="preview-panel admin-panel">
              <div className="preview-header">
                <div>
                  <div className="preview-title">预览：{previewName}</div>
                  <div className="muted">{previewContentType || "unknown"}</div>
                </div>
                <div className="row-actions">
                  {previewIsText ? (
                    <BzButton size="small" onClick={() => void copyPreviewText()}>
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
            <BzCard className="physical-panel admin-panel">
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

              {detailErrorMessage ? <BzAlert title={detailErrorMessage} type="error" showIcon className="detail-error" /> : null}

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
                  {canReverse ? <BzButton onClick={applyRefsSearch}>查询</BzButton> : null}
                  <BzButton disabled={!refsKeyword && !refsKeywordInput} onClick={clearRefsSearch}>
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
                    <div className="refs-table admin-table-surface">
                      <BzTable data={reverseRefs} columns={refsColumns} rowKey="id" size="small" />
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
