package com.corwin.system.user.interfaces.web.req;

import java.util.List;

/**
 * Request object containing a list of user IDs for batch operations.
 *
 * @author Corwin 2026/7/7
 */
public record BatchUserIdsReq(List<Long> userIds) {}
