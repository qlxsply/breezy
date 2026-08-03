package com.corwin.system.config.interfaces.web.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @author Corwin 2026/8/3
 */
public record BatchResetConfigReq(
        @NotEmpty
        @Size(max = 100)
        List<@Valid BatchResetConfigItemReq> items,
        @Size(max = 500)
        String reason
) {

    public BatchResetConfigReq {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
