package com.corwin.jsonfmt.application.command;

import java.util.List;

/**
 * @author Corwin 2026/3/11
 */
public record JsonFmtRecordBatchDeleteCommand(List<String> recordIds) {}
