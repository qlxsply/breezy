package com.corwin.system.role.application.view;

import java.util.List;

/**
 * View of the currently selected (granted) resource IDs for a role.
 *
 * @author Corwin 2026/5/19
 */
public record RoleGrantSelectionView(
        List<Long> resourceIds
) {
}
