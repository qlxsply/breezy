package com.corwin.reminder.interfaces.web.req;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * @author Corwin 2026/1/28
 */
@Getter
@Setter
public class ReminderPollReq {
    private Instant startTime;
}
