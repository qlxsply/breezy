package com.corwin.system.user.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for paginated user queries.
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface UserMybatisMapper {

  /**
   * Paginates users with optional status and username filters using MyBatis.
   *
   * @param status optional status filter
   * @param username optional username pattern (LIKE pattern)
   * @param spec the pagination specification
   * @return a page of users
   */
  PageData<User> page(
      @Param("status") UserStatus status,
      @Param("username") String username,
      @Param("spec") PageSpec spec);
}
