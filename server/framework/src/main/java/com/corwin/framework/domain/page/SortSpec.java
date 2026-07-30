package com.corwin.framework.domain.page;

/**
 * A single sort specification: a field name and sort direction.
 *
 * @param field     the field name to sort by
 * @param direction the sort direction; defaults to {@link SortDirection#ASC} if null
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
