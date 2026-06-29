insert into sys_resource (code, parent_id, name, resource_type, path, component, icon, sort_no, visible, enabled,
    default_entry, system_builtin, remark, created_at, updated_at)
values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, ?, current_timestamp, current_timestamp);
