package com.corwin.reminder.application.command;

import com.corwin.reminder.domain.model.TodoTaskStatus;

import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public record TodoReorderCommand(
        List<GroupCommand> groups
) {

    public record GroupCommand(
            TodoTaskStatus status,
            List<Long> orderedIds
    ) {
    }
}
