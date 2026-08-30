package com.corwin.reminder.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.reminder.domain.model.TodoTask;
import com.corwin.reminder.domain.model.TodoTaskStatus;
import com.corwin.reminder.domain.repo.TodoTaskPageQuery;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 待办任务 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface TodoTaskMybatisMapper {

  PageData<TodoTask> pageByQuery(
      @Param("query") TodoTaskPageQuery query, @Param("spec") PageSpec spec);

  PageData<TodoTask> findByStatusInAndRemindAtLessThanEqual(
      @Param("statuses") List<TodoTaskStatus> statuses,
      @Param("now") Instant now,
      @Param("spec") PageSpec spec);
}
