package com.corwin.system.dict.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.dict.domain.model.DictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字典类型 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface DictTypeMybatisMapper {

    PageData<DictType> page(@Param("code") String code, @Param("name") String name, @Param("spec") PageSpec spec);
}
