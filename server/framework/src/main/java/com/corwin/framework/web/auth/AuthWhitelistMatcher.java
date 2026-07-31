package com.corwin.framework.web.auth;

import com.corwin.framework.config.builtin.FrameworkConfigSpecs;
import com.corwin.framework.config.runtime.ConfigSnapshot;
import com.corwin.framework.config.runtime.Configs;
import com.corwin.framework.util.PathUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Corwin 2026/3/23
 */
@Component
public class AuthWhitelistMatcher {

    private volatile CompiledRules compiledRules = new CompiledRules(-1, List.of());

    public boolean matches(String requestPath) {
        String normalizedPath = PathUtil.normalize(requestPath);
        for (CompiledWhitelistRule rule : currentRules()) {
            if (rule.matches(normalizedPath)) {
                return true;
            }
        }
        return false;
    }

    private List<CompiledWhitelistRule> currentRules() {
        ConfigSnapshot<FrameworkConfigSpecs.AuthWhitelistConfig> snapshot =
                Configs.snapshot(FrameworkConfigSpecs.AUTH_WHITELIST);
        CompiledRules current = compiledRules;
        if (current.revision() == snapshot.revision()) {
            return current.rules();
        }
        synchronized (this) {
            current = compiledRules;
            if (current.revision() != snapshot.revision()) {
                List<CompiledWhitelistRule> rules = snapshot.value().rules().stream()
                        .map(CompiledWhitelistRule::compile)
                        .toList();
                compiledRules = new CompiledRules(snapshot.revision(), rules);
            }
            return compiledRules.rules();
        }
    }

    private record CompiledRules(long revision, List<CompiledWhitelistRule> rules) {
        private CompiledRules {
            rules = List.copyOf(rules);
        }
    }
}
