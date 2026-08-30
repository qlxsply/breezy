package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseTable;
import com.corwin.datasource.domain.repo.DatabaseTablePageQuery;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 数据表 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface DatabaseTableMybatisMapper {

  PageData<DatabaseTable> pageByQuery(
      @Param("query") DatabaseTablePageQuery query, @Param("spec") PageSpec spec);
}
