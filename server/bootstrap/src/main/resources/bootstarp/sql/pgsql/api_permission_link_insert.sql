insert into sys_api_permission (api_id, permission_id, system_builtin, created_at)
select api.id, permission.id, true, current_timestamp
from sys_api api
join sys_permission permission on permission.code = ?
where api.module = ?
  and api.protocol = ?
  and api.http_method = ?
  and api.path_pattern = ?;
