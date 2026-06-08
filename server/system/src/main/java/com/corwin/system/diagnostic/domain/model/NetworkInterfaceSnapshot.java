package com.corwin.system.diagnostic.domain.model;

import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
public record NetworkInterfaceSnapshot(
        String name,
        String displayName,
        boolean up,
        boolean loopback,
        List<String> addresses
) {

    public NetworkInterfaceSnapshot {
        addresses = addresses == null ? List.of() : List.copyOf(addresses);
    }
}
