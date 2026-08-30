package com.corwin.system.dict.application.view;

import com.corwin.system.dict.domain.model.DictSourceType;
import com.corwin.system.dict.domain.model.DictStructureType;
import com.corwin.system.dict.domain.model.DictValueType;

/**
 * View object representing a dictionary type for read-only display.
 *
 * @author Corwin 2026/3/15
 */
public record DictTypeView(
    String id,
    String code,
    String name,
    String description,
    String enumClass,
    DictValueType valueType,
    DictStructureType structureType,
    DictSourceType sourceType,
    boolean enabled) {}
