package com.corwin.system.role.application.view;

import java.util.List;

/**
 * @author Corwin 2026/5/19
 */
public record RoleGrantSelectionView(
        List<Long> resourceIds
) {
}
