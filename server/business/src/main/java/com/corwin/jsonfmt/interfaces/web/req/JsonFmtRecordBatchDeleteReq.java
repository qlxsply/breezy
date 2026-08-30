package com.corwin.jsonfmt.interfaces.web.req;

import java.util.List;

/**
 * @author Corwin 2026/3/11
 */
public record JsonFmtRecordBatchDeleteReq(List<String> recordIds) {}
