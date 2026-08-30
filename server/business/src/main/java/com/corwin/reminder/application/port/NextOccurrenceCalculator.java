package com.corwin.reminder.application.port;

import com.corwin.reminder.domain.model.ScheduleEvent;
import java.time.Instant;

/**
 * @author Corwin 2026/1/12
 */
public interface NextOccurrenceCalculator {

  /**
   * @return 下一次发生的开始时间；若无下一次（结束/取消）则返回 null
   */
  Instant nextStart(ScheduleEvent event, Instant fromExclusive);
}
