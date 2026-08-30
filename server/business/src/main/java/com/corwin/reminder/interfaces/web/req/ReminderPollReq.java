package com.corwin.reminder.interfaces.web.req;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Corwin 2026/1/28
 */
@Getter
@Setter
public class ReminderPollReq {
  private Instant startTime;
}
