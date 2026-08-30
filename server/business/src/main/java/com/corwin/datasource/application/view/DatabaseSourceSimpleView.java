package com.corwin.datasource.application.view;

import com.corwin.datasource.domain.model.DatabaseSourceSourceType;
import com.corwin.datasource.domain.model.DatabaseSourceStatus;
import com.corwin.datasource.domain.model.DatabaseType;

/**
 * @author Corwin 2026/3/30
 */
public record DatabaseSourceSimpleView(
    Long id,
    String name,
    DatabaseType dbType,
    String username,
    DatabaseSourceStatus status,
    DatabaseSourceSourceType sourceType) {}
