package com.corwin.jsonfmt.interfaces.web.req;

/**
 * @author Corwin 2026/3/2
 */
public record JsonFmtRecordSaveReq(
        String id,
        String name,
        String content
) {
}
