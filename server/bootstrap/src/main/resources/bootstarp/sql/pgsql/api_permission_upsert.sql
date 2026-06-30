insert into sys_permission (code, name, user_scope, created_at, updated_at)
values (?, ?, ?, current_timestamp, current_timestamp)
on conflict (code) do update set
    name = excluded.name,
    user_scope = excluded.user_scope,
    updated_at = excluded.updated_at;
