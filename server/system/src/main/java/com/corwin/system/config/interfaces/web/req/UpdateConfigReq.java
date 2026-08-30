package com.corwin.system.config.interfaces.web.req;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * @author Corwin 2026/7/31
 */
public record UpdateConfigReq(
    @NotNull @PositiveOrZero Long expectedRevision,
    @Size(max = 500) String reason,
    @NotNull JsonNode value) {}
