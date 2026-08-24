"use client";

import { usePermission } from "@admin/features/resources/permissions";
import { useAdminQueryPanelLayout } from "@admin/shared/hooks/useAdminQueryPanelLayout";
import { message } from "@admin/shared/lib/feedback/message";
import type { AdminActionItem } from "@admin/shared/ui/admin/admin-action";
import { createAdminActionsColumn } from "@admin/shared/ui/admin/admin-actions-column";
import { type AdminDetailSection, AdminDetailTable } from "@admin/shared/ui/admin/AdminDetailTable";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import { AdminTableTools } from "@admin/shared/ui/admin/AdminTableTools";
import { BzAlert } from "@admin/shared/ui/bz/BzAlert";
import { BzButton } from "@admin/shared/ui/bz/BzButton";
import { BzCard } from "@admin/shared/ui/bz/BzCard";
import { BzEmpty } from "@admin/shared/ui/bz/BzEmpty";
import { BzFormItem } from "@admin/shared/ui/bz/BzFormItem";
import { BzInput } from "@admin/shared/ui/bz/BzInput";
import { BzLoading } from "@admin/shared/ui/bz/BzLoading";
import { BzOption } from "@admin/shared/ui/bz/BzOption";
import { BzSelect } from "@admin/shared/ui/bz/BzSelect";
import { BzTable, type BzTableColumn } from "@admin/shared/ui/bz/BzTable";
import { BzTag } from "@admin/shared/ui/bz/BzTag";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import {
  downloadSystemFile,
  fetchSystemFileView,
  getLogicalFilePhysicalDetail,
  listPhysicalFileLogicalRefs,
  listSystemNodes,
} from "./api/client";
import type { StorageListQuery } from "./api/payload";
import type {
  PhysicalFileDetail,
  StorageSortBy,
  StorageSortOrder,
  SystemFileItem,
} from "./model/types";
import { SYSTEM_FILE_PERMISSIONS } from "./permissions";
import styles from "./SystemFilesPage.module.css";

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
  return ["application/json", "application/xml", "application/yaml", "application/sql"].includes(
    normalized,
  );
}

export function SystemFilesPage() {
  const canAdmin = usePermission(SYSTEM_FILE_PERMISSIONS.adminView);
  const canPhysical = usePermission(SYSTEM_FILE_PERMISSIONS.physicalView);
  const canReverse = usePermission(SYSTEM_FILE_PERMISSIONS.referenceView);
  const canView = usePermission(SYSTEM_FILE_PERMISSIONS.preview);
  const canDownload = usePermission(SYSTEM_FILE_PERMISSIONS.download);

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [items, setItems] = useState<SystemFileItem[]>([]);
  const [currentParentId, setCurrentParentId] = useState<string | undefined>(undefined);
  const [breadcrumbs, setBreadcrumbs] = useState<Array<{ id: string; name: string }>>([]);
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const { queryCardRef, queryGridRef, queryExpanded, setQueryExpanded, querySingleRow } =
    useAdminQueryPanelLayout(queryPanelVisible);

  const [keywordInput, setKeywordInput] = useState("");
  const [activeKeyword, setActiveKeyword] = useState("");
  const [sortBy, setSortBy] = useState<StorageSortBy>("NAME");
  const [sortOrder, setSortOrder] = useState<StorageSortOrder>("ASC");

  const [previewUrl, setPreviewUrl] = useState("");
  const [previewName, setPreviewName] = useState("");
  const [previewContentType, setPreviewContentType] = useState("");
  const [previewText, setPreviewText] = useState("");
  const [previewResolvedType, setPreviewResolvedType] = useState("");

  const [physicalDetail, setPhysicalDetail] = useState<PhysicalFileDetail | null>(null);
  const [reverseRefs, setReverseRefs] = useState<SystemFileItem[]>([]);
  const [refsLoading, setRefsLoading] = useState(false);
  const [refsErrorMessage, setRefsErrorMessage] = useState("");
  const [refsKeywordInput, setRefsKeywordInput] = useState("");
  const [refsKeyword, setRefsKeyword] = useState("");
  const listControllerRef = useRef<AbortController | null>(null);
  const previewControllerRef = useRef<AbortController | null>(null);
  const detailControllerRef = useRef<AbortController | null>(null);
  const refsControllerRef = useRef<AbortController | null>(null);
  const previewObjectUrlRef = useRef<string | null>(null);

  const previewIsImage = previewResolvedType.startsWith("image/");
  const previewIsText = isTextType(previewResolvedType);

  const currentPath = useMemo(() => {
    if (breadcrumbs.length === 0) return "/";
    return `/${breadcrumbs.map((b) => b.name).join("/")}`;
  }, [breadcrumbs]);

  const pathSegments = useMemo(() => [{ id: "", name: "/" }, ...breadcrumbs], [breadcrumbs]);

  const clearPreview = useCallback(() => {
    previewControllerRef.current?.abort();
    previewControllerRef.current = null;
    if (previewObjectUrlRef.current) URL.revokeObjectURL(previewObjectUrlRef.current);
    previewObjectUrlRef.current = null;
    setPreviewUrl("");
    setPreviewName("");
    setPreviewContentType("");
    setPreviewResolvedType("");
    setPreviewText("");
  }, []);

  const reload = useCallback(async () => {
    listControllerRef.current?.abort();
    clearPreview();
    if (!canAdmin) {
      setItems([]);
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    listControllerRef.current = controller;
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
      const nextItems = await listSystemNodes(query, { signal: controller.signal });
      if (listControllerRef.current === controller && !controller.signal.aborted) {
        setItems(nextItems);
      }
    } catch (err) {
      if (!controller.signal.aborted && listControllerRef.current === controller) {
        setErrorMessage(extractErrorMessage(err, "文件列表加载失败"));
      }
    } finally {
      if (listControllerRef.current === controller) {
        listControllerRef.current = null;
        setLoading(false);
      }
    }
  }, [activeKeyword, canAdmin, clearPreview, currentParentId, sortBy, sortOrder]);

  useEffect(() => {
    void reload();
  }, [reload]);

  useEffect(
    () => () => {
      listControllerRef.current?.abort();
      previewControllerRef.current?.abort();
      detailControllerRef.current?.abort();
      refsControllerRef.current?.abort();
      if (previewObjectUrlRef.current) URL.revokeObjectURL(previewObjectUrlRef.current);
      previewObjectUrlRef.current = null;
    },
    [],
  );

  useEffect(() => {
    if (!canView) clearPreview();
  }, [canView, clearPreview]);

  useEffect(() => {
    if (canPhysical) return;
    detailControllerRef.current?.abort();
    detailControllerRef.current = null;
    setPhysicalDetail(null);
  }, [canPhysical]);

  useEffect(() => {
    if (canReverse) return;
    refsControllerRef.current?.abort();
    refsControllerRef.current = null;
    setRefsLoading(false);
    setReverseRefs([]);
  }, [canReverse]);

  async function loadReverseRefs(detail: PhysicalFileDetail, keyword: string) {
    if (!canReverse) return;
    refsControllerRef.current?.abort();
    const controller = new AbortController();
    refsControllerRef.current = controller;
    setRefsLoading(true);
    setRefsErrorMessage("");
    try {
      const nextRefs = await listPhysicalFileLogicalRefs(
        detail.physicalFileId,
        keyword,
        sortBy,
        sortOrder,
        { signal: controller.signal },
      );
      if (refsControllerRef.current === controller && !controller.signal.aborted) {
        setReverseRefs(nextRefs);
      }
    } catch (err) {
      if (!controller.signal.aborted && refsControllerRef.current === controller) {
        setReverseRefs([]);
        setRefsErrorMessage(extractErrorMessage(err, "反向引用查询失败"));
      }
    } finally {
      if (refsControllerRef.current === controller) {
        refsControllerRef.current = null;
        setRefsLoading(false);
      }
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
    detailControllerRef.current?.abort();
    detailControllerRef.current = null;
    refsControllerRef.current?.abort();
    refsControllerRef.current = null;
    setRefsLoading(false);
    setPhysicalDetail(null);
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
    const controller = new AbortController();
    previewControllerRef.current = controller;
    try {
      const blob = await fetchSystemFileView(row.id, { signal: controller.signal });
      if (previewControllerRef.current !== controller || controller.signal.aborted) return;
      setPreviewName(row.name);
      setPreviewContentType(row.contentType || blob.type || "");
      const resolved = resolvePreviewType(row.name, row.contentType || blob.type || "");
      setPreviewResolvedType(resolved);
      if (resolved) {
        setPreviewContentType(resolved);
      }
      if (resolved.startsWith("image/")) {
        const objectUrl = URL.createObjectURL(blob);
        if (previewControllerRef.current !== controller || controller.signal.aborted) {
          URL.revokeObjectURL(objectUrl);
          return;
        }
        previewObjectUrlRef.current = objectUrl;
        setPreviewUrl(objectUrl);
        setPreviewText("");
        return;
      }
      if (isTextType(resolved)) {
        const text = await blob.text();
        if (previewControllerRef.current !== controller || controller.signal.aborted) return;
        setPreviewText(text);
        setPreviewUrl("");
        return;
      }
      setPreviewUrl("");
      setPreviewText("");
    } catch (err) {
      if (!controller.signal.aborted && previewControllerRef.current === controller) {
        message.error(extractErrorMessage(err, "文件预览失败"));
      }
    } finally {
      if (previewControllerRef.current === controller) previewControllerRef.current = null;
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
    detailControllerRef.current?.abort();
    refsControllerRef.current?.abort();
    const controller = new AbortController();
    detailControllerRef.current = controller;
    setRefsErrorMessage("");
    try {
      const detail = await getLogicalFilePhysicalDetail(item.id, { signal: controller.signal });
      if (detailControllerRef.current !== controller || controller.signal.aborted) return;
      setPhysicalDetail(detail);
      setRefsKeywordInput("");
      setRefsKeyword("");
      setReverseRefs([]);
      if (canReverse) {
        await loadReverseRefs(detail, "");
      }
    } catch (err) {
      if (!controller.signal.aborted && detailControllerRef.current === controller) {
        message.error(extractErrorMessage(err, "物理文件信息加载失败"));
        setPhysicalDetail(null);
      }
    } finally {
      if (detailControllerRef.current === controller) detailControllerRef.current = null;
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
      actions.push({
        key: "enter",
        label: "进入",
        level: "default",
        onClick: () => enterFolder(item),
      });
    }
    if (item.type === "FILE") {
      if (canView) {
        actions.push({
          key: "preview",
          label: "预览",
          level: "default",
          onClick: () => void previewFile(item),
        });
      }
      if (canDownload) {
        actions.push({
          key: "download",
          label: "下载",
          level: "default",
          onClick: () => void downloadFile(item),
        });
      }
      if (canPhysical) {
        actions.push({
          key: "physical",
          label: "物理信息",
          level: "primary",
          onClick: () => void loadPhysicalDetail(item),
        });
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
          <button
            type="button"
            className={styles.fileLink}
            onClick={() => enterFolder(row)}
          >
            <span aria-hidden="true">📁</span>
            <span>{row.name}</span>
          </button>
        ) : (
          <span className={styles.fileName}>
            <span aria-hidden="true">📄</span>
            <span>{row.name}</span>
          </span>
        ),
    },
    {
      key: "id",
      title: "ID",
      width: 300,
      render: (row) => <span className={styles.monoMuted}>{row.id}</span>,
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
  ];
  const listActionsColumn = createAdminActionsColumn({ rows: items, getActions: getItemActions });
  if (listActionsColumn) listColumns.push(listActionsColumn);

  const refsColumns: Array<BzTableColumn<SystemFileItem>> = [
    {
      key: "name",
      title: "名称",
      minWidth: 220,
      render: (row) => (
        <span className={styles.fileName}>
          <span aria-hidden="true">📄</span>
          <span>{row.name}</span>
        </span>
      ),
    },
    {
      key: "id",
      title: "ID",
      minWidth: 220,
      render: (row) => <span className={styles.monoMuted}>{row.id}</span>,
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
  ];
  const refsActionsColumn = createAdminActionsColumn({
    rows: reverseRefs,
    getActions: (row) =>
      getItemActions(row).filter((action) => action.key !== "physical" && action.key !== "enter"),
  });
  if (refsActionsColumn) refsColumns.push(refsActionsColumn);

  const showSearchTip = !!activeKeyword;

  const physicalDetailSections = useMemo<AdminDetailSection[]>(() => {
    if (!physicalDetail) return [];
    return [
      {
        title: "基础信息",
        fields: [
          { label: "逻辑文件ID", value: physicalDetail.logicalFileId },
          { label: "物理文件ID", value: physicalDetail.physicalFileId },
          {
            label: "Owner",
            value: `${physicalDetail.logicalOwnerType}/${physicalDetail.logicalOwnerId}`,
          },
          { label: "大小", value: formatSize(physicalDetail.fileSize) },
          { label: "内容类型", value: physicalDetail.contentType || "-" },
          { label: "引用数", value: String(physicalDetail.refCount) },
        ],
      },
      {
        title: "存储信息",
        fields: [
          { label: "Hash", value: physicalDetail.hash || "-", span: "full" },
          { label: "相对路径", value: physicalDetail.relativePath || "-", span: "full" },
          {
            label: "绝对路径",
            value: physicalDetail.absolutePath || "-",
            span: "full",
            multiline: true,
          },
        ],
      },
    ];
  }, [physicalDetail]);

  return (
    <div className={layoutStyles.page}>
      <div className={layoutStyles.content}>
        <div className={layoutStyles.pageStack}>
          <BzCard
            className={`${layoutStyles.panel} ${layoutStyles.tableCard} ${layoutStyles.listCard}`}
            shadow="never"
          >
            <div className={layoutStyles.listRegion}>
              {canAdmin && queryPanelVisible ? (
                <div className={layoutStyles.listQueryPanel}>
                  <div
                    ref={queryCardRef}
                    className={[
                      layoutStyles.queryLayout,
                      querySingleRow
                        ? layoutStyles.singleRow
                        : queryExpanded
                          ? layoutStyles.expanded
                          : layoutStyles.collapsed,
                    ].join(" ")}
                  >
                    <form
                      ref={queryGridRef}
                      className={layoutStyles.queryGrid}
                      onSubmit={(e) => {
                        e.preventDefault();
                        applySearch();
                      }}
                    >
                      <BzFormItem className={layoutStyles.queryField}>
                        <div className={layoutStyles.queryFieldLabel}>搜索</div>
                        <div className={layoutStyles.queryFieldControl}>
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
                      <BzFormItem className={layoutStyles.queryField}>
                        <div className={layoutStyles.queryFieldLabel}>排序</div>
                        <div className={`${layoutStyles.queryFieldControl} ${styles.sortControls}`}>
                          <BzSelect
                            modelValue={sortBy}
                            onValueChange={(v) => setSortBy((v ?? "NAME") as StorageSortBy)}
                          >
                            <BzOption
                              label="名称"
                              value="NAME"
                            />
                            <BzOption
                              label="大小"
                              value="SIZE"
                            />
                            <BzOption
                              label="类型"
                              value="TYPE"
                            />
                            <BzOption
                              label="最新修改"
                              value="UPDATED_AT"
                            />
                          </BzSelect>
                          <BzSelect
                            modelValue={sortOrder}
                            onValueChange={(v) => setSortOrder((v ?? "ASC") as StorageSortOrder)}
                          >
                            <BzOption
                              label="升序"
                              value="ASC"
                            />
                            <BzOption
                              label="降序"
                              value="DESC"
                            />
                          </BzSelect>
                        </div>
                      </BzFormItem>
                      <div className={layoutStyles.queryActions}>
                        <BzButton
                          className={layoutStyles.filterSecondary}
                          nativeType="button"
                          onClick={clearSearch}
                          disabled={!activeKeyword && !keywordInput}
                        >
                          重置
                        </BzButton>
                        <BzButton
                          className={layoutStyles.filterPrimary}
                          buttonType="primary"
                          nativeType="button"
                          onClick={applySearch}
                        >
                          搜索
                        </BzButton>
                        {!querySingleRow ? (
                          <button
                            className={layoutStyles.filterToggle}
                            type="button"
                            aria-expanded={queryExpanded}
                            onClick={() => setQueryExpanded((value) => !value)}
                          >
                            <span>{queryExpanded ? "收起" : "展开"}</span>
                            <i
                              className={`${layoutStyles.filterToggleIcon} ${queryExpanded ? layoutStyles.up : layoutStyles.down}`}
                              aria-hidden="true"
                            />
                          </button>
                        ) : null}
                      </div>
                    </form>
                  </div>
                </div>
              ) : null}

              <div className={layoutStyles.listToolbarRow}>
                <div className={`${layoutStyles.listBusinessActions} ${styles.headerStack}`}>
                  <div
                    className={styles.breadcrumbs}
                    title={currentPath}
                  >
                    {pathSegments.map((segment, index) => (
                      <span
                        key={`${segment.id || "root"}-${index}`}
                        className={styles.breadcrumbWrap}
                      >
                        {index > 0 ? <span className={styles.breadcrumbSeparator}>/</span> : null}
                        <button
                          type="button"
                          className={`${styles.breadcrumbSegment} ${
                            index === pathSegments.length - 1 ? styles.current : ""
                          }`}
                          onClick={() => goToBreadcrumb(index)}
                        >
                          {segment.name}
                        </button>
                      </span>
                    ))}
                  </div>
                </div>
                <div className={layoutStyles.listQueryTools}>
                  <AdminTableTools
                    queryPanelVisible={queryPanelVisible}
                    onToggleQueryPanel={() => setQueryPanelVisible((value) => !value)}
                    onRefresh={() => void reload()}
                  />
                  <button
                    className={layoutStyles.circleButton}
                    type="button"
                    title="返回根目录"
                    onClick={goRoot}
                  >
                    <span
                      className={styles.rootIcon}
                      aria-hidden="true"
                    >
                      /
                    </span>
                  </button>
                  <button
                    className={layoutStyles.circleButton}
                    type="button"
                    title="返回上级"
                    onClick={goBack}
                    disabled={breadcrumbs.length === 0}
                  >
                    <span
                      className={styles.rootIcon}
                      aria-hidden="true"
                    >
                      ..
                    </span>
                  </button>
                </div>
              </div>

              <BzLoading
                loading={loading}
                text="加载中..."
              >
                {!canAdmin ? (
                  <BzEmpty description="无权限查看文件管理页面" />
                ) : errorMessage ? (
                  <div className={styles.stateBlock}>
                    <BzAlert
                      title={errorMessage}
                      type="error"
                      showIcon
                    />
                    <BzButton
                      size="small"
                      buttonType="primary"
                      onClick={() => void reload()}
                    >
                      重试
                    </BzButton>
                  </div>
                ) : !loading && items.length === 0 ? (
                  <BzEmpty description={activeKeyword ? "没有匹配结果" : "暂无数据"} />
                ) : (
                  <div>
                    {showSearchTip ? (
                      <BzAlert
                        className={styles.searchTip}
                        type="info"
                        closable={false}
                        showIcon
                        title="搜索中：仅显示当前目录及子目录名称匹配结果"
                      />
                    ) : null}
                    <div className={`${layoutStyles.tableSurface} ${layoutStyles.listTableArea}`}>
                      <BzTable
                        data={items}
                        columns={listColumns}
                        rowKey="id"
                        size="small"
                      />
                    </div>
                  </div>
                )}
              </BzLoading>
            </div>
          </BzCard>

          {previewName ? (
            <BzCard className={`${styles.previewPanel} ${layoutStyles.panel}`}>
              <div className={styles.panelHeader}>
                <div>
                  <div className={styles.panelTitle}>预览：{previewName}</div>
                  <div className={styles.muted}>{previewContentType || "unknown"}</div>
                </div>
                <div className={styles.rowActions}>
                  {previewIsText ? (
                    <BzButton
                      size="small"
                      onClick={() => void copyPreviewText()}
                    >
                      复制文本
                    </BzButton>
                  ) : null}
                  <BzButton
                    size="small"
                    onClick={clearPreview}
                  >
                    关闭
                  </BzButton>
                </div>
              </div>
              <div className={styles.previewBody}>
                {previewIsImage ? (
                  <img
                    src={previewUrl}
                    alt="preview"
                    className={styles.previewImage}
                  />
                ) : previewIsText ? (
                  <pre className={styles.previewText}>{previewText}</pre>
                ) : (
                  <BzEmpty description="该文件类型不支持内嵌预览，请使用下载。" />
                )}
              </div>
            </BzCard>
          ) : null}

          {physicalDetail ? (
            <BzCard className={`${styles.physicalPanel} ${layoutStyles.panel}`}>
              <div className={styles.panelHeader}>
                <div>
                  <div className={styles.panelTitle}>物理文件详情：{physicalDetail.fileName}</div>
                  <div className={styles.muted}>逻辑文件：{physicalDetail.logicalFileName}</div>
                </div>
                <BzButton
                  size="small"
                  onClick={clearPhysicalDetail}
                >
                  关闭
                </BzButton>
              </div>

              <AdminDetailTable sections={physicalDetailSections} />

              <div className={styles.refsHead}>
                <h4>同物理文件逻辑引用</h4>
                <div className={styles.refsSearch}>
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
                  <BzButton
                    disabled={!refsKeyword && !refsKeywordInput}
                    onClick={clearRefsSearch}
                  >
                    清空
                  </BzButton>
                </div>
              </div>

              {!canReverse ? (
                <BzEmpty description="无权限查看反向引用信息" />
              ) : (
                <BzLoading
                  loading={refsLoading}
                  text="引用查询中..."
                  className={styles.refsBody}
                >
                  {refsErrorMessage ? (
                    <BzAlert
                      title={refsErrorMessage}
                      type="error"
                      showIcon
                    />
                  ) : reverseRefs.length > 0 ? (
                    <div className={`${styles.refsTable} ${layoutStyles.tableSurface}`}>
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
