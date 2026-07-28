package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.userfeature.domain.model.ProductApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 产品应用 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface ProductApplicationMybatisMapper {

    PageData<ProductApplication> page(@Param("keyword") String keyword, @Param("enabled") Boolean enabled,
            @Param("spec") PageSpec spec);
}
