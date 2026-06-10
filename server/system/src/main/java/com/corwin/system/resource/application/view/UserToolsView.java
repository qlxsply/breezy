package com.corwin.system.resource.application.view;

import java.util.List;

/**
 * @author Corwin 2026/6/6
 */
public record UserToolsView(
        List<UserToolPageView> tools,
        List<String> permissionCodes
) {
}
