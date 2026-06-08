package com.corwin.system.diagnostic.application.view;

import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
public record DiagnosticCapabilityView(
        boolean jfrAvailable,
        boolean httpAvailable,
        boolean dbPoolAvailable,
        boolean sqlAvailable,
        boolean deepModeSupported,
        List<String> dataSourceNames
) {

    public DiagnosticCapabilityView {
        dataSourceNames = dataSourceNames == null ? List.of() : List.copyOf(dataSourceNames);
    }
}
