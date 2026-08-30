package com.corwin.system.diagnostic.interfaces.web.req;

/**
 * Request DTO for querying historical diagnostic snapshots.
 *
 * @author Corwin 2026/7/29
 */
public record DiagnosticHistoryReq(Integer limit) {}
