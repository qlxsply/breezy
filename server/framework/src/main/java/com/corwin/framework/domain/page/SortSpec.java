package com.corwin.framework.domain.page;

/**
 * @author Corwin 2026/3/30
 */
public record SortSpec(
        String field,
        SortDirection direction
) {
    public SortSpec {
        if (direction == null) {
            direction = SortDirection.ASC;
        }
    }
}
