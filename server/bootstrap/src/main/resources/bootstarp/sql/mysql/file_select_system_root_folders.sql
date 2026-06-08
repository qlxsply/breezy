select folder_name
from sys_storage_folder
where owner_type = ?
  and owner_id = ?
  and parent_id is null
