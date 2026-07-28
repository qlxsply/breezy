package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.scheduler.domain.model.SchedulerJobExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 调度任务执行历史 MyBatis 查询 Mapper。
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface SchedulerJobExecutionMybatisMapper {

    PageData<SchedulerJobExecution> pageByJobId(@Param("jobId") String jobId, @Param("spec") PageSpec spec);
}
