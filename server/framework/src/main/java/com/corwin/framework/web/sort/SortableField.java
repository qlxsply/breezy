package com.corwin.framework.web.sort;

/**
 * Maps a frontend-facing sort field name to its corresponding SQL column name.
 *
 * @param field  the field name exposed to the frontend
 * @param column the corresponding SQL column name
 * @author Corwin 2026/7/29
 */
public record SortableField(
        String field,
        String column
) {
}
