update sys_user_application_package
set package_name = ?,
    package_type = ?,
    description = ?,
    enabled = ?,
    default_package = ?,
    system_built_in = ?,
    display_order = ?,
    updated_by = ?,
    updated_at = ?
where package_code = ?;
