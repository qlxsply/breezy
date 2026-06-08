update sys_config
set expired = true
where code in (${codePlaceholders})
