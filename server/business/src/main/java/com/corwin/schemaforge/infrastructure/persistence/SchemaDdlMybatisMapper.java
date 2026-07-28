package com.corwin.schemaforge.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.schemaforge.interfaces.web.res.SchemaDdlRes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 数据库 DDL MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface SchemaDdlMybatisMapper {

    PageData<SchemaDdlRes> pageQuery(@Param("managedDatabaseId") Long managedDatabaseId,
            @Param("nameLike") String nameLike, @Param("spec") PageSpec spec);
}
