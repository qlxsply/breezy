package com.corwin.framework.web.auth;

import com.corwin.framework.util.PathUtil;
import lombok.Getter;
import org.springframework.http.server.PathContainer;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * @author Corwin 2026/3/23
 */
@Getter
public final class CompiledWhitelistRule {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

    private final WhitelistMatchType matchType;
    private final String pattern;
    private final PathPattern compiledPathPattern;

    private CompiledWhitelistRule(WhitelistMatchType matchType, String pattern, PathPattern compiledPathPattern) {
        this.matchType = matchType;
        this.pattern = pattern;
        this.compiledPathPattern = compiledPathPattern;
    }

    public boolean matches(String requestPath) {
        String normalizedPath = PathUtil.normalize(requestPath);
        return switch (this.matchType) {
            case EXACT -> this.pattern.equals(normalizedPath);
            case ANT -> ANT_PATH_MATCHER.match(this.pattern, normalizedPath);
            case PATH_PATTERN -> compiledPathPattern.matches(PathContainer.parsePath(normalizedPath));
        };
    }

    public static CompiledWhitelistRule compile(AuthWhitelistItem item) {
        if (item == null) {
            throw new IllegalArgumentException("whitelist item cannot be null");
        }

        WhitelistMatchType matchType = WhitelistMatchType.fromCode(item.type());
        String normalizedPattern = PathUtil.normalize(item.pattern());

        PathPattern pathPattern = null;
        if (matchType == WhitelistMatchType.PATH_PATTERN) {
            pathPattern = PATH_PATTERN_PARSER.parse(normalizedPattern);
        }

        return new CompiledWhitelistRule(matchType, normalizedPattern, pathPattern);
    }

}
