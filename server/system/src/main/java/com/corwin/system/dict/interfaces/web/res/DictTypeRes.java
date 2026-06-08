package com.corwin.system.dict.interfaces.web.res;

import com.corwin.system.dict.domain.model.DictSourceType;
import com.corwin.system.dict.domain.model.DictStructureType;
import com.corwin.system.dict.domain.model.DictValueType;

/**
 * @author Corwin 2026/3/15
 */
public record DictTypeRes(
        String id,
        String code,
        String name,
        String description,
        String enumClass,
        DictValueType valueType,
        DictStructureType structureType,
        DictSourceType sourceType,
        boolean enabled
) {
}
