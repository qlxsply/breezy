package com.corwin.datasource.application.view;

/**
 *
 * @author Corwin 2026/2/5
 */
public record TestConnectionView(
        boolean success,
        String message
) {

    public static TestConnectionView ok() {
        return new TestConnectionView(true, null);
    }

    public static TestConnectionView failed(String message) {
        return new TestConnectionView(false, message);
    }
}
