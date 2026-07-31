package com.corwin.system.config.interfaces.web.req;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * @author Corwin 2026/7/31
 */
public record ValidateConfigReq(
        @NotNull
        JsonNode value
) {
}
