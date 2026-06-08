select code, id, user_scope
from sys_permission
where enabled = true
order by code;
