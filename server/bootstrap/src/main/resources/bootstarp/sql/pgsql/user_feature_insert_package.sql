insert into sys_user_application_package (
    package_code, package_name, package_type, description, enabled, default_package,
    system_built_in, display_order, created_by, created_at, updated_by, updated_at
) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
