package com.corwin.framework.mybatis.pagination;

/**
 * Describes whether a MyBatis mapper method requires automatic {@link PageData} pagination
 * based on its parameter and return types.
 *
 * @param automaticPagination whether the method qualifies for automatic pagination
 * @author Corwin 2026/7/28
 */
record PageMethodDescriptor(boolean automaticPagination) {

    static final PageMethodDescriptor ORDINARY = new PageMethodDescriptor(false);

    static final PageMethodDescriptor PAGED = new PageMethodDescriptor(true);
}
