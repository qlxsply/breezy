package com.corwin.system.dict.interfaces.web.req;

import com.corwin.system.dict.domain.model.DictStructureType;
import com.corwin.system.dict.domain.model.DictValueType;
import java.util.List;

/**
 * Request DTO for creating a new dictionary type.
 *
 * @author Corwin 2026/3/15
 */
public record CreateDictTypeReq(
    String code,
    String name,
    String description,
    String enumClass,
    DictValueType valueType,
    DictStructureType structureType,
    boolean enabled,
    List<SaveDictTypeItemReq> items) {}
