package com.corwin.system.dict.interfaces.web.req;

import com.corwin.system.dict.domain.model.DictStructureType;
import com.corwin.system.dict.domain.model.DictValueType;
import java.util.List;

/**
 * Request DTO for updating an existing dictionary type.
 *
 * @author Corwin 2026/3/15
 */
public record UpdateDictTypeReq(
    String name,
    String description,
    String enumClass,
    DictValueType valueType,
    DictStructureType structureType,
    boolean enabled,
    List<SaveDictTypeItemReq> items) {}
