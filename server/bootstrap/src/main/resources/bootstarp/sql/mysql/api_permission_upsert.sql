insert into sys_permission (code, name, user_scope, created_at, updated_at)
values (?, ?, ?, current_timestamp, current_timestamp)
on duplicate key update
    name = values(name),
    user_scope = values(user_scope),
    updated_at = values(updated_at);
