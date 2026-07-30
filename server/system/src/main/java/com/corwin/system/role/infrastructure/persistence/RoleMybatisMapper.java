package com.corwin.system.role.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.role.domain.model.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for paginated role queries with dynamic filters.
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface RoleMybatisMapper {

    /**
     * Paginated query with keyword and enabled filter.
     *
     * @param keyword keyword pattern for code/name search
     * @param enabled optional enabled filter
     * @param spec    pagination specification
     * @return paginated role data
     */
    PageData<Role> page(@Param("keyword") String keyword, @Param("enabled") Boolean enabled,
            @Param("spec") PageSpec spec);
}
