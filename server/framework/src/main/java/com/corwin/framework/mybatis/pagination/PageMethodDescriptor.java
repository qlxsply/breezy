package com.corwin.framework.mybatis.pagination;

/**
 * PageData 自动分页方法描述。
 *
 * @author Corwin 2026/7/28
 */
record PageMethodDescriptor(boolean automaticPagination) {

    static final PageMethodDescriptor ORDINARY = new PageMethodDescriptor(false);

    static final PageMethodDescriptor PAGED = new PageMethodDescriptor(true);
}
