insert into sys_permission (code, name, user_scope, description, system_builtin, enabled, created_at, updated_at)
values (?, ?, ?, ?, true, true, current_timestamp, current_timestamp)
on duplicate key update
    name = values(name),
    user_scope = values(user_scope),
    description = values(description),
    system_builtin = values(system_builtin),
    enabled = values(enabled),
    updated_at = values(updated_at);
