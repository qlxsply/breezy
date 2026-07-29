package com.corwin.framework.web.sort;

/**
 * 可排序字段定义，field 面向前端，column 面向 SQL。
 *
 * @author Corwin 2026/7/29
 */
public record SortableField(
        String field,
        String column
) {
}
