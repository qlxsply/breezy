package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.scheduler.domain.model.SchedulerJobExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for paginated job execution history queries.
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface SchedulerJobExecutionMybatisMapper {

    /**
     * Paginated query for execution records of a specific job.
     *
     * @param jobId the job ID
     * @param spec  pagination specification
     * @return paginated execution data
     */
    PageData<SchedulerJobExecution> pageByJobId(@Param("jobId") String jobId, @Param("spec") PageSpec spec);
}
