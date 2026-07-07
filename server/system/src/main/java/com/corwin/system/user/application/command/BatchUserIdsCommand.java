package com.corwin.system.user.application.command;

import java.util.List;

/**
 * @author Corwin 2026/7/7
 */
public record BatchUserIdsCommand(
        List<Long> userIds
) {
}
