package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.repo.DatabaseSourcePageQuery;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 数据源 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface DatabaseSourceMybatisMapper {

    PageData<DatabaseSource> pageByQuery(@Param("query") DatabaseSourcePageQuery query, @Param("spec") PageSpec spec);
}
