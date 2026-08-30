package com.corwin.bootstrap.application;

/**
 * @author Corwin 2026/4/22
 */
public record BootstrapTaskReport(
    BootstrapTaskKey task, boolean dryRun, boolean success, long durationMs, String message) {}
