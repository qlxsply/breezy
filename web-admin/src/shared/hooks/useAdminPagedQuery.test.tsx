import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import { type ReactNode, StrictMode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { PageResult } from "../types/pagination";
import { type AdminPagedQueryRequest, useAdminPagedQuery } from "./useAdminPagedQuery";

interface TestFilters {
  keyword: string;
}

interface TestRow {
  id: string;
}

afterEach(cleanup);

describe("useAdminPagedQuery", () => {
  it("keeps draft filters separate and submits normalized filters", async () => {
    const query = vi.fn(
      async ({ filters, pageNo, pageSize }: AdminPagedQueryRequest<TestFilters>) =>
        createPage([{ id: filters.keyword || "initial" }], pageNo, pageSize),
    );
    const { result } = renderHook(() =>
      useAdminPagedQuery<TestRow, TestFilters>({
        initialFilters: { keyword: "" },
        normalizeFilters: (filters) => ({ keyword: filters.keyword.trim() }),
        query,
      }),
    );

    await waitFor(() => expect(result.current.rows[0]?.id).toBe("initial"));
    act(() => result.current.setDraftFilters({ keyword: "  next  " }));
    expect(query).toHaveBeenCalledTimes(1);
    expect(result.current.appliedFilters.keyword).toBe("");

    act(() => result.current.submit());
    await waitFor(() => expect(result.current.rows[0]?.id).toBe("next"));
    expect(result.current.appliedFilters.keyword).toBe("next");
    expect(result.current.pageNo).toBe(1);
  });

  it("restarts the initial request after the Strict Mode effect probe", async () => {
    const query = vi.fn(async ({ pageNo, pageSize }: AdminPagedQueryRequest<TestFilters>) =>
      createPage([{ id: "strict" }], pageNo, pageSize),
    );
    const { result } = renderHook(
      () =>
        useAdminPagedQuery<TestRow, TestFilters>({
          initialFilters: { keyword: "" },
          query,
        }),
      { wrapper: ({ children }: { children: ReactNode }) => <StrictMode>{children}</StrictMode> },
    );

    await waitFor(() => expect(result.current.rows[0]?.id).toBe("strict"));
    expect(query).toHaveBeenCalled();
    expect(result.current.loading).toBe(false);
  });

  it("aborts and discards stale page responses", async () => {
    const requests: Array<{
      request: AdminPagedQueryRequest<TestFilters>;
      resolve: (page: PageResult<TestRow>) => void;
    }> = [];
    const query = vi.fn(
      (request: AdminPagedQueryRequest<TestFilters>) =>
        new Promise<PageResult<TestRow>>((resolve) => requests.push({ request, resolve })),
    );
    const { result } = renderHook(() =>
      useAdminPagedQuery<TestRow, TestFilters>({
        initialFilters: { keyword: "" },
        query,
      }),
    );

    await waitFor(() => expect(requests).toHaveLength(1));
    act(() => result.current.setPageNo(2));
    expect(requests[0].request.signal.aborted).toBe(true);
    await waitFor(() => expect(requests).toHaveLength(2));

    await act(async () => {
      requests[1].resolve(createPage([{ id: "new" }], 2, 10, 2, 20));
    });
    expect(result.current.rows[0]?.id).toBe("new");

    await act(async () => {
      requests[0].resolve(createPage([{ id: "stale" }], 1, 10, 2, 20));
    });
    expect(result.current.rows[0]?.id).toBe("new");
    expect(result.current.pageNo).toBe(2);
    expect(result.current.loading).toBe(false);
  });

  it("follows consecutive last-page reductions without issuing a duplicate effect request", async () => {
    const query = vi.fn(async ({ pageNo, pageSize }: AdminPagedQueryRequest<TestFilters>) =>
      pageNo === 3
        ? createPage([], 3, pageSize, 2, 15)
        : pageNo === 2
          ? createPage([], 2, pageSize, 1, 9)
          : createPage([{ id: "last-page" }], 1, pageSize, 1, 9),
    );
    const { result } = renderHook(() =>
      useAdminPagedQuery<TestRow, TestFilters>({
        initialFilters: { keyword: "" },
        initialPageNo: 3,
        query,
      }),
    );

    await waitFor(() => expect(result.current.rows[0]?.id).toBe("last-page"));
    expect(query).toHaveBeenCalledTimes(3);
    expect(query.mock.calls.map(([request]) => request.pageNo)).toEqual([3, 2, 1]);
    expect(result.current.pageNo).toBe(1);
  });

  it("preserves the previous page when refresh fails", async () => {
    const query = vi.fn(async ({ pageNo, pageSize }: AdminPagedQueryRequest<TestFilters>) =>
      createPage([{ id: "retained" }], pageNo, pageSize),
    );
    const { result } = renderHook(() =>
      useAdminPagedQuery<TestRow, TestFilters>({
        initialFilters: { keyword: "" },
        query,
      }),
    );
    await waitFor(() => expect(result.current.rows[0]?.id).toBe("retained"));
    query.mockRejectedValueOnce(new Error("refresh failed"));

    await act(async () => result.current.refresh());

    expect(result.current.rows[0]?.id).toBe("retained");
    expect(result.current.error?.message).toBe("refresh failed");
    expect(result.current.loading).toBe(false);
  });

  it("does not refresh or retain sensitive rows while disabled", async () => {
    const query = vi.fn(async ({ pageNo, pageSize }: AdminPagedQueryRequest<TestFilters>) =>
      createPage([{ id: "protected" }], pageNo, pageSize),
    );
    const { result, rerender } = renderHook(
      ({ enabled }) =>
        useAdminPagedQuery<TestRow, TestFilters>({
          initialFilters: { keyword: "" },
          enabled,
          query,
        }),
      { initialProps: { enabled: true } },
    );
    await waitFor(() => expect(result.current.rows[0]?.id).toBe("protected"));

    rerender({ enabled: false });
    await waitFor(() => expect(result.current.rows).toHaveLength(0));
    const callsBeforeRefresh = query.mock.calls.length;
    await act(async () => result.current.refresh());

    expect(query).toHaveBeenCalledTimes(callsBeforeRefresh);
    expect(result.current.rows).toHaveLength(0);
  });

  it("uses the latest batched action snapshot without a duplicate effect request", async () => {
    let resolveSecondPage: (page: PageResult<TestRow>) => void = () => {};
    const secondPage = { signal: null as AbortSignal | null };
    const query = vi.fn((request: AdminPagedQueryRequest<TestFilters>) => {
      if (request.pageNo === 2) {
        secondPage.signal = request.signal;
        return new Promise<PageResult<TestRow>>((resolve) => {
          resolveSecondPage = resolve;
        });
      }
      return Promise.resolve(
        createPage([{ id: `page-${request.pageNo}` }], request.pageNo, request.pageSize, 2, 20),
      );
    });
    const { result } = renderHook(() =>
      useAdminPagedQuery<TestRow, TestFilters>({
        initialFilters: { keyword: "" },
        query,
      }),
    );
    await waitFor(() => expect(result.current.rows[0]?.id).toBe("page-1"));

    let refreshPromise: Promise<void> = Promise.resolve();
    act(() => {
      result.current.setPageNo(2);
      refreshPromise = result.current.refresh();
    });
    await waitFor(() => expect(query).toHaveBeenCalledTimes(2));
    expect(secondPage.signal?.aborted).toBe(false);
    await act(async () => {
      resolveSecondPage(createPage([{ id: "page-2" }], 2, 10, 2, 20));
      await refreshPromise;
    });
    expect(result.current.rows[0]?.id).toBe("page-2");
    expect(query).toHaveBeenCalledTimes(2);

    act(() => {
      result.current.setPageNo(1);
      result.current.setPageNo(2);
      result.current.setPageNo(1);
    });
    await waitFor(() => expect(result.current.rows[0]?.id).toBe("page-1"));
    expect(result.current.pageNo).toBe(1);
  });
});

function createPage<T>(
  elements: T[],
  pageNo: number,
  pageSize: number,
  totalPages = elements.length > 0 ? 1 : 0,
  totalElements = elements.length,
): PageResult<T> {
  return {
    pageNo,
    pageSize,
    numberOfElements: elements.length,
    totalPages,
    totalElements,
    elements,
  };
}
