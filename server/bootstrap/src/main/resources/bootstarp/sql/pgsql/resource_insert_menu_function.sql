insert into sys_menu_function (menu_id, function_id, parent_id, sort_no, visible, default_entry, system_builtin,
    created_at, updated_at)
values (?, ?, ?, ?, ?, ?, true, current_timestamp, current_timestamp);
