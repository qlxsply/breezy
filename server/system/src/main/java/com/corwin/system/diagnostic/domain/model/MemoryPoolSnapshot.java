package com.corwin.system.diagnostic.domain.model;

/**
 * @author Corwin 2026/4/16
 */
public record MemoryPoolSnapshot(
        String name,
        long usedBytes,
        long committedBytes,
        long maxBytes
) {
}
