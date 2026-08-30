package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.model.WebUserStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for paginated web user queries.
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface WebUserMybatisMapper {

  /**
   * Paginated query for web users with optional keyword and status filtering.
   *
   * @param keyword optional LIKE pattern for display/nickname search
   * @param status optional status filter
   * @param spec pagination and sorting specification
   * @return paginated result
   */
  PageData<WebUser> page(
      @Param("keyword") String keyword,
      @Param("status") WebUserStatus status,
      @Param("spec") PageSpec spec);
}
