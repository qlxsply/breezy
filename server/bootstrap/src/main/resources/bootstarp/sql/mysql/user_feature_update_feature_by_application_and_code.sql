update sys_product_feature
set feature_name = ?,
    description = ?,
    enabled = ?,
    system_built_in = ?,
    display_order = ?,
    updated_by = ?,
    updated_at = ?
where application_id = ? and feature_code = ?;
