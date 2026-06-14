update sys_product_application
set application_name = ?,
    description = ?,
    icon = ?,
    route_path = ?,
    component_path = ?,
    enabled = ?,
    system_built_in = ?,
    display_order = ?,
    updated_by = ?,
    updated_at = ?
where application_code = ?;
