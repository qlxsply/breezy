package com.corwin.framework.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;

/**
 * Repository interface for dynamic paginated queries.
 * <p>
 * Implementations provide filtered, sorted, and paginated queries
 * based on a domain-specific query object.
 *
 * @param <T> the result entity type
 * @param <Q> the query condition type
 * @author Corwin 2026/4/9
 */
public interface DynamicPageQueryRepository<T, Q> {

    PageData<T> pageByQuery(Q query, PageSpec spec);
}
