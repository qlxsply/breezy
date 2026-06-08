insert into sys_storage_folder (
    id,
    owner_type,
    owner_id,
    parent_id,
    folder_name,
    created_at,
    updated_at
) values (
    ?,
    ?,
    ?,
    null,
    ?,
    current_timestamp,
    current_timestamp
)
