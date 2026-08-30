package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseColumn;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 数据列 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface DatabaseColumnMybatisMapper {

  PageData<DatabaseColumn> page(
      @Param("tableId") Long tableId,
      @Param("nameLike") String nameLike,
      @Param("spec") PageSpec spec);
}
