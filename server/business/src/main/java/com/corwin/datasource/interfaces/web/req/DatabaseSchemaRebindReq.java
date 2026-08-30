package com.corwin.datasource.interfaces.web.req;

import com.corwin.framework.json.JsonLongString;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Corwin 2026/2/7
 */
@Getter
@Setter
public class DatabaseSchemaRebindReq {
  @JsonLongString private Long dataSourceId;
}
