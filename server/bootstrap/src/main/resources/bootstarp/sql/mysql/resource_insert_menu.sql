insert into sys_menu (code, parent_id, name, path, component, icon, menu_type, sort_no, visible, enabled, system_builtin, remark,
    created_at, updated_at)
values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, ?, current_timestamp, current_timestamp);
