select file_name
from sys_storage_file
where owner_type = ?
  and owner_id = ?
  and node_type = 'FOLDER'
  and parent_id is null
