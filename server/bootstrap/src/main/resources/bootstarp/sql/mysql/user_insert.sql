insert into sys_user (id, user_type, username, nickname, password_hash, password_algo, user_status, deleted_flag,
    last_password_changed_at, created_at, created_by, updated_at, updated_by)
values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, ?, current_timestamp, ?);
