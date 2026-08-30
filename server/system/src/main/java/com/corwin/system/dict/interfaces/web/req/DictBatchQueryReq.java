package com.corwin.system.dict.interfaces.web.req;

import java.util.List;

/**
 * Request DTO for batch querying dictionary items by multiple type codes.
 *
 * @author Corwin 2026/3/15
 */
public record DictBatchQueryReq(List<String> codes) {}
