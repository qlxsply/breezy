insert into sys_permission (code, name, user_scope, description, system_builtin, enabled, created_at, updated_at)
values (?, ?, ?, ?, true, true, current_timestamp, current_timestamp)
on conflict (code) do update set
    name = excluded.name,
    user_scope = excluded.user_scope,
    description = excluded.description,
    system_builtin = excluded.system_builtin,
    enabled = excluded.enabled,
    updated_at = excluded.updated_at;
