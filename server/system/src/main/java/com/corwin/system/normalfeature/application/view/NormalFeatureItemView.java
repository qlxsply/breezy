package com.corwin.system.normalfeature.application.view;

/**
 * @author Corwin 2026/5/31
 */
public record NormalFeatureItemView(
        Long id,
        String code,
        String name,
        String description,
        boolean enabled
) {
}
