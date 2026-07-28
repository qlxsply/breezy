package com.corwin.system.role.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.role.domain.model.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface RoleMybatisMapper {

    PageData<Role> page(@Param("keyword") String keyword, @Param("enabled") Boolean enabled,
            @Param("spec") PageSpec spec);
}
