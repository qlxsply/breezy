"use client";

import { disableApi, pageApis, publishApi } from "@admin/api/apis";
import { batchListDictOptions } from "@admin/api/dicts";
import { ApiTable } from "@admin/components/apis-admin/ApiTable";
import { BzButton, BzCard, BzFormItem, BzInput, BzPagination } from "@admin/components/bz";
import { hasResourceCodeAccess } from "@admin/core/registry/permissions-registry";
import type { ApiEntry } from "@admin/types/api-admin";
import type { DictItem } from "@admin/types/dict-admin";
import type { PageResult } from "@admin/types/page";
import { useEffect, useMemo, useRef, useState } from "react";

const API_DICT_CODES = ["API_METHOD", "API_PROTOCOL", "API_ACCESS_TYPE"] as const;
const USER_TYPE_LABELS: Record<string, string> = {
  SYSTEM: "系统账号",
  INTERNAL: "账号",
  EXTERNAL: "用户",
  GUEST: "游客",
};

export function ApisAdminPage() {
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState<PageResult<ApiEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });
  const [queryPanelVisible, setQueryPanelVisible] = useState(false);
  const [queryExpanded, setQueryExpanded] = useState(false);
  const [querySingleRow, setQuerySingleRow] = useState(true);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [methodLabelMap, setMethodLabelMap] = useState<Record<string, string>>({});
  const [protocolLabelMap, setProtocolLabelMap] = useState<Record<string, string>>({});
  const [accessTypeLabelMap, setAccessTypeLabelMap] = useState<Record<string, string>>({});

  const canPublish = hasResourceCodeAccess("api-manage-publish");
  const canDisable = hasResourceCodeAccess("api-manage-disable");
  const pageSizeOptions = [10, 20, 30, 50, 100, 200];
  const queryCardRef = useRef<HTMLDivElement | null>(null);
  const queryGridRef = useRef<HTMLFormElement | null>(null);

  useEffect(() => {
    void loadDictionaries();
  }, []);

  useEffect(() => {
    void reload();
  }, [appliedKeyword, pageNo, pageSize]);

  useEffect(() => {
    if (!queryPanelVisible) {
      return;
    }

    const card = queryCardRef.current;
    const grid = queryGridRef.current;
    if (!card || !grid) {
      return;
    }

    const refreshCollapseState = () => {
      const fields = Array.from(grid.querySelectorAll<HTMLElement>(".admin-query-field"));
      if (fields.length === 0) {
        card.style.removeProperty("--admin-query-collapsed-height");
        card.style.removeProperty("--admin-query-expanded-height");
        setQuerySingleRow(true);
        return;
      }

      const previousMaxHeight = grid.style.maxHeight;
      grid.style.maxHeight = "none";

      const rowTops = [...new Set(fields.map((field) => Math.round(field.offsetTop)))].sort((left, right) => left - right);
      const firstRowTop = rowTops[0] || 0;
      const firstRowFields = fields.filter((field) => Math.round(field.offsetTop) === firstRowTop);
      const firstRowBottom = Math.max(...firstRowFields.map((field) => field.offsetTop + field.offsetHeight), 0);
      const collapsedHeight = Math.max(firstRowBottom - firstRowTop, 0);
      const expandedHeight = grid.scrollHeight;

      grid.style.maxHeight = previousMaxHeight;

      card.style.setProperty("--admin-query-collapsed-height", `${collapsedHeight}px`);
      card.style.setProperty("--admin-query-expanded-height", `${expandedHeight}px`);
      setQuerySingleRow(rowTops.length <= 1);
    };

    refreshCollapseState();

    const observer = new ResizeObserver(() => {
      refreshCollapseState();
    });

    observer.observe(grid);
    return () => {
      observer.disconnect();
    };
  }, [queryPanelVisible]);

  const enrichedRows = useMemo(
    () =>
      page.elements.map((api) => ({
        ...api,
        protocolLabel: protocolLabelMap[api.protocol] || api.protocol,
        httpMethodLabel: methodLabelMap[api.httpMethod] || api.httpMethod,
        accessTypeLabel: accessTypeLabelMap[api.accessType] || api.accessType,
        userTypeLabels: resolveUserTypeLabels(api.userTypes),
        auditTooltip: buildAuditTooltip(api),
      })),
    [accessTypeLabelMap, methodLabelMap, page.elements, protocolLabelMap],
  );

  async function loadDictionaries() {
    try {
      const result = await batchListDictOptions([...API_DICT_CODES]);
      setMethodLabelMap(toLabelMap(result.API_METHOD));
      setProtocolLabelMap(toLabelMap(result.API_PROTOCOL));
      setAccessTypeLabelMap(toLabelMap(result.API_ACCESS_TYPE));
    } catch {
      setMethodLabelMap({});
      setProtocolLabelMap({});
      setAccessTypeLabelMap({});
    }
  }

  async function reload() {
    setLoading(true);
    try {
      const nextPage = await pageApis(appliedKeyword, pageNo, pageSize);
      setPage(nextPage);
    } finally {
      setLoading(false);
    }
  }

  function applyFilters() {
    setAppliedKeyword(keywordDraft.trim());
    setPageNo(1);
  }

  function resetFilters() {
    setKeywordDraft("");
    setAppliedKeyword("");
    setPageNo(1);
  }

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          {queryPanelVisible ? (
            <BzCard
              className="admin-panel admin-filter-card"
              shadow="never"
            >
              <div
                ref={queryCardRef}
                className={[
                  "admin-query-layout",
                  querySingleRow ? "is-single-row" : queryExpanded ? "is-expanded" : "is-collapsed",
                ].join(" ")}
              >
                <div className="admin-query-header">
                  <div className="admin-query-title">筛选条件</div>
                  <div className="admin-query-actions">
                    <BzButton
                      className="admin-filter-secondary"
                      onClick={resetFilters}
                    >
                      重置
                    </BzButton>
                    <BzButton
                      className="admin-filter-primary"
                      buttonType="primary"
                      onClick={applyFilters}
                    >
                      搜索
                    </BzButton>
                    {!querySingleRow ? (
                      <button
                        className="admin-filter-toggle"
                        type="button"
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
                    <div className="admin-query-field__label">关键字</div>
                    <div className="admin-query-field__control">
                      <BzInput
                        modelValue={keywordDraft}
                        placeholder="搜索模块、路径、处理类、处理方法"
                        clearable
                        onValueChange={setKeywordDraft}
                        onKeyUp={(event) => event.key === "Enter" && applyFilters()}
                      />
                    </div>
                  </BzFormItem>
                </form>
              </div>
            </BzCard>
          ) : null}

          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">接口列表</div>
                <div className="admin-table-tools">
                  <button
                    className={`admin-vben-circle-button${queryPanelVisible ? " is-active" : ""}`}
                    type="button"
                    title={queryPanelVisible ? "关闭搜索框" : "打开搜索框"}
                    onClick={() => setQueryPanelVisible((value) => !value)}
                  >
                    <i
                      className="admin-vben-circle-button__icon admin-vben-circle-button__icon--search"
                      aria-hidden="true"
                    />
                  </button>
                  <button
                    className="admin-vben-circle-button"
                    type="button"
                    title="刷新列表"
                    onClick={() => void reload()}
                  >
                    <i
                      className="admin-vben-circle-button__icon admin-vben-circle-button__icon--refresh"
                      aria-hidden="true"
                    />
                  </button>
                </div>
              </div>
            }
          >
            <div className="admin-table-surface">
              <ApiTable
                rows={enrichedRows}
                loading={loading}
                canPublish={canPublish}
                canDisable={canDisable}
                onPublish={(api) => void onPublish(api)}
                onDisable={(api) => void onDisable(api)}
              />
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
      </div>
    </div>
  );

  async function onPublish(api: ApiEntry) {
    if (!canPublish) return;
    await publishApi(api.id);
    await reload();
  }

  async function onDisable(api: ApiEntry) {
    if (!canDisable) return;
    await disableApi(api.id);
    await reload();
  }
}

function toLabelMap(items?: DictItem[]): Record<string, string> {
  const map: Record<string, string> = {};
  for (const item of items || []) {
    if (!item.itemValue) continue;
    map[item.itemValue] = item.itemLabel || item.itemValue;
  }
  return map;
}

function resolveUserTypeLabels(raw?: string): string[] {
  return parseUserTypes(raw).map((code) => USER_TYPE_LABELS[code] || code);
}

function parseUserTypes(raw?: string): string[] {
  const normalized = raw?.trim();
  if (!normalized) return [];
  try {
    const parsed = JSON.parse(normalized);
    if (Array.isArray(parsed)) return parsed.map((item) => String(item).trim()).filter(Boolean);
  } catch {
    // ignore
  }
  return normalized
    .replace(/^\[|\]$/g, "")
    .split(",")
    .map((item) => item.replace(/^["'\s]+|["'\s]+$/g, ""))
    .filter(Boolean);
}

function buildAuditTooltip(api: ApiEntry): string {
  if (!api.auditDeclared) return "";
  const lines = [
    api.auditResource ? `审计资源：${api.auditResource}` : "",
    api.auditAction ? `审计动作：${api.auditAction}` : "",
    api.auditDescription ? `审计描述：${api.auditDescription}` : "",
  ].filter(Boolean);
  return lines.join("\n");
}
