package com.corwin.system.diagnostic.domain.model;

import java.util.List;

/**
 * Snapshot of a single network interface including status and IP addresses.
 *
 * @author Corwin 2026/4/16
 */
public record NetworkInterfaceSnapshot(
        String name,
        String displayName,
        boolean up,
        boolean loopback,
        List<String> addresses
) {

    /**
     * Compact constructor that normalises a null addresses list to an immutable empty list.
     */
    public NetworkInterfaceSnapshot {
        addresses = addresses == null ? List.of() : List.copyOf(addresses);
    }
}
