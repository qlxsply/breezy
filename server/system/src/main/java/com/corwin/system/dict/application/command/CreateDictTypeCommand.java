package com.corwin.system.dict.application.command;

import com.corwin.system.dict.domain.model.DictStructureType;
import com.corwin.system.dict.domain.model.DictValueType;

import java.util.List;

/**
 * @author Corwin 2026/3/15
 */
public record CreateDictTypeCommand(
        String code,
        String name,
        String description,
        String enumClass,
        DictValueType valueType,
        DictStructureType structureType,
        boolean enabled,
        List<SaveDictTypeItemCommand> items
) {
}
