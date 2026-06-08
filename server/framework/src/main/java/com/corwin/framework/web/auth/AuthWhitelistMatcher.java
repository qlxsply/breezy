package com.corwin.framework.web.auth;

import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.json.Json;
import com.corwin.framework.util.PathUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Corwin 2026/3/23
 */
@Slf4j
@Component
public class AuthWhitelistMatcher {

    public boolean matches(String requestPath) {
        String normalizedPath = PathUtil.normalize(requestPath);
        List<CompiledWhitelistRule> rules = ConfigRegistry.customV(DefaultConfigKeys.AUTH_WHITELIST,
                this::compileRulesFromRaw);
        for (CompiledWhitelistRule rule : rules) {
            if (rule.matches(normalizedPath)) {
                return true;
            }
        }
        return false;
    }

    private List<CompiledWhitelistRule> compileRulesFromRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        List<AuthWhitelistItem> items = Json.parse(raw, new TypeReference<>() {
        });
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompiledWhitelistRule> rules = new ArrayList<>(items.size());
        for (AuthWhitelistItem item : items) {
            rules.add(CompiledWhitelistRule.compile(item));
        }
        return Collections.unmodifiableList(rules);
    }

}
