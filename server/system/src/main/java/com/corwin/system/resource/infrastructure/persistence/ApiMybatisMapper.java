package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.repo.ApiPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for paginated API metadata queries.
 *
 * <p>Provides dynamic SQL-based pagination and filtering for the API admin page.</p>
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface ApiMybatisMapper {

    /**
     * Executes a paginated query with dynamic filtering for API metadata.
     *
     * @param query the query filter criteria
     * @param spec  the page specification with sorting
     * @return the paginated API data
     */
    PageData<Api> page(@Param("query") ApiPageQuery query, @Param("spec") PageSpec spec);
}
