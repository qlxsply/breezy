package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.auth.domain.model.LoginEvent;
import com.corwin.system.auth.domain.repo.LoginLogPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for paginated login log queries.
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface LoginLogMybatisMapper {

  /**
   * Executes a paginated query for login events based on the given search criteria.
   *
   * @param query the page query criteria
   * @param spec the page and sort specification
   * @return paginated result of {@link LoginEvent}
   */
  PageData<LoginEvent> pageByQuery(
      @Param("query") LoginLogPageQuery query, @Param("spec") PageSpec spec);
}
