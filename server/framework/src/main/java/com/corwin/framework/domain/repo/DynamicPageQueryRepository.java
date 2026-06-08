package com.corwin.framework.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;

/**
 * 动态条件查询仓储接口
 *
 * @author Corwin 2026/4/9
 */
public interface DynamicPageQueryRepository<T, Q> {

    PageData<T> pageByQuery(Q query, PageSpec spec);
}
