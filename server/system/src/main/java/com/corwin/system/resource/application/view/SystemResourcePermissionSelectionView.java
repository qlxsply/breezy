package com.corwin.system.resource.application.view;

import java.util.List;

/**
 * View object containing the permission IDs selected for a resource.
 *
 * @author Corwin 2026/6/29
 */
public record SystemResourcePermissionSelectionView(List<Long> permissionIds) {}
