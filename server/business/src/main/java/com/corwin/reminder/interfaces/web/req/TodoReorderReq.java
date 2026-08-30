package com.corwin.reminder.interfaces.web.req;

import com.corwin.reminder.domain.model.TodoTaskStatus;
import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public record TodoReorderReq(List<GroupReq> groups) {

  public record GroupReq(TodoTaskStatus status, List<Long> orderedIds) {}
}
