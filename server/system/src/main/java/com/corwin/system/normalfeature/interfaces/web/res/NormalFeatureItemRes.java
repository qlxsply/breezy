package com.corwin.system.normalfeature.interfaces.web.res;

/**
 * @author Corwin 2026/5/31
 */
public record NormalFeatureItemRes(
        String id,
        String code,
        String name,
        String description,
        boolean enabled
) {
}
