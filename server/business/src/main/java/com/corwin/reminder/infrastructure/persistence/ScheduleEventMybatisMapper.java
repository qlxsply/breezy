package com.corwin.reminder.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.reminder.domain.model.ScheduleEvent;
import com.corwin.reminder.domain.model.ScheduleEventStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 日程事件 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface ScheduleEventMybatisMapper {

    PageData<ScheduleEvent> page(@Param("status") ScheduleEventStatus status, @Param("titleLike") String titleLike,
            @Param("spec") PageSpec spec);
}
