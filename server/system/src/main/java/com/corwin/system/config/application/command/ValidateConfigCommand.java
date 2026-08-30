package com.corwin.system.config.application.command;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Corwin 2026/7/30
 */
public record ValidateConfigCommand(String configKey, JsonNode value) {}
