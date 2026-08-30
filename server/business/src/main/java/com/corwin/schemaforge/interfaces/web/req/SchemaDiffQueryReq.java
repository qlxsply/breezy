package com.corwin.schemaforge.interfaces.web.req;

import com.corwin.framework.json.JsonLongString;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Corwin 2026/2/24
 */
@Getter
@Setter
public class SchemaDiffQueryReq {

  @JsonLongString private Long refDbId;

  @JsonLongString private Long targetDbId;
}
