package com.corwin.reminder.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.reminder.domain.model.ScheduleEvent;
import com.corwin.reminder.domain.model.ScheduleEventStatus;

/**
 *
 * @author Corwin 2026/1/12
 */
public interface ScheduleEventRepository extends DomainRepository<ScheduleEvent, Long> {

    PageData<ScheduleEvent> findAll(PageSpec spec);

    PageData<ScheduleEvent> findByStatus(ScheduleEventStatus status, PageSpec spec);

    PageData<ScheduleEvent> findByStatusAndTitleContainingIgnoreCase(ScheduleEventStatus status, String titleLike,
            PageSpec spec);
}
