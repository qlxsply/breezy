"use client";

import type { PageResult } from "@admin/shared/types/pagination";
import type { Dispatch, SetStateAction } from "react";
import { useCallback, useEffect, useReducer, useRef, useState } from "react";

export interface AdminPagedQueryRequest<F> {
  filters: Readonly<F>;
  pageNo: number;
  pageSize: number;
  signal: AbortSignal;
}

export interface UseAdminPagedQueryOptions<T, F> {
  initialFilters: F;
  query: (request: AdminPagedQueryRequest<F>) => Promise<PageResult<T>>;
  normalizeFilters?: (filters: F) => F;
  initialPageNo?: number;
  initialPageSize?: number;
  pageSizes?: readonly number[];
  enabled?: boolean;
  clearOnDisable?: boolean;
}

export interface AdminPagedQueryResult<T, F> {
  page: PageResult<T>;
  rows: readonly T[];
  draftFilters: F;
  appliedFilters: F;
  setDraftFilters: Dispatch<SetStateAction<F>>;
  pageNo: number;
  pageSize: number;
  pageSizes: readonly number[];
  loading: boolean;
  error: Error | null;
  submit: () => void;
  reset: () => void;
  refresh: () => Promise<void>;
  setPageNo: (pageNo: number) => void;
  setPageSize: (pageSize: number) => void;
  clear: () => void;
}

interface QueryState<F> {
  draftFilters: F;
  appliedFilters: F;
  pageNo: number;
  pageSize: number;
  revision: number;
}

type QueryAction<F> =
  | { type: "set-draft"; value: SetStateAction<F> }
  | { type: "submit"; filters: F }
  | { type: "reset"; filters: F }
  | { type: "set-page"; pageNo: number }
  | { type: "set-page-size"; pageSize: number }
  | { type: "reconcile-page"; pageNo: number; pageSize: number };

const DEFAULT_PAGE_SIZES = [10, 20, 30, 50, 100] as const;
const MAX_PAGE_FALLBACKS = 5;

export function useAdminPagedQuery<T, F>({
  initialFilters,
  query,
  normalizeFilters,
  initialPageNo = 1,
  initialPageSize = 10,
  pageSizes = DEFAULT_PAGE_SIZES,
  enabled = true,
  clearOnDisable = true,
}: UseAdminPagedQueryOptions<T, F>): AdminPagedQueryResult<T, F> {
  const firstPageNo = normalizePositiveInteger(initialPageNo, 1);
  const firstPageSize = normalizePositiveInteger(initialPageSize, 10);
  const initialFiltersRef = useRef(initialFilters);
  const queryRef = useRef(query);
  const normalizeFiltersRef = useRef(normalizeFilters);
  const enabledRef = useRef(enabled);
  queryRef.current = query;
  normalizeFiltersRef.current = normalizeFilters;
  enabledRef.current = enabled;

  const [queryState, dispatch] = useReducer(queryReducer<F>, {
    draftFilters: initialFiltersRef.current,
    appliedFilters: initialFiltersRef.current,
    pageNo: firstPageNo,
    pageSize: firstPageSize,
    revision: 0,
  });
  const queryStateRef = useRef(queryState);
  queryStateRef.current = queryState;
  const applyQueryAction = useCallback((action: QueryAction<F>) => {
    queryStateRef.current = queryReducer(queryStateRef.current, action);
    dispatch(action);
  }, []);

  const [page, setPage] = useState<PageResult<T>>(() => emptyPage(firstPageNo, firstPageSize));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const controllerRef = useRef<AbortController | null>(null);
  const requestSequenceRef = useRef(0);
  const startedRevisionRef = useRef<number | null>(null);

  const cancelCurrentRequest = useCallback(() => {
    requestSequenceRef.current += 1;
    controllerRef.current?.abort();
    controllerRef.current = null;
  }, []);

  const execute = useCallback(
    async (snapshot: QueryState<F>): Promise<void> => {
      if (!enabledRef.current) return;
      cancelCurrentRequest();
      const controller = new AbortController();
      const sequence = requestSequenceRef.current;
      controllerRef.current = controller;
      startedRevisionRef.current = snapshot.revision;
      setLoading(true);
      setError(null);

      try {
        let targetPage = snapshot.pageNo;
        let result: PageResult<T>;
        let fallbacks = 0;
        while (true) {
          result = await queryRef.current({
            filters: snapshot.appliedFilters,
            pageNo: targetPage,
            pageSize: snapshot.pageSize,
            signal: controller.signal,
          });
          if (
            !enabledRef.current ||
            !isCurrentRequest(sequence, controller, requestSequenceRef, controllerRef)
          ) {
            return;
          }
          const lastPage = Math.max(1, normalizeNonNegativeInteger(result.totalPages));
          if (targetPage <= lastPage) break;
          if (fallbacks >= MAX_PAGE_FALLBACKS) {
            throw new Error("列表数据持续变化，请刷新后重试");
          }
          targetPage = lastPage;
          fallbacks += 1;
        }

        const latestLastPage = Math.max(1, normalizeNonNegativeInteger(result.totalPages));
        const resolvedPageNo = Math.min(
          normalizePositiveInteger(result.pageNo, targetPage),
          latestLastPage,
        );
        const resolvedPageSize = normalizePositiveInteger(result.pageSize, snapshot.pageSize);
        setPage({ ...result, pageNo: resolvedPageNo, pageSize: resolvedPageSize });
        applyQueryAction({
          type: "reconcile-page",
          pageNo: resolvedPageNo,
          pageSize: resolvedPageSize,
        });
      } catch (cause) {
        if (
          enabledRef.current &&
          isCurrentRequest(sequence, controller, requestSequenceRef, controllerRef) &&
          !isAbortError(cause)
        ) {
          setError(toError(cause));
        }
      } finally {
        if (isCurrentRequest(sequence, controller, requestSequenceRef, controllerRef)) {
          controllerRef.current = null;
          setLoading(false);
        }
      }
    },
    [applyQueryAction, cancelCurrentRequest],
  );

  useEffect(() => {
    const effectRevision = queryState.revision;
    const cancelEffectRequest = () => {
      if (startedRevisionRef.current !== effectRevision) return;
      cancelCurrentRequest();
      startedRevisionRef.current = null;
    };
    if (!enabled) {
      cancelCurrentRequest();
      startedRevisionRef.current = null;
      setLoading(false);
      setError(null);
      if (clearOnDisable) setPage(emptyPage(queryState.pageNo, queryState.pageSize));
      return;
    }
    if (startedRevisionRef.current === effectRevision) return cancelEffectRequest;
    void execute(queryState);
    return cancelEffectRequest;
  }, [cancelCurrentRequest, clearOnDisable, enabled, execute, queryState.revision]);

  const setDraftFilters = useCallback<Dispatch<SetStateAction<F>>>(
    (value) => {
      applyQueryAction({ type: "set-draft", value });
    },
    [applyQueryAction],
  );

  const submit = useCallback(() => {
    const current = queryStateRef.current;
    const filters = normalizeFiltersRef.current
      ? normalizeFiltersRef.current(current.draftFilters)
      : current.draftFilters;
    cancelCurrentRequest();
    applyQueryAction({ type: "submit", filters });
  }, [applyQueryAction, cancelCurrentRequest]);

  const reset = useCallback(() => {
    cancelCurrentRequest();
    applyQueryAction({ type: "reset", filters: initialFiltersRef.current });
  }, [applyQueryAction, cancelCurrentRequest]);

  const refresh = useCallback(() => execute(queryStateRef.current), [execute]);

  const setPageNo = useCallback(
    (pageNo: number) => {
      const normalized = normalizePositiveInteger(pageNo, 1);
      if (normalized === queryStateRef.current.pageNo) return;
      cancelCurrentRequest();
      applyQueryAction({ type: "set-page", pageNo: normalized });
    },
    [applyQueryAction, cancelCurrentRequest],
  );

  const setPageSize = useCallback(
    (pageSize: number) => {
      if (!Number.isFinite(pageSize) || pageSize <= 0) return;
      const normalized = Math.floor(pageSize);
      if (normalized === queryStateRef.current.pageSize) return;
      cancelCurrentRequest();
      applyQueryAction({ type: "set-page-size", pageSize: normalized });
    },
    [applyQueryAction, cancelCurrentRequest],
  );

  const clear = useCallback(() => {
    cancelCurrentRequest();
    setLoading(false);
    setError(null);
    const current = queryStateRef.current;
    setPage(emptyPage(current.pageNo, current.pageSize));
  }, [cancelCurrentRequest]);

  return {
    page,
    rows: page.elements,
    draftFilters: queryState.draftFilters,
    appliedFilters: queryState.appliedFilters,
    setDraftFilters,
    pageNo: queryState.pageNo,
    pageSize: queryState.pageSize,
    pageSizes,
    loading,
    error,
    submit,
    reset,
    refresh,
    setPageNo,
    setPageSize,
    clear,
  };
}

function queryReducer<F>(state: QueryState<F>, action: QueryAction<F>): QueryState<F> {
  switch (action.type) {
    case "set-draft":
      return {
        ...state,
        draftFilters:
          typeof action.value === "function"
            ? (action.value as (previous: F) => F)(state.draftFilters)
            : action.value,
      };
    case "submit":
      return {
        ...state,
        draftFilters: action.filters,
        appliedFilters: action.filters,
        pageNo: 1,
        revision: state.revision + 1,
      };
    case "reset":
      return {
        ...state,
        draftFilters: action.filters,
        appliedFilters: action.filters,
        pageNo: 1,
        revision: state.revision + 1,
      };
    case "set-page":
      return action.pageNo === state.pageNo
        ? state
        : { ...state, pageNo: action.pageNo, revision: state.revision + 1 };
    case "set-page-size":
      return action.pageSize === state.pageSize
        ? state
        : { ...state, pageNo: 1, pageSize: action.pageSize, revision: state.revision + 1 };
    case "reconcile-page":
      return action.pageNo === state.pageNo && action.pageSize === state.pageSize
        ? state
        : { ...state, pageNo: action.pageNo, pageSize: action.pageSize };
  }
}

function emptyPage<T>(pageNo: number, pageSize: number): PageResult<T> {
  return {
    pageNo,
    pageSize,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  };
}

function isCurrentRequest(
  sequence: number,
  controller: AbortController,
  sequenceRef: { current: number },
  controllerRef: { current: AbortController | null },
): boolean {
  return (
    sequence === sequenceRef.current &&
    controller === controllerRef.current &&
    !controller.signal.aborted
  );
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === "AbortError";
}

function toError(error: unknown): Error {
  return error instanceof Error ? error : new Error("列表加载失败");
}

function normalizePositiveInteger(value: number, fallback: number): number {
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : fallback;
}

function normalizeNonNegativeInteger(value: number): number {
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 0;
}
