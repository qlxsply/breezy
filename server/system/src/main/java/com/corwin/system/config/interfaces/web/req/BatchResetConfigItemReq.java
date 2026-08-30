package com.corwin.system.config.interfaces.web.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * @author Corwin 2026/8/3
 */
public record BatchResetConfigItemReq(
    @NotBlank @Size(max = 160) String configKey, @NotNull @PositiveOrZero Long expectedRevision) {}
