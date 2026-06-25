insert into sys_storage_file (
    id,
    owner_type,
    owner_id,
    parent_id,
    node_type,
    file_name,
    physical_file_id,
    purpose,
    created_at,
    updated_at
) values (
    ?,
    ?,
    ?,
    null,
    'FOLDER',
    ?,
    null,
    null,
    current_timestamp,
    current_timestamp
)
