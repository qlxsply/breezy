package com.corwin.system.diagnostic.domain.model;

/**
 * Snapshot of a single JVM memory pool (heap or non-heap) usage metrics.
 *
 * @author Corwin 2026/4/16
 */
public record MemoryPoolSnapshot(String name, long usedBytes, long committedBytes, long maxBytes) {}
