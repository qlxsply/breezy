package com.corwin.framework.event.durable.store;

import javax.sql.DataSource;

/**
 * Resolves the {@link DatabaseVendor} from a {@link javax.sql.DataSource}.
 *
 * @author Corwin 2026/4/12
 */
public interface DatabaseVendorResolver {

    /**
     * Resolves the database vendor from the given data source.
     */
    DatabaseVendor resolve(DataSource dataSource);
}

