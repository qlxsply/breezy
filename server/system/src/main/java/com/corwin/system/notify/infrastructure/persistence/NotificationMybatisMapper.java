package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.notify.domain.model.Notification;
import com.corwin.system.notify.domain.repo.NotificationPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for custom paginated notification queries.
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface NotificationMybatisMapper {

    /**
     * Executes a paginated query for notifications based on the given filter criteria.
     *
     * @param query the notification page query filter
     * @param spec  the page specification (page number, size, sort)
     * @return the paginated notification data
     */
    PageData<Notification> pageByQuery(@Param("query") NotificationPageQuery query, @Param("spec") PageSpec spec);
}
