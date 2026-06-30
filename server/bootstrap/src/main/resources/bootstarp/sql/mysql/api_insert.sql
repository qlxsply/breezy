insert into sys_api (module, protocol, http_method, path_pattern, handler_class, handler_method,
    permission_declared, access_type, user_type, audit_declared, audit_resource, audit_action,
    audit_description, enabled, system_builtin, created_at, updated_at)
values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, true, current_timestamp, current_timestamp);
