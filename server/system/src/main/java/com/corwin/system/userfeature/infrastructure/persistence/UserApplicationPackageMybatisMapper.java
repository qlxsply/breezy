package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.userfeature.domain.model.UserApplicationPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for paginated queries of {@link
 * com.corwin.system.userfeature.domain.model.UserApplicationPackage}.
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface UserApplicationPackageMybatisMapper {

  PageData<UserApplicationPackage> page(
      @Param("keyword") String keyword,
      @Param("enabled") Boolean enabled,
      @Param("spec") PageSpec spec);
}
