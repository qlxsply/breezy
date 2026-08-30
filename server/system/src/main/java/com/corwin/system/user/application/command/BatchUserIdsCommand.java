package com.corwin.system.user.application.command;

import java.util.List;

/**
 * Command containing a list of user IDs for batch operations.
 *
 * @author Corwin 2026/7/7
 */
public record BatchUserIdsCommand(List<Long> userIds) {}
