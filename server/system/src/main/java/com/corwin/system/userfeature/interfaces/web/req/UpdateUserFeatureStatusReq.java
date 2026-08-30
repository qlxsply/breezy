package com.corwin.system.userfeature.interfaces.web.req;

/**
 * Request DTO for updating the enabled/disabled status of an application or package.
 *
 * @param enabled true to enable, false to disable
 * @author Corwin 2026/6/14
 */
public record UpdateUserFeatureStatusReq(boolean enabled) {}
