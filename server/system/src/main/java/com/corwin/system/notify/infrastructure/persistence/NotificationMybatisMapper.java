package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.notify.domain.model.Notification;
import com.corwin.system.notify.domain.repo.NotificationPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 通知 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface NotificationMybatisMapper {

    PageData<Notification> pageByQuery(@Param("query") NotificationPageQuery query, @Param("spec") PageSpec spec);
}
