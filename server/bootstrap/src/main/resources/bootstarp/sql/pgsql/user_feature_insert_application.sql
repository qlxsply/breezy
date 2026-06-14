insert into sys_product_application (
    application_code, application_name, description, icon, route_path, component_path,
    enabled, system_built_in, display_order, created_by, created_at, updated_by, updated_at
) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
