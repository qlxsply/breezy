package com.corwin.system.diagnostic.domain.repo;

import com.corwin.system.diagnostic.domain.model.DiagnosticEvent;

import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
public interface DiagnosticEventRepository {

    void configureCapacity(int capacity);

    void append(DiagnosticEvent event);

    List<DiagnosticEvent> latest(int limit);

    void clear();
}
