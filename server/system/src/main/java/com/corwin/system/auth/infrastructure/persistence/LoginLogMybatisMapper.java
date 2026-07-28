package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.auth.domain.model.LoginEvent;
import com.corwin.system.auth.domain.repo.LoginLogPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 登录日志 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface LoginLogMybatisMapper {

    PageData<LoginEvent> pageByQuery(@Param("query") LoginLogPageQuery query, @Param("spec") PageSpec spec);
}
