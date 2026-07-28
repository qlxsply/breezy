package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.repo.ApiPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * API 元数据 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface ApiMybatisMapper {

    PageData<Api> page(@Param("query") ApiPageQuery query, @Param("spec") PageSpec spec);
}
